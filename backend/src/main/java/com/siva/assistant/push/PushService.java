package com.siva.assistant.push;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PushService {

    /** devices is how many were tried; delivered is how many Firebase accepted. */
    public record Summary(boolean configured, int devices, int delivered) {
    }

    private final PushSender sender;
    private final DeviceTokenRepository devices;

    public PushService(PushSender sender, DeviceTokenRepository devices) {
        this.sender = sender;
        this.devices = devices;
    }

    /** Sends to every device the user registered, and forgets devices Firebase says no longer exist. */
    public Summary sendToUser(long userId, PushMessage message) {
        if (!sender.isConfigured()) {
            return new Summary(false, 0, 0);
        }
        List<DeviceToken> tokens = devices.findByUserId(userId);
        int delivered = 0;
        for (DeviceToken device : tokens) {
            PushResult result = sender.send(device.getToken(), message);
            if (result == PushResult.DELIVERED) {
                delivered++;
            } else if (result == PushResult.INVALID_TOKEN) {
                devices.deleteByToken(device.getToken());
            }
        }
        return new Summary(true, tokens.size(), delivered);
    }
}
