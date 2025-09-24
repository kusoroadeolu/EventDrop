package com.victor.EventDrop.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class RedisConnectionFactoryDevConfig {

    @Bean public RedisConnectionFactory lettuceConnectionFactory(){
        return new LettuceConnectionFactory();
    }

}
