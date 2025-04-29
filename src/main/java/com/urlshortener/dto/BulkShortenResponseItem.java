package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkShortenResponseItem {
    private String originalUrl;
    private String shortUrl;
    private String status;
    private String error; // null if success
}
