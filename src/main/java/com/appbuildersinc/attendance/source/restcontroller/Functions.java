package com.appbuildersinc.attendance.source.restcontroller;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.PasswordUtil;
import com.appbuildersinc.attendance.source.Utilities.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.SuperAdminDB;
import com.appbuildersinc.attendance.source.database.UserDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
//DATABASE ONLY ACCESSIBLE HERE
//BUSINESS LOGIC HERE????

@Service
public class Functions {
    private final UserDB userdb;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;
    @Autowired
    public Functions(UserDB userdb, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass) {
        this.userdb = userdb;
        this.emailclass =emailutil;
        this.keyclass =keyutil;
        this.jwtclass = jwtutil;
        this.admindb=admindb;
        this.adminjwtclass = adminjwtclass;
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

    public Map<String,Object> checkJwtAuthAfterLoginFaculty(String jwt) throws Exception {
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
        if (claims.get("role").equals("STUDENT")) {
            response.put("status", "E");
            response.put("message", "NOT AUTHORIZED.");
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
    public Map<String,Object> checkJwtAuthAfterLoginStudent(String jwt) throws Exception {
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
        if (claims.get("role").equals("FACULTY")) {
            response.put("status", "E");
            response.put("message", "NOT AUTHORIZED.");
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

    public boolean attemptloginadmin(String email,String password){
        String hashedPassword=admindb.getPasswordByEmail(email);
        System.out.println(hashedPassword);
        if(hashedPassword==null){
            return false;
        }
        boolean s= PasswordUtil.verifyPassword(password,hashedPassword);

        return s;
    }



    public Map<String, String> getNameDeptbyEmail(String email) {
        return SuperAdminDB.getNameDeptbyEmail(email);
    }



    public Map<String,Object> checkJwtAuthAfterLoginAdmin(String jwt) throws Exception {
        HashMap<String, Object> response = new HashMap<>();
        // Check if the JWT is null or empty
        if (jwt == null) {
            response.put("status", "E");
            response.put("message", "JWT TOKEN NOT PASSED");
            return response;
        }

        Map<String, Object> claims = adminjwtclass.parseJwt(jwt);
        Object error = claims.get("error");

        // Check if the JWT is expired or invalid
        if ("Token expired".equals(error)) {
            response.put("status", "TO");
            response.put("message", "Login Expired. Please re-login.");
            return response;
        }
        if (!claims.get("role").equals("ADMIN")) {
            response.put("status", "E");
            response.put("message", "NOT AUTHORIZED.");
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
}
