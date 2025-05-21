package com.appbuildersinc.attendance.source.restcontroller;

import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class Functions {
    public ResponseEntity<String> helloworld() {
        return ResponseEntity.ok("Hello, World!");
    }
    public ResponseEntity<String> byeworld() {
        return ResponseEntity.ok("Bye, World!");
    }


}
