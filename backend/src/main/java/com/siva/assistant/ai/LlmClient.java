package com.siva.assistant.ai;

/**
 * The only thing the rest of the app knows about the AI provider. Swapping Gemini for another
 * model means writing one more implementation of this interface.
 */
public interface LlmClient {

    /** False when no provider is set up; suggestions then answer 503 instead of calling anything. */
    boolean isConfigured();

    /** Stored with every request so you can see which model produced what. */
    String modelName();

    /** Asks for recipes as JSON. Throws {@link LlmException} when the provider cannot answer. */
    LlmReply generate(LlmRequest request);
}
