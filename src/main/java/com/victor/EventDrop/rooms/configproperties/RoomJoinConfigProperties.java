package com.victor.EventDrop.rooms.configproperties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("room.join")
@Component
@Getter
@Setter
public class RoomJoinConfigProperties {
    private String queuePrefix;
    private String routingKeyPrefix;
    private String exchangeName;
}
