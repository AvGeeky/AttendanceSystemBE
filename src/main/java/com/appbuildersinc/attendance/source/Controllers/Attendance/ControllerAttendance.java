package com.appbuildersinc.attendance.source.Controllers.Attendance;

import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
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


 * <li><b>ROLES DEFINITION FOR JWT CLAIMS

 * The following roles are used throughout the application to define user access
 * and permissions within the JWT claims structure:
 * <ul>
 *   <li><b>FACULTY</b> &ndash;</b> Standard faculty member</li>
 *   <li><b>ADDITIONAL ROLE (addnl_role)</b></li>
 *   <li><b>CLASS_ADVISOR (C)</b> &ndash;</b> Faculty member and serving as a Class Advisor</li>
 *   <li><b>MENTOR (M) &ndash;</b> Faculty member and serving as a Mentor</li>
 *   <li><b>BOTH (CM) </b> Faculty member and serving as a Mentor & Class Advisor</li>
 *   <li><b>STUDENT</b>   &ndash;</b> Student user</li>
 * </ul>
 * <p>
 * <li><b>These roles are critical for authorization logic and should be kept in sync
 * with the application's access control policies.
 * </p>
 */

/*
public ResponseEntity<Map<String,Object>> updateMenteeList(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                         String authorizationHeader,
                                                         @RequestBody Map<String, Object> requestBody) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();


        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }
*/

//ONLY JWT, AUTHENTICATION AND RETURNING VALUES HERE. CALL functionsService FOR BUSINESS LOGIC!!
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


    @Autowired
    public ControllerAttendance(FunctionsAttendance fa, FunctionsFaculty functionsFacultyService, FacultyDB userdbutil, ClassDB classDB, FacultyJwtUtil jwtutil, KeyPairUtil keyutil, StudentjwtUtil stdjwtutil, StudentDB studdb, SuperAdminjwtUtil adminutil, SuperAdminDB SuperAdminDbClass, LogicalGroupingDB logicalGroupingDbClass, FunctionsClass functionsClassService, FunctionsStudents functionsStudentsService, SubstitutionDB substitutionDBclass, RedisService redisService) {
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
    }

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
                response.put("status", "NA");
                response.put("message", "Attendance tracking is already active for this class.");
                response.put("codes", redisService.getThreeQRCodes(classCode));
                return ResponseEntity.ok(response);
            }
            //generate QR codes for attendance and initialise redis databases
            List<String> codesForQr = functionsAttendanceService.initialiseQRAttendanceAndReturnCodes(classCode);

            response.put("status","S");
            response.put("message", "QR codes generated successfully.");
            response.put("codes", codesForQr);

            redisService.storeQRAttendanceCodesWithWindow(classCode, codesForQr);

            return ResponseEntity.ok(response);


        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

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
                response.put("codes", redisService.getSingleAttendanceCodes(classCode));
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
            String classCode = functionsAttendanceService.extractClassCode(passcode);
            if (!redisService.isSingleAttendanceCodeValid(classCode,passcode)) {
                response.put("status", "E");
                response.put("message", "Invalid passcode code or class code.");
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
                response.put("message", "all student details retrieving successfully");
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
    @PostMapping("/faculty/saveManualAttendance")
    public ResponseEntity<Map<String,Object>> saveManualAttendance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object> request) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        String status = (String) claims.get("status");
        if(status.equals("S")){
            Map<String, Object> response = new HashMap<>();
            Boolean done=functionsAttendanceService.SaveManualAttendance((String)request.get("classCode"),(String)claims.get("email"),(String)request.get("subCode"),(List<String>)request.get("present"),(List<String>)request.get("absent"));
            if(done){
                if ((String)request.get("subCode") != null) {
                    substitutionDBclass.deleteSubstitutionCode((String)request.get("subCode"));
                }
                response.put("status","S");
                response.put("message","save manual attendance done successfully");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("status","E");
                response.put("message","error while saving manual attendance");
                return ResponseEntity.status(503).body(response);
            }

        }
        else{
            return ResponseEntity.status(401).body(claims);
        }

    }






}




