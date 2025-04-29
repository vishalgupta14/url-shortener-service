package com.urlshortener.dto;

import lombok.Data;

@Data
public class ShortenRequest {
    private String longUrl;
    private String customAlias;
    private Integer expiryDays;
}
