package com.appbuildersinc.attendance.source.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

// ServerDBFns is a service class that handles database operations related to allowed emails.
@Service
public class ServerDBFns {
    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();
    static String uri = dotenv.get("API_KEY");

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;

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

    public static void setAllowedEmails(List<String> allowedEmails) {
        // Remove old allowed_emails document
        collection.deleteOne(new Document("doctype", "allowed_emails"));

        // Create new document with each email as a separate field
        Document doc = new Document("doctype", "allowed_emails");
        for (int i = 0; i < allowedEmails.size(); i++) {
            doc.append("email" + i, allowedEmails.get(i));
        }
        collection.insertOne(doc);
    }

    public static void main(String[] args) {
        List<String> sampleEmails = List.of(
                "saipranav2310324@ssn.edu.in",
                "murari2310237@ssn.edu.in"
        );
        setAllowedEmails(sampleEmails);

    }


}