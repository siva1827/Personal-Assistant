package com.siva.assistant.ai;

/** Used until an API key is added. Everything else in the app works as normal. */
public class NoLlmClient implements LlmClient {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String modelName() {
        return "none";
    }

    @Override
    public LlmReply generate(LlmRequest request) {
        throw new LlmException("No AI provider is configured");
    }
}
