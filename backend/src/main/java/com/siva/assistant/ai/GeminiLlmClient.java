package com.siva.assistant.ai;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Google Gemini through the Generative Language REST API.
 * https://ai.google.dev/api/generate-content
 *
 * The API key is sent in a header, never in the URL, so it does not end up in access logs.
 */
public class GeminiLlmClient implements LlmClient {

    private final RestClient http;
    private final String apiKey;
    private final String model;

    /** @param http a client whose base URL is the Gemini API host */
    public GeminiLlmClient(RestClient http, String apiKey, String model) {
        this.http = http;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public LlmReply generate(LlmRequest request) {
        JsonNode response;
        try {
            response = http.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body(request))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpStatusCodeException e) {
            throw new LlmException(explain(e.getStatusCode().value()), e);
        } catch (RestClientException e) {
            throw new LlmException("Could not reach the AI service", e);
        }
        return read(response);
    }

    private Map<String, Object> body(LlmRequest request) {
        List<Map<String, Object>> contents = new ArrayList<>();
        for (LlmRequest.Turn turn : request.turns()) {
            contents.add(Map.of(
                    "role", turn.role() == LlmRequest.Role.USER ? "user" : "model",
                    "parts", List.of(Map.of("text", turn.text()))));
        }
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", RecipeSchema.gemini());
        generationConfig.put("temperature", 0.8);
        generationConfig.put("maxOutputTokens", 8192);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", request.system()))));
        body.put("contents", contents);
        body.put("generationConfig", generationConfig);
        return body;
    }

    private static LlmReply read(JsonNode response) {
        if (response == null) {
            throw new LlmException("The AI service returned nothing");
        }
        String blocked = response.path("promptFeedback").path("blockReason").asText("");
        if (!blocked.isEmpty()) {
            throw new LlmException("The AI service declined this request (" + blocked + ")");
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : response.path("candidates").path(0).path("content").path("parts")) {
            text.append(part.path("text").asText(""));
        }
        if (text.isEmpty()) {
            String reason = response.path("candidates").path(0).path("finishReason").asText("unknown");
            throw new LlmException("The AI service gave an empty answer (" + reason + ")");
        }
        JsonNode tokens = response.path("usageMetadata").path("totalTokenCount");
        return new LlmReply(text.toString(), tokens.isNumber() ? tokens.asInt() : null);
    }

    /** Short, human-readable causes; the response body is deliberately not echoed to users. */
    private static String explain(int status) {
        return switch (status) {
            case 400 -> "The AI service rejected the request. Check that GEMINI_API_KEY is valid and GEMINI_MODEL exists";
            case 401, 403 -> "The AI service refused the API key. Check GEMINI_API_KEY and that the API is enabled";
            case 404 -> "The AI model was not found. Check GEMINI_MODEL";
            case 429 -> "The AI service quota or rate limit was reached. Try again later";
            default -> status >= 500 ? "The AI service is temporarily unavailable" : "The AI service returned an error (" + status + ")";
        };
    }
}
