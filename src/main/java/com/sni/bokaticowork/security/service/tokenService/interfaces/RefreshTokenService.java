package com.sni.bokaticowork.security.service.tokenService.interfaces;


import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.model.RefreshToken;

public interface RefreshTokenService {

    void storeRefreshToken(String token, Users user);
    boolean isRefreshTokenActive(String token);
    RefreshToken getActiveToken(String token);

    /** Le jeton tel qu'il est enregistre, revoque ou non · c'est ce qui permet de voir une reutilisation. */
    java.util.Optional<RefreshToken> findByToken(String token);

    /** Revoque un jeton deja charge, sans repasser par son hash. */
    void revoke(RefreshToken token);
    void rotateToken(String oldToken, String newToken);
    void revokeToken(String token);
    void revokeAllTokenForUser(Long userId);
}
