package com.mobicommServices3.service;

import com.mobicommServices3.model.AppUser;

import com.mobicommServices3.model.Role;
import com.mobicommServices3.model.Subscriber;
import com.mobicommServices3.repository.AppUserRepository;
import com.mobicommServices3.repository.SubscriberRepository;
import com.mobicommServices3.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    @Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ConcurrentHashMap<String, String> otpStore = new ConcurrentHashMap<>();

    public String requestOtp(String mobileNumber) {
        SecureRandom random = new SecureRandom();
        int otpInt = 1000 + random.nextInt(9000);
        String otp = String.valueOf(otpInt);

        Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                .orElseGet(() -> {
                    AppUser newUser = new AppUser();
                    newUser.setUsername(mobileNumber); // Use mobileNumber as username
                    newUser.setEmail(mobileNumber + "@example.com");
                    newUser.setPasswordHash(passwordEncoder.encode("defaultPassword"));
                    newUser.setRole(Role.SUBSCRIBER);
                    newUser.setCreatedAt(LocalDateTime.now());
                    newUser.setIsActive(true);
                    appUserRepository.save(newUser);

                    Subscriber newSubscriber = new Subscriber();
                    newSubscriber.setUser(newUser);
                    newSubscriber.setMobileNumber(mobileNumber);
                    newSubscriber.setIsActive(true);
                    return subscriberRepository.save(newSubscriber);
                });

        otpStore.put(mobileNumber, otp);
        return "OTP for " + mobileNumber + " is: " + otp;
    }

    public String verifyOtp(String mobileNumber, String otp) {
        String storedOtp = otpStore.get(mobileNumber);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("Subscriber not found with mobile: " + mobileNumber));
        AppUser user = subscriber.getUser();
        if (user == null) {
            throw new RuntimeException("User details not found for subscriber");
        }

        String token = jwtUtils.generateToken(mobileNumber, user.getRole().name()); // Use mobileNumber
        otpStore.remove(mobileNumber);
        return token;
    }
}