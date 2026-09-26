package com.rajesh.urlshortener.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortCodeGeneratorTest {

    @Test
    void generatesConfiguredLength() {
        ShortCodeGenerator generator = new ShortCodeGenerator(12);

        assertEquals(12, generator.generate().length());
    }

    @Test
    void defaultConfiguredLengthIsEight() {
        ShortCodeGenerator generator = new ShortCodeGenerator(8);

        assertEquals(8, generator.generate().length());
    }

    @Test
    void generatesOnlyAlphanumericCharacters() {
        String code = new ShortCodeGenerator(32).generate();

        assertTrue(code.matches("[A-Za-z0-9]+"));
    }

    @Test
    void repeatedGenerationProducesDifferentValues() {
        ShortCodeGenerator generator = new ShortCodeGenerator(8);
        Set<String> codes = new HashSet<>();

        for (int index = 0; index < 100; index++) {
            codes.add(generator.generate());
        }

        assertTrue(codes.size() > 1);
    }

    @Test
    void rejectsLengthsOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(0));
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(-1));
        assertThrows(IllegalArgumentException.class, () -> new ShortCodeGenerator(33));
    }
}
