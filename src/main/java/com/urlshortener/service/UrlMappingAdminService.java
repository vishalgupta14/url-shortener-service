package com.urlshortener.service;

import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
public class UrlMappingAdminService {

    private final UrlMappingRepository repository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public UrlMappingAdminService(UrlMappingRepository repository,
                             @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }
    public Flux<UrlMapping> getAllUrls(int page, int size) {
        int skip = page * size;
        return repository.findAll()
                .skip(skip)
                .take(size);
    }

    public Mono<Void> deleteByShortKey(String shortKey) {
        String normalizedKey = shortKey.toLowerCase();

        return repository.findByShortKey(normalizedKey)
                .flatMap(url ->
                        repository.delete(url)
                                .then(redisTemplate.opsForValue().delete(normalizedKey).then())
                )
                .switchIfEmpty(Mono.error(new RuntimeException("Short key not found: " + normalizedKey)));
    }

    public Mono<Long> deleteExpiredUrls() {
        Instant now = Instant.now();

        return repository.findAll()
                .filter(mapping -> mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(now))
                .flatMap(expired ->
                        repository.delete(expired)
                                .then(redisTemplate.opsForValue().delete(expired.getShortKey().toLowerCase()))
                                .thenReturn(1L)
                )
                .reduce(Long::sum)
                .defaultIfEmpty(0L);
    }

}
