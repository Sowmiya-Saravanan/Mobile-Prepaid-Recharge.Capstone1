package com.mobicommServices3.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.validation.constraints.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "recharge_transactions")
public class RechargeTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @NotNull(message = "Mobile number cannot be null")
    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @NotNull(message = "Recharge plan cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private RechargePlan rechargePlan;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;

    @NotNull(message = "Transaction date cannot be null")
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @NotNull(message = "Amount cannot be null")
    @Positive(message = "Amount must be positive")
    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "payment_method", nullable = true)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_provider", nullable = true)
    private String paymentProvider;
    
  

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference // Manage serialization of payments
    private List<Payment> payments;
}