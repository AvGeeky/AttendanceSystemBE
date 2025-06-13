package com.appbuildersinc.attendance.source.Controllers.Students;

import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.LogicalGroupingDB;
import com.appbuildersinc.attendance.source.database.MongoDB.StudentDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SuperAdminDB;
import com.appbuildersinc.attendance.source.database.MongoDB.FacultyDB;
import com.appbuildersinc.attendance.source.functions.Miscallaneous.FunctionsMisc;
import com.appbuildersinc.attendance.source.functions.Students.FunctionsStudents;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
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
public class ControllerStudents {
    private final FunctionsMisc functionsMiscService;
    private final FunctionsStudents functionsStudentsService;
    private final FacultyDB userdbclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil facultyJwtUtil;
    private final StudentjwtUtil studentjwtUtil;
    private final SuperAdminjwtUtil adminjwtUtil;
    private final StudentDB studentDbClass;
    private final SuperAdminDB SuperAdminDbClass;
    private final LogicalGroupingDB logicalGroupingDbClass;

    @Autowired
    public ControllerStudents(FunctionsStudents fsu, FunctionsMisc functionsService, FacultyDB userdbutil, FacultyJwtUtil jwtutil, KeyPairUtil keyutil, StudentjwtUtil stdjwtutil, StudentDB studdb, SuperAdminjwtUtil adminutil, SuperAdminDB SuperAdminDbClass, LogicalGroupingDB logicalGroupingDbClass) {
        this.functionsMiscService = functionsService;
        this.functionsStudentsService = fsu;
        this.userdbclass = userdbutil;
        this.facultyJwtUtil = jwtutil;
        this.keyclass =keyutil;
        this.studentjwtUtil = stdjwtutil;
        this.studentDbClass =studdb;
        this.adminjwtUtil=adminutil;
        this.SuperAdminDbClass=SuperAdminDbClass;
        this.logicalGroupingDbClass = logicalGroupingDbClass;
    }


    /**
     * Endpoint for authenticating a student using Google ID token.
     *
     * @param request Map containing the ID token.
     * @return ResponseEntity with status and message.
     * @throws IOException if there is an error during token verification.
     */

    @PostMapping("/student/googleAuth")
    public ResponseEntity<Map<String, Object>> authenticateWithGoogle(@RequestBody Map<String, String> request) throws IOException {
        Map<String, Object> response = new HashMap<>();

        //  Check if ID token is provided
        String idToken = request.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            response.put("status", "E");
            response.put("message", "ID token not provided in request body.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response); // 400
        }

        //  Setup verifier for Google token
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), new JacksonFactory())
                .setAudience(Collections.singletonList(functionsStudentsService.googleClientId)) // Replace with your actual client ID
                .build();

        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (GeneralSecurityException e) {
            response.put("status", "E");
            response.put("message", "Security error while verifying token. Please try again later.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response); // 503
        } catch (IOException e) {
            response.put("status", "E");
            response.put("message", "Network error occurred while verifying token. Please try again.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response); // 503
        }

        //  Token verification failed (e.g. expired, tampered, or wrong audience)
        if (token == null) {
            response.put("status", "E");
            response.put("message", "Invalid or expired Google ID token.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // 401
        }


        //  Token is valid
        GoogleIdToken.Payload payload = token.getPayload();
        String hd = payload.getHostedDomain(); // Get the hosted domain if available

        if (hd == null || !hd.equals("ssn.edu.in")) {
            //  Check if the email domain matches the expected domain
            response.put("status", "E");
            response.put("message", "Unauthorized domain. Please use a valid institutional email.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response); // 401
        }
        String email = payload.getEmail();


        // You can optionally validate the email domain or userId here

        //  Generate internal JWT for session
        Map<String, Object> claims = studentjwtUtil.createClaims(email, true);
        String jwt = studentjwtUtil.signJwt(claims);
        Map<String,Object> details = studentDbClass.getStudentDetailsByEmail(email);
        String hmacPasscode = (String) details.get("hmacpasscode");
        response.put("status", "S");
        response.put("email", email);
        response.put("name", payload.get("name"));
        response.put("hmacpasscode", hmacPasscode);
        response.put("message", "Login successful");
        response.put("token", jwt);
        return ResponseEntity.ok(response); // 200
    }


    /**
     * Endpoint to get student details after authentication.
     *
     * @param authorizationHeader JWT token in the Authorization header.
     * @return ResponseEntity with student details or error message.
     * @throws Exception if there is an error during processing.
     */

    @GetMapping("/student/getDetails")
    public ResponseEntity<Map<String,Object>>getStudentDetails(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                                   String authorizationHeader)throws Exception {

        Map<String, Object> claims = functionsStudentsService.checkJwtAuthAfterLoginStudent(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> details = studentDbClass.getStudentDetailsByEmail((String) claims.get("email"));
            if (details != null) {
                response.put("status", "S");
                response.put("message", "Student details retrieved succesully");
                details.remove("_id");
                details.remove("hmacpasscode");
                response.put("details", details);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "E");
                response.put("message", "Error in retrieving student details or no change between passed and database data. Please try again.");
                return ResponseEntity.status(503).body(response);
            }


        } else {
            return ResponseEntity.status(401).body(claims);
        }
    }








//    @PostMapping("/student/setDetails")
//    public ResponseEntity<Map<String,Object>> setStudentDetails(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
//                                                                @RequestBody Map<String, Object> requestBody) throws Exception{
//
//    Map<String,Object> claims=functionsService.checkJwtAuthAfterLoginStudent(authorizationHeader);
//    String status=(String)claims.get("status");
//    if(status.equals("S")){
//        Map<String,Object> response =new HashMap<>();
//        boolean succ = studentDbClass.updateStudentDocumentsbyemail(
//                (String) claims.get("email"),
//                (String) requestBody.get("name"),
//                (String) requestBody.get("regno"),
//                (String) requestBody.get("passout")
//        );
//        if (succ) {
//            response.put("status", "S");
//            response.put("message", "Student  details updated successfully!");
//            return ResponseEntity.ok(response);
//
//        } else {
//            //Error in updating user details
//            response.put("status", "E");
//            response.put("message", "No change between database and passed details or Error. Please try again.");
//            return ResponseEntity.status(503).body(response);
//        }
//
//
//    }
//    else{
//        return ResponseEntity.status(401).body(claims);
//
//    }
//
//}

}




