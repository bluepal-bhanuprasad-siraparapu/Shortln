package com.urlshortener.security;

import com.urlshortener.entity.User;
import com.urlshortener.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .role(com.urlshortener.entity.Role.USER)
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("test@example.com");

        assertNotNull(result);
        assertEquals("test@example.com", result.getUsername());
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, 
            () -> userDetailsService.loadUserByUsername("nonexistent@example.com"));
    }
}
