package com.appbuildersinc.attendance.source.database.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/redis")
public class TestRedisController {

    @Autowired
    private RedisService redisService;

    @PostMapping("/set")
    public ResponseEntity<Map<String, String>> set(@RequestParam String key, @RequestParam String value, @RequestParam(defaultValue = "60") long ttl) {
        redisService.setValue(key, value, ttl);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Set key '" + key + "' with TTL " + ttl + "s");
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> get(@RequestParam String key) {
        String val = redisService.getValue(key);
        Map<String, Object> response = new HashMap<>();
        if (val != null) {
            response.put("key", key);
            response.put("value", val);
            response.put("status", "found");
        } else {
            response.put("message", "Key not found");
            response.put("key", key);
            response.put("status", "not_found");
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Map<String, String>> delete(@RequestParam String key) {
        boolean deleted = redisService.deleteKey(key); // Assuming deleteKey returns a boolean indicating success
        Map<String, String> response = new HashMap<>();
        response.put("message", "Deleted key " + key);
        response.put("status", deleted ? "success" : "not_found_or_failed");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exists")
    public ResponseEntity<Map<String, Object>> exists(@RequestParam String key) {
        boolean exists = redisService.hasKey(key);
        Map<String, Object> response = new HashMap<>();
        response.put("key", key);
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}