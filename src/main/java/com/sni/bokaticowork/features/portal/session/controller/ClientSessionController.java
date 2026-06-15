package com.sni.bokaticowork.features.portal.session.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import com.sni.bokaticowork.features.portal.session.dto.response.ClientSessionResponse;
import com.sni.bokaticowork.features.portal.session.service.ClientSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.V1 + "/client/sessions")
@RequiredArgsConstructor
public class ClientSessionController {

    private final ClientContextService clientContextService;
    private final ClientSessionService clientSessionService;

    @GetMapping
    public ResponseEntity<List<ClientSessionResponse>> listSessions() {
        Member member = clientContextService.getAuthenticatedMember();
        return ResponseEntity.ok(clientSessionService.listSessions(member));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revokeSession(@PathVariable Long sessionId) {
        Member member = clientContextService.getAuthenticatedMember();
        clientSessionService.revokeSession(member, sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> revokeAllSessions() {
        Member member = clientContextService.getAuthenticatedMember();
        clientSessionService.revokeAllSessions(member);
        return ResponseEntity.noContent().build();
    }
}
