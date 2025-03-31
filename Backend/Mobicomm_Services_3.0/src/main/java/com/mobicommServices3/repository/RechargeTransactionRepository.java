package com.mobicommServices3.repository;

import com.mobicommServices3.model.RechargeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RechargeTransactionRepository extends JpaRepository<RechargeTransaction, Long> {
    List<RechargeTransaction> findByMobileNumber(String mobileNumber);

	
}