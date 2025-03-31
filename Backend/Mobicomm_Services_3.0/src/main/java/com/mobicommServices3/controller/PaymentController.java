package com.mobicommServices3.controller;

import com.mobicommServices3.dto.TransactionDTO;
import com.mobicommServices3.model.RechargeTransaction;
import com.mobicommServices3.model.TransactionStatus;
import com.mobicommServices3.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    // Initiate a payment (generic endpoint, can be used for other providers)
    @PostMapping("/initiate")
    public ResponseEntity<?> initiatePayment(@RequestBody PaymentRequest request) {
        try {
            paymentService.initiatePayment(
                request.transactionId(),
                request.paymentMethod(),
                request.amount(),
                request.provider()
            );
            log.info("Payment initiated for transaction ID: {}", request.transactionId());
            return ResponseEntity.ok(Map.of("status", "Payment initiated"));
        } catch (Exception e) {
            log.error("Payment initiation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Confirm a payment (generic endpoint)
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(@RequestBody ConfirmPaymentRequest request) {
        try {
            paymentService.confirmPayment(
                request.transactionId(),
                request.paymentMethod(),
                request.provider()
            );
            log.info("Payment confirmed for transaction ID: {}", request.transactionId());
            return ResponseEntity.ok(Map.of("status", "Payment confirmed"));
        } catch (Exception e) {
            log.error("Payment confirmation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Cancel a payment
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelPayment(@RequestBody CancelPaymentRequest request) {
        try {
            paymentService.cancelPayment(request.transactionId());
            log.info("Payment cancelled for transaction ID: {}", request.transactionId());
            return ResponseEntity.ok(Map.of("status", "Payment cancelled"));
        } catch (Exception e) {
            log.error("Payment cancellation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Get transaction details
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<?> getTransaction(@PathVariable Long transactionId) {
        try {
            TransactionDTO transaction = paymentService.getTransaction(transactionId, null);
            log.info("Fetched transaction ID: {} for guest", transactionId);
            return ResponseEntity.ok(transaction);
        } catch (Exception e) {
            log.error("Failed to fetch transaction: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Razorpay-specific: Create an order
    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestParam Long transactionId) {
        try {
            RechargeTransaction transaction = paymentService.getTransactionEntity(transactionId);
            if (transaction.getStatus() != TransactionStatus.PENDING) {
                throw new RuntimeException("Transaction is not in PENDING state");
            }
            Map<String, Object> orderData = paymentService.createRazorpayOrder(transaction);
            orderData.put("razorpayKey", razorpayKeyId); // Include Razorpay key in response
            log.info("Razorpay order created for transaction ID: {}", transactionId);
            return ResponseEntity.ok(orderData);
        } catch (Exception e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Razorpay-specific: Verify payment
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestParam String razorpayPaymentId,
                                           @RequestParam String razorpayOrderId,
                                           @RequestParam String razorpaySignature,
                                           @RequestParam Long transactionId) {
        try {
            paymentService.verifyAndCompleteRazorpayPayment(razorpayPaymentId, razorpayOrderId, 
                                                            razorpaySignature, transactionId);
            log.info("Razorpay payment verified for transaction ID: {}", transactionId);
            return ResponseEntity.ok(Map.of("status", "Payment successful"));
        } catch (Exception e) {
            log.error("Razorpay payment verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    // Request and response records
    record PaymentRequest(Long transactionId, String paymentMethod, BigDecimal amount, String provider) {}
    record ConfirmPaymentRequest(Long transactionId, String paymentMethod, String provider) {}
    record CancelPaymentRequest(Long transactionId) {}
    record ErrorResponse(String message) {}
}