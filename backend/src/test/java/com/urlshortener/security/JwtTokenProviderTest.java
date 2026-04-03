package com.urlshortener.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String jwtSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private int jwtExpirationMs = 3600000;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", jwtSecret);
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", jwtExpirationMs);
        jwtTokenProvider.init(); // Manually call @PostConstruct
    }

    @Test
    void generateJwtToken_ReturnsValidToken() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");

        String token = jwtTokenProvider.generateJwtToken(authentication);
        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertEquals("test@example.com", jwtTokenProvider.getUserNameFromJwtToken(token));
    }

    @Test
    void validateJwtToken_ValidToken_ReturnsTrue() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("test@example.com");

        String token = jwtTokenProvider.generateJwtToken(authentication);
        assertTrue(jwtTokenProvider.validateJwtToken(token));
    }

    @Test
    void validateJwtToken_MalformedToken_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateJwtToken("malformed.token.here"));
    }

    @Test
    void validateJwtToken_EmptyToken_ReturnsFalse() {
        assertFalse(jwtTokenProvider.validateJwtToken(""));
    }
}
