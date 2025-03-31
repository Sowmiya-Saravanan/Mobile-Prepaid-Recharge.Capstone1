package com.mobicommServices3.repository;

import com.mobicommServices3.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    // For Admin login flow
    Optional<AppUser> findByUsername(String username);
    
    // Only if mobileNumber is stored in AppUser (rare if you store it in Subscriber)
    // Optional<AppUser> findByMobileNumber(String mobileNumber);
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);

	
}