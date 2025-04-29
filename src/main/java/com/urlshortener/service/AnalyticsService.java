package com.urlshortener.service;

import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.dto.AnalyticsSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UrlMappingRepository repository;

    public Flux<UrlMapping> getTopClickedUrls(int page, int size) {
        int skip = page * size;
        return repository.findAll(Sort.by(Sort.Direction.DESC, "clickCount"))
                .skip(skip)
                .take(size);
    }

    public Mono<AnalyticsSummary> getSummary() {
        return repository.findAll()
                .collectList()
                .map(all -> {
                    long totalUrls = all.size();
                    long totalClicks = all.stream()
                            .mapToLong(url -> url.getClickCount() == 0 ? 0 : url.getClickCount())
                            .sum();
                    return new AnalyticsSummary(totalUrls, totalClicks);
                });
    }

}
