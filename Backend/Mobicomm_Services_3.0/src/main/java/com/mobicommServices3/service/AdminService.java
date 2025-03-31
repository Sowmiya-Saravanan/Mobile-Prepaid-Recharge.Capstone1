package com.mobicommServices3.service;

import com.mobicommServices3.dto.AdminRechargePlanDTO;
import com.mobicommServices3.dto.AdminSubscriberDTO;
import com.mobicommServices3.dto.CategoryDTO;
import com.mobicommServices3.dto.RechargePlanDTO;
import com.mobicommServices3.exception.AdminException;
import com.mobicommServices3.exception.SubscriberException;
import com.mobicommServices3.model.Category;
import com.mobicommServices3.model.RechargePlan;
import com.mobicommServices3.model.RechargeTransaction;
import com.mobicommServices3.model.Subscriber;
import com.mobicommServices3.repository.CategoryRepository;
import com.mobicommServices3.repository.RechargePlanRepository;
import com.mobicommServices3.repository.RechargeTransactionRepository;
import com.mobicommServices3.repository.SubscriberRepository;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final RechargePlanRepository rechargePlanRepository;
    private final CategoryRepository categoryRepository;
    private final SubscriberRepository subscriberRepository;
    private final RechargeTransactionRepository transactionRepository;
    private final TwilioService twilioService;

    @Autowired
    public AdminService(
            RechargePlanRepository rechargePlanRepository,
            CategoryRepository categoryRepository,
            SubscriberRepository subscriberRepository,
            RechargeTransactionRepository transactionRepository,
            TwilioService twilioService) {
        this.rechargePlanRepository = rechargePlanRepository;
        this.categoryRepository = categoryRepository;
        this.subscriberRepository = subscriberRepository;
        this.transactionRepository = transactionRepository;
        this.twilioService = twilioService;
    }

    // New method to fetch a plan by ID
    public AdminRechargePlanDTO getPlanById(Long id) {
        RechargePlan plan = rechargePlanRepository.findById(id)
                .orElseThrow(() -> new AdminException(
                        AdminException.ErrorCode.PLAN_NOT_FOUND,
                        "Plan not found with ID: " + id
                ));
        return convertToAdminRechargePlanDTO(plan);
    }

    // New method to fetch a category by ID
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AdminException(
                        AdminException.ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found with ID: " + id
                ));
        return convertToCategoryDTO(category);
    }
    // Helper methods for validation
    private void validateMobileNumber(String mobileNumber) {
        if (mobileNumber == null || !mobileNumber.matches("\\d{10}")) {
            throw new SubscriberException(
                    SubscriberException.ErrorCode.INVALID_MOBILE_NUMBER,
                    "Invalid mobile number. It must be a 10-digit number."
            );
        }
    }

    private void validatePrice(BigDecimal price) {
        if (price == null) {
            throw new AdminException(
                    AdminException.ErrorCode.VALIDATION_ERROR,
                    "Price is required"
            );
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new AdminException(
                    AdminException.ErrorCode.INVALID_PRICE,
                    "Price must be greater than zero"
            );
        }
    }

    private void validateValidityDays(Integer validityDays) {
        if (validityDays == null) {
            throw new AdminException(
                    AdminException.ErrorCode.VALIDATION_ERROR,
                    "Validity days is required"
            );
        }
        if (validityDays <= 0) {
            throw new AdminException(
                    AdminException.ErrorCode.INVALID_VALIDITY_DAYS,
                    "Validity days must be greater than zero"
            );
        }
    }

    private void validatePlanName(String planName) {
        if (planName == null || planName.trim().isEmpty()) {
            throw new AdminException(
                    AdminException.ErrorCode.VALIDATION_ERROR,
                    "Plan name is required and cannot be empty"
            );
        }
    }

    private void validateCategoryName(String categoryName) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            throw new AdminException(
                    AdminException.ErrorCode.VALIDATION_ERROR,
                    "Category name is required and cannot be empty"
            );
        }
    }

    // Fetch all plans for admin (active and inactive)
    public List<AdminRechargePlanDTO> getAllPlans() {
        return rechargePlanRepository.findAll().stream()
                .map(this::convertToAdminRechargePlanDTO)
                .collect(Collectors.toList());
    }

    // Fetch only active plans for subscribers (using the original RechargePlanDTO)
    public List<RechargePlanDTO> getActivePlansForSubscribers() {
        return rechargePlanRepository.findByIsActiveTrue().stream()
                .map(this::convertToRechargePlanDTO)
                .collect(Collectors.toList());
    }

    // Save or update a plan
    @Transactional
    public RechargePlan savePlan(RechargePlan plan) {
        // Perform any additional validation if needed
        validatePlanName(plan.getPlanName());
        validatePrice(plan.getPrice());
        validateValidityDays(plan.getValidityDays());

        // Ensure the category is valid if provided
        if (plan.getCategory() != null) {
            Category category = categoryRepository.findById(plan.getCategory().getCategoryId())
                    .orElseThrow(() -> new AdminException(
                            AdminException.ErrorCode.CATEGORY_NOT_FOUND,
                            "Category not found with ID: " + plan.getCategory().getCategoryId()
                    ));
            plan.setCategory(category);
        }

        return rechargePlanRepository.save(plan);
    }

    @Transactional
    public void deletePlan(Long id) {
        if (!rechargePlanRepository.existsById(id)) {
            throw new AdminException(
                    AdminException.ErrorCode.PLAN_NOT_FOUND,
                    "Plan not found with ID: " + id
            );
        }
        rechargePlanRepository.deleteById(id);
    }

    @Transactional
    public void togglePlanActivation(Long planId, Boolean isActive) {
        if (isActive == null) {
            throw new AdminException(
                    AdminException.ErrorCode.VALIDATION_ERROR,
                    "isActive field is required"
            );
        }
        RechargePlan plan = rechargePlanRepository.findById(planId)
                .orElseThrow(() -> new AdminException(
                        AdminException.ErrorCode.PLAN_NOT_FOUND,
                        "Plan not found with ID: " + planId
                ));
        plan.setIsActive(isActive);
        rechargePlanRepository.save(plan);
    }

    // Category Management
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::convertToCategoryDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDTO saveCategory(CategoryDTO categoryDTO) {
        validateCategoryName(categoryDTO.getCategoryName());

        Category category;
        if (categoryDTO.getCategoryId() != null) {
            category = categoryRepository.findById(categoryDTO.getCategoryId())
                    .orElseThrow(() -> new AdminException(
                            AdminException.ErrorCode.CATEGORY_NOT_FOUND,
                            "Category not found with ID: " + categoryDTO.getCategoryId()
                    ));
        } else {
            category = new Category();
        }
        category.setCategoryName(categoryDTO.getCategoryName());
        category.setDescription(categoryDTO.getDescription());
        category.setIsActive(categoryDTO.getIsActive() != null ? categoryDTO.getIsActive() : true);

        Optional<Category> existingCategory = categoryRepository.findByCategoryName(categoryDTO.getCategoryName());
        if (existingCategory.isPresent() && !existingCategory.get().getCategoryId().equals(category.getCategoryId())) {
            throw new AdminException(
                    AdminException.ErrorCode.CATEGORY_NAME_IN_USE,
                    "Category name already exists: " + categoryDTO.getCategoryName()
            );
        }

        Category savedCategory = categoryRepository.save(category);
        return convertToCategoryDTO(savedCategory);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AdminException(
                        AdminException.ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found with ID: " + id
                ));
        category.setIsActive(false);
        categoryRepository.save(category);

        List<RechargePlan> plans = rechargePlanRepository.findByCategory(category);
        plans.forEach(plan -> {
            plan.setIsActive(false);
            rechargePlanRepository.save(plan);
        });
    }

    @Transactional
    public void toggleCategoryActivation(Long categoryId, Boolean isActive) {
        if (isActive == null) {
            throw new AdminException(
                    AdminException.ErrorCode.VALIDATION_ERROR,
                    "isActive field is required"
            );
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AdminException(
                        AdminException.ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found with ID: " + categoryId
                ));
        category.setIsActive(isActive);
        categoryRepository.save(category);

        List<RechargePlan> plans = rechargePlanRepository.findByCategory(category);
        plans.forEach(plan -> {
            plan.setIsActive(isActive);
            rechargePlanRepository.save(plan);
        });
    }

    // Subscriber and Transaction Management
    public List<AdminSubscriberDTO> getAllSubscribers() {
        return subscriberRepository.findAll().stream()
                .map(this::convertToSubscriberDTO)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getTransactionsByMobileNumber(String mobileNumber) {
        validateMobileNumber(mobileNumber);
        List<RechargeTransaction> transactions = transactionRepository.findByMobileNumber(mobileNumber);
        if (transactions.isEmpty()) {
            throw new AdminException(
                    AdminException.ErrorCode.TRANSACTION_NOT_FOUND,
                    "No transactions found for mobile number: " + mobileNumber
            );
        }
        return transactions.stream()
                .map(tx -> Map.of(
                        "transactionDate", tx.getTransactionDate(),
                        "amount", tx.getAmount(),
                        "paymentMethod", tx.getPaymentMethod() != null ? tx.getPaymentMethod().toString() : "N/A",
                        "status", tx.getStatus().toString(),
                        "rechargePlan", Map.of("planName", tx.getRechargePlan().getPlanName())
                ))
                .collect(Collectors.toList());
    }

    public List<AdminSubscriberDTO> getExpiringSubscribers(int days) {
        if (days <= 0) {
            throw new AdminException(
                    AdminException.ErrorCode.VALIDATION_ERROR,
                    "Days must be greater than zero"
            );
        }
        LocalDate now = LocalDate.now();
        LocalDate endDate = now.plusDays(days);
        return subscriberRepository.findExpiringSubscribers(now, endDate).stream()
                .map(this::convertToSubscriberDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, String> sendExpiryNotification(Long subscriberId) {
        Subscriber subscriber = subscriberRepository.findById(subscriberId)
                .orElseThrow(() -> new SubscriberException(
                        SubscriberException.ErrorCode.SUBSCRIBER_NOT_FOUND,
                        "Subscriber not found with ID: " + subscriberId
                ));

        if (subscriber.getCurrentPlan() == null || subscriber.getPlanExpiryDate() == null) {
            throw new AdminException(
                    AdminException.ErrorCode.VALIDATION_ERROR,
                    "Subscriber does not have an active plan or expiry date."
            );
        }

        String mobileNumber = subscriber.getMobileNumber();
        String planName = subscriber.getCurrentPlan().getPlanName();
        LocalDate expiryDate = subscriber.getPlanExpiryDate();
        String message = String.format(
                "Dear %s, your %s plan is expiring on %s. Please recharge to continue enjoying our services!",
                subscriber.getUser() != null ? subscriber.getUser().getUsername() : "Subscriber",
                planName,
                expiryDate.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        );

        Message twilioMessage;
        try {
            twilioMessage = twilioService.sendSms(mobileNumber, message);
        } catch (RuntimeException e) {
            throw new AdminException(
                    AdminException.ErrorCode.NOTIFICATION_FAILED,
                    "Failed to send notification: " + e.getMessage(),
                    e
            );
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Notification sent successfully to subscriber ID: " + subscriberId);
        response.put("twilioSid", twilioMessage.getSid());
        return response;
    }

    // Analytics
    public Map<String, Object> getAnalytics() {
        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalSubscribers", subscriberRepository.count());
        analytics.put("totalRevenue",
                rechargePlanRepository.findAll().stream()
                        .mapToDouble(plan -> plan.getPrice().doubleValue())
                        .sum());
        analytics.put("activePlans", rechargePlanRepository.countByIsActiveTrue());
        analytics.put("renewals", subscriberRepository.findAll().stream()
                .filter(s -> s.getPlanExpiryDate() != null && s.getPlanExpiryDate().isAfter(LocalDate.now()))
                .count());
        return analytics;
    }

    // Helper methods for DTO conversion
    public AdminRechargePlanDTO convertToAdminRechargePlanDTO(RechargePlan plan) {
        return new AdminRechargePlanDTO(
                plan.getPlanId(),
                plan.getPlanName(),
                plan.getCategory() != null ? plan.getCategory().getCategoryName() : null,
                plan.getPrice(),
                plan.getValidityDays(),
                plan.getDataLimit(),
                plan.getTalktime(),
                plan.getSms(),
                plan.getFeatures(),
                plan.getIsActive()
        );
    }

    public RechargePlanDTO convertToRechargePlanDTO(RechargePlan plan) {
        return new RechargePlanDTO(
                plan.getPlanId(),
                plan.getPlanName(),
                plan.getPrice(),
                plan.getValidityDays(),
                plan.getDataLimit(),
                plan.getTalktime(),
                plan.getSms(),
                plan.getFeatures()
        );
    }

    public AdminSubscriberDTO convertToSubscriberDTO(Subscriber subscriber) {
        return new AdminSubscriberDTO(
                subscriber.getSubscriberId(),
                subscriber.getMobileNumber(),
                subscriber.getUser() != null ? subscriber.getUser().getUsername() : "N/A",
                subscriber.getCurrentPlan() != null ? subscriber.getCurrentPlan().getPlanName() : "No Plan",
                subscriber.getPlanExpiryDate() != null ? subscriber.getPlanExpiryDate().toString() : null,
                subscriber.getIsActive() != null ? subscriber.getIsActive() : false
        );
    }

    public CategoryDTO convertToCategoryDTO(Category category) {
        return new CategoryDTO(
                category.getCategoryId(),
                category.getCategoryName(),
                category.getDescription(),
                category.getIsActive()
        );
    }

    public Category findCategoryByName(String categoryName) {
        return categoryRepository.findByCategoryName(categoryName)
                .orElseThrow(() -> new AdminException(
                        AdminException.ErrorCode.CATEGORY_NOT_FOUND,
                        "Category not found: " + categoryName
                ));
    }
    public RechargePlan findPlanById(Long id) {
        return rechargePlanRepository.findById(id)
                .orElseThrow(() -> new AdminException(
                        AdminException.ErrorCode.PLAN_NOT_FOUND,
                        "Plan not found with ID: " + id
                ));
    }

}