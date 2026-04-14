package com.sni.bokaticowork.features.inventory.stock.mapper.decorator;

import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryLocationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryLocationResponse;
import com.sni.bokaticowork.features.inventory.stock.mapper.interfaces.InventoryLocationMapper;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class InventoryLocationMapperDecorator implements InventoryLocationMapper {

    @Autowired
    @Qualifier("delegate")
    private InventoryLocationMapper delegate;

    @Override
    public InventoryLocation toEntity(InventoryLocationRequest request) {
        InventoryLocation entity = delegate.toEntity(request);
        if (entity.getActive() == null) entity.setActive(Boolean.TRUE);
        return entity;
    }

    @Override
    public void updateEntity(InventoryLocation entity, InventoryLocationRequest request) {
        delegate.updateEntity(entity, request);
        if (entity.getActive() == null) entity.setActive(Boolean.TRUE);
    }

    @Override
    public InventoryLocationResponse toResponse(InventoryLocation entity) {
        InventoryLocationResponse response = delegate.toResponse(entity);
        if (entity != null && entity.getParentLocation() != null) {
            response.setParentLocationCode(entity.getParentLocation().getLocationCode());
        }
        return response;
    }
}
