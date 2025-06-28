package com.appbuildersinc.attendance.source.Utilities.jsonVerifier;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class JsonVerifier {
   public Map<String,Object> jsonbodycheck(List<String>keys,Map<String,Object> input){
       List<String> missingkeys=new ArrayList<>();
       Map<String,Object> response=new HashMap<>();
       for(String key:keys){
         Object value=input.get(key);
         if(!input.containsKey(key) || value==null){
             missingkeys.add(key);
         }
           if (value instanceof String && ((String) value).trim().isEmpty()) {
               missingkeys.add(key);
           }

           if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
               missingkeys.add(key);
           }

           if (value instanceof Collection && ((Collection<?>) value).isEmpty()) {
               missingkeys.add(key);
           }

       }
       if(missingkeys.size()!=0){
           response.put("status","E");
           response.put("details",missingkeys);
           response.put("message","these follwings keys were either found to be missing or empty or null");

           return response;
       }
       else{
           response.put("status","S");
           response.put("message","input body is perfect");
           return response;
       }

   }


}
