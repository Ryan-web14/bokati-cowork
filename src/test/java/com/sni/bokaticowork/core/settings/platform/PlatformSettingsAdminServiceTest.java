package com.sni.bokaticowork.core.settings.platform;

import com.sni.bokaticowork.core.audit.model.SettingsAuditLogs;
import com.sni.bokaticowork.core.audit.repository.SettingsAuditLogsRepository;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les droits, le controle des valeurs et la trace · {@code SUPER_ADMIN} tout, {@code ADMIN} une
 * partie, jamais un secret.
 */
class PlatformSettingsAdminServiceTest {

    private static final String WINDOW = "bokati.marketplace.payment-window-minutes";
    private static final String FEE = "bokati.wallet.transfer.fee-rate";
    private static final String RETRY_CRON = "bokati.billing.dunning.cron";
    private static final String DELAY = "bokati.subscription.workers.renewal-delay-ms";
    private static final String SECRET = "app.security.jwt.secret";

    private final PlatformSettingRepository repository = mock(PlatformSettingRepository.class);
    private final SettingsAuditLogsRepository audit = mock(SettingsAuditLogsRepository.class);
    private final PlatformSettings settings = mock(PlatformSettings.class);
    private final StandardEnvironment environment = new StandardEnvironment();
    private final SensitiveSettingAlerter alerter = mock(SensitiveSettingAlerter.class);
    private final PlatformSettingsAdminService service =
            new PlatformSettingsAdminService(repository, audit, settings, environment, List.of(new PolicyStub()), alerter);

    private final Authentication superAdmin = user("root@sni", "ROLE_SUPER_ADMIN");
    private final Authentication admin = user("admin@sni", "ROLE_ADMIN");

    /** Un module qui garde sa table · les droits et le journal restent ceux des parametres. */
    private static final class PolicyStub implements PlatformSettingProvider {
        private String grace = "1";

        @Override
        public List<ProvidedSetting> settings() {
            return List.of(new ProvidedSetting(new PlatformSettingCatalog.Definition("subscription.policy.grace-period-days",
                    SettingDomain.SUBSCRIPTIONS, PlatformSettingCatalog.Type.INTEGER, PlatformSettingCatalog.Effect.LIVE, "1", "Tolérance"),
                    grace, List.of()));
        }

        @Override
        public void update(String key, String value) {
            grace = value;
        }
    }

    private static Authentication user(String name, String role) {
        return new UsernamePasswordAuthenticationToken(name, "x", AuthorityUtils.createAuthorityList(role));
    }

