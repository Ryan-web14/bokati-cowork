package com.sni.bokaticowork.features.portal.profile.service;

import com.sni.bokaticowork.features.client.member.dto.request.UpdateMemberRequest;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.features.portal.profile.dto.request.ClientUpdateProfileRequest;
import com.sni.bokaticowork.features.portal.profile.dto.response.ClientProfileResponse;
import com.sni.bokaticowork.security.authentication.service.interfaces.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientProfileService {

    private final MemberService memberService;
    private final AuthenticationService authenticationService;

    @Transactional(readOnly = true)
    public ClientProfileResponse getProfile(Member member) {
        return toResponse(member);
    }

    @Transactional
    public ClientProfileResponse updateProfile(Member member, ClientUpdateProfileRequest request) {
        UpdateMemberRequest update = new UpdateMemberRequest();
        update.setFirstname(request.firstname());
        update.setLastname(request.lastname());
        update.setPhone(request.phone());
        update.setWhatsappPhone(request.whatsappPhone());
        memberService.update(member.getMemberId(), update);
        Member refreshed = memberService.getByMemberIdForService(member.getMemberId());
        return toResponse(refreshed);
    }

    public void requestPasswordChange(Member member) {
        authenticationService.resetPassword(member.getEmail());
    }

    private ClientProfileResponse toResponse(Member m) {
        return new ClientProfileResponse(
                m.getMemberId(),
                m.getFirstname(),
                m.getLastname(),
                m.getDisplayName(),
                m.getEmail(),
                m.getPhone(),
                m.getWhatsappPhone(),
                m.getStatus() != null ? m.getStatus().name() : null,
                m.getAvatarUrl(),
                m.getPortalActivatedAt(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }
}
