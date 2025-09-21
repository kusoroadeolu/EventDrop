package com.victor.EventDrop.rooms.configproperties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("room.leave")
@Component
@Getter
@Setter
public class RoomLeaveConfigProperties {
    private String queueName;
    private String routingKey;
    private String exchangeName;
}
