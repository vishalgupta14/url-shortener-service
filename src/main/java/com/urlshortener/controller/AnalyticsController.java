package com.urlshortener.controller;

import com.urlshortener.dto.AnalyticsSummary;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /* curl http://localhost:8111/admin/analytics/top-clicked?page=0&size=10 */
    @GetMapping("/top-clicked")
    public Flux<UrlMapping> getTopClicked(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return analyticsService.getTopClickedUrls(page, size);
    }

    /* curl http://localhost:8111/admin/analytics/summary */
    @GetMapping("/summary")
    public Mono<AnalyticsSummary> getSummary() {
        return analyticsService.getSummary();
    }

}
