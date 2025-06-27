package com.appbuildersinc.attendance.source.database.MongoDB;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
@Repository
public class LogicalGroupingDB {

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

            collection = database.getCollection("Logical Grouping");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
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

    public boolean updateLogicalGroupingInDB(Document doc, String groupcode) {
        Document query = new Document("groupcode", groupcode);
        Document existing = collection.find(query).first();

        if (existing != null) {
            // Update existing group
            return collection.updateOne(query, new Document("$set", doc)).getModifiedCount() > 0;
        } else {
            return false;
        }
    }

    public boolean insertLogicalGroupingToDB(Document doc) {
        collection.insertOne(doc);
        return true;
    }


   /* public List<Map<String, Object>> viewalllogicalgroupings() {
        List<Map<String, Object>> groupings = new ArrayList<>();
        for (Document doc : collection.find()) {
            groupings.add(new HashMap<>(doc));
        }
        return groupings;
    }*/

    public List<Map<String, Object>> viewalllogicalgroupings(String dept) {
        List<Map<String, Object>> groupings = new ArrayList<>();
        Document doc1 = new Document("department", dept);
        for (Document doc2 : collection.find(doc1)) {
            groupings.add(new HashMap<>(doc2));
        }
        return groupings;
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

    public Boolean removeRegNofromGrouping(String registernumber,String groupcode) {
        Document query = new Document("groupcode", groupcode);
        Document group = collection.find(query).first();
        List<String> registernumbers=(List<String>)group.get("registernumbers");
        Boolean done=registernumbers.remove(registernumber);
        if(done){
            //System.out.println("Register number removed from grouping");
            return collection.updateOne(query,new Document("$set",new Document("registernumbers",registernumbers))).getModifiedCount()>0;
        }
        else{
            return false;
        }
    }
    public String getAdvisorEmail(String groupcode){
        Document query=new Document("groupcode",groupcode);
        Document group=collection.find(query).first();
        return (String)group.get("advisorEmail");
    }
}


