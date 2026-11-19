package com.sni.bokaticowork.security.superadmin;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.security.admin.role.service.interfaces.RoleUserService;
import com.sni.bokaticowork.security.admin.user.dto.request.UserRequest;
import com.sni.bokaticowork.security.admin.user.model.Users;
import com.sni.bokaticowork.security.admin.user.repository.UserRepository;
import com.sni.bokaticowork.security.admin.user.service.interfaces.UserService;
import com.sni.bokaticowork.security.service.passwordResetService.support.PasswordPolicyValidator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SuperAdminInvitationServiceTest {

    private final SuperAdminInvitationRepository invitations = mock(SuperAdminInvitationRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final UserService userService = mock(UserService.class);
    private final RoleUserService roles = mock(RoleUserService.class);
    private final PasswordPolicyValidator policy = mock(PasswordPolicyValidator.class);
    private final SuperAdminInvitationService service = new SuperAdminInvitationService(invitations, users, userService, roles, policy);

    private static Users user(String email) {
        Users user = new Users();
        user.setEmail(email);
        return user;
    }

    @Test
    void onlyTheInvitedAccountGetsALinkAndOnlyTheHashIsKept() {
        SuperAdminInvitation invitation = SuperAdminInvitation.builder().targetEmail("admin@sni-cg.com").build();
        when(invitations.findOpenFor("admin@sni-cg.com")).thenReturn(List.of(invitation));
        when(invitations.findOpenFor("autre@sni-cg.com")).thenReturn(List.of());

        assertThat(service.issueOnLogin(user("autre@sni-cg.com"))).isEmpty();
        Optional<String> url = service.issueOnLogin(user("admin@sni-cg.com"));

        assertThat(url).get().asString().contains("/sni/api/v1/auth/super-admin-setup/form?token=");
        String raw = url.get().substring(url.get().indexOf("token=") + 6);
        assertThat(invitation.getStatus()).isEqualTo(SuperAdminInvitation.ISSUED);
        assertThat(invitation.getTokenHash()).isEqualTo(SuperAdminInvitationService.hash(raw)).isNotEqualTo(raw);
        assertThat(invitation.getExpiresAt()).isAfter(Instant.now().plusSeconds(29 * 60));
    }

    @Test
    void theLinkCreatesOneSuperAdminThenClosesForever() {
        SuperAdminInvitation invitation = SuperAdminInvitation.builder().targetEmail("admin@sni-cg.com").status(SuperAdminInvitation.ISSUED)
                .tokenHash(SuperAdminInvitationService.hash("jeton")).expiresAt(Instant.now().plusSeconds(600)).build();
        when(invitations.lockByTokenHash(SuperAdminInvitationService.hash("jeton"))).thenReturn(Optional.of(invitation));
        when(policy.validate(anyString())).thenReturn(Optional.empty());
        Users created = user("ryan@elleaose.com");
        created.setId(42L);
        when(userService.createUser(any())).thenReturn(created);

        service.complete("jeton", "Ryan", "Bikindou", " Ryan@ElleAOse.com ", "Secret123", "1.2.3.4");

        ArgumentCaptor<UserRequest> request = ArgumentCaptor.forClass(UserRequest.class);
        verify(userService).createUser(request.capture());
        assertThat(request.getValue().getEmail()).isEqualTo("ryan@elleaose.com");
        verify(roles).addRoleToUser(42L, "SUPER_ADMIN", "INVITATION:admin@sni-cg.com");
        verify(userService).activateUser("ryan@elleaose.com");
        assertThat(invitation.getStatus()).isEqualTo(SuperAdminInvitation.USED);
        assertThat(invitation.getTokenHash()).isNull();
        assertThat(SuperAdminInvitationService.usable(invitation)).isFalse();
    }

    @Test
    void anExpiredOrUnknownLinkOrAnExistingEmailIsRefused() {
        SuperAdminInvitation expired = SuperAdminInvitation.builder().targetEmail("admin@sni-cg.com").status(SuperAdminInvitation.ISSUED)
                .expiresAt(Instant.now().minusSeconds(1)).build();
        when(invitations.lockByTokenHash(SuperAdminInvitationService.hash("vieux"))).thenReturn(Optional.of(expired));
        when(invitations.lockByTokenHash(SuperAdminInvitationService.hash("inconnu"))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.complete("vieux", "A", "B", "a@b.cg", "Secret123", null)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.complete("inconnu", "A", "B", "a@b.cg", "Secret123", null)).isInstanceOf(ForbiddenException.class);

        SuperAdminInvitation valid = SuperAdminInvitation.builder().targetEmail("admin@sni-cg.com").status(SuperAdminInvitation.ISSUED)
                .expiresAt(Instant.now().plusSeconds(600)).build();
        when(invitations.lockByTokenHash(SuperAdminInvitationService.hash("bon"))).thenReturn(Optional.of(valid));
        when(users.existsByEmailIgnoreCase("admin@sni-cg.com")).thenReturn(true);
        assertThatThrownBy(() -> service.complete("bon", "A", "B", "admin@sni-cg.com", "Secret123", null)).isInstanceOf(BadRequestException.class);
        verify(userService, never()).createUser(any());
    }
}
