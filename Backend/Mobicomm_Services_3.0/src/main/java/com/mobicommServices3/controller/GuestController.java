package com.mobicommServices3.controller;

import com.mobicommServices3.dto.TransactionDTO;
import com.mobicommServices3.service.GuestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/guest")
@Slf4j
public class GuestController {

    @Autowired
    private GuestService guestService;

    @PostMapping("/recharge")
    public ResponseEntity<?> guestRecharge(@RequestBody GuestRechargeRequest request) {
        Long transactionId = guestService.initiateGuestRecharge(request.mobileNumber(), request.planId());
        return ResponseEntity.ok(Map.of("transactionId", transactionId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<?> getGuestTransactions(@RequestParam String mobileNumber) {
        List<TransactionDTO> transactions = guestService.getGuestTransactions(mobileNumber);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/check")
    public ResponseEntity<?> checkMobileNumber(@RequestParam String mobileNumber) {
        guestService.checkMobileNumber(mobileNumber);
        return ResponseEntity.ok(Map.of("message", "Mobile number is valid and registered"));
    }
}

record GuestRechargeRequest(String mobileNumber, Long planId) {}
