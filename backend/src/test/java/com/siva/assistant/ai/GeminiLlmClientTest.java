package com.siva.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiLlmClientTest {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-test:generateContent";
    private static final String API_KEY = "AIza-secret-key-123";

    private MockRestServiceServer server;
    private GeminiLlmClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://generativelanguage.googleapis.com");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GeminiLlmClient(builder.build(), API_KEY, "gemini-test");
    }

    private static final String GOOD_REPLY = """
            {"candidates":[{"content":{"role":"model","parts":[{"text":"{\\"recipes\\":[]}"}]},"finishReason":"STOP"}],
             "usageMetadata":{"promptTokenCount":10,"candidatesTokenCount":20,"totalTokenCount":30}}
            """;

    @Test
    void sendsTheRequestTheWayGeminiExpectsAndReadsTheReply() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", API_KEY))
                .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("be careful"))
                .andExpect(jsonPath("$.contents[0].role").value("user"))
                .andExpect(jsonPath("$.contents[0].parts[0].text").value("DATA pantry"))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.type").value("OBJECT"))
                .andExpect(jsonPath("$.generationConfig.responseSchema.properties.recipes.type").value("ARRAY"))
                .andRespond(withSuccess(GOOD_REPLY, MediaType.APPLICATION_JSON));

        LlmReply reply = client.generate(LlmRequest.ofUser("be careful", "DATA pantry"));

        assertThat(reply.text()).isEqualTo("{\"recipes\":[]}");
        assertThat(reply.totalTokens()).isEqualTo(30);
        server.verify();
    }

    @Test
    void keepsTheApiKeyOutOfTheUrl() {
        server.expect(requestTo(URL)).andRespond(withSuccess(GOOD_REPLY, MediaType.APPLICATION_JSON));

        client.generate(LlmRequest.ofUser("s", "u"));

        // requestTo(URL) only matches the exact URL, which has no query string, so the key was not appended
        server.verify();
    }

    @Test
    void sendsARepairConversationWithTheModelsEarlierReply() {
        server.expect(requestTo(URL))
                .andExpect(jsonPath("$.contents.length()").value(3))
                .andExpect(jsonPath("$.contents[1].role").value("model"))
                .andExpect(jsonPath("$.contents[1].parts[0].text").value("oops not json"))
                .andExpect(jsonPath("$.contents[2].role").value("user"))
                .andRespond(withSuccess(GOOD_REPLY, MediaType.APPLICATION_JSON));

        client.generate(LlmRequest.ofUser("s", "u").withFollowUp("oops not json", "fix it"));

        server.verify();
    }

    @Test
    void joinsMultiplePartsAndCopesWithMissingUsageData() {
        String reply = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"a\\\":\"},{\"text\":\"1}\"}]}}]}";
        server.expect(requestTo(URL)).andRespond(withSuccess(reply, MediaType.APPLICATION_JSON));

        LlmReply result = client.generate(LlmRequest.ofUser("s", "u"));

        assertThat(result.text()).isEqualTo("{\"a\":1}");
        assertThat(result.totalTokens()).isNull();
    }

    @Test
    void explainsCommonFailuresWithoutLeakingTheKeyOrTheResponseBody() {
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"message\":\"quota for key " + API_KEY + "\"}}"));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.FORBIDDEN));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.NOT_FOUND));
        server.expect(requestTo(URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        LlmRequest request = LlmRequest.ofUser("s", "u");
        assertThatThrownBy(() -> client.generate(request)).isInstanceOf(LlmException.class)
                .hasMessageContaining("quota").hasMessageNotContaining(API_KEY);
        assertThatThrownBy(() -> client.generate(request)).hasMessageContaining("refused the API key");
        assertThatThrownBy(() -> client.generate(request)).hasMessageContaining("rejected the request");
        assertThatThrownBy(() -> client.generate(request)).hasMessageContaining("GEMINI_MODEL");
        assertThatThrownBy(() -> client.generate(request)).hasMessageContaining("temporarily unavailable");
    }

    @Test
    void reportsBlockedPromptsAndEmptyAnswers() {
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andRespond(withSuccess(
                "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\"}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(URL)).andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        LlmRequest request = LlmRequest.ofUser("s", "u");
        assertThatThrownBy(() -> client.generate(request)).hasMessageContaining("declined").hasMessageContaining("SAFETY");
        assertThatThrownBy(() -> client.generate(request)).hasMessageContaining("empty answer")
                .hasMessageContaining("MAX_TOKENS");
        assertThatThrownBy(() -> client.generate(request)).hasMessageContaining("empty answer");
    }

    @Test
    void reportsNetworkProblems() {
        server.expect(requestTo(URL)).andRespond(withException(new IOException("connection reset")));

        assertThatThrownBy(() -> client.generate(LlmRequest.ofUser("s", "u")))
                .isInstanceOf(LlmException.class).hasMessageContaining("Could not reach");
    }

    @Test
    void reportsTheModelItWasBuiltFor() {
        assertThat(client.modelName()).isEqualTo("gemini-test");
        assertThat(client.isConfigured()).isTrue();
    }
}
