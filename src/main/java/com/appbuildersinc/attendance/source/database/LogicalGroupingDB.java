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

import java.util.*;
@Repository
public class LogicalGroupingDB {
    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();
    static String uri = dotenv.get("API_KEY");

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;
    private static MongoCollection<Document> collection1;
    static {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri + "/?serverSelectionTimeoutMS=60000"))
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("AttendEz");

            collection=database.getCollection("Logical Grouping");
            collection1=database.getCollection("Users");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Boolean insertLogicalGrouping(Map<String, Object> group, String dept) {
        String section = (String) group.get("section");
        String degree = (String) group.get("degree");
        String passout = (String) group.get("passout");
        String advisor = (String) group.get("advisor");

        boolean isElective = (degree == null && advisor == null);
        String groupcode = isElective ? dept + section + passout : dept + passout + section;

        Document query = new Document("passout", passout).append("section", section);
        if (!isElective) {
            query.append("degree", degree);
        }

        Document existing = collection.find(query).first();
        List<String> classCodes = (List<String>) group.get("class-code");
        List<String> regNumbers = (List<String>) group.get("registernumbers");

        // Validating and processing timetable
        Map<String, List<Map<String, Object>>> timetable = (Map<String, List<Map<String, Object>>>) group.get("timetable");

        // Validation 1: Check if every classCode used in timetable is listed
        int valid = 1;
        outer:
        for (List<Map<String, Object>> periods : timetable.values()) {
            for (Map<String, Object> period : periods) {
                String code = (String) period.get("classCode");
                if (!code.equals("_") && !classCodes.contains(code)) {
                    valid = 0;
                    break outer;
                }
            }
        }
        if (valid == 0) return false;

        // Validation 2: Every classCode must appear at least once in the timetable
        for (String code : classCodes) {
            boolean found = false;
            for (List<Map<String, Object>> periods : timetable.values()) {
                for (Map<String, Object> period : periods) {
                    if (code.equals(period.get("classCode"))) {
                        found = true;
                        break;
                    }
                }
                if (found) break;
            }
            if (!found) return false;
        }

        Document doc2 = new Document("section", section)
                .append("registernumbers", regNumbers)
                .append("timetable", timetable)
                .append("class-code", classCodes)
                .append("groupcode", groupcode)
                .append("dept", dept)
                .append("passout", passout);

        if (!isElective) {
            doc2.append("degree", degree).append("advisor", advisor);
            Document filter = new Document("faculty_email", advisor);
            Document update = new Document("$set", new Document("advisor-list", regNumbers));
            collection1.updateOne(filter, update);
        }

        if (existing != null) {
            return collection.updateOne(query, new Document("$set", doc2)).getModifiedCount() > 0;
        } else {
            collection.insertOne(doc2);
            return true;
        }
    }

    public List<Map<String,Object>> viewalllogicalgroupings(String dept){
        List<Map<String,Object>> groupings =new ArrayList<>();
        Document doc1=new Document("dept",dept);
        for(Document doc2:collection.find(doc1)){
            groupings.add(new HashMap<>(doc2));
        }
        return groupings;

    }
    public boolean deletelogicalgroup(String dept,String groupcode){
        Document doc1=new Document("dept",dept)
                .append("groupcode",groupcode);
        return  collection.deleteOne(doc1).getDeletedCount()>0;


    }

}
