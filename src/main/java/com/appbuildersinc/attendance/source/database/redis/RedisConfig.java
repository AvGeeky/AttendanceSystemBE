package com.appbuildersinc.attendance.source.database.redis;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RedisConfig {

    static Dotenv dotenv = Dotenv.configure()
            .filename("apiee.env")
            .load();

    private final String host = dotenv.get("REDIS_HOST");
    private final int port = Integer.parseInt(dotenv.get("REDIS_PORT"));
    private final String password = dotenv.get("REDIS_PASSWORD");

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        cfg.setPassword(RedisPassword.of(password));
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().build();
        return new LettuceConnectionFactory(cfg, clientConfig);
    }

//    @Bean
//    public RedisTemplate<String, String> stringRedisTemplate(RedisConnectionFactory factory) {
//        RedisTemplate<String, String> tpl = new RedisTemplate<>();
//        tpl.setConnectionFactory(factory);
//        tpl.setKeySerializer(new StringRedisSerializer());
//        tpl.setValueSerializer(new StringRedisSerializer());
//        tpl.setHashKeySerializer(new StringRedisSerializer());
//        tpl.setHashValueSerializer(new StringRedisSerializer());
//        tpl.afterPropertiesSet();
//        return tpl;
//    }
}
