package com.mobicommServices3.controller;

import com.mobicommServices3.dto.AdminLoginRequest;
import com.mobicommServices3.dto.JwtResponse;
import com.mobicommServices3.dto.SubscriberOtpRequest;
import com.mobicommServices3.dto.SubscriberOtpVerificationRequest;
import com.mobicommServices3.model.AppUser;
import com.mobicommServices3.model.Role;
import com.mobicommServices3.model.Subscriber;
import com.mobicommServices3.repository.AppUserRepository;
import com.mobicommServices3.repository.SubscriberRepository;
import com.mobicommServices3.security.JwtUtils;
import com.mobicommServices3.service.TwilioService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private AppUserRepository userRepository;
    @Autowired private SubscriberRepository subscriberRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private TwilioService twilioService;

    private final ConcurrentHashMap<String, String> otpStore = new ConcurrentHashMap<>();
    private final boolean devMode = true; // Toggle for development

    @PostMapping("/login")
    public ResponseEntity<?> adminLogin(@RequestBody @Valid AdminLoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            AppUser user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            String role = user.getRole().name(); // e.g., "ADMIN"
            String token = jwtUtils.generateToken(user.getUsername(), role);
            log.info("Generated token for user {} with role {}", user.getUsername(), role);
            JwtResponse jwtResponse = new JwtResponse(token, user.getUsername(), "ROLE_" + role);
            return ResponseEntity.ok(jwtResponse);
        } catch (BadCredentialsException e) {
            log.warn("Login failed for username: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid credentials"));
        }
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestSubscriberOtp(@RequestBody @Valid SubscriberOtpRequest request) {
        String mobileNumber = request.getMobileNumber();

        // Check if subscriber exists; if not, return an error
        Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                .orElse(null);

        if (subscriber == null) {
            log.warn("No MobiComm user found for mobile number: {}", mobileNumber);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("No MobiComm user found for mobile number: " + mobileNumber));
        }

        // Generate and send OTP via SMS
        String otp = twilioService.sendOtp(mobileNumber);
        otpStore.put(mobileNumber, otp);
        log.info("OTP sent via SMS to mobile {}: {}", mobileNumber, otp);

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent to " + mobileNumber + " via SMS");
        if (devMode) response.put("otp", otp); // Return OTP in dev mode for testing
        return ResponseEntity.ok(response);
    }
    
   

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifySubscriberOtp(@RequestBody @Valid SubscriberOtpVerificationRequest request) {
        String mobileNumber = request.getMobileNumber();
        String providedOtp = request.getOtp();
        String storedOtp = otpStore.get(mobileNumber);
        if (storedOtp == null || !storedOtp.equals(providedOtp)) {
            log.warn("Invalid OTP attempt for {}", mobileNumber);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid OTP"));
        }

        Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("Subscriber not found with mobile: " + mobileNumber));
        AppUser user = subscriber.getUser();
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("User details not found"));
        }

        String token = jwtUtils.generateToken(mobileNumber, user.getRole().name());
        JwtResponse jwtResponse = new JwtResponse(token, mobileNumber, user.getRole().getAuthority());
        otpStore.remove(mobileNumber);
        return ResponseEntity.ok(jwtResponse);
    }
}

record ErrorResponse(String message) {}