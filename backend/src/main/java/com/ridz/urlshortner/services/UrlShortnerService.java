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
import java.util.concurrent.TimeUnit;

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

    private void cacheUrl(String shortCode, String originalUrl) {
        try {
            redisTemplate.opsForValue().set("url:" + shortCode, originalUrl, cacheTtlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to cache URL for {}:{}", shortCode, e.getMessage());
        }
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < maxGenerationAttempts; attempt++) {
            String code = generateRandomBase62();
            if (!shortCodeExists(code)) {
                return code;
            }
        }
        throw new RuntimeException("Failed to generate unique short code after " + maxGenerationAttempts + " attempts");
    }

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

    private String getCachedUrl(String shortCode) {
        try {
            return (String) redisTemplate.opsForValue().get("url:" + shortCode);
        } catch (Exception e) {
            log.warn("Failed to cached URL for {}:{}", shortCode, e.getMessage());
            return null;
        }
    }

    private boolean deleteUrl(String shortCode) {
        UrlData urlData = urlMappings.get(shortCode);
        if (urlData != null) {
            urlData.setActive(false);
            deleteCacheUrl(shortCode);
            log.info("Delete URL: {}", shortCode);
            return true;
        }
        return false;
    }

    private void deleteCacheUrl(String shortCode) {
        try {
            redisTemplate.delete("url:" + shortCode);
        } catch (Exception e) {
            log.warn("Failed to cached URL for {}:{}", shortCode, e.getMessage());
        }
    }
}
