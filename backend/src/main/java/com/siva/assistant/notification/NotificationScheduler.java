package com.siva.assistant.notification;

import com.siva.assistant.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Creates reminders for every user on a timer, so they exist even when nobody has the app open.
 * Today they are shown in the app; this is also where push messages will be sent from.
 */
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final UserRepository users;
    private final NotificationService notifications;

    public NotificationScheduler(UserRepository users, NotificationService notifications) {
        this.users = users;
        this.notifications = notifications;
    }

    @Scheduled(fixedDelayString = "${app.notifications.interval}", initialDelayString = "PT1M")
    public void run() {
        int created = 0;
        for (Long userId : users.findAllIds()) {
            try {
                created += notifications.generateFor(userId);
            } catch (RuntimeException e) {
                // One user's problem must not stop everyone else's reminders
                log.warn("Could not generate notifications for user {}", userId, e);
            }
        }
        if (created > 0) {
            log.info("Created {} notifications", created);
        }
    }
}
