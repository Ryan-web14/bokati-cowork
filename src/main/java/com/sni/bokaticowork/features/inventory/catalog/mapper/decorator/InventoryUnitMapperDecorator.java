package com.sni.bokaticowork.features.inventory.catalog.mapper.decorator;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryUnitRequest;
import com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces.InventoryUnitMapper;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class InventoryUnitMapperDecorator implements InventoryUnitMapper {

    @Autowired
    @Qualifier("delegate")
    private InventoryUnitMapper delegate;

    @Override
    public InventoryUnit toEntity(InventoryUnitRequest request) {
        InventoryUnit entity = delegate.toEntity(request);
        if (entity.getActive() == null) {
            entity.setActive(Boolean.TRUE);
        }
        return entity;
    }

    @Override
    public void updateEntity(InventoryUnit entity, InventoryUnitRequest request) {
        delegate.updateEntity(entity, request);
        if (entity.getActive() == null) {
            entity.setActive(Boolean.TRUE);
        }
    }
}
