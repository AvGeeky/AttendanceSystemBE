package com.appbuildersinc.attendance.source.database.MongoDB;
import com.appbuildersinc.attendance.source.functions.Class.FunctionsClass;
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

    public boolean saveRefreshedClassTimetable(String groupCode, String classCode, Map<String, List<Map<String, Object>>> newTimetable) {
        try {
            Document query = new Document("groupCode", groupCode)
                    .append("classCode", classCode);
            Document oldDoc = collection.find(query).first();
            if (oldDoc == null) return false;

            collection.updateOne(query, new Document("$set", new Document("timetable", newTimetable)));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Object> deleteClassAndReturnInfo( String classCode, String groupCode) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            Document query = new Document("groupCode", groupCode)
                    .append("classCode", classCode);
            Document doc = collection.find(query).first();
            if (doc == null) return null;
            result.put("regNumbers", doc.get("regNumbers"));
            result.put("facultyEmail", doc.getString("facultyEmail"));
            collection.deleteOne(query);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
