package com.mobicommServices3.controller;


import com.mobicommServices3.model.RechargePlan;
import com.mobicommServices3.service.RechargePlanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
@Slf4j
public class RechargePlanController {

    @Autowired private RechargePlanService rechargePlanService;

    @GetMapping
    public ResponseEntity<List<RechargePlan>> getAllPlans() {
        log.info("Fetching all recharge plans");
        List<RechargePlan> plans = rechargePlanService.getAllPlans();
        return ResponseEntity.ok(plans);
    }
}