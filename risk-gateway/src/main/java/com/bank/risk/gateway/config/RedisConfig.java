package com.bank.risk.gateway.config;

import org.springframework.context.annotation.*;
import redis.clients.jedis.JedisPooled;

@Configuration
public class RedisConfig {
    @Bean
    public JedisPooled jedis() {
        return new JedisPooled("localhost", 6379);
    }
}
