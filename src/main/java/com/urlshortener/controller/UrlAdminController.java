package com.urlshortener.controller;

import com.urlshortener.model.UrlMapping;
import com.urlshortener.service.UrlMappingAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UrlAdminController {

    private final UrlMappingAdminService adminService;

    /* curl http://localhost:8111/admin/urls?page=0&size=5 */
    @GetMapping("/urls")
    public Flux<UrlMapping> listUrls(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return adminService.getAllUrls(page, size);
    }

    /* curl -X DELETE http://localhost:8111/admin/urls/{shortKey} */
    @DeleteMapping("/urls/{shortKey}")
    public Mono<Void> deleteUrl(@PathVariable String shortKey) {
        return adminService.deleteByShortKey(shortKey);
    }

    /* curl -X DELETE http://localhost:8111/admin/expired */
    @DeleteMapping("/expired")
    public Mono<ResponseEntity<String>> deleteExpired() {
        return adminService.deleteExpiredUrls()
                .map(count -> ResponseEntity.ok("Deleted expired entries: " + count));
    }

}
