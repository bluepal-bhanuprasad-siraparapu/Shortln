package com.urlshortener.aspect;

import com.urlshortener.security.UserDetailsImpl;
import com.urlshortener.service.AuditLogService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuditAspect auditAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getTarget()).thenReturn(new Object()); // Generic target
        when(joinPoint.getArgs()).thenReturn(new Object[]{"arg1"});
    }

    @Test
    void logAfterSuccessfulReturn_WithUser_LogsSuccessWithIdentity() {
        try (MockedStatic<SecurityContextHolder> mockedSecurity = mockStatic(SecurityContextHolder.class)) {
            SecurityContext securityContext = mock(SecurityContext.class);
            Authentication authentication = mock(Authentication.class);
            UserDetailsImpl userDetails = mock(UserDetailsImpl.class);

            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(userDetails);
            
            when(userDetails.getId()).thenReturn(1L);
            when(userDetails.getEmail()).thenReturn("aspect@example.com");

            auditAspect.logAfterSuccessfulReturn(joinPoint, "result");

            verify(auditLogService).log(
                    eq("SERVICE_SUCCESS: testMethod"),
                    anyString(),
                    eq(1L),
                    eq("aspect@example.com"),
                    isNull()
            );
        }
    }

    @Test
    void logAfterSuccessfulReturn_Anonymous_LogsSuccessWithNullIdentity() {
        try (MockedStatic<SecurityContextHolder> mockedSecurity = mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(mock(SecurityContext.class));
            // Authentication will be null by default

            auditAspect.logAfterSuccessfulReturn(joinPoint, "result");

            verify(auditLogService).log(
                    eq("SERVICE_SUCCESS: testMethod"),
                    anyString(),
                    isNull(),
                    isNull(),
                    isNull()
            );
        }
    }

    @Test
    void logAfterThrowing_LogsFailure() {
        try (MockedStatic<SecurityContextHolder> mockedSecurity = mockStatic(SecurityContextHolder.class)) {
            mockedSecurity.when(SecurityContextHolder::getContext).thenReturn(mock(SecurityContext.class));
            
            Exception error = new RuntimeException("Test Exception");

            auditAspect.logAfterThrowing(joinPoint, error);

            verify(auditLogService).log(
                    eq("SERVICE_FAILURE: testMethod"),
                    contains("Test Exception"),
                    isNull(),
                    isNull(),
                    isNull()
            );
        }
    }
}
