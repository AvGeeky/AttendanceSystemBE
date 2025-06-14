package com.appbuildersinc.attendance.source.database.MongoDB;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ClassDB {
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
            collection = database.getCollection("Class");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Autowired
    LogicalGroupingDB logicalGroupingDB;

    public boolean createNewClass(
            String groupCode, String classCode, String dept,
            String className, String facultyName, String passoutYear, String facultyEmail,
            String credits, Map<String, List<Map<String, Object>>> newTimetable, List<String> regNumbers,
            String noOfStudents
    ) {
        Document query = new Document("groupCode", groupCode)
                .append("classCode", classCode);

        Document classDoc = new Document("groupCode", groupCode)
                .append("classCode", classCode)
                .append("dept", dept)
                .append("className", className)
                .append("facultyName", facultyName)
                .append("passoutYear", passoutYear)
                .append("facultyEmail", facultyEmail)
                .append("credits", credits)
                .append("timetable", newTimetable)
                .append("regNumbers", regNumbers)
                .append("noOfStudents", noOfStudents);

        try {
            Document existing = collection.find(query).first();
            if (existing == null) {
                collection.insertOne(classDoc);
            } else {
                String existingEmail = existing.getString("facultyEmail");
                if (existingEmail == null || !existingEmail.equals(facultyEmail)) {
                    return false;
                }
                collection.updateOne(query, new Document("$set", classDoc));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean classExists(String classCode) {
        try {
            Document query = new Document("classCode", classCode);
            Document existing = collection.find(query).first();
            return existing != null;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean refreshClassTimetable(String groupCode, String classCode) {
        try {
            Document query = new Document("groupCode", groupCode)
                    .append("classCode", classCode);
            Document oldDoc = collection.find(query).first();
            if (oldDoc == null) return false;

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

            collection.updateOne(query, new Document("$set", new Document("timetable", newTimetable)));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
