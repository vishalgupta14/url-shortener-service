package com.urlshortener.validate;

import com.urlshortener.exception.InvalidAliasException;
import com.urlshortener.exception.InvalidUrlFormatException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class InputValidator {

    public ValidationResult validateAndCleanInputs(String longUrl, String customAlias) {
        if (longUrl == null || longUrl.isBlank()) {
            throw new InvalidUrlFormatException("Long URL cannot be empty!");
        }

        String trimmedLongUrl = longUrl.trim();

        if (!trimmedLongUrl.startsWith("http://") && !trimmedLongUrl.startsWith("https://")) {
            throw new InvalidUrlFormatException("Invalid URL format! URL must start with http:// or https://");
        }

        if (customAlias != null && !customAlias.isBlank()) {
            String normalizedAlias = customAlias.trim().toLowerCase();
            if (!normalizedAlias.matches("^[a-zA-Z0-9_-]+$")) {
                throw new InvalidAliasException("Custom alias contains invalid characters! Only letters, numbers, hyphens (-) and underscores (_) are allowed.");
            }
            return new ValidationResult(trimmedLongUrl, normalizedAlias);
        } else {
            return new ValidationResult(trimmedLongUrl, null);
        }
    }
}
