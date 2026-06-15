package com.sni.bokaticowork.features.portal.session.service;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.session.dto.response.ClientSessionResponse;
import com.sni.bokaticowork.security.model.RefreshToken;
import com.sni.bokaticowork.security.repository.RefreshTokenRepository;
import com.sni.bokaticowork.security.service.tokenService.interfaces.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClientSessionService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public List<ClientSessionResponse> listSessions(Member member) {
        long userId = member.getUser().getId();
        return refreshTokenRepository.findAllByUser_IdAndRevokedFalse(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void revokeSession(Member member, Long sessionId) {
        long userId = member.getUser().getId();
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));
        if (token.getUser().getId() != userId) {
            throw new ResourceNotFoundException("Session not found");
        }
        refreshTokenService.revokeToken(token.getToken());
    }

    @Transactional
    public void revokeAllSessions(Member member) {
        refreshTokenService.revokeAllTokenForUser(member.getUser().getId());
    }

    private ClientSessionResponse toResponse(RefreshToken token) {
        boolean active = !token.isRevoked()
                && token.getExpiration() != null
                && token.getExpiration().isAfter(Instant.now());
        return new ClientSessionResponse(token.getId(), token.getExpiration(), active);
    }
}
