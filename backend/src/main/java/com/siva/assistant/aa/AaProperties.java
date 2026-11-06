package com.siva.assistant.aa;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * provider: "none" (default) keeps bank connect off; "mock" turns on the built-in demo provider
 * that invents accounts and transactions. consentDays is how long an approved consent lasts;
 * historyDays is how far back the data range of a new consent reaches.
 */
@ConfigurationProperties(prefix = "app.aa")
public record AaProperties(String provider, int consentDays, int historyDays) {

    public AaProperties {
        provider = provider == null || provider.isBlank() ? "none" : provider.trim().toLowerCase();
        consentDays = consentDays <= 0 ? 30 : consentDays;
        historyDays = historyDays <= 0 ? 90 : historyDays;
    }
}
