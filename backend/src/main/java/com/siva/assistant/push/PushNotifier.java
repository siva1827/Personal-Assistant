package com.siva.assistant.push;

import com.siva.assistant.notification.NotificationCreated;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends each new in-app notification to the user's phones as well. It runs after the
 * notification is safely saved, and in the background, so a slow Firebase call never delays
 * the request that created it and a rolled-back notification is never pushed.
 */
@Component
public class PushNotifier {

    private static final Logger log = LoggerFactory.getLogger(PushNotifier.class);

    private final PushService pushService;

    public PushNotifier(PushService pushService) {
        this.pushService = pushService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreated event) {
        try {
            pushService.sendToUser(event.userId(), new PushMessage(event.title(), event.body(), Map.of(
                    "link", event.link() == null ? "" : event.link(),
                    "type", event.type().name())));
        } catch (RuntimeException e) {
            // Push is a convenience; the notification itself is already saved and visible in the app
            log.warn("Could not push notification to user {}", event.userId(), e);
        }
    }
}
