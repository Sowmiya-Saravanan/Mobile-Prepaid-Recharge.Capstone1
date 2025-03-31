package com.mobicommServices3.service;

import com.mobicommServices3.model.RechargePlan;

import com.mobicommServices3.repository.RechargePlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RechargePlanService {

    @Autowired
    private RechargePlanRepository rechargePlanRepository;

    public List<RechargePlan> getAllPlans() {
        return rechargePlanRepository.findAll();
    }

	
}