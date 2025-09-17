package com.appbuildersinc.attendance.source.Controllers.Faculty;

import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.jsonVerifier.JsonVerifier;
import com.appbuildersinc.attendance.source.database.MongoDB.*;
import com.appbuildersinc.attendance.source.functions.Attendance.FunctionsAttendance;
import com.appbuildersinc.attendance.source.functions.Faculty.FunctionsFaculty;
import com.appbuildersinc.attendance.source.functions.Class.FunctionsClass;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <b>Standard HTTP Error Response Codes:</b>
 * <ul>
 *    <b>STATUS 'E' FOR ALL ERRORS. STATUS 'S' FOR ALL SUCCESS</b>
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
 *   <li><b>CLASS_ADVISOR (A)</b> &ndash;</b> Faculty member and serving as a Class Advisor</li>
 *   <li><b>MENTOR (M) &ndash;</b> Faculty member and serving as a Mentor</li>
 *   <li><b>BOTH (MA) </b> Faculty member and serving as a Mentor & Class Advisor</li>
 *   <li><b>STUDENT</b>   &ndash;</b> Student user</li>
 * </ul>
 * <p>
 * <li><b>These roles are critical for authorization logic and should be kept in sync
 * with the application's access control policies.
 * </p>

    * <b>ControllerFaculty</b> is a Spring REST controller that handles HTTP requests related to faculty operations.
 * <b>Endpoints:</b>
 * <ul>
 *   <li><b>GET /faculty/refreshTimetable</b> - Input: JWT in header, body: { } - Returns merged timetable for faculty after JWT validation.</li>
 *   <li><b>POST /faculty/createOrUpdateClass</b> - Input: JWT in header, body: {name, classCode, groupCode, credits} - Creates or updates a class for faculty.</li>
 *   <li><b>POST /faculty/dropClass</b> - Input: JWT in header, body: {classCode, groupCode} - Drops a class assigned to faculty.</li>
 *   <li><b>POST /faculty/transferClass</b> - Input: JWT in header, body: {classCode, groupCode, newFacEmail} - Transfers a class to another faculty.</li>
 *   <li><b>GET /faculty/getClassDetails</b> - Input: JWT in header, body: {classCode, groupCode} - Returns details of a specific class.</li>
 *   <li><b>GET /faculty/getAllLogicalGroupings</b> - Input: JWT in header - Returns all logical groupings for faculty's department.</li>
 *   <li><b>POST /faculty/updateMenteeListAndReturnDetails</b> - Input: JWT in header, body: {mentee_list, reset} - Updates mentee list and returns details.</li>
 *   <li><b>POST /faculty/setEmail</b> - Input: email param - Sends OTP to faculty email and returns JWT with OTP claim.</li>
 *   <li><b>POST /faculty/verifyOtp</b> - Input: JWT in header, otp param - Verifies OTP and returns new JWT if successful.</li>
 *   <li><b>POST /faculty/updatePassword</b> - Input: JWT in header, password param - Updates faculty password after OTP verification.</li>
 *   <li><b>POST /faculty/login</b> - Input: email, password params - Authenticates faculty and returns JWT and user details.</li>
 *   <li><b>GET /faculty/getDetails</b> - Input: JWT in header - Returns faculty user details after authentication.</li>
 *   <li><b>POST /faculty/setDetails</b> - Input: JWT in header, body: {name, department, position, mentor} - Updates faculty profile details.</li>
 *   <li><b>GET /faculty/getMentorListAttendance</b> - Input: JWT in header - Returns attendance for mentees if faculty is a mentor.</li>
 *   <li><b>GET /faculty/getAdvisorListAttendance</b> - Input: JWT in header - Returns attendance for advisees if faculty is an advisor.</li>
 *   <li><b>GET /faculty/getStudentAttendanceByClassCode</b> - Input: JWT in header, body: {classcode} - Returns student attendance for a class.</li>
 *   <li><b>GET /faculty/getLectureAttendanceByClassCode</b> - Input: JWT in header, body: {classcode} - Returns lecture attendance for a class.</li>
 *   <li><b>POST /faculty/flipAttendance</b> - Input: JWT in header, body: {classcode, registernumber, lecturenumber} - Flips attendance for a student in a lecture.</li>
 * </ul>
 */

@RestController
public class ControllerFaculty {
    private final FunctionsClass functionsMiscService;
    private final FunctionsFaculty functionsFacultyService;
    private final FunctionsClass functionsClassService;
    private final FunctionsAttendance functionsAttendanceService;

