package com.appbuildersinc.attendance.source.database.MongoDB;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.PasswordUtil.generateHmacPasscode;

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
    public Map<String,Object> getStudentDetailsByRegisterNumber(String regno){
        Document query=new Document("registerNumber",regno);
        Document ans =  studentsCollection.find(query).first();
        ans.remove("hmacpasscode"); // Remove sensitive information
        return ans;
    }
    public Boolean updateStudentDocumentsbyemail(String email, String name, String regno, String passout) {
        Document query = new Document("email", email);
        Document updateFields = new Document()
                .append("name", name)
                .append("registerNumber", regno)
                .append("passout", passout);
        Document update = new Document("$set", updateFields);
        com.mongodb.client.result.UpdateResult result = studentsCollection.updateOne(
                query, update, new com.mongodb.client.model.UpdateOptions().upsert(true));
        return result.getModifiedCount() > 0 || result.getUpsertedId() != null;
    }
    public Boolean insertStudentsByAdmin( List<Map <String,String>> studlist,String dept) throws Exception {
        List<Document> studentdocs=new ArrayList<>();
        for(Map<String,String> studlist1:studlist) {
           Document doc = new Document("email", studlist1.get("email"))
                   .append("name", studlist1.get("name"))
                   .append("registerNumber", studlist1.get("registerNumber"))
                   .append("department", dept)
                   .append("passout", studlist1.get("passout"))
                   .append("course", studlist1.get("course"))
                   .append("degree", studlist1.get("degree"))
                   .append("digitalid", studlist1.get("digitalid"))
                   .append("hmacpasscode", generateHmacPasscode(studlist1.get("email")));
           studentdocs.add(doc);
       }
       if(!studentdocs.isEmpty()){
           studentsCollection.insertMany(studentdocs);
           return true;
       }
       else{
           return false;
       }




    }
    public List<Map<String,Object>> getListOfAllStudentDetails(String dept){
           List <Map<String,Object>> students =new ArrayList<>();
           Document doc1=new Document("department",dept);
           for(Document doc:studentsCollection.find(doc1)){
               students.add(new HashMap<>(doc));
           }
           return students;
    }


}
