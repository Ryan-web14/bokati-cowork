package com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryItemRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryItemResponse;
import com.sni.bokaticowork.features.inventory.catalog.mapper.decorator.InventoryItemMapperDecorator;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(InventoryItemMapperDecorator.class)
public interface InventoryItemMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "itemCode", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "searchText", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventoryItem toEntity(InventoryItemRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "itemCode", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "searchText", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget InventoryItem entity, InventoryItemRequest request);

    InventoryItemResponse toResponse(InventoryItem entity);
}
