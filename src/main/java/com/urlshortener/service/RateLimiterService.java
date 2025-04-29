package com.urlshortener.service;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final RateLimiter shortenerRateLimiter;

    public <T> Mono<T> executeWithRateLimiter(Mono<T> mono) {
        return mono.transformDeferred(RateLimiterOperator.of(shortenerRateLimiter));
    }
}
