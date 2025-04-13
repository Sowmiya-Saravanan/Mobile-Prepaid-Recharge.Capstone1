package com.mobicommServices3.service;

import com.mobicommServices3.dto.TransactionDTO;
import com.mobicommServices3.dto.RechargePlanDTO;
import com.mobicommServices3.model.*;
import com.mobicommServices3.repository.PaymentRepository;
import com.mobicommServices3.repository.RechargeTransactionRepository;
import com.mobicommServices3.repository.SubscriberRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private RechargeTransactionRepository transactionRepository;
    @Autowired
    private SubscriberRepository subscriberRepository;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private TwilioService twilioService;
    @Autowired
    private RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public Payment initiatePayment(Long transactionId, String paymentMethodStr, BigDecimal amount, String provider) {
        RechargeTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        validateOwnership(transaction);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Transaction is not in PENDING state");
        }

        PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());

        Payment payment = new Payment();
        payment.setTransaction(transaction);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(TransactionStatus.QUEUED);
        payment.setPaymentDate(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);

        transaction.setPaymentMethod(paymentMethod);
        transaction.setPaymentProvider(provider);
        transactionRepository.save(transaction);

        return savedPayment;
    }

    public void confirmPayment(Long transactionId, String paymentMethodStr, String provider) {
        RechargeTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        validateOwnership(transaction);

        if (transaction.getStatus() != TransactionStatus.PENDING && transaction.getStatus() != TransactionStatus.QUEUED) {
            throw new RuntimeException("Transaction cannot be confirmed from current state: " + transaction.getStatus());
        }

        PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());

        transaction.setStatus(TransactionStatus.SUCCESSFUL);
        transaction.setPaymentMethod(paymentMethod);
        transaction.setPaymentProvider(provider);
        transaction.setExpiryDate(LocalDate.now().plusDays(transaction.getRechargePlan().getValidityDays()));
        transactionRepository.save(transaction);

        Payment payment = paymentRepository.findByTransactionTransactionIdAndStatus(transactionId, TransactionStatus.QUEUED)
                .orElseThrow(() -> new RuntimeException("No queued payment found for transaction: " + transactionId));
        payment.setStatus(TransactionStatus.SUCCESSFUL);
        paymentRepository.save(payment);

        updateSubscriber(transaction);
        sendConfirmationEmail(transaction);
        sendConfirmationSms(transaction); // SMS failure won't throw exception
    }

    public void cancelPayment(Long transactionId) {
        RechargeTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        validateOwnership(transaction);

        if (transaction.getStatus() != TransactionStatus.PENDING && transaction.getStatus() != TransactionStatus.QUEUED) {
            throw new RuntimeException("Transaction cannot be cancelled from current state: " + transaction.getStatus());
        }

        transaction.setStatus(TransactionStatus.CANCELLED);
        transactionRepository.save(transaction);

        Payment payment = paymentRepository.findByTransactionTransactionIdAndStatus(transactionId, TransactionStatus.QUEUED)
                .orElse(null);
        if (payment != null) {
            payment.setStatus(TransactionStatus.CANCELLED);
            paymentRepository.save(payment);
        }
    }

    public TransactionDTO getTransaction(Long transactionId, String mobileNumber) {
        RechargeTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        // For guest users, validate mobile number
        if (mobileNumber != null && !transaction.getMobileNumber().equals(mobileNumber)) {
            throw new RuntimeException("Unauthorized access to transaction");
        }

        // For logged-in users, validate ownership
        validateOwnership(transaction);

        RechargePlan plan = transaction.getRechargePlan();
        RechargePlanDTO planDTO = new RechargePlanDTO(plan.getPlanId(), plan.getPlanName(), plan.getPrice(),
                plan.getValidityDays(), plan.getDataLimit(), plan.getTalktime(), plan.getSms(), plan.getFeatures());

        return new TransactionDTO(transaction.getTransactionId(), transaction.getMobileNumber(),
                transaction.getAmount(), transaction.getStatus().name(),
                transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : null,
                transaction.getPaymentProvider(), planDTO, transaction.getExpiryDate(),
                transaction.getTransactionDate());
    }

    public RechargeTransaction getTransactionEntity(Long transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));
    }

    public Map<String, Object> createRazorpayOrder(RechargeTransaction transaction) throws RazorpayException {
        try {
            log.info("Creating Razorpay order for transaction ID: {}", transaction.getTransactionId());
            log.debug("Razorpay Key ID: {}", razorpayKeyId);
            log.debug("Transaction amount: {}", transaction.getAmount());

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", transaction.getAmount().multiply(new BigDecimal(100)).intValue()); // Convert to paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "txn_" + transaction.getTransactionId());

            log.debug("Razorpay order request: {}", orderRequest.toString());

            Order order = razorpayClient.orders.create(orderRequest);

            Payment payment = new Payment();
            payment.setTransaction(transaction);
            payment.setAmount(transaction.getAmount());
            payment.setStatus(TransactionStatus.PENDING);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setRazorpayOrderId(order.get("id"));
            paymentRepository.save(payment);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("amount", order.get("amount"));
            response.put("currency", order.get("currency"));
            response.put("transactionId", transaction.getTransactionId());
            return response;
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for transaction ID {}: {}", transaction.getTransactionId(), e.getMessage(), e);
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }
    public Map<String, Object> verifyAndCompleteRazorpayPayment(String razorpayPaymentId, String razorpayOrderId, 
                                                               String razorpaySignature, Long transactionId) throws Exception {
        // Verify Razorpay signature
        JSONObject attributesJson = new JSONObject();
        attributesJson.put("razorpay_payment_id", razorpayPaymentId);
        attributesJson.put("razorpay_order_id", razorpayOrderId);
        attributesJson.put("razorpay_signature", razorpaySignature);

        try {
            Utils.verifyPaymentSignature(attributesJson, razorpayKeySecret);
        } catch (RazorpayException e) {
            log.error("Razorpay signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Payment signature verification failed: " + e.getMessage());
        }

        RechargeTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        validateOwnership(transaction);

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            throw new RuntimeException("Transaction is not in PENDING state");
        }

        // Update transaction status
        transaction.setStatus(TransactionStatus.SUCCESSFUL);
        transaction.setPaymentMethod(PaymentMethod.UPI); // Adjust based on Razorpay response if needed
        transaction.setPaymentProvider("Razorpay");
        transaction.setExpiryDate(LocalDate.now().plusDays(transaction.getRechargePlan().getValidityDays()));
        transactionRepository.save(transaction);

        // Update payment record
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId)
                .orElse(new Payment());
        payment.setTransaction(transaction);
        payment.setAmount(transaction.getAmount());
        payment.setPaymentMethod(transaction.getPaymentMethod());
        payment.setStatus(TransactionStatus.SUCCESSFUL);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setRazorpayOrderId(razorpayOrderId);
        paymentRepository.save(payment);

        // Update subscriber and send notifications
        updateSubscriber(transaction);
        sendConfirmationEmail(transaction);
        boolean smsSent = sendConfirmationSms(transaction); // Capture SMS status

        Map<String, Object> response = new HashMap<>();
        response.put("status", "Payment successful");
        response.put("smsSent", smsSent);
        return response;
    }
    private void validateOwnership(RechargeTransaction transaction) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String mobileNumberFromToken = auth.getName();
            Subscriber subscriber = subscriberRepository.findByMobileNumber(mobileNumberFromToken)
                    .orElseThrow(() -> new RuntimeException("Subscriber not found for mobile: " + mobileNumberFromToken));
            if (!transaction.getMobileNumber().equals(subscriber.getMobileNumber())) {
                log.error("Unauthorized: Transaction mobile number {} does not match subscriber mobile number {}", 
                        transaction.getMobileNumber(), subscriber.getMobileNumber());
                throw new RuntimeException("Unauthorized: Transaction does not belong to this user");
            }
        } else {
            log.info("No authenticated user found for transaction ID {}, skipping ownership validation", transaction.getTransactionId());
        }
    }

    private void updateSubscriber(RechargeTransaction transaction) {
        Subscriber subscriber = subscriberRepository.findByMobileNumber(transaction.getMobileNumber())
                .orElseGet(() -> {
                    Subscriber newSubscriber = new Subscriber();
                    newSubscriber.setMobileNumber(transaction.getMobileNumber());
                    newSubscriber.setUser(getCurrentUser());
                    return newSubscriber;
                });
        subscriber.setCurrentPlan(transaction.getRechargePlan());
        subscriber.setPlanExpiryDate(transaction.getExpiryDate());
        subscriberRepository.save(subscriber);
    }

    private boolean sendConfirmationSms(RechargeTransaction transaction) {
        try {
            Subscriber subscriber = subscriberRepository.findByMobileNumber(transaction.getMobileNumber())
                    .orElseThrow(() -> new RuntimeException("Subscriber not found"));
            String messageBody = String.format(
                    "Dear %s, your recharge of ₹%.2f for the %s plan has been successfully processed. Transaction ID: %d. Thank you!",
                    subscriber.getUser().getUsername(), transaction.getAmount().doubleValue(),
                    transaction.getRechargePlan().getPlanName(), transaction.getTransactionId());
            twilioService.sendSms(transaction.getMobileNumber(), messageBody);
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS for transaction ID {}: {}", transaction.getTransactionId(), e.getMessage());
            return false;
        }
    }

    private void sendConfirmationEmail(RechargeTransaction transaction) {
        Subscriber subscriber = subscriberRepository.findByMobileNumber(transaction.getMobileNumber())
                .orElseThrow(() -> new RuntimeException("Subscriber not found for mobile number: " + transaction.getMobileNumber()));
        String subscriberEmail = subscriber.getUser().getEmail();
        String subscriberName = subscriber.getUser().getUsername();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(subscriberEmail);
        message.setSubject("MobiComm - Recharge Confirmation");
        message.setText(String.format(
                "Dear %s,\n\nYour recharge has been successfully completed!\n\n" +
                "Transaction ID: %d\nMobile Number: %s\nPlan: %s\nAmount: ₹%.2f\n" +
                "Payment Method: %s%s\nDate: %s\n\nThank you for choosing MobiComm!\nSupport: support@mobicomm.com",
                subscriberName, transaction.getTransactionId(), transaction.getMobileNumber(),
                transaction.getRechargePlan().getPlanName(), transaction.getAmount().doubleValue(),
                transaction.getPaymentMethod() != null ? transaction.getPaymentMethod().name() : "N/A",
                transaction.getPaymentProvider() != null ? " (" + transaction.getPaymentProvider() + ")" : "",
                transaction.getTransactionDate().toString()));
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email for transaction ID {}: {}", transaction.getTransactionId(), e.getMessage());
        }
    }

    private AppUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            String mobileNumber = auth.getName();
            return subscriberRepository.findByMobileNumber(mobileNumber).map(Subscriber::getUser)
                    .orElseThrow(() -> new RuntimeException("User not found for mobile number: " + mobileNumber));
        }
        return null;
    }
}
