package com.rajesh.urlshortener.api;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreateShortUrlRequest(@NotBlank String url, Instant expiresAt) {
}
