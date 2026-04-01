package com.urlshortener.controller;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.urlshortener.dto.MessageResponse;
import com.urlshortener.dto.UserDto;
import com.urlshortener.entity.Plan;
import com.urlshortener.security.UserDetailsImpl;
import com.urlshortener.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Slf4j
@Tag(name = "Payments", description = "Endpoints for managing subscriptions and Razorpay integration")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final UserService userService;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Operation(summary = "Create Razorpay order", description = "Initializes a new payment order for the PRO plan upgrade")
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) {
        log.info("Creating Razorpay order");
        try {
            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", 99900); // amount in the smallest currency unit (999.00 INR)
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());

            Order order = razorpay.orders.create(orderRequest);

            return ResponseEntity.ok(Map.of(
                    "id", order.get("id").toString(),
                    "amount", (Integer) order.get("amount"),
                    "currency", order.get("currency").toString(),
                    "keyId", keyId // Send keyId to frontend for checkout initialization
            ));
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Error creating Razorpay order: " + e.getMessage()));
        }
    }

    @Operation(summary = "Verify payment", description = "Verifies the Razorpay payment signature and upgrades the user's plan to PRO")
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(
            @RequestBody Map<String, String> response,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        
        log.info("Verifying Razorpay payment for user {}", userDetails.getUsername());
        try {
            String razorpayOrderId = response.get("razorpay_order_id");
            String razorpayPaymentId = response.get("razorpay_payment_id");
            String razorpaySignature = response.get("razorpay_signature");

            // Verify signature
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", razorpayOrderId);
            options.put("razorpay_payment_id", razorpayPaymentId);
            options.put("razorpay_signature", razorpaySignature);

            boolean isValid = Utils.verifyPaymentSignature(options, keySecret);

            if (isValid) {
                UserDto updatedUser = userService.updateUserPlan(userDetails.getId(), Plan.PRO);
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Payment verified and plan upgraded to PRO",
                        "user", updatedUser
                ));
            } else {
                return ResponseEntity.badRequest().body(new MessageResponse("Invalid payment signature"));
            }
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Verification error: " + e.getMessage()));
        }
    }
}
