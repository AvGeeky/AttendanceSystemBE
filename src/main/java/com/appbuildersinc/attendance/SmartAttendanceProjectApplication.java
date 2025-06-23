package com.appbuildersinc.attendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SmartAttendanceProjectApplication {
	// This is the main entry point for the Spring Boot application.
	public static void main(String[] args) {
		SpringApplication.run(SmartAttendanceProjectApplication.class, args);
	}

}
