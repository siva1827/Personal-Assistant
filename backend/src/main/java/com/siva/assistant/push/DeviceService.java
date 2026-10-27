package com.siva.assistant.push;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceService {

    /** A person rarely has more than a few devices; the least recently used are dropped beyond this. */
    static final int MAX_DEVICES_PER_USER = 10;

    private final DeviceTokenRepository devices;
    private final Clock clock;

    public DeviceService(DeviceTokenRepository devices, Clock clock) {
        this.devices = devices;
        this.clock = clock;
    }

    /** Idempotent: registering the same token again just refreshes it, and moves it to this user. */
    @Transactional
    public void register(long userId, String token, DevicePlatform platform) {
        Instant now = Instant.now(clock);
        devices.findByToken(token).ifPresentOrElse(
                existing -> existing.assignTo(userId, platform, now),
                () -> devices.save(new DeviceToken(userId, token, platform, now)));
        trimToLimit(userId);
    }

    /** Called on sign-out so the next person on this phone does not get this account's messages. */
    @Transactional
    public void unregister(long userId, String token) {
        devices.deleteByTokenAndUserId(token, userId);
    }

    private void trimToLimit(long userId) {
        List<DeviceToken> all = devices.findByUserIdOrderByLastSeenAtAscIdAsc(userId);
        for (int i = 0; i < all.size() - MAX_DEVICES_PER_USER; i++) {
            devices.delete(all.get(i));
        }
    }
}
