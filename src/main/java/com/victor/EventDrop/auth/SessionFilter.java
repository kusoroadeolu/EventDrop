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

        String path = request.getRequestURI();

        //Handle all favicon ico issues.
        if ("/favicon.ico".equals(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        if(authentication != null && authentication.isAuthenticated()){
            log.info("User is already authenticated");
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

        //Since SSE's hit my filter continuously just add checks to prevent flooding my logs with unneeded logs
        if(!"/rooms".equals(path)){
            log.info("Attempting to authenticate user with session ID: {}", sessionId);
        }


        Occupant occupant = occupantRepository.findBySessionId(sessionId);

        if(occupant != null){

            if(!roomRepository.existsByRoomCode(occupant.getRoomCode())){
                log.info("Room: {} does not exist again", occupant.getRoomCode());
                SecurityContextHolder.clearContext();

            }else{
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(occupant, null , List.of(occupant.getOccupantRole()));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                if(!"/rooms".equals(path)){
                    //Refresh the session
                    redisTemplate.expire("occupant:" + sessionId, Duration.ofMinutes(5));
                    log.info("Successfully authenticated user and refreshed session ID.");
                }
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