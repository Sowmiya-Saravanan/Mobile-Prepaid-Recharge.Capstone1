package com.mobicommServices3.service;

import com.mobicommServices3.model.Payment;
import com.mobicommServices3.model.PaymentMethod;
import com.mobicommServices3.model.RechargePlan;
import com.mobicommServices3.model.RechargeTransaction;
import com.mobicommServices3.model.TransactionStatus;
import com.mobicommServices3.repository.PaymentRepository;
import com.mobicommServices3.repository.RechargePlanRepository;
import com.mobicommServices3.repository.RechargeTransactionRepository;
import com.mobicommServices3.repository.SubscriberRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class RechargeService {

    private final SubscriberRepository subscriberRepository;
    private final RechargePlanRepository rechargePlanRepository;
    private final RechargeTransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;


    public RechargeService(
            SubscriberRepository subscriberRepository,
            RechargePlanRepository rechargePlanRepository,
            RechargeTransactionRepository transactionRepository,
            PaymentRepository paymentRepository) {
        this.subscriberRepository = subscriberRepository;
        this.rechargePlanRepository = rechargePlanRepository;
        this.transactionRepository = transactionRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public RechargeTransaction initiateRecharge(String mobileNumber, Long planId) {
        // Verify mobile number exists in subscribers
        if (!subscriberRepository.existsByMobileNumber(mobileNumber)) {
            throw new RuntimeException("Mobile number not registered with MobiComm: " + mobileNumber);
        }

        RechargePlan plan = rechargePlanRepository.findById(planId)
            .orElseThrow(() -> new RuntimeException("Plan not found: " + planId));

        RechargeTransaction transaction = new RechargeTransaction();
        transaction.setMobileNumber(mobileNumber);
        transaction.setRechargePlan(plan);
        transaction.setAmount(plan.getPrice());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setExpiryDate(LocalDate.now().plusDays(plan.getValidityDays()));

        return transactionRepository.save(transaction);
    }

    public RechargeTransaction getTransaction(Long transactionId, String mobileNumber) {
        RechargeTransaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!transaction.getMobileNumber().equals(mobileNumber)) {
            throw new RuntimeException("Unauthorized access to transaction");
        }
        return transaction;
    }

    public boolean subscriberExists(String mobileNumber) {
        return subscriberRepository.existsByMobileNumber(mobileNumber);
    }

    @Transactional
    public void confirmRecharge(Long transactionId, String mobileNumber, String paymentMethod) {
        RechargeTransaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!transaction.getMobileNumber().equals(mobileNumber)) {
            throw new RuntimeException("Unauthorized access to transaction");
        }
        if (transaction.getStatus() != TransactionStatus.PENDING && transaction.getStatus() != TransactionStatus.QUEUED) {
            throw new RuntimeException("Transaction already processed or not in a confirmable state");
        }

        Payment payment = new Payment();
        payment.setTransaction(transaction);
        payment.setAmount(transaction.getAmount());
        payment.setPaymentMethod(PaymentMethod.valueOf(paymentMethod.replace(" ", "_").toUpperCase()));
        payment.setStatus(TransactionStatus.SUCCESSFUL);
        payment.setPaymentDate(LocalDateTime.now());
        paymentRepository.save(payment);

        transaction.setStatus(TransactionStatus.SUCCESSFUL);
        transaction.setPaymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        transactionRepository.save(transaction);
    }

    @Transactional
    public void cancelRecharge(Long transactionId, String mobileNumber) {
        RechargeTransaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!transaction.getMobileNumber().equals(mobileNumber)) {
            throw new RuntimeException("Unauthorized access to transaction");
        }
        if (transaction.getStatus() != TransactionStatus.PENDING && transaction.getStatus() != TransactionStatus.QUEUED) {
            throw new RuntimeException("Transaction already processed or not in a cancellable state");
        }

        transaction.setStatus(TransactionStatus.FAILED);
        transactionRepository.save(transaction);
    }
}