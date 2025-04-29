package com.urlshortener.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnalyticsSummary {
    private long totalUrls;
    private long totalClicks;
}
