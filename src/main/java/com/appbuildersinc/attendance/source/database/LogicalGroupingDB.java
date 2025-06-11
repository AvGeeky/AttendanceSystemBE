package com.appbuildersinc.attendance.source.database;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.InsertOneResult;
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

    public Boolean insertlogicalgrouping(Map<String, Object> group, String dept) {
        String section = (String) group.get("section");
        String degree = (String) group.get("degree");
        String passout = (String) group.get("passout");
        String advisor = (String) group.get("advisor");

        boolean isElective = (degree == null && advisor == null);
        System.out.println(isElective);
        Document query = new Document("passout", passout)
                .append("section",section);
        if (!isElective) {
            query.append("degree", degree);
        }

        Document existing = collection.find(query).first();
        System.out.println(existing!=null);
        // Cast timetable as a Map<String, List<String>>
        Map<String, List<String>> timetable = (Map<String, List<String>>) group.get("timetable");
        List<String> classcode = (List<String>) group.get("class-code");
        int flag = 1;
        for (List<String> periods : timetable.values()) {
            for (String classcode1 : periods) {
                if (!classcode1.equals("_") && classcode.indexOf(classcode1) == -1) {
                    flag = 0;
                    break;
                }
            }
            if (flag == 0) break;
        }
        if (flag == 0) return false;
        int flag1=1;
        for(int i=0;i<classcode.size();i++) {
            int count = 0;
            for (List<String> periods : timetable.values()) {

                    if (periods.indexOf(classcode.get(i)) != -1) {
                        count += 1;
                    }

            }
            if (count == 0) {
                flag1 = 0;
                break;
            }
        }
        if(flag1==0){
            return false;
        }
        String groupcode = isElective ? dept + section : dept + passout + section;

        Document doc2 = new Document("section", section)
                .append("registernumbers", group.get("registernumbers"))
                .append("timetable", timetable)
                .append("class-code", classcode)
                .append("groupcode", groupcode)
                .append("dept",dept)
                .append("passout", passout);
        if (!isElective) {
            doc2.append("degree", degree)
                    .append("advisor", advisor);
            Document filter=new Document("faculty_email",advisor);
            Document update =new Document("$set",new Document("advisor-list",group.get("registernumbers")));
            collection1.updateOne(filter,update);
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
