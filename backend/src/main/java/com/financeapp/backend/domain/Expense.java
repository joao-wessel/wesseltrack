package com.financeapp.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expense")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, length = 60)
    private String paymentSource;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal originalAmount;

    @Column(nullable = false, length = 140)
    private String description;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private boolean recurring;

    @Column(nullable = false)
    private Integer installmentNumber;

    @Column(nullable = false)
    private Integer installmentCount;

    @Column(length = 64)
    private String installmentGroup;
}
