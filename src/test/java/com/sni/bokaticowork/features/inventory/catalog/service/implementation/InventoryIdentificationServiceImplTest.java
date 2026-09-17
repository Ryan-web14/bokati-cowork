package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceAlreadyExistException;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryBarcodeRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryBarcodeResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryBarcodeType;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryBarcode;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryBarcodeRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryPackagingRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryItemLookupService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryUnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryIdentificationServiceImplTest {

    @Mock
    private InventoryItemLookupService itemLookupService;
    @Mock
    private InventoryUnitService unitService;
    @Mock
    private InventoryBarcodeRepository barcodeRepository;
    @Mock
    private InventoryPackagingRepository packagingRepository;

    @InjectMocks
    private InventoryIdentificationServiceImpl service;

    private InventoryItem item;

    @BeforeEach
    void setUp() {
        item = InventoryItem.builder().id(1L).itemCode("ART-1").name("Ciment").build();
        when(itemLookupService.findByItemCodeOrThrow(anyString())).thenReturn(item);
        when(barcodeRepository.save(any(InventoryBarcode.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void rejectsAnEan13ThatIsNotThirteenDigits() {
        InventoryBarcodeRequest request = barcodeRequest(InventoryBarcodeType.EAN13, "12345");

        BadRequestException exception =
                assertThrows(BadRequestException.class, () -> service.addBarcode("ART-1", request));

        assertTrue(exception.getMessage().contains("13 digits"));
    }

    @Test
    void acceptsAnEan13OfThirteenDigits() {
        InventoryBarcodeRequest request = barcodeRequest(InventoryBarcodeType.EAN13, "3760001234567");

        InventoryBarcodeResponse response = service.addBarcode("ART-1", request);

        assertEquals("3760001234567", response.getBarcodeValue());
    }

    @Test
    void leavesFreeFormTypesUnchecked() {
        InventoryBarcodeRequest request = barcodeRequest(InventoryBarcodeType.INTERNAL, "cim-50-kg");

        InventoryBarcodeResponse response = service.addBarcode("ART-1", request);

        // Normalise en majuscules, mais aucune contrainte de longueur.
        assertEquals("CIM-50-KG", response.getBarcodeValue());
    }

    @Test
    void refusesAValueAlreadyAssignedToAnotherItem() {
        when(barcodeRepository.existsByBarcodeValue("3760001234567")).thenReturn(true);

        InventoryBarcodeRequest request = barcodeRequest(InventoryBarcodeType.EAN13, "3760001234567");

        assertThrows(ResourceAlreadyExistException.class, () -> service.addBarcode("ART-1", request));
    }

    @Test
    void makesTheFirstBarcodeOfAnItemPrimaryWithoutBeingAsked() {
        when(barcodeRepository.findAllByItemOrderByPrimaryCodeDescBarcodeValueAsc(item)).thenReturn(List.of());

        InventoryBarcodeResponse response = service.addBarcode("ART-1", barcodeRequest(InventoryBarcodeType.INTERNAL, "A1"));

        assertTrue(response.getPrimaryCode());
    }

    @Test
    void doesNotPromoteASecondBarcodeUnlessAsked() {
        InventoryBarcode existing = InventoryBarcode.builder().item(item).barcodeValue("A1").primaryCode(true).build();
        when(barcodeRepository.findAllByItemOrderByPrimaryCodeDescBarcodeValueAsc(item)).thenReturn(List.of(existing));

        InventoryBarcodeResponse response = service.addBarcode("ART-1", barcodeRequest(InventoryBarcodeType.INTERNAL, "A2"));

        assertEquals(Boolean.FALSE, response.getPrimaryCode());
        verify(barcodeRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void clearsThePreviousPrimaryBeforePromotingANewOne() {
        // L index unique partiel en base interdit deux codes principaux : sans ce nettoyage,
        // la promotion echouerait au flush.
        InventoryBarcode existing = InventoryBarcode.builder().item(item).barcodeValue("A1").primaryCode(true).build();
        when(barcodeRepository.findAllByItemOrderByPrimaryCodeDescBarcodeValueAsc(item)).thenReturn(List.of(existing));
        when(barcodeRepository.findAllByItemAndPrimaryCodeTrue(item)).thenReturn(List.of(existing));

        InventoryBarcodeRequest request = barcodeRequest(InventoryBarcodeType.INTERNAL, "A2");
        request.setPrimaryCode(true);

        InventoryBarcodeResponse response = service.addBarcode("ART-1", request);

        assertTrue(response.getPrimaryCode());
        assertEquals(Boolean.FALSE, existing.getPrimaryCode());
        verify(barcodeRepository).saveAllAndFlush(List.of(existing));
    }

    @Test
    void defaultsScanQuantityToOne() {
        when(barcodeRepository.findAllByItemOrderByPrimaryCodeDescBarcodeValueAsc(item)).thenReturn(List.of());

        InventoryBarcodeResponse response = service.addBarcode("ART-1", barcodeRequest(InventoryBarcodeType.INTERNAL, "A1"));

        assertEquals(BigDecimal.ONE, response.getQuantity());
    }

    private InventoryBarcodeRequest barcodeRequest(InventoryBarcodeType type, String value) {
        InventoryBarcodeRequest request = new InventoryBarcodeRequest();
        request.setBarcodeType(type);
        request.setBarcodeValue(value);
        return request;
    }
}
