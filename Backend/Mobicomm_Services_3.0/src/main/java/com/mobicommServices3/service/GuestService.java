package com.mobicommServices3.service;

import com.mobicommServices3.dto.RechargePlanDTO;
import com.mobicommServices3.dto.TransactionDTO;
import com.mobicommServices3.exception.MobileNumberNotFoundException; // Add this import
import com.mobicommServices3.model.RechargePlan;
import com.mobicommServices3.model.RechargeTransaction;
import com.mobicommServices3.model.Subscriber;
import com.mobicommServices3.repository.RechargeTransactionRepository;
import com.mobicommServices3.repository.SubscriberRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GuestService {

    @Autowired
    private RechargeService rechargeService;

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private RechargeTransactionRepository rechargeTransactionRepository;

    @Transactional
    public Long initiateGuestRecharge(String mobileNumber, Long planId) {
        if (mobileNumber == null || !mobileNumber.matches("\\d{10}")) {
            throw new IllegalArgumentException("Invalid mobile number. Must be a 10-digit number.");
        }
        if (!rechargeService.subscriberExists(mobileNumber)) {
            throw new MobileNumberNotFoundException("The mobile number is not registered in MobiComm."); // Updated
        }

        RechargeTransaction transaction = rechargeService.initiateRecharge(mobileNumber, planId);
        log.info("Guest recharge initiated for {} with plan ID: {}, transaction ID: {}", 
                 mobileNumber, planId, transaction.getTransactionId());
        return transaction.getTransactionId();
    }

    public List<TransactionDTO> getGuestTransactions(String mobileNumber) {
        Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("Subscriber not found: " + mobileNumber));

        List<RechargeTransaction> transactions = rechargeTransactionRepository.findByMobileNumber(mobileNumber);
        log.info("Fetched {} transactions for guest {}", transactions.size(), mobileNumber);

        return transactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void checkMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.matches("\\d{10}")) {
            throw new IllegalArgumentException("Invalid mobile number. Must be a 10-digit number.");
        }
        if (!rechargeService.subscriberExists(mobileNumber)) {
            throw new MobileNumberNotFoundException("The mobile number is not registered in MobiComm."); // Updated
        }
        log.info("Mobile number {} is valid and registered", mobileNumber);
    }

    private TransactionDTO convertToDTO(RechargeTransaction transaction) {
        RechargePlan plan = transaction.getRechargePlan();
        RechargePlanDTO planDTO = new RechargePlanDTO(
            plan.getPlanId(),
            plan.getPlanName(),
            plan.getPrice(),
            plan.getValidityDays(),
            plan.getDataLimit(),
            plan.getTalktime(),
            plan.getSms(),
            plan.getFeatures()
        );

        return new TransactionDTO(
            transaction.getTransactionId(),
            transaction.getMobileNumber(),
            transaction.getAmount(),
            transaction.getStatus().name(),
            transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : null,
            transaction.getPaymentProvider(),
            planDTO,
            transaction.getExpiryDate(),
            transaction.getTransactionDate()
        );
    }
}