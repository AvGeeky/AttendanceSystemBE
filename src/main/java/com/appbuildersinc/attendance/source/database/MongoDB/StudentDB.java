package com.appbuildersinc.attendance.source.database.MongoDB;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.*;

import static com.appbuildersinc.attendance.source.Utilities.AuthenticationUtils.PasswordUtil.generateHmacPasscode;

@Repository
public class StudentDB {
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

            collection =database.getCollection("Students");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean doesRegisterNumberExist(String regno) {
        Document query = new Document("registerNumber", regno);
        return collection.find(query).first() != null;
    }
    public Map<String,Object> getStudentDetailsByEmail(String email){
        Document query=new Document("email",email);
        return collection.find(query).first();
    }

    public String getStudentNameByRegNo(String regno){
        Document query=new Document("registerNumber",regno);
        return collection.find(query).first().get("name").toString();
    }

    public Map<String,Object> getStudentDetailsByRegisterNumber(String regno){
        Document query=new Document("registerNumber",regno);
        Document ans =  collection.find(query).first();
        if (ans == null) {
            return null; // Return null if no student found
        }
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
        com.mongodb.client.result.UpdateResult result = collection.updateOne(
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
           collection.insertMany(studentdocs);
           return true;
       }
       else{
           return false;
       }
    }
    public List<Map<String,Object>> getListOfAllStudentDetails(String dept){
           List <Map<String,Object>> students =new ArrayList<>();
           Document doc1=new Document("department",dept);
           for(Document doc: collection.find(doc1)){
               students.add(new HashMap<>(doc));
           }
           return students;
    }
    public boolean addClassToRegisteredClasses(String regno, String className) {
        Document query = new Document("registerNumber", regno);
        Document student = collection.find(query).first();
        if (student == null) {
            return false;
        }
        List<String> registeredClasses = (List<String>) student.getOrDefault("registeredClasses", new ArrayList<String>());
        Set<String> registeredClassesSet = new HashSet<>(registeredClasses);
        if (!registeredClassesSet.add(className)) {
            return false;
        }
        registeredClasses.add(className);
        Document update = new Document("$set", new Document("registeredClasses", registeredClasses));
        collection.updateOne(query, update);
        return true;
    }

    public boolean removeClassFromRegisteredClasses(String regno, String classCode) {
        Document query = new Document("registerNumber", regno);
        Document student = collection.find(query).first();
        if (student == null) {
            return false;
        }
        List<String> registeredClasses = (List<String>) student.getOrDefault("registeredClasses", new ArrayList<String>());
        if (!registeredClasses.remove(classCode)) {
            return false;
        }
        Document update = new Document("$set", new Document("registeredClasses", registeredClasses));
        collection.updateOne(query, update);
        return true;
    }

    //

    public boolean removeGroupingFromRegisteredGroupings(Set<String> regnos, String logicalGrouping) {
        boolean updated = false;
        for (String regno : regnos) {
            Document query = new Document("registerNumber", regno);
            Document student = collection.find(query).first();
            if (student == null) {
                continue;
            }
            List<String> registeredGroupings = (List<String>) student.getOrDefault("registeredGroupings", new ArrayList<String>());
            if (!registeredGroupings.remove(logicalGrouping)) {
                continue;
            }
            Document update = new Document("$set", new Document("registeredGroupings", registeredGroupings));
            collection.updateOne(query, update);
            updated = true;
        }
        return updated;
    }

    public boolean addGroupingToRegisteredGroupings(List<String> regnos, String logicalGrouping) {
        boolean updated = false;
        for (String regno : regnos) {
            Document query = new Document("registerNumber", regno);
            Document student = collection.find(query).first();
            if (student == null) {
                continue;
            }
            List<String> registeredGroupings = (List<String>) student.getOrDefault("registeredGroupings", new ArrayList<String>());
            Set<String> registeredGroupingsSet = new HashSet<>(registeredGroupings);
            if (!registeredGroupingsSet.add(logicalGrouping)) {
                continue;
            }
            registeredGroupings.add(logicalGrouping);
            Document update = new Document("$set", new Document("registeredGroupings", registeredGroupings));
            collection.updateOne(query, update);
            updated = true;
        }
        return updated;
    }
    public boolean addGroupingToRegisteredGroupings(Set<String> regnos, String logicalGrouping) {
        boolean updated = false;
        for (String regno : regnos) {

            Document query = new Document("registerNumber", regno);
            Document student = collection.find(query).first();
            if (student == null) {
                continue;
            }
            List<String> registeredGroupings = (List<String>) student.getOrDefault("registeredGroupings", new ArrayList<String>());
            Set<String> registeredGroupingsSet = new HashSet<>(registeredGroupings);
            if (!registeredGroupingsSet.add(logicalGrouping)) {
                continue;
            }
            registeredGroupings.add(logicalGrouping);
            //System.out.println(regno+registeredGroupings);
            Document update = new Document("$set", new Document("registeredGroupings", registeredGroupings));
            collection.updateOne(query, update);
            updated = true;
        }
        return updated;
    }

    public List<String> getStudentRegisteredClasses(String email){
        Document query = new Document("email", email);
        Document student = collection.find(query).first();
        if (student == null) {
            return new ArrayList<>();
        }
        return (List<String>) student.getOrDefault("registeredClasses", new ArrayList<String>());
    }

    public List<String> getStudentRegisteredGroupings(String regno){
        Document query = new Document("registerNumber", regno);
        Document student = collection.find(query).first();
        if (student == null) {
            return new ArrayList<>();
        }
        return (List<String>) student.getOrDefault("registeredGroupings", new ArrayList<String>());
    }

    public Map<String, Map<String, Object>> getHMACPasscodesAndNames(List<String> regnos) {
        Map<String, Object> hmacPasscodes = new HashMap<>();
        Map<String, Object> regNoNameMap = new HashMap<>();

        for (String regno : regnos) {
            Document query = new Document("registerNumber", regno);
            Document student = collection.find(query).first();

            if (student == null) {
                continue;
            }

            regNoNameMap.put(regno, student.get("name").toString());
            hmacPasscodes.put(regno, student.get("hmacpasscode").toString());
        }

        Map<String, Map<String, Object>> result = new HashMap<>();
        result.put("hmacPasscodes", hmacPasscodes);
        result.put("regNoNameMap", regNoNameMap);

        return result;
    }
    public String getHMACPasscode(String regno){
        HashMap<String,Object> hmacPasscodes = new HashMap<>();
        Document query = new Document("registerNumber", regno);
        Document student = collection.find(query).first();
        return student.get("hmacpasscode").toString();
    }


    public Boolean removeStudent(String registernumber) {
       Document query=new Document("registerNumber",registernumber);
        return collection.deleteOne(query).getDeletedCount()>0;
    }
}
