package com.siva.assistant.aa;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The data exchanged with an Account Aggregator provider, independent of which provider it is. */
public final class AaModel {

    private AaModel() {
    }

    public enum TransactionType {
        DEBIT, CREDIT
    }

    /** What the user is asked to approve. */
    public record ConsentSpec(String purpose, LocalDate dataFrom, LocalDate dataTo) {
    }

    /** redirectUrl is where a real provider sends the user to approve; the demo provider has none. */
    public record CreatedConsent(String providerRef, String redirectUrl) {
    }

    public record Account(String ref, String bankName, String maskedNumber, String type, BigDecimal balance) {
    }

    public record Transaction(String id, String accountRef, LocalDate date, BigDecimal amount,
                              TransactionType type, String narration) {
    }

    public record FinancialData(List<Account> accounts, List<Transaction> transactions) {
    }
}
