package com.rajesh.urlshortener.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Generates cryptographically secure alphanumeric short codes. */
@Component
public class ShortCodeGenerator {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int MAX_CODE_LENGTH = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final int length;

    public ShortCodeGenerator(@Value("${app.short-url.code-length:8}") int length) {
        if (length < 1 || length > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("Short code length must be between 1 and " + MAX_CODE_LENGTH);
        }
        this.length = length;
    }

    public String generate() {
        StringBuilder code = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            code.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
