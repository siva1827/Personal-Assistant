package com.siva.assistant.favorite;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "favorite_recipes")
public class FavoriteRecipe {

    @EmbeddedId
    private FavoriteId id;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false)
    private Instant savedAt;

    protected FavoriteRecipe() {
    }

    public FavoriteRecipe(FavoriteId id) {
        this.id = id;
    }

    public FavoriteId getId() {
        return id;
    }

    public Instant getSavedAt() {
        return savedAt;
    }
}
