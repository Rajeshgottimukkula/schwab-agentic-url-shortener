package com.rajesh.urlshortener.service;

import com.rajesh.urlshortener.domain.ShortUrl;
import com.rajesh.urlshortener.persistence.ShortUrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortUrlLookupServiceTest {

    private ShortUrlRepository repository;
    private ShortUrlLookupService service;

    @BeforeEach
    void setUp() {
        repository = mock(ShortUrlRepository.class);
        service = new ShortUrlLookupService(repository);
    }

    @Test
    void returnsMappingInformationWithoutChangingClickCount() {
        Instant createdAt = Instant.parse("2026-09-01T12:00:00Z");
        Instant expiresAt = Instant.parse("2026-12-31T23:59:59Z");
        when(repository.findByShortCode("Ab12Cd34")).thenReturn(Optional.of(new ShortUrl(
                "Ab12Cd34", "https://example.com/path", createdAt, expiresAt
        )));

        LookedUpShortUrl result = service.lookup("Ab12Cd34");

        assertEquals(new LookedUpShortUrl("Ab12Cd34", "https://example.com/path", expiresAt, 0L), result);
        verify(repository).findByShortCode("Ab12Cd34");
        verify(repository, never()).incrementClickCountIfActive(anyString(), any());
    }

    @Test
    void unknownCodeUsesExistingNotFoundException() {
        when(repository.findByShortCode("missing")).thenReturn(Optional.empty());

        assertThrows(ShortUrlNotFoundException.class, () -> service.lookup("missing"));

        verify(repository, never()).incrementClickCountIfActive(anyString(), any());
    }
}
