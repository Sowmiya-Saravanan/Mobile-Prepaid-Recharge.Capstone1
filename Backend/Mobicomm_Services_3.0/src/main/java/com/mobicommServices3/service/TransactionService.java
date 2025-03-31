package com.mobicommServices3.service;

import com.mobicommServices3.model.AppUser;
import com.mobicommServices3.model.PaymentMethod;
import com.mobicommServices3.model.RechargeTransaction;
import com.mobicommServices3.model.Subscriber;
import com.mobicommServices3.model.TransactionStatus;
import com.mobicommServices3.repository.RechargeTransactionRepository;
import com.mobicommServices3.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TransactionService {

    @Autowired
    private RechargeTransactionRepository transactionRepository;

    @Autowired
    private SubscriberRepository subscriberRepository;

    public RechargeTransaction confirmTransaction(Long transactionId, String paymentMethod) {
        RechargeTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        transaction.setStatus(TransactionStatus.SUCCESSFUL);
        try {
            transaction.setPaymentMethod(PaymentMethod.valueOf(paymentMethod.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid payment method: " + paymentMethod);
        }
        return transactionRepository.save(transaction);
    }

    public void cancelTransaction(Long transactionId) {
        RechargeTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);
    }

    public RechargeTransaction findById(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }

    public AppUser findUserByMobileNumber(String mobileNumber) {
        return subscriberRepository.findByMobileNumber(mobileNumber)
                .map(Subscriber::getUser)
                .orElseThrow(() -> new RuntimeException("User not found for mobile number: " + mobileNumber));
    }
}