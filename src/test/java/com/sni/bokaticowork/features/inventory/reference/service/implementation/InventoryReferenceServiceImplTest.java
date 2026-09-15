package com.sni.bokaticowork.features.inventory.reference.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryCategoryRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryUnitRepository;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import com.sni.bokaticowork.features.inventory.reference.dto.InventoryOptionResponse;
import com.sni.bokaticowork.features.inventory.stock.repository.InventoryLocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class InventoryReferenceServiceImplTest {

    @Mock
    private InventoryUnitRepository unitRepository;
    @Mock
    private InventoryCategoryRepository categoryRepository;
    @Mock
    private InventoryLocationRepository locationRepository;
    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private InventoryReferenceServiceImpl service;

    @Test
    void exposesEveryEnumGroupWithAtLeastOneOption() {
        Map<String, List<InventoryOptionResponse>> groups = service.enums();

        assertFalse(groups.isEmpty());
        groups.forEach((name, options) ->
                assertFalse(options.isEmpty(), "Le groupe " + name + " ne doit pas etre vide"));
    }

    @Test
    void translatesTechnicalValuesIntoReadableLabels() {
        List<InventoryOptionResponse> movementTypes = service.enumGroup("movementTypes");

        InventoryOptionResponse in = movementTypes.stream()
                .filter(option -> "IN".equals(option.getValue()))
                .findFirst()
                .orElseThrow();

        assertEquals("Entree", in.getLabel());
        assertTrue(in.isSelectable());
    }

    @Test
    void fallsBackToAReadableLabelWhenNoTranslationIsDeclared() {
        // Toute valeur doit ressortir avec un libelle non vide et different du nom technique brut,
        // pour qu'un ajout d'enum ne produise jamais une option illisible.
        service.enums().forEach((group, options) -> options.forEach(option -> {
            assertFalse(option.getLabel() == null || option.getLabel().isBlank(),
                    "Libelle manquant dans " + group + " pour " + option.getValue());
        }));
    }

    @Test
    void rejectsAnUnknownGroupAndListsTheAvailableOnes() {
        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> service.enumGroup("doesNotExist"));

        assertTrue(exception.getMessage().contains("itemTypes"));
    }

    @Test
    void rejectsANullGroup() {
        assertThrows(ResourceNotFoundException.class, () -> service.enumGroup(null));
    }

    @Test
    void groupNamesMatchTheKeysOfTheEnumMap() {
        assertEquals(service.enums().keySet(), new java.util.LinkedHashSet<>(service.groupNames()));
    }

    @Test
    void liveDataExposesTheFourDropdownSources() {
        Map<String, List<InventoryOptionResponse>> data = service.data();

        assertTrue(data.containsKey("units"));
        assertTrue(data.containsKey("categories"));
        assertTrue(data.containsKey("locations"));
        assertTrue(data.containsKey("suppliers"));
    }
}
