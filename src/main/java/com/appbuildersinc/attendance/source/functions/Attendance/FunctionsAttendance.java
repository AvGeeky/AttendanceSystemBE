package com.appbuildersinc.attendance.source.functions.Attendance;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.ClassDB;
import com.appbuildersinc.attendance.source.database.MongoDB.FacultyDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SubstitutionDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SuperAdminDB;
import com.appbuildersinc.attendance.source.database.redis.RedisService;
import com.appbuildersinc.attendance.source.functions.Students.FunctionsStudents;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class FunctionsAttendance {
    private static final String CHAR_POOL = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final String DELIMITER = "~";

    private final ClassDB classDB;
    private final SubstitutionDB substitutionDBclass;
    private final FacultyDB userdb;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;
    private final FunctionsStudents functionstudenclass;
    final RedisService redisService;
    @Autowired
    public FunctionsAttendance(ClassDB classDB, SubstitutionDB substitutionDBclass, FacultyDB userdb, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass, FunctionsStudents functionstudenclass, RedisService redisService) {
        this.classDB = classDB;
        this.substitutionDBclass = substitutionDBclass;
        this.userdb = userdb;
        this.emailclass =emailutil;
        this.keyclass =keyutil;
        this.jwtclass = jwtutil;
        this.admindb=admindb;
        this.adminjwtclass = adminjwtclass;
        this.functionstudenclass = functionstudenclass;
        this.redisService = redisService;

    }

    public boolean isAuthorizedViaSubcodeOrEmail(String classCode, String email, String subCode) {
        // Check if substitution code is valid
        if (subCode != null) {
            boolean subCodeAuth = substitutionDBclass.fetchAndVerifySubstitutionCode(subCode, classCode);
            if (subCodeAuth) return true;
        }

        // If not authorized by subCode, check faculty email
        String facultyEmail = classDB.getFacultyEmailFromClass(classCode);
        return facultyEmail != null && facultyEmail.equalsIgnoreCase(email);
    }


    public String generateSubstitutionCode(String classCode, Date date) throws NoSuchAlgorithmException {

            // Convert Date to a string like "2025-06-16"
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = sdf.format(date);

            // Combine classCode + date string
            String input = classCode + dateStr;

            // Hash using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Take first few bytes and convert to long
            long hashLong = 0;
            for (int i = 0; i < 5; i++) {
                hashLong = (hashLong << 8) | (hashBytes[i] & 0xff);
            }

            // Convert to base36 (lowercase letters + digits)
            String code = Long.toString(Math.abs(hashLong), 36);

            // Ensure exactly 6 characters
            if (code.length() < CODE_LENGTH) {
                code = String.format("%6s", code).replace(' ', 'a').substring(0, CODE_LENGTH);
            } else if (code.length() > CODE_LENGTH) {
                code = code.substring(0, CODE_LENGTH);
            }

            return code;
    }

    public Date createCleanDate (Date dateofuse){
        Calendar cal = Calendar.getInstance();
        cal.setTime(dateofuse);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date cleanDate = cal.getTime();
        return cleanDate;
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHAR_POOL.length());
            sb.append(CHAR_POOL.charAt(index));
        }
        return sb.toString();
    }

    public List<String> generateAttendanceCodes(String classCode) {
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String randomPart = generateRandomString(CODE_LENGTH);
            String fullCode = randomPart + DELIMITER + classCode;
            codes.add(fullCode);
        }
        return codes;
    }

    public String generateSingleAttendanceCode(String classCode) {
            String randomPart = generateRandomString(CODE_LENGTH);
            String fullCode = randomPart + DELIMITER + classCode;
            return fullCode;
    }

    public String extractClassCode(String fullCode) {
        return fullCode.split("~")[1];
    }


    public List<String> initialiseQRAttendanceAndReturnCodes(String classCode){
        CloseAttendanceWithoutSaving(classCode);

        List<String> classCodes = generateAttendanceCodes(classCode);

        Map<String,Object> regNoHmacMapping = classDB.getClassRegNoHmacMapping(classCode);

        redisService.storeHmacKeys(classCode,regNoHmacMapping);

        redisService.storeStudentNameDetails(classCode);

        redisService.initializeAttendanceTracking(classCode);

        return classCodes;

    }

    public String initialiseSingleCodeAttendanceAndReturnCode(String classCode){
        CloseAttendanceWithoutSaving(classCode);
        String singleAttendanceCode = generateSingleAttendanceCode(classCode);

        Map<String,Object> regNoHmacMapping = classDB.getClassRegNoHmacMapping(classCode);

        redisService.storeHmacKeys(classCode,regNoHmacMapping);

        redisService.storeStudentNameDetails(classCode);

        redisService.initializeAttendanceTracking(classCode);

        return singleAttendanceCode;

    }

    public void CloseAttendanceWithoutSaving(String classCode){

        redisService.deleteVerifiedStudents(classCode);
        redisService.deleteStudentHMACMappings(classCode);
        redisService.deleteStudentNamesForClass(classCode);

        redisService.deleteActiveClassCodes(classCode);
        redisService.deleteActiveSingleClassCode(classCode);
    }

    public void SaveAttendanceAndClose(String classCode){
        Map<String, Integer> lectureRecord = generateLectureRecord(
                redisService.getHmacKeyMapForClass(classCode),
                redisService.getVerifiedStudents(classCode)
        );
        classDB.addAttendanceRecord(classCode,lectureRecord);
        redisService.deleteVerifiedStudents(classCode);
        redisService.deleteStudentHMACMappings(classCode);
        redisService.deleteStudentNamesForClass(classCode);

        redisService.deleteActiveClassCodes(classCode);
        redisService.deleteActiveSingleClassCode(classCode);
    }

    public String createDigest(String originalText, String HMAC_SECRET) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secretKeySpec);

        byte[] hmacBytes = sha256_HMAC.doFinal(originalText.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
    }


    public boolean verifyDigest(String originalText, String receivedHmac, String HMAC_SECRET) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(HMAC_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secretKeySpec);

        byte[] computedHmac = sha256_HMAC.doFinal(originalText.getBytes(StandardCharsets.UTF_8));
        String computedBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(computedHmac);

        return computedBase64.equals(receivedHmac);
    }

    /**
     * Generates a lecture record showing present (1) or absent (0) for each student.
     *
     * @param studentHmacMap Map of all register numbers and their HMAC keys.
     * @param verifiedStudents Set of register numbers who have marked attendance successfully.
     * @return A Map where key = regno, value = 1 (present) or 0 (absent).
     */
    public Map<String, Integer> generateLectureRecord(Map<String, String> studentHmacMap, Set<String> verifiedStudents) {
        Map<String, Integer> lectureRecord = new HashMap<>();

        for (String regno : studentHmacMap.keySet()) {
            lectureRecord.put(regno, verifiedStudents.contains(regno) ? 1 : 0);
        }
        // Date formatted as YYYYMMDD
        int dateInt = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        // e.g., 20250616

        // Time formatted as HHmm
        int timeInt = Integer.parseInt(LocalTime.now().format(DateTimeFormatter.ofPattern("HHmm")));

        // Store the date and time in the lecture record
        lectureRecord.put("date", dateInt);
        lectureRecord.put("time", timeInt);

        return lectureRecord;
    }
    public Map<String,String> getAllStudentDetails(String classCode,String subCode,String email){
       if(isAuthorizedViaSubcodeOrEmail(classCode,email,subCode)){
            Map<String,String> result= classDB.getregistermap(classCode);
           return result;

       }
       else{
           return null;
       }

    }
    public Boolean SaveManualAttendance(String classCode,String email,String subCode,List<String>present,List<String>absent){
        if(isAuthorizedViaSubcodeOrEmail(classCode,email,subCode)){
            Map<String,Object> classdetails=classDB.getAllClassDetails(classCode);
            Map<String,Object> attendance=(Map<String,Object>)classdetails.get("attendance");
            int nextlectureno=attendance.size()+1;
            String lecturekey="lecture."+nextlectureno;
            Map<String,Object> lecturerecord=new HashMap<>();
            int dateInt = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
            int timeInt = Integer.parseInt(LocalTime.now().format(DateTimeFormatter.ofPattern("HHmm")));
            lecturerecord.put("date",dateInt);
            lecturerecord.put("time",timeInt);
            for(String registernumber:present){
                lecturerecord.put(registernumber,1);
            }
            for(String registernumber:absent){
                lecturerecord.put(registernumber,0);
            }
            attendance.put(lecturekey,lecturerecord);
            return classDB.updateAttendance(classCode, attendance);
        }
        else{
            return false;
        }

    }


}
