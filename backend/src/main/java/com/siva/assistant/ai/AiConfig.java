package com.siva.assistant.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiConfig {

    private static final Logger log = LoggerFactory.getLogger(AiConfig.class);
    private static final Pattern SAFE_MODEL = Pattern.compile("[A-Za-z0-9._-]{1,80}");

    /**
     * Gemini when a key is set (or AI_PROVIDER=gemini), a clearly labelled demo with
     * AI_PROVIDER=demo, otherwise "not configured". Asking for Gemini without a key stops startup.
     */
    @Bean
    LlmClient llmClient(AiProperties props, ObjectMapper mapper) {
        AiProperties.Gemini gemini = props.gemini();
        switch (props.provider()) {
            case "none":
                log.info("AI suggestions are off (AI_PROVIDER=none)");
                return new NoLlmClient();
            case "demo":
                log.warn("AI suggestions are running in DEMO mode: recipes are made up, not from a real model");
                return new DemoLlmClient(mapper);
            case "gemini":
                if (gemini.apiKey().isEmpty()) {
                    throw new IllegalStateException("AI_PROVIDER=gemini needs GEMINI_API_KEY to be set");
                }
                return gemini(gemini);
            case "auto":
                if (gemini.apiKey().isEmpty()) {
                    log.info("AI suggestions are off: no GEMINI_API_KEY set (see docs/AI_SETUP.md)");
                    return new NoLlmClient();
                }
                return gemini(gemini);
            default:
                throw new IllegalStateException("Unknown AI_PROVIDER '" + props.provider()
                        + "'. Use auto, gemini, demo or none");
        }
    }

    private static LlmClient gemini(AiProperties.Gemini gemini) {
        if (!SAFE_MODEL.matcher(gemini.model()).matches()) {
            throw new IllegalStateException("GEMINI_MODEL contains unexpected characters");
        }
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(gemini.timeoutSeconds()));
        RestClient http = RestClient.builder().baseUrl(gemini.baseUrl()).requestFactory(factory).build();
        log.info("AI suggestions are on, using Gemini model {}", gemini.model());
        return new GeminiLlmClient(http, gemini.apiKey(), gemini.model());
    }
}
