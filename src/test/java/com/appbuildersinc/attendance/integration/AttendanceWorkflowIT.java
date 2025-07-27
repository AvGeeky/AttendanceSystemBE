package com.appbuildersinc.attendance.integration;

import com.appbuildersinc.attendance.source.Utilities.JWTUtils.FacultyJwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.StudentjwtUtil;
import com.appbuildersinc.attendance.source.Utilities.JWTUtils.SuperAdminjwtUtil;
import com.appbuildersinc.attendance.source.functions.SuperAdmin.FunctionsSuperAdmin;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.http.HttpHeaders;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@SpringBootTest
@AutoConfigureMockMvc
public class AttendanceWorkflowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public SuperAdminjwtUtil superAdminjwtUtil() {
            return Mockito.mock(SuperAdminjwtUtil.class);
        }

        @Bean
        @Primary
        public FacultyJwtUtil facultyJwtUtil() {
            return Mockito.mock(FacultyJwtUtil.class);
        }

        @Bean
        @Primary
        public StudentjwtUtil studentJwtUtil() {
            return Mockito.mock(StudentjwtUtil.class);
        }
    }


    @Autowired
    private SuperAdminjwtUtil superAdminjwtUtil;

    @Autowired
    private FacultyJwtUtil facultyJwtUtil;

    @Autowired
    private StudentjwtUtil studentJwtUtil;

    @Test
    void testFullWorkFlow_Create_TestAttendance_Then_Delete() throws Exception{
        String adminToken=null;
        try{

            adminToken=loginassuperadmin();
            addstudents(adminToken);
            addteacher(adminToken);
            createoreditgrouping(adminToken);
            System.out.println("Created students teacher and the logical grouping also!");
        }

        finally{
            if(adminToken!=null){
                deletegroupiing(adminToken);
                deletestudent(adminToken);
                deletefaculty(adminToken);
                System.out.println("Sucessfully deleted the grouping students and faculty");
            }

        }




    }





    private String loginassuperadmin() throws Exception {
        when(superAdminjwtUtil.signJwt(any(Map.class))).thenReturn("fake-super-admin-jwt-token-for-test");
        Map<String, Object> credentials = new HashMap<>();
        credentials.put("email", "arjun.kumar@example.com");
        credentials.put("password", "password");

        MvcResult result = mockMvc.perform(post("/SuperAdmin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("S"))
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String responsebody = result.getResponse().getContentAsString();
        Map<String, Object> responsemap = objectMapper.readValue(responsebody, new TypeReference<>() {});
        return (String) responsemap.get("token");
    }

    

    private void addstudents(String token) throws Exception{
        Map<String,Object> claimsfromtoken=new HashMap<>();
        claimsfromtoken.put("email","arjun.kumar@example.com");
        claimsfromtoken.put("dept","CSE");
        claimsfromtoken.put("authorised",true);
        claimsfromtoken.put("role","ADMIN");

        when(superAdminjwtUtil.parseJwt("Bearer " + token)).thenReturn(claimsfromtoken);

        List<Map<String,String>> students=new ArrayList<>();
         Map<String,String> student1=new HashMap<>();
         student1.put("email","mridula2310239@ssn.edu.in");
         student1.put("name","mridula");
         student1.put("registerNumber","3122235001077");
         student1.put("passout","2032");
         student1.put("degree","B.E");
         student1.put("digitalid","2310235");

         student1.put("course","Computer Science Engineering");

         Map<String,String> student2=new HashMap<>();
        student2.put("email","mithun2310620@ssn.edu.in");
        student2.put("name","mithun");
        student2.put("registerNumber","3122235001089");
        student2.put("passout","2032");
        student2.put("degree","B.E");
        student2.put("digitalid","2310620");

        student2.put("course","Computer Science Engineering");

        Map<String,String> student3=new HashMap<>();
        student3.put("email","mithuna2310621@ssn.edu.in");
        student3.put("name","mithuna");
        student3.put("registerNumber","3122235001098");
        student3.put("passout","2032");
        student3.put("degree","B.E");
        student3.put("digitalid","2310621");

        student3.put("course","Computer Science Engineering");

        students.add(student1);
        students.add(student2);
        students.add(student3);
        mockMvc.perform(post("/SuperAdmin/addStudents")
                .header(HttpHeaders.AUTHORIZATION,"Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(students)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("S"))
                .andExpect(jsonPath("$.message").value("inserted all new students and their details successfully"));



    }
    private void addteacher(String token) throws Exception{
         Map<String,Object> claimsfromtoken=new HashMap<>();
        claimsfromtoken.put("email","arjun.kumar@example.com");
        claimsfromtoken.put("dept","CSE");
        claimsfromtoken.put("authorised",true);
        claimsfromtoken.put("role","ADMIN");

        when(superAdminjwtUtil.parseJwt("Bearer " + token)).thenReturn(claimsfromtoken);
        Map<String,Object> faculty =new HashMap<>();
        faculty.put("email","srividyasreekumar@yahoo.com");
        faculty.put("position","Professor");
        faculty.put("name","vidya sree");
        faculty.put("mentor","False");

        mockMvc.perform(post("/SuperAdmin/addOrUpdateTeacher")
                .header(HttpHeaders.AUTHORIZATION,"Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(faculty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Status").value("S"))
                .andExpect(jsonPath("$.Message").value("Faculty details added or updated successfully"));


    }
    public void createoreditgrouping(String token) throws Exception {
        Map<String,Object> claimsfromtoken=new HashMap<>();
        claimsfromtoken.put("email","arjun.kumar@example.com");
        claimsfromtoken.put("dept","CSE");
        claimsfromtoken.put("authorised",true);
        claimsfromtoken.put("role","ADMIN");

        when(superAdminjwtUtil.parseJwt("Bearer " + token)).thenReturn(claimsfromtoken);
        Map<String,Object> group=new HashMap<>();
        group.put("degree","B.E");
        String[] codes={ "CSE3201","CSE3202", "CSE3203", "CSE3204","CSE3205"};
        group.put("class-code",codes);
        group.put("passout","2032");
        group.put("section","B");

        String[] regnos={"3122235001077","3122235001089","3122235001098"};
        group.put("registernumbers",regnos);
        Map<String, List<Map<String, Object>>> timetable = new HashMap<>();

// Monday
        List<Map<String, Object>> monday = new ArrayList<>();
        monday.add(Map.of("classCode", "CSE3201", "startTime", "08:00", "durationMinutes", 45));
        monday.add(Map.of("classCode", "CSE3202", "startTime", "08:45", "durationMinutes", 50));
        monday.add(Map.of("classCode", "CSE3203", "startTime", "11:20", "durationMinutes", 45));
        monday.add(Map.of("classCode", "CSE3204", "startTime", "13:05", "durationMinutes", 45));
        monday.add(Map.of("classCode", "CSE3205", "startTime", "14:10", "durationMinutes", 45));
        timetable.put("Monday", monday);

// Wednesday
        List<Map<String, Object>> wednesday = new ArrayList<>();
        wednesday.add(Map.of("classCode", "CSE3202", "startTime", "08:00", "durationMinutes", 45));
        wednesday.add(Map.of("classCode", "CSE3203", "startTime", "08:45", "durationMinutes", 50));
        wednesday.add(Map.of("classCode", "CSE3204", "startTime", "11:20", "durationMinutes", 45));
        wednesday.add(Map.of("classCode", "CSE3205", "startTime", "13:05", "durationMinutes", 45));
        wednesday.add(Map.of("classCode", "CSE3201", "startTime", "14:10", "durationMinutes", 45));
        timetable.put("Wednesday", wednesday);

// Thursday
        List<Map<String, Object>> thursday = new ArrayList<>();
        thursday.add(Map.of("classCode", "CSE3203", "startTime", "08:00", "durationMinutes", 45));
        thursday.add(Map.of("classCode", "CSE3204", "startTime", "08:45", "durationMinutes", 50));
        thursday.add(Map.of("classCode", "CSE3205", "startTime", "11:20", "durationMinutes", 45));
        thursday.add(Map.of("classCode", "CSE3201", "startTime", "13:05", "durationMinutes", 45));
        thursday.add(Map.of("classCode", "CSE3202", "startTime", "14:10", "durationMinutes", 45));
        timetable.put("Thursday", thursday);

// Friday
        List<Map<String, Object>> friday = new ArrayList<>();
        friday.add(Map.of("classCode", "CSE3204", "startTime", "08:00", "durationMinutes", 45));
        friday.add(Map.of("classCode", "CSE3205", "startTime", "08:45", "durationMinutes", 50));
        friday.add(Map.of("classCode", "CSE3201", "startTime", "11:20", "durationMinutes", 45));
        friday.add(Map.of("classCode", "CSE3202", "startTime", "13:05", "durationMinutes", 45));
        friday.add(Map.of("classCode", "CSE3203", "startTime", "14:10", "durationMinutes", 45));
        timetable.put("Friday", friday);


        group.put("timetable",timetable);
        group.put("advisorEmail","srividyasreekumar@yahoo.com");
        mockMvc.perform(post("/SuperAdmin/createOrEditLogicalGrouping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(group)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("S"))
                .andExpect(jsonPath("$.message").value("logical grouping inserted or updated successfully!"));




    }
    public void deletegroupiing(String token) throws Exception {
        Map<String, Object> claimsfromtoken = new HashMap<>();
        claimsfromtoken.put("email", "arjun.kumar@example.com");
        claimsfromtoken.put("dept", "CSE");
        claimsfromtoken.put("authorised", true);
        claimsfromtoken.put("role", "ADMIN");

        when(superAdminjwtUtil.parseJwt("Bearer " + token)).thenReturn(claimsfromtoken);
        Map<String, Object> group = new HashMap<>();
        group.put("groupid", "CSE2032B");
        mockMvc.perform(delete("/SuperAdmin/deleteGrouping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(group)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("S"))
                .andExpect(jsonPath("$.message").value("deleted the grouping successfully and register nos deleted from class advisor if applicable"));


    }
    public void deletestudent(String token) throws Exception{
        Map<String, Object> claimsfromtoken = new HashMap<>();
        claimsfromtoken.put("email", "arjun.kumar@example.com");
        claimsfromtoken.put("dept", "CSE");
        claimsfromtoken.put("authorised", true);
        claimsfromtoken.put("role", "ADMIN");

        when(superAdminjwtUtil.parseJwt("Bearer " + token)).thenReturn(claimsfromtoken);
        Map<String,Object> students=new HashMap<>();
        List<String> registernumbers = new ArrayList<>(List.of(
                "3122235001077",
                "3122235001089",
                "3122235001098"
        ));
       students.put("registernumbers",registernumbers);
       mockMvc.perform(delete("/SuperAdmin/deleteStudents")
               .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(students)))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.status").value("S"))
               .andExpect(jsonPath("$.message").value("Student details deleted successfully"));





    }
    public void deletefaculty(String token) throws Exception{
        Map<String, Object> claimsfromtoken = new HashMap<>();
        claimsfromtoken.put("email", "arjun.kumar@example.com");
        claimsfromtoken.put("dept", "CSE");
        claimsfromtoken.put("authorised", true);
        claimsfromtoken.put("role", "ADMIN");

        when(superAdminjwtUtil.parseJwt("Bearer " + token)).thenReturn(claimsfromtoken);
        Map<String,Object> faculty=new HashMap<>();
        faculty.put("email","srividyasreekumar@yahoo.com");
        mockMvc.perform(delete("/SuperAdmin/deleteTeacher")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(faculty)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("S"))
                .andExpect(jsonPath("$.message").value("Teacher deleted successfully"));


    }
}
