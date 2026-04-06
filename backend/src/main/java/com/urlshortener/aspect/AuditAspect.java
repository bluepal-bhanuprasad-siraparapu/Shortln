package com.urlshortener.aspect;

import com.urlshortener.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.urlshortener.security.UserDetailsImpl;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Pointcut("execution(public * com.urlshortener.service.*.*(..))")
    public void serviceLayer() {}

    @Pointcut("!execution(* com.urlshortener.service.AuditLogService.*(..))")
    public void excludeAuditService() {}

    @AfterReturning(pointcut = "serviceLayer() && excludeAuditService()", returning = "result")
    public void logAfterSuccessfulReturn(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        String details = String.format("Action performed: '%s' in category '%s'. Parameters: %s", 
                methodName, className.replace("Service", ""), Arrays.toString(args));
        
        Long userId = null;
        String email = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            userId = userDetails.getId();
            email = userDetails.getEmail();
        }

        auditLogService.log("SERVICE_SUCCESS: " + methodName, details, userId, email, null);
    }

    @AfterThrowing(pointcut = "serviceLayer() && excludeAuditService()", throwing = "error")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable error) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        Object[] args = joinPoint.getArgs();
        
        String details = String.format("Failed to execute '%s' in category '%s'. Error: %s. Parameters: %s", 
                methodName, className.replace("Service", ""), error.getMessage(), Arrays.toString(args));
        
        Long userId = null;
        String email = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl userDetails) {
            userId = userDetails.getId();
            email = userDetails.getEmail();
        }

        auditLogService.log("SERVICE_FAILURE: " + methodName, details, userId, email, null);
    }
}
