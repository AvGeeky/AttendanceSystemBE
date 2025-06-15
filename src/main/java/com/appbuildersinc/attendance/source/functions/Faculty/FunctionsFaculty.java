package com.appbuildersinc.attendance.source.functions.Faculty;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.PasswordUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
//DATABASE ONLY ACCESSIBLE HERE
//BUSINESS LOGIC HERE????

@Service
public class FunctionsFaculty {
    private final FacultyDB facultyDB;
    private final StudentDB studentdb;
    private final ClassDB classDB;
    private final LogicalGroupingDB logicalGroupingDB;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;
    @Autowired
    public FunctionsFaculty(ClassDB cldb, StudentDB stu, FacultyDB facultyDB, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass, LogicalGroupingDB logicalGroupingDB) {
        this.facultyDB = facultyDB;
        this.classDB = cldb;
        this.studentdb=stu;
        this.emailclass =emailutil;
        this.keyclass =keyutil;
        this.jwtclass = jwtutil;
        this.admindb=admindb;
        this.adminjwtclass = adminjwtclass;
        this.logicalGroupingDB = logicalGroupingDB;
    }
    public boolean isEmailAllowed(String email)
    {
        return facultyDB.isEmailAllowed(email);
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

    public boolean hashAndUpdatePassword(String email, String password) throws Exception {
        String hashedPassword = PasswordUtil.hashPassword(password);

        return facultyDB.updatePasswordByEmail(email,hashedPassword);
    }

    public boolean attemptLogin(String email, String password) throws Exception {
        String hashedPassword = facultyDB.getPasswordByEmail(email);
        if (hashedPassword == null) {
            return false;
        }
        return PasswordUtil.verifyPassword(password, hashedPassword);
    }

    public boolean updateMenteeList(String email, List<String> menteeList, String reset) {
        return facultyDB.updateMenteeListByEmail(email, menteeList, reset);
    }

    public Map<String,Object> getMenteeListDetails(String email) {
        Map<String, Object> response = new HashMap<>();
        List<String> menteeList = facultyDB.getMenteeList(email);
        if (menteeList != null) {
            for (String mentee : menteeList) {
                Map<String, Object> menteeDetails = studentdb.getStudentDetailsByRegisterNumber(mentee);
                if (menteeDetails != null) {
                    response.put(mentee, menteeDetails);
                } else {
                    response.put(mentee, "Mentee details not found");
                }
            }
        } else {
            return null;
        }
        return response;
    }

    public List<Map<String,Object>> getAllLogicalGroupings(String dept) {
        Set<Map<String, Object>> deptLG = new HashSet<>();
        deptLG.addAll(logicalGroupingDB.viewalllogicalgroupings(dept));
        deptLG.addAll(logicalGroupingDB.viewalllogicalgroupings("FirstYear"));
        return new ArrayList<>(deptLG);
    }

    public boolean transferClass (String classCode, String groupCode, String newFacEmail){
        String oldFacEmail = classDB.getFacultyEmailFromClass(classCode,groupCode);
        String newFacName = facultyDB.getUserNameByEmail(newFacEmail);
        if (!facultyDB.removeClassFromFacultyClasses(oldFacEmail,classCode)){
            return false;
        }
        if (!facultyDB.addClassToFacultyClasses(newFacEmail,classCode)){
            return false;
        }
        if (!classDB.updateClassFacultyDetails(groupCode,classCode,newFacEmail,newFacName)){
            return false;
        }

        emailclass.sendClassTransferMail(newFacEmail,classDB.getAllClassDetails(classCode,groupCode));
        return true;


    }





}
