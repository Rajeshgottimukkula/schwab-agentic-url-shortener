package com.rajesh.urlshortener.service;

/** Raised when the bounded short-code allocation attempts are exhausted. */
public class ShortCodeGenerationException extends RuntimeException {

    public ShortCodeGenerationException(Throwable cause) {
        super("Unable to allocate a unique short code after 5 attempts", cause);
    }
}
