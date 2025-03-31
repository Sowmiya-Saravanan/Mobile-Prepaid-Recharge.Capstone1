package com.mobicommServices3.repository;

import com.mobicommServices3.model.Payment;
import com.mobicommServices3.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByTransactionTransactionIdAndStatus(Long transactionId, TransactionStatus status);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
}