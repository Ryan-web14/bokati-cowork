package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces.InventoryItemMapper;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemPriceHistoryRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRevisionHistoryRepository;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemTemplateRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryCategoryService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryUnitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryItemServiceImplTest {

    @Mock
    private InventoryItemRepository repository;
    @Mock
    private InventoryCategoryService categoryService;
    @Mock
    private InventoryUnitService unitService;
    @Mock
    private InventoryItemMapper mapper;
    @Mock
    private SequenceGeneratorFacade sequenceGenerator;
    @Mock
    private InventoryItemPriceHistoryRepository priceHistoryRepository;
    @Mock
    private InventoryItemRevisionHistoryRepository revisionHistoryRepository;
    @Mock
    private InventoryItemTemplateRepository templateRepository;

    @Test
    void shouldGenerateItemCodeWhenCreatingInventoryItem() {
        InventoryItemServiceImpl service = new InventoryItemServiceImpl(
                repository, categoryService, unitService, mapper, sequenceGenerator,
                priceHistoryRepository, revisionHistoryRepository, templateRepository
        );
        InventoryItemRequest request = new InventoryItemRequest();
        request.setName("Chair");
        request.setPsku("123456789012");
        request.setItemType(InventoryItemType.ASSET);

        // Code format: INV-{typeToken}-{categoryToken}-{YYYYMMDD}-{8digits}
        // No category → GEN, ASSET → AST, sequence "00000001" → rightDigits = "00000001"
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String expectedCode = "INV-AST-GEN-" + today + "-00000001";

        when(mapper.toEntity(request)).thenReturn(new InventoryItem());
        when(sequenceGenerator.next(eq("inventory_item"), any(LocalDate.class))).thenReturn("00000001");
        when(repository.existsByItemCode(anyString())).thenReturn(false);
        when(repository.existsByPsku(anyString())).thenReturn(false);
        when(repository.existsConflictingIdentification(any(), any(), any(), any(), any())).thenReturn(false);
        when(repository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(InventoryItem.class))).thenAnswer(invocation -> {
            InventoryItem item = invocation.getArgument(0);
            return InventoryItemResponse.builder().itemCode(item.getItemCode()).build();
        });

        InventoryItemResponse response = service.create(request);

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(repository).save(itemCaptor.capture());
        verify(sequenceGenerator).next(eq("inventory_item"), any(LocalDate.class));

        assertNotNull(response.getItemCode());
        assertTrue(response.getItemCode().startsWith("INV-AST-GEN-"), "Code should follow INV-{type}-{category}-{date}-{seq} format");
        assertEquals(expectedCode, response.getItemCode());
        assertEquals(expectedCode, itemCaptor.getValue().getItemCode());
        assertNotNull(itemCaptor.getValue().getShortCode());
        assertNotNull(itemCaptor.getValue().getDisplayCode());
    }
}
