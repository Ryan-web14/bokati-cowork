package com.sni.bokaticowork.security.admin.support;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Chaque super-administrateur actif est prevenu, et le contenu ne peut pas injecter de HTML.
 */
@ExtendWith(MockitoExtension.class)
class PrivilegeChangeNotifierTest {

    @Mock private RoleUserRepository roleUserRepository;
    @Mock private DefaultEmailSender emailSender;
    @InjectMocks private PrivilegeChangeNotifier notifier;

    @Test
    @DisplayName("Tous les super-administrateurs actifs recoivent l'alerte")
    void everySuperAdminIsNotified() throws Exception {
        when(roleUserRepository.findEnabledHolderEmails("SUPER_ADMIN")).thenReturn(List.of("a@x.com", "b@x.com"));

        notifier.notify("admin@x.com", "Role attribue", "cible@x.com", "SUPER_ADMIN");

        verify(emailSender).sendHtmlEmail(eq("a@x.com"), contains("Role attribue"), anyString());
        verify(emailSender).sendHtmlEmail(eq("b@x.com"), contains("Role attribue"), anyString());
    }

    @Test
    @DisplayName("Sans super-administrateur actif, rien n'est envoye")
    void nobodyToNotify() throws Exception {
        when(roleUserRepository.findEnabledHolderEmails("SUPER_ADMIN")).thenReturn(List.of());

        notifier.notify("admin@x.com", "Role attribue", "cible@x.com", "STAFF");

        verify(emailSender, never()).sendHtmlEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Les valeurs sont echappees · un nom de role ne devient pas du HTML")
    void valuesAreEscaped() {
        String html = PrivilegeChangeNotifier.html("a@x.com", "Role cree", "<script>alert(1)</script>", "d",
                LocalDateTime.of(2026, 10, 26, 9, 30));

        assertThat(html).doesNotContain("<script>").contains("&lt;script&gt;").contains("26/10/2026 09:30");
    }
}
