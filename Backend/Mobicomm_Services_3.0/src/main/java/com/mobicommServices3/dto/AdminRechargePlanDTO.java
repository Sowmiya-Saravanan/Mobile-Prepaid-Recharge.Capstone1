package com.mobicommServices3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminRechargePlanDTO {
    private Long planId;
    private String planName;
    private String category; // Added for admin to see the category name
    private BigDecimal price;
    private Integer validityDays;
    private String dataLimit;
    private String talktime;
    private String sms;
    private String features;
    private Boolean isActive; // Added for admin to see the activation status
}