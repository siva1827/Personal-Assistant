package com.siva.assistant.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * provider: "auto" picks Gemini when a key is set, "gemini" requires a key, "demo" returns
 * made-up recipes without calling anything, "none" disables suggestions.
 */
@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String provider, int dailyLimit, int cacheMinutes, Gemini gemini) {

    public record Gemini(String apiKey, String model, String baseUrl, int timeoutSeconds) {

        public Gemini {
            apiKey = apiKey == null ? "" : apiKey.trim();
            model = model == null || model.isBlank() ? "gemini-2.5-flash" : model.trim();
            baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://generativelanguage.googleapis.com" : baseUrl;
            timeoutSeconds = timeoutSeconds <= 0 ? 60 : timeoutSeconds;
        }
    }

    public AiProperties {
        provider = provider == null || provider.isBlank() ? "auto" : provider.trim().toLowerCase();
        dailyLimit = dailyLimit <= 0 ? 20 : dailyLimit;
        cacheMinutes = cacheMinutes < 0 ? 30 : cacheMinutes;
        gemini = gemini == null ? new Gemini(null, null, null, 0) : gemini;
    }
}
