package com.rajesh.urlshortener.service;

import com.rajesh.urlshortener.domain.ShortCodeGenerator;
import com.rajesh.urlshortener.domain.ShortUrl;
import com.rajesh.urlshortener.persistence.ShortUrlRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortUrlCreationServiceTest {

    private ShortUrlRepository repository;
    private ShortCodeGenerator codeGenerator;
    private ShortUrlCreationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShortUrlRepository.class);
        codeGenerator = mock(ShortCodeGenerator.class);
        service = new ShortUrlCreationService(repository, codeGenerator);
    }

    @Test
    void validatesDestinationAndPersistsMappingUsingGeneratedCode() {
        Instant expiresAt = Instant.parse("2026-12-31T23:59:59Z");
        when(codeGenerator.generate()).thenReturn("Ab12Cd34");
        when(repository.saveAndFlush(any(ShortUrl.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreatedShortUrl created = service.create("https://example.com/path?source=test", expiresAt);

        assertEquals("Ab12Cd34", created.code());
        assertEquals("https://example.com/path?source=test", created.originalUrl());
        assertEquals(expiresAt, created.expiresAt());
        verify(repository).saveAndFlush(any(ShortUrl.class));
        verify(codeGenerator).generate();
    }

    @Test
    void persistsCreationTimestampAndInitialClickCount() {
        when(codeGenerator.generate()).thenReturn("Ab12Cd34");
        when(repository.saveAndFlush(any(ShortUrl.class))).thenAnswer(invocation -> {
            ShortUrl saved = invocation.getArgument(0);
            assertNotNull(saved.getCreatedAt());
            assertEquals(0L, saved.getClickCount());
            return saved;
        });

        service.create("https://example.com", null);

        verify(repository).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void retriesUniqueCodeCollisionAndReturnsSuccessfulAttempt() {
        when(codeGenerator.generate()).thenReturn("collision", "available");
        when(repository.saveAndFlush(any(ShortUrl.class)))
                .thenThrow(shortCodeCollision())
                .thenAnswer(invocation -> invocation.getArgument(0));

        CreatedShortUrl created = service.create("https://example.com", null);

        assertEquals("available", created.code());
        verify(codeGenerator, times(2)).generate();
        verify(repository, times(2)).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void stopsAfterFiveCollisionsAndThrowsControlledFailure() {
        when(codeGenerator.generate()).thenReturn("collision");
        when(repository.saveAndFlush(any(ShortUrl.class))).thenThrow(shortCodeCollision());

        assertThrows(ShortCodeGenerationException.class, () -> service.create("https://example.com", null));

        verify(codeGenerator, times(5)).generate();
        verify(repository, times(5)).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void doesNotRetryUnrelatedIntegrityFailures() {
        when(codeGenerator.generate()).thenReturn("Ab12Cd34");
        DataIntegrityViolationException failure = new DataIntegrityViolationException(
                "unrelated constraint",
                new SQLException("constraint failure", "23000", 1048)
        );
        when(repository.saveAndFlush(any(ShortUrl.class))).thenThrow(failure);

        assertThrows(DataIntegrityViolationException.class, () -> service.create("https://example.com", null));

        verify(codeGenerator, times(1)).generate();
        verify(repository).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void rejectsInvalidDestinationBeforePersistence() {
        assertThrows(IllegalArgumentException.class, () -> service.create("ftp://example.com", null));

        verify(repository, never()).saveAndFlush(any(ShortUrl.class));
        verify(codeGenerator, never()).generate();
    }

    private DataIntegrityViolationException shortCodeCollision() {
        SQLException sqlException = new SQLException("duplicate key", "23000", 1062);
        ConstraintViolationException constraintViolation = new ConstraintViolationException(
                "duplicate key", sqlException, "uk_short_urls_short_code"
        );
        return new DataIntegrityViolationException("short-code collision", constraintViolation);
    }
}
