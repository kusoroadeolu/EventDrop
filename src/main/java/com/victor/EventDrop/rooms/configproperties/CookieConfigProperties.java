package com.victor.EventDrop.rooms.configproperties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("cookies.session")
@Component
@Getter
@Setter
public class CookieConfigProperties {
    private boolean secure;
    private boolean httpOnly;
    private int maxAge;
}
