package com.appbuildersinc.attendance.source.functions.Faculty;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.PasswordUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.FacultyDB;
import com.appbuildersinc.attendance.source.database.MongoDB.LogicalGroupingDB;
import com.appbuildersinc.attendance.source.database.MongoDB.StudentDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SuperAdminDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
//DATABASE ONLY ACCESSIBLE HERE
//BUSINESS LOGIC HERE????

@Service
public class FunctionsFaculty {
    private final FacultyDB userdb;
    private final StudentDB studentdb;
    private final LogicalGroupingDB logicalGroupingDB;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;
    @Autowired
    public FunctionsFaculty(StudentDB stu, FacultyDB userdb, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass, LogicalGroupingDB logicalGroupingDB) {
        this.userdb = userdb;
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

    public boolean updateMenteeList(String email, List<String> menteeList, String reset) {
        return userdb.updateMenteeListByEmail(email, menteeList, reset);
    }

    public Map<String,Object> getMenteeListDetails(String email) {
        Map<String, Object> response = new HashMap<>();
        List<String> menteeList = userdb.getMenteeList(email);
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
    public boolean createNewClass( String groupCode, String classCode, String className, String dept, String facultyEmail,
    String credits)
    {

        Map<String, Object> logicalGrouping = logicalGroupingDB.getLogicalGroupingByCode(groupCode);

        String passoutYear = (String) logicalGrouping.get("passout");

        Map<String,Object> facultyDetails = userdb.getFacultyDetailsByEmail(facultyEmail);
        String facultyName = (String) facultyDetails.get("name");

        List<String> regNumbers = (List<String>) logicalGrouping.get("registernumbers");

        String noOfStudents = Integer.toString(regNumbers.size());

        //Timetable
        Map<String, List<Map<String, Object>>> timetable = (Map<String, List<Map<String, Object>>>) logicalGrouping.get("timetable");
        Map<String, List<Map<String, Object>>> newTimetable = new HashMap<>();
        for (String day : timetable.keySet()) {
            List<Map<String, Object>> slots = timetable.get(day);
            if (slots == null) continue;

            for (Map<String, Object> slot : slots) {
                if (slot == null || slot.get("classCode") == null) continue;

                if (slot.get("classCode").equals(classCode)) {
                    newTimetable.computeIfAbsent(day, k -> new ArrayList<>()).add(slot);
                }
            }
        }

        boolean success = classDB.createNewClass(
                groupCode, classCode, dept,className, facultyName,passoutYear, facultyEmail, credits, newTimetable, regNumbers,
                , noOfStudents
        );
        if (success) return true;
        else {
            return false;
        }

    }


}
