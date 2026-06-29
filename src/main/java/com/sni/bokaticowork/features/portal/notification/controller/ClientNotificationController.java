package com.sni.bokaticowork.features.portal.notification.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.notification.dto.ClientNotificationResponse;
import com.sni.bokaticowork.features.portal.notification.service.ClientNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiPath.V1 + "/client/notifications")
@RequiredArgsConstructor
public class ClientNotificationController {

    private final ClientContextService clientContextService;
    private final ClientNotificationService clientNotificationService;

    @GetMapping
    public ResponseEntity<List<ClientNotificationResponse>> listUnread(
            @RequestParam(defaultValue = "20") int limit) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientNotificationService.listUnread(member, limit));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> countUnread() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(Map.of("count", clientNotificationService.countUnread(member)));
    }

    @PatchMapping("/{notificationNumber}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String notificationNumber) {
        Member member = clientContextService.getAuthenticatedMember();
        clientNotificationService.markAsRead(member, notificationNumber);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.RequestMapping(value = "/read-all", method = {org.springframework.web.bind.annotation.RequestMethod.PATCH, org.springframework.web.bind.annotation.RequestMethod.POST})
    public ResponseEntity<Void> markAllRead() {
        Member member = clientContextService.getAuthenticatedMember();
        clientNotificationService.markAllRead(member);
        return ResponseEntity.noContent().build();
    }
}
