package com.victor.EventDrop.auth;


import com.victor.EventDrop.occupants.Occupant;
import com.victor.EventDrop.occupants.OccupantRepository;
import com.victor.EventDrop.rooms.RoomRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;



@Component
@Slf4j
@RequiredArgsConstructor
public class SessionFilter extends OncePerRequestFilter {

    private final OccupantRepository occupantRepository;
    private final RoomRepository roomRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        //Handle all favicon ico issues.
        if ("/favicon.ico".equals(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isAuthenticated = authentication != null && authentication.isAuthenticated();
        boolean isSseURI = requestURI.startsWith("/rooms") && "GET".equals(method);

        //Since SSE's hit my filter continuously just add checks to prevent flooding my logs with unneeded logs
        if (isSseURI && isAuthenticated) {
            filterChain.doFilter(request, response);
            return;
        }

        String sessionId = extractSessionIdFromCookie(request);

        if(sessionId == null || sessionId.isEmpty()){
            log.info("No session ID found or invalid format. Denying request.");
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        Occupant occupant = occupantRepository.findBySessionId(sessionId);

        if(occupant != null){
            if(!roomRepository.existsByRoomCode(occupant.getRoomCode())){
                SecurityContextHolder.clearContext();
            }else{
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(occupant, null , List.of(occupant.getOccupantRole()));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                redisTemplate.expire("occupant:" + sessionId, Duration.ofMinutes(5));
                log.info("Successfully authenticated user and refreshed session ID.");

            }

        }else{
            log.info("Occupant not found for the session ID");
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractSessionIdFromCookie(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if(cookies != null){
            for (Cookie cookie : request.getCookies()){
                if(cookie != null && cookie.getName().equals("SESSION_ID")){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

}