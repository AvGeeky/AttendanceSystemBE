package com.appbuildersinc.attendance.source.database.MongoDB;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.cdimascio.dotenv.Dotenv;
import org.bson.Document;
import com.mongodb.client.model.IndexOptions;
import org.springframework.stereotype.Repository;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Repository
public class SubstitutionDB {
    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();
    static String uri = dotenv.get("API_KEY");
    private static final TimeZone IST = TimeZone.getTimeZone("Asia/Kolkata");
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
            collection = database.getCollection("Sub");


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method 1: Store substitution code with TTL
    public void storeSubstitutionCode(String code, String classCode, Date dateOfUse) {
        try {
            //Calendar instance for IST and time as 23.59.59.999
            Calendar cal = Calendar.getInstance(IST);
            cal.setTime(dateOfUse);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);

            Date expiresAt = cal.getTime();


            Document doc = new Document("code", code)
                    .append("classCode", classCode)
                    .append("timeOfUse", dateOfUse)
                    .append("expiresAt", expiresAt);

            collection.insertOne(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method 2: Fetch substitution code if not expired
    public String fetchClassCodeFromSubstitutionCode(String code) {
        try {
            Document query = new Document("code", code);
            Document doc = collection.find(query).first();

            if (doc != null && doc.containsKey("timeOfUse")) {
                Date dateOfUse = doc.getDate("timeOfUse");

                // Get start of today
                Calendar todayStart = Calendar.getInstance(IST);
                todayStart.set(Calendar.HOUR_OF_DAY, 0);
                todayStart.set(Calendar.MINUTE, 0);
                todayStart.set(Calendar.SECOND, 0);
                todayStart.set(Calendar.MILLISECOND, 0);

                // Get start of tomorrow
                Calendar todayEnd = (Calendar) todayStart.clone();
                todayEnd.add(Calendar.DAY_OF_MONTH, 1);

                if (dateOfUse.compareTo(todayStart.getTime()) >= 0 &&
                        dateOfUse.compareTo(todayEnd.getTime()) < 0) {
                    return doc.get("classCode").toString(); // valid today
                }
            }

            return null; // not found or not today's code

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //boolean check = substitutionDBclass.fetchAndVerifySubstitutionCode(substitutionCode,classCode);
    // If substitution code is valid today, and if classCode matches, return true
    public boolean fetchAndVerifySubstitutionCode(String Subcode, String classCode) {
        try {
            Document query = new Document("code", Subcode);
            Document doc = collection.find(query).first();

            if (doc != null && doc.containsKey("timeOfUse")) {
                Date dateOfUse = doc.getDate("timeOfUse");

                // Get start of today
                Calendar todayStart = Calendar.getInstance();
                todayStart.set(Calendar.HOUR_OF_DAY, 0);
                todayStart.set(Calendar.MINUTE, 0);
                todayStart.set(Calendar.SECOND, 0);
                todayStart.set(Calendar.MILLISECOND, 0);

                // Get start of tomorrow
                Calendar todayEnd = (Calendar) todayStart.clone();
                todayEnd.add(Calendar.DAY_OF_MONTH, 1);

                if (dateOfUse.compareTo(todayStart.getTime()) >= 0 &&
                        dateOfUse.compareTo(todayEnd.getTime()) < 0) {
                        if ( doc.get("classCode").toString().equalsIgnoreCase(classCode)) {
                            return true;
                        }// valid today and class code matches
                        else return false; // class code does not match
                }
            }
        return false; // not found
        } catch (Exception e) {
            e.printStackTrace();
            return false; // error occurred
        }
    }

    // Method 4: Delete substitution code by code value
    public boolean deleteSubstitutionCode(String code) {
        try {
            Document query = new Document("code", code);
            return collection.deleteOne(query).getDeletedCount() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
