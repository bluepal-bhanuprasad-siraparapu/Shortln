package com.urlshortener.service;

import com.urlshortener.dto.JwtResponse;
import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.entity.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.security.JwtTokenProvider;
import com.urlshortener.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        log.info("Attempting authentication for email: {}", loginRequest.getEmail());
        
        // Explicitly check for user existence to provide specific error
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Error: User not registered!"));

        // Explicitly check password match to provide specific error
        if (!encoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Error: Invalid password!");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Check for subscription expiry on login
        if (user.getPlan() == com.urlshortener.entity.Plan.PRO && user.getSubscriptionExpiry() != null) {
            if (user.getSubscriptionExpiry().isBefore(java.time.LocalDateTime.now())) {
                user.setPlan(com.urlshortener.entity.Plan.FREE);
                user.setSubscriptionExpiry(null);
                userRepository.save(user);
            }
        }

        auditLogService.log("LOGIN_SUCCESS", "User logged in with email: " + loginRequest.getEmail(), user.getId(), user.getEmail(), null);

        return new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getName(),
                userDetails.getUsername(),
                user.getRole().name(),
                user.getPlan().name(),
                user.getSubscriptionExpiry());
    }

    @Transactional
    public void registerUser(RegisterRequest signUpRequest) {
        log.info("Processing registration for email: {}", signUpRequest.getEmail());
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new IllegalArgumentException("Error: Email is already in use!");
        }

        String autoLoginToken = java.util.UUID.randomUUID().toString();
        // Create new user's account
        User user = User.builder()
                .name(signUpRequest.getName())
                .email(signUpRequest.getEmail())
                .password(encoder.encode(signUpRequest.getPassword()))
                .plan(signUpRequest.getPlan())
                .build();
        user.setAutoLoginToken(autoLoginToken);
        user.setAutoLoginExpiry(LocalDateTime.now().plusHours(24));

        userRepository.save(user);
        
        auditLogService.log("USER_REGISTER", "New user registered with email: " + signUpRequest.getEmail(), user.getId(), user.getEmail(), null);
        
        // Notify Admins about new user
        notificationService.notifyAdmins("New user joined: " + user.getName() + " (" + user.getEmail() + ")", "SYSTEM");

        // Send welcoming email with a login button
        emailService.sendWelcomeEmail(user.getEmail(), user.getName(), autoLoginToken);
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Error: This mail is not registered!"));

        String otp = String.format("%06d", new Random().nextInt(1000000));
        user.setResetOtp(otp);
        user.setResetOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        auditLogService.log("FORGOT_PASSWORD", "OTP sent to " + email, user.getId(), user.getEmail(), null);

        emailService.sendOtp(email, otp);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Error: Email not found!"));

        if (user.getResetOtp() == null || !user.getResetOtp().equals(otp)) {
            throw new IllegalArgumentException("Error: Invalid OTP!");
        }

        if (user.getResetOtpExpiry() == null || user.getResetOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Error: OTP has expired!");
        }

        user.setPassword(encoder.encode(newPassword));
        user.setResetOtp(null);
        user.setResetOtpExpiry(null);
        userRepository.save(user);

        auditLogService.log("PASSWORD_RESET", "Password reset successfully for " + email, user.getId(), user.getEmail(), null);
    }

    @Transactional
    public JwtResponse magicLogin(String token) {
        log.info("Authenticating user via magic link token");
        User user = userRepository.findByAutoLoginToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Error: Invalid or expired magic link!"));

        if (user.getAutoLoginExpiry() == null || user.getAutoLoginExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Error: Magic link has expired!");
        }

        // Nullify to prevent reuse
        user.setAutoLoginToken(null);
        user.setAutoLoginExpiry(null);
        userRepository.save(user);

        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateJwtToken(authentication);

        // Check for subscription expiry on login
        if (user.getPlan() == com.urlshortener.entity.Plan.PRO && user.getSubscriptionExpiry() != null) {
            if (user.getSubscriptionExpiry().isBefore(LocalDateTime.now())) {
                user.setPlan(com.urlshortener.entity.Plan.FREE);
                user.setSubscriptionExpiry(null);
                userRepository.save(user);
            }
        }

        auditLogService.log("MAGIC_LOGIN_SUCCESS", "User logged in via magic link: " + user.getEmail(), user.getId(), user.getEmail(), null);

        return new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getName(),
                userDetails.getUsername(),
                user.getRole().name(),
                user.getPlan().name(),
                user.getSubscriptionExpiry());
    }
}
