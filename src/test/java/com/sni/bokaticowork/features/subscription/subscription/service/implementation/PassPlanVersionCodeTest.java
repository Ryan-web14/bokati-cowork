package com.sni.bokaticowork.features.subscription.subscription.service.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.subscription.repository.PassPlanEntitlementRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanRepository;
import com.sni.bokaticowork.features.subscription.repository.PassPlanVersionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.PassPlanDtos;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassDurationUnit;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlan;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanEntitlement;
import com.sni.bokaticowork.features.subscription.subscription.model.PassPlanVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une version de pass plan se designe par son code · son identifiant, trop long pour JavaScript,
 * revenait arrondi du navigateur et ne designait plus rien.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassPlanVersionCodeTest {

    private static final long ROUNDED_BY_JAVASCRIPT = 7315645027609924123L;

    @Mock private PassPlanRepository plans;
    @Mock private PassPlanVersionRepository versions;
    @Mock private PassPlanEntitlementRepository entitlements;
    @InjectMocks private PassPlanServiceImpl service;

    private PassPlan plan;
    private PassPlanVersion version;

    @BeforeEach
    void setUp() {
        plan = PassPlan.builder().id(ROUNDED_BY_JAVASCRIPT).code("PLP-000012").build();
        version = PassPlanVersion.builder().id(ROUNDED_BY_JAVASCRIPT).plan(plan).versionNumber(2).versionCode("PLP-000012-V2").build();
        when(plans.findByCode("PLP-000012")).thenReturn(Optional.of(plan));
        when(versions.findByVersionCode("PLP-000012-V2")).thenReturn(Optional.of(version));
        when(versions.save(any(PassPlanVersion.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    @DisplayName("Une nouvelle version recoit le code de son plan et son numero")
    void createdVersionHasACode() {
        when(versions.countByPlan(plan)).thenReturn(2);

        PassPlanDtos.PassPlanVersionResponse created = service.createVersion("PLP-000012",
                new PassPlanDtos.CreatePassPlanVersionRequest("Journée", null, 1, PassDurationUnit.values()[0], null, null, null, null, null, null));

        ArgumentCaptor<PassPlanVersion> saved = ArgumentCaptor.forClass(PassPlanVersion.class);
        verify(versions).save(saved.capture());
        assertThat(saved.getValue().getVersionCode()).isEqualTo("PLP-000012-V3");
        assertThat(created.id()).isEqualTo("PLP-000012-V3");
        assertThat(created.versionCode()).isEqualTo("PLP-000012-V3");
    }

    @Test
    @DisplayName("Aucun identifiant technique ne sort · les ecrans gardent id, qui porte le code")
    void noTechnicalIdInTheResponse() {
        JsonNode json = new ObjectMapper().registerModule(new JavaTimeModule()).valueToTree(service.getVersion("PLP-000012-V2"));

        assertThat(json.get("id").asText()).isEqualTo("PLP-000012-V2");
        assertThat(json.toString()).doesNotContain(String.valueOf(ROUNDED_BY_JAVASCRIPT));
    }

    @Test
    @DisplayName("Le code se lit sans tenir compte de la casse · un code inconnu dit son nom")
    void lookup() {
        assertThat(service.getVersion(" plp-000012-v2 ").versionNumber()).isEqualTo(2);
        assertThatThrownBy(() -> service.getVersion("7315645027609924000"))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("Version de plan introuvable");
    }

    @Test
    @DisplayName("Un droit se retire par son code, dans sa version")
    void removeEntitlementByCode() {
        PassPlanEntitlement cafe = PassPlanEntitlement.builder().passVersion(version)
                .entitlementDefinition(EntitlementDefinition.builder().code("CAFE").build()).build();
        PassPlanEntitlement salle = PassPlanEntitlement.builder().passVersion(version)
                .entitlementDefinition(EntitlementDefinition.builder().code("SALLE").build()).build();
        when(entitlements.findAllByPassVersion(version)).thenReturn(List.of(cafe, salle));

        service.removeEntitlement("PLP-000012-V2", "cafe");

        verify(entitlements).deleteAll(List.of(cafe));
        assertThatThrownBy(() -> service.removeEntitlement("PLP-000012-V2", "ABSENT")).isInstanceOf(ResourceNotFoundException.class);
    }
}
