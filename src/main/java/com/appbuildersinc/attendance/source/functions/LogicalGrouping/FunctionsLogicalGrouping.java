package com.appbuildersinc.attendance.source.functions.LogicalGrouping;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.*;

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

    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;

    @Autowired
    public FunctionsLogicalGrouping(LogicalGroupingDB logicalGroupingDB, ClassDB classDB, StudentDB studentdb, FacultyDB userdb, KeyPairUtil keyclass, FacultyJwtUtil jwtclass, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass) {
        this.userdb = userdb;
        this.logicalGroupingDB = logicalGroupingDB;
        this.keyclass = keyclass;
        this.jwtclass = jwtclass;
        this.admindb = admindb;
        this.adminjwtclass = adminjwtclass;
        this.studentdb = studentdb;
        this.classDB = classDB;
    }

    private boolean refreshTimeTable(String classCode, String groupCode) {

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
        return classDB.saveRefreshedClassTimetable(classCode,newTimetable);
    }

    public boolean insertLogicalGrouping(Map<String, Object> group, String dept, String email) {
        String section = (String) group.get("section");
        String degree = (String) group.get("degree");
        String passout = (String) group.get("passout");
        String advisorEmail = (String) group.get("advisorEmail");


        if (advisorEmail != null && !userdb.isEmailAllowed(advisorEmail)) {
            return false; // Invalid advisor email
        }
        Set<String> regNumbersSet = new HashSet<>((List<String>) group.get("registernumbers"));
        for (String regNo : regNumbersSet) {
            if (!studentdb.doesRegisterNumberExist(regNo)) {
                return false; // Invalid register number
            }
        }
        List<String> regNumbers = new ArrayList<>(regNumbersSet);

        boolean isElective = (advisorEmail == null);

        List<String> classCodes = (List<String>) group.get("class-code");




        // Build groupcode
        String electiveName = "";
        if (isElective) {
            for (String eleccode : classCodes) {
                electiveName += eleccode;
            }
        }

        if (section == null || section.isEmpty()) {
            section = "NULL";
        }

        String groupcode = isElective ? dept + electiveName + passout : dept + passout + section;
        Map<String, Object> oldLG = logicalGroupingDB.getLogicalGroupingByCode(groupcode);
        boolean isNew = oldLG == null;
        if (isNew){
            for (int i = 0; i < classCodes.size(); i++) {
                classCodes.set(i, classCodes.get(i) + '-' + section + '-' + passout);
            }
        }

        // Timetable validation
        Map<String, List<Map<String, Object>>> timetable = (Map<String, List<Map<String, Object>>>) group.get("timetable");
        //System.out.println(timetable);
        if (isNew) {
            for (List<Map<String, Object>> periods : timetable.values()) {
                for (Map<String, Object> period : periods) {
                    String code = (String) period.get("classCode");
                    if (code != null && !code.equals("_")) {
                        period.put("classCode", code +'-'+ section + '-' + passout);
                    }
                }
            }

            // Validation 1 Are there any foreign classCodes?
            for (List<Map<String, Object>> periods : timetable.values()) {
                for (Map<String, Object> period : periods) {
                   // System.out.println(period + " period");
                    String code = (String) period.get("classCode");
                    if (!code.equals("_") && !classCodes.contains(code)) {
                      //  System.out.println("Foreign class code found: " + code);
                        return false;
                    }
                }
            }
        }

        // Validation 2 Are all registered classes present at least once?
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

            if (!found) {
                //System.out.println("Class code not found in timetable: " + code);
                return false;
            }
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
            Map<String, Map<String, Object>> hmacAndNames= new HashMap<>(studentdb.getHMACPasscodesAndNames(regNumbers));
            Map<String,Object> regnosHMAC = hmacAndNames.get("hmacPasscodes");
            Map<String, Object> regNoNameMap = hmacAndNames.get("regNoNameMap");
            for (String classCode : classCodes) {
                classDB.updateClassRegNoHmacNameMapping(classCode,regnosHMAC,regNoNameMap);
            }

        }

        if (timetableChanged) {
            doc.append("timetable", timetable);
            for (String classCode : classCodes) {
                classDB.saveRefreshedClassTimetable(classCode, timetable);
            }
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
            else if (registerNumbersChanged) {
                userdb.updateClassAdvisorListByEmail(advisorEmail, regNumbers, groupcode);
            }
        }
        boolean updated = false;
        // Delegate insert/update to DB layer
        if (isNew){
            updated = logicalGroupingDB.insertLogicalGroupingToDB(doc);
        }
        else {
            updated = logicalGroupingDB.updateLogicalGroupingInDB(doc, groupcode);
        }

        List<String> existingClassCodes = new ArrayList<>();
        for (String classCode : classCodes) {
            if (classDB.classExists(classCode)) {
                existingClassCodes.add(classCode);
                if (timetableChanged) {
                    refreshTimeTable(classCode, groupcode);
                }
                if (registerNumbersChanged) {
                    classDB.updateRegisterNumbers(classCode, regNumbers);
                }
            }
        }
        if (isNew){
            studentdb.addGroupingToRegisteredGroupings(regNumbers,groupcode);
        }

        // If register numbers changed, update student mappings
        if (!isNew && registerNumbersChanged) {
            List<String> oldList = (List<String>) oldLG.getOrDefault("registernumbers", new ArrayList<>());

            List<String> newList = regNumbers;

            Set<String> oldSet = new HashSet<>(oldList);
            Set<String> newSet = new HashSet<>(newList);

            Set<String> removed = new HashSet<>(oldSet);
            removed.removeAll(newSet); // Removed students
            Set<String> added = new HashSet<>(newSet);
            added.removeAll(oldSet); // New students


            for (String regNo : removed) {
                //System.out.println("Removing " + regNo + " from group " + groupcode);
                for (String className : existingClassCodes) {
                    studentdb.removeClassFromRegisteredClasses(regNo, className);
                }
            }


            for (String regNo : added) {
                //System.out.println("Adding " + regNo + " to group " + groupcode);
                for (String className : existingClassCodes) {
                    studentdb.addClassToRegisteredClasses(regNo, className);
                }
            }
            studentdb.addGroupingToRegisteredGroupings(added,groupcode);
            studentdb.removeGroupingFromRegisteredGroupings(removed,groupcode);
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
        studentdb.removeGroupingFromRegisteredGroupings(oldSet, groupcode);
        for (String className : classCodes) {
            String facultyEmail = classDB.getFacultyEmailFromClass(className);
            userdb.removeClassFromFacultyClasses(facultyEmail, className);
            classDB.deleteClassAndReturnInfo(className);
        }

        return logicalGroupingDB.deleteLogicalGroupByDeptAndCode(dept, groupcode);
    }

}

