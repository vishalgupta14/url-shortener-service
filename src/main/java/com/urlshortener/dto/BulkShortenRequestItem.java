package com.urlshortener.dto;

import lombok.Data;

@Data
public class BulkShortenRequestItem {
    private String longUrl;
    private String customAlias;   // optional
    private Integer expiryDays;   // optional
}
