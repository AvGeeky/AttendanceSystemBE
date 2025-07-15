package com.appbuildersinc.attendance.source.Controllers.Attendance;

import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.jsonVerifier.JsonVerifier;
import com.appbuildersinc.attendance.source.database.MongoDB.*;
import com.appbuildersinc.attendance.source.database.redis.RedisService;
import com.appbuildersinc.attendance.source.functions.Attendance.FunctionsAttendance;
import com.appbuildersinc.attendance.source.functions.Class.FunctionsClass;
import com.appbuildersinc.attendance.source.functions.Faculty.FunctionsFaculty;
import com.appbuildersinc.attendance.source.functions.Students.FunctionsStudents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.util.*;

/**
 * <b>Standard HTTP Error Response Codes:</b>
 * <ul>
 *    <b>RETURN STATUS 'E' FOR ALL ERRORS. STATUS 'S' FOR ALL SUCCESS</b>
 *   <li><b>400 Bad Request:</b> Required data not passed / JWT not passed.</li>
 *   <li><b>401 Unauthorized:</b> Authentication is required or has failed.</li>
 *   <li><b>403 Forbidden:</b> The user does not have permission to access the resource.</li>
 *   <li><b>503 Service Unavailable:</b> The server is currently unable to handle the request.</li>
 * </ul>
 * </p>
 * <hr>
 * <b>Attendance Controller Endpoints Overview</b>
 * <ul>
 *   <li><b>/faculty/createSubstitutionCode</b> (POST): Faculty creates a substitution code for a class and date. <br>
 *       <b>Input:</b> JWT in header, classCode, dateOfUse (yyyy-MM-dd). <br>
 *       <b>Output:</b> Substitution code or error message.
 *   </li>
 *   <li><b>/faculty/fetchClassCodeFromSubstitutionCode</b> (GET): Fetches the class code mapped to a substitution code. <br>
 *       <b>Input:</b> JWT in header, substitutionCode. <br>
 *       <b>Output:</b> Class code or error message.
 *   </li>
 *   <li><b>/faculty/qr/generateQRCode</b> (POST): Generates QR codes for attendance for a class. <br>
 *       <b>Input:</b> JWT in header, classCode, optional subCode. <br>
 *       <b>Output:</b> List of QR codes or error/status.
 *   </li>
 *   <li><b>/student/qr/sendCode</b> (POST): Student submits scanned QR code and digest for attendance. <br>
 *       <b>Input:</b> JWT in header, digest, qrCode. <br>
 *       <b>Output:</b> Attendance verification status.
 *   </li>
 *   <li><b>/faculty/passcode/generateCode</b> (POST): Faculty generates a single passcode for attendance. <br>
 *       <b>Input:</b> JWT in header, classCode, optional subCode. <br>
 *       <b>Output:</b> Passcode or error/status.
 *   </li>
 *   <li><b>/student/passcode/sendCode</b> (POST): Student submits passcode and digest for attendance. <br>
 *       <b>Input:</b> JWT in header, digest, passcode. <br>
 *       <b>Output:</b> Attendance verification status.
 *   </li>
 *   <li><b>/faculty/liveAttendanceViewWithVersion</b> (GET): Faculty fetches live attendance data and version for a class. <br>
 *       <b>Input:</b> JWT in header, classCode, optional version. <br>
 *       <b>Output:</b> Attendance data, version, and status.
 *   </li>
 *   <li><b>/faculty/qrpasscode/confirmAttendanceClose</b> (POST): Faculty closes attendance session and saves data. <br>
 *       <b>Input:</b> JWT in header, classCode, optional subCode. <br>
 *       <b>Output:</b> Confirmation status.
 *   </li>
 *   <li><b>/generateDigest</b> (GET, testing only): Generates HMAC digest for a code and register number. <br>
 *       <b>Input:</b> JWT in header, reg, code. <br>
 *       <b>Output:</b> HMAC passcode and digest.
 *   </li>
 *   <li><b>/faculty/getAllStudentDetails</b> (POST): Faculty fetches all student details for a class. <br>
 *       <b>Input:</b> JWT in header, classCode, optional subCode. <br>
 *       <b>Output:</b> Student details or error.
 *   </li>
 *   <li><b>/faculty/saveManualAttendance</b> (POST): Faculty saves manual attendance for a class. <br>
 *       <b>Input:</b> JWT in header, request body with classCode, present, absent, optional subCode. <br>
 *       <b>Output:</b> Status of save operation.
 *   </li>
 * </ul>
 * <hr>
 */


