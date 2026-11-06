package com.siva.assistant.aa;

import com.siva.assistant.aa.AaModel.ConsentSpec;
import com.siva.assistant.aa.AaModel.CreatedConsent;
import com.siva.assistant.aa.AaModel.FinancialData;
import java.time.LocalDate;
import java.util.Optional;

/**
 * The only thing the rest of the app knows about an Account Aggregator. Connecting a real one
 * (Setu, Finvu, OneMoney and so on) means writing another implementation of this interface.
 */
public interface AaProvider {

    String name();

    /** False when no provider is set up; the bank connect endpoints then answer 503. */
    boolean isConfigured();

    /** True for the built-in demo provider, which fabricates data and lets the app approve consents itself. */
    boolean isMock();

    CreatedConsent createConsent(ConsentSpec spec);

    /** What the aggregator currently says about the consent; empty when it has nothing to report. */
    Optional<ConsentStatus> consentStatus(String providerRef);

    void revoke(String providerRef);

    /** Accounts and transactions covered by an active consent, between the two dates inclusive. */
    FinancialData fetch(String providerRef, LocalDate from, LocalDate to);

    /** Demo provider only: plays the part of the user approving or rejecting in their AA app. */
    default void simulateDecision(String providerRef, boolean approve) {
        throw new UnsupportedOperationException(name() + " does not simulate approvals");
    }
}
