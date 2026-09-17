package com.example.shortlink.api;

import java.time.Instant;

public record LinkStatsResponse(String originalUrl, Instant createdAt, long clickCount) {
}
