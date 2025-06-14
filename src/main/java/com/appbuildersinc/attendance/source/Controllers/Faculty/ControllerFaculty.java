package com.appbuildersinc.attendance.source.Controllers.Faculty;

import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.LogicalGroupingDB;
import com.appbuildersinc.attendance.source.database.MongoDB.StudentDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SuperAdminDB;
import com.appbuildersinc.attendance.source.database.MongoDB.FacultyDB;
import com.appbuildersinc.attendance.source.functions.Faculty.FunctionsFaculty;
import com.appbuildersinc.attendance.source.functions.Miscallaneous.FunctionsMisc;
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
public class ControllerFaculty {
    private final FunctionsMisc functionsMiscService;
    private final FunctionsFaculty functionsFacultyService;
    private final FacultyDB userdbclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil facultyJwtUtil;
    private final StudentjwtUtil studentjwtUtil;
    private final SuperAdminjwtUtil adminjwtUtil;
    private final StudentDB studentDbClass;
    private final SuperAdminDB SuperAdminDbClass;
    private final LogicalGroupingDB logicalGroupingDbClass;


    @Autowired
    public ControllerFaculty(FunctionsFaculty fs, FunctionsMisc functionsMiscService, FacultyDB userdbutil, FacultyJwtUtil jwtutil, KeyPairUtil keyutil, StudentjwtUtil stdjwtutil, StudentDB studdb, SuperAdminjwtUtil adminutil, SuperAdminDB SuperAdminDbClass, LogicalGroupingDB logicalGroupingDbClass) {
        this.functionsMiscService = functionsMiscService;
        this.functionsFacultyService = fs;
        this.userdbclass = userdbutil;
        this.facultyJwtUtil = jwtutil;
        this.keyclass =keyutil;
        this.studentjwtUtil = stdjwtutil;
        this.studentDbClass =studdb;
        this.adminjwtUtil=adminutil;
        this.SuperAdminDbClass=SuperAdminDbClass;
        this.logicalGroupingDbClass = logicalGroupingDbClass;

    }

