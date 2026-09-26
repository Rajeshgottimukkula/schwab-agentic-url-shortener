package com.rajesh.urlshortener.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DestinationUrlTest {

    @Test
    void acceptsHttpUrl() {
        assertDoesNotThrow(() -> new DestinationUrl("http://example.com"));
    }

    @Test
    void acceptsHttpsUrl() {
        assertDoesNotThrow(() -> new DestinationUrl("https://example.com"));
    }

    @Test
    void acceptsPathQueryAndFragment() {
        assertDoesNotThrow(() -> new DestinationUrl("https://example.com/path/to/page?source=test#section"));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> new DestinationUrl(null));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> new DestinationUrl(" \t\n"));
    }

    @Test
    void rejectsRelativeUrl() {
        assertThrows(IllegalArgumentException.class, () -> new DestinationUrl("/relative/path"));
    }

    @Test
    void rejectsUnsupportedScheme() {
        assertThrows(IllegalArgumentException.class, () -> new DestinationUrl("ftp://example.com/file"));
    }

    @Test
    void rejectsMalformedUrl() {
        assertThrows(IllegalArgumentException.class, () -> new DestinationUrl("https://exa mple.com/path"));
    }

    @Test
    void rejectsUrlWithoutHost() {
        assertThrows(IllegalArgumentException.class, () -> new DestinationUrl("https:///path"));
    }

    @Test
    void requiresExactLowercaseHttpScheme() {
        assertThrows(IllegalArgumentException.class, () -> new DestinationUrl("HTTP://example.com"));
    }
}
