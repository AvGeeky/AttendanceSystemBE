package com.appbuildersinc.attendance.source.Controllers.SuperAdmin;

import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.PasswordUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.jsonVerifier.JsonVerifier;
import com.appbuildersinc.attendance.source.database.MongoDB.LogicalGroupingDB;
import com.appbuildersinc.attendance.source.database.MongoDB.StudentDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SuperAdminDB;
import com.appbuildersinc.attendance.source.database.MongoDB.FacultyDB;
import com.appbuildersinc.attendance.source.functions.Class.FunctionsClass;
import com.appbuildersinc.attendance.source.functions.LogicalGrouping.FunctionsLogicalGrouping;
import com.appbuildersinc.attendance.source.functions.SuperAdmin.FunctionsSuperAdmin;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
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
 *
    <B>Controller Endpoints Overview</B>
    <ul>
        <li><b>/test/genHash</b> - Returns hashed password as plain text. Used for testing password hashing.</li>
        <li><b>/SuperAdmin/login</b> - Returns JSON with status, message, name, dept, and JWT token. Used for Super Admin login.</li>
        <li><b>/SuperAdmin/addStudents</b> - Returns JSON with status and message. Adds multiple students; requires JWT.</li>
        <li><b>/SuperAdmin/viewAllStudents</b> - Returns JSON with status, message, and student details list. Views all students; requires JWT.</li>
        <li><b>/SuperAdmin/createOrEditLogicalGrouping</b> - Returns JSON with status and message. Creates/edits logical groupings; requires JWT.</li>
        <li><b>/SuperAdmin/viewAllGroupings</b> - Returns JSON with status, message, and groupings list. Views all logical groupings; requires JWT.</li>
        <li><b>/SuperAdmin/deleteGrouping</b> - Returns JSON with status and message. Deletes a logical grouping; requires JWT.</li>
        <li><b>/SuperAdmin/viewAllTeachers</b> - Returns JSON with status, message, and teacher details list. Views all teachers; requires JWT.</li>
        <li><b>/SuperAdmin/addOrUpdateTeacher</b> - Returns JSON with status and message. Adds or updates a teacher; requires JWT.</li>
        <li><b>/SuperAdmin/deleteTeacher</b> - Returns JSON with status and message. Deletes a teacher; requires JWT.</li>
        <li><b>/SuperAdmin/deleteStudents</b> - Returns JSON with status and message. Deletes students; requires JWT.</li>
    </ul>
    */

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
    private final JsonVerifier  jsonverifier;
    private final RedisTemplate<String, String> redisTemplate;
    @Autowired
    public ControllerSuperAdmin(FunctionsLogicalGrouping functionsLogicalGroupingService, FunctionsSuperAdmin fsa, FunctionsClass functionsClassService, FacultyDB userdbutil, FacultyJwtUtil jwtutil, KeyPairUtil keyutil, StudentjwtUtil stdjwtutil, StudentDB studdb, SuperAdminjwtUtil adminutil, SuperAdminDB SuperAdminDbClass, LogicalGroupingDB logicalGroupingDbClass, JsonVerifier jsonverifier, RedisTemplate<String, String> redisTemplate) {
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
        this.jsonverifier = jsonverifier;
        this.redisTemplate = redisTemplate;
    }



    @GetMapping("/test/genHash")
    public ResponseEntity<String> generateHash(@RequestParam String password) {
        return ResponseEntity.ok((PasswordUtil.hashPassword(password)));
    }

    /**
     * Endpoint: `/SuperAdmin/login`
     * Allows the Super Admin to log in using their email and password.
     * Expects a JSON body with `email` and `password` fields.
     * Returns a JWT token and user details upon successful authentication.
     *
     * @param credentials A map containing `email` and `password` keys.
     * @return ResponseEntity with status, message, user details, and JWT token if login is successful.
     */
    @PostMapping("/SuperAdmin/login")
    public ResponseEntity<Map<String,Object>> adminlogin(@RequestBody Map<String,Object> credentials) {
        Map <String,Object> response=new HashMap<>();
        Map<String,Object> bodycheck = jsonverifier.jsonbodycheck(List.of("email","password"),credentials);
        if(((String)bodycheck.get("status")).equals("E")){
            return ResponseEntity.status(400).body(bodycheck);
        }
        String email=(String)credentials.get("email");
        String password=(String)credentials.get("password");
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
                response.put("message","inserted all new students and their details successfully");
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
    @PostMapping("/SuperAdmin/viewAllStudents")
    public ResponseEntity<Map<String,Object>> viewStudents(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object> request) throws Exception {
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");

        if(status.equals("S")) {
            List<Map<String, Object>> list;
            Map<String, Object> response = new HashMap<>();
            List<String> depts = (List<String>) request.get("depts");
            list = studentDbClass.getListOfAllStudentDetails(depts);
            if (list != null) {
                for (Map<String, Object> l : list) {
                    l.remove("_id");
                    l.remove("hmacpasscode");
                }
                response.put("status", "S");
                response.put("details", list);
                response.put("message", "student details retrieved sucessfuly");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("status","E");
                response.put("message","department list is null or empty");
                return ResponseEntity.status(503).body(response);
            }

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
            Map<String,Object> bodycheck =jsonverifier.jsonbodycheck(List.of("degree","class-code", "passout","section","registernumbers","timetable"),group);
            if(((String)bodycheck.get("status")).equals("E")){
                return ResponseEntity.status(400).body(bodycheck);
            }
            boolean done=functionsLogicalGroupingService.insertLogicalGrouping(group,(String)claims.get("dept"),(String)claims.get("email"));
            if(done){
                response.put("status","S");
                response.put("message","logical grouping inserted or updated successfully!");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("status","E");
                response.put("message","logical grouping not inserted or updated successfully. Either all class codes are not present in the timetable/no changes made/advisor or students not present in DB.");
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
    @DeleteMapping("/SuperAdmin/deleteGrouping")
    public ResponseEntity<Map<String,Object>> deletegrouping(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object> groupid) throws Exception {
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");
        if(status.equals("S")){
            Map<String,Object> response=new HashMap<>();
            Map<String,Object> requestbody=jsonverifier.jsonbodycheck(List.of("groupid"),groupid);
            if(((String)requestbody.get("status")).equals("E")){
                return ResponseEntity.status(400).body(requestbody);
            }
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

    /**
     * <b>View All Teachers Endpoint for Super Admin</b>
     * <p>
     * This endpoint allows a Super Admin to view all teachers in the system.
     * The request must include a valid JWT in the Authorization header.
     *
     * @param authorizationHeader The JWT token for Super Admin authentication, passed in the Authorization header.
     * @return A response entity with status 'S' and a list of teacher details if successful,
     *         or status 'E' and an error message if the operation fails.
     * @throws Exception If an error occurs during the process.
     */
    @GetMapping("/SuperAdmin/viewAllTeachers")
    public ResponseEntity<Map<String,Object>> viewAllTeachers(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestParam String department) throws Exception{
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");
        if(status.equals("S")){
            Map<String,Object> response=new HashMap<>();

            //System.out.println(department);
            List<Map<String,Object>> teacherlist= userdbclass.viewAllTeachers(department);
            //System.out.println("Teacher list: " + teacherlist);

            if(teacherlist!=null && !(teacherlist.isEmpty())) {
                response.put("status", "S");
                response.put("details", teacherlist);
                response.put("message", "Faculty details retrieved succesfully!");
                return ResponseEntity.ok(response);
            }

            else{
                response.put("status","E");
                response.put("message","department would have passed as null or no such dept exisits ");
                return ResponseEntity.status(503).body(response);
            }

        }
        else{
            return ResponseEntity.status(401).body(claims);

        }
    }

    /**
     * <b>Add or Update Teacher Endpoint for Super Admin</b>
     * <p>
     * This endpoint allows a Super Admin to add or update teacher details.
     * The request must include a valid JWT in the Authorization header.
     * The teacher details should be provided in the request body as a map.
     *
     * @param authorizationHeader The JWT token for Super Admin authentication, passed in the Authorization header.
     * @param faculty A map containing the details of the teacher to add or update.
     * @return A response entity with status 'S' and a success message if the operation is successful,
     *         or status 'E' and an error message if the operation fails.
     * @throws Exception If an error occurs during the process.
     */
   @PostMapping("/SuperAdmin/addOrUpdateTeacher")
    public ResponseEntity<Map<String,Object>> addOrUpdateFaculty(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object>faculty) throws Exception {
       Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
       String status=(String)claims.get("status");
       if(status.equals("S")){
           Map<String,Object> response=new HashMap<>();
           String dept=(String)claims.get("dept");
           Map<String,Object>bodycheck=jsonverifier.jsonbodycheck(List.of("email", "position", "name", "mentor"), faculty);
           if(((String)bodycheck.get("status")).equals("E")){
               return ResponseEntity.status(400).body(bodycheck);
           }
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

   /**
    * <b>Delete Teacher Endpoint for Super Admin</b>
    * <p>
    * This endpoint allows a Super Admin to delete a teacher from the system.
    * The request must include a valid JWT in the Authorization header.
    * The teacher's email should be provided in the request body.
    *
    * @param authorizationHeader The JWT token for Super Admin authentication, passed in the Authorization header.
    * @param request A map containing the email of the teacher to be deleted.
    * @return A response entity with status 'S' and a success message if the operation is successful,
    *         or status 'E' and an error message if the operation fails.
    * @throws Exception If an error occurs during the process.
    */
   @DeleteMapping("/SuperAdmin/deleteTeacher")
    public ResponseEntity<Map<String,Object>> deleteFaculty(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object> request) throws Exception {
       Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
       String status=(String)claims.get("status");
       if(status.equals("S")){
           Map<String,Object> response=new HashMap<>();
           Map<String,Object> bodycheck=jsonverifier.jsonbodycheck(List.of("email"),request);
           if(((String)bodycheck.get("status")).equals("E")){
               return ResponseEntity.status(400).body(bodycheck);
           }
           boolean done=functionsSuperAdminService.deleteTeacher((String)request.get("email"));

           if(done){
               response.put("status","S");
               response.put("message","Teacher deleted successfully");
               return ResponseEntity.ok(response);
           }
           else{
               response.put("status","E");
               response.put("message","Teacher Not deleted. Class advisor and faculty registered classes not empty or some other error occurred");
               return ResponseEntity.status(503).body(response);
           }
       }
       else{
           return ResponseEntity.status(401).body(claims);
       }
   }

    /**
     * <b>Delete Students Endpoint for Super Admin</b>
     * @param authorizationHeader
     * @param request
     * @return A response entity with status 'S' and a success message if the operation is successful,
     * @throws Exception
     */
    @DeleteMapping("/SuperAdmin/deleteStudents")
    public ResponseEntity<Map<String,Object>> deleteStudents(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,@RequestBody Map<String,Object> request) throws Exception {
        Map<String,Object> claims=functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status=(String)claims.get("status");
        if(status.equals("S")){
            Map<String,Object> response =new HashMap<>();
            Map<String,Object> bodycheck=jsonverifier.jsonbodycheck(List.of("registernumbers"),request);
            if(((String)bodycheck.get("status")).equals("E")){
                return ResponseEntity.status(400).body(bodycheck);
            }
            Boolean done=functionsSuperAdminService.deleteStudent((List<String>)request.get("registernumbers"));
            if(done){
                response.put("status","S");
                response.put("message","Student details deleted successfully");
                return ResponseEntity.ok(response);
            }
            else{
                response.put("status","E");
                response.put("message","Student details not deleted successfully. Check register numbers or some other error occurred");
                return ResponseEntity.status(503).body(response);
            }
        }
        else{
            return ResponseEntity.status(401).body(claims);
        }

    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }


    @GetMapping("/SuperAdmin/health")
    public ResponseEntity<Map<String, Object>> readiness(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) throws Exception {

        Map<String, Object> claims = functionsSuperAdminService.checkJwtAuthAfterLoginAdmin(authorizationHeader);
        String status = (String) claims.get("status");

        if (!"S".equals(status)) {
            return ResponseEntity.status(401).body(claims);
        }

        Map<String, Object> response = new HashMap<>();

        try {
            // Mongo check
            String mongoStatus;
            String uri = System.getenv("API_KEY"); // Replace with actual env key name if needed
            try (MongoClient mongoClient = MongoClients.create(
                    MongoClientSettings.builder()
                            .applyConnectionString(new ConnectionString(uri))
                            .build())) {

                // Use any existing database name; "admin" is often a safe choice
                MongoDatabase db = mongoClient.getDatabase("admin");
                Document result = db.runCommand(new Document("ping", 1));
                mongoStatus = "UP";

            } catch (Exception e) {
                mongoStatus = "DOWN: " + e.getMessage();
            }

            // Redis check

            String redisStatus;
            String sampleKey = "attendance:codes:SAMPLECLASS";
            String sampleKeyTTL;
            try {
                redisTemplate.opsForValue().set("healthcheck", "ok", Duration.ofSeconds(2));
                redisStatus = "UP";
                Long ttl = redisTemplate.getExpire(sampleKey);
                sampleKeyTTL = (ttl != null ? ttl + "s" : "key not found");
            } catch (Exception e) {
                redisStatus = "DOWN: " + e.getMessage();
                sampleKeyTTL = "N/A";
            }

            // System time
            String serverTime = Instant.now().toString();

            // Memory check
            Runtime runtime = Runtime.getRuntime();
            long free = runtime.freeMemory();
            long total = runtime.totalMemory();
            long used = total - free;
            long max = runtime.maxMemory();

            Map<String, Object> memoryStats = new HashMap<>();
            memoryStats.put("usedMB", used / (1024 * 1024));
            memoryStats.put("freeMB", free / (1024 * 1024));
            memoryStats.put("totalMB", total / (1024 * 1024));
            memoryStats.put("maxMB", max / (1024 * 1024));

            // Final response
            response.put("status", "S");
            response.put("message", "Admin health diagnostics retrieved successfully.");
            response.put("mongo", mongoStatus);
            response.put("redis", redisStatus);
            response.put("serverTime", serverTime);
            response.put("sampleQRCodeTTL", sampleKeyTTL);
            response.put("memory", memoryStats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "E");
            response.put("message", "Unexpected error in admin health diagnostics: " + e.getMessage());
            return ResponseEntity.status(503).body(response);
        }
    }



}




