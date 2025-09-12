package com.victor.EventDrop.rooms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("room.expiry")
@Component
@Getter
@Setter
public class RoomExpiryConfigProperties {
    private String queueName;
    private String exchangeName;
    private String routingKey;
}
