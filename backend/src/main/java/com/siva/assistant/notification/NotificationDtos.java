package com.siva.assistant.notification;

import java.time.Instant;
import java.util.List;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(Long id, NotificationType type, String title, String body, String link,
                                       boolean read, Instant createdAt) {

        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                    n.getReadAt() != null, n.getCreatedAt());
        }
    }

    public record NotificationFeed(long unread, List<NotificationResponse> items) {
    }
}
