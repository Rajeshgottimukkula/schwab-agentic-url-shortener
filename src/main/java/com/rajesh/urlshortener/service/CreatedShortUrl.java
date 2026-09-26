package com.rajesh.urlshortener.service;

import java.time.Instant;

/** Creation result exposed to the API layer without exposing the persistence entity. */
public record CreatedShortUrl(String code, String originalUrl, Instant expiresAt) {
}
