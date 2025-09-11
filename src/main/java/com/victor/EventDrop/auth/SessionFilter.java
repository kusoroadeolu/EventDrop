package com.victor.EventDrop.auth;





import com.victor.EventDrop.occupants.Occupant;

import com.victor.EventDrop.occupants.OccupantRepository;

import jakarta.servlet.FilterChain;

import jakarta.servlet.ServletException;

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

import java.util.List;

import java.util.concurrent.TimeUnit;



@Component
@Slf4j
@RequiredArgsConstructor
public class SessionFilter extends OncePerRequestFilter {

    private final OccupantRepository occupantRepository;
    private final RedisTemplate<String, Object> redisTemplate;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null && authentication.isAuthenticated()){
            log.info("User is already authenticated");
            filterChain.doFilter(request, response);
            return;

        }

        String authHeader = request.getHeader("Authorization");

        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            log.info("No auth header found or invalid format. Denying request.");
            filterChain.doFilter(request, response);
            return;
        }

        String sessionId = authHeader.substring(7);
        log.info("Attempting to authenticate user with session ID: {}", sessionId);

        Occupant occupant = occupantRepository.findBySessionId(sessionId);

        if(occupant != null){
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(occupant, null , List.of(occupant.getOccupantRole()));
            SecurityContextHolder.getContext().setAuthentication(authToken);

            //Refresh the session
            redisTemplate.expire(sessionId, 5L, TimeUnit.MINUTES);
            log.info("Successfully authenticated user and refreshed session ID.");
        }else{
            log.info("Occupant not found for the session ID");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

}