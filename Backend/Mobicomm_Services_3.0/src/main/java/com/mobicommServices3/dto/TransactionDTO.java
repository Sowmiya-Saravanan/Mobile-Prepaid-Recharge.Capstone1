package com.mobicommServices3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Long transactionId;
    private String mobileNumber;
    private BigDecimal amount;
    private String status; // Changed to String to match convertToDTO
    private String paymentMethod;    // Remains String for frontend compatibility
    private String paymentProvider;  // e.g., "GOOGLE_PAY"
    private RechargePlanDTO rechargePlan;
    private LocalDate expiryDate;    // Added
    private LocalDateTime transactionDate; // Added
}