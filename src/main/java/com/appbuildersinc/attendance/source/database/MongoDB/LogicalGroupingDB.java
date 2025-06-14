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

    static {
        try {
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(uri + "/?serverSelectionTimeoutMS=60000"))
                    .build();
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase("AttendEz");

            collection = database.getCollection("Logical Grouping");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


//    public Boolean insertLogicalGrouping(Map<String, Object> group, String dept, String email) {
//        String section = (String) group.get("section");
//        String degree = (String) group.get("degree");
//        String passout = (String) group.get("passout");
//        String advisorEmail = (String) group.get("advisorEmail");
//
//        if (!userdb.isEmailAllowed(advisorEmail)) {
//            return false; // Invalid advisor email
//        }
//        boolean isElective = (advisorEmail == null);
//
//
//        Document query = new Document("passout", passout).append("department", dept).append("degree", degree).append("section", section);
//
//
//        Document existing = collection.find(query).first();
//        List<String> classCodes = (List<String>) group.get("class-code");
//        List<String> regNumbers = (List<String>) group.get("registernumbers");
//        String electiveName = "";
//        if (isElective) {
//            for (String eleccode : (List<String>) group.get("class-code")) {
//                electiveName = electiveName + eleccode;
//            }
//        }
//        String groupcode = isElective ? dept + electiveName + passout : dept + passout + section;
//        // Validating and processing timetable
//        Map<String, List<Map<String, Object>>> timetable = (Map<String, List<Map<String, Object>>>) group.get("timetable");
//
//        // Validation 1: Check if every classCode used in timetable is listed
//        int valid = 1;
//        outer:
//        for (List<Map<String, Object>> periods : timetable.values()) {
//            for (Map<String, Object> period : periods) {
//                String code = (String) period.get("classCode");
//                if (!code.equals("_") && !classCodes.contains(code)) {
//                    valid = 0;
//                    break outer;
//                }
//            }
//        }
//        if (valid == 0) return false;
//
//        // Validation 2: Every classCode must appear at least once in the timetable
//        for (String code : classCodes) {
//            boolean found = false;
//            for (List<Map<String, Object>> periods : timetable.values()) {
//                for (Map<String, Object> period : periods) {
//                    if (code.equals(period.get("classCode"))) {
//                        found = true;
//                        break;
//                    }
//                }
//                if (found) break;
//            }
//            if (!found) return false;
//        }
//
//        Document doc2 = new Document("degree", degree)
//                .append("registernumbers", regNumbers)
//                .append("timetable", timetable)
//                .append("class-code", classCodes)
//                .append("groupcode", groupcode)
//                .append("department", dept)
//                .append("passout", passout)
//                .append("section", section);
//
//        if (!isElective) {
//            doc2.append("advisorEmail", advisorEmail);
//            userdb.updateClassAdvisorListByEmail(advisorEmail, regNumbers, groupcode);
//
//        }
//
//        if (existing != null) {
//            // Update existing group
//            for (String classCode : classCodes) {
//                // Assuming you have a registered class, update its timetable
//                if (classDB.classExists(classCode)) {
//                    classDB.refreshClassTimetable(groupcode,classCode);
//                }
//            }
//            return collection.updateOne(query, new Document("$set", doc2)).getModifiedCount() > 0;
//        } else {
//            collection.insertOne(doc2);
//            return true;
//        }
//    }

    public boolean insertOrUpdateLogicalGroupingToDB(Document doc, String degree, String dept, String passout, String section, List<String> classCodes, String groupcode) {

        Document query = new Document("passout", passout)
                .append("department", dept)
                .append("degree", degree)
                .append("section", section);

        Document existing = collection.find(query).first();

        if (existing != null) {
            // Update existing group
            return collection.updateOne(query, new Document("$set", doc)).getModifiedCount() > 0;
        } else {
            collection.insertOne(doc);
            return true;
        }
    }




    public List<Map<String, Object>> viewalllogicalgroupings(String dept) {
        List<Map<String, Object>> groupings = new ArrayList<>();
        Document doc1 = new Document("department", dept);
        for (Document doc2 : collection.find(doc1)) {
            groupings.add(new HashMap<>(doc2));
        }
        return groupings;
    }



    public Map<String, Object> getLogicalGroupByDeptAndCode(String dept, String groupcode) {
        Document query = new Document("department", dept).append("groupcode", groupcode);
        Document result = collection.find(query).first();
        return result != null ? result : null;
    }
    public boolean deleteLogicalGroupByDeptAndCode(String dept, String groupcode) {
        Document query = new Document("department", dept).append("groupcode", groupcode);
        return collection.deleteOne(query).getDeletedCount() > 0;
    }

    public Map<String, Object> getLogicalGroupingByCode(String groupcode) {
        Document query = new Document("groupcode", groupcode);
        Document group = collection.find(query).first();
        if (group != null) {
            return new HashMap<>(group);
        } else {
            return null; // Group not found
        }
    }
}


