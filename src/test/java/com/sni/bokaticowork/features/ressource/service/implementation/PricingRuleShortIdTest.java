package com.sni.bokaticowork.features.ressource.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.ressource.enums.ResourceBookingUnit;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourcePricingRule;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourcePricingRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * L'API expose le numero court d'une regle, jamais l'identifiant genere que JavaScript arrondit.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PricingRuleShortIdTest {

    private static final long GENERATED_ID = 2_026_102_612_345_678_901L;

    @Mock private ResourcePricingRuleRepository repository;
    @InjectMocks private ResourcePricingRuleServiceImpl service;

    private ResourcePricingRule rule() {
        ResourcePricingRule rule = new ResourcePricingRule();
        rule.setId(GENERATED_ID);
        rule.setShortId(42L);
        rule.setResource(Resource.builder().code("RES-1").build());
        rule.setResourceBookingUnit(ResourceBookingUnit.HOUR);
        return rule;
    }

    @Test
    @DisplayName("Le champ id de la reponse porte le numero court")
    void responseCarriesTheShortId() {
        when(repository.findByShortId(42L)).thenReturn(Optional.of(rule()));

        assertThat(service.getPricingRule(42L).getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Le numero court tient dans un nombre JavaScript, l'identifiant genere non")
    void shortIdIsSafeForJavaScript() {
        long maxSafeInteger = (1L << 53) - 1;

        assertThat(GENERATED_ID).isGreaterThan(maxSafeInteger);
        assertThat(rule().getShortId()).isLessThanOrEqualTo(maxSafeInteger);
    }

    @Test
    @DisplayName("L'identifiant genere reste accepte pour les clients qui l'avaient garde")
    void generatedIdIsStillAccepted() {
        when(repository.findByShortId(GENERATED_ID)).thenReturn(Optional.empty());
        when(repository.findById(GENERATED_ID)).thenReturn(Optional.of(rule()));

        assertThat(service.getPricingRule(GENERATED_ID).getId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("Un numero inconnu repond introuvable")
    void unknownNumberIsNotFound() {
        when(repository.findByShortId(anyLong())).thenReturn(Optional.empty());
        when(repository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPricingRule(7L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
