package com.mobicommServices3.model;

import jakarta.validation.constraints.*;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recharge_plans")
public class RechargePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @NotNull(message = "Plan name cannot be null")
    @Size(min = 5, max = 100, message = "Plan name must be between 5 and 100 characters")
    @Column(nullable = false)
    private String planName;

    // Many-to-one relationship with Category
    @NotNull(message = "Category cannot be null")
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    
    @NotNull(message = "Price cannot be null")
    @Positive(message = "Price must be positive")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    
    @NotNull(message = "Validity days cannot be null")
    @Positive(message = "Validity days must be positive")
   
    @Column(nullable = false)
    private int validityDays;

    private String dataLimit;
    private String talktime;
    private String sms;
    
    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String features;
    
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}