    @PostMapping("/faculty/createOrUpdateClass")
    public ResponseEntity<Map<String,Object>> createClass(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                               String authorizationHeader,
                                                               @RequestBody Map<String, Object> requestBody) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);

        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            String name = (String) requestBody.get("name");
            String dept = (String) claims.get("dept");
            String classCode = (String) requestBody.get("classCode");
            String logicalGroupingCode = (String) requestBody.get("groupCode");
            String credits = (String) requestBody.get("credits");
            if (name == null || name.isEmpty() || classCode == null || classCode.isEmpty() || logicalGroupingCode == null || logicalGroupingCode.isEmpty()) {
                response.put("status", "E");
                response.put("message", "Invalid request body. Please provide valid class details.");
                return ResponseEntity.status(400).body(response);
            }
            boolean succ = functionsFacultyService.createNewClass(
                    logicalGroupingCode,
                    classCode,
                    name,
                    dept,
                    claims.get("email").toString(),
                    credits);
            if (succ) {
                response.put("status", "S");
                response.put("message", "Class created successfully!");
                return ResponseEntity.ok(response);
            } else {
                //Error in creating class
                response.put("status", "E");
                response.put("message", "Error in creating class. Make sure you are not creating a duplicate class already taken by another teacher.");
                return ResponseEntity.status(503).body(response);
            }

        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }






    @GetMapping("/faculty/getAllLogicalGroupings")
    public ResponseEntity<Map<String,Object>> getAllLogicalGroupings(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                               String authorizationHeader) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            List<Map<String,Object>> logicalGroupings = functionsFacultyService.getAllLogicalGroupings(claims.get("dept").toString());
            response.put("status", "S");
            response.put("message", "Logical groupings retrieved successfully!");
            response.put("logical_groupings", logicalGroupings);
            return ResponseEntity.ok(response);
        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    @PostMapping("/faculty/updateMenteeListAndReturnDetails")
    public ResponseEntity<Map<String,Object>> updateMenteeListAndReturnDetails(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                               String authorizationHeader,
                                                               @RequestBody Map<String, Object> requestBody) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            if (requestBody.get("mentee_list") == null || !(requestBody.get("mentee_list") instanceof List)) {
                response.put("status", "E");
                response.put("message", "Invalid mentee list format. Please provide a valid list.");
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
                response.put("message", "Error in updating mentee list. Please try again.");
                return ResponseEntity.status(503).body(response);
            }


        } else {
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    @PostMapping("/faculty/setEmail")
    public ResponseEntity<Map<String,Object>> setEmail(@RequestParam String email) throws Exception {
        Map<String, Object> response = new HashMap<>();
       if (functionsFacultyService.isEmailAllowed(email)){
           String enc_otp = functionsFacultyService.sendMailReturnOtp(email);

           Map<String, Object> claims = facultyJwtUtil.createClaims(email,false,enc_otp,false,"","");
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


    @PostMapping("/faculty/verifyOtp")
    public ResponseEntity<Map<String,Object>> verifyOtp(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                        @RequestParam String otp) throws Exception {

        Map<String, Object> claims = functionsFacultyService.checkJwtAuthBeforeLogin(authorizationHeader);

        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with Business Logic
            String jwt;
            HashMap<String, Object> response = new HashMap<>();

            String enc_otp = (String) claims.get("enc_otp");
            if (enc_otp == null || enc_otp.isEmpty()){
                response.put("status", "E");
                response.put("message", "Email ID not set yet. Please set email ID first.");
                return ResponseEntity.status(401).body(response);
            }

            int dec_otp = Integer.parseInt(keyclass.decryptString(enc_otp));

            if (dec_otp == Integer.parseInt(otp)){
                response.put("status", "S");
                response.put("message", "OTP has been successfully verified!");

                facultyJwtUtil.updateEncOtp(claims, "");
                facultyJwtUtil.updateOtpAuthStatus(claims, true);

                jwt = facultyJwtUtil.signJwt(claims);
                response.put("token", jwt);

                return ResponseEntity.ok(response);
            }
            else {
                response.put("status", "E");
                response.put("message", "Invalid OTP. Please try again.");
                return ResponseEntity.status(401).body(response);
            }

        }
        else{
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }


    @PostMapping("/faculty/updatePassword")
    public ResponseEntity<Map<String,Object>> updatePassword(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                        @RequestParam String password) throws Exception {
        //Check if the JWT is valid
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthBeforeLogin(authorizationHeader);


        String status = (String) claims.get("status");

        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();

            if ((boolean) claims.get("otp_auth")){
                //OTP is verified, proceed with setting username and password
                String email = (String) claims.get("email");
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

            }
            else{
                //OTP is not verified, return error response

                response.put("status", "E");
                response.put("message", "OTP not verified. Please verify OTP first.");
                return ResponseEntity.status(401).body(response);
            }
        }
        else{
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    @PostMapping("/faculty/login")
    public ResponseEntity<Map<String,Object>> login(@RequestParam String email,
                                                    @RequestParam String password
                                                    ) throws Exception {

            Map<String, Object> response = new HashMap<>();
            if (functionsFacultyService.attemptLogin(email,password)) {


                //Login successful
                response.put("status", "S");
                response.put("message", "Login successful!");


                Map<String, Object> details = userdbclass.getUserDetailsByEmail(email);
                Map<String, Object> claims = facultyJwtUtil.createClaims(email,true,"",false,"",details.get("department").toString());

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
                response.put("email",details.get("faculty_email"));


                String jwt = facultyJwtUtil.signJwt(claims);
                response.put("token", jwt);
                return ResponseEntity.ok(response);
            }
            else{
                //Login failed
                response.put("status", "E");
                response.put("message", "Invalid email or password. Please try again.");
                return ResponseEntity.status(401).body(response);
            }


    }


    @GetMapping("/faculty/getDetails")
    public ResponseEntity<Map<String,Object>> getDetails(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                             String authorizationHeader) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            Map<String,Object> details = userdbclass.getUserDetailsByEmail((String) claims.get("email"));
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
        }
        else{
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }

    @PostMapping("/faculty/setDetails")
    public ResponseEntity<Map<String,Object>> setDetails(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                         String authorizationHeader,
                                                         @RequestBody Map<String, Object> requestBody) throws Exception {
        Map<String, Object> claims = functionsFacultyService.checkJwtAuthAfterLoginFaculty(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();

           boolean succ = userdbclass.updateUserDocumentByEmail((String) claims.get("email"),
                   (String) requestBody.get("name"),
                   (String) requestBody.get("department"),
                   (String) requestBody.get("position"),
                   (String) requestBody.get("mentor"));
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
        }
        else{
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }







}




