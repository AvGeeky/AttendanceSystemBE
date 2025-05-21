package com.appbuildersinc.attendance.source.restcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    private final Functions functionsService;
    private final ResponseEntity<String> byeworld;

    @Autowired
    public Controller(Functions functionsService, ResponseEntity<String> byeworld) {
        this.functionsService = functionsService;
        this.byeworld = byeworld;
    }


    /**
     * This endpoint returns a simple "Hello, World!" message.
     *
     * @return A ResponseEntity containing the "Hello, World!" message.
     */
    @GetMapping("/helloworld")
    public ResponseEntity<String> helloWorld(){
        return functionsService.helloworld();
    }

    /**
     * This endpoint returns a simple "Bye, World!" message.
     *
     * @return A ResponseEntity containing the "Bye, World!" message.
     */
    @GetMapping("/byeworld")
    public ResponseEntity<String> byeWorld(){
        return byeworld;
    }



}