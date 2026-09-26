package com.rajesh.urlshortener.api;

import com.rajesh.urlshortener.service.CreatedShortUrl;
import com.rajesh.urlshortener.service.ShortUrlCreationService;
import com.rajesh.urlshortener.service.LookedUpShortUrl;
import com.rajesh.urlshortener.service.ShortUrlLookupService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {

    private final ShortUrlCreationService shortUrlCreationService;
    private final ShortUrlLookupService shortUrlLookupService;

    public ShortUrlController(
            ShortUrlCreationService shortUrlCreationService,
            ShortUrlLookupService shortUrlLookupService
    ) {
        this.shortUrlCreationService = shortUrlCreationService;
        this.shortUrlLookupService = shortUrlLookupService;
    }

    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request) {
        CreatedShortUrl created = shortUrlCreationService.create(request.url(), request.expiresAt());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(created.code())
                .toUri();

        CreateShortUrlResponse response = new CreateShortUrlResponse(
                created.code(),
                created.originalUrl(),
                created.expiresAt(),
                location.toString()
        );
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{code}")
    public ShortUrlLookupResponse lookup(@PathVariable String code) {
        LookedUpShortUrl found = shortUrlLookupService.lookup(code);
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}")
                .buildAndExpand(found.code())
                .toUriString();

        return new ShortUrlLookupResponse(
                found.code(),
                found.originalUrl(),
                found.expiresAt(),
                found.clickCount(),
                shortUrl
        );
    }
}
