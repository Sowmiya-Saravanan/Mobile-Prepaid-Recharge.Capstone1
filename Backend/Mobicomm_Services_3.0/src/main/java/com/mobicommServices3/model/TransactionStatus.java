package com.mobicommServices3.model;

public enum TransactionStatus {
	PENDING,    // Transaction initiated but not yet processed
    SUCCESSFUL, // Payment completed successfully
    FAILED,     // Payment failed
    QUEUED,     // Transaction queued for processing
    CANCELLED   // Transaction cancelled by user
}