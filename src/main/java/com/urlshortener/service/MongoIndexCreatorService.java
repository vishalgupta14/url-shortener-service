package com.urlshortener.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class MongoIndexCreatorService {

    private final ReactiveMongoTemplate reactiveMongoTemplate;

    public Mono<Void> createIndexes() {
        // Ensure TTL index on "expiresAt" field in "url_mapping" collection
        return reactiveMongoTemplate.indexOps("url_mapping")
                .ensureIndex(
                        new Index()
                                .on("expiresAt", Sort.Direction.ASC)
                                .expire(0)
                )
                .doOnSuccess(indexName -> log.info("TTL index [{}] created/verified on url_mapping.", indexName))
                .doOnError(error -> log.error("Failed to create TTL index on url_mapping: {}", error.getMessage()))
                .then();
    }
}
