package com.urlshortener.service;

import com.urlshortener.exception.AliasAlreadyExistsException;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.validate.InputValidator;
import com.urlshortener.validate.ValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

@Slf4j // Add Lombok Logger annotation
@Service
public class UrlMappingService {

    private final UrlMappingRepository repository;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public UrlMappingService(UrlMappingRepository repository,
                             @Qualifier("reactiveRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    @Value("${shortener.base-url}")
    private String baseUrl;

    @Value("${shortener.redis.cache-ttl-seconds}")
    private long cacheTtlSeconds;

    private static final String CHAR_POOL = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int SHORT_KEY_LENGTH = 6;
    private static final int MAX_RETRY = 5;

    public Mono<String> shortenUrl(String longUrl, String customAlias, Integer expiryDays) {

        try {
            ValidationResult validationResult = InputValidator.validateAndCleanInputs(longUrl, customAlias);
            longUrl = validationResult.cleanedLongUrl();
            customAlias = validationResult.cleanedCustomAlias();
        } catch (IllegalArgumentException e) {
            return Mono.error(e);
        }

        // Calculate expiry time
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(Duration.ofDays(
                expiryDays != null ? expiryDays : 30 // default to 30 days if not provided
        ));

        if (customAlias != null) {
            // User provided a custom alias; check if already exists
            String finalLongUrl = longUrl;
            String finalCustomAlias = customAlias;
            return repository.findByShortKey(customAlias)
                    .flatMap(existing -> Mono.<String>error(new AliasAlreadyExistsException("Custom alias already taken!")))
                    .switchIfEmpty(Mono.defer(() -> saveUrlMapping(finalLongUrl, finalCustomAlias, createdAt, expiresAt)));
        } else {
            // No custom alias provided; generate random short key
            String finalLongUrl1 = longUrl;
            return generateUniqueShortKey()
                    .flatMap(shortKey -> saveUrlMapping(finalLongUrl1, shortKey, createdAt, expiresAt));
        }
    }



    private Mono<String> saveUrlMapping(String longUrl, String shortKey, Instant createdAt, Instant expiresAt) {
        // Build UrlMapping object
        UrlMapping mapping = new UrlMapping();
        mapping.setShortKey(shortKey);
        mapping.setLongUrl(longUrl);
        mapping.setCreatedAt(createdAt);
        mapping.setExpiresAt(expiresAt);

        // Save UrlMapping to MongoDB
        return repository.save(mapping)
                .flatMap(saved ->
                        redisTemplate.opsForValue()
                                .set(saved.getShortKey(), saved.getLongUrl(), Duration.between(createdAt, expiresAt))
                                .onErrorResume(e -> {
                                    // If Redis set fails, log the error but continue
                                    log.error("Redis SET failed during saveUrlMapping for key [{}]: {}", saved.getShortKey(), e.getMessage());
                                    return Mono.empty();
                                })
                                .then(Mono.just(baseUrl + saved.getShortKey())) // <-- proper Mono<String> return
                );
    }

    public Mono<String> getLongUrl(String shortKey) {
        String normalizedKey = shortKey.toLowerCase();
        // Check in Redis
        return redisTemplate.opsForValue().get(normalizedKey)
                .onErrorResume(e -> {
                    // If Redis read fails, log and continue with fallback
                    log.error("Redis GET failed for key [{}]: {}", normalizedKey, e.getMessage());
                    return Mono.empty();
                })
                .flatMap(Mono::just) // If found in cache
                .switchIfEmpty(
                        // If not in Redis, fetch from DB
                        repository.findByShortKey(normalizedKey)
                                .flatMap(urlMapping -> {
                                    // Check if the URL has expired
                                    if (urlMapping.getExpiresAt() != null && Instant.now().isAfter(urlMapping.getExpiresAt())) {
                                        log.warn("Short URL [{}] has expired.", normalizedKey);
                                        return Mono.empty(); // Returning empty will trigger .defaultIfEmpty(404)
                                    }
                                    // Save in Redis for next time with TTL
                                    return redisTemplate.opsForValue()
                                            .set(normalizedKey, urlMapping.getLongUrl(), Duration.between(Instant.now(), urlMapping.getExpiresAt()))
                                            .onErrorResume(e -> {
                                                // If Redis SET fails
                                                log.error("Redis SET failed for key [{}]: {}", normalizedKey, e.getMessage());
                                                return Mono.empty();
                                            })
                                            .thenReturn(urlMapping.getLongUrl());
                                })
                )
                .doOnNext(longUrl ->
                        // Increment click count
                        incrementClickCount(normalizedKey)
                );
    }


    public Mono<Long> getClickCount(String shortKey) {
        String normalizedKey = shortKey.toLowerCase();
        return repository.findByShortKey(normalizedKey)
                .map(UrlMapping::getClickCount);
    }

    private Mono<String> generateUniqueShortKey() {
        // Try to generate a unique short key
        return tryGenerateShortKey(0);
    }

    //Collisions will be very rare because 62^6 = 56 billion combinations possible.
    private Mono<String> tryGenerateShortKey(int retryCount) {
        String shortKey = generateRandomShortKey();
        return repository.findByShortKey(shortKey)
                .flatMap(existing -> {
                    if (retryCount >= MAX_RETRY) {
                        // Retry limit reached, throw error
                        return Mono.error(new RuntimeException("Unable to generate unique short key after retries"));
                    }
                    // If key exists, retry again
                    log.warn("Short key collision detected: [{}], retrying {}/{}", shortKey, retryCount + 1, MAX_RETRY);
                    return tryGenerateShortKey(retryCount + 1);
                })
                .switchIfEmpty(Mono.just(shortKey)); // No collision, accept it
    }

    private String generateRandomShortKey() {
        // Generate random 6-character short key
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SHORT_KEY_LENGTH; i++) {
            sb.append(CHAR_POOL.charAt(random.nextInt(CHAR_POOL.length())));
        }
        return sb.toString();
    }

    private void incrementClickCount(String shortKey) {
        String normalizedKey = shortKey.toLowerCase();
        repository.findByShortKey(normalizedKey)
                .flatMap(urlMapping -> {
                    urlMapping.setClickCount(urlMapping.getClickCount() + 1);
                    return repository.save(urlMapping);
                })
                .subscribe(
                        success -> log.debug("Click count incremented for [{}]", normalizedKey),
                        error -> log.error("Failed to increment click count for [{}]: {}", normalizedKey, error.getMessage())
                );
    }




}
