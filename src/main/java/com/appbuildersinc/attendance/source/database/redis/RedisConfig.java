// src/main/java/com/yourapp/config/RedisConfig.java
package com.appbuildersinc.attendance.source.database.redis;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.core.*;

@Configuration
public class RedisConfig {
    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();
    private String host = dotenv.get("REDIS_HOST");
    private int port = Integer.parseInt(dotenv.get("REDIS_PORT"));
    private String password = dotenv.get("REDIS_PASSWORD");
    
//    @Value("${spring.data.redis.host}")
//    private String host;
//
//    @Value("${spring.data.redis.port}")
//    private int port;
//
//    @Value("${spring.data.redis.password}")
//    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Standalone Redis configuration
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        cfg.setPassword(RedisPassword.of(password));

        // Build a Lettuce client configuration (default is auto, but explicit is safer)
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                // you can customize timeout here too
                .build();

        return new LettuceConnectionFactory(cfg, clientConfig);
    }

    @Bean
    public RedisTemplate<String,String> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String,String> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(factory);
        return tpl;
    }
}
