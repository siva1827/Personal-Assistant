package com.siva.assistant.ai;

/** The model answered, but not in the required shape. */
public class InvalidModelResponse extends Exception {

    public InvalidModelResponse(String message) {
        super(message);
    }

    public InvalidModelResponse(String message, Throwable cause) {
        super(message, cause);
    }
}
