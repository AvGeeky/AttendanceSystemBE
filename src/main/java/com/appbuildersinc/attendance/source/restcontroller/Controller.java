package com.appbuildersinc.attendance.source.restcontroller;

import com.appbuildersinc.attendance.source.Utilities.KeyPairUtil;
import com.appbuildersinc.attendance.source.database.UserDB;
import com.appbuildersinc.attendance.source.Utilities.jwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
Error handling template:
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
public class Controller {
    private final Functions functionsService;
    private final UserDB userdbclass;
    private final jwtUtil jwtclass;
    private final KeyPairUtil keyclass;

    @Autowired
    public Controller(Functions functionsService, UserDB userdbutil, jwtUtil jwtutil, KeyPairUtil keyutil) {
        this.functionsService = functionsService;
        this.userdbclass = userdbutil;
        this.jwtclass = jwtutil;
        this.keyclass =keyutil;
    }

    @PostMapping("/faculty/setEmail")
    public ResponseEntity<Map<String,Object>> setEmail(@RequestParam String email) throws Exception {
        Map<String, Object> response = new HashMap<>();
       if (functionsService.isEmailAllowed(email)){
           String enc_otp = functionsService.sendMailReturnOtp(email);

           Map<String, Object> claims = jwtclass.createClaims(email,false,"FACULTY",enc_otp,false,"");
           String jwt = jwtclass.signJwt(claims);

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

        Map<String, Object> claims = functionsService.checkJwtAuthBeforeLogin(authorizationHeader);

        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with Business Logic
            String jwt;
            HashMap<String, Object> response = new HashMap<>();

            String enc_otp = (String) claims.get("enc_otp");
            if (enc_otp .equals("")){
                response.put("status", "E");
                response.put("message", "Email ID not set yet. Please set email ID first.");
                return ResponseEntity.status(401).body(response);
            }

            int dec_otp = Integer.parseInt(keyclass.decryptString(enc_otp));

            if (dec_otp == Integer.parseInt(otp)){
                response.put("status", "S");
                response.put("message", "OTP has been successfully verified!");

                jwtclass.updateEncOtp(claims, "");
                jwtclass.updateOtpAuthStatus(claims, true);

                jwt = jwtclass.signJwt(claims);
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
        Map<String, Object> claims = functionsService.checkJwtAuthBeforeLogin(authorizationHeader);


        String status = (String) claims.get("status");

        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();

            if ((boolean) claims.get("otp_auth")){
                //OTP is verified, proceed with setting username and password
                String email = (String) claims.get("email");
                if (functionsService.hashAndUpdatePassword(email, password)) {
                    jwtclass.updateOtpAuthStatus(claims, false);
                    String jwt = jwtclass.signJwt(claims);
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
    public ResponseEntity<Map<String,Object>> login(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
                                                             @RequestParam String email,
                                                             @RequestParam String password
                                                    ) throws Exception {
        Map<String, Object> claims = functionsService.checkJwtAuthBeforeLogin(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();
            if (functionsService.attemptLogin(email,password)){
                //Login successful
                response.put("status", "S");
                response.put("message", "Login successful!");

                jwtclass.updateAuthorised(claims,true);
                jwtclass.updateEmail(claims,email);
                jwtclass.updateRole(claims,"FACULTY");

                Map<String, Object> details = userdbclass.getUserDetailsByEmail(email);

                if (details.get("name")== null){
                    response.put("status", "FL"); //first login
                    response.put("message", "First login. Please set your details by calling necessary endpoint.");
                    response.put("token", jwtclass.signJwt(claims));
                    return ResponseEntity.ok(response);
                }

                if (details.get("mentor").equals("True") && details.get("class_advisor").equals("True")) {
                    jwtclass.updateAddnlRole(claims, "CM");
                    response.put("role", "CM");
                }
                else if (details.get("mentor").equals("True")){
                    jwtclass.updateAddnlRole(claims,"M");
                    response.put("role", "M");
                }
                else if (details.get("class_advisor").equals("True")) {
                    jwtclass.updateAddnlRole(claims, "A");
                    response.put("role", "A");
                }

                String jwt = jwtclass.signJwt(claims);
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
        else{
            //JWT is invalid, return error response
            return ResponseEntity.status(401).body(claims);
        }
    }


    @GetMapping("/faculty/getDetails")
    public ResponseEntity<Map<String,Object>> getDetails(@RequestHeader(HttpHeaders.AUTHORIZATION)
                                                             String authorizationHeader) throws Exception {
        Map<String, Object> claims = functionsService.checkJwtAuthAfterLogin(authorizationHeader);
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
        Map<String, Object> claims = functionsService.checkJwtAuthAfterLogin(authorizationHeader);
        //Check if the JWT is valid
        String status = (String) claims.get("status");
        if (status.equals("S")) {
            //JWT is valid, proceed with business logic
            Map<String, Object> response = new HashMap<>();

           boolean succ = userdbclass.updateUserDocumentByEmail((String) claims.get("email"),
                   (String) requestBody.get("name"),
                   (String) requestBody.get("department"),
                   (String) requestBody.get("position"),
                   (String) requestBody.get("mentor"),
                   (String) requestBody.get("class_advisor"));
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