package com.siva.assistant.push;

public enum PushResult {
    DELIVERED,
    /** The device is gone (app uninstalled, token expired); the token should be deleted. */
    INVALID_TOKEN,
    /** A temporary or unexplained failure; the token is kept. */
    FAILED
}
