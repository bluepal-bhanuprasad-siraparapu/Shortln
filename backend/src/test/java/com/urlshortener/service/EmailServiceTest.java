package com.urlshortener.service;

import com.urlshortener.entity.User;
import com.urlshortener.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        mimeMessage = mock(MimeMessage.class);
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void getUserByEmail_Success() {
        User user = User.builder().email("test@example.com").build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        User result = emailService.getUserByEmail("test@example.com");

        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void getUserByEmail_NotFound() {
        when(userRepository.findByEmail("none@example.com")).thenReturn(Optional.empty());

        User result = emailService.getUserByEmail("none@example.com");

        assertNull(result);
    }

    @Test
    void sendOtp_Success() {
        emailService.sendOtp("test@example.com", "123456");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(auditLogService, times(1)).log(eq("EMAIL_SENT: OTP"), anyString());
    }

    @Test
    void sendWelcomeEmail_Success() {
        emailService.sendWelcomeEmail("test@example.com", "Test User", "token123");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(auditLogService, times(1)).log(eq("EMAIL_SENT: WELCOME"), anyString());
    }

    @Test
    void sendLinkExpiryEmail_Success() {
        emailService.sendLinkExpiryEmail("test@example.com", "Test User", "My Link", "abc123", "2026-10-10");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(auditLogService, times(1)).log(eq("EMAIL_SENT: EXPIRY"), anyString());
    }
}