    private final FacultyDB userdbclass;
    private final ClassDB classDB;
    private final StudentDB studentDbClass;
    private final SuperAdminDB SuperAdminDbClass;
    private final LogicalGroupingDB logicalGroupingDbClass;

    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil facultyJwtUtil;
    private final StudentjwtUtil studentjwtUtil;
    private final SuperAdminjwtUtil adminjwtUtil;

    private final JsonVerifier jsonverifier;

    @Autowired
    public ControllerFaculty(FunctionsFaculty fs, FunctionsClass functionsMiscService, FacultyDB userdbutil, ClassDB classDB, FacultyJwtUtil jwtutil, KeyPairUtil keyutil, StudentjwtUtil stdjwtutil, StudentDB studdb, SuperAdminjwtUtil adminutil, SuperAdminDB SuperAdminDbClass, LogicalGroupingDB logicalGroupingDbClass, FunctionsClass functionsClassService, FunctionsAttendance functionsAttendanceService, JsonVerifier jsonverifier) {
        this.functionsMiscService = functionsMiscService;
        this.functionsFacultyService = fs;
        this.userdbclass = userdbutil;
        this.classDB = classDB;
        this.facultyJwtUtil = jwtutil;
        this.keyclass = keyutil;
        this.studentjwtUtil = stdjwtutil;
        this.studentDbClass = studdb;
        this.adminjwtUtil = adminutil;
        this.SuperAdminDbClass = SuperAdminDbClass;
        this.logicalGroupingDbClass = logicalGroupingDbClass;
        this.functionsClassService = functionsClassService;
        this.functionsAttendanceService = functionsAttendanceService;
        this.jsonverifier = jsonverifier;
    }

