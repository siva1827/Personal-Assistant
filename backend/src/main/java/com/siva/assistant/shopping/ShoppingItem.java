package com.siva.assistant.shopping;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "shopping_list_items")
public class ShoppingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "estimated_cost")
    private BigDecimal estimatedCost;

    @Column(nullable = false)
    private boolean purchased;

    @Column(name = "source_recipe_id")
    private Long sourceRecipeId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ShoppingItem() {
    }

    public ShoppingItem(Long userId, String name, BigDecimal estimatedCost, Long sourceRecipeId) {
        this.userId = userId;
        this.name = name;
        this.estimatedCost = estimatedCost;
        this.sourceRecipeId = sourceRecipeId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public boolean isPurchased() {
        return purchased;
    }

    public void setPurchased(boolean purchased) {
        this.purchased = purchased;
    }

    public Long getSourceRecipeId() {
        return sourceRecipeId;
    }
}
