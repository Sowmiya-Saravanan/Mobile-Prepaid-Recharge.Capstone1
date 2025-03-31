package com.mobicommServices3.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubscriberOtpVerificationRequest {
    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;
    
    @NotBlank(message = "OTP is required")
    private String otp;
}