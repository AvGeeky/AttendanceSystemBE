package com.appbuildersinc.attendance.source.database.redis;

import com.appbuildersinc.attendance.source.database.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/redis")
public class TestRedisController {

    @Autowired
    private RedisService redisService;

    @PostMapping("/set")
    public String set(@RequestParam String key, @RequestParam String value, @RequestParam(defaultValue = "60") long ttl) {
        redisService.setValue(key, value, ttl);
        return "Set key '" + key + "' with TTL " + ttl + "s";
    }

    @GetMapping("/get")
    public String get(@RequestParam String key) {
        String val = redisService.getValue(key);
        return val != null ? val : "Key not found";
    }

    @DeleteMapping("/delete")
    public String delete(@RequestParam String key) {
        redisService.deleteKey(key);
        return "Deleted key " + key;
    }

    @GetMapping("/exists")
    public boolean exists(@RequestParam String key) {
        return redisService.hasKey(key);
    }
}
