package com.appbuildersinc.attendance.source.database.MongoDB;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.*;

//FacultyDB is a repository class that handles database operations related to user management.
@Repository
public class FacultyDB {
//    static Dotenv dotenv = Dotenv.configure()
//            .filename("apiee.env")
//            .load();
//    static String uri = dotenv.get("API_KEY");

    static String uri = System.getenv("API_KEY");

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;


    static {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri))
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("AttendEz");
            collection = database.getCollection("Users");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Method to add details of a new faculty member
    public boolean updateUserDocumentByEmail(String emailId, String name, String department, String position) {
        Document query = new Document("faculty_email", emailId);
        Document updateFields = new Document()
                .append("name", name)
                .append("department", department)
                .append("position", position);

        Document update = new Document("$set", updateFields);
        return collection.updateOne(query, update).getModifiedCount() > 0;
    }

    public boolean updateMenteeListByEmail(String emailId, List<String> mentorList, String reset) {
        Document query = new Document("faculty_email", emailId);
        Document user = collection.find(query).first();
        if (reset.equalsIgnoreCase("true")) {
            // If reset is true, replace the existing list with the new one
            Document update = new Document("$set", new Document("mentee_list", mentorList));
            return collection.updateOne(query, update).getModifiedCount() > 0;
        }
        List<String> existingList = user != null && user.get("mentee_list") != null
                ? new ArrayList<>((List<String>) user.get("mentee_list")) : new ArrayList<>();
        for (String regNo : mentorList) {
            if (!existingList.contains(regNo)) {
                existingList.add(regNo);
            }
        }
        Document update = new Document("$set", new Document("mentee_list", existingList));
        return collection.updateOne(query, update).getModifiedCount() > 0;
    }

    public boolean updateClassAdvisorListByEmail(String emailId, List<String> classAdvisorList, String groupCode) {
        Document query = new Document("faculty_email", emailId);
        Document user = collection.find(query).first();

        Map<String, List<String>> classAdvisorMap = user != null && user.get("class_advisor_list") != null
                ? new HashMap<>((Map<String, List<String>>) user.get("class_advisor_list"))
                : new HashMap<>();

        classAdvisorMap.put(groupCode, classAdvisorList);

        Document update = new Document("$set", new Document("class_advisor_list", classAdvisorMap));
        return collection.updateOne(query, update).getModifiedCount() > 0;
    }
    // Method to remove register numbers from class_advisor_list by email
    public boolean removeClassAdvisorListByEmail(String emailId, String groupCodeToRemove) {
        Document query = new Document("faculty_email", emailId);
        Document user = collection.find(query).first();
        Map<String, List<String>> classAdvisorMap = user != null && user.get("class_advisor_list") != null
                ? new HashMap<>((Map<String, List<String>>) user.get("class_advisor_list"))
                : new HashMap<>();

        classAdvisorMap.remove(groupCodeToRemove);

        Document update = new Document("$set", new Document("class_advisor_list", classAdvisorMap));
        return collection.updateOne(query, update).getModifiedCount() > 0;
    }

    // Method to get a password given an email
    public String getPasswordByEmail(String email) {
        Document query = new Document("faculty_email", email);
        Document user = collection.find(query).first();
        return user != null ? user.getString("password") : null;
    }
    public List<String> getMenteeList(String email) {
        Document query = new Document("faculty_email", email);
        Document user = collection.find(query).first();
        return user != null ? (List<String>) user.get("mentee_list") : null;
    }
    // Method to update the password for a user given their email
    public boolean updatePasswordByEmail(String email, String newPassword) {
        Document query = new Document("faculty_email", email);
        Document update = new Document("$set", new Document("password", newPassword));
        if (collection.updateOne(query, update).getModifiedCount() > 0) {
            return true;
        } else {
            // If no document was updated, insert a new one
            Document newUser = new Document("faculty_email", email)
                    .append("password", newPassword);
            collection.insertOne(newUser);
            return true;
        }
    }

    // Method to check if a given faculty email is allowed
    public boolean isEmailAllowed(String email) {
        Document query = new Document("faculty_email", email);
        Document result = collection.find(query).first();
        return result != null;
    }
    // Method to get user details by email
    public boolean isFacultyMentor(String email) {
        Document query = new Document("faculty_email", email);
        query = collection.find(query).first();
        if (query.isEmpty()) {
            return false; // Return null if the email is empty
        }
        String mentorField = query.getString("mentor");
        if ((mentorField != null) && !mentorField.isEmpty()) {
            if (mentorField.equalsIgnoreCase("true")) {
                return true; // Return true if the mentor field is true
            } else {
                return false; // Return null if the mentor field is false
            }
        }
        else return false; // Return null if the mentor field is not present
    }



    // Method to get user details by email
    public Map<String, Object> getUserDetailsByEmail(String email) {
        Document query = new Document("faculty_email", email);
        return collection.find(query).first();
    }

    // Method to get user name by email
    public String getUserNameByEmail(String email) {
        Document query = new Document("faculty_email", email);
        Document user = collection.find(query).first();
        return user != null ? user.getString("name") : null;
    }

    public List<Map<String,Object>> viewAllTeachers(String dept){
        if (dept == null || dept.trim().isEmpty()) {
            return null;
        }

       // System.out.println("dept"+dept);
        Document query=new Document("department",dept);
       List<Map<String,Object>> teacherlist=new ArrayList<>();
       for(Document doc2:collection.find(query)){
           doc2.remove("password");
           doc2.remove("_id");
           teacherlist.add(new HashMap<>(doc2));
       }
       return teacherlist;


    }
    public Boolean addorUpdateTeachers(String dept,Map<String,Object> teacher){
       String email=(String)teacher.get("email");
       Document doc=new Document("faculty_email",email);
       Document found=collection.find(doc).first();
       if(found==null){
           Document doc2=new Document("department",dept)
                   .append("faculty_email",(String)teacher.get("email"))
                   .append("position",(String)teacher.get("position"))
                   .append("name",(String)teacher.get("name"))
                   .append("mentor",(String)teacher.get("mentor"))
                   .append("class_advisor","True");

            collection.insertOne(doc2);
           return true;



       }
       else{

           Document doc3=new Document("department",dept)
                   .append("faculty_email",(String)teacher.get("email"))
                   .append("position",(String)teacher.get("position"))
                   .append("name",(String)teacher.get("name"))
                   .append("mentor",(String)teacher.get("mentor"));

           return collection.updateOne(doc,new Document("$set", doc3)).getModifiedCount()>0;
       }
    }
    public Map<String, Object> getFacultyDetailsByEmail(String email) {
        Document query = new Document("faculty_email", email);
        Document faculty = collection.find(query).first();
        if (faculty != null) {
            faculty.remove("password");// Remove sensitive information
            return new HashMap<>(faculty);
        }
        return null;
    }
    public boolean addClassToFacultyClasses(String facultyEmail, String className) {
        Document query = new Document("faculty_email", facultyEmail);
        Document faculty = collection.find(query).first();
        if (faculty == null) {
            return false;
        }
        List<String> facultyClasses = (List<String>) faculty.getOrDefault("facultyClasses", new ArrayList<String>());
        Set<String> facultyClassesSet = new HashSet<>(facultyClasses);
        if (!facultyClassesSet.add(className)) {
            return false;
        }
        facultyClasses.add(className);
        Document update = new Document("$set", new Document("facultyClasses", facultyClasses));
        collection.updateOne(query, update);
        return true;
    }


    public boolean removeClassFromFacultyClasses(String facultyEmail, String classCode) {
        Document query = new Document("faculty_email", facultyEmail);
        Document faculty = collection.find(query).first();
        if (faculty == null) {
            return false;
        }
        List<String> facultyClasses = (List<String>) faculty.getOrDefault("facultyClasses", new ArrayList<String>());
        if (!facultyClasses.remove(classCode)) {
            return false;
        }
        Document update = new Document("$set", new Document("facultyClasses", facultyClasses));
        collection.updateOne(query, update);
        return true;
    }

    public List<String> getFacultyRegisteredClasses(String email){
        Document query = new Document("faculty_email", email);
        Document fac = collection.find(query).first();
        if (fac == null) {
            return new ArrayList<>();
        }
        return (List<String>) fac.getOrDefault("facultyClasses", new ArrayList<String>());
    }


    public Map<String,Object> getAdvisorList(String email) {
      Document query=new Document("faculty_email",email);
      //System.out.println(email);
      Document fac=collection.find(query).first();
      if(fac==null){
          return null;
      }
      return  (Map<String,Object>)fac.get("class_advisor_list");
    }

    public Boolean deleteFacultyByEmail(String email) {
      Document query=new Document("faculty_email",email);
      return collection.deleteOne(query).getDeletedCount()>0;

    }

    public Boolean removeRegNoFromAdvisorList(String registernumber,String email,String groupcode) {
       Document query=new Document("faculty_email",email);
       Document result=collection.find(query).first();
       Map<String,Object> advisorlist=(Map<String,Object>) result.get("class_advisor_list");
        Boolean done=((List<String>)advisorlist.get(groupcode)).remove(registernumber);
        if(done){
           return collection.updateOne(query,new Document("$set",new Document("class_advisor_list",advisorlist))).getModifiedCount()>0;

        }
        else{
            return false;
        }
    }
}