package com.siva.assistant.recipe;

import com.siva.assistant.user.DietaryPreference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A recipe as returned by the AI model. The list-like fields are kept as raw JSON text
 * and parsed when building responses, so the stored document matches what the model returned.
 */
@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "cook_time_minutes")
    private Integer cookTimeMinutes;

    private Integer servings;

    private String cuisine;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_type", nullable = false)
    private DietaryPreference dietaryType = DietaryPreference.ANY;

    @Column(nullable = false, columnDefinition = "json")
    private String ingredients;

    @Column(name = "missing_ingredients", nullable = false, columnDefinition = "json")
    private String missingIngredients;

    @Column(nullable = false, columnDefinition = "json")
    private String steps;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Recipe() {
    }

    public Recipe(Long userId, Long requestId, String title, String description, Integer cookTimeMinutes,
                  Integer servings, String cuisine, DietaryPreference dietaryType, String ingredients,
                  String missingIngredients, String steps) {
        this.userId = userId;
        this.requestId = requestId;
        this.title = title;
        this.description = description;
        this.cookTimeMinutes = cookTimeMinutes;
        this.servings = servings;
        this.cuisine = cuisine;
        this.dietaryType = dietaryType;
        this.ingredients = ingredients;
        this.missingIngredients = missingIngredients;
        this.steps = steps;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getRequestId() {
        return requestId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getCookTimeMinutes() {
        return cookTimeMinutes;
    }

    public Integer getServings() {
        return servings;
    }

    public String getCuisine() {
        return cuisine;
    }

    public DietaryPreference getDietaryType() {
        return dietaryType;
    }

    public String getIngredients() {
        return ingredients;
    }

    public String getMissingIngredients() {
        return missingIngredients;
    }

    public String getSteps() {
        return steps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
