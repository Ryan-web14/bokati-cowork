package com.sni.bokaticowork.features.inventory.catalog.service.implementation;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.enums.InventoryItemType;
import com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces.InventoryItemMapper;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryCategoryService;
import com.sni.bokaticowork.features.inventory.catalog.service.interfaces.InventoryUnitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void shouldGenerateItemCodeWhenCreatingInventoryItem() {
        InventoryItemServiceImpl service = new InventoryItemServiceImpl(
                repository, categoryService, unitService, mapper, sequenceGenerator
        );
        InventoryItemRequest request = new InventoryItemRequest();
        request.setName("Chair");
        request.setPsku("123456789012");
        request.setItemType(InventoryItemType.ASSET);

        when(mapper.toEntity(request)).thenReturn(new InventoryItem());
        when(sequenceGenerator.next("inventory_item")).thenReturn("ITM-00000001");
        when(repository.existsByItemCode("ITM-00000001")).thenReturn(false);
        when(repository.existsByPsku("123456789012")).thenReturn(false);
        when(repository.existsConflictingIdentification(any(), any(), any(), any(), any())).thenReturn(false);
        when(repository.save(any(InventoryItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(InventoryItem.class))).thenAnswer(invocation -> {
            InventoryItem item = invocation.getArgument(0);
            return InventoryItemResponse.builder().itemCode(item.getItemCode()).build();
        });

        InventoryItemResponse response = service.create(request);

        ArgumentCaptor<InventoryItem> itemCaptor = ArgumentCaptor.forClass(InventoryItem.class);
        verify(repository).save(itemCaptor.capture());
        verify(sequenceGenerator).next("inventory_item");
        assertEquals("ITM-00000001", response.getItemCode());
        assertEquals("ITM-00000001", itemCaptor.getValue().getItemCode());
        assertEquals("ITM-00000001", itemCaptor.getValue().getShortCode());
        assertEquals("ITM-00000001", itemCaptor.getValue().getDisplayCode());
    }
}
