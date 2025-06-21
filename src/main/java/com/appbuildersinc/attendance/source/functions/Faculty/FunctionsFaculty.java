package com.appbuildersinc.attendance.source.functions.Faculty;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.PasswordUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.*;
import com.appbuildersinc.attendance.source.functions.Students.FunctionsStudents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

//BUSINESS LOGIC HERE

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
    private final FunctionsStudents functionstudent;
    @Autowired
    public FunctionsFaculty(ClassDB cldb, StudentDB stu, FacultyDB facultyDB, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass, LogicalGroupingDB logicalGroupingDB, FunctionsStudents functionstudent) {
        this.facultyDB = facultyDB;
        this.classDB = cldb;
        this.studentdb=stu;
        this.emailclass =emailutil;
        this.keyclass =keyutil;
        this.jwtclass = jwtutil;
        this.admindb=admindb;
        this.adminjwtclass = adminjwtclass;
        this.logicalGroupingDB = logicalGroupingDB;
        this.functionstudent = functionstudent;
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
        if (!claims.get("role").equals("FACULTY")) {
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
    public Map<String,Object> getAdvisorList(String email){

        Map<String,Object> advisorlist=facultyDB.getAdvisorList(email);
        return advisorlist;

    }

    public List<Map<String,Object>> getAllLogicalGroupings() {
        Set<Map<String, Object>> deptLG = new HashSet<>();
        deptLG.addAll(logicalGroupingDB.viewalllogicalgroupings());
        return new ArrayList<>(deptLG);
    }


    public boolean transferClass (String classCode, String groupCode, String newFacEmail){
        String oldFacEmail = classDB.getFacultyEmailFromClass(classCode);
        String newFacName = facultyDB.getUserNameByEmail(newFacEmail);
        if (!facultyDB.removeClassFromFacultyClasses(oldFacEmail,classCode)){
            return false;
        }
        if (!facultyDB.addClassToFacultyClasses(newFacEmail,classCode)){
            return false;
        }
        if (!classDB.updateClassFacultyDetails(classCode,newFacEmail,newFacName)){
            return false;
        }

        emailclass.sendClassTransferMail(newFacEmail,classDB.getAllClassDetails(classCode));
        return true;


    }


    public Map<String, Object> getMergedTimetable(String facEmail) {
        // Step 1: Fetch all class codes for the student
        List<String> classCodes = facultyDB.getFacultyRegisteredClasses(facEmail);
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
            classDetailsMap.put(classCode, classDB.getClassDetailsWithoutAttendance(classCode));
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
    public Map<String,Map<String,Object>> getMentorListAttendance(String email) {
        Map<String, Object> menteedetails = getMenteeListDetails(email);
        Map<String, Map<String, Object>> result = new HashMap<>();
        if(menteedetails ==null){
            return null;
        }
        for (String registerno : menteedetails.keySet()) {
            if (!studentdb.doesRegisterNumberExist(registerno)){
                continue;
            }
            Map<String, Object> details = new HashMap<>();
            Object menteeObj = menteedetails.get(registerno);
            if (menteeObj instanceof Map) {
                Map<String, Object> menteeInfo = (Map<String, Object>) menteeObj;
                //List<String> classcodes = (List<String>) menteeInfo.get("registeredClasses");
                details.put("name",menteeInfo.get("name"));
                details.put("attendance",functionstudent.getAttendance(null,(String)menteeInfo.get("email")));
                result.put(registerno,details);
            }
        }
        return  result;

    }
    public Map<String, List<Map<String, Object>>> getAdvisorListAttendance(String email) {
        Map<String, Object> advisordetails = getAdvisorList(email);
        if (advisordetails == null) {
            return null;
        }
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        for (String groupcode : advisordetails.keySet()) {
            List<Map<String, Object>> groupList = new ArrayList<>();
            for (String registernumber : (List<String>) advisordetails.get(groupcode)) {
                Map<String, Object> details = new HashMap<>();
                Map<String, Object> studentdetails = studentdb.getStudentDetailsByRegisterNumber(registernumber);
                //List<String> classcodes = (List<String>) studentdetails.get("registeredClasses");
                details.put("name", studentdetails.get("name"));
                details.put("registernumber", registernumber);
                details.put("attendance", functionstudent.getAttendance(null, (String) studentdetails.get("email")));
                groupList.add(details);
            }
            result.put(groupcode, groupList);
        }
        return result;
    }
    public Map<String,Map<String,Object>> getStudentAttendanceByClassCode(String email,String classcode){
        List<String> classcodes=facultyDB.getFacultyRegisteredClasses(email);
        if(classcodes.size()==0){
            return null;
        }
        else{
            if(classcodes.indexOf(classcode)==-1){
                return null;
            }
            else{
                Map<String,String> registernomap=new HashMap<>();
                registernomap=classDB.getregistermap(classcode);
                Map<String,Map<String,Object>> result=new HashMap<>();
                for(String registernumber:registernomap.keySet()){
                    Map<String, Object> details = new HashMap<>();
                    Map<String, Object> studentdetails = studentdb.getStudentDetailsByRegisterNumber(registernumber);
                    details.put("name", registernomap.get(registernumber));
                    List<String> class_code=new ArrayList<>();
                    class_code.add(classcode);
                    details.put("attendance",functionstudent.getAttendance(class_code, (String) studentdetails.get("email")));
                    result.put(registernumber,details);
                }
               return result;
            }
        }

    }
    public Map<String,Map<String,Object>> getLectureAttendanceByClassCode(String email,String classcode){
        List<String> classcodes=facultyDB.getFacultyRegisteredClasses(email);
        if(classcodes.size()==0){
            return null;
        }
        else {
            if (classcodes.indexOf(classcode) == -1) {
                return null;
            }
            else{
                Map<String,String> registernomap=new HashMap<>();
                registernomap=classDB.getregistermap(classcode);
                Map<String,Map<String,Object>> result=new HashMap<>();
                Map<String, Object> studentDetailsMap = new HashMap<>();
                studentDetailsMap.put("map", registernomap);
                result.put("student-details",studentDetailsMap);
                Map<String, Object> classdetail = classDB.getAllClassDetails(classcode);
                Map<String, Object> classattendance = (Map<String,Object>) classdetail.get("attendance");
                for(String lectureno:classattendance.keySet()){
                    Map<String,Object> value=(Map<String,Object>)classattendance.get(lectureno);
                    Map<String,Object> lectureattendance=new HashMap<>();
                    Object date=value.remove("date");
                    Object time=value.remove("time");
                    lectureattendance.put("date",date);
                    lectureattendance.put("time",time);
                    lectureattendance.put("attendance",value);
                    result.put(lectureno,lectureattendance);
                }
                return result;
            }



        }

    }
    public Boolean flipAttendance(String classcode,String email,String registernumber,String lecturenumber){
        List<String> classcodes=facultyDB.getFacultyRegisteredClasses(email);
        if(classcodes.size()==0){
            //System.out.println("error1");
            return false;
        }
        else {
            if (classcodes.indexOf(classcode) == -1) {
                //System.out.println("error2"+classcode);
                return false;
            }
            else{
                Map<String,String> registernomap=new HashMap<>();
                registernomap=classDB.getregistermap(classcode);
                if(registernomap.get(registernumber)==null){
                    //System.out.println("error4");
                    return false;
                }
                else{
                    Map<String, Object> classdetail = classDB.getAllClassDetails(classcode);
                    Map<String, Object> classattendance = (Map<String,Object>) classdetail.get("attendance");
                    String lecture="lecture."+lecturenumber;
                    if(classattendance.get(lecture)==null){
                       // System.out.println("error3");
                        return false;
                    }
                    else{
                        Map<String,Object> lectureattendance=(Map<String,Object>)classattendance.get(lecture);
                        if((Integer)lectureattendance.get(registernumber)==1){
                            lectureattendance.put(registernumber,0);
                            return classDB.updateAttendance(classcode, classattendance);

                        }
                        else{
                            lectureattendance.put(registernumber,1);
                            return classDB.updateAttendance(classcode, classattendance);

                        }
                    }
                }




            }





        }




    }


}
