package com.mobicommServices3.dto;

import java.math.BigDecimal;

import java.time.LocalDateTime;

import com.mobicommServices3.model.Payment;

public record PaymentResponse(
	    Long id,
	    Long transactionId,
	    BigDecimal amount,
	    String paymentMethod,
	    String status,
	    LocalDateTime paymentDate
	) {
	    public static PaymentResponse fromPayment(Payment payment) {
	        return new PaymentResponse(
	            payment.getPaymentId(),
	            payment.getTransaction().getTransactionId(),
	            payment.getAmount(),
	            payment.getPaymentMethod().name(),
	            payment.getStatus().name(),
	            payment.getPaymentDate()
	        );
	    }
	}