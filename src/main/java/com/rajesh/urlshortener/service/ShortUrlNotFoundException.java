package com.rajesh.urlshortener.service;

public class ShortUrlNotFoundException extends RuntimeException {

    public ShortUrlNotFoundException(String code) {
        super("No active short URL exists for code: " + code);
    }
}
