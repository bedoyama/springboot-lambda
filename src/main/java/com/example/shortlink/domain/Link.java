package com.example.shortlink.domain;

import java.time.Instant;

public record Link(String code, String originalUrl, Instant createdAt, long clickCount) {

    public Link incrementClicks() {
        return new Link(code, originalUrl, createdAt, clickCount + 1);
    }
}
