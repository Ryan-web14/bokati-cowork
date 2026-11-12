package com.sni.bokaticowork.core.settings.platform;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.features.portal.notification.service.AdminInAppNotifier;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Les super-administrateurs apprennent tout changement d'un parametre sensible, et seulement ceux-la. */
class SensitiveSettingAlerterTest {

    private static final String FEE = "bokati.wallet.transfer.fee-rate";

    private final RoleUserRepository roles = mock(RoleUserRepository.class);
    private final DefaultEmailSender email = mock(DefaultEmailSender.class);
    private final AdminInAppNotifier inApp = mock(AdminInAppNotifier.class);
    private final SensitiveSettingAlerter alerter = new SensitiveSettingAlerter(roles, email, inApp);

    @Test
    @DisplayName("Un changement sensible part par courriel a chaque super-administrateur et dans l'application")
    void sensitiveChangeIsSent() {
        when(roles.findEnabledHolderEmails("SUPER_ADMIN")).thenReturn(List.of("a@sni", "b@sni"));

        alerter.changed(FEE, "Taux des frais", "0.01", "0.5", "root@sni");

        verify(inApp).broadcastAlert(eq(SensitiveSettingAlerter.ALERT_TYPE), anyString(), contains("Taux des frais"),
                contains("0.5"), eq("WARNING"), any());
        verify(email).queueEmail(eq("a@sni"), anyString(), contains("frais"), eq(true), eq("PLATFORM_SETTING"), eq(FEE));
        verify(email).queueEmail(eq("b@sni"), anyString(), anyString(), anyBoolean(), anyString(), eq(FEE));
    }

    @Test
    @DisplayName("Un parametre ordinaire ou une valeur inchangee ne previent personne")
    void ordinaryChangeIsSilent() {
        alerter.changed("bokati.marketplace.payment-window-minutes", "Fenetre", "30", "45", "root@sni");
        alerter.changed(FEE, "Taux des frais", "0.01", "0.01", "root@sni");

        verifyNoInteractions(inApp, email, roles);
    }

    @Test
    @DisplayName("Les valeurs sont echappees dans le courriel")
    void valuesAreEscaped() {
        when(roles.findEnabledHolderEmails("SUPER_ADMIN")).thenReturn(List.of("a@sni"));

        alerter.changed(FEE, "Taux", "<b>", "x", "root@sni");

        verify(email).queueEmail(eq("a@sni"), anyString(), contains("&lt;b&gt;"), eq(true), anyString(), anyString());
        verify(email, never()).queueEmail(anyString(), anyString(), contains("<b>Ancienne valeur</b> : <b>"), anyBoolean(), anyString(), anyString());
    }
}
