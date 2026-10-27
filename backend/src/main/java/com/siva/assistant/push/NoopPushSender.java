package com.siva.assistant.push;

/** Used when Firebase is not set up, so the rest of the app works exactly the same. */
public class NoopPushSender implements PushSender {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public PushResult send(String deviceToken, PushMessage message) {
        return PushResult.FAILED;
    }
}
