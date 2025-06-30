package com.appbuildersinc.attendance.source.database.MongoDB;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import org.bson.Document;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ClassDB {
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
            collection = database.getCollection("Class");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean createNewClass(
            String groupCode, String classCode, String dept,
            String className, String facultyName, String passoutYear, String facultyEmail,
            String credits, Map<String, List<Map<String, Object>>> newTimetable, List<String> regNumbers,
            String noOfStudents, Map<String,Object> regnoHMACMap, Map<String,Object> regnoNameMap
    ) {
        Document query = new Document("classCode", classCode);
        Document classDoc = new Document("groupCode", groupCode)
                .append("classCode", classCode)
                .append("dept", dept)
                .append("className", className)
                .append("facultyName", facultyName)
                .append("passoutYear", passoutYear)
                .append("facultyEmail", facultyEmail)
                .append("credits", credits)
                .append("timetable", newTimetable)
                .append("regnoHMACMap", regnoHMACMap)
                .append("regNumbers", regNumbers)
                .append("regnoNameMap", regnoNameMap);

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

    public Map<String,Object> getAllClassDetails(String classCode) {
        try {
            Document query = new Document("classCode", classCode);
            Document existing = collection.find(query).first();
            return existing;
        } catch (Exception e) {

            return null;
        }
    }

    public Map<String,Object> getClassDetailsWithoutAttendanceAndRegNo(String classCode) {
        try {
            Document query = new Document("classCode", classCode);
            Document existing = collection.find(query).first();
            Map<String,Object> classDetails = new java.util.HashMap<>();
            classDetails.put("classCode", existing.getString("classCode"));
            classDetails.put("groupCode", existing.getString("groupCode"));
            classDetails.put("department", existing.getString("dept"));
            classDetails.put("className", existing.getString("className"));
            classDetails.put("facultyName", existing.getString("facultyName"));
            classDetails.put("facultyEmail", existing.getString("facultyEmail"));
            classDetails.put("credits", existing.getString("credits"));
            classDetails.put("passoutYear", existing.getString("passoutYear"));
            return classDetails;
        } catch (Exception e) {

            return null;
        }
    }

    public Map<String,Object> getClassDetailsWithoutAttendance(String classCode) {
        try {
            Document query = new Document("classCode", classCode);
            Document existing = collection.find(query).first();
            Map<String,Object> classDetails = new java.util.HashMap<>();
            classDetails.put("classCode", existing.getString("classCode"));
            classDetails.put("groupCode", existing.getString("groupCode"));
            classDetails.put("department", existing.getString("dept"));
            classDetails.put("className", existing.getString("className"));
            classDetails.put("facultyName", existing.getString("facultyName"));
            classDetails.put("facultyEmail", existing.getString("facultyEmail"));
            classDetails.put("credits", existing.getString("credits"));
            classDetails.put("passoutYear", existing.getString("passoutYear"));
            classDetails.put("regNumbers", existing.getList("regNumbers", String.class));
            return classDetails;
        } catch (Exception e) {

            return null;
        }
    }

    public Map<String, List<Map<String, Object>>> getClassTimetable(String classCode) {
        try {
            Document query = new Document("classCode", classCode);
            Document existing = collection.find(query).first();
            return (Map<String, List<Map<String, Object>>>) existing.get("timetable");
        } catch (Exception e) {
            return null;
        }
    }

    public String getFacultyEmailFromClass(String classCode) {
        try {
            Document query = new Document("classCode", classCode);
            Document existing = collection.find(query).first();
            return (String) existing.get("facultyEmail");
        } catch (Exception e) {

            return null;
        }
    }

    public boolean saveRefreshedClassTimetable(String classCode, Map<String, List<Map<String, Object>>> newTimetable) {
        try {
            Document query = new Document("classCode", classCode);
            Document oldDoc = collection.find(query).first();
            if (oldDoc == null) return false;

            collection.updateOne(query, new Document("$set", new Document("timetable", newTimetable)));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Object> deleteClassAndReturnInfo( String classCode) {
        Map<String, Object> result = new java.util.HashMap<>();
        try {
            Document query = new Document("classCode", classCode);
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

    public boolean updateRegisterNumbers(String classCode, List<String> regNumbers){
        try {
            Document query = new Document("classCode", classCode);
            Document doc = collection.find(query).first();
            if (doc == null) return false;
            collection.updateOne(doc, new Document("$set", new Document("regNumbers", regNumbers)));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public boolean updateClassFacultyDetails(String classCode, String newFacEmail, String newFacName) {
        try {
            Document query = new Document("classCode", classCode);
            Document oldDoc = collection.find(query).first();
            if (oldDoc == null) return false;

            collection.updateOne(query, new Document("$set", new Document("facultyEmail", newFacEmail)));
            collection.updateOne(query, new Document("$set", new Document("facultyName", newFacName)));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateClassRegNoHmacNameMapping(String classCode, Map<String,Object> regnoHMACMap, Map<String,Object> regnoNameMap) {
        try {
            Document query = new Document("classCode", classCode);
            Document oldDoc = collection.find(query).first();
            if (oldDoc == null) return false;
            Document updateFields = new Document()
                    .append("regnoHMACMap", regnoHMACMap)
                    .append("regnoNameMap", regnoNameMap);
            collection.updateOne(query, new Document("$set", updateFields));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String,Object> getClassRegNoHmacMapping(String classCode) {
        try {
            Document query = new Document("classCode", classCode);
            Document oldDoc = collection.find(query).first();
            if (oldDoc == null) return null;

            Document regnoHmacDoc = (Document) oldDoc.get("regnoHMACMap");
            if (regnoHmacDoc == null) return null;

            return new HashMap<>(regnoHmacDoc); // Convert Document to HashMap
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Map<String,Object> getClassRegNoNameMapping(String classCode) {
        try {
            Document query = new Document("classCode", classCode);
            Document oldDoc = collection.find(query).first();
            if (oldDoc == null) return null;

            Document regnoNameDoc = (Document) oldDoc.get("regnoNameMap");
            if (regnoNameDoc == null) return null;

            return new HashMap<>(regnoNameDoc); // Convert Document to HashMap
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addAttendanceRecord(String classId, Map<String, Integer> lectureRecord) {
        try {
            Document query = new Document("classCode", classId);
            Document existing = collection.find(query).first();
            if (existing == null) return;

            Map<String,Map<String, Integer>> attendance = (Map<String, Map<String, Integer>>) existing.get("attendance");
            if (attendance == null) {
                attendance = new HashMap<>();
            }

            int nextLectureNumber = 1;

            if (!attendance.isEmpty()) {
                nextLectureNumber = attendance.keySet().stream()
                        .map(key -> key.replace("lecture.", ""))   // Remove prefix
                        .mapToInt(Integer::parseInt)               // Parse number part
                        .max()
                        .orElse(0) + 1;

            }

            // Update using dot notation to add lecture record
            String lectureKey = "lecture." + nextLectureNumber;
            attendance.put(lectureKey, lectureRecord);
            collection.updateOne(query, new Document("$set", new Document("attendance", attendance)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public Map<String, String> getregistermap(String classcode) {
        Document query = new Document("classCode", classcode);
        Document result = collection.find(query).first();
        if (result == null || result.get("regnoNameMap") == null) {
            return null;
        }
        return (Map<String, String>) result.get("regnoNameMap");
    }

    public Map<String, Object> getAttendance(String classCode) {
        try {
            Document query = new Document("classCode", classCode);
            Document existing = collection.find(query).first();
            if (existing == null) return null;
            Document attendanceDoc = (Document) existing.get("attendance");
            if (attendanceDoc == null) return null;
            return (Map<String, Object>) attendanceDoc;
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean updateAttendance(String classcode, Map<String, Object> classattendance) {
        if (classcode == null || classattendance == null) {
            return false;
        }
        Document query = new Document("classCode", classcode);
        if (collection == null) {
            return false;
        }
        return collection.updateOne(query, new Document("$set", new Document("attendance", classattendance))).getModifiedCount() > 0;
    }

    public Boolean deleteStudentFromClass(String registernumber,String classcode){
        Document query=new Document("classCode",classcode);
        Document result=collection.find(query).first();
        List<String> registernumbers=(List<String>)result.get("regNumbers");
        Boolean regremoval=registernumbers.remove(registernumber);
        Map<String,String> regnohmacmap=(Map<String,String>)result.get("regnoHMACMap");
         String value=regnohmacmap.remove(registernumber);
         Map<String,String> regnonamemap=(Map<String,String>) result.get("regnoNameMap");
         String value1=regnonamemap.remove(registernumber);
         if(regremoval && !value.isEmpty() && !value1.isEmpty()){
            return  collection.updateOne(query,new Document("$set",new Document("regnoHMACMap",regnohmacmap)
                     .append("regnoNameMap",regnonamemap)
                     .append("regNumbers",registernumbers)
             )).getModifiedCount()>0;
         }
         else{
             return false;
         }
    }

    public String getLogicalGroupingFromClassCode(String classcode) {
        Document query=new Document("classCode",classcode);
        Document result=collection.find(query).first();
        return (String) result.get("groupCode");


    }
}
