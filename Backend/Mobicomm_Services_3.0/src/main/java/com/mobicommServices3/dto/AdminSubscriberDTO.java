package com.mobicommServices3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSubscriberDTO {
    private Long subscriberId;
    private String mobileNumber;
    private String username;
    private String currentPlanName;
    private String planExpiryDate; // Changed from LocalDate to String
    private Boolean isActive;
}