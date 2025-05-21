package com.appbuildersinc.attendance.source.restcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    private final Functions functionsService;

    @Autowired
    public Controller(Functions functionsService) {
        this.functionsService = functionsService;
    }
    /**
     * This endpoint returns a simple "Hello, World!" message.
     *
     * @return A ResponseEntity containing the "Hello, World!" message.
     */
    @GetMapping("/helloworld")
    public ResponseEntity<String> setSecretId(){
        return functionsService.helloworld();
    }

}