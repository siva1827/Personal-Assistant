package com.siva.assistant.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

class AiConfigTest {

    private final AiConfig config = new AiConfig();

    private LlmClient client(String provider, String key, String model) {
        AiProperties props = new AiProperties(provider, 20, 30, new AiProperties.Gemini(key, model, null, 0));
        return config.llmClient(props, Jackson2ObjectMapperBuilder.json().build());
    }

    @Test
    void autoUsesGeminiOnlyWhenAKeyIsSet() {
        assertThat(client("auto", "", null)).isInstanceOf(NoLlmClient.class);
        assertThat(client("auto", "   ", null)).isInstanceOf(NoLlmClient.class);

        LlmClient gemini = client("auto", "AIza-key", "gemini-2.5-flash");
        assertThat(gemini).isInstanceOf(GeminiLlmClient.class);
        assertThat(gemini.isConfigured()).isTrue();
        assertThat(gemini.modelName()).isEqualTo("gemini-2.5-flash");
    }

    @Test
    void anUnconfiguredClientSaysSoAndRefusesToGenerate() {
        LlmClient none = client("auto", "", null);

        assertThat(none.isConfigured()).isFalse();
        assertThatThrownBy(() -> none.generate(LlmRequest.ofUser("s", "u"))).isInstanceOf(LlmException.class);
    }

    @Test
    void demoAndNoneAreExplicitChoices() {
        assertThat(client("demo", "", null)).isInstanceOf(DemoLlmClient.class);
        assertThat(client("demo", "", null).modelName()).isEqualTo("demo");
        assertThat(client("none", "AIza-key", null)).isInstanceOf(NoLlmClient.class);
    }

    @Test
    void askingForGeminiWithoutAKeyStopsStartup() {
        assertThatThrownBy(() -> client("gemini", "", null)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GEMINI_API_KEY");
    }

    @Test
    void rejectsUnknownProvidersAndSuspiciousModelNames() {
        assertThatThrownBy(() -> client("chatgpt", "key", null)).hasMessageContaining("Unknown AI_PROVIDER");
        assertThatThrownBy(() -> client("gemini", "key", "../../etc/passwd")).hasMessageContaining("GEMINI_MODEL");
        assertThatThrownBy(() -> client("gemini", "key", "model name?x=1")).hasMessageContaining("GEMINI_MODEL");
    }

    @Test
    void propertiesFallBackToSensibleDefaults() {
        AiProperties props = new AiProperties(null, 0, -1, null);

        assertThat(props.provider()).isEqualTo("auto");
        assertThat(props.dailyLimit()).isEqualTo(20);
        assertThat(props.cacheMinutes()).isEqualTo(30);
        assertThat(props.gemini().model()).isEqualTo("gemini-2.5-flash");
        assertThat(props.gemini().baseUrl()).isEqualTo("https://generativelanguage.googleapis.com");
        assertThat(props.gemini().timeoutSeconds()).isEqualTo(60);
        assertThat(new AiProperties(" DEMO ", 5, 0, null).provider()).isEqualTo("demo");
        assertThat(new AiProperties("auto", 5, 0, null).cacheMinutes()).isZero();
    }
}
