package com.siva.assistant.ai;

/** The provider could not give an answer: bad key, quota, blocked content, network, outage. */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
