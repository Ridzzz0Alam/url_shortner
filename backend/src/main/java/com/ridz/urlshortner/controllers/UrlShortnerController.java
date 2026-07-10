package com.ridz.urlshortner.controllers;

import com.ridz.urlshortner.services.RateLimitService;
import com.ridz.urlshortner.services.UrlShortnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class UrlShortnerController {
    private final UrlShortnerService urlShortnerService;
    private final RateLimitService rateLimitService;

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(
            
    )

}
