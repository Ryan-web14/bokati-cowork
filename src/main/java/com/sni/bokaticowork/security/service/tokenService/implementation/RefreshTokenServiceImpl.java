package com.sni.bokaticowork.security.service.tokenService.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.model.RefreshToken;
import com.sni.bokaticowork.security.repository.RefreshTokenRepository;
import com.sni.bokaticowork.security.service.tokenService.interfaces.RefreshTokenService;
import com.sni.bokaticowork.security.service.tokenService.interfaces.TokenHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@RequiredArgsConstructor
@Transactional
@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${app.security.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpiration;

    private final RefreshTokenRepository refreshTokenRepo;
    private final TokenHashService tokenHashService;

    @Override
    public void storeRefreshToken(String token, Users user) {
        String tokenHash = tokenHashService.hash(token);

        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(tokenHash)
                .expiration(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();

        refreshTokenRepo.save(rt);
    }

    @Override
    public boolean isRefreshTokenActive(String token) {
        String tokenHash = tokenHashService.hash(token);

        return refreshTokenRepo.findByTokenAndRevokedFalse(tokenHash)
                .filter(rt -> rt.getExpiration() != null && rt.getExpiration().isAfter(Instant.now()))
                .isPresent();
    }

    @Override
    public RefreshToken getActiveToken(String token) {
        String tokenHash = tokenHashService.hash(token);

        return refreshTokenRepo.findByTokenAndRevokedFalse(tokenHash)
                .filter(rt -> rt.getExpiration() != null && rt.getExpiration().isAfter(Instant.now()))
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found or expired"));
    }

    @Override
    public void rotateToken(String oldToken, String newToken) {
        RefreshToken oldRt = getActiveToken(oldToken);

        oldRt.setRevoked(true);
        refreshTokenRepo.save(oldRt);

        String newTokenHash = tokenHashService.hash(newToken);

        RefreshToken rt = RefreshToken.builder()
                .user(oldRt.getUser())
                .token(newTokenHash)
                .expiration(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();

        refreshTokenRepo.save(rt);
    }

    @Override
    public void revokeToken(String token) {
        String tokenHash = tokenHashService.hash(token);

        refreshTokenRepo.findByTokenAndRevokedFalse(tokenHash).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepo.save(rt);
        });
    }

    @Override
    public void revokeAllTokenForUser(Long userId) {
        refreshTokenRepo.revokeActiveTokensByUserId(userId);
    }
}