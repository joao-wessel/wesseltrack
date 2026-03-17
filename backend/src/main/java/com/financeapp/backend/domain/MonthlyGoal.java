package com.financeapp.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.YearMonth;

@Entity
@Table(name = "monthly_goal", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "monthKey"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Convert(converter = com.financeapp.backend.domain.YearMonthAttributeConverter.class)
    @Column(nullable = false, length = 7)
    private YearMonth monthKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
}
