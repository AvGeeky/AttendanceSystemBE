package com.appbuildersinc.attendance.source.Controllers.Attendance;

import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.*;
import com.appbuildersinc.attendance.source.functions.Attendance.FunctionsAttendance;
import com.appbuildersinc.attendance.source.functions.Class.FunctionsClass;
import com.appbuildersinc.attendance.source.functions.Faculty.FunctionsFaculty;
import com.appbuildersinc.attendance.source.functions.Students.FunctionsStudents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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





    @Autowired
    public ControllerAttendance(FunctionsAttendance fa, FunctionsFaculty functionsFacultyService, FacultyDB userdbutil, ClassDB classDB, FacultyJwtUtil jwtutil, KeyPairUtil keyutil, StudentjwtUtil stdjwtutil, StudentDB studdb, SuperAdminjwtUtil adminutil, SuperAdminDB SuperAdminDbClass, LogicalGroupingDB logicalGroupingDbClass, FunctionsClass functionsClassService, FunctionsStudents functionsStudentsService, SubstitutionDB substitutionDBclass) {
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
                SubstitutionDB.storeSubstitutionCode(subCode, classCode, cleanDate);
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
            String classCode = SubstitutionDB.fetchClassCodeFromSubstitutionCode(substitutionCode);
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



}




