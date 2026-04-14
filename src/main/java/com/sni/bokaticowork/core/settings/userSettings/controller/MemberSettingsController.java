package com.sni.bokaticowork.core.settings.userSettings.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateMemberSettingsRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateNotificationPreferenceRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.MemberSettingsResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.NotificationPreferenceResponse;
import com.sni.bokaticowork.core.settings.userSettings.service.interfaces.MemberSettingsService;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/members/{memberId}/settings")
public class MemberSettingsController {

    private final MemberSettingsService memberSettingsService;

    @GetMapping
    public ResponseEntity<MemberSettingsResponse> get(@PathVariable String memberId) {
        return ResponseEntity.ok(memberSettingsService.getSettings(memberId));
    }

    @PatchMapping
    @Audited(module = "SETTINGS", action = "UPDATE_MEMBER_SETTINGS", ressource = "user_settings")
    @Idempotent(operation = "MEMBER_SETTINGS_UPDATE")
    public ResponseEntity<MemberSettingsResponse> update(@PathVariable String memberId,
                                                         @Valid @RequestBody UpdateMemberSettingsRequest request,
                                                         @RequestParam(required = false) String reason,
                                                         Authentication authentication) {
        return ResponseEntity.ok(memberSettingsService.updateSettings(memberId, request, actorId(authentication), reason));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationPreferenceResponse>> notifications(@PathVariable String memberId) {
        return ResponseEntity.ok(memberSettingsService.getNotificationPreferences(memberId));
    }

    @PatchMapping("/notifications")
    @Audited(module = "SETTINGS", action = "UPDATE_NOTIFICATION_PREFERENCE", ressource = "notification_preferences")
    @Idempotent(operation = "MEMBER_NOTIFICATION_PREFERENCE_UPDATE")
    public ResponseEntity<List<NotificationPreferenceResponse>> updateNotification(@PathVariable String memberId,
                                                                                   @Valid @RequestBody UpdateNotificationPreferenceRequest request,
                                                                                   @RequestParam(required = false) String reason,
                                                                                   Authentication authentication) {
        return ResponseEntity.ok(memberSettingsService.updateNotificationPreference(memberId, request, actorId(authentication), reason));
    }

    private Long actorId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getUser().getId();
    }
}
