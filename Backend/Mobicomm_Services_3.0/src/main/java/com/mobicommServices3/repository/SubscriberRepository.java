package com.mobicommServices3.repository;

import com.mobicommServices3.model.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
	@Query("SELECT s FROM Subscriber s JOIN FETCH s.user LEFT JOIN FETCH s.currentPlan " +
		       "WHERE s.isActive = true AND s.planExpiryDate BETWEEN :startDate AND :endDate")
		List<Subscriber> findExpiringSubscribers(@Param("startDate") LocalDate startDate, 
		                                        @Param("endDate") LocalDate endDate);

//    @Query("SELECT s FROM Subscriber s JOIN FETCH s.user JOIN FETCH s.currentPlan WHERE s.isActive = true")
//    List<Subscriber> findAllActiveWithDetails();
    
    Optional<Subscriber> findByMobileNumber(String mobileNumber);
    boolean existsByMobileNumber(String mobileNumber);
    
    
    @Override
    @Query("SELECT s FROM Subscriber s LEFT JOIN FETCH s.user LEFT JOIN FETCH s.currentPlan")
    List<Subscriber> findAll();
}