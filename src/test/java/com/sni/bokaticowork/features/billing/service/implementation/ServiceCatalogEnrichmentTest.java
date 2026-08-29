package com.sni.bokaticowork.features.billing.service.implementation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.CreateServiceCatalogItemRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateServiceCatalogItemRequest;
import com.sni.bokaticowork.features.billing.dto.response.ServiceCatalogItemResponse;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ServiceCatalogEnrichmentTest {

    @Mock
    private ServiceCatalogItemRepository catalogItemRepository;

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ServiceCatalogServiceImpl service;

    @Test
    void shouldStoreTheEnrichedFieldsAndExposeTheDerivedFloor() {
        when(sequenceGenerator.next(anyString())).thenReturn("SRV-001");
        when(catalogItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ServiceCatalogItemResponse created = service.create(new CreateServiceCatalogItemRequest(
                "Salle de reunion", null, "ESPACES", "heure", new BigDecimal("15000"), "XAF", null, 1,
                new BigDecimal("8000"), null, new BigDecimal("20"), new BigDecimal("15"), "block",
                "Salle equipee", List.of("Ecran", "Paperboard", "  "), null,
                "hourly", BigDecimal.ONE, BigDecimal.ONE, new BigDecimal("8"), true,
                "REUNION", List.of("premium"), "EXT-42",
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));

        assertThat(created.effectiveFloorPrice()).isEqualByComparingTo("10000");
        assertThat(created.floorPriceOrigin()).isEqualTo("DERIVED_FROM_MARGIN");
        // Les libelles vides sont ecartes a l'enregistrement.
        assertThat(created.includedItems()).containsExactly("Ecran", "Paperboard");
        assertThat(created.tags()).containsExactly("premium");
        // Les valeurs libres sont normalisees en majuscules.
        assertThat(created.discountPolicy()).isEqualTo("BLOCK");
        assertThat(created.billingMode()).isEqualTo("HOURLY");
    }

    @Test
    void shouldRefuseAFloorAboveTheSellingPrice() {
        when(sequenceGenerator.next(anyString())).thenReturn("SRV-002");

        assertThatThrownBy(() -> service.create(create(new BigDecimal("10000"), new BigDecimal("12000"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invendable");
    }

    @Test
    void shouldClearAFieldOnlyWhenExplicitlyAsked() {
        ServiceCatalogItem existing = ServiceCatalogItem.builder()
                .itemCode("SRV-003")
                .name("Bureau")
                .unitPrice(new BigDecimal("50000"))
                .floorPrice(new BigDecimal("40000"))
                .costPrice(new BigDecimal("30000"))
                .build();
        when(catalogItemRepository.findByItemCode("SRV-003")).thenReturn(Optional.of(existing));
        when(catalogItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Une modification qui ne mentionne pas floorPrice le conserve.
        ServiceCatalogItemResponse untouched = service.update("SRV-003", update(null));
        assertThat(untouched.floorPrice()).isEqualByComparingTo("40000");
        assertThat(untouched.floorPriceOrigin()).isEqualTo("EXPLICIT");

        // Sans clearFields, un plancher pose par erreur ne pourrait jamais etre retire.
        ServiceCatalogItemResponse cleared = service.update("SRV-003", update(List.of("floorPrice")));
        assertThat(cleared.floorPrice()).isNull();
        assertThat(cleared.effectiveFloorPrice()).isEqualByComparingTo("30000");
        assertThat(cleared.floorPriceOrigin()).isEqualTo("COST_PRICE");
    }

    @Test
    void shouldRejectAnUnknownFieldNameRatherThanIgnoreIt() {
        ServiceCatalogItem existing = ServiceCatalogItem.builder()
                .itemCode("SRV-004").name("Bureau").unitPrice(new BigDecimal("50000")).build();
        when(catalogItemRepository.findByItemCode("SRV-004")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update("SRV-004", update(List.of("unitPrice"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("non effacable");
    }

    @Test
    void shouldRejectAnUnknownDiscountPolicy() {
        when(sequenceGenerator.next(anyString())).thenReturn("SRV-005");

        CreateServiceCatalogItemRequest request = new CreateServiceCatalogItemRequest(
                "Bureau", null, null, null, new BigDecimal("1000"), null, null, null,
                null, null, null, null, "STRICT",
                null, null, null, null, null, null, null, null,
                null, null, null, null, null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("NONE, WARN ou BLOCK");
    }

    private CreateServiceCatalogItemRequest create(BigDecimal unitPrice, BigDecimal floorPrice) {
        return new CreateServiceCatalogItemRequest(
                "Bureau", null, null, null, unitPrice, null, null, null,
                null, floorPrice, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    private UpdateServiceCatalogItemRequest update(List<String> clearFields) {
        return new UpdateServiceCatalogItemRequest(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                clearFields);
    }
}
