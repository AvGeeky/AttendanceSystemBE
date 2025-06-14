package com.appbuildersinc.attendance.source.functions.Class;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.ClassDB;
import com.appbuildersinc.attendance.source.database.MongoDB.FacultyDB;
import com.appbuildersinc.attendance.source.database.MongoDB.LogicalGroupingDB;
import com.appbuildersinc.attendance.source.database.MongoDB.SuperAdminDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
//DATABASE ONLY ACCESSIBLE HERE
//BUSINESS LOGIC HERE????

@Service
public class FunctionsClass {
    private final FacultyDB userdb;
    private final ClassDB classDB;
    private final LogicalGroupingDB logicalGroupingDB;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;

    @Autowired
    public FunctionsClass(LogicalGroupingDB logicalGroupingDB, FacultyDB userdb, ClassDB classDB, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass) {
        this.userdb = userdb;
        this.classDB = classDB;
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

    public boolean classExists (String classCode){
        return classDB.classExists(classCode);
    }







}
