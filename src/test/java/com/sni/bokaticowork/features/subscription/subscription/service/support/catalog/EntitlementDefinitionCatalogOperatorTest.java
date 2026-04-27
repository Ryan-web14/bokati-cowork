package com.sni.bokaticowork.features.subscription.subscription.service.support.catalog;

import com.sni.bokaticowork.features.ressource.model.ResourceGroup;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceGroupRepository;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceTypeRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementDefinitionRepository;
import com.sni.bokaticowork.features.subscription.subscription.dto.request.CreateEntitlementDefinitionRequest;
import com.sni.bokaticowork.features.subscription.subscription.enums.ConsumptionMode;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementResetPolicy;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementType;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementUnit;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.service.support.SubscriptionCodeFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementDefinitionCatalogOperatorTest {

    @Mock
    private EntitlementDefinitionRepository entitlementDefinitionRepository;

    @Mock
    private ResourceTypeRepository resourceTypeRepository;

    @Mock
    private ResourceGroupRepository resourceGroupRepository;

    @Mock
    private SubscriptionCodeFactory codeFactory;

    @InjectMocks
    private EntitlementDefinitionCatalogOperator operator;

    @Test
    void shouldUpdateEntitlementDefinitionWithoutChangingCode() {
        EntitlementDefinition existing = EntitlementDefinition.builder()
                .id(10L)
                .code("ENT-ACC-HOU-RES-PER-RTY-202604-00000002")
                .name("Hourly access")
                .description("Initial")
                .entitlementType(EntitlementType.ACCESS)
                .unit(EntitlementUnit.HOUR)
                .consumptionMode(ConsumptionMode.RESERVE_THEN_CONSUME)
                .resetPolicy(EntitlementResetPolicy.PER_BILLING_CYCLE)
                .stackable(true)
                .transferable(false)
                .metadataJson("{\"a\":1}")
                .active(true)
                .build();

        ResourceType resourceType = ResourceType.builder().code("RTY-001").build();

        CreateEntitlementDefinitionRequest request = new CreateEntitlementDefinitionRequest(
                "  Hourly access plus  ",
                "  Updated description  ",
                EntitlementType.ACCESS,
                EntitlementUnit.HOUR,
                ConsumptionMode.RESERVE_THEN_CONSUME,
                EntitlementResetPolicy.NEVER,
                false,
                true,
                "RTY-001",
                null,
                "  {\"b\":2}  ",
                false
        );

        when(entitlementDefinitionRepository.findByCodeIgnoreCase("ENT-ACC-HOU-RES-PER-RTY-202604-00000002"))
                .thenReturn(Optional.of(existing));
        when(resourceTypeRepository.findByCode("RTY-001")).thenReturn(Optional.of(resourceType));
        when(entitlementDefinitionRepository.save(existing)).thenReturn(existing);

        EntitlementDefinition updated = operator.update("ENT-ACC-HOU-RES-PER-RTY-202604-00000002", request);

        ArgumentCaptor<EntitlementDefinition> captor = ArgumentCaptor.forClass(EntitlementDefinition.class);
        verify(entitlementDefinitionRepository).save(captor.capture());

        EntitlementDefinition saved = captor.getValue();
        assertEquals("ENT-ACC-HOU-RES-PER-RTY-202604-00000002", saved.getCode());
        assertEquals("Hourly access plus", saved.getName());
        assertEquals("Updated description", saved.getDescription());
        assertEquals(EntitlementResetPolicy.NEVER, saved.getResetPolicy());
        assertEquals(false, saved.getStackable());
        assertEquals(true, saved.getTransferable());
        assertSame(resourceType, saved.getResourceType());
        assertNull(saved.getResourceGroup());
        assertEquals("{\"b\":2}", saved.getMetadataJson());
        assertEquals(false, saved.getActive());
        assertSame(existing, updated);
    }
}
