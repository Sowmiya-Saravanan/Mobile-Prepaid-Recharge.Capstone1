package com.mobicommServices3.repository;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mobicommServices3.model.Category;
import com.mobicommServices3.model.RechargePlan;


@Repository
public interface RechargePlanRepository extends JpaRepository<RechargePlan, Long> {
    List<RechargePlan> findByIsActiveTrue();
    RechargePlan findByPlanName(String planName); // Already exists
	List<RechargePlan> findByCategory(Category category);
	Object countByIsActiveTrue();
}