package com.victor.EventDrop.rooms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("room.join")
@Component
@Getter
@Setter
public class RoomJoinConfigProperties {
    private String queueName;
    private String routingKey;
    private String exchangeName;
}
