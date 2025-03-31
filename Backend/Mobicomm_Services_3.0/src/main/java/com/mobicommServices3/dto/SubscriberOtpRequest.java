package com.mobicommServices3.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubscriberOtpRequest {
    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;
}