package com.urlshortener.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RateLimiter shortenerRateLimiter(io.github.resilience4j.ratelimiter.RateLimiterRegistry registry) {
        return registry.rateLimiter("shortener");
    }
}
