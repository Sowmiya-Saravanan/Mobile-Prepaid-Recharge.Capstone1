package com.mobicommServices3.controller;

import com.mobicommServices3.model.RechargeTransaction;


import com.mobicommServices3.service.RechargeService;
import com.mobicommServices3.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscriber")
@Slf4j
public class RechargeController { // Renamed to RechargeController based on your info

    @Autowired
    private RechargeService rechargeService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/recharge")
    public ResponseEntity<?> subscriberRecharge(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SubscriberRechargeRequest request) {
        try {
            // Extract and validate token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("Invalid Authorization header");
            }
            String token = authHeader.substring(7); // Remove "Bearer "
            String mobileNumberFromToken = jwtUtils.getUsernameFromToken(token);
            if (mobileNumberFromToken == null || !jwtUtils.validateToken(token)) {
                throw new IllegalArgumentException("Invalid or expired token");
            }

            // Extract mobile number from request and validate
            String mobileNumberFromRequest = request.mobileNumber();
            if (mobileNumberFromRequest != null && !mobileNumberFromRequest.equals(mobileNumberFromToken)) {
                throw new IllegalArgumentException("Mobile number in request does not match authenticated user");
            }
            String mobileNumber = mobileNumberFromToken; // Use token-derived mobile number

            // Validate mobile number format
            if (!mobileNumber.matches("\\d{10}")) {
                throw new IllegalArgumentException("Invalid mobile number. Must be a 10-digit number.");
            }

            // Verify subscriber exists
            if (!rechargeService.subscriberExists(mobileNumber)) {
                throw new IllegalArgumentException("Mobile number not registered");
            }

            // Initiate recharge
            RechargeTransaction transaction = rechargeService.initiateRecharge(mobileNumber, request.planId());
            log.info("Authenticated recharge initiated for {} with plan ID: {}", mobileNumber, request.planId());

            // Return numeric transactionId
            return ResponseEntity.ok(Map.of("transactionId", transaction.getTransactionId()));
        } catch (Exception e) {
            log.error("Subscriber recharge failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

// Request DTO
record SubscriberRechargeRequest(Long planId, String mobileNumber) {}