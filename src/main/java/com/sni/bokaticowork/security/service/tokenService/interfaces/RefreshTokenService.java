package com.sni.bokaticowork.security.service.tokenService.interfaces;


import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.model.RefreshToken;

public interface RefreshTokenService {

    void storeRefreshToken(String token, Users user);
    boolean isRefreshTokenActive(String token);
    RefreshToken getActiveToken(String token);
    void rotateToken(String oldToken, String newToken);
    void revokeToken(String token);
    void revokeAllTokenForUser(Long userId);
}