    /**
     * Returns a JWT refresh cookie for the faculty after login.
     * @param authorizationHeader JWT token in the header
     * @return ResponseEntity with status and message
     * @throws Exception if any error occurs
     * This endpoint is used to set a secure HTTP-only cookie for the faculty after successful login.
     */
    @PostMapping("/faculty/returnHTTPRefreshCookie")
    public ResponseEntity<Map<String, Object>> returnHTTPRefreshCookie(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, HttpServletResponse response) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            System.out.println("hi");
            String email = (String) claims.get("email");
            String enc_email = keyclass.encryptString(email);
            Cookie cookie = new Cookie("identity", enc_email);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24 * 300); // 300 days
            cookie.setAttribute("SameSite", "Strict");
            response.addCookie(cookie);
            return ResponseEntity.ok(Map.of("message", "cookie set", "status", "S"));
        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    @PostMapping("/faculty/refreshJWTWithCookie")
    public ResponseEntity<Map<String,Object>> refreshJWTWithCookie(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @CookieValue(value = "identity", required = false) String encryptedIdentity)
            throws Exception {
        Map<String, Object> claims = facultyJwtUtil.parseJwtAllowExpired(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("error");
        if (status != null && status.equalsIgnoreCase("TO")){
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            if (encryptedIdentity == null) {
                response.put("status", "E");
                response.put("message", "No identity cookie found.");
                return ResponseEntity.status(400).body(response);
            }
            String facEmail = claims.get("email").toString();
            String email = keyclass.decryptString(encryptedIdentity);
            if (!email.equals(facEmail)) {
                response.put("status", "E");
                response.put("message", "Email mismatch with cookie.");
                return ResponseEntity.status(403).body(response);
            }
            Map<String, Object> newClaims = facultyJwtUtil.createClaims(email, true, "", false, claims.get("addnl_role").toString(), claims.get("dept").toString());
            String newJwt = facultyJwtUtil.signJwt(newClaims);
            response.put("status", "S");
            response.put("token", newJwt);
            response.put("message", "JWT refreshed successfully.");
            return ResponseEntity.ok(response);
        } else {
            if (status != null && status.equalsIgnoreCase("invalid token")){
                return ResponseEntity.status(400).body(Map.of("status", "E", "message", "Invalid JWT token passed."));
            }
            return ResponseEntity.status(400).body(Map.of("status", "E", "message", "JWT token not expired"));
        }



    }

    @PostMapping("/faculty/refreshJWTWithEncEmail")
    public ResponseEntity<Map<String,Object>> refreshJWTWithEncEmail(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestParam String encEmail)
            throws Exception {
        Map<String, Object> claims = facultyJwtUtil.parseJwtAllowExpired(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("error");
        if (status != null && status.equalsIgnoreCase("TO")){
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            if (encEmail == null) {
                response.put("status", "E");
                response.put("message", "No identity encrypted email found.");
                return ResponseEntity.status(400).body(response);
            }
            String facEmail = claims.get("email").toString();
            // 1. Get the string safely
            encEmail = encEmail.trim()
                    .replace(" ", "+")              // convert spaces back to '+'
                    .replaceAll("[\\r\\n\"]", "");  // remove newlines, quotes

            // 2. Pad if needed
            int mod = encEmail.length() % 4;
            if (mod != 0) {
                encEmail += "=".repeat(4 - mod);
            }
            byte[] decodedBytes = java.util.Base64.getUrlDecoder().decode(encEmail);
            encEmail = new String(decodedBytes, StandardCharsets.UTF_8);
            String email_dept = keyclass.decryptString(encEmail);
            String[] parts = email_dept.split("~");
            if (parts.length != 2) {
                response.put("status", "E");
                response.put("message", "Invalid encrypted email format.");
                return ResponseEntity.status(400).body(response);
            }
            String email = parts[0];
            String dept = parts[1];
            if (!email.equals(facEmail) || !dept.equals(claims.get("dept").toString())) {
                response.put("status", "E");
                response.put("message", "Email mismatch with cookie.");
                return ResponseEntity.status(403).body(response);
            }

            Map<String, Object> newClaims = facultyJwtUtil.createClaims(email, true, "", false, claims.get("addnl_role").toString(), claims.get("dept").toString());
            String newJwt = facultyJwtUtil.signJwt(newClaims);
            response.put("status", "S");
            response.put("token", newJwt);
            response.put("message", "JWT refreshed successfully.");
            return ResponseEntity.ok(response);
        } else {
            if (status != null && status.equalsIgnoreCase("invalid token")){
                return ResponseEntity.status(400).body(Map.of("status", "E", "message", "Invalid JWT token passed."));
            }
            return ResponseEntity.status(400).body(Map.of("status", "E", "message", "JWT token not expired"));
        }



    }



    /**
     * Refreshes the timetable for the faculty by merging their classes.
     * Requires a valid JWT in the Authorization header.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @return A ResponseEntity containing the merged timetable or an error message.
     * @throws Exception If there is an error during processing.
     */
    @GetMapping("/faculty/refreshTimetable")
    public ResponseEntity<Map<String, Object>> refreshTimetable(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                                String authorizationHeader) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> timetable = functionsFacultyService.getMergedTimetable((String) claims.get("email"));
            if (timetable == null || timetable.isEmpty()) {
                response.put("status", "E");
                response.put("message", "Error in fetching timetable. Please try again later.");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            response.put("timetable", timetable);
            response.put("status", "S");
            response.put("message", "Timetable refreshed successfully");
            return ResponseEntity.ok(response);


        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }



    /**
     * Creates or updates a class for the faculty.
     * Requires a valid JWT in the Authorization header and class details in the request body.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param requestBody         The class details in the request body.
     * @return A ResponseEntity containing the status of the operation or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/createOrUpdateClass")
    public ResponseEntity<Map<String, Object>> createClass(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                           String authorizationHeader,
                                                           @RequestBody Map<String, Object> requestBody) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);

        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            String name = (String) requestBody.get("name");
            String dept = (String)claims.get("dept");
            String classCode = (String) requestBody.get("classCode");
            String logicalGroupingCode = (String) requestBody.get("groupCode");
            String credits = (String) requestBody.get("credits");
           Map<String,Object> requestbody=jsonverifier.jsonbodycheck(List.of("groupCode","classCode","name","credits"),requestBody);
           if(((String)requestbody.get("status")).equals("E")){
               return ResponseEntity.status(400).body(requestbody);
           }
            boolean succ = functionsClassService.createNewClass(
                    logicalGroupingCode,
                    classCode,
                    name,
                    dept,
                    claims.get("email").toString(),
                    credits);
            if (succ) {
                response.put("status", "S");
                response.put("message", "Class created / updated successfully!");
                return ResponseEntity.ok(response);
            } else {
                //Error in creating class
                response.put("status", "E");
                response.put("message", "Error in creating / updating class. Make sure you are not creating a duplicate class already taken by another teacher or selecting a classcode not in the logical group.");
                return ResponseEntity.status(503).body(response);
            }

        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }



    /**
     * Drops a class assigned to the faculty.
     * Requires a valid JWT in the Authorization header and class details in the request body.
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param requestBody         The class details in the request body.
     * @return A ResponseEntity containing the status of the operation or an error message.
     * @throws Exception If there is an error during processing.
     */
    @DeleteMapping("/faculty/dropClass")
    public ResponseEntity<Map<String, Object>> dropClass(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                         String authorizationHeader,
                                                         @RequestBody Map<String, Object> requestBody) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);

        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();

            String classCode = (String) requestBody.get("classCode");
            //String logicalGroupingCode = (String) requestBody.get("groupCode");
            Map<String,Object> jsonbody=jsonverifier.jsonbodycheck(List.of("classCode"),requestBody);
            if(((String)jsonbody.get("status")).equals("E")){
                return ResponseEntity.status(400).body(jsonbody);
            }

            boolean succ = functionsClassService.dropClass(classCode);
            if (succ) {
                response.put("status", "S");
                response.put("message", "Class dropped successfully!");
                return ResponseEntity.ok(response);
            } else {
                //Error in creating class
                response.put("status", "E");
                response.put("message", "Error in dropping. Try again later.");
                return ResponseEntity.status(503).body(response);
            }

        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }



    /**
     * Transfers a class to another faculty member.
     * Requires a valid JWT in the Authorization header and class details in the request body.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param requestBody         The class details in the request body.
     * @return A ResponseEntity containing the status of the operation or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/transferClass")
    public ResponseEntity<Map<String, Object>> transferClass(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                             String authorizationHeader,
                                                             @RequestBody Map<String, Object> requestBody) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);

        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            Map<String,Object> resp = jsonverifier.jsonbodycheck(List.of("classCode","newFacEmail"),requestBody);
            if (((String) resp.get("status")).equals("E")) {
                return ResponseEntity.status(400).body(resp);
            }
            String classCode = (String) requestBody.get("classCode");
            String newFacultyEmail = (String) requestBody.get("newFacEmail");

            boolean succ = functionsFacultyService.transferClass(classCode,newFacultyEmail);
            if (succ) {
                response.put("status", "S");
                response.put("message", "Class transferred successfully!");
                return ResponseEntity.ok(response);
            } else {
                //Error in creating class
                response.put("status", "E");
                response.put("message", "Error in transferring. Try again later.");
                return ResponseEntity.status(503).body(response);
            }

        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }



    /**
     * Retrieves details of a specific class for the faculty.
     * Requires a valid JWT in the Authorization header and class details in the request body.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param classCode

     * @return A ResponseEntity containing the class details or an error message.
     * @throws Exception If there is an error during processing.
     */
    @GetMapping("/faculty/getClassDetails")
    public ResponseEntity<Map<String, Object>> getClassDetails(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                               String authorizationHeader,
                                                               @RequestParam String classCode) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);

        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();

            if(classCode.isEmpty()){
                response.put("status","E");
                response.put("message","Empty String classcode");
                return ResponseEntity.status(400).body(response);
            }

            Map<String, Object> details = classDB.getAllClassDetails(classCode);

            if (details != null) {
                details.remove("regnoHMACMap");
                response.put("status", "S");
                response.put("message", "Class fetched successfully!");
                response.put("details", details);
                return ResponseEntity.ok(response);
            } else {
                //Error in creating class
                response.put("status", "E");
                response.put("message", "Error in fetching. Try again later.");
                return ResponseEntity.status(503).body(response);
            }

        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }




    /**
     * Retrieves all logical groupings.
     * Requires a valid JWT in the Authorization header.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @return A ResponseEntity containing the logical groupings or an error message.
     * @throws Exception If there is an error during processing.
     */
    @GetMapping("/faculty/getAllLogicalGroupings")
    public ResponseEntity<Map<String, Object>> getAllLogicalGroupings(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                                      String authorizationHeader,@RequestParam String department) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            if(department.isEmpty()){
                response.put("status","E");
                response.put("message","empty string department input");
                return ResponseEntity.status(400).body(response);
            }
            List<Map<String, Object>> logicalGroupings = functionsFacultyService.getAllLogicalGroupings(department);
            if(!logicalGroupings.isEmpty()) {
                response.put("status", "S");
                response.put("message", "Logical groupings retrieved successfully!");
                response.put("logical_groupings", logicalGroupings);
                return ResponseEntity.ok(response);
            }
            else{
                response.put("status", "E");
                response.put("message","wrong dept or no logical groupings of that dept exist");
                return ResponseEntity.status(503).body(response);
            }
        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }



    /**
     * Updates the mentee list for the faculty and returns the updated details.
     * Requires a valid JWT in the Authorization header and mentee list in the request body.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param requestBody         The request body containing the mentee list and reset flag.
     * @return A ResponseEntity containing the status of the operation and updated mentee details or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/updateMenteeListAndReturnDetails")
    public ResponseEntity<Map<String, Object>> updateMenteeListAndReturnDetails(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                                                String authorizationHeader,
                                                                                @RequestBody Map<String, Object> requestBody) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            if (requestBody.get("mentee_list") == null || !(requestBody.get("mentee_list") instanceof List)||requestBody.get("reset")==null||((String)requestBody.get("reset")).isEmpty()) {
                response.put("status", "E");
                response.put("message", "Invalid mentee list format. Please provide a valid list or reset not given properly");
                return ResponseEntity.status(400).body(response);
            }
            List<String> menteeList = (List<String>) requestBody.get("mentee_list");
            String email = (String) claims.get("email");
            String reset = (String) requestBody.get("reset");
            if (functionsFacultyService.updateMenteeList(email, menteeList, reset)) {
                response.put("status", "S");
                response.put("message", "Mentee list updated successfully!");
                response.put("mentee_list_details", functionsFacultyService.getMenteeListDetails(email));
                return ResponseEntity.ok(response);
            } else {
                //Error in updating mentee list
                response.put("status", "E");
                response.put("message", "This teacher is not a mentor, contact admin / error in updating db. Please try again later.");
                return ResponseEntity.status(400).body(response);
            }


        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }



    /**
     * Sets the email for the faculty and sends an OTP to that email.
     * Requires a valid faculty email
     * @param credentials  The json contiaining faculty email to set.
     * @return A ResponseEntity containing the status of the operation and JWT with OTP claim or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/setEmail")
    public ResponseEntity<Map<String, Object>> setEmail(@RequestBody Map<String,Object> credentials) throws Exception {
        Map<String,Object> bodycheck = jsonverifier.jsonbodycheck(List.of("email"),credentials);
        if(((String)bodycheck.get("status")).equals("E")){
            return ResponseEntity.status(400).body(bodycheck);
        }
        String email=(String)credentials.get("email");
        Map<String, Object> response = new HashMap<>();
        if (functionsFacultyService.isEmailAllowed(email)) {
            String enc_otp = functionsFacultyService.sendMailReturnOtp(email);

            Map<String, Object> claims = facultyJwtUtil.createClaims(email, false, enc_otp, false, "", "");
            String jwt = facultyJwtUtil.signJwt(claims);

            response.put("status", "S");
            response.put("message", "OTP has been successfully sent!");
            response.put("token", jwt);
            return ResponseEntity.ok(response);
        }
        response.put("status", "E");
        response.put("message", "The Email ID is not a Faculty Email ID. Contact Admin.");
        return ResponseEntity.status(401).body(response);
    }




    /**
     * Verifies the OTP sent to the faculty's email.
     * Requires a valid JWT in the Authorization header and OTP as a request parameter.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param credentials                 The OTP to verify.
     * @return A ResponseEntity containing the status of the operation and new JWT if successful or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/verifyOtp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                         @RequestBody Map<String,Object> credentials) throws Exception {

        Map<String, Object> claims = functionsFacultyService.checkJwtAuthBeforeLogin(authorizationHeader);

        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String,Object> bodycheck = jsonverifier.jsonbodycheck(List.of("otp"),credentials);
            if(((String)bodycheck.get("status")).equals("E")){
                return ResponseEntity.status(400).body(bodycheck);
            }
            String otp=(String)credentials.get("otp");
            //JWT is valid, proceed with Business Logic
            String jwt;
            HashMap<String, Object> response = new HashMap<>();

            String enc_otp = (String) claims.get("enc_otp");
            if (enc_otp == null || enc_otp.isEmpty()) {
                response.put("status", "E");
                response.put("message", "Email ID not set yet. Please set email ID first.");
                return ResponseEntity.status(401).body(response);
            }

            int dec_otp = Integer.parseInt(keyclass.decryptString(enc_otp));
            if(otp==null ||otp.isEmpty()){
                response.put("status","E");
                response.put("message","the otp is empty string or null");
                return ResponseEntity.status(400).body(response);
            }
            if (dec_otp == Integer.parseInt(otp)) {
                response.put("status", "S");
                response.put("message", "OTP has been successfully verified!");

                facultyJwtUtil.updateEncOtp(claims, "");
                facultyJwtUtil.updateOtpAuthStatus(claims, true);

                jwt = facultyJwtUtil.signJwt(claims);
                response.put("token", jwt);

                return ResponseEntity.ok(response);
            } else {
                response.put("status", "E");
                response.put("message", "Invalid OTP. Please try again.");
                return ResponseEntity.status(401).body(response);
            }

        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }




    /**
     * Updates the password for the faculty after OTP verification.
     * Requires a valid JWT in the Authorization header and new password as a request parameter.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param credentials            The new password to set.
     * @return A ResponseEntity containing the status of the operation and new JWT if successful or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/updatePassword")
    public ResponseEntity<Map<String, Object>> updatePassword(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                              @RequestBody Map<String,Object> credentials) throws Exception {
        //Check if the JWT is valid
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthBeforeLogin(authorizationHeader);

        String status = (String) claims.get("status");

        if (status.equals("S")) {
            Map<String,Object> bodycheck = jsonverifier.jsonbodycheck(List.of("password"),credentials);
            if(((String)bodycheck.get("status")).equals("E")){
                return ResponseEntity.status(400).body(bodycheck);
            }
            String password=(String)credentials.get("password");
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();

            if ((boolean) claims.get("otp_auth")) {
                //OTP is verified, proceed with setting username and password
                String email = (String) claims.get("email");
                if(password.isEmpty()){
                    response.put("status","E");
                    response.put("message","password given is an empty string");
                    return ResponseEntity.status(400).body(response);
                }
                if (functionsFacultyService.hashAndUpdatePassword(email, password)) {
                    facultyJwtUtil.updateOtpAuthStatus(claims, false);
                    String jwt = facultyJwtUtil.signJwt(claims);
                    response.put("status", "S");
                    response.put("message", "Password has been successfully updated!");
                    response.put("token", jwt);
                    return ResponseEntity.ok(response);
                } else {
                    //Error in updating password

                    response.put("status", "E");
                    response.put("message", "Error in updating password. Please try again.");
                    return ResponseEntity.status(503).body(response);
                }

            } else {
                //OTP is not verified, return error response

                response.put("status", "E");
                response.put("message", "OTP not verified. Please verify OTP first.");
                return ResponseEntity.status(401).body(response);
            }
        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }



    /**
     * Logs in the faculty using email and password.
     * Expects a JSON body with \`email\` and \`password\` fields.
     * If successful, returns a JWT and user details.
     *
     * @param credentials Map containing \`email\` and \`password\` keys.
     * @return A ResponseEntity containing the status of the login operation and user details or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String,Object> credentials) throws Exception {

        Map<String,Object> bodycheck = jsonverifier.jsonbodycheck(List.of("email","password"),credentials);
        if(((String)bodycheck.get("status")).equals("E")){
            return ResponseEntity.status(400).body(bodycheck);
        }
        String email=(String)credentials.get("email");
        String password=(String)credentials.get("password");

        Map<String, Object> response = new HashMap<>();
        if (functionsFacultyService.attemptLogin(email, password)) {

            //Login successful
            response.put("status", "S");
            response.put("message", "Login successful!");


            Map<String, Object> details = userdbclass.getUserDetailsByEmail(email);
            Map<String, Object> claims = facultyJwtUtil.createClaims(email, true, "", false, "", details.get("department").toString());

            if (details.get("name") == null) {
                response.put("status", "FL"); //first login
                response.put("message", "First login. Please set your details by calling necessary endpoint.");
                response.put("token", facultyJwtUtil.signJwt(claims));
                return ResponseEntity.ok(response);
            }

            if (details.get("mentor").equals("True") && details.get("class_advisor").equals("True")) {
                facultyJwtUtil.updateAddnlRole(claims, "CM");
                response.put("role", "MA");
            } else if (details.get("mentor").equals("True")) {
                facultyJwtUtil.updateAddnlRole(claims, "M");
                response.put("role", "M");
            } else if (details.get("class_advisor").equals("True")) {
                facultyJwtUtil.updateAddnlRole(claims, "A");
                response.put("role", "A");
            }
            response.put("name", details.get("name"));
            response.put("department", details.get("department"));
            response.put("email", details.get("faculty_email"));
            response.put("position", details.get("position"));

            String encEmail = keyclass.encryptString(email + "~" + details.get("department").toString());
            encEmail = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(encEmail.getBytes(StandardCharsets.UTF_8));
            response.put("encEmail", encEmail);

            String jwt = facultyJwtUtil.signJwt(claims);
            response.put("token", jwt);
            return ResponseEntity.ok(response);
        } else {
            //Login failed
            response.put("status", "E");
            response.put("message", "Invalid email or password. Please try again.");
            return ResponseEntity.status(401).body(response);
        }


    }




    /**
     * Retrieves the details of the faculty user.
     * Requires a valid JWT in the Authorization header.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @return A ResponseEntity containing the user details or an error message.
     * @throws Exception If there is an error during processing.
     */
    @GetMapping("/faculty/getDetails")
    public ResponseEntity<Map<String, Object>> getDetails(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                          String authorizationHeader) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> details = userdbclass.getUserDetailsByEmail((String) claims.get("email"));
            if (details != null) {
                response.put("status", "S");
                response.put("message", "User details retrieved successfully!");
                details.remove("_id");
                details.remove("password");

                response.put("details", details);
                return ResponseEntity.ok(response);
            } else {
                //Error in retrieving user details
                response.put("status", "E");
                response.put("message", "Error in retrieving user details. Please try again.");
                return ResponseEntity.status(503).body(response);
            }
        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }



    /**
     * Sets the details of the faculty user.
     * Requires a valid JWT in the Authorization header and user details in the request body.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param requestBody         The user details in the request body.
     * @return A ResponseEntity containing the status of the operation or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/setDetails")
    public ResponseEntity<Map<String, Object>> setDetails(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                          String authorizationHeader,
                                                          @RequestBody Map<String, Object> requestBody) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            Map<String,Object> bodycheck=jsonverifier.jsonbodycheck(List.of("name","position"),requestBody);
            if(((String)bodycheck.get("status")).equals("E")){
                return ResponseEntity.status(400).body(bodycheck);
            }
            boolean succ = userdbclass.updateUserDocumentByEmail((String) claims.get("email"),
                    (String) requestBody.get("name"),
                    (String) claims.get("dept"),
                    (String) requestBody.get("position"));

            if (succ) {
                response.put("status", "S");
                response.put("message", "User details updated successfully!");
                return ResponseEntity.ok(response);

            } else {
                //Error in updating user details
                response.put("status", "E");
                response.put("message", "Error in updating user details. Please try again.");
                return ResponseEntity.status(503).body(response);
            }
        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }



    /**
     * Retrieves the list of mentees for the faculty.
     * Requires a valid JWT in the Authorization header.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @return A ResponseEntity containing the list of mentees or an error message.
     * @throws Exception If there is an error during processing.
     */
    @GetMapping("/faculty/getMentorListAttendance")
    public ResponseEntity<Map<String, Object>> getMentorListAttendance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            if (!functionsFacultyService.isFacultyMentor((String) claims.get("email"))){
                Map<String, Object> response = new HashMap<>();
                response.put("status", "NM");
                response.put("message", "this teacher is not a mentor");
                return ResponseEntity.status(400).body(response);
            }
            Map<String, Map<String, Object>> result = functionsFacultyService.getMentorListAttendance((String) claims.get("email"));
            Map<String, Object> response = new HashMap<>();
            if (result != null) {
                response.put("status", "S");
                response.put("details", result);
                response.put("message", "mentees attendance retrieved successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "MLNT");
                response.put("message", "Mentee list not present. Please set the mentee list first.");
                return ResponseEntity.status(400).body(response);
            }


        } else {
            return ResponseEntity.status(401).body(claims);

        }

    }



    /**
     * Retrieves the list of advisors for the faculty.
     * Requires a valid JWT in the Authorization header.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @return A ResponseEntity containing the list of advisors or an error message.
     * @throws Exception If there is an error during processing.
     */
    @GetMapping("/faculty/getAdvisorListAttendance")
    public ResponseEntity<Map<String, Object>> getAdvisorListAttendance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String, List<Map<String, Object>>> result = functionsFacultyService.getAdvisorListAttendance((String) claims.get("email"));
            Map<String, Object> response = new HashMap<>();
            if (result != null) {
                response.put("status", "S");
                response.put("details", result);
                response.put("message", "advisor list attendance retrieved successfully");
                return ResponseEntity.ok(response);

            } else {
                response.put("status", "E");
                response.put("message", "this teacher is not an advisor");
                return ResponseEntity.status(503).body(response);

            }

        } else {
            return ResponseEntity.status(401).body(claims);

        }


    }



    /**
     * Retrieves student attendance by class code for the faculty.
     * Requires a valid JWT in the Authorization header and class code in the request body.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param request             The request body containing the class code.
     * @return A ResponseEntity containing the student attendance details or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/getStudentAttendanceByClassCode")
    public ResponseEntity<Map<String, Object>> getStudentAttendanceByClassCode(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody Map<String, Object> request) throws Exception {

        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String,Object> jsonbody=jsonverifier.jsonbodycheck(List.of("classcode"),request);
            if(((String)jsonbody.get("status")).equals("E")){
                return ResponseEntity.status(400).body(jsonbody);
            }
            Map<String, Map<String, Object>> result = functionsFacultyService.getStudentAttendanceByClassCode((String) claims.get("email"), (String) request.get("classcode"));
            Map<String, Object> response = new HashMap<>();
            if (result != null) {
                response.put("status", "S");
                response.put("details", result);
                response.put("message", "successfully retrieved student attendance by class code. Check faculty-code mapping.");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "E");
                response.put("message", "error in retrieving attendance by class code");
                return ResponseEntity.status(503).body(response);

            }


        } else {
            return ResponseEntity.status(401).body(claims);

        }
    }



    /**
     * Retrieves lecture attendance by class code for the faculty.
     * Requires a valid JWT in the Authorization header and class code in the request body.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param request             The request body containing the class code.
     * @return A ResponseEntity containing the lecture attendance details or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/getLectureAttendanceByClassCode")
    public ResponseEntity<Map<String, Object>> getLectureAttendanceByClassCode(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody Map<String, Object> request) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String,Object> jsonbody=jsonverifier.jsonbodycheck(List.of("classcode"),request);
            if(((String)jsonbody.get("status")).equals("E")){
                return ResponseEntity.status(400).body(jsonbody);
            }
            Map<String, Map<String, Object>> result = functionsFacultyService.getLectureAttendanceByClassCode((String) claims.get("email"), (String) request.get("classcode"));
            Map<String, Object> response = new HashMap<>();
            if (result != null) {
                response.put("status", "S");
                response.put("details", result);
                response.put("message", "attendance details retrieved successfully");
                return ResponseEntity.ok(response);

            } else {

                response.put("status", "E");
                response.put("message", "error in retrieving attendance by class code");
                return ResponseEntity.status(503).body(response);
            }


        } else {
            return ResponseEntity.status(401).body(claims);

        }

    }



    /**
     * Flips the attendance status of a student for a specific lecture.
     * Requires a valid JWT in the Authorization header and class code, register number, and lecture number in the request body.
     *
     * @param authorizationHeader The JWT token in the Authorization header.
     * @param request             The request body containing class code, register number, and lecture number.
     * @return A ResponseEntity containing the status of the operation or an error message.
     * @throws Exception If there is an error during processing.
     */
    @PostMapping("/faculty/flipAttendance")
    public ResponseEntity<Map<String, Object>> flipAttendance(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody Map<String,Object> request) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String, Object> response = new HashMap<>();
            Map<String,Object> jsonbody=jsonverifier.jsonbodycheck(List.of("classcode","registernumber","lecturenumber"),request);
            if(((String)jsonbody.get("status")).equals("E")){
                return ResponseEntity.status(400).body(jsonbody);
            }
            //System.out.println((String)request.get("classcode"));
            Boolean done = functionsFacultyService.flipAttendance((String) request.get("classcode"), (String) claims.get("email"), (String) request.get("registernumber"), (String) request.get("lecturenumber"));
            if (done) {
                response.put("status", "S");
                response.put("message", "flipped the attendance of the student successfully");
                return ResponseEntity.ok(response);

            } else {
                response.put("status", "E");
                response.put("message", "error in updating due to incorrect register-number or class-code or lecture-number");
                return ResponseEntity.status(503).body(response);

            }


        } else {
            return ResponseEntity.status(401).body(claims);
        }

    }





    @PostMapping("/faculty/deleteLecture")
    public ResponseEntity<Map<String, Object>> deleteLecture(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody Map<String,Object> request) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String, Object> response = new HashMap<>();
            Map<String,Object> jsonbody=jsonverifier.jsonbodycheck(List.of("classcode","lecturenumber"),request);
            if(((String)jsonbody.get("status")).equals("E")){
                return ResponseEntity.status(400).body(jsonbody);
            }
            int done = functionsFacultyService.deleteLectureAndShift((String) claims.get("email"),(String) request.get("classcode"), (String) request.get("lecturenumber"));
            if (done==1) {
                response.put("status", "S");
                response.put("message", "Deleted the lecture successfully");
                return ResponseEntity.ok(response);

            } else {
                response.put("status", "E");
                if (done==-1){
                    response.put("message", "Class-code not registered for this faculty");
                    return ResponseEntity.status(503).body(response);
                }
                else if (done==-2){
                    response.put("message", "Lecture number not found in the class");
                    return ResponseEntity.status(503).body(response);
                }
                else {
                    response.put("message", "Error in deleting lecture. Try again later.");
                    return ResponseEntity.status(503).body(response);
                }
            }


        } else {
            return ResponseEntity.status(401).body(claims);
        }

    }
    @PostMapping("/faculty/deletelecture")
    public ResponseEntity<Map<String,Object>> deletelecture(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader, @RequestBody Map<String,Object> request) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String, Object> response = new HashMap<>();
            Map<String,Object> jsonbody=jsonverifier.jsonbodycheck(List.of("classcode","lectureno"),request);
            if(((String)jsonbody.get("status")).equals("E")){
                return ResponseEntity.status(400).body(jsonbody);
            }
            boolean done=functionsFacultyService.deletelecture((String)claims.get("email"),(String)request.get("classcode"),(String)request.get("lectureno"));
            if(done){
                response.put("status", "S");
                response.put("message", "Deleted the lecture successfully");
                return ResponseEntity.ok(response);

            }
            else{
                response.put("message","failed to delete the lecture");
                return ResponseEntity.status(503).body(response);
            }



        }
        else{
            return ResponseEntity.status(401).body(claims);
        }
    }


}





