package com.urlshortener.service;

import com.urlshortener.dto.JwtResponse;
import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.entity.Plan;
import com.urlshortener.entity.Role;
import com.urlshortener.entity.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.security.JwtTokenProvider;
import com.urlshortener.security.UserDetailsImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .plan(Plan.FREE)
                .active(1)
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password");
        registerRequest.setName("Test User");
        registerRequest.setPlan(Plan.FREE);
    }

    @Test
    void registerUser_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(encoder.encode(anyString())).thenReturn("encodedPassword");

        authService.registerUser(registerRequest);

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_DuplicateEmail_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.registerUser(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void authenticateUser_Success() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, 
                "Test User", 
                "test@example.com", 
                "password", 
                1, 
                0, 
                "FREE", 
                Collections.emptyList()
        );
        
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtTokenProvider.generateJwtToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(encoder.matches(anyString(), anyString())).thenReturn(true);

        JwtResponse response = authService.authenticateUser(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
    }

    @Test
    void forgotPassword_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        authService.forgotPassword("test@example.com");

        assertNotNull(testUser.getResetOtp());
        assertNotNull(testUser.getResetOtpExpiry());
        verify(emailService, times(1)).sendOtp(eq("test@example.com"), anyString());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void resetPassword_Success() {
        testUser.setResetOtp("123456");
        testUser.setResetOtpExpiry(LocalDateTime.now().plusMinutes(5));
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(encoder.encode("newPassword")).thenReturn("newEncodedPassword");

        authService.resetPassword("test@example.com", "123456", "newPassword");

        assertEquals("newEncodedPassword", testUser.getPassword());
        assertNull(testUser.getResetOtp());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void resetPassword_InvalidOtp_ThrowsException() {
        testUser.setResetOtp("123456");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, 
            () -> authService.resetPassword("test@example.com", "wrong-otp", "newPassword"));
    }

    @Test
    void resetPassword_ExpiredOtp_ThrowsException() {
        testUser.setResetOtp("123456");
        testUser.setResetOtpExpiry(LocalDateTime.now().minusMinutes(1));
        
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class, 
            () -> authService.resetPassword("test@example.com", "123456", "newPassword"));
    }
}
