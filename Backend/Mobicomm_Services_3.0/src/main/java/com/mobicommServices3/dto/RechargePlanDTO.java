package com.mobicommServices3.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RechargePlanDTO {
    private Long planId;
    private String planName;
    private BigDecimal price;
    private Integer validityDays;
    private String dataLimit;
    private String talktime;
    private String sms;
    private String features;
}