package com.sni.bokaticowork.features.inventory.stock.mapper.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryLocationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryLocationResponse;
import com.sni.bokaticowork.features.inventory.stock.mapper.decorator.InventoryLocationMapperDecorator;
import com.sni.bokaticowork.features.inventory.stock.model.InventoryLocation;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(InventoryLocationMapperDecorator.class)
public interface InventoryLocationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "locationCode", ignore = true)
    @Mapping(target = "parentLocation", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventoryLocation toEntity(InventoryLocationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "locationCode", ignore = true)
    @Mapping(target = "parentLocation", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget InventoryLocation entity, InventoryLocationRequest request);

    InventoryLocationResponse toResponse(InventoryLocation entity);
}
