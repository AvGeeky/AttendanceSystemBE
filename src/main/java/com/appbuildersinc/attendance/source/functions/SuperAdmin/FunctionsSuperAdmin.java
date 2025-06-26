package com.appbuildersinc.attendance.source.functions.SuperAdmin;

import com.appbuildersinc.attendance.source.Utilities.Email.emailUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.KeyPairUtil;
import com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.PasswordUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.database.MongoDB.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

//BUSINESS LOGIC HERE

@Service
public class FunctionsSuperAdmin {
    private final FacultyDB userdb;
    private emailUtil emailclass;
    private final KeyPairUtil keyclass;
    private final FacultyJwtUtil jwtclass;
    private final SuperAdminDB admindb;
    private final SuperAdminjwtUtil adminjwtclass;
    private final StudentDB studentdb;
    private final ClassDB classdb;
    private final LogicalGroupingDB groupdb;
    @Autowired
    public FunctionsSuperAdmin(FacultyDB userdb, FacultyJwtUtil jwtutil, emailUtil emailutil, KeyPairUtil keyutil, SuperAdminDB admindb, SuperAdminjwtUtil adminjwtclass, StudentDB studentdb, ClassDB classdb, LogicalGroupingDB groupdb) {
        this.userdb = userdb;
        this.emailclass =emailutil;
        this.keyclass =keyutil;
        this.jwtclass = jwtutil;
        this.admindb=admindb;
        this.adminjwtclass = adminjwtclass;
        this.studentdb = studentdb;
        this.classdb = classdb;
        this.groupdb = groupdb;
    }

    public boolean attemptloginadmin(String email,String password){
        String hashedPassword=admindb.getPasswordByEmail(email);

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
        Object errObj = claims.get("error");
        String error = (errObj != null) ? errObj.toString().trim() : "";

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
        if (!claims.get("role").equals("ADMIN")) {
            response.put("status", "E");
            response.put("message", "NOT AUTHORIZED.");
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
    public Boolean deleteTeacher(String email){
        if (!userdb.isEmailAllowed(email)) {
            return false;
        }
        Map<String,Object> details=userdb.getFacultyDetailsByEmail(email);
        if ((details.get("class_advisor_list")==null ||((Map<String, Object>) details.get("class_advisor_list")).isEmpty()) && (details.get("facultyClasses")==null||((List<String>) details.get("facultyClasses")).isEmpty())) {
               return userdb.deleteFacultyByEmail(email);
        }
        else{
            return false;
        }
    }
    public Boolean deleteStudent(List<String> registernumbers){
        for(String registernumber:registernumbers){
            if (!studentdb.doesRegisterNumberExist(registernumber)){
                return false;
            }
            Map<String,Object> studentdetails=studentdb.getStudentDetailsByRegisterNumber(registernumber);
            List<String> registeredclasses=(List<String>)studentdetails.get("registeredClasses");

            if (!(registeredclasses == null || registeredclasses.isEmpty())) {
                for(String classcode:registeredclasses){
                    classdb.deleteStudentFromClass(registernumber,classcode);
                }
            }
            List<String> logicalgroupingset = studentdb.getStudentRegisteredGroupings(registernumber);
            if (!(logicalgroupingset == null || logicalgroupingset.isEmpty())) {
                for(String grouping:logicalgroupingset){
                    boolean done=groupdb.removeRegNofromGrouping(registernumber,grouping);
                    if(done){
                        String advisoremail=groupdb.getAdvisorEmail(grouping);
                        if(advisoremail!=null){
                            Boolean regremovaldone=userdb.removeRegNoFromAdvisorList(registernumber,advisoremail,grouping);

                            if(regremovaldone){
                                Boolean studentdocremoval =studentdb.removeStudent(registernumber);
                                if(!studentdocremoval){
                                    return false;
                                }
                            }
                            else{
                                return false;
                            }
                        }
                    }
                    else{
                        return false;
                    }
                }
            }
            else{
                Boolean studentdocremoval =studentdb.removeStudent(registernumber);
                if(!studentdocremoval){
                    return false;
                }
            }
        }
        return true;


    }


}
