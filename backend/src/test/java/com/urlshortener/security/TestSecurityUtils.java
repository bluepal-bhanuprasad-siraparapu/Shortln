package com.urlshortener.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

public class TestSecurityUtils {

    public static void setupMockUser(Long id, String name, String email, String role) {
        UserDetailsImpl userDetails = new UserDetailsImpl(
                id,
                name,
                email,
                "password",
                1,
                0,
                "FREE",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void setupMockAdmin() {
        setupMockUser(1L, "Admin User", "admin@example.com", "ADMIN");
    }

    public static void setupMockUser() {
        setupMockUser(2L, "Regular User", "user@example.com", "USER");
    }
    
    public static void clear() {
        SecurityContextHolder.clearContext();
    }
}
