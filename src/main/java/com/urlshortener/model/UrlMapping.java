package com.urlshortener.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "url_mapping")
public class UrlMapping {

    @Id
    private String id;

    @Indexed(unique = true)
    private String shortKey;

    private String longUrl;

    private Instant createdAt;

    private Instant expiresAt;

    private long clickCount = 0;
}
