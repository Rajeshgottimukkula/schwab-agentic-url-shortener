package com.rajesh.urlshortener.service;

import java.time.Instant;

/** Lookup result exposed to the API layer without exposing the persistence entity. */
public record LookedUpShortUrl(String code, String originalUrl, Instant expiresAt, long clickCount) {
}
