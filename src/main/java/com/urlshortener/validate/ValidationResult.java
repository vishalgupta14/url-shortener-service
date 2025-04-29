package com.urlshortener.validate;

public record ValidationResult(String cleanedLongUrl, String cleanedCustomAlias) {
}
