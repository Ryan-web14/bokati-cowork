package com.sni.bokaticowork.features.portal.profile.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.profile.dto.request.ClientUpdateProfileRequest;
import com.sni.bokaticowork.features.portal.profile.dto.response.ClientProfileResponse;
import com.sni.bokaticowork.features.portal.profile.service.ClientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.V1 + "/client/profile")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientContextService clientContextService;
    private final ClientProfileService clientProfileService;

    @GetMapping
    public ResponseEntity<ClientProfileResponse> getProfile() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientProfileService.getProfile(member));
    }

    @PatchMapping
    public ResponseEntity<ClientProfileResponse> updateProfile(
            @Valid @RequestBody ClientUpdateProfileRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientProfileService.updateProfile(member, request));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> requestPasswordChange() {
        Member member = clientContextService.getAuthenticatedMember();
        clientProfileService.requestPasswordChange(member);
        return ResponseEntity.noContent().build();
    }
}
