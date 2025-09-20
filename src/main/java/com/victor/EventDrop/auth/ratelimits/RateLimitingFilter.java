package com.victor.EventDrop.auth.ratelimits;

import com.victor.EventDrop.exceptions.RateLimitExceededException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${rate-limit.max-request-default}") //For other
    private int defaultMaxRequestPerMinute;

    @Value("${rate-limit.max-request-strict}") //For uploads, room creation, deletes
    private int strictMaxRequestPerMinute;


    private static final int DEFAULT_REDIS_EXPIRATION_DURATION = 5;

    /**
     * A filter which checks on every request to ensure an IP address hasnt exceeded their rate limit
     * */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String ipAddress = request.getRemoteAddr();
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        //Skip limits for SSE
        if (requestURI.startsWith("/rooms") && "GET".equals(method)) {
            log.info("Hitting rooms");
            filterChain.doFilter(request, response);
            return;
        }

        if ("127.0.0.1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info("Getting ip address: {}", ipAddress);
        try{
            validateRateLimitForIp(ipAddress, requestURI);
        }catch (RateLimitExceededException e){
            response.sendError(429, "Slow down. Too many requests...");
            return;
        }

        filterChain.doFilter(request, response);
    }


    private void validateRateLimitForIp(String ip, String requestUri) throws RateLimitExceededException {
        LocalDateTime now = LocalDateTime.now();
        int elapsedSeconds = now.getSecond();
        int currentMinute = now.getMinute();
        int lastMinute = currentMinute == 0 ? 59 : currentMinute - 1;

        Integer requestCountLastMinute = (Integer) redisTemplate.opsForValue().get(constructRedisKey(ip, lastMinute));

        if(requestCountLastMinute == null){
            requestCountLastMinute = 0;
        }

        String currentMinuteKey = constructRedisKey(ip, currentMinute);
        Long currentMinuteRequestCount = redisTemplate.opsForValue().increment(currentMinuteKey, 1);
        redisTemplate.expire(currentMinuteKey, DEFAULT_REDIS_EXPIRATION_DURATION, TimeUnit.MINUTES);

        if(currentMinuteRequestCount != null){
            double weightedAverage = (
                    (double)elapsedSeconds * currentMinuteRequestCount.doubleValue()
                            + (double) (60 - elapsedSeconds) * requestCountLastMinute.doubleValue()) / 60;
            log.info("Weighted average request: {}", weightedAverage);
            handleRequestValidationLogic(weightedAverage, requestUri);
        }
    }


    /**
     * Implements limits based on what endpoint the request is coming from
     * @param weightedAverage The weighted average of the number of requests between the last minute and the current one
     * @param requestUri The request uri/endpoint the request is coming from
     * */
    private void handleRequestValidationLogic(double weightedAverage, String requestUri){
        Set<String> strictlyProtectedPaths = Set.of("/rooms/create", "/files", "/files/batch");

        if(strictlyProtectedPaths.contains(requestUri) && weightedAverage > strictMaxRequestPerMinute){
            throw new RateLimitExceededException();
        }else if(weightedAverage > defaultMaxRequestPerMinute){
            throw new RateLimitExceededException();
        }
    }

    //Constructs the redis key
    private String constructRedisKey(String ip, Integer minute){
        return String.format("count#%s#%d", ip, minute);
    }
}
