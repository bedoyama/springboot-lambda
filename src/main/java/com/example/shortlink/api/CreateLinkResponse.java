package com.example.shortlink.api;

public record CreateLinkResponse(String code, String shortUrl, String originalUrl) {
}
