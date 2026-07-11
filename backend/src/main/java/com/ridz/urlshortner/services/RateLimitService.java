package com.ridz.urlshortner.services;

import com.ridz.urlshortner.models.RateLimitData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${url_shortner.rate-limit.requests-per-minute}")
    private int requestsPerMinute;

    @Value("${url_shortner.rate-limit.requests-per-hour}")
    private int requestsPerHour;

    private final ConcurrentHashMap<String, RateLimitData> rateLimitDate = new ConcurrentHashMap<>();

    private static final String REDIS_KEY_PREFIX = "rateLimit:";

    public boolean isAllowed(String clientIp) {
        String redisKey = REDIS_KEY_PREFIX + clientIp;
        // 192.168.1.100
        // ratelimit:192.168.1.10

        LocalDateTime now = LocalDateTime.now();

        RateLimitData data = getRateLimitDataFromRedis(redisKey);

        if (data == null) {
            data = rateLimitDate.computeIfAbsent(clientIp, k -> RateLimitData.builder().build());
        }

        return false;
    }
}
