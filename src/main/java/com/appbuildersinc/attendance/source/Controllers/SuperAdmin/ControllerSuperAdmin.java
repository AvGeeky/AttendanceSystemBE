package com.appbuildersinc.attendance.source.Controllers.SuperAdmin;

import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.PasswordUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.LogicalGroupingDB;
import com.appbuildersinc.attendance.source.database.MongoDB.StudentDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SuperAdminDB;
import com.appbuildersinc.attendance.source.database.MongoDB.FacultyDB;
import com.appbuildersinc.attendance.source.functions.Class.FunctionsClass;
import com.appbuildersinc.attendance.source.functions.LogicalGrouping.FunctionsLogicalGrouping;
import com.appbuildersinc.attendance.source.functions.SuperAdmin.FunctionsSuperAdmin;
import org.springframework.beans.factory.annotation.Autowired;
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
Error handling template for Muraribranch:
Map<String, Object> claims = functionsService.checkJwtAuthAfterLogin(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();


        }
        else{
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
*/

//ONLY JWT, AUTHENTICATION AND RETURNING VALUES HERE. CALL functionsService FOR BUSINESS LOGIC!!
@RestController
public class ControllerSuperAdmin {
    private final FunctionsClass functionsClassService;
    private final FunctionsLogicalGrouping functionsLogicalGroupingService;
    private final FunctionsSuperAdmin functionsSuperAdminService;
    private final FacultyDB userdbclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil facultyJwtUtil;
    private final StudentjwtUtil studentjwtUtil;
    private final SuperAdminjwtUtil adminjwtUtil;
    private final StudentDB studentDbClass;
    private final SuperAdminDB SuperAdminDbClass;
    private final LogicalGroupingDB logicalGroupingDbClass;

    @Autowired
    public ControllerSuperAdmin(FunctionsLogicalGrouping functionsLogicalGroupingService, FunctionsSuperAdmin fsa, FunctionsClass functionsClassService, FacultyDB userdbutil, FacultyJwtUtil jwtutil, KeyPairUtil keyutil, StudentjwtUtil stdjwtutil, StudentDB studdb, SuperAdminjwtUtil adminutil, SuperAdminDB SuperAdminDbClass, LogicalGroupingDB logicalGroupingDbClass) {
        this.functionsClassService = functionsClassService;
        this.functionsLogicalGroupingService= functionsLogicalGroupingService;
        this.functionsSuperAdminService = fsa;
        this.userdbclass = userdbutil;
        this.facultyJwtUtil = jwtutil;
        this.keyclass =keyutil;
        this.studentjwtUtil = stdjwtutil;
        this.studentDbClass =studdb;
        this.adminjwtUtil=adminutil;
        this.SuperAdminDbClass=SuperAdminDbClass;
        this.logicalGroupingDbClass = logicalGroupingDbClass;
    }

    @GetMapping("/test/genHash")
    public ResponseEntity<String> generateHash(@RequestParam String password) throws Exception {
        return ResponseEntity.ok((PasswordUtil.hashPassword(password)));
    }

    /**
     * <b>Login Endpoint for Super Admin</b>
     * <p>
     * This endpoint allows the Super Admin to log in using their email and password.
     * It returns a JWT token upon successful authentication.
     *
     * @param email    The email of the Super Admin.
     * @param password The password of the Super Admin.
     * @return A response entity containing the status, message, and JWT token if login is successful.
     * @throws Exception If an error occurs during the login process.
     */
    @GetMapping("/SuperAdmin/login")
    public ResponseEntity<Map<String,Object>> adminlogin(@RequestParam String email,
                                                    @RequestParam String password
                                                        ) throws Exception {
        Map <String,Object> response=new HashMap();
        if(functionsSuperAdminService.attemptloginadmin(email,password)){
            response.put("status","S");
            response.put("message","Login Successful!");
            Map<String,String> namedeptmap=functionsSuperAdminService.getNameDeptbyEmail(email);
            response.put("dept",namedeptmap.get("Department"));
            response.put("name",namedeptmap.get("Name"));
            Map<String, Object> claims = adminjwtUtil.createClaims(email,namedeptmap.get("Department"),true);
            String jwt=adminjwtUtil.signJwt(claims);
            response.put("token",jwt);
            return ResponseEntity.ok(response);

         }
        else{
            response.put("status", "E");
            response.put("message", "Invalid email or password. Please try again.");
            return ResponseEntity.status(401).body(response);
        }
    }

    /**
     * <b>Add Students Endpoint for Super Admin</b>
     * <p>
     * This endpoint allows a Super Admin to add multiple students to the system.
     * The request must include a valid JWT in the Authorization header.
     * The students' details are provided as a list in the request body.
     *
     * @param authorizationHeader The JWT token for Super Admin authentication, passed in the Authorization header.
     * @param studlist A list of maps, each containing student details to be added.
     * @return A response entity with status 'S' and a success message if students are added successfully,
     *         or status 'E' and an error message if the operation fails.
     * @throws Exception If an error occurs during the process.
     */

    @PostMapping("/SuperAdmin/addStudents")
    public ResponseEntity<Map<String,Object>> addStudents(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody List<Map<String,String>> studlist) throws Exception {
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");
        if(status.equals("S")){
            Map<String,Object> response=new HashMap<>();
            String dept=(String)claims.get("dept");

            if(studentDbClass.insertStudentsByAdmin(studlist,dept)){
                response.put("status","S");
                response.put("message","inserted student details successfully");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("status","E");
                response.put("message","student details not inserted successfully");
                return ResponseEntity.status(503).body(response);
            }
        }
        else{
            return ResponseEntity.status(401).body(claims);
        }

    }
    /**
     * <b>View All Students Endpoint for Super Admin</b>
     * <p>
     * This endpoint allows a Super Admin to view all students in the system.
     * The request must include a valid JWT in the Authorization header.
     *
     * @param authorizationHeader The JWT token for Super Admin authentication, passed in the Authorization header.
     * @return A response entity with status 'S' and a list of student details if successful,
     *         or status 'E' and an error message if the operation fails.
     * @throws Exception If an error occurs during the process.
     */
    @GetMapping("/SuperAdmin/viewAllStudents")
    public ResponseEntity<Map<String,Object>> addStudents(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) throws Exception {
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");

        if(status.equals("S")){
            List<Map<String,Object>> list=new ArrayList<>();
            Map<String,Object> response=new HashMap<>();
            list=studentDbClass.getListOfAllStudentDetails((String)claims.get("dept"));
            for(Map<String,Object> l:list){
                l.remove("_id");
                l.remove("hmacpasscode");
            }
            response.put("status","S");
            response.put("details",list);
            response.put("message","student details retrieved sucessfuly");
            return ResponseEntity.ok(response);
        }
        else{
            return ResponseEntity.status(401).body(claims);
        }

    }

    /**
     * <b>Create or Edit Logical Grouping Endpoint for Super Admin</b>
     * <p>
     * This endpoint allows a Super Admin to create a new logical grouping or edit an existing one.
     * The request must include a valid JWT in the Authorization header for authentication.
     * The logical grouping details should be provided in the request body as a map.
     * </p>
     *
     * @param authorizationHeader The JWT token for Super Admin authentication, passed in the Authorization header.
     * @param group A map containing the details of the logical grouping to create or edit.
     * @return A response entity with status 'S' and a success message if the operation is successful,
     *         or status 'E' and an error message if the operation fails.
     * @throws Exception If an error occurs during the process.
     */
    @PostMapping("/SuperAdmin/createOrEditLogicalGrouping")
    public ResponseEntity<Map<String,Object>> createoreditgrouping(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object> group) throws Exception{
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");
        if(status.equals("S")){
            Map<String,Object> response=new HashMap<>();
            boolean done=functionsLogicalGroupingService.insertLogicalGrouping(group,(String)claims.get("dept"),(String)claims.get("email"));
            if(done){
                response.put("status","S");
                response.put("message","logical grouping inserted or updated successfully!");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("status","E");
                response.put("message","logical grouping not inserted or updated successfully. Either all class codes are not present in the timetable/no changes made or some other error occurred");
                return ResponseEntity.status(503).body(response);
            }



        }

        else{

            return ResponseEntity.status(401).body(claims);

        }

    }
    /**
     * <b>View All Logical Groupings Endpoint for Super Admin</b>
     * <p>
     * This endpoint allows a Super Admin to view all logical groupings in the system.
     * The request must include a valid JWT in the Authorization header.
     *
     * @param authorizationHeader The JWT token for Super Admin authentication, passed in the Authorization header.
     * @return A response entity with status 'S' and a list of logical groupings if successful,
     *         or status 'E' and an error message if the operation fails.
     * @throws Exception If an error occurs during the process.
     */
    @GetMapping("/SuperAdmin/viewAllGroupings")
    public ResponseEntity<Map<String,Object>> viewgrouping(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) throws Exception {
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");
        if(status.equals("S")) {
            String dept=(String)claims.get("dept");
            List<Map<String,Object>> groupings =new ArrayList<>();
            Map<String,Object> response=new HashMap<>();
            groupings=logicalGroupingDbClass.viewalllogicalgroupings(dept);
            response.put("status","S");
            response.put("groups",groupings);
            response.put("message","groupings got succesfully");
            return ResponseEntity.ok(response);

        }
        else{
            return ResponseEntity.status(401).body(claims);
        }
    }
    @PostMapping("/SuperAdmin/deleteGrouping")
    public ResponseEntity<Map<String,Object>> deletegrouping(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object> groupid) throws Exception {
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");
        if(status.equals("S")){
            Map<String,Object> response=new HashMap<>();
            Boolean done=functionsLogicalGroupingService.deleteLogicalGroup((String)claims.get("dept"),(String)groupid.get("groupid"));
            if(done){
                response.put("status","S");
                response.put("message","deleted the grouping successfully and register nos deleted from class advisor if applicable");
               return ResponseEntity.ok(response);
            }
            else{
                response.put("status","E");
                response.put("message","no successful deletion");
                return ResponseEntity.status(503).body(response);


            }
        }
        else{
            return ResponseEntity.status(401).body(claims);
        }
    }
    @GetMapping("/SuperAdmin/viewAllTeachers")
    public ResponseEntity<Map<String,Object>> viewAllTeachers(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) throws Exception{
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");
        if(status.equals("S")){
            Map<String,Object> response=new HashMap<>();
            String dept=(String)claims.get("dept");
            List<Map<String,Object>> teacherlist= userdbclass.viewAllTeachers(dept);
            response.put("status","S");
            response.put("details",teacherlist);
            response.put("message","Faculty details retrieved succesfully!");
            return ResponseEntity.ok(response);
        }
        else{
            return ResponseEntity.status(401).body(claims);

        }
    }
   @PostMapping("/SuperAdmin/addOrUpdateTeacher")
    public ResponseEntity<Map<String,Object>> addOrUpdateFaculty(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object>faculty) throws Exception {
       Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
       String status=(String)claims.get("status");
       if(status.equals("S")){
           Map<String,Object> response=new HashMap<>();
           String dept=(String)claims.get("dept");
           boolean done=userdbclass.addorUpdateTeachers(dept,faculty);
           if(done){
               response.put("Status","S");
               response.put("Message","Faculty details added or updated successfully");
              return  ResponseEntity.ok(response);
           }
           else{
               response.put("Status","E");
               response.put("Message","Faculty details not added or updated successfully");
               return ResponseEntity.status(503).body(response);
           }
       }
       else{
           return ResponseEntity.status(401).body(claims);
       }
   }
}




