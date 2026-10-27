package com.siva.assistant.notification;

import com.siva.assistant.notification.NotificationDtos.NotificationFeed;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public NotificationFeed feed(@AuthenticationPrincipal Long userId) {
        return notificationService.feed(userId);
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        notificationService.markRead(userId, id);
    }

    @PostMapping("/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal Long userId) {
        return Map.of("updated", notificationService.markAllRead(userId));
    }
}
