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
@Repository
public class StudentDB {
    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();
    static String uri = dotenv.get("API_KEY");

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> studentsCollection;
    static {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri + "/?serverSelectionTimeoutMS=60000"))
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("AttendEz");

            studentsCollection=database.getCollection("Students");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String,Object> getStudentDetailsByEmail(String email){
        Document query=new Document("email",email);
        return studentsCollection.find(query).first();
    }
    public Boolean updateStudentDocumentsbyemail(String email, String name, String regno, String passout, List<String> classes, Map<String,Object>attendance){
        Document query=new Document("email",email);
        Document updateFields=new Document()
                .append("name",name)
                .append("registerNumber",regno)
                .append("passout",passout)
                .append("registeredClasses",classes)
                .append("attendance",attendance);
        Document update=new Document("$set",updateFields);
        return studentsCollection.updateOne(query,update).getModifiedCount()>0;

    }



}
