package com.rajesh.urlshortener.service;

import com.rajesh.urlshortener.domain.ShortUrl;
import com.rajesh.urlshortener.persistence.ShortUrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ShortUrlRedirectService {

    private final ShortUrlRepository shortUrlRepository;
    private final Clock clock;

    public ShortUrlRedirectService(ShortUrlRepository shortUrlRepository, Clock clock) {
        this.shortUrlRepository = shortUrlRepository;
        this.clock = clock;
    }

    @Transactional
    public String resolveAndRecordClick(String code) {
        Instant now = clock.instant();
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(code)
                .filter(mapping -> mapping.getExpiresAt() == null || !mapping.getExpiresAt().isBefore(now))
                .orElseThrow(() -> new ShortUrlNotFoundException(code));

        // A single SQL update prevents concurrent redirects from overwriting each other's increments.
        // Recheck expiry atomically in case the mapping expires after the initial lookup.
        if (shortUrlRepository.incrementClickCountIfActive(code, now) != 1) {
            throw new ShortUrlNotFoundException(code);
        }

        return shortUrl.getOriginalUrl();
    }
}
