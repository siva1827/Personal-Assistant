package com.siva.assistant.aa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bank_accounts")
public class BankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "consent_id", nullable = false)
    private Long consentId;

    @Column(name = "provider_ref", nullable = false)
    private String providerRef;

    @Column(name = "bank_name", nullable = false)
    private String bankName;

    @Column(name = "masked_number", nullable = false)
    private String maskedNumber;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    private BigDecimal balance;

    @Column(name = "balance_at")
    private Instant balanceAt;

    protected BankAccount() {
    }

    public BankAccount(Long userId, Long consentId, String providerRef, String bankName, String maskedNumber,
                       String accountType) {
        this.userId = userId;
        this.consentId = consentId;
        this.providerRef = providerRef;
        this.bankName = bankName;
        this.maskedNumber = maskedNumber;
        this.accountType = accountType;
    }

    public Long getId() {
        return id;
    }

    public Long getConsentId() {
        return consentId;
    }

    public String getProviderRef() {
        return providerRef;
    }

    public String getBankName() {
        return bankName;
    }

    public String getMaskedNumber() {
        return maskedNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Instant getBalanceAt() {
        return balanceAt;
    }

    public void updateBalance(BigDecimal balance, Instant at) {
        this.balance = balance;
        this.balanceAt = at;
    }
}
