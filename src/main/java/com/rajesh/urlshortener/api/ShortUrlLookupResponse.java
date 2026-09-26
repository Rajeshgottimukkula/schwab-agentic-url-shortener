package com.rajesh.urlshortener.api;

import java.time.Instant;

public record ShortUrlLookupResponse(
        String code,
        String originalUrl,
        Instant expiresAt,
        long clickCount,
        String shortUrl
) {
}
