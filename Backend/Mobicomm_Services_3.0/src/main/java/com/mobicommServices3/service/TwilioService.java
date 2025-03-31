package com.mobicommServices3.service;

import com.mobicommServices3.config.TwilioConfig;
import com.mobicommServices3.model.Subscriber;
import com.mobicommServices3.repository.CategoryRepository;
import com.mobicommServices3.repository.RechargePlanRepository;
import com.mobicommServices3.repository.RechargeTransactionRepository;
import com.mobicommServices3.repository.SubscriberRepository;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Random;

@Service
public class TwilioService {

	@Autowired
    private SubscriberRepository subscriberRepository;

    @Autowired
    private RechargePlanRepository rechargePlanRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RechargeTransactionRepository transactionRepository;
    
    @Autowired
    private TwilioConfig twilioConfig;
    
 

    // Send OTP (already used in AuthController)
    public String sendOtp(String mobileNumber) {
        String otp = generateOtp();
        String messageBody = "Your OTP for MobiComm is: " + otp;
        sendSms(mobileNumber, messageBody);
        return otp;
    }

    // Generic method to send SMS
    public Message sendSms(String mobileNumber, String messageBody) {
        try {
            String formattedNumber = mobileNumber.startsWith("+") ? mobileNumber : "+91" + mobileNumber;
            Message message = Message.creator(
                new PhoneNumber(formattedNumber),
                new PhoneNumber(twilioConfig.getPhoneNumber()),
                messageBody
            ).create();
            System.out.println("SMS sent to " + formattedNumber + " with SID: " + message.getSid());
            return message; // Return the Message object
        } catch (Exception e) {
            System.err.println("Failed to send SMS: " + e.getMessage());
            throw new RuntimeException("Failed to send SMS: " + e.getMessage());
        }
    }
    
    
    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}