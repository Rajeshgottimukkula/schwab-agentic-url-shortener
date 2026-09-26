package com.rajesh.urlshortener.service;

import com.rajesh.urlshortener.domain.ShortUrl;
import com.rajesh.urlshortener.persistence.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortUrlRedirectServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-26T12:00:00Z");

    private ShortUrlRepository repository;
    private ShortUrlRedirectService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShortUrlRepository.class);
        service = new ShortUrlRedirectService(repository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void activeMappingReturnsDestinationAndIncrementsExactlyOnce() {
        when(repository.findByShortCode("Ab12Cd34"))
                .thenReturn(Optional.of(mapping(null)));
        when(repository.incrementClickCountIfActive("Ab12Cd34", NOW)).thenReturn(1);

        assertEquals("https://example.com/path", service.resolveAndRecordClick("Ab12Cd34"));

        verify(repository).incrementClickCountIfActive("Ab12Cd34", NOW);
    }

    @Test
    void unknownCodeDoesNotIncrement() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThrows(ShortUrlNotFoundException.class, () -> service.resolveAndRecordClick("missing"));

        verify(repository, never()).incrementClickCountIfActive("missing", NOW);
    }

    @Test
    void expiredCodeDoesNotIncrement() {
        when(repository.findByShortCode("expired"))
                .thenReturn(Optional.of(mapping(NOW.minusNanos(1))));

        assertThrows(ShortUrlNotFoundException.class, () -> service.resolveAndRecordClick("expired"));

        verify(repository, never()).incrementClickCountIfActive("expired", NOW);
    }

    @Test
    void expirationBoundaryIsStillActiveAtExactExpiryInstant() {
        when(repository.findByShortCode("boundary"))
                .thenReturn(Optional.of(mapping(NOW)));
        when(repository.incrementClickCountIfActive("boundary", NOW)).thenReturn(1);

        assertEquals("https://example.com/path", service.resolveAndRecordClick("boundary"));

        verify(repository).incrementClickCountIfActive("boundary", NOW);
    }

    @Test
    void treatsConditionalIncrementFailureAsNotFound() {
        when(repository.findByShortCode("now-expired"))
                .thenReturn(Optional.of(mapping(NOW)));
        when(repository.incrementClickCountIfActive("now-expired", NOW)).thenReturn(0);

        assertThrows(ShortUrlNotFoundException.class, () -> service.resolveAndRecordClick("now-expired"));
    }

    private ShortUrl mapping(Instant expiresAt) {
        return new ShortUrl("Ab12Cd34", "https://example.com/path", NOW.minusSeconds(60), expiresAt);
    }
}
