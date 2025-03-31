package com.mobicommServices3.dto;

import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriberDTO {
    private Long subscriberId;
    private String mobileNumber;
    private UserDTO user;
    private RechargePlanDTO currentPlan;
    private LocalDate planExpiryDate;
    private Boolean isActive;
    private Boolean emailNotifications; // Added
    private Boolean smsNotifications;   // Added
    private Boolean appNotifications;   // Added
}