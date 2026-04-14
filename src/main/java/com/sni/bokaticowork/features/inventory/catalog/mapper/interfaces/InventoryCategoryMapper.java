package com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryCategoryRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryCategoryResponse;
import com.sni.bokaticowork.features.inventory.catalog.mapper.decorator.InventoryCategoryMapperDecorator;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryCategory;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(InventoryCategoryMapperDecorator.class)
public interface InventoryCategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventoryCategory toEntity(InventoryCategoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "parentCategory", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget InventoryCategory entity, InventoryCategoryRequest request);

    InventoryCategoryResponse toResponse(InventoryCategory entity);
}
