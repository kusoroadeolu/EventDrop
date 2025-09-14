package com.victor.EventDrop.rooms.configproperties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("room.file-delete")
@Component
@Getter
@Setter
public class RoomFileDeleteConfigProperties {
    private String queuePrefix;
    private String routingKeyPrefix;
    private String exchangeName;
}
