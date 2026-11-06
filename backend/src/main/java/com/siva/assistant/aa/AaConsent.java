package com.siva.assistant.aa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "aa_consents")
public class AaConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String provider;

    @Column(name = "provider_ref", nullable = false)
    private String providerRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConsentStatus status = ConsentStatus.PENDING;

    @Column(nullable = false)
    private String purpose;

    @Column(name = "data_from", nullable = false)
    private LocalDate dataFrom;

    @Column(name = "data_to", nullable = false)
    private LocalDate dataTo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected AaConsent() {
    }

    public AaConsent(Long userId, String provider, String providerRef, String purpose, LocalDate dataFrom,
                     LocalDate dataTo) {
        this.userId = userId;
        this.provider = provider;
        this.providerRef = providerRef;
        this.purpose = purpose;
        this.dataFrom = dataFrom;
        this.dataTo = dataTo;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public String getPurpose() {
        return purpose;
    }

    public LocalDate getDataFrom() {
        return dataFrom;
    }

    public LocalDate getDataTo() {
        return dataTo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }

    /** Approval starts the clock: the permission lasts for validity from now. */
    public void activate(Instant now, java.time.Duration validity) {
        this.status = ConsentStatus.ACTIVE;
        this.approvedAt = now;
        this.expiresAt = now.plus(validity);
    }

    public void moveTo(ConsentStatus next) {
        this.status = next;
    }

    public void markSynced(Instant now) {
        this.lastSyncedAt = now;
    }

    /** Active consents past their end time are no longer usable, whatever the provider says. */
    public boolean isPastExpiry(Instant now) {
        return status == ConsentStatus.ACTIVE && expiresAt != null && !expiresAt.isAfter(now);
    }
}