    @BeforeEach
    void setUp() {
        environment.getPropertySources().addFirst(new MapPropertySource("file", Map.of(
                WINDOW, "30", SECRET, "ne-doit-jamais-sortir", DELAY, "900000")));
        when(repository.findAll()).thenReturn(List.of());
        when(repository.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("L'ADMIN ne voit que ses domaines, le SUPER_ADMIN les 28")
    void domainsFollowTheRole() {
        assertThat(service.domains(superAdmin)).hasSize(28);
        assertThat(service.domains(admin)).hasSize(13).allMatch(view -> view.domain().adminEditable());
        assertThat(service.list(null, admin)).noneMatch(view -> view.key().equals(FEE));
    }

    @Test
    @DisplayName("L'ADMIN modifie un reglage courant, pas un reglage reserve")
    void adminIsLimited() {
        assertThat(service.update(WINDOW, "45", "Fêtes", admin).value()).isEqualTo("45");

        assertThatThrownBy(() -> service.update(FEE, "0.02", null, admin)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.history(FEE, admin)).isInstanceOf(ForbiddenException.class);
        assertThat(service.update(FEE, "0.02", null, superAdmin).value()).isEqualTo("0.02");
    }

    @Test
    @DisplayName("Chaque changement laisse l'ancienne valeur, la nouvelle, l'auteur et le motif")
    void changesAreAudited() {
        service.update(WINDOW, "45", "Fêtes", admin);

        ArgumentCaptor<SettingsAuditLogs> log = ArgumentCaptor.forClass(SettingsAuditLogs.class);
        verify(audit).save(log.capture());
        assertThat(log.getValue().getSettingKey()).isEqualTo(WINDOW);
        assertThat(log.getValue().getOldValue()).isEqualTo("30");
        assertThat(log.getValue().getNewValue()).isEqualTo("45");
        assertThat(log.getValue().getReason()).isEqualTo("admin@sni · Fêtes");
        verify(settings).refresh();
    }

    @Test
    @DisplayName("Une valeur invalide est refusee avant d'atteindre la base")
    void invalidValuesAreRefused() {
        assertThatThrownBy(() -> service.update(WINDOW, "trente", null, superAdmin))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("nombre entier");
        assertThatThrownBy(() -> service.update(RETRY_CRON, "tous les matins", null, superAdmin))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("planification");
        assertThatThrownBy(() -> service.update(DELAY, "10", null, superAdmin))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("1000");
        assertThatThrownBy(() -> service.update("bokati.resource.availability.generation.weekday-hours", "18:00-08:00", null, superAdmin))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("HH:mm");
        assertThatThrownBy(() -> service.update("bokati.support.sender-email", "pas-une-adresse", null, superAdmin))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("e-mail");
        assertThatThrownBy(() -> service.update("bokati.billing.default-currency", "${app.locale}", null, superAdmin))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.update("bokati.inconnu", "1", null, superAdmin))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Un secret ne sort jamais et ne se modifie pas, meme pour le SUPER_ADMIN")
    void secretsStayOut() {
        PlatformSettingsAdminService.SettingView view = service.list(SettingDomain.SECURITY, superAdmin).stream()
                .filter(setting -> setting.key().equals(SECRET)).findFirst().orElseThrow();

        assertThat(view.value()).isNull();
        assertThat(view.defaultValue()).isNull();
        assertThat(view.configured()).isTrue();
        assertThat(view.editable()).isFalse();
        assertThat(view.toString()).doesNotContain("ne-doit-jamais-sortir");
        assertThatThrownBy(() -> service.update(SECRET, "x", null, superAdmin)).isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Un reglage lu au demarrage annonce qu'il attend le redemarrage · pas un reglage immediat")
    void restartIsAnnounced() {
        assertThat(service.update(DELAY, "600000", null, superAdmin).restartPending()).isTrue();
        assertThat(service.update(WINDOW, "45", null, superAdmin).restartPending()).isFalse();
    }

    @Test
    @DisplayName("Remise a zero : la ligne part, le fichier de configuration reprend, et c'est trace")
    void resetReturnsToTheFile() {
        PlatformSetting stored = PlatformSetting.builder().key(WINDOW).value("45").build();
        when(repository.findById(WINDOW)).thenReturn(Optional.of(stored));

        PlatformSettingsAdminService.SettingView view = service.reset(WINDOW, null, admin);

        verify(repository).delete(stored);
        assertThat(view.modified()).isFalse();
        assertThat(view.value()).isEqualTo("30");
        ArgumentCaptor<SettingsAuditLogs> log = ArgumentCaptor.forClass(SettingsAuditLogs.class);
        verify(audit).save(log.capture());
        assertThat(log.getValue().getOldValue()).isEqualTo("45");
        assertThat(log.getValue().getNewValue()).isEqualTo("30");
    }

    @Test
    @DisplayName("Un reglage de module suit les memes droits et le meme journal, et se remet a zero")
    void providedSettingsShareRightsAndJournal() {
        PlatformSettingsAdminService.SettingView view = service.update("subscription.policy.grace-period-days", "3", "Essai", admin);

        assertThat(view.value()).isEqualTo("3");
        assertThat(view.modified()).isTrue();
        assertThat(view.effect()).isEqualTo(PlatformSettingCatalog.Effect.LIVE);
        ArgumentCaptor<SettingsAuditLogs> log = ArgumentCaptor.forClass(SettingsAuditLogs.class);
        verify(audit).save(log.capture());
        assertThat(log.getValue().getOldValue()).isEqualTo("1");
        assertThat(log.getValue().getNewValue()).isEqualTo("3");
        verify(repository, never()).save(any());

        assertThat(service.list(SettingDomain.SUBSCRIPTIONS, admin))
                .anyMatch(setting -> setting.key().equals("subscription.policy.grace-period-days"));
        assertThat(service.reset("subscription.policy.grace-period-days", null, admin).value()).isEqualTo("1");
    }

    @Test
    @DisplayName("La valeur en vigueur vient de la ligne enregistree, le defaut reste celui du fichier")
    void storedValueWins() {
        when(repository.findAll()).thenReturn(List.of(PlatformSetting.builder().key(WINDOW).value("45").updatedBy("admin@sni").build()));

        PlatformSettingsAdminService.SettingView view = service.list(SettingDomain.MARKETPLACE, admin).stream()
                .filter(setting -> setting.key().equals(WINDOW)).findFirst().orElseThrow();

        assertThat(view.value()).isEqualTo("45");
        assertThat(view.defaultValue()).isEqualTo("30");
        assertThat(view.modified()).isTrue();
        assertThat(view.updatedBy()).isEqualTo("admin@sni");
    }

    @Test
    @DisplayName("Un parametre sensible se dit sensible, avec son risque, et son changement alerte")
    void sensitiveSettingsAlert() {
        PlatformSettingsAdminService.SettingView view = service.update(FEE, "0.02", "Promotion", superAdmin);

        assertThat(view.sensitive()).isTrue();
        assertThat(view.risk()).contains("frais");
        verify(alerter).changed(org.mockito.ArgumentMatchers.eq(FEE), any(), any(), org.mockito.ArgumentMatchers.eq("0.02"), org.mockito.ArgumentMatchers.eq("root@sni"));
        assertThat(service.update(WINDOW, "45", null, admin).sensitive()).isFalse();
    }

    @Test
    @DisplayName("La remise a zero d'un parametre sensible alerte aussi")
    void sensitiveResetAlerts() {
        when(repository.findById(FEE)).thenReturn(Optional.of(PlatformSetting.builder().key(FEE).value("0.05").build()));

        service.reset(FEE, null, superAdmin);

        verify(alerter).changed(org.mockito.ArgumentMatchers.eq(FEE), any(), org.mockito.ArgumentMatchers.eq("0.05"), any(),
                org.mockito.ArgumentMatchers.eq("root@sni"));
    }
}