@RestController
public class ControllerAttendance {
    private final FunctionsClass functionsClassService;
    private final FunctionsAttendance functionsAttendanceService;
    private final FunctionsFaculty functionsFacultyService;
    private final FunctionsStudents functionsStudentsService;

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

    private final RedisService redisService;
    private final JsonVerifier jsonverifier;

    @Autowired
    public ControllerAttendance(FunctionsAttendance fa, FunctionsFaculty functionsFacultyService, FacultyDB userdbutil, ClassDB classDB, FacultyJwtUtil jwtutil, KeyPairUtil keyutil, StudentjwtUtil stdjwtutil, StudentDB studdb, SuperAdminjwtUtil adminutil, SuperAdminDB SuperAdminDbClass, LogicalGroupingDB logicalGroupingDbClass, FunctionsClass functionsClassService, FunctionsStudents functionsStudentsService, SubstitutionDB substitutionDBclass, RedisService redisService, JsonVerifier jsonverifier) {
        this.functionsFacultyService = functionsFacultyService;
        this.classDB = classDB;
        this.functionsClassService = functionsClassService;
        this.functionsAttendanceService=fa;
        this.userdbclass = userdbutil;
        this.facultyJwtUtil = jwtutil;
        this.keyclass =keyutil;
        this.studentjwtUtil = stdjwtutil;
        this.studentDbClass =studdb;
        this.adminjwtUtil=adminutil;
        this.SuperAdminDbClass=SuperAdminDbClass;
        this.logicalGroupingDbClass = logicalGroupingDbClass;
        this.functionsStudentsService = functionsStudentsService;
        this.substitutionDBclass = substitutionDBclass;
        this.redisService = redisService;
        this.jsonverifier = jsonverifier;
    }




    /**
     * Faculty creates a substitution code for a class and date.
     * @param authorizationHeader JWT token in the header
     * @param classCode Class code for which to create the substitution code
     * @param dateOfUse Date of use for the substitution code (yyyy-MM-dd)
     * @return ResponseEntity with status, substitution code, or error message
     * @throws Exception if any error occurs
     */
    @PostMapping("/faculty/createSubstitutionCode")
    public ResponseEntity<Map<String,Object>> createSubstitutionCode(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                                     @RequestParam String classCode,
                                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date dateOfUse // Accepts only yyyy-MM-dd
                                                                        ) throws Exception {
            Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
            //Check if the JWT is valid
            String status = (String) claims.get("status");
            if (status.equals("S")) {
                //JWT is valid, proceed with business logic
                Map<String, Object> response = new HashMap<>();
                if (!classDB.classExists(classCode)){
                    response.put("status", "E");
                    response.put("message", "Class code does not exist.");
                    return ResponseEntity.status(404).body(response);
                }
                String subCode = functionsAttendanceService.generateSubstitutionCode(classCode,dateOfUse);
                Date cleanDate = functionsAttendanceService.createCleanDate(dateOfUse);
                substitutionDBclass.storeSubstitutionCode(subCode, classCode, cleanDate);
                response.put("status", "S");
                response.put("substitutionCode", subCode);
                response.put("message", "Substitution code created successfully.");
                return ResponseEntity.ok(response);
            } else {
                //JWT is invalid, return error response
                return ResponseEntity.status(401).body(claims);
            }
    }

    /**
     * Fetch class code from a substitution code.
     * @param authorizationHeader JWT token in the header
     * @param substitutionCode Substitution code to fetch class code
     * @return ResponseEntity with status, class code, or error message
     * @throws Exception if any error occurs
     */
    @GetMapping("/faculty/fetchClassCodeFromSubstitutionCode")
    public ResponseEntity<Map<String,Object>> fetchClassCodeFromSubstitutionCode(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                                     @RequestParam String substitutionCode
    ) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            String classCode = substitutionDBclass.fetchClassCodeFromSubstitutionCode(substitutionCode);

            if (classCode == null) {
                response.put("status", "E");
                response.put("message", "Substitution code not found / date of use not arrived yet / expired.");
                return ResponseEntity.status(404).body(response);
            }
            response.put("status", "S");
            response.put("classCode", classCode);
            response.put("message", "Class code fetched successfully.");
            return ResponseEntity.ok(response);
        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    /**
     * Faculty generates QR codes for attendance.
     * @param authorizationHeader JWT token in the header
     * @param subCode Optional substitution code
     * @param classCode Class code for which to generate QR codes
     * @return ResponseEntity with status, message, and generated QR codes
     * @throws Exception if any error occurs
     */
    @PostMapping("/faculty/qr/generateQRCode")
    public ResponseEntity<Map<String,Object>> generateQRCode(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                               String authorizationHeader,
                                                               @RequestParam(required = false) String subCode,
                                                               @RequestParam String classCode) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            if (!classDB.classExists(classCode)) {
                response.put("status", "E");
                response.put("message", "Class code does not exist.");
                return ResponseEntity.status(404).body(response);
            }
            if (!functionsAttendanceService.isAuthorizedViaSubcodeOrEmail(classCode, (String) claims.get("email"), subCode)) {
                response.put("status", "E");
                response.put("message", "Unauthorized access. Invalid substitution code or email.");
                return ResponseEntity.status(403).body(response);
            }

