package com.appbuildersinc.attendance.source.database.redis;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.*;


@Configuration
public class RedisConfig {

//    static Dotenv dotenv = Dotenv.configure()
//            .filename("apiee.env")
//            .load();

    private final String host = System.getenv("REDIS_HOST");
    private final int port = Integer.parseInt(System.getenv("REDIS_PORT"));
    private final String password = System.getenv("REDIS_PASSWORD");

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        cfg.setPassword(RedisPassword.of(password));
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().build();
        return new LettuceConnectionFactory(cfg, clientConfig);
    }

}
