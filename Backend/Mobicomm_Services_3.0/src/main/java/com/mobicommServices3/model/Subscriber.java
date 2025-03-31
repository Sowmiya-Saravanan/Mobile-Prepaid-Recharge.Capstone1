package com.mobicommServices3.model;

import jakarta.validation.constraints.*;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscribers")
public class Subscriber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long subscriberId;

    @NotNull(message = "User cannot be null")
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @NotNull(message = "Mobile number cannot be null")
    @Pattern(regexp = "\\d{10}", message = "Mobile number must be exactly 10 digits")
    @Column(nullable = false, unique = true)
    private String mobileNumber;

    @ManyToOne
    @JoinColumn(name = "current_plan_id")
    private RechargePlan currentPlan;

    @FutureOrPresent(message = "Plan expiry date cannot be in the past")
    private LocalDate planExpiryDate;

    @Column(nullable = false)
    private Boolean isActive;// Soft delete

    // Add notification preference fields
    @Column(nullable = false)
    private Boolean emailNotifications = true;

    @Column(nullable = false)
    private Boolean smsNotifications = false;

    @Column(nullable = false)
    private Boolean appNotifications = true;
    

    public Subscriber(AppUser user, String mobileNumber, RechargePlan currentPlan, LocalDate planExpiryDate,
            Boolean emailNotifications, Boolean smsNotifications, Boolean appNotifications) {
this.user = user;
this.mobileNumber = mobileNumber;
this.currentPlan = currentPlan;
this.planExpiryDate = planExpiryDate;
this.emailNotifications = emailNotifications != null ? emailNotifications : true;
this.smsNotifications = smsNotifications != null ? smsNotifications : false;
this.appNotifications = appNotifications != null ? appNotifications : true;
// isActive will be set by updateIsActive() during persistence
}
    
    @PrePersist
    @PreUpdate
    public void updateIsActive() {
        this.isActive = this.currentPlan != null; // Set isActive to true if currentPlanId exists, false otherwise
    }
    
    
}