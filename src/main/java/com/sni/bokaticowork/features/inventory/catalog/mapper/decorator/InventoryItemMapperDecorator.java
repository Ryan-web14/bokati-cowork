package com.sni.bokaticowork.features.inventory.catalog.mapper.decorator;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces.InventoryItemMapper;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class InventoryItemMapperDecorator implements InventoryItemMapper {

    @Autowired
    @Qualifier("delegate")
    private InventoryItemMapper delegate;

    @Override
    public InventoryItem toEntity(InventoryItemRequest request) {
        InventoryItem entity = delegate.toEntity(request);
        applyDefaults(entity);
        return entity;
    }

    @Override
    public void updateEntity(InventoryItem entity, InventoryItemRequest request) {
        delegate.updateEntity(entity, request);
        applyDefaults(entity);
    }

    @Override
    public InventoryItemResponse toResponse(InventoryItem entity) {
        InventoryItemResponse response = delegate.toResponse(entity);
        if (entity == null) {
            return response;
        }
        if (entity.getCategory() != null) {
            response.setCategoryCode(entity.getCategory().getCode());
            response.setCategoryName(entity.getCategory().getName());
        }
        if (entity.getUnit() != null) {
            response.setUnitCode(entity.getUnit().getCode());
            response.setUnitName(entity.getUnit().getName());
        }
        return response;
    }

    private void applyDefaults(InventoryItem entity) {
        if (entity.getTaxable() == null) entity.setTaxable(Boolean.FALSE);
        if (entity.getAllowNegativeStock() == null) entity.setAllowNegativeStock(Boolean.FALSE);
        if (entity.getRequiresExpiryDate() == null) entity.setRequiresExpiryDate(Boolean.FALSE);
        if (entity.getRequiresLotNumber() == null) entity.setRequiresLotNumber(Boolean.FALSE);
        if (entity.getRequiresSerialNumber() == null) entity.setRequiresSerialNumber(Boolean.FALSE);
        if (entity.getActive() == null) entity.setActive(Boolean.TRUE);
    }
}
