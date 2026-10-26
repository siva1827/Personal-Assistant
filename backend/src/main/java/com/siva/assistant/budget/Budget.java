package com.siva.assistant.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    /** First day of the budget month. */
    @Column(nullable = false)
    private LocalDate month;

    @Column(name = "limit_amount", nullable = false)
    private BigDecimal limitAmount;

    protected Budget() {
    }

    public Budget(Long userId, Long categoryId, LocalDate month, BigDecimal limitAmount) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.month = month;
        this.limitAmount = limitAmount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public LocalDate getMonth() {
        return month;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }
}
