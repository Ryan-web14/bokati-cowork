package com.sni.bokaticowork.features.portal.profile.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.profile.dto.request.ClientUpdateProfileRequest;
import com.sni.bokaticowork.features.portal.profile.dto.response.ClientProfileResponse;
import com.sni.bokaticowork.features.portal.profile.dto.response.OnboardingStatusResponse;
import com.sni.bokaticowork.features.portal.profile.service.ClientOnboardingService;
import com.sni.bokaticowork.features.portal.profile.service.ClientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPath.V1 + "/client")
@RequiredArgsConstructor
public class ClientProfileController {

    private final ClientContextService clientContextService;
    private final ClientProfileService clientProfileService;
    private final ClientOnboardingService clientOnboardingService;

    @GetMapping({"/profile", "/me"})
    public ResponseEntity<ClientProfileResponse> getProfile() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientProfileService.getProfile(member));
    }

    @PutMapping("/profile")
    public ResponseEntity<ClientProfileResponse> updateProfile(
            @Valid @RequestBody ClientUpdateProfileRequest request) {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientProfileService.updateProfile(member, request));
    }

    @PostMapping("/profile/change-password")
    public ResponseEntity<Void> requestPasswordChange() {
        Member member = clientContextService.getAuthenticatedMember();
        clientProfileService.requestPasswordChange(member);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/onboarding-status")
    public ResponseEntity<OnboardingStatusResponse> onboardingStatus() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientOnboardingService.getOnboardingStatus(member));
    }
}
