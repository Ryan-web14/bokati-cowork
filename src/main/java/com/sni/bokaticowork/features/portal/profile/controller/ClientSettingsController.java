package com.sni.bokaticowork.features.portal.profile.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateMemberSettingsRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.request.UpdateNotificationPreferenceRequest;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.MemberSettingsResponse;
import com.sni.bokaticowork.core.settings.userSettings.dto.response.NotificationPreferenceResponse;
import com.sni.bokaticowork.core.settings.userSettings.service.interfaces.MemberSettingsService;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Le membre regle lui-meme ses preferences depuis le portail : les memes que celles que le
 * back-office voit sur sa fiche, avec la meme trace au journal.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client/settings")
@RequiredArgsConstructor
public class ClientSettingsController {

    private static final String REASON = "Portail membre";

    private final ClientContextService clientContextService;
    private final MemberSettingsService memberSettingsService;

    @GetMapping
    public ResponseEntity<MemberSettingsResponse> get() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(memberSettingsService.getSettings(member.getMemberId()));
    }

    @PatchMapping
    @Audited(module = "SETTINGS", action = "UPDATE_OWN_SETTINGS", ressource = "user_settings")
    public ResponseEntity<MemberSettingsResponse> update(@Valid @RequestBody UpdateMemberSettingsRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(memberSettingsService.updateSettings(member.getMemberId(), request, actorId(member), REASON));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationPreferenceResponse>> notifications() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(memberSettingsService.getNotificationPreferences(member.getMemberId()));
    }

    @PatchMapping("/notifications")
    @Audited(module = "SETTINGS", action = "UPDATE_OWN_NOTIFICATION_PREFERENCE", ressource = "notification_preferences")
    public ResponseEntity<List<NotificationPreferenceResponse>> updateNotification(
            @Valid @RequestBody UpdateNotificationPreferenceRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(memberSettingsService.updateNotificationPreference(member.getMemberId(), request, actorId(member), REASON));
    }

    private static Long actorId(Member member) {
        return member.getUser() == null ? null : member.getUser().getId();
    }
}
