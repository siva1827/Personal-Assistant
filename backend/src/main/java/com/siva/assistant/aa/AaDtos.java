package com.siva.assistant.aa;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AaDtos {

    private AaDtos() {
    }

    /** configured is false when no provider is set up; mock is true for the demo provider. */
    public record StatusResponse(boolean configured, boolean mock, String provider) {
    }

    /** historyDays: how many days of past transactions the consent covers (default 90). */
    public record CreateConsentRequest(@Min(7) @Max(365) Integer historyDays) {
    }

    public record AccountResponse(Long id, String bankName, String maskedNumber, String accountType,
                                  BigDecimal balance, Instant balanceAt) {

        static AccountResponse from(BankAccount a) {
            return new AccountResponse(a.getId(), a.getBankName(), a.getMaskedNumber(), a.getAccountType(),
                    a.getBalance(), a.getBalanceAt());
        }
    }

    public record ConsentResponse(Long id, ConsentStatus status, String purpose, LocalDate dataFrom, LocalDate dataTo,
                                  Instant createdAt, Instant approvedAt, Instant expiresAt, Instant lastSyncedAt,
                                  String redirectUrl, List<AccountResponse> accounts) {
    }

    /** What a sync did. creditsIgnored are incoming payments, which the app does not track. */
    public record SyncResult(int imported, int skipped, int creditsIgnored, List<AccountResponse> accounts) {
    }
}
