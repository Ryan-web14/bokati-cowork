package com.sni.bokaticowork.features.inventory.catalog.mapper.decorator;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryCategoryRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryCategoryResponse;
import com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces.InventoryCategoryMapper;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class InventoryCategoryMapperDecorator implements InventoryCategoryMapper {

    @Autowired
    @Qualifier("delegate")
    private InventoryCategoryMapper delegate;

    @Override
    public InventoryCategory toEntity(InventoryCategoryRequest request) {
        InventoryCategory entity = delegate.toEntity(request);
        if (entity.getActive() == null) {
            entity.setActive(Boolean.TRUE);
        }
        return entity;
    }

    @Override
    public void updateEntity(InventoryCategory entity, InventoryCategoryRequest request) {
        delegate.updateEntity(entity, request);
        if (entity.getActive() == null) {
            entity.setActive(Boolean.TRUE);
        }
    }

    @Override
    public InventoryCategoryResponse toResponse(InventoryCategory entity) {
        InventoryCategoryResponse response = delegate.toResponse(entity);
        if (entity != null && entity.getParentCategory() != null) {
            response.setParentCategoryCode(entity.getParentCategory().getCode());
            response.setParentCategoryName(entity.getParentCategory().getName());
        }
        return response;
    }
}
