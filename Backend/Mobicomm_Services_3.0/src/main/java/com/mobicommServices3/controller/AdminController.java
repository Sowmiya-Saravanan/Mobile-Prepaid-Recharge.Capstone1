	package com.mobicommServices3.controller;
	
	import com.mobicommServices3.dto.AdminRechargePlanDTO;
	import com.mobicommServices3.dto.AdminSubscriberDTO;
	import com.mobicommServices3.dto.CategoryDTO;
	import com.mobicommServices3.dto.RechargePlanDTO;
	import com.mobicommServices3.model.RechargePlan;
	import com.mobicommServices3.service.AdminService;
	import org.springframework.beans.factory.annotation.Autowired;
	import org.springframework.http.ResponseEntity;
	import org.springframework.security.access.prepost.PreAuthorize;
	import org.springframework.web.bind.annotation.*;
	
	import java.math.BigDecimal;
	import java.util.List;
	import java.util.Map;
	
	@RestController
	@RequestMapping("/api/admin")
	public class AdminController {
	
	    private final AdminService adminService;
	
	    @Autowired
	    public AdminController(AdminService adminService) {
	        this.adminService = adminService;
	    }
	
	    // Admin endpoints to manage plans
	    @GetMapping("/plans")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<List<AdminRechargePlanDTO>> getAllPlans() {
	        return ResponseEntity.ok(adminService.getAllPlans());
	    }
	
	    @GetMapping("/plans/{id}")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<AdminRechargePlanDTO> getPlanById(@PathVariable Long id) {
	        AdminRechargePlanDTO plan = adminService.getPlanById(id);
	        return ResponseEntity.ok(plan);
	    }
	
	    @PostMapping("/plans")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<AdminRechargePlanDTO> addPlan(@RequestBody Map<String, Object> requestBody) {
	        RechargePlan plan = new RechargePlan();
	
	        // Set plan name
	        String planName = (String) requestBody.get("planName");
	        if (planName == null || planName.trim().isEmpty()) {
	            throw new IllegalArgumentException("Plan name is required");
	        }
	        plan.setPlanName(planName);
	
	        // Set price with validation
	        Object priceObj = requestBody.get("price");
	        if (priceObj == null) {
	            throw new IllegalArgumentException("Price is required");
	        }
	        try {
	            BigDecimal price = new BigDecimal(priceObj.toString());
	            if (price.compareTo(BigDecimal.ZERO) <= 0) {
	                throw new IllegalArgumentException("Price must be greater than zero");
	            }
	            plan.setPrice(price);
	        } catch (NumberFormatException e) {
	            throw new IllegalArgumentException("Invalid price format: " + priceObj, e);
	        }
	
	        // Set validity days with validation
	        Object validityDaysObj = requestBody.get("validityDays");
	        if (validityDaysObj == null) {
	            throw new IllegalArgumentException("Validity days is required");
	        }
	        try {
	            int validityDays = Integer.parseInt(validityDaysObj.toString());
	            if (validityDays <= 0) {
	                throw new IllegalArgumentException("Validity days must be greater than zero");
	            }
	            plan.setValidityDays(validityDays);
	        } catch (NumberFormatException e) {
	            throw new IllegalArgumentException("Invalid validity days format: " + validityDaysObj, e);
	        }
	
	        // Set other fields
	        plan.setDataLimit((String) requestBody.get("dataLimit"));
	        plan.setTalktime((String) requestBody.get("talktime"));
	        plan.setSms((String) requestBody.get("sms"));
	        plan.setFeatures((String) requestBody.get("features"));
	        plan.setIsActive(true); // Default to active for new plans
	
	        // Handle category
	        String categoryName = (String) requestBody.get("category");
	        if (categoryName != null && !categoryName.isEmpty()) {
	            plan.setCategory(adminService.findCategoryByName(categoryName));
	        }
	
	        RechargePlan savedPlan = adminService.savePlan(plan);
	        return ResponseEntity.ok(adminService.convertToAdminRechargePlanDTO(savedPlan));
	    }
	
	    @PutMapping("/plans/{id}")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<AdminRechargePlanDTO> updatePlan(@PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
	        // Fetch existing plan DTO to get current isActive status
	        AdminRechargePlanDTO existingPlanDTO = adminService.getPlanById(id);
	        RechargePlan plan = new RechargePlan();
	        plan.setPlanId(id);

	        // Set plan name
	        String planName = (String) requestBody.get("planName");
	        if (planName == null || planName.trim().isEmpty()) {
	            throw new IllegalArgumentException("Plan name is required");
	        }
	        plan.setPlanName(planName);

	        // Set price with validation
	        Object priceObj = requestBody.get("price");
	        if (priceObj == null) {
	            throw new IllegalArgumentException("Price is required");
	        }
	        try {
	            BigDecimal price = new BigDecimal(priceObj.toString());
	            if (price.compareTo(BigDecimal.ZERO) <= 0) {
	                throw new IllegalArgumentException("Price must be greater than zero");
	            }
	            plan.setPrice(price);
	        } catch (NumberFormatException e) {
	            throw new IllegalArgumentException("Invalid price format: " + priceObj, e);
	        }

	        // Set validity days with validation
	        Object validityDaysObj = requestBody.get("validityDays");
	        if (validityDaysObj == null) {
	            throw new IllegalArgumentException("Validity days is required");
	        }
	        try {
	            int validityDays = Integer.parseInt(validityDaysObj.toString());
	            if (validityDays <= 0) {
	                throw new IllegalArgumentException("Validity days must be greater than zero");
	            }
	            plan.setValidityDays(validityDays);
	        } catch (NumberFormatException e) {
	            throw new IllegalArgumentException("Invalid validity days format: " + validityDaysObj, e);
	        }

	        // Set other fields
	        plan.setDataLimit((String) requestBody.get("dataLimit"));
	        plan.setTalktime((String) requestBody.get("talktime"));
	        plan.setSms((String) requestBody.get("sms"));
	        plan.setFeatures((String) requestBody.get("features"));
	        plan.setIsActive(existingPlanDTO.getIsActive()); // Preserve existing isActive status

	        // Handle category
	        String categoryName = (String) requestBody.get("category");
	        if (categoryName != null && !categoryName.isEmpty()) {
	            plan.setCategory(adminService.findCategoryByName(categoryName));
	        } else {
	            // Preserve existing category if not provided
	            RechargePlan existingPlan = adminService.findPlanById(id);
	            plan.setCategory(existingPlan.getCategory());
	        }

	        RechargePlan updatedPlan = adminService.savePlan(plan);
	        return ResponseEntity.ok(adminService.convertToAdminRechargePlanDTO(updatedPlan));
	    }
	
	    @DeleteMapping("/plans/{id}")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
	        adminService.deletePlan(id);
	        return ResponseEntity.ok().build();
	    }
	
	    @PutMapping("/plans/{id}/toggle")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<AdminRechargePlanDTO> togglePlanActivation(
	            @PathVariable Long id,
	            @RequestBody Map<String, Boolean> requestBody) {
	        Boolean isActive = requestBody.get("isActive");
	        if (isActive == null) {
	            throw new IllegalArgumentException("isActive field is required");
	        }
	        adminService.togglePlanActivation(id, isActive);
	        AdminRechargePlanDTO updatedPlan = adminService.getPlanById(id);
	        return ResponseEntity.ok(updatedPlan);
	    }
	
	    // Admin endpoints to manage categories
	    @GetMapping("/categories")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
	        return ResponseEntity.ok(adminService.getAllCategories());
	    }
	
	    @PostMapping("/categories")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<CategoryDTO> addCategory(@RequestBody CategoryDTO categoryDTO) {
	        CategoryDTO savedCategory = adminService.saveCategory(categoryDTO);
	        return ResponseEntity.ok(savedCategory);
	    }
	
	    @PutMapping("/categories/{id}")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<CategoryDTO> updateCategory(@PathVariable Long id, @RequestBody CategoryDTO categoryDTO) {
	        categoryDTO.setCategoryId(id);
	        CategoryDTO updatedCategory = adminService.saveCategory(categoryDTO);
	        return ResponseEntity.ok(updatedCategory);
	    }
	
	    @DeleteMapping("/categories/{id}")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
	        adminService.deleteCategory(id);
	        return ResponseEntity.ok().build();
	    }
	
	    @PutMapping("/categories/{id}/toggle")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<CategoryDTO> toggleCategoryActivation(
	            @PathVariable Long id,
	            @RequestBody Map<String, Boolean> requestBody) {
	        Boolean isActive = requestBody.get("isActive");
	        if (isActive == null) {
	            throw new IllegalArgumentException("isActive field is required");
	        }
	        adminService.toggleCategoryActivation(id, isActive);
	        CategoryDTO updatedCategory = adminService.getCategoryById(id);
	        return ResponseEntity.ok(updatedCategory);
	    }
	
	    @GetMapping("/analytics")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<Map<String, Object>> getAnalytics() {
	        return ResponseEntity.ok(adminService.getAnalytics());
	    }
	
	    // Subscriber endpoint to get active plans only (using RechargePlanDTO)
	    @GetMapping("/subscriber/plans")
	    @PreAuthorize("hasRole('ROLE_USER')")
	    public ResponseEntity<List<RechargePlanDTO>> getActivePlansForSubscribers() {
	        return ResponseEntity.ok(adminService.getActivePlansForSubscribers());
	    }
	
	    @GetMapping("/subscribers")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<List<AdminSubscriberDTO>> getAllSubscribers() {
	        return ResponseEntity.ok(adminService.getAllSubscribers());
	    }
	
	    @GetMapping("/subscribers/mobile/{mobileNumber}/transactions")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<List<Map<String, Object>>> getTransactionsByMobileNumber(@PathVariable String mobileNumber) {
	        return ResponseEntity.ok(adminService.getTransactionsByMobileNumber(mobileNumber));
	    }
	
	    @GetMapping("/expiring-subscribers")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<List<AdminSubscriberDTO>> getExpiringSubscribers(@RequestParam("days") int days) {
	        return ResponseEntity.ok(adminService.getExpiringSubscribers(days));
	    }
	
	    @PostMapping("/subscribers/{subscriberId}/notify")
	    @PreAuthorize("hasRole('ROLE_ADMIN')")
	    public ResponseEntity<Map<String, String>> sendExpiryNotification(@PathVariable Long subscriberId) {
	        return ResponseEntity.ok(adminService.sendExpiryNotification(subscriberId));
	    }
	}