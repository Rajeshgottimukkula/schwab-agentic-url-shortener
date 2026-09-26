package com.rajesh.urlshortener.service;

import com.rajesh.urlshortener.domain.ShortUrl;
import com.rajesh.urlshortener.persistence.ShortUrlRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlLookupService {

    private final ShortUrlRepository shortUrlRepository;

    public ShortUrlLookupService(ShortUrlRepository shortUrlRepository) {
        this.shortUrlRepository = shortUrlRepository;
    }

    @Transactional(readOnly = true)
    public LookedUpShortUrl lookup(String code) {
        ShortUrl shortUrl = shortUrlRepository.findByShortCode(code)
                .orElseThrow(() -> new ShortUrlNotFoundException(code));

        return new LookedUpShortUrl(
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getExpiresAt(),
                shortUrl.getClickCount()
        );
    }
}
