package com.mobicommServices3.controller;


import com.mobicommServices3.controller.PaymentController.ErrorResponse;
import com.mobicommServices3.dto.MobileNumberRequest;

import com.mobicommServices3.dto.RechargePlanDTO;
import com.mobicommServices3.dto.SubscriberDTO;
import com.mobicommServices3.dto.TransactionDTO;
import com.mobicommServices3.dto.UserDTO;
import com.mobicommServices3.model.AppUser;
import com.mobicommServices3.model.RechargePlan;
import com.mobicommServices3.model.RechargeTransaction;
import com.mobicommServices3.model.Subscriber;
import com.mobicommServices3.repository.AppUserRepository;
import com.mobicommServices3.repository.RechargeTransactionRepository;
import com.mobicommServices3.repository.SubscriberRepository;
import com.mobicommServices3.security.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/subscriber")
@Slf4j
public class SubscriberController {

    @Autowired
    private RechargeTransactionRepository rechargeTransactionRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/update")
    public ResponseEntity<?> updateProfile(@RequestHeader("Authorization") String authHeader, @RequestBody UpdateProfileRequest request) {
        try {
            String mobileNumber = extractMobileNumber(authHeader);
            Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found: " + mobileNumber));
            AppUser user = subscriber.getUser();

            // Validate email format
            if (!isValidEmail(request.email())) {
                log.warn("Invalid email provided: {}", request.email());
                return ResponseEntity.badRequest().body(new ErrorResponse("Invalid email format"));
            }

            // Check if email is already in use by another user
            if (!user.getEmail().equals(request.email()) && appUserRepository.existsByEmail(request.email())) {
                log.warn("Email already in use: {}", request.email());
                return ResponseEntity.badRequest().body(new ErrorResponse("Email already in use"));
            }

            // Validate new mobile number
            if (!mobileNumber.equals(request.mobileNumber()) && subscriberRepository.existsByMobileNumber(request.mobileNumber())) {
                log.warn("Mobile number already in use: {}", request.mobileNumber());
                return ResponseEntity.badRequest().body(new ErrorResponse("Mobile number already in use"));
            }

            // Update fields
            user.setUsername(request.username());
            user.setEmail(request.email());
            if (request.passwordHash() != null && !request.passwordHash().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(request.passwordHash()));
            }
            subscriber.setMobileNumber(request.mobileNumber());

            subscriberRepository.save(subscriber);
            log.info("Updated profile for subscriber: {}", mobileNumber);
            return ResponseEntity.ok(convertToSubscriberDTO(subscriber));
        } catch (Exception e) {
            log.error("Failed to update profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }



    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            String mobileNumber = extractMobileNumber(authHeader);
            Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found: " + mobileNumber));
            SubscriberDTO subscriberDTO = convertToSubscriberDTO(subscriber);
            log.info("Fetched profile for {}", mobileNumber);
            return ResponseEntity.ok(subscriberDTO);
        } catch (Exception e) {
            log.error("Failed to fetch profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getAllTransactions(@RequestHeader("Authorization") String authHeader) {
        try {
            String mobileNumber = extractMobileNumber(authHeader);
            Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found: " + mobileNumber));
            List<RechargeTransaction> transactions = rechargeTransactionRepository.findByMobileNumber(mobileNumber);
            log.info("Fetched {} transactions for {}", transactions.size(), mobileNumber);

            List<TransactionDTO> transactionDTOs = transactions.stream()
                    .map(this::convertToTransactionDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(transactionDTOs);
        } catch (Exception e) {
            log.error("Failed to fetch transactions: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/validate-number")
    public ResponseEntity<?> validateNumber(@RequestBody MobileNumberRequest request) {
        String mobileNumber = request.getMobileNumber();
        if (!isValidMobileNumber(mobileNumber)) {
            log.warn("Invalid mobile number provided: {}", mobileNumber);
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid 10-digit mobile number"));
        }
        boolean isRegistered = subscriberRepository.findByMobileNumber(mobileNumber).isPresent();
        log.info("Validated number {}: {}", mobileNumber, isRegistered ? "registered" : "not registered");
        return ResponseEntity.ok(isRegistered ? "Number is valid" : "Number not registered with MobiComm");
    }
    
    @GetMapping("/validate-token")
    @PreAuthorize("hasAuthority('ROLE_SUBSCRIBER')")
    public ResponseEntity<Map<String, String>> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String mobileNumber = extractMobileNumber(authHeader);
            Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found: " + mobileNumber));
            log.info("Validated token for subscriber: {}", mobileNumber);
            Map<String, String> response = new HashMap<>();
            response.put("status", "valid");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return ResponseEntity.status(401).body(new HashMap<String, String>() {{
                put("status", "invalid");
                put("error", e.getMessage());
            }});
        }
    }
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkSubscriber(@RequestParam String mobileNumber) {
        if (!isValidMobileNumber(mobileNumber)) {
            log.warn("Invalid mobile number for check: {}", mobileNumber);
            return ResponseEntity.badRequest().body(Map.of("exists", false));
        }
        boolean exists = subscriberRepository.existsByMobileNumber(mobileNumber);
        log.info("Checked number {}: exists = {}", mobileNumber, exists);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/notifications")
    public ResponseEntity<?> updateNotifications(@RequestHeader("Authorization") String authHeader, @RequestBody NotificationPreferencesRequest request) {
        try {
            String mobileNumber = extractMobileNumber(authHeader);
            Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found: " + mobileNumber));

            subscriber.setEmailNotifications(request.emailNotifications());
            subscriber.setSmsNotifications(request.smsNotifications());
            subscriber.setAppNotifications(request.appNotifications());

            subscriberRepository.save(subscriber);
            log.info("Updated notification preferences for subscriber: {}", mobileNumber);
            return ResponseEntity.ok("Notification preferences updated successfully");
        } catch (Exception e) {
            log.error("Failed to update notification preferences: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(@RequestHeader("Authorization") String authHeader) {
        try {
            String mobileNumber = extractMobileNumber(authHeader);
            Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found: " + mobileNumber));

            NotificationPreferencesRequest preferences = new NotificationPreferencesRequest(
                subscriber.getEmailNotifications(),
                subscriber.getSmsNotifications(),
                subscriber.getAppNotifications()
            );

            log.info("Fetched notification preferences for subscriber: {}", mobileNumber);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            log.error("Failed to fetch notification preferences: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(@RequestHeader("Authorization") String authHeader) {
        try {
            String mobileNumber = extractMobileNumber(authHeader);
            Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found: " + mobileNumber));
            AppUser user = subscriber.getUser();

            subscriber.setIsActive(false);
            user.setIsActive(false);

            subscriberRepository.save(subscriber);
            log.info("Deleted account for subscriber: {}", mobileNumber);
            return ResponseEntity.ok("Account deleted successfully");
        } catch (Exception e) {
            log.error("Failed to delete account: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    private SubscriberDTO convertToSubscriberDTO(Subscriber subscriber) {
        UserDTO userDTO = new UserDTO(
            subscriber.getUser().getUserId(),
            subscriber.getUser().getUsername(),
            subscriber.getUser().getEmail(),
            subscriber.getUser().getRole().name(),
            subscriber.getUser().getCreatedAt(),
            subscriber.getUser().getIsActive()
        );

        RechargePlanDTO planDTO = null;
        if (subscriber.getCurrentPlan() != null) {
            RechargePlan plan = subscriber.getCurrentPlan();
            planDTO = new RechargePlanDTO(
                plan.getPlanId(),
                plan.getPlanName(),
                plan.getPrice(),
                plan.getValidityDays(),
                plan.getDataLimit(),
                plan.getTalktime(),
                plan.getSms(),
                plan.getFeatures()
            );
        }

        return new SubscriberDTO(
            subscriber.getSubscriberId(),
            subscriber.getMobileNumber(),
            userDTO,
            planDTO,
            subscriber.getPlanExpiryDate(),
            subscriber.getIsActive(),
            subscriber.getEmailNotifications(),
            subscriber.getSmsNotifications(),
            subscriber.getAppNotifications()
        );
    }

    private TransactionDTO convertToTransactionDTO(RechargeTransaction transaction) {
        RechargePlanDTO planDTO = new RechargePlanDTO(
            transaction.getRechargePlan().getPlanId(),
            transaction.getRechargePlan().getPlanName(),
            transaction.getRechargePlan().getPrice(),
            transaction.getRechargePlan().getValidityDays(),
            transaction.getRechargePlan().getDataLimit(),
            transaction.getRechargePlan().getTalktime(),
            transaction.getRechargePlan().getSms(),
            transaction.getRechargePlan().getFeatures()
        );

        return new TransactionDTO(
            transaction.getTransactionId(),
            transaction.getMobileNumber(),
            transaction.getAmount(),
            transaction.getStatus().name(),
            transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : null,
            transaction.getPaymentProvider(),
            planDTO,
            transaction.getExpiryDate(),
            transaction.getTransactionDate()
        );
    }

    private String extractMobileNumber(String authHeader) {
        String token = authHeader.substring(7);
        return jwtUtils.getUsernameFromToken(token);
    }
    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
        return email != null && Pattern.matches(emailRegex, email);
    }

    private boolean isValidMobileNumber(String mobileNumber) {
        return mobileNumber != null && mobileNumber.matches("\\d{10}");
    }
}

record UpdateProfileRequest(String username, String email, String mobileNumber, String passwordHash) {}
record NotificationPreferencesRequest(boolean emailNotifications, boolean smsNotifications, boolean appNotifications) {}
