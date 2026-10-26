package com.sni.bokaticowork.features.portal.session;

import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.portal.session.service.ClientSessionService;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.model.RefreshToken;
import com.sni.bokaticowork.security.repository.RefreshTokenRepository;
import com.sni.bokaticowork.security.service.tokenService.interfaces.RefreshTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * « Deconnecter cet appareil » deconnecte vraiment.
 *
 * <p>Le hash stocke etait passe a {@code revokeToken}, qui le hachait une seconde fois et ne
 * trouvait jamais rien · l'appareil restait connecte.</p>
 */
@ExtendWith(MockitoExtension.class)
class ClientSessionServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserSessionService sessionService;
    @InjectMocks private ClientSessionService service;

    private static Member member(Users user) {
        Member member = new Member();
        member.setUser(user);
        return member;
    }

    private static Users user() {
        Users user = new Users();
        user.setId(42L);
        return user;
    }

    @Test
    @DisplayName("Revoquer un appareil revoque son jeton et ferme sa session")
    void revokingOneDeviceClosesIt() {
        Users user = user();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken("hash-du-jeton");
        when(refreshTokenRepository.findById(7L)).thenReturn(Optional.of(token));

        service.revokeSession(member(user), 7L);

        verify(refreshTokenService).revoke(token);
        verify(sessionService).invalidateSessionByRefreshTokenHash("hash-du-jeton");
    }

    @Test
    @DisplayName("Tout revoquer ferme aussi les sessions, donc les jetons d'acces deja emis")
    void revokingAllClosesSessionsToo() {
        Users user = user();

        service.revokeAllSessions(member(user));

        verify(sessionService).revokeAllForUser(user);
    }
}
