package com.victor.EventDrop.rooms;

import com.victor.EventDrop.rooms.configproperties.CookieConfigProperties;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtils {

    private final CookieConfigProperties cookieConfigProperties;

    public Cookie setSessionCookie(String sessionId){
        Cookie cookie = new Cookie("SESSION_ID", sessionId);
        cookie.setHttpOnly(cookieConfigProperties.isHttpOnly());
        cookie.setSecure(cookieConfigProperties.isSecure());
        cookie.setPath("/");
        cookie.setMaxAge(cookieConfigProperties.getMaxAge());
        return cookie;
    }
}
