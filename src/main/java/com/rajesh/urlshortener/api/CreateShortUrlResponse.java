package com.rajesh.urlshortener.api;

import java.time.Instant;

public record CreateShortUrlResponse(String code, String originalUrl, Instant expiresAt, String shortUrl) {
}
