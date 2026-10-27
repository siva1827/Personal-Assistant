package com.siva.assistant.ai;

import java.util.List;

/** A provider-neutral request: one system message and a conversation of user and model turns. */
public record LlmRequest(String system, List<Turn> turns) {

    public enum Role {
        USER, MODEL
    }

    public record Turn(Role role, String text) {
    }

    public static LlmRequest ofUser(String system, String userText) {
        return new LlmRequest(system, List.of(new Turn(Role.USER, userText)));
    }

    /** Appends a model reply and a follow-up user message, for asking the model to fix its answer. */
    public LlmRequest withFollowUp(String modelReply, String userMessage) {
        var next = new java.util.ArrayList<>(turns);
        next.add(new Turn(Role.MODEL, modelReply));
        next.add(new Turn(Role.USER, userMessage));
        return new LlmRequest(system, List.copyOf(next));
    }
}
