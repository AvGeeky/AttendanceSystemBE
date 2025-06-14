package com.appbuildersinc.attendance.source.functions.Class;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//DATABASE ONLY ACCESSIBLE HERE
//BUSINESS LOGIC HERE????

@Service
public class FunctionsClass {
    private final FacultyDB userdb;
    private final FacultyDB facultyDB;
    private final ClassDB classDB;
    private final StudentDB studentdb;
    private final LogicalGroupingDB logicalGroupingDB;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;

    @Autowired
    public FunctionsClass(LogicalGroupingDB logicalGroupingDB, FacultyDB userdb, FacultyDB facultyDB, ClassDB classDB, StudentDB studentdb, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass) {
        this.userdb = userdb;
        this.facultyDB = facultyDB;
        this.classDB = classDB;
        this.studentdb = studentdb;
        this.emailclass =emailutil;
        this.keyclass =keyutil;
        this.jwtclass = jwtutil;
        this.admindb=admindb;
        this.adminjwtclass = adminjwtclass;
        this.logicalGroupingDB=logicalGroupingDB;

    }

    public boolean refreshTimeTable(String classCode, String groupCode) {

        Map<String, Object> logicalGrouping = logicalGroupingDB.getLogicalGroupingByCode(groupCode);
        Map<String, List<Map<String, Object>>> timetable =
                (Map<String, List<Map<String, Object>>>) logicalGrouping.get("timetable");

        Map<String, List<Map<String, Object>>> newTimetable = new HashMap<>();
        for (String day : timetable.keySet()) {
            List<Map<String, Object>> slots = timetable.get(day);
            if (slots == null) continue;
            for (Map<String, Object> slot : slots) {
                if (slot == null || slot.get("classCode") == null) continue;
                if (slot.get("classCode").equals(classCode)) {
                    newTimetable.computeIfAbsent(day, k -> new java.util.ArrayList<>()).add(slot);
                }
            }
        }
        return classDB.saveRefreshedClassTimetable(groupCode,classCode,newTimetable);
    }

    public boolean createNewClass( String groupCode, String classCode, String className, String dept, String facultyEmail,
                                   String credits)
    {

        Map<String, Object> logicalGrouping = logicalGroupingDB.getLogicalGroupingByCode(groupCode);

        String passoutYear = (String) logicalGrouping.get("passout");

        Map<String,Object> facultyDetails = facultyDB.getFacultyDetailsByEmail(facultyEmail);
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
                groupCode, classCode, dept,className, facultyName,passoutYear, facultyEmail, credits, newTimetable, regNumbers
                , noOfStudents
        );
        if (success) {
            for (String regNumber : regNumbers) {
                studentdb.addClassToRegisteredClasses(regNumber, classCode);
            }
            facultyDB.addClassToFacultyClasses(facultyEmail, classCode);
            return true;
        }
        else {
            return false;
        }

    }

    public boolean dropClass(String classCode, String groupCode) {
        Map<String,Object> info = classDB.deleteClassAndReturnInfo(classCode, groupCode);
        if (info == null) {
            return false; // Class not found or deletion failed
        }
        userdb.removeClassFromFacultyClasses(info.get("facultyEmail").toString(),classCode);
        for (String regno : (List<String>) info.get("regNumbers")) {
            studentdb.removeClassFromRegisteredClasses(regno, classCode);
        }
        return true;
    }









}
