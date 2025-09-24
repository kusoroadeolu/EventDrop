package com.victor.EventDrop.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
@Profile("prod")
public class RedisConnectionFactoryProdConfig {

    private final RedisCloudConfigProperties redisConfigProperties;
    private final AsyncTaskExecutor asyncTaskExecutor;

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        RedisStandaloneConfiguration standaloneConfiguration = new RedisStandaloneConfiguration();
        standaloneConfiguration.setPort(redisConfigProperties.getPort());
        standaloneConfiguration.setHostName(redisConfigProperties.getHost());
        standaloneConfiguration.setUsername(redisConfigProperties.getUsername());
        standaloneConfiguration.setPassword(redisConfigProperties.getPassword());

        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration
                .builder()
                .commandTimeout(Duration.ofSeconds(30))
                .build();

        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(standaloneConfiguration, clientConfiguration);
        lettuceConnectionFactory.setExecutor(asyncTaskExecutor);
        return lettuceConnectionFactory;
    }
}
