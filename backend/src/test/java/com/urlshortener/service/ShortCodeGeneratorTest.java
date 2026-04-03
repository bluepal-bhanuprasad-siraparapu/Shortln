package com.urlshortener.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    private final ShortCodeGenerator generator = new ShortCodeGenerator();

    @Test
    void testGenerate_ReturnsCorrectLength() {
        String code = generator.generate();
        assertNotNull(code);
        assertEquals(6, code.length());
    }

    @Test
    void testGenerate_ReturnsAlphanumericCharacters() {
        String code = generator.generate();
        assertTrue(code.matches("^[a-zA-Z0-9]+$"));
    }

    @Test
    void testGenerate_GeneratesUniqueCodes() {
        Set<String> generatedCodes = new HashSet<>();
        for(int i = 0; i < 1000; i++) {
            generatedCodes.add(generator.generate());
        }
        assertEquals(1000, generatedCodes.size(), "Generated codes should be unique across 1000 iterations");
    }
}
