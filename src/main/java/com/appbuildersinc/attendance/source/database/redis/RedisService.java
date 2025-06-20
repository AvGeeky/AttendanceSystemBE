package com.appbuildersinc.attendance.source.database.redis;

import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RedisService {
    public static final int DEFAULT_TTL_HOURS = 1; // FOR VERIFIED STUD AND FAST LOOKUP
    public static final int DEFAULT_TTL_CODE_SECONDS = 60; // FOR CODE LOOKUPS
    private static final String VERSION_PREFIX = "attendance:version:";
    private static final Duration VERSION_TTL = Duration.ofHours(1);
    private static final long MIN_INTERVAL_MS = 1000; // 1 second between bumps

    private final Map<String, Long> lastBumpTime = new ConcurrentHashMap<>();

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private final FacultyDB userdbclass;
    private final ClassDB classDB;
    private final StudentDB studentDbClass;
    private final SuperAdminDB SuperAdminDbClass;
    private final LogicalGroupingDB logicalGroupingDbClass;
    private final SubstitutionDB substitutionDBclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil facultyJwtUtil;
    private final StudentjwtUtil studentjwtUtil;
    private final SuperAdminjwtUtil adminjwtUtil;



    @Autowired
    RedisService(FacultyDB userdbclass, ClassDB classDB, StudentDB studentDbClass, SuperAdminDB superAdminDbClass, LogicalGroupingDB logicalGroupingDbClass, SubstitutionDB substitutionDBclass, KeyPairUtil keyclass, FacultyJwtUtil facultyJwtUtil, StudentjwtUtil studentjwtUtil, SuperAdminjwtUtil adminjwtUtil){
        this.userdbclass = userdbclass;
        this.classDB = classDB;
        this.studentDbClass = studentDbClass;
        this.SuperAdminDbClass = superAdminDbClass;
        this.logicalGroupingDbClass = logicalGroupingDbClass;
        this.substitutionDBclass = substitutionDBclass;
        this.keyclass = keyclass;
        this.facultyJwtUtil = facultyJwtUtil;
        this.studentjwtUtil = studentjwtUtil;
        this.adminjwtUtil = adminjwtUtil;

    }



    public void storeHmacKeys(String classCode, Map<String, Object> regNoToHmacMap) {
        String key = "attendance:students:" + classCode;

        if (regNoToHmacMap == null || regNoToHmacMap.isEmpty()) return;

        redisTemplate.opsForHash().putAll(key, regNoToHmacMap);

        // Set TTL to 1 hour
        redisTemplate.expire(key, Duration.ofHours(DEFAULT_TTL_HOURS));
    }


    public void storeStudentNameDetails(String classCode) {
        String key = "attendance:name:" + classCode;
        Map<String, Object> regnoName = classDB.getClassRegNoNameMapping(classCode);

        // Use putAll to store the entire map at once
        redisTemplate.opsForHash().putAll(key, regnoName);

        // Set TTL to 1 hour
        redisTemplate.expire(key, Duration.ofHours(DEFAULT_TTL_HOURS));
    }


    public Map<String,Object> getStudentNameFromRedis(String classCode, Set<String> regNoSet) {
        String key = "attendance:name:" + classCode;
        Map<String,Object> names = new HashMap<>();
        for (String regNo : regNoSet) {
            names.put(regNo,redisTemplate.opsForHash().get(key, regNo));
        }
        return names;
    }

    public void deleteStudentNamesForClass(String classCode) {
        String key = "attendance:name:" + classCode;
        if (redisTemplate.hasKey(key)) {
            redisTemplate.delete(key);
        }
    }





    public Map<String, String> getHmacKeyMapForClass(String classCode) {
        String key = "attendance:students:" + classCode;

        Map<Object, Object> rawMap = redisTemplate.opsForHash().entries(key);
        if (rawMap == null || rawMap.isEmpty()) return Collections.emptyMap();

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : rawMap.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue().toString());
        }

        return result;
    }



    public String getHmacKey(String classCode, String regNo) {
        String key = "attendance:students:" + classCode;

        // Check if hash still exists
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Object hmac = redisTemplate.opsForHash().get(key, regNo);
            return hmac != null ? hmac.toString() : null;
        }

        return null; // Key expired or does not exist
    }

    public void initializeAttendanceTracking(String classId) {
        String markedKey = "attendance:marked:" + classId;

        // Initialize empty Set for marked students to make sure the ttl starts counting down from creation
        redisTemplate.opsForSet().add(markedKey, "init"); // dummy value
        redisTemplate.opsForSet().remove(markedKey, "init");

        // Set TTL to 1 hour
        redisTemplate.expire(markedKey, Duration.ofHours(DEFAULT_TTL_HOURS));
    }

    public boolean isAttendanceTrackingActive(String classId) {
        String singleKey = "attendance:scodes:" + classId;
        String qrKey = "attendance:codes:" + classId;
        String markedKey = "attendance:marked:" + classId;
        return (redisTemplate.hasKey(singleKey) ||
               redisTemplate.hasKey(qrKey)) ||
                redisTemplate.hasKey(markedKey);
    }

    public void markStudentVerified(String classId, String registerNumber) {
        String markedKey = "attendance:marked:" + classId;

        // Add to verified set
        redisTemplate.opsForSet().add(markedKey, registerNumber);

    }

    public Set<String> getVerifiedStudents(String classId) {
        String markedKey = "attendance:marked:" + classId;

        Set<String> result = redisTemplate.opsForSet().members(markedKey);
        return result != null ? result : Collections.emptySet();
    }


    public long getVerifiedStudentCount(String classId) {
        String markedKey = "attendance:marked:" + classId;
        Long count = redisTemplate.opsForSet().size(markedKey);
        return count != null ? count : 0;
    }

    public List<String> getThreeQRCodes(String classCode) {
        String redisKey = "attendance:codes:" + classCode;

        // Fetch all code -> window JSON entries
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);

        // Sort by "start" time and return just the codes
        return entries.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey().toString(), entry.getValue().toString()))
                .sorted(Comparator.comparingLong(e -> extractStartTime(e.getValue())))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Helper method to extract the "start" value from the JSON string
    private long extractStartTime(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Long> window = mapper.readValue(json, new TypeReference<>() {});
            return window.getOrDefault("start", Long.MAX_VALUE);
        } catch (Exception e) {
            return Long.MAX_VALUE; // On error, place it last
        }
    }


    public Set<Object> getSingleAttendanceCodes(String classCode) {
        String redisKey = "attendance:scodes:" + classCode;

        // Return all field keys (i.e., attendance codes) stored in the hash
        return redisTemplate.opsForHash().keys(redisKey);
    }




    public void storeQRAttendanceCodesWithWindow(String classCode, List<String> codes) {
        if (codes == null || codes.size() != 3) {
            throw new IllegalArgumentException("Exactly 3 codes required.");
        }

        String redisKey = "attendance:codes:" + classCode;
        long currentMillis = System.currentTimeMillis();

        Map<String, String> codeWithWindows = new HashMap<>();

        for (int i = 0; i < 3; i++) {
            long start = currentMillis + (i * DEFAULT_TTL_CODE_SECONDS * 1000);
            long end = start + DEFAULT_TTL_CODE_SECONDS * 1000;

            String jsonWindow = String.format("{\"start\":%d,\"end\":%d}", start, end);
            codeWithWindows.put(codes.get(i), jsonWindow);
        }

        redisTemplate.opsForHash().putAll(redisKey, codeWithWindows);

        // Set TTL to cover total duration (3 * DEFAULT_TTL_CODE_SECONDS)
        redisTemplate.expire(redisKey, Duration.ofSeconds(DEFAULT_TTL_CODE_SECONDS * 3));
    }

    //for qr
    public boolean isQRAttendanceCodeValid(String classCode, String code) {
        String redisKey = "attendance:codes:" + classCode;

        String json = (String) redisTemplate.opsForHash().get(redisKey, code);
        if (json == null) return false;

        try {
            // Parse JSON manually for speed (no full JSON lib used)
            int startIdx = json.indexOf("\"start\":") + 8;
            int endIdx = json.indexOf(",\"end\":");
            long start = Long.parseLong(json.substring(startIdx, endIdx));

            int endStart = json.indexOf("\"end\":") + 6;
            int endBrace = json.indexOf("}", endStart);
            long end = Long.parseLong(json.substring(endStart, endBrace));

            long now = System.currentTimeMillis();
            return now >= start && now < end;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void storeSingleAttendanceCode(String classCode, String code) {
        String redisKey = "attendance:scodes:" + classCode;

        // Store the code as key in a hash, value can be a dummy or actual timestamp if needed
        redisTemplate.opsForHash().put(redisKey, code, "valid");

        // Set TTL of 1 hour from now
        if (redisTemplate.getExpire(redisKey) == -1) {
            redisTemplate.expire(redisKey, Duration.ofHours(1));
        }

    }

    //For passcode
    public boolean isSingleAttendanceCodeValid(String classCode, String code) {
        String redisKey = "attendance:scodes:" + classCode;

        // Just check if the code is a field in the hash
        return redisTemplate.opsForHash().hasKey(redisKey, code);
    }

    public void deleteActiveClassCodes(String classCode) {
        String key = "attendance:codes:" + classCode;
        if (redisTemplate.hasKey(key)) {
            redisTemplate.delete(key);
        }
    }


    public void deleteActiveSingleClassCode(String classCode) {
        String key = "attendance:scodes:" + classCode;
        if (redisTemplate.hasKey(key)) {
            redisTemplate.delete(key);
        }
    }

    public void deleteVerifiedStudents(String classCode) {
        String key = "attendance:marked:" + classCode;
        if (redisTemplate.hasKey(key)) {
            redisTemplate.delete(key);
        }
    }

    public void deleteStudentHMACMappings(String classCode) {
        String key = "attendance:students:" + classCode;
        if (redisTemplate.hasKey(key)) {
            redisTemplate.delete(key);
        }
    }

    /**
     * <b>\uD83D\uDCF6 Version Bouncing & Polling Strategy for Real-Time Attendance Updates</b>
     * <br><br>
     * <ul>
     *   <li><b>Purpose:</b> This strategy provides an efficient alternative to real-time streaming like WebSockets for live attendance tracking. It uses a Redis key to indicate updates and enables frontend polling to fetch updates only when needed.</li>
     *   <li><b>How It Works:</b>
     *     <ul>
     *       <li>A Redis key: <code>attendance:version:&lt;classId&gt;</code> is maintained per class.</li>
     *       <li>When a student is verified, the version is <b>bumped</b> (updated with a timestamp).</li>
     *       <li>The frontend polls periodically to check the version.</li>
     *       <li>If the version has changed, updated attendance data is fetched.</li>
     *       <li>If not, the frontend skips fetching — saving network and compute resources.</li>
     *     </ul>
     *   </li>
     *   <li><b>Version Bumping Strategy for Attendance Tracking</b>
     *     <ul>
     *       <li>Instead of bumping the Redis version every time a student marks attendance, we introduce a controlled versioning system where updates occur at most once per second (or configurable interval ).</li>
     *     </ul>
     *   </li>
     *   <li><b>Why Not Bump Every Time?</b>
     *     <ul>
     *       <li>Frequent bumps can occur if multiple students verify at the same time.</li>
     *       <li>Causes redundant writes to Redis version database(even if it's fast).</li>
     *       <li>Triggers unnecessary frontend re-fetches.</li>
     *       <li>May cause network and processing overhead.</li>
     *     </ul>
     *   </li>
     *   <li><b>Why Use an Interval (1s or 3.5s)?</b>
     *     <ul>
     *       <li>Debounces frequent backend writes.</li>
     *       <li>Ensures frontend gets updates without being overwhelmed.</li>
     *       <li>Humans can't perceive small delays — under 1s feels real-time.</li>
     *     </ul>
     *   </li>
     *   <li><b>Strategy (Debounce Logic):</b>
     *     <ul>
     *       <li>Track the last bump timestamp (e.g. in Redis or memory).</li>
     *       <li>Only bump if <code>now - last_bump &gt;= MIN_INTERVAL</code>.</li>
     *       <li>Otherwise, skip bumping — frontend will still poll periodically.</li>
     *     </ul>
     *   </li>
     *   <li><b>Analogy:</b> Imagine a whiteboard being updated every time a student walks in. It flickers and becomes unreadable. But if you update it once per second, it stays smooth and accurate.</li>
     *   <li><b>\u23F1\uFE0F Debounce Logic</b>
     *     <ul>
     *       <li>A debounce buffer (e.g., 1 second) is used to avoid excessive writes to Redis.</li>
     *       <li>Even if multiple students are verified in quick succession, the version is updated only once per buffer window.</li>
     *     </ul>
     *   </li>
     *   <li><b>Redis Keys Used:</b>
     *     <ul>
     *       <li><code>attendance:version:&lt;classId&gt;</code> – String – current version of attendance (UUID or timestamp).</li>
     *       <li><code>attendance:marked:&lt;classId&gt;</code> – Set – students who’ve marked attendance.</li>
     *     </ul>
     *   </li>
     *   <li><b>Advantages:</b>
     *     <ul>
     *       <li><b>Lightweight</b>: No WebSockets or SSE needed.</li>
     *       <li><b>Optimized</b>: Works well with Redis and fast clients like React Native.</li>
     *       <li><b>Mobile-friendly</b>: Works over polling with minimal resource use.</li>
     *       <li><b>Testable</b>: Easy to simulate in Postman using periodic polling.</li>
     *     </ul>
     *   </li>
     * </ul>
     */

    // Bumps version immediately
    private void bumpVersion(String classId) {
        String versionKey = VERSION_PREFIX + classId;
        String timestamp = String.valueOf(System.currentTimeMillis());
        redisTemplate.opsForValue().set(versionKey, timestamp, VERSION_TTL);
        lastBumpTime.put(classId, System.currentTimeMillis());
    }

    // Debounced bump: only bumps if MIN_INTERVAL_MS has passed since last bump
    public void bumpVersionDebounced(String classId) {
        long now = System.currentTimeMillis();
        long last = lastBumpTime.getOrDefault(classId, 0L);

        if (now - last >= MIN_INTERVAL_MS) {
            bumpVersion(classId);
        }
    }

    // Returns current version (timestamp string or null if not set)
    public String getVersion(String classId) {
        String versionKey = VERSION_PREFIX + classId;
        return redisTemplate.opsForValue().get(versionKey);
    }















}
