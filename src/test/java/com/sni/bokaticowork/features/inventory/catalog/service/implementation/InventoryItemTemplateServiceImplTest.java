package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryVariantGenerationRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryVariantGenerationResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryTrackingType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItemTemplate;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryVariantAxis;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryVariantValue;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemTemplateRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryCategoryService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryUnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryItemTemplateServiceImplTest {

    @Mock
    private InventoryItemTemplateRepository templateRepository;
    @Mock
    private InventoryItemRepository itemRepository;
    @Mock
    private InventoryItemService itemService;
    @Mock
    private InventoryCategoryService categoryService;
    @Mock
    private InventoryUnitService unitService;
    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @InjectMocks
    private InventoryItemTemplateServiceImpl service;

    private InventoryItemTemplate template;

    @BeforeEach
    void setUp() {
        template = InventoryItemTemplate.builder()
                .id(1L)
                .templateCode("TPL-00001")
                .name("Tee-shirt")
                .itemType(InventoryItemType.CONSUMABLE)
                .trackingType(InventoryTrackingType.QUANTITY)
                .axes(new ArrayList<>())
                .build();

        template.getAxes().add(axis(template, "TAILLE", "Taille", "M", "L"));
        template.getAxes().add(axis(template, "COULEUR", "Couleur", "ROUGE"));

        when(templateRepository.findByTemplateCode("TPL-00001")).thenReturn(Optional.of(template));
        when(itemRepository.findByTemplateAndVariantSignature(any(), anyString())).thenReturn(Optional.empty());
        when(itemService.createVariant(any(InventoryItemRequest.class), anyString(), anyString()))
                .thenAnswer(call -> {
                    InventoryItemRequest request = call.getArgument(0);
                    return InventoryItemResponse.builder()
                            .itemCode("INV-" + request.getName().hashCode())
                            .name(request.getName())
                            .build();
                });
    }

    @Test
    void producesOneVariantPerCombinationOfAxisValues() {
        InventoryVariantGenerationResponse response = service.generateVariants("TPL-00001", null);

        // Deux tailles fois une couleur font deux variantes.
        assertEquals(2, response.getRequestedCombinations());
        assertEquals(2, response.getCreatedCount());
        assertEquals(0, response.getSkippedCount());
        verify(itemService, times(2)).createVariant(any(), anyString(), anyString());
    }

    @Test
    void buildsASignatureThatIdentifiesTheCombination() {
        InventoryVariantGenerationResponse response = service.generateVariants("TPL-00001", null);

        List<String> signatures = response.getVariants().stream()
                .map(InventoryVariantGenerationResponse.Variant::getVariantSignature)
                .toList();

        assertTrue(signatures.contains("TAILLE=M;COULEUR=ROUGE"));
        assertTrue(signatures.contains("TAILLE=L;COULEUR=ROUGE"));
    }

    @Test
    void namesTheVariantFromTheTemplateAndTheValueLabels() {
        InventoryVariantGenerationResponse response = service.generateVariants("TPL-00001", null);

        List<String> names = response.getVariants().stream()
                .map(InventoryVariantGenerationResponse.Variant::getName)
                .toList();

        assertTrue(names.contains("Tee-shirt M Rouge"));
    }

    @Test
    void leavesExistingVariantsAloneSoTheOperationCanBeReplayed() {
        InventoryItem existing = InventoryItem.builder().itemCode("INV-EXISTING").name("Tee-shirt M Rouge").build();
        when(itemRepository.findByTemplateAndVariantSignature(template, "TAILLE=M;COULEUR=ROUGE"))
                .thenReturn(Optional.of(existing));

        InventoryVariantGenerationResponse response = service.generateVariants("TPL-00001", null);

        assertEquals(1, response.getCreatedCount());
        assertEquals(1, response.getSkippedCount());
        verify(itemService, times(1)).createVariant(any(), anyString(), anyString());
    }

    @Test
    void dryRunReportsTheVolumeWithoutCreatingAnything() {
        InventoryVariantGenerationRequest request = new InventoryVariantGenerationRequest();
        request.setDryRun(true);

        InventoryVariantGenerationResponse response = service.generateVariants("TPL-00001", request);

        assertTrue(response.isDryRun());
        assertEquals(2, response.getRequestedCombinations());
        assertEquals(0, response.getCreatedCount());
        verify(itemService, never()).createVariant(any(), anyString(), anyString());
    }

    @Test
    void narrowsGenerationToTheSelectedValues() {
        InventoryVariantGenerationRequest request = new InventoryVariantGenerationRequest();
        request.setSelectedValues(Map.of("TAILLE", List.of("M")));

        InventoryVariantGenerationResponse response = service.generateVariants("TPL-00001", request);

        assertEquals(1, response.getRequestedCombinations());
        assertEquals("TAILLE=M;COULEUR=ROUGE", response.getVariants().get(0).getVariantSignature());
    }

    @Test
    void ignoresInactiveValues() {
        template.getAxes().get(0).getValues().get(1).setActive(Boolean.FALSE);

        InventoryVariantGenerationResponse response = service.generateVariants("TPL-00001", null);

        assertEquals(1, response.getRequestedCombinations());
        assertFalse(response.getVariants().get(0).getVariantSignature().contains("TAILLE=L"));
    }

    @Test
    void rejectsAnUnknownAxisInTheSelection() {
        InventoryVariantGenerationRequest request = new InventoryVariantGenerationRequest();
        request.setSelectedValues(Map.of("MATIERE", List.of("COTON")));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> service.generateVariants("TPL-00001", request));

        assertTrue(exception.getMessage().contains("MATIERE"));
    }

    @Test
    void rejectsASelectionThatEmptiesAnAxis() {
        InventoryVariantGenerationRequest request = new InventoryVariantGenerationRequest();
        request.setSelectedValues(Map.of("TAILLE", List.of("XXL")));

        assertThrows(BadRequestException.class, () -> service.generateVariants("TPL-00001", request));
    }

    @Test
    void rejectsATemplateWithoutAnyAxis() {
        template.getAxes().clear();

        assertThrows(BadRequestException.class, () -> service.generateVariants("TPL-00001", null));
    }

    private InventoryVariantAxis axis(InventoryItemTemplate owner, String axisCode, String name, String... valueCodes) {
        InventoryVariantAxis axis = InventoryVariantAxis.builder()
                .template(owner)
                .axisCode(axisCode)
                .name(name)
                .position(1)
                .values(new ArrayList<>())
                .build();
        int position = 1;
        for (String valueCode : valueCodes) {
            axis.getValues().add(InventoryVariantValue.builder()
                    .axis(axis)
                    .valueCode(valueCode)
                    .label(valueCode.charAt(0) + valueCode.substring(1).toLowerCase())
                    .position(position++)
                    .active(Boolean.TRUE)
                    .build());
        }
        return axis;
    }
}
