package com.urlshortener.config;

import com.urlshortener.service.MongoIndexCreatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final MongoIndexCreatorService mongoIndexCreatorService;

    @EventListener(ContextRefreshedEvent.class)
    public void initIndexes() {
        mongoIndexCreatorService.createIndexes()
                .subscribe();
    }
}
