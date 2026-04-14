package com.sni.bokaticowork.security.service.tokenService.implementation;


import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.model.RefreshToken;
import com.sni.bokaticowork.security.repository.RefreshTokenRepository;
import com.sni.bokaticowork.security.service.tokenService.interfaces.RefreshTokenService;
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

    @Override
    public void storeRefreshToken(String token, Users user){
        RefreshToken rt = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiration(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();
        refreshTokenRepo.save(rt);
    }

    @Override
    public boolean isRefreshTokenActive(String token){
        return refreshTokenRepo.findByTokenAndRevokedFalse(token)
                .filter(rt -> rt.getExpiration() != null && rt.getExpiration().isAfter(Instant.now()))
                .isPresent();
    }

    @Override
    public RefreshToken getActiveToken(String token) {
        return refreshTokenRepo.findByTokenAndRevokedFalse(token)
                .filter(rt -> rt.getExpiration() != null && rt.getExpiration().isAfter(Instant.now()))
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token not found or expired"));
    }

    @Override
    public void rotateToken(String oldToken, String newToken){
        RefreshToken oldRt = getActiveToken(oldToken);
        oldRt.setRevoked(true);
        refreshTokenRepo.save(oldRt);

        RefreshToken rt = RefreshToken.builder()
                .user(oldRt.getUser())
                .token(newToken)
                .expiration(Instant.now().plusMillis(refreshTokenExpiration))
                .revoked(false)
                .build();
        refreshTokenRepo.save(rt);
    }

    @Override
    public void revokeToken(String token) {
        refreshTokenRepo.findByTokenAndRevokedFalse(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepo.save(rt);
        });
    }

    @Override
    public void revokeAllTokenForUser(Long userId){
        refreshTokenRepo.revokeActiveTokensByUserId(userId);
    }
}
