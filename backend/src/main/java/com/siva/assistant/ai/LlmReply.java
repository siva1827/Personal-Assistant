package com.siva.assistant.ai;

/** The model's text and, when the provider reports it, how many tokens the call used. */
public record LlmReply(String text, Integer totalTokens) {
}
