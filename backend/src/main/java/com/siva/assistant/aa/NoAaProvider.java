package com.siva.assistant.aa;

import com.siva.assistant.aa.AaModel.ConsentSpec;
import com.siva.assistant.aa.AaModel.CreatedConsent;
import com.siva.assistant.aa.AaModel.FinancialData;
import java.time.LocalDate;
import java.util.Optional;

/** The default: bank connect is off until a provider is chosen. */
public class NoAaProvider implements AaProvider {

    @Override
    public String name() {
        return "none";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public boolean isMock() {
        return false;
    }

    @Override
    public CreatedConsent createConsent(ConsentSpec spec) {
        throw new UnsupportedOperationException("No Account Aggregator is configured");
    }

    @Override
    public Optional<ConsentStatus> consentStatus(String providerRef) {
        return Optional.empty();
    }

    @Override
    public void revoke(String providerRef) {
    }

    @Override
    public FinancialData fetch(String providerRef, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException("No Account Aggregator is configured");
    }
}
