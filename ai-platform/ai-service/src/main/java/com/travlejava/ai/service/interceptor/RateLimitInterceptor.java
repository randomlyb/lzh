package com.travlejava.ai.service.interceptor;

import com.travlejava.ai.service.exception.RateLimitException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitInterceptor.class);
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final int WINDOW_SECONDS = 60;
    
    // Redis宕机时的本地降级限流(内存限流)
    private static final ConcurrentHashMap<String, AtomicLong> localRateLimitMap = new ConcurrentHashMap<>();
    private static volatile boolean redisAvailable = true;

    private final StringRedisTemplate redisTemplate;

    public RateLimitInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = getUserId(request);
        if (userId == null) {
            return true; // 未登录用户不限流
        }

        String endpoint = request.getRequestURI();
        String key = RATE_LIMIT_PREFIX + userId + ":" + endpoint;

        try {
            if (redisAvailable) {
                // 尝试使用Redis限流
                return checkRateLimitWithRedis(key, userId, endpoint);
            } else {
                // Redis不可用,使用本地内存限流降级
                return checkRateLimitWithLocalMemory(key, userId, endpoint);
            }
        } catch (Exception e) {
            // Redis异常,降级到本地限流
            if (redisAvailable) {
                logger.error("Redis限流失败,降级到本地限流: {}", e.getMessage());
                redisAvailable = false;
            }
            return checkRateLimitWithLocalMemory(key, userId, endpoint);
        }
    }

    /**
     * 使用Redis进行限流
     */
    private boolean checkRateLimitWithRedis(String key, String userId, String endpoint) {
        Long count = redisTemplate.opsForValue().increment(key);
        
        // 第一次请求,设置过期时间
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        // 超过限制
        if (count != null && count > MAX_REQUESTS_PER_MINUTE) {
            logger.warn("Rate limit exceeded for user: {} on endpoint: {}", userId, endpoint);
            throw new RateLimitException("请求过于频繁,请稍后再试");
        }

        return true;
    }

    /**
     * 使用本地内存进行限流(Redis宕机时的降级方案)
     */
    private boolean checkRateLimitWithLocalMemory(String key, String userId, String endpoint) {
        AtomicLong counter = localRateLimitMap.computeIfAbsent(key, k -> new AtomicLong(0));
        long count = counter.incrementAndGet();
        
        // 简单的时间窗口限流(每分钟重置)
        if (count > MAX_REQUESTS_PER_MINUTE) {
            logger.warn("本地限流触发 for user: {} on endpoint: {}", userId, endpoint);
            throw new RateLimitException("请求过于频繁,请稍后再试");
        }
        
        return true;
    }

    private String getUserId(HttpServletRequest request) {
        // 从请求头或Session中获取用户ID
        // 这里简化处理,实际应该从JWT Token中解析
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // 简化:从Token中提取userId(实际应该解析JWT)
            return "user"; // 占位符
        }
        return null;
    }
}