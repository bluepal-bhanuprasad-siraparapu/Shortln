package com.urlshortener.controller;

import com.urlshortener.dto.JwtResponse;
import com.urlshortener.dto.LoginRequest;
import com.urlshortener.dto.MessageResponse;
import com.urlshortener.dto.RegisterRequest;
import com.urlshortener.dto.ForgotPasswordRequest;
import com.urlshortener.dto.ResetPasswordRequest;
import com.urlshortener.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and password management")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Authenticate user", description = "Verifies user credentials and returns a JWT token")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
        @ApiResponse(responseCode = "400", description = "Invalid credentials or validation error")
    })
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Initiating authentication for email: {}", loginRequest.getEmail());
        try {
            JwtResponse response = authService.authenticateUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Authentication failed for email: {} - Reason: {}", loginRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }

    @Operation(summary = "Register new user", description = "Creates a new user account with default role")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully"),
        @ApiResponse(responseCode = "400", description = "Email already exists or validation error")
    })
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest signUpRequest) {
        log.info("Initiating registration for email: {}", signUpRequest.getEmail());
        try {
            authService.registerUser(signUpRequest);
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
        } catch (IllegalArgumentException e) {
            log.warn("Registration validation failed for email: {} - Reason: {}", signUpRequest.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during registration for email: {}", signUpRequest.getEmail(), e);
            return ResponseEntity.badRequest().body(new MessageResponse("Error: Registration failed"));
        }
    }

    @Operation(summary = "Forgot password", description = "Sends a password reset OTP to the user's registered email address")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Password reset requested for email: {}", request.getEmail());
        try {
            authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(new MessageResponse("OTP sent successfully to your email!"));
        } catch (Exception e) {
            log.warn("Password reset request failed for email: {} - Reason: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Reset password", description = "Verifies the OTP and updates the user's password")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Resetting password for email: {}", request.getEmail());
        try {
            authService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
            log.info("Password successfully reset for email: {}", request.getEmail());
            return ResponseEntity.ok(new MessageResponse("Password reset successful! You can now login."));
        } catch (Exception e) {
            log.warn("Password reset failed for email: {} - Reason: {}", request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }

    @Operation(summary = "Magic login", description = "Authenticates a user via a one-time magic link token")
    @GetMapping("/magic-login")
    public ResponseEntity<?> magicLogin(@RequestParam("token") String token) {
        log.info("Magic login attempt with token");
        try {
            JwtResponse response = authService.magicLogin(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Magic login failed - Reason: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}
