package com.sni.bokaticowork.features.client.member.controller;

import com.sni.bokaticowork.core.maintenance.MemberPurgeService;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberProfileService;
import com.sni.bokaticowork.features.client.member.service.interfaces.MemberService;
import com.sni.bokaticowork.security.admin.provisioning.service.interfaces.UserProvisioningService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * La purge par DELETE /members/{id}?purge=true porte les garde-fous de la route dediee.
 *
 * <p>Regression : le seul CLIENT:DELETE suffisait, sans confirm=true ni role administrateur.</p>
 */
class MemberControllerPurgeTest {

    private final MemberService memberService = mock(MemberService.class);
    private final MemberPurgeService memberPurgeService = mock(MemberPurgeService.class);
    private final MemberController controller = new MemberController(
            memberService, mock(MemberProfileService.class), mock(UserProvisioningService.class), memberPurgeService);

    private static Authentication auth(String... authorities) {
        return new TestingAuthenticationToken("user", null, authorities);
    }

    @Test
    void aClientDeleteRightAloneCannotPurge() {
        Authentication manager = auth("ROLE_MANAGER", "CLIENT:DELETE", "CLIENT_DELETE");

        assertThatThrownBy(() -> controller.archive("MBR-1", true, true, "test", manager))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(memberPurgeService);
    }

    @Test
    void anAdminWithoutSystemSettingsCannotPurge() {
        Authentication admin = auth("ROLE_ADMIN", "CLIENT:DELETE");

        assertThatThrownBy(() -> controller.archive("MBR-1", true, true, "test", admin))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(memberPurgeService);
    }

    @Test
    void anAdminMustStillConfirm() {
        Authentication admin = auth("ROLE_ADMIN", "SYSTEM:SETTINGS", "CLIENT:DELETE");

        ResponseEntity<?> response = controller.archive("MBR-1", true, false, "test", admin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(memberPurgeService, never()).purge(any(), any());
    }

    @Test
    void anAdminWhoConfirmsPurges() {
        Authentication admin = auth("ROLE_ADMIN", "SYSTEM:SETTINGS");

        ResponseEntity<?> response = controller.archive("MBR-1", true, true, "test", admin);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(memberPurgeService).purge("MBR-1", "test");
    }

    @Test
    void archivingStaysOpenToClientDelete() {
        Authentication manager = auth("ROLE_MANAGER", "CLIENT:DELETE");

        ResponseEntity<?> response = controller.archive("MBR-1", false, false, null, manager);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(memberService).delete("MBR-1");
        verifyNoInteractions(memberPurgeService);
    }
}
