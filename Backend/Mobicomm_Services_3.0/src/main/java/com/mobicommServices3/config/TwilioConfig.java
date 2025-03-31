package com.mobicommServices3.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String phoneNumber;

    // Default constructor
    public TwilioConfig() {
        // Do nothing here
    }

    @PostConstruct
    public void initTwilio() {
        if (accountSid == null || authToken == null || phoneNumber == null) {
            throw new IllegalStateException("Twilio configuration properties are not set correctly: " +
                    "accountSid=" + accountSid + ", authToken=" + authToken + ", phoneNumber=" + phoneNumber);
        }
        Twilio.init(accountSid, authToken);
        System.out.println("Twilio initialized with Account SID: " + accountSid);
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}