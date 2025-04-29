package com.urlshortener.repository;

import com.urlshortener.model.UrlMapping;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface UrlMappingRepository extends ReactiveMongoRepository<UrlMapping, String> {
    Mono<UrlMapping> findByShortKey(String shortKey);
}
