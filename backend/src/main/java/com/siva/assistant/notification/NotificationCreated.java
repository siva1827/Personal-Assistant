package com.siva.assistant.notification;

/** Published once for each new notification, after the database transaction commits. */
public record NotificationCreated(long userId, NotificationType type, String title, String body, String link) {
}
