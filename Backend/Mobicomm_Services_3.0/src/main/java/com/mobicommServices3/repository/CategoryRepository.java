package com.mobicommServices3.repository;
import com.mobicommServices3.model.Category;
import com.mobicommServices3.model.Subscriber;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
	
	
	Optional<Category> findByCategoryName(String categoryName);
	
    List<Category> findByIsActiveTrue();

	
}