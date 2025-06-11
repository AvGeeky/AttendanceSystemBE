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

            collection=database.getCollection("Logical Grouping");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public Boolean insertlogicalgrouping(Map<String,Object> group,String dept){
        String passout=(String)group.get("passout");
        String degree=(String)group.get("degree");
        String section=(String)group.get("section");
        Document doc=new Document("degree",degree)
                .append("passout",passout)
                .append("section",section);
        Document doc1=collection.find(doc).first();
        if(doc1!=null){
            List<ArrayList<String>> timetable=new ArrayList<>();
            timetable=(ArrayList<ArrayList<String>>)group.get("timetable");
            List<String> classcode=new ArrayList<>();
            classcode=(ArrayList<String>)group.get("class-code");
            int flag=1;
            for(ArrayList<String> a:timetable){
                for (String classcode1:a){
                    if(!classcode1.equals("_") && classcode.indexOf(classcode1)==-1){
                        flag=0;
                        break;
                    }
                }

            }
            if(flag==0){
                return false;

            }
            else{
                String groupcode=dept+(String)group.get("passout")+(String)group.get("section");

                Document doc2=new Document("passout",passout)
                        .append("section",section)
                        .append("degree",degree)
                        .append("registernumbers",(HashSet<String>)group.get("registernumbers"))
                        .append("timetable",timetable)
                        .append("class-code",classcode)
                        .append("advisor",group.get("advisor"))
                        .append("groupcode",groupcode);
                return collection.updateOne(doc1,doc2).getModifiedCount() > 0;

            }



        }
        else{
           List<ArrayList<String>> timetable=new ArrayList<>();
           timetable=(ArrayList<ArrayList<String>>)group.get("timetable");
           List<String> classcode=new ArrayList<>();
           classcode=(ArrayList<String>)group.get("class-code");
           int flag=1;
           for(ArrayList<String> a:timetable){
               for (String classcode1:a){
                   if(!classcode1.equals("_") && classcode.indexOf(classcode1)==-1){
                      flag=0;
                      break;
                   }
               }

           }
           if(flag==0){
               return false;

           }
           else{
               String groupcode=dept+(String)group.get("passout")+(String)group.get("section");

               Document doc2=new Document("passout",passout)
                       .append("section",section)
                       .append("degree",degree)
                       .append("registernumbers",(HashSet<String>)group.get("registernumbers"))
                       .append("timetable",timetable)
                       .append("class-code",classcode)
                       .append("advisor",group.get("advisor"))
                       .append("groupcode",groupcode);

               collection.insertOne(doc2);
               return true;
           }

        }


    }

}
