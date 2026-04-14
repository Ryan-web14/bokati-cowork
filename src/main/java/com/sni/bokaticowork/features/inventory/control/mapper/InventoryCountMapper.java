package com.sni.bokaticowork.features.inventory.control.mapper;

import com.sni.bokaticowork.features.inventory.control.dto.response.InventoryCountItemResponse;
import com.sni.bokaticowork.features.inventory.control.dto.response.InventoryCountResponse;
import com.sni.bokaticowork.features.inventory.control.model.InventoryCount;
import com.sni.bokaticowork.features.inventory.control.model.InventoryCountItem;
import org.springframework.stereotype.Component;

@Component
public class InventoryCountMapper {

    public InventoryCountResponse toResponse(InventoryCount count) {
        if (count == null) return null;
        return InventoryCountResponse.builder()
                .countCode(count.getCountCode())
                .locationCode(count.getLocation().getLocationCode())
                .locationName(count.getLocation().getName())
                .status(count.getStatus())
                .createdBy(count.getCreatedBy())
                .notes(count.getNotes())
                .startedAt(count.getStartedAt())
                .reviewedAt(count.getReviewedAt())
                .validatedAt(count.getValidatedAt())
                .createdAt(count.getCreatedAt())
                .items(count.getItems() == null ? java.util.List.of() : count.getItems().stream().map(this::toItemResponse).toList())
                .build();
    }

    public InventoryCountItemResponse toItemResponse(InventoryCountItem item) {
        if (item == null) return null;
        return InventoryCountItemResponse.builder()
                .itemCode(item.getItem().getItemCode())
                .itemName(item.getItem().getName())
                .expectedQuantity(item.getExpectedQuantity())
                .countedQuantity(item.getCountedQuantity())
                .varianceQuantity(item.getVarianceQuantity())
                .notes(item.getNotes())
                .build();
    }
}
