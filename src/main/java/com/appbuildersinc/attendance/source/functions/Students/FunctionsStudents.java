package com.appbuildersinc.attendance.source.functions.Students;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.ClassDB;
import com.appbuildersinc.attendance.source.database.MongoDB.FacultyDB;
import com.appbuildersinc.attendance.source.database.MongoDB.StudentDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SuperAdminDB;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
//DATABASE ONLY ACCESSIBLE HERE
//BUSINESS LOGIC HERE????

@Service
public class FunctionsStudents {
    private final FacultyDB userdb;
    private final ClassDB classDB;
    private final StudentDB studentDB;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;
    @Autowired
    public FunctionsStudents(FacultyDB userdb, ClassDB classDB, StudentDB studentDB, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass) {
        this.userdb = userdb;
        this.classDB = classDB;
        this.studentDB = studentDB;
        this.emailclass =emailutil;
        this.keyclass =keyutil;
        this.jwtclass = jwtutil;
        this.admindb=admindb;
        this.adminjwtclass = adminjwtclass;
    }
    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();
    public String googleClientId = dotenv.get("GOOGLE_CLIENT_ID");



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

    public Map<String, Object> getMergedTimetable(String studEmail) {
        // Step 1: Fetch all class codes for the student
        List<String> classCodes = studentDB.getStudentRegisteredClasses(studEmail);
        List<Map<String, List<Map<String, Object>>>> listOfTimetables = new ArrayList<>();

        // Step 2: Collect individual timetables
        for (String classCode : classCodes) {
            listOfTimetables.add(classDB.getClassTimetable(classCode));
        }

        // Step 3: Merge timetables
        Map<String, List<Map<String, Object>>> mergedTimetable = mergeTimetables(listOfTimetables);

        // Step 4: Validate merged timetable — only classCodes student is actually registered for
        for (List<Map<String, Object>> periods : mergedTimetable.values()) {
            for (Map<String, Object> period : periods) {
                String code = (String) period.get("classCode");
                if (!code.equals("_") && !classCodes.contains(code)) {
                    return null;
                }
            }
        }

        // Step 5: Ensure every registered class appears at least once
        for (String code : classCodes) {
            boolean found = false;
            for (List<Map<String, Object>> periods : mergedTimetable.values()) {
                for (Map<String, Object> period : periods) {
                    if (code.equals(period.get("classCode"))) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (!found) return null;
        }

        // Step 6: Fetch unique class details separately
        Map<String, Object> classDetailsMap = new HashMap<>();
        for (String classCode : classCodes) {
            classDetailsMap.put(classCode, classDB.getClassDetailsWithoutAttendanceAndTimetable(classCode));
        }

        // Step 7: Return final structured response
        Map<String, Object> result = new HashMap<>();
        result.put("timetable", mergedTimetable);
        result.put("classDetails", classDetailsMap);

        return result;
    }

    public static Map<String, List<Map<String, Object>>> mergeTimetables(List<Map<String, List<Map<String, Object>>>> timetables) {
        Map<String, List<Map<String, Object>>> merged = new HashMap<>();

        for (Map<String, List<Map<String, Object>>> timetable : timetables) {
            for (Map.Entry<String, List<Map<String, Object>>> entry : timetable.entrySet()) {
                String day = entry.getKey();
                List<Map<String, Object>> periods = entry.getValue();

                merged.computeIfAbsent(day, k -> new ArrayList<>()).addAll(periods);
            }
        }

        return merged;
    }

}
