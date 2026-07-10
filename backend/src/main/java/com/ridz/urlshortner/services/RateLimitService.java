package com.ridz.urlshortner.services;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
    public boolean isAllowed(String clientIp) {
        return false;
    }
}
