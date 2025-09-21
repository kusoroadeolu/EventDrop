package com.victor.EventDrop.rabbitmq;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("spring.rabbitmq.factory")
@Setter
@Getter
public class RabbitConfigProperties {
    private String host;
    private String username;
    private String virtualHost;
    private int port;
    private String password;
}
