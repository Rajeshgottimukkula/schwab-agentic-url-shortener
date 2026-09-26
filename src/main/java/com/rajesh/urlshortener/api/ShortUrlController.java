package com.rajesh.urlshortener.api;

import com.rajesh.urlshortener.service.CreatedShortUrl;
import com.rajesh.urlshortener.service.ShortUrlCreationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {

    private final ShortUrlCreationService shortUrlCreationService;

    public ShortUrlController(ShortUrlCreationService shortUrlCreationService) {
        this.shortUrlCreationService = shortUrlCreationService;
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
}
