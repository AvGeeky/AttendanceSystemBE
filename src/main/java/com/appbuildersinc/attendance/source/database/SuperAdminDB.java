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

import java.util.HashMap;
import java.util.Map;

@Repository
public class SuperAdminDB {
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
            collection = database.getCollection("Super Admin");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getDeptbyEmail(String email) {
       Document doc=new Document("email",email);
       Document admin=collection.find(doc).first();
       if(admin!=null){
           return admin.getString("department");
       }
       return null;
    }
    public static Map<String,String> getNameDeptbyEmail(String email)  {
        Document doc=new Document("email",email);
        Document admin=collection.find(doc).first();
        if(admin!=null){
            Map m = new HashMap();
            m.put("Department",admin.getString("department"));
            m.put("Name",admin.getString("name"));
            return m;
        }
        return null;
    }

    public String getPasswordByEmail(String Email){
        Document doc=new Document("email",Email);
        Document admin=collection.find(doc).first();
        if(admin!=null){
            return admin.getString("password");

        }
        else{
            return null;
        }
    }


}
