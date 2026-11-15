package com.sni.bokaticowork.core.audit.security;

import com.sni.bokaticowork.core.exception.customs.AccountLockedException;
import com.sni.bokaticowork.core.exception.customs.BadCredentialException;
import com.sni.bokaticowork.security.admin.user.mapper.interfaces.UserMapper;
import com.sni.bokaticowork.security.admin.user.model.UserPrincipal;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.admin.user.service.implementation.UserServiceImpl;
import com.sni.bokaticowork.security.authentication.service.implementation.AuthenticationServiceImpl;
import com.sni.bokaticowork.core.audit.service.interfaces.UserSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lot A2 : le compte se verrouille apres trop d'echecs, pour une duree, et le verrou echu ne bloque
 * plus ; l'echec de connexion ne defait plus son propre compte ; les refus en rafale ne sont ecrits
 * qu'une fois par minute.
 */
class SecurityA2Test {

    private final UserRepository users = mock(UserRepository.class);
    private final SecurityEvents events = mock(SecurityEvents.class);
    private final UserServiceImpl service = new UserServiceImpl(users, mock(UserMapper.class), mock(PasswordEncoder.class), mock(UserSessionService.class));

    SecurityA2Test() {
        ReflectionTestUtils.setField(service, "maxFailedLoginAttempts", 3);
        ReflectionTestUtils.invokeMethod(service, "setSecurityEvents", events);
    }

    @Test
    void theAccountLocksForThirtyMinutesAfterTooManyFailures() {
        Users user = Users.builder().id(7L).userId("USR-7").email("awa@example.com").failedLoginAttempts(0).isAccountLocked(false).build();
        when(users.findByEmailIgnoreCase("awa@example.com")).thenReturn(Optional.of(user));

        service.recordLoginFailure("awa@example.com");
        service.recordLoginFailure("awa@example.com");
        assertThat(user.lockActive()).isFalse();
        service.recordLoginFailure("awa@example.com");

        assertThat(user.lockActive()).isTrue();
        assertThat(user.getLockedUntil()).isBetween(Instant.now().plusSeconds(29 * 60), Instant.now().plusSeconds(31 * 60));
        assertThat(new UserPrincipal(user).isAccountNonLocked()).isFalse();
        verify(events).record(eq(SecurityEventType.ACCOUNT_LOCKED), eq(7L), eq("USR-7"), eq("awa@example.com"), any(Object[].class));
        verify(users, times(3)).save(user);
    }

    @Test
    void anExpiredLockNoLongerBlocksAndCountingStartsAgain() {
        Users user = Users.builder().id(7L).userId("USR-7").email("awa@example.com").failedLoginAttempts(3).isAccountLocked(true)
                .lockedUntil(Instant.now().minusSeconds(60)).build();
        when(users.findByEmailIgnoreCase("awa@example.com")).thenReturn(Optional.of(user));
        assertThat(new UserPrincipal(user).isAccountNonLocked()).isTrue();

        service.recordLoginFailure("awa@example.com");
        assertThat(user.getIsAccountLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);

        service.recordLoginSuccess("awa@example.com");
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void aLoginFailureKeepsItsCount() throws Exception {
        Transactional transactional = AuthenticationServiceImpl.class
                .getMethod("login", com.sni.bokaticowork.security.authentication.dto.request.LoginRequest.class,
                        jakarta.servlet.http.HttpServletRequest.class)
                .getAnnotation(Transactional.class);
        assertThat(List.of(transactional.noRollbackFor())).contains(BadCredentialException.class, AccountLockedException.class);
    }

    @Test
    void burstsAreWrittenOncePerMinute() {
        SecurityEvents real = mock(SecurityEvents.class, org.mockito.Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(real, "throttle", new java.util.concurrent.ConcurrentHashMap<String, Instant>());
        org.mockito.Mockito.doNothing().when(real).record(any(), any(), any(), any(), any(jakarta.servlet.http.HttpServletRequest.class), any(Object[].class));
        for (int i = 0; i < 5; i++) {
            real.recordThrottled("429|1.2.3.4|login", SecurityEventType.RATE_LIMITED, null, null, null, null);
        }
        real.recordThrottled("429|5.6.7.8|login", SecurityEventType.RATE_LIMITED, null, null, null, null);
        ArgumentCaptor<SecurityEventType> types = ArgumentCaptor.forClass(SecurityEventType.class);
        verify(real, times(2)).record(types.capture(), any(), any(), any(), any(), any(Object[].class));
    }
}
