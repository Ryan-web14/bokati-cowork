package com.sni.bokaticowork.features.notification.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.notification.dto.response.NotificationMessageResponse;
import com.sni.bokaticowork.features.notification.enums.NotificationChannel;
import com.sni.bokaticowork.features.notification.service.interfaces.NotificationService;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPath.V1 + "/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private static final int MAX_UNREAD_LIMIT = 50;

    private final NotificationService notificationService;

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationMessageResponse>> listUnread(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "20") int limit) {
        String email = resolveEmail(principal);
        int safeLimit = Math.max(1, Math.min(limit, MAX_UNREAD_LIMIT));
        return ResponseEntity.ok(notificationService.listUnread(email, NotificationChannel.IN_APP, safeLimit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread(
            @AuthenticationPrincipal UserPrincipal principal) {
        String email = resolveEmail(principal);
        return ResponseEntity.ok(Map.of("count",
                notificationService.countUnread(email, NotificationChannel.IN_APP)));
    }

    @PatchMapping("/{notificationNumber}/read")
    public ResponseEntity<NotificationMessageResponse> markAsRead(
            @PathVariable String notificationNumber) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationNumber));
    }

    @RequestMapping(value = "/read-all", method = {RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllRead(resolveEmail(principal));
        return ResponseEntity.noContent().build();
    }

    private String resolveEmail(UserPrincipal principal) {
        if (principal == null || principal.getUsername() == null) {
            throw new IllegalStateException("Authenticated admin email is required");
        }
        return principal.getUsername();
    }
}
