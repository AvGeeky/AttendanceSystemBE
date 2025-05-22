package com.appbuildersinc.attendance.source.restcontroller;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.PasswordUtil;
import com.appbuildersinc.attendance.source.Utilities.jwtUtil;
import com.appbuildersinc.attendance.source.database.UserDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

//DATABASE ONLY ACCESSIBLE HERE
//BUSINESS LOGIC HERE!!!!

@Service
public class Functions {
    private final UserDB userdb;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final jwtUtil jwtclass;


    @Autowired
    public Functions(UserDB userdb,jwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil) {
        this.userdb = userdb;
        this.emailclass =emailutil;
        this.keyclass =keyutil;
        this.jwtclass = jwtutil;
    }

    public boolean isEmailAllowed(String email) {
        return userdb.isEmailAllowed(email);
    }

    public String sendMailReturnOtp(String email) throws Exception {
        int otp = emailclass.sendMail(email);
        return keyclass.encryptString(Integer.toString(otp));
    }

    public Map<String,Object> checkJwtAuthBeforeLogin(String jwt) throws Exception {
        HashMap<String, Object> response = new HashMap<>();
        // Check if the JWT is null or empty
        if (jwt == null) {
            response.put("status", "E");
            response.put("message", "JWT TOKEN NOT PASSED");
            return response;
        }

        Map<String, Object> claims = jwtclass.parseJwt(jwt);
        Object error = claims.get("error");

        // Check if the JWT is expired or invalid
        if ("Token expired".equals(error)) {
            response.put("status", "TO");
            response.put("message", "Login Expired. Please re-login.");
            return response;
        }
        if ("Invalid token".equals(error)) {
            response.put("status", "TO");
            response.put("message", "Invalid Login Token. Please re-login.");
            return response;
        }

        //return the claims if valid
        claims.put("status", "S");
        return claims;
    }

    public Map<String,Object> checkJwtAuthAfterLogin(String jwt) throws Exception {
        HashMap<String, Object> response = new HashMap<>();
        // Check if the JWT is null or empty
        if (jwt == null) {
            response.put("status", "E");
            response.put("message", "JWT TOKEN NOT PASSED");
            return response;
        }

        Map<String, Object> claims = jwtclass.parseJwt(jwt);
        Object error = claims.get("error");

        // Check if the JWT is expired or invalid
        if ("Token expired".equals(error)) {
            response.put("status", "TO");
            response.put("message", "Login Expired. Please re-login.");
            return response;
        }
        if ("Invalid token".equals(error)) {
            response.put("status", "TO");
            response.put("message", "Invalid Login Token. Please re-login.");
            return response;
        }

        //return the claims if valid
        if ((boolean)claims.get("authorised")) {
            claims.put("status", "S");
            return claims;
        }
        else{
            response.put("status", "E");
            response.put("message", "NOT AUTHORIZED. Please re-login.");
            return response;
        }

    }

    public boolean hashAndUpdatePassword(String email, String password) throws Exception {
        String hashedPassword = PasswordUtil.hashPassword(password);
        return userdb.updatePasswordByEmail(email,hashedPassword);
    }

    public boolean attemptLogin(String email, String password) throws Exception {
        String hashedPassword = userdb.getPasswordByEmail(email);
        if (hashedPassword == null) {
            return false;
        }
        return PasswordUtil.verifyPassword(password, hashedPassword);
    }





}