            if (redisService.isAttendanceTrackingActive(classCode)){
                response.put("status", "S");
                response.put("message", "Existing attendance tracking has been closed. New QR codes generated and synced.");
                List<String> codesForQr = functionsAttendanceService.initialiseQRAttendanceAndReturnCodes(classCode);
                response.put("codes", codesForQr);
                redisService.storeQRAttendanceCodesWithWindow(classCode, codesForQr);
                return ResponseEntity.ok(response);
            }
            //generate QR codes for attendance and initialise redis databases
            List<String> codesForQr = functionsAttendanceService.initialiseQRAttendanceAndReturnCodes(classCode);

            response.put("status","S");
            response.put("message", "QR codes generated and synced successfully.");
            response.put("codes", codesForQr);

            redisService.storeQRAttendanceCodesWithWindow(classCode, codesForQr);

            return ResponseEntity.ok(response);


        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    /**
     * Student submits scanned QR code and digest for attendance verification.
     * @param authorizationHeader JWT token in the header
     * @param digest HMAC digest of the QR code
     * @param qrCode Scanned QR code submitted by the student
     * @return ResponseEntity with status and message
     * @throws Exception if any error occurs
     */
    @PostMapping("/student/qr/sendCode")
    public ResponseEntity<Map<String,Object>> sendCode(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                             String authorizationHeader,
                                                             @RequestParam String digest,
                                                             @RequestParam String qrCode
                                                       ) throws Exception {
        Map<String, Object> claims = functionsStudentsService.checkJwtAuthAfterLoginStudent(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            String registerNumber = claims.get("registerNumber").toString();
            Map<String, Object> response = new HashMap<>();
            String classCode = functionsAttendanceService.extractClassCode(qrCode);
            if (!redisService.isQRAttendanceCodeValid(classCode,qrCode)) {
                response.put("status", "E");
                response.put("message", "Invalid QR code or class code.");
                return ResponseEntity.status(404).body(response);
            }
            if (!functionsAttendanceService.verifyDigest(qrCode,
                                                        digest,
                                                        redisService.getHmacKey(classCode, registerNumber))){
                response.put("status", "E");
                response.put("message", "HMAC verification failed. Invalid passcode.");
                return ResponseEntity.status(403).body(response);
            }
            redisService.markStudentVerified(classCode,registerNumber);
            redisService.bumpVersionDebounced(classCode);
            response.put("status", "S");
            response.put("message", "Attendance verified and marked for class-"+classCode);
            return ResponseEntity.ok(response);

        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    /**
     * Faculty generates a single passcode for attendance.
     * @param authorizationHeader JWT token in the header
     * @param subCode Optional substitution code
     * @param classCode Class code for which to generate the passcode
     * @return ResponseEntity with status, message, and generated passcode
     * @throws Exception if any error occurs
     */
    @PostMapping("/faculty/passcode/generateCode")
    public ResponseEntity<Map<String,Object>> generateCode(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                             String authorizationHeader,
                                                             @RequestParam(required = false) String subCode,
                                                             @RequestParam String classCode) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            if (!classDB.classExists(classCode)) {
                response.put("status", "E");
                response.put("message", "Class code does not exist.");
                return ResponseEntity.status(404).body(response);
            }
            if (!functionsAttendanceService.isAuthorizedViaSubcodeOrEmail(classCode, (String) claims.get("email"), subCode)) {
                response.put("status", "E");
                response.put("message", "Unauthorized access. Invalid substitution code or email.");
                return ResponseEntity.status(403).body(response);
            }
            if (redisService.isAttendanceTrackingActive(classCode)){
                response.put("status", "NA");
                response.put("message", "Attendance tracking is already active for this class.");
                List<Object> codeSingle = new ArrayList<>(redisService.getSingleAttendanceCodes(classCode));
                if (codeSingle.isEmpty()){
                    functionsAttendanceService.CloseAttendanceWithoutSaving(classCode);
                    String passcode = functionsAttendanceService.initialiseSingleCodeAttendanceAndReturnCode(classCode);

                    response.put("status","S");
                    response.put("message", "QR Event closed and new passcode generated.");
                    response.put("codes", passcode);

                    redisService.storeSingleAttendanceCode(classCode, passcode);

                    return ResponseEntity.ok(response);
                }
                response.put("codes", codeSingle.get(0));
                return ResponseEntity.ok(response);
            }
            //generate codes for attendance and initialise redis databases
            String passcode = functionsAttendanceService.initialiseSingleCodeAttendanceAndReturnCode(classCode);

            response.put("status","S");
            response.put("message", "Passcode generated successfully.");
            response.put("codes", passcode);

            redisService.storeSingleAttendanceCode(classCode, passcode);

            return ResponseEntity.ok(response);


        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    /**
     * Student submits passcode and digest for attendance verification.
     * @param authorizationHeader JWT token in the header
     * @param digest HMAC digest of the passcode
     * @param passcode Passcode submitted by the student
     * @return ResponseEntity with status and message
     * @throws Exception if any error occurs
     */
    @PostMapping("/student/passcode/sendCode")
    public ResponseEntity<Map<String,Object>> sendPasscode(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                       String authorizationHeader,
                                                       @RequestParam String digest,
                                                       @RequestParam String passcode
    ) throws Exception {
        Map<String, Object> claims = functionsStudentsService.checkJwtAuthAfterLoginStudent(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            String registerNumber = claims.get("registerNumber").toString();
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            String classCode = functionsAttendanceService.extractClassCodeFromRedis(passcode);
            if (!redisService.isSingleAttendanceCodeValid(classCode,passcode)) {
                response.put("status", "E");
                response.put("message", "Invalid passcode code.");
                return ResponseEntity.status(404).body(response);
            }
            if (!functionsAttendanceService.verifyDigest(passcode,
                    digest,
                    redisService.getHmacKey(classCode, registerNumber))){
                response.put("status", "E");
                response.put("message", "HMAC verification failed. Invalid passcode.");
                return ResponseEntity.status(403).body(response);
            }
            redisService.markStudentVerified(classCode,registerNumber);
            redisService.bumpVersionDebounced(classCode);
            response.put("status", "S");
            response.put("message", "Attendance verified and marked for class-"+classCode);
            return ResponseEntity.ok(response);

        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    /**
     * Fetch live attendance data for a class with versioning.
     * @param authorizationHeader JWT token in the header
     * @param classCode Class code for which to fetch attendance
     * @param version Optional version parameter to check for updates
     * @return ResponseEntity with status, version, verified count, and attendance record
     * @throws Exception if any error occurs
     */
    @GetMapping("/faculty/liveAttendanceViewWithVersion")
    public ResponseEntity<Map<String,Object>> liveAttendanceWebhook(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                             String authorizationHeader,
                                                             @RequestParam String classCode,
                                                                    @RequestParam(required = false) String version ) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            String currentVersion = redisService.getVersion(classCode);
            if (!redisService.isAttendanceTrackingActive(classCode)){
                response.put("status", "NA");
                response.put("message", "Attendance tracking is not active for this class.");
                return ResponseEntity.ok(response);
            }
            if (version!=null && version.equals(currentVersion)){
                response.put("status", "NA");
                response.put("message", "No new attendance updates.");
                return ResponseEntity.ok(response);
            }
            long getVerifiedCount = redisService.getVerifiedStudentCount(classCode);
            Map<String, Object> nameDetails = redisService.getStudentNameFromRedis(classCode,
                                                                        redisService.getVerifiedStudents(classCode));
            response.put("status", "S");
            response.put("version", currentVersion);
            response.put("verifiedCount", getVerifiedCount);
            response.put("attendanceRecord", nameDetails);
            response.put("message", "Live attendance data fetched successfully.");
            return ResponseEntity.ok(response);
        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    /**
     * Confirm attendance and close the session.
     * @param authorizationHeader JWT token in the header
     * @param classCode Class code for which to confirm attendance
     * @param subCode Optional substitution code
     * @return ResponseEntity with status and message
     * @throws Exception if any error occurs
     */
    @PostMapping("/faculty/qrpasscode/confirmAttendanceClose")
    public ResponseEntity<Map<String,Object>> confirmAttendanceClose(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                                    String authorizationHeader,
                                                                    @RequestParam String classCode,
                                                                     @RequestParam (required = false) String subCode) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();

            if (!functionsAttendanceService.isAuthorizedViaSubcodeOrEmail(classCode, (String) claims.get("email"), subCode)) {
                response.put("status", "E");
                response.put("message", "Unauthorized access. Invalid substitution code or email.");
                return ResponseEntity.status(403).body(response);
            }

            if (!redisService.isAttendanceTrackingActive(classCode)) {
                response.put("status", "NA");
                response.put("message", "No new attendance updates.");
                return ResponseEntity.ok(response); // 304
            }

            if (subCode != null) {
                substitutionDBclass.deleteSubstitutionCode(subCode);
            }

            functionsAttendanceService.SaveAttendanceAndClose(classCode);

            response.put("status", "S");
            response.put("message", "Attendance Saved and Closed.");
            return ResponseEntity.ok(response);
        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    //ONLY FOR TESTING PURPOSES
    @GetMapping("/generateDigest")
    public ResponseEntity<Map<String,Object>> generateDigest(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                                    String authorizationHeader,
                                                                    @RequestParam String reg,
                                                                    @RequestParam String code) throws Exception {


            String hmacpasscode = studentDbClass.getHMACPasscode(reg);
            Map<String, Object> response = new HashMap<>();
            response.put("hmacpasscode", hmacpasscode);
            response.put("digest", functionsAttendanceService.createDigest(code, hmacpasscode));
            return ResponseEntity.ok(response);

    }

    @GetMapping("/student/getStudentHmacPasscode")
    public ResponseEntity<Map<String, Object>> getStudentHmacPasscode(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) throws Exception {
        Map<String, Object> claims = functionsStudentsService.checkJwtAuthAfterLoginStudent(authorizationHeader);
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String, Object> response = new HashMap<>();
            String hmacpasscode = studentDbClass.getHMACPasscode(claims.get("registerNumber").toString());
            if (hmacpasscode != null) {
                response.put("status", "S");
                response.put("hmac", hmacpasscode);
                response.put("message", "HMAC Passcode retrieved successfully.");
                return ResponseEntity.ok(response);
            } else {
                response.put("status","E");
                response.put("message","error in retrieving Passcode");
                return ResponseEntity.status(503).body(response);
            }
        } else {
            return ResponseEntity.status(401).body(claims);
        }
    }

    /**
     * Get all student details for a class.
     * @param authorizationHeader JWT token in the header
     * @param subCode Optional substitution code
     * @param classCode Class code for which to fetch student details
     * @return ResponseEntity with status and student details
     * @throws Exception if any error occurs
     */
    @PostMapping("/faculty/getAllStudentDetails")
    public ResponseEntity<Map<String, Object>> getAllStudentDetails(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestParam(required = false) String subCode, @RequestParam String classCode) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String, Object> response = new HashMap<>();
            Map<String, String> result = functionsAttendanceService.getAllStudentDetails((String) classCode, (String) subCode, (String) claims.get("email"));
            if (result != null) {
                response.put("status", "S");
                response.put("details", result);
                response.put("message", "all student details retrieved successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status","E");
                response.put("message","error in retrieving details");
                return ResponseEntity.status(503).body(response);
            }
        } else {
            return ResponseEntity.status(401).body(claims);
        }
    }

    /**
     * Save manual attendance for a class.
     * @param authorizationHeader JWT token in the header
     * @param request Request body containing classCode, present, absent lists
     * @param subCode Optional substitution code
     * @return ResponseEntity with status and message
     * @throws Exception if any error occurs
     */
    @PostMapping("/faculty/saveManualAttendance")
    public ResponseEntity<Map<String,Object>> saveManualAttendance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object> request, @RequestParam (required = false) String subCode) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        String status = (String) claims.get("status");
        if(status.equals("S")){
            Map<String, Object> response = new HashMap<>();
            if(((String)request.get("classCode"))==null||((String)request.get("classCode")).isEmpty()||request.get("present")==null||request.get("absent")==null){
                response.put("status","E");
                response.put("message","input body given is not proper");
                return ResponseEntity.status(400).body(response);
            }
            if((subCode!=null) && subCode.isEmpty()){
                response.put("status","E");
                response.put("message","subcode is an empty string here");
                return ResponseEntity.status(400).body(response);
            }
            Boolean done=functionsAttendanceService.SaveManualAttendance((String)request.get("classCode"),(String)claims.get("email"),subCode,(List<String>)request.get("present"),(List<String>)request.get("absent"));
            if(done){
                if (subCode != null) {
                    substitutionDBclass.deleteSubstitutionCode(subCode);
                }
                response.put("status","S");
                response.put("message","save manual attendance done successfully");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("status","E");
                response.put("message","error while saving manual attendance. Possibly Classcode is not registered for you and/or substitution code is not valid, or register nos list has invalid register nos/duplicates");
                return ResponseEntity.status(503).body(response);
            }

        }
        else{
            return ResponseEntity.status(401).body(claims);
        }

    }








}




