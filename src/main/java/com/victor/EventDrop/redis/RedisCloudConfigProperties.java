package com.victor.EventDrop.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Profile("prod")
@ConfigurationProperties("spring.redis.cloud")
public class RedisCloudConfigProperties {
    private String host;
    private int port;
    private String username;
    private String password;
}
