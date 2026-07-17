package com.whatiread.notification.web;

import com.whatiread.identity.security.CurrentUserId;
import com.whatiread.notification.api.NotificationDto;
import com.whatiread.notification.service.NotificationService;
import com.whatiread.shared.web.ApiPaths;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.NOTIFICATIONS)
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    List<NotificationDto> list(@CurrentUserId UUID userId) {
        return notificationService.list(userId);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markAllRead(@CurrentUserId UUID userId) {
        notificationService.markAllRead(userId);
    }

    @PostMapping("/{notificationId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@CurrentUserId UUID userId, @PathVariable UUID notificationId) {
        notificationService.markRead(userId, notificationId);
    }
}
