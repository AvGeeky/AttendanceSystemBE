package com.appbuildersinc.attendance.source.restcontroller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {
    @GetMapping("/hi")
    public String index() {
        return "Hello World";
    }
}