package com.urlshortener.controller;

import com.urlshortener.dto.BulkShortenRequestItem;
import com.urlshortener.dto.BulkShortenResponseItem;
import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.service.RateLimiterService;
import com.urlshortener.service.UrlMappingService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@RestController
@RequiredArgsConstructor
public class UrlMappingController {

    private final UrlMappingService urlMappingService;
    private final RateLimiterService rateLimiterService;

    /**
     * Shorten a long URL with optional custom alias
     *
     * curl -X POST http://localhost:8111/shorten \
     *      -H "Content-Type: application/json" \
     *      -d '{"longUrl": "https://google.com", "customAlias": "openai"}'
     */
    @PostMapping("/shorten")
    public Mono<ResponseEntity<String>> shortenUrl(@RequestBody ShortenRequest request) {
        return rateLimiterService.executeWithRateLimiter(
                urlMappingService.shortenUrl(request.getLongUrl(), request.getCustomAlias(), request.getExpiryDays())
        ).map(ResponseEntity::ok);
    }


    /**
     * Redirect to the original URL
     *
     * curl -i http://localhost:8111/{shortKey}
     */
    @GetMapping("/{shortKey}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String shortKey) {
        return urlMappingService.getLongUrl(shortKey)
                .map(longUrl -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setLocation(URI.create(longUrl));
                    return new ResponseEntity<Void>(headers, HttpStatus.FOUND);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get click analytics for a short URL
     *
     * curl http://localhost:8111/analytics/{shortKey}
     */
    @GetMapping("/analytics/{shortKey}")
    public Mono<ResponseEntity<Long>> getAnalytics(@PathVariable String shortKey) {
        return urlMappingService.getClickCount(shortKey)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Preview the original URL without redirecting
     *
     * curl http://localhost:8111/preview/{shortKey}
     */
    @GetMapping("/preview/{shortKey}")
    public Mono<ResponseEntity<String>> preview(@PathVariable String shortKey) {
        return urlMappingService.getLongUrl(shortKey)
                .map(longUrl -> ResponseEntity.ok(longUrl))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Bulk shorten multiple URLs
     *
     * curl -X POST http://localhost:8111/bulk-shorten \
     *      -H "Content-Type: application/json" \
     *      -d '[{"longUrl":"https://openai.com","customAlias":"openai","expiryDays":30},{"longUrl":"https://google.com"}]'
     */
    @PostMapping("/bulk-shorten")
    public Mono<ResponseEntity<List<BulkShortenResponseItem>>> bulkShorten(@RequestBody List<BulkShortenRequestItem> bulkRequests) {
        return Flux.fromIterable(bulkRequests)
                .flatMap(requestItem -> urlMappingService.shortenUrl(requestItem.getLongUrl(), requestItem.getCustomAlias(), requestItem.getExpiryDays())
                        .map(shortUrl -> new BulkShortenResponseItem(
                                requestItem.getLongUrl(),
                                shortUrl,
                                "SUCCESS",
                                null
                        ))
                        .onErrorResume(error -> Mono.just(new BulkShortenResponseItem(
                                requestItem.getLongUrl(),
                                null,
                                "FAILED",
                                error.getMessage()
                        )))
                )
                .collectList()
                .map(ResponseEntity::ok);
    }

    /**
     * Bulk shorten URLs via CSV upload
     *
     * curl -X POST http://localhost:8111/bulk-shorten/csv \
     *      -H "Content-Type: multipart/form-data" \
     *      -F "file=@urls.csv"
     */
    @PostMapping("/bulk-shorten/csv")
    public Mono<ResponseEntity<List<BulkShortenResponseItem>>> bulkShortenCsv(@RequestPart("file") MultipartFile file) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

            CSVParser csvParser = CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .parse(reader);

            List<BulkShortenRequestItem> bulkRequests = new ArrayList<>();

            List<BulkShortenResponseItem> invalidRows = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                try {
                    String longUrl = record.get("longUrl").trim();
                    if (longUrl.isBlank()) {
                        invalidRows.add(new BulkShortenResponseItem(
                                null,
                                null,
                                "FAILED",
                                "Missing longUrl in CSV row"
                        ));
                        continue;
                    }

                    BulkShortenRequestItem item = new BulkShortenRequestItem();
                    item.setLongUrl(longUrl);

                    if (record.isMapped("customAlias") && !record.get("customAlias").isBlank()) {
                        item.setCustomAlias(record.get("customAlias").trim());
                    }

                    if (record.isMapped("expiryDays") && !record.get("expiryDays").isBlank()) {
                        try {
                            item.setExpiryDays(Integer.parseInt(record.get("expiryDays").trim()));
                        } catch (NumberFormatException nfe) {
                            invalidRows.add(new BulkShortenResponseItem(
                                    longUrl,
                                    null,
                                    "FAILED",
                                    "Invalid expiryDays: must be an integer"
                            ));
                            continue;
                        }
                    }

                    bulkRequests.add(item);

                } catch (Exception e) {
                    invalidRows.add(new BulkShortenResponseItem(
                            null,
                            null,
                            "FAILED",
                            "Error parsing CSV row: " + e.getMessage()
                    ));
                }
            }

            return Flux.fromIterable(bulkRequests)
                    .flatMap(requestItem -> urlMappingService.shortenUrl(requestItem.getLongUrl(), requestItem.getCustomAlias(), requestItem.getExpiryDays())
                            .map(shortUrl -> new BulkShortenResponseItem(
                                    requestItem.getLongUrl(),
                                    shortUrl,
                                    "SUCCESS",
                                    null
                            ))
                            .onErrorResume(error -> Mono.just(new BulkShortenResponseItem(
                                    requestItem.getLongUrl(),
                                    null,
                                    "FAILED",
                                    error.getMessage()
                            )))
                    )
                    .concatWith(Flux.fromIterable(invalidRows)) // Add invalid parsed rows at the end
                    .collectList()
                    .map(ResponseEntity::ok);

        } catch (Exception e) {
            return Mono.error(new RuntimeException("Failed to process CSV file: " + e.getMessage()));
        }
    }


}
