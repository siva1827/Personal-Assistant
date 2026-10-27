package com.siva.assistant.push;

public interface PushSender {

    /** False when no Firebase credentials are set; sending then does nothing. */
    boolean isConfigured();

    PushResult send(String deviceToken, PushMessage message);
}
