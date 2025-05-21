package com.appbuildersinc.attendance.source.restcontroller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <b>Standard HTTP Error Response Codes:</b>
 * <ul>
 *   <li><b>400 Bad Request:</b> The request is invalid or malformed.</li>
 *   <li><b>401 Unauthorized:</b> Authentication is required or has failed.</li>
 *   <li><b>403 Forbidden:</b> The user does not have permission to access the resource.</li>
 *   <li><b>404 Not Found:</b> The requested resource does not exist.</li>
 *   <li><b>405 Method Not Allowed:</b> The HTTP method is not supported for the resource.</li>
 *   <li><b>409 Conflict:</b> The request could not be completed due to a conflict (e.g., duplicate data).</li>
 *   <li><b>415 Unsupported Media Type:</b> The request media type is not supported.</li>
 *   <li><b>422 Unprocessable Entity:</b> The request is well-formed but contains semantic errors.</li>
 *   <li><b>429 Too Many Requests:</b> The user has sent too many requests in a given amount of time.</li>
 *   <li><b>500 Internal Server Error:</b> A generic server error.</li>
 *   <li><b>502 Bad Gateway:</b> The server received an invalid response from an upstream server.</li>
 *   <li><b>503 Service Unavailable:</b> The server is currently unable to handle the request.</li>
 * </ul>
 */
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
        return functionsService.byeworld();
    }



}