package com.ridz.urlshortner.services;

import com.ridz.urlshortner.models.UrlData;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlShortnerService {
    private final RedisTemplate<String, Object> redisTemplate;

    private final Map<String, UrlData> urlMappings = new ConcurrentHashMap<>();

    @Value("${urlshortner.base-url}")
    private String baseUrl;

    @Value("${urlshortner.short-code.length}")
    private int shortCodeLength;

    @Value("${urlshortner.short-code.max-attempts}")
    private int maxGenerationAttempts;

    @Value("${urlshortner.cache.ttl-minutes}")
    private int cacheTtlMinutes;

    private static final String BASE_62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUWXYZ";

    private boolean shortCodeExists(String code) {
        return urlMappings.containsKey(code);
    }

    private String generateRandomBase62() {
        StringBuilder sb = new StringBuilder(shortCodeLength);
        for (int i = 0; i < shortCodeLength; i++) {
            int index = ThreadLocalRandom.current().nextInt(BASE_62_CHARS.length());

            sb.append(BASE_62_CHARS.charAt(index));
        }
        return sb.toString();
    }
}
