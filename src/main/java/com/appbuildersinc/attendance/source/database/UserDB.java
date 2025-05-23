package com.appbuildersinc.attendance.source.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

//UserDB is a repository class that handles database operations related to user management.
@Repository
public class UserDB {
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
    public boolean updateUserDocumentByEmail(String emailId, String name, String department, String position, String mentor, String classAdvisor) {
        Document query = new Document("faculty_email", emailId);
        Document updateFields = new Document()
                .append("name", name)
                .append("department", department)
                .append("position", position)
                .append("mentor", mentor)
                .append("class_advisor", classAdvisor);
        Document update = new Document("$set", updateFields);
        return collection.updateOne(query, update).getModifiedCount() > 0;
    }

    // Method to get a password given an email
    public String getPasswordByEmail(String email) {
        Document query = new Document("faculty_email", email);
        Document user = collection.find(query).first();
        return user != null ? user.getString("password") : null;
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
        Document query = new Document("doctype", "allowed_emails");
        Document allowedDoc = collection.find(query).first();
        if (allowedDoc == null) return false;
        // Remove the doctype field before searching
        allowedDoc.remove("doctype");
        // Check if the email is present as a value
        return allowedDoc.containsValue(email);
    }

    // Method to get user details by email
    public Map<String, Object> getUserDetailsByEmail(String email) {
        Document query = new Document("faculty_email", email);
        return collection.find(query).first();
    }



}