package com.sni.bokaticowork.features.subscription.lifecycle.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingCatalog;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingProvider;
import com.sni.bokaticowork.core.settings.platform.SettingDomain;
import com.sni.bokaticowork.features.subscription.lifecycle.model.ProrationPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionPolicy;
import com.sni.bokaticowork.features.subscription.lifecycle.repository.SubscriptionPolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La politique d'abonnement se lit et s'ecrit depuis les parametres, sous ses propres regles.
 */
class SubscriptionPolicySettingsTest {

    private final SubscriptionPolicyRepository repository = mock(SubscriptionPolicyRepository.class);
    private final SubscriptionPolicySettings settings = new SubscriptionPolicySettings(new SubscriptionPolicyService(repository));
    private SubscriptionPolicy policy;

    @BeforeEach
    void setUp() {
        policy = SubscriptionPolicy.builder().policyCode("DEFAULT").name("Politique").gracePeriodDays(2).build();
        when(repository.findFirstByActiveTrueOrderByIdAsc()).thenReturn(Optional.of(policy));
        when(repository.save(any(SubscriptionPolicy.class))).thenAnswer(call -> call.getArgument(0));
    }

    private PlatformSettingProvider.ProvidedSetting setting(String key) {
        return settings.settings().stream().filter(item -> item.definition().key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Les 13 reglages sortent dans le domaine Abonnements, immediats, avec le defaut du modele")
    void listed() {
        assertThat(settings.settings()).hasSize(13).allSatisfy(item -> {
            assertThat(item.definition().domain()).isEqualTo(SettingDomain.SUBSCRIPTIONS);
            assertThat(item.definition().effect()).isEqualTo(PlatformSettingCatalog.Effect.LIVE);
        });
        assertThat(setting("subscription.policy.grace-period-days").value()).isEqualTo("2");
        assertThat(setting("subscription.policy.grace-period-days").definition().defaultValue()).isEqualTo("1");
        assertThat(setting("subscription.policy.proration-policy").options()).contains("DAILY", "NONE");
    }

    @Test
    @DisplayName("Un reglage modifie ne touche que lui")
    void updatesOneField() {
        settings.update("subscription.policy.suspension-after-grace-days", "5");
        settings.update("subscription.policy.proration-policy", "NONE");

        assertThat(policy.getSuspensionAfterGraceDays()).isEqualTo(5);
        assertThat(policy.getProrationPolicy()).isEqualTo(ProrationPolicy.NONE);
        assertThat(policy.getGracePeriodDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("Les regles de la politique restent les siennes")
    void policyRulesApply() {
        assertThatThrownBy(() -> settings.update("subscription.policy.grace-period-days", "deux"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("nombre entier");
        assertThatThrownBy(() -> settings.update("subscription.policy.grace-period-days", "-1"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> settings.update("subscription.policy.early-termination-percent", "120"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("100");
        assertThatThrownBy(() -> settings.update("subscription.policy.proration-policy", "AU_HASARD"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("DAILY");
        assertThatThrownBy(() -> settings.update("subscription.policy.quote-validity-days", "0"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("Vider la penalite fixe la retire · impossible tant que la formule l'exige")
    void fixedFeeCanBeCleared() {
        settings.update("subscription.policy.early-termination-fixed-fee", "25000");
        assertThat(setting("subscription.policy.early-termination-fixed-fee").value()).isEqualTo("25000");

        settings.update("subscription.policy.early-termination-fixed-fee", "");
        assertThat(policy.getEarlyTerminationFixedFee()).isNull();

        assertThatThrownBy(() -> settings.update("subscription.policy.early-termination-formula", "FIXED_FEE"))
                .isInstanceOf(BadRequestException.class);
    }
}
