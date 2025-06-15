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
        Map<String, Object> oldLG = logicalGroupingDB.getLogicalGroupingByCode(groupcode);
        boolean isNew = oldLG == null;

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

        // Flags for changes
        boolean timetableChanged = isNew || !Objects.equals(oldLG.get("timetable"), timetable);
        boolean registerNumbersChanged = isNew || !Objects.equals(oldLG.get("registernumbers"), regNumbers);

        // Build document
        Document doc = new Document("degree", degree)
                .append("class-code", classCodes)
                .append("groupcode", groupcode)
                .append("department", dept)
                .append("passout", passout)
                .append("section", section);

        if (registerNumbersChanged) {
            doc.append("registernumbers", regNumbers);
        }

        if (timetableChanged) {
            doc.append("timetable", timetable);
        }

        if (!isElective) {
            String oldAdvisorEmail = isNew ? null : (String) oldLG.get("advisorEmail");
            if (isNew || !Objects.equals(oldAdvisorEmail, advisorEmail)) {
                if (oldAdvisorEmail != null) {
                    userdb.removeClassAdvisorListByEmail(oldAdvisorEmail, groupcode);
                }
                doc.append("advisorEmail", advisorEmail);
                userdb.updateClassAdvisorListByEmail(advisorEmail, regNumbers, groupcode);
            }
        }

        // Delegate insert/update to DB layer
        boolean updated = logicalGroupingDB.insertOrUpdateLogicalGroupingToDB(
                doc, degree, dept, passout, section, classCodes, groupcode);

        List<String> existingClassCodes = new ArrayList<>();
        for (String classCode : classCodes) {
            if (classDB.classExists(classCode, groupcode)) {
                existingClassCodes.add(classCode);
                if (timetableChanged) {
                    functionsClass.refreshTimeTable(classCode, groupcode);
                }
                if (registerNumbersChanged) {
                    classDB.updateRegisterNumbers(classCode, groupcode, regNumbers);
                }
            }
        }

        // If register numbers changed, update student mappings
        if (!isNew && registerNumbersChanged) {
            List<String> oldList = (List<String>) oldLG.get("registernumbers");
            List<String> newList = regNumbers;

            Set<String> oldSet = new HashSet<>(oldList);
            Set<String> newSet = new HashSet<>(newList);

            Set<String> removed = new HashSet<>(oldSet);
            removed.removeAll(newSet); // Removed students
            Set<String> added = new HashSet<>(newSet);
            added.removeAll(oldSet); // New students

            for (String regNo : removed) {
                for (String className : existingClassCodes) {
                    studentdb.removeClassFromRegisteredClasses(regNo, className);
                }
            }

            for (String regNo : added) {
                for (String className : existingClassCodes) {
                    studentdb.addClassToRegisteredClasses(regNo, className);
                }
            }
        }

        return updated;
    }


    public boolean deleteLogicalGroup(String dept, String groupcode) {
        Map<String, Object> group = logicalGroupingDB.getLogicalGroupingByCode(groupcode);

        if (group == null) return false;

        String advisorEmail = (String) group.get("advisorEmail");



        if (advisorEmail != null) {
            userdb.removeClassAdvisorListByEmail(advisorEmail, groupcode);
        }

        List<String> classCodes = (List<String>) group.get("class-code");
        List<String> oldList = (List<String>) group.get("registernumbers");


        Set<String> oldSet = new HashSet<>(oldList);

        for (String regNo : oldSet) {
            for (String className : classCodes) {
                studentdb.removeClassFromRegisteredClasses(regNo, className);
            }
        }
        for (String className : classCodes) {
            String facultyEmail = classDB.getFacultyEmailFromClass(className,groupcode);
            userdb.removeClassFromFacultyClasses(facultyEmail, className);
            classDB.deleteClassAndReturnInfo(className,groupcode);
        }

        return logicalGroupingDB.deleteLogicalGroupByDeptAndCode(dept, groupcode);
    }

}

