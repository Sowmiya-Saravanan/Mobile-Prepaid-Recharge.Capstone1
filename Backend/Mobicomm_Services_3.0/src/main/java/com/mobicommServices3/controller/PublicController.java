package com.mobicommServices3.controller;

import java.util.List;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mobicommServices3.model.RechargePlan;
import com.mobicommServices3.repository.RechargePlanRepository;

@RestController
@RequestMapping("/api")
public class PublicController {

    @Autowired
    private RechargePlanRepository rechargePlanRepository;

    @GetMapping("/public/plans")
    public List<RechargePlan> getActivePlans() {
        return rechargePlanRepository.findByIsActiveTrue();
    }
}