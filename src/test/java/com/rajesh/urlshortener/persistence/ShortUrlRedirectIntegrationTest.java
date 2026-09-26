package com.rajesh.urlshortener.persistence;

import com.rajesh.urlshortener.domain.ShortUrl;
import com.rajesh.urlshortener.service.ShortUrlNotFoundException;
import com.rajesh.urlshortener.service.ShortUrlRedirectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ShortUrlRedirectIntegrationTest {

    @Autowired
    private ShortUrlRepository repository;

    @Autowired
    private ShortUrlRedirectService redirectService;

    @Test
    void atomicIncrementAccumulatesAndUnknownCodeDoesNotChangeRows() {
        long countBefore = repository.count();
        repository.saveAndFlush(new ShortUrl(
                "itAtomic123", "https://example.com/integration", Instant.now(), null
        ));

        assertEquals("https://example.com/integration", redirectService.resolveAndRecordClick("itAtomic123"));
        assertEquals("https://example.com/integration", redirectService.resolveAndRecordClick("itAtomic123"));
        org.junit.jupiter.api.Assertions.assertThrows(
                ShortUrlNotFoundException.class, () -> redirectService.resolveAndRecordClick("absent")
        );

        repository.flush();
        assertEquals(2L, repository.findByShortCode("itAtomic123").orElseThrow().getClickCount());
        assertEquals(countBefore + 1, repository.count());
    }

    @Test
    void conditionalIncrementSkipsExpiredMappings() {
        repository.saveAndFlush(new ShortUrl(
                "itExpired123", "https://example.com/expired", Instant.now(), Instant.parse("2020-01-01T00:00:00Z")
        ));

        assertEquals(0, repository.incrementClickCountIfActive("itExpired123", Instant.now()));
        assertEquals(0L, repository.findByShortCode("itExpired123").orElseThrow().getClickCount());
    }
}
