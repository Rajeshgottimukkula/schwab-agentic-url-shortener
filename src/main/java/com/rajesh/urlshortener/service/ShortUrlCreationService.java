package com.rajesh.urlshortener.service;

import com.rajesh.urlshortener.domain.DestinationUrl;
import com.rajesh.urlshortener.domain.ShortCodeGenerator;
import com.rajesh.urlshortener.domain.ShortUrl;
import com.rajesh.urlshortener.persistence.ShortUrlRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;

@Service
public class ShortUrlCreationService {

    private static final int MAX_CODE_ATTEMPTS = 5;
    private static final String SHORT_CODE_UNIQUE_CONSTRAINT = "uk_short_urls_short_code";
    private static final int MYSQL_DUPLICATE_KEY_ERROR = 1062;

    private final ShortUrlRepository shortUrlRepository;
    private final ShortCodeGenerator shortCodeGenerator;

    public ShortUrlCreationService(ShortUrlRepository shortUrlRepository, ShortCodeGenerator shortCodeGenerator) {
        this.shortUrlRepository = shortUrlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
    }

    public CreatedShortUrl create(String rawUrl, Instant expiresAt) {
        DestinationUrl destinationUrl = new DestinationUrl(rawUrl);
        DataIntegrityViolationException lastCollision = null;

        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            ShortUrl shortUrl = new ShortUrl(
                    shortCodeGenerator.generate(),
                    destinationUrl.value(),
                    Instant.now(),
                    expiresAt
            );

            try {
                // Each repository call has its own transaction so a collision does not poison the next attempt.
                shortUrlRepository.saveAndFlush(shortUrl);
                return new CreatedShortUrl(shortUrl.getShortCode(), shortUrl.getOriginalUrl(), shortUrl.getExpiresAt());
            } catch (DataIntegrityViolationException exception) {
                if (!isShortCodeCollision(exception)) {
                    throw exception;
                }
                lastCollision = exception;
            }
        }

        throw new ShortCodeGenerationException(lastCollision);
    }

    private boolean isShortCodeCollision(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                if (SHORT_CODE_UNIQUE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())
                        || violation.getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR) {
                    return true;
                }
            }
            if (cause instanceof SQLException sqlException
                    && sqlException.getErrorCode() == MYSQL_DUPLICATE_KEY_ERROR) {
                return true;
            }
        }
        return false;
    }
}
