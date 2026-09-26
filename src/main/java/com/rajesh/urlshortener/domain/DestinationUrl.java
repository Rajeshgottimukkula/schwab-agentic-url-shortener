package com.rajesh.urlshortener.domain;

import java.net.URI;
import java.net.URISyntaxException;

/** A syntactically valid absolute HTTP or HTTPS destination URL. */
public record DestinationUrl(String value) {

    public DestinationUrl {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Destination URL must not be null or blank");
        }

        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Destination URL is malformed", exception);
        }

        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("Destination URL must be absolute");
        }

        if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
            throw new IllegalArgumentException("Destination URL scheme must be http or https");
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("Destination URL must include a host");
        }
    }
}
