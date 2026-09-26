package com.rajesh.urlshortener.persistence;

import com.rajesh.urlshortener.domain.ShortUrl;
import com.rajesh.urlshortener.service.ShortUrlLookupService;
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
class ShortUrlLookupIntegrationTest {

    @Autowired
    private ShortUrlRepository repository;

    @Autowired
    private ShortUrlLookupService lookupService;

    @Test
    void lookupDoesNotIncrementClickCount() {
        repository.saveAndFlush(new ShortUrl(
                "itLookup123", "https://example.com/lookup", Instant.now(), null
        ));

        assertEquals(0L, lookupService.lookup("itLookup123").clickCount());
        assertEquals(0L, repository.findByShortCode("itLookup123").orElseThrow().getClickCount());
    }
}
