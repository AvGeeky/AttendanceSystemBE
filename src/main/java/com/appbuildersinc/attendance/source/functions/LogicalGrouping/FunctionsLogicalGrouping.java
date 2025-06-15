package com.appbuildersinc.attendance.source.functions.LogicalGrouping;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.*;
import com.appbuildersinc.attendance.source.functions.Class.FunctionsClass;
import org.bson.Document;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class FunctionsLogicalGrouping {
    private final FacultyDB userdb;
    private final StudentDB studentdb;
    private final ClassDB classDB;
    private final LogicalGroupingDB logicalGroupingDB;
    private final FunctionsClass functionsClass;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;

    @Autowired
    public FunctionsLogicalGrouping(LogicalGroupingDB logicalGroupingDB, ClassDB classDB, StudentDB studentdb, FacultyDB userdb, KeyPairUtil keyclass, FacultyJwtUtil jwtclass, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass, FunctionsClass functionsClass) {
        this.userdb = userdb;
        this.logicalGroupingDB = logicalGroupingDB;
        this.keyclass = keyclass;
        this.jwtclass = jwtclass;
        this.admindb = admindb;
        this.adminjwtclass = adminjwtclass;
        this.studentdb = studentdb;
        this.classDB = classDB;
        this.functionsClass = functionsClass;
    }

    public boolean insertLogicalGrouping(Map<String, Object> group, String dept, String email) {
        String section = (String) group.get("section");
        String degree = (String) group.get("degree");
        String passout = (String) group.get("passout");
        String advisorEmail = (String) group.get("advisorEmail");

        if (advisorEmail != null && !userdb.isEmailAllowed(advisorEmail)) {
            return false; // Invalid advisor email
        }

        boolean isElective = (advisorEmail == null);
        List<String> classCodes = (List<String>) group.get("class-code");
        List<String> regNumbers = (List<String>) group.get("registernumbers");

        // Build groupcode
        String electiveName = "";
        if (isElective) {
            for (String eleccode : classCodes) {
                electiveName += eleccode;
            }
        }
        String groupcode = isElective ? dept + electiveName + passout : dept + passout + section;

        // Timetable validation
        Map<String, List<Map<String, Object>>> timetable = (Map<String, List<Map<String, Object>>>) group.get("timetable");

        // Validation 1
        for (List<Map<String, Object>> periods : timetable.values()) {
            for (Map<String, Object> period : periods) {
                String code = (String) period.get("classCode");
                if (!code.equals("_") && !classCodes.contains(code)) {
                    return false;
                }
            }
        }

        // Validation 2
        for (String code : classCodes) {
            boolean found = false;
            for (List<Map<String, Object>> periods : timetable.values()) {
                for (Map<String, Object> period : periods) {
                    if (code.equals(period.get("classCode"))) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (!found) return false;
        }

        // Build document
        Document doc = new Document("degree", degree)
                .append("registernumbers", regNumbers)
                .append("timetable", timetable)
                .append("class-code", classCodes)
                .append("groupcode", groupcode)
                .append("department", dept)
                .append("passout", passout)
                .append("section", section);

        if (!isElective) {
            doc.append("advisorEmail", advisorEmail);
            userdb.updateClassAdvisorListByEmail(advisorEmail, regNumbers, groupcode);
        }
        // Delegate insert/update to DB layer
        boolean updated = logicalGroupingDB.insertOrUpdateLogicalGroupingToDB(doc, degree, dept, passout, section, classCodes, groupcode);
        List<String> existingClassCodes = new ArrayList<>();
        for (String classCode : classCodes) {
            if (classDB.classExists(classCode)) {
                existingClassCodes.add(classCode);
                functionsClass.refreshTimeTable(classCode,groupcode);
                classDB.updateRegisterNumbers(classCode,groupcode,regNumbers);
            }
        }
        for (String regNo:regNumbers) {
            for (String className:existingClassCodes){
                studentdb.addClassToRegisteredClasses(regNo, className);
            }
        }


            return updated;
    }

    public boolean deleteLogicalGroup(String dept, String groupcode) {
        Map<String, Object> group = logicalGroupingDB.getLogicalGroupByDeptAndCode(dept, groupcode);
        if (group == null) return false;

        String advisorEmail = (String) group.get("advisorEmail");
        List<String> regNumbers = (List<String>) group.get("registernumbers");

        if (advisorEmail != null) {
            userdb.removeClassAdvisorListByEmail(advisorEmail, groupcode);
        }

        return logicalGroupingDB.deleteLogicalGroupByDeptAndCode(dept, groupcode);
    }

}

