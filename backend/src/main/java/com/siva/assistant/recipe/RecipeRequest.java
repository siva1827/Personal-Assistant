package com.siva.assistant.recipe;

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

/** One row per AI call: what was sent, which model answered, how many tokens it cost, and how it ended. */
@Entity
@Table(name = "recipe_requests")
public class RecipeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "theme_id")
    private Long themeId;

    @Column(name = "custom_note")
    private String customNote;

    /** The ingredient names that were sent, as a JSON array. */
    @Column(name = "pantry_snapshot", nullable = false, columnDefinition = "json")
    private String pantrySnapshot;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "tokens_used")
    private Integer tokensUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    @Column(name = "request_hash")
    private String requestHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RecipeRequest() {
    }

    public RecipeRequest(Long userId, Long themeId, String customNote, String pantrySnapshot, String modelName,
                         Integer tokensUsed, RequestStatus status, String requestHash) {
        this.userId = userId;
        this.themeId = themeId;
        this.customNote = customNote;
        this.pantrySnapshot = pantrySnapshot;
        this.modelName = modelName;
        this.tokensUsed = tokensUsed;
        this.status = status;
        this.requestHash = requestHash;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Integer getTokensUsed() {
        return tokensUsed;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public String getRequestHash() {
        return requestHash;
    }
}
