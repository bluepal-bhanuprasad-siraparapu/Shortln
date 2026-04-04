package com.urlshortener;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Full context loading test to boost coverage across all @Configuration classes,
 * Beans, and Spring-managed infrastructure that unit tests usually bypass.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationIntegrationTest {

    @Test
    void contextLoads() {
        // This exercises all @Configuration classes, @Bean definitions,
        // and @PostConstruct initializers (like DataInitializer)
        assertNotNull(this.getClass());
    }
}
