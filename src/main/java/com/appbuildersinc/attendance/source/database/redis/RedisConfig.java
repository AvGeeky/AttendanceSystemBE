package com.appbuildersinc.attendance.source.database.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);
        redisStandaloneConfiguration.setPassword(RedisPassword.of(password));

        // IMPROVEMENT 1: Connection Pooling
        // This manages multiple connections so threads don't block each other
        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(4000))
                .poolConfig(buildPoolConfig())
                .build();

        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    }

    private org.apache.commons.pool2.impl.GenericObjectPoolConfig buildPoolConfig() {
        org.apache.commons.pool2.impl.GenericObjectPoolConfig poolConfig = new org.apache.commons.pool2.impl.GenericObjectPoolConfig();
        poolConfig.setMaxTotal(16); // Max active connections
        poolConfig.setMaxIdle(8);   // Max idle connections waiting
        poolConfig.setMinIdle(4);   // Min connections to keep alive
        return poolConfig;
    }
}
