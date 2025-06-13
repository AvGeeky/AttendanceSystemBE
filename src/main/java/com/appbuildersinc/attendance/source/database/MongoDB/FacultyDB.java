package com.appbuildersinc.attendance.source.database.MongoDB;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//FacultyDB is a repository class that handles database operations related to user management.
@Repository
public class FacultyDB {
    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();
    static String uri = dotenv.get("API_KEY");

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;
    private static MongoCollection<Document> studentsCollection;

    static {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri + "/?serverSelectionTimeoutMS=60000"))
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("AttendEz");
            collection = database.getCollection("Users");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Method to add details of a new faculty member
    public boolean updateUserDocumentByEmail(String emailId, String name, String department, String position, String mentor) {
        Document query = new Document("faculty_email", emailId);
        Document updateFields = new Document()
                .append("name", name)
                .append("department", department)
                .append("position", position)
                .append("mentor", mentor);
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

    public boolean updateClassAdvisorListByEmail(String emailId, List<String> classAdvisorList) {
        Document query = new Document("faculty_email", emailId);
        Document user = collection.find(query).first();
        List<String> existingList = user != null && user.get("class_advisor_list") != null
                ? new ArrayList<>((List<String>) user.get("class_advisor_list")) : new ArrayList<>();
        for (String regNo : classAdvisorList) {
            if (!existingList.contains(regNo)) {
                existingList.add(regNo);
            }
        }
        Document update = new Document("$set", new Document("class_advisor_list", existingList));
        return collection.updateOne(query, update).getModifiedCount() > 0;
    }
    // Method to remove register numbers from class_advisor_list by email
    public boolean removeClassAdvisorListByEmail(String emailId, List<String> regNosToRemove) {
        Document query = new Document("faculty_email", emailId);
        Document user = collection.find(query).first();
        List<String> existingList = user != null && user.get("class_advisor_list") != null
                ? new ArrayList<>((List<String>) user.get("class_advisor_list")) : new ArrayList<>();
        existingList.removeAll(regNosToRemove);
        Document update = new Document("$set", new Document("class_advisor_list", existingList));
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
    public Map<String, Object> getUserDetailsByEmail(String email) {
        Document query = new Document("faculty_email", email);
        return collection.find(query).first();
    }

    public List<Map<String,Object>> viewAllTeachers(String dept){
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
                   .append("class_advisor",(String)teacher.get("advisor"));

            collection.insertOne(doc2);
           return true;



       }
       else{

           Document doc3=new Document("department",dept)
                   .append("faculty_email",(String)teacher.get("email"))
                   .append("position",(String)teacher.get("position"))
                   .append("name",(String)teacher.get("name"))
                   .append("mentor",(String)teacher.get("mentor"))
                   .append("class_advisor",(String)teacher.get("advisor"));
           return collection.updateOne(doc,new Document("$set", doc3)).getModifiedCount()>0;
       }
    }


}