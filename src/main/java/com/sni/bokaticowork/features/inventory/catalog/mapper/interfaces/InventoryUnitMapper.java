package com.sni.bokaticowork.features.inventory.catalog.mapper.interfaces;

import com.sni.bokaticowork.features.inventory.catalog.dto.request.InventoryUnitRequest;
import com.sni.bokaticowork.features.inventory.catalog.dto.response.InventoryUnitResponse;
import com.sni.bokaticowork.features.inventory.catalog.mapper.decorator.InventoryUnitMapperDecorator;
import com.sni.bokaticowork.features.inventory.catalog.model.InventoryUnit;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(InventoryUnitMapperDecorator.class)
public interface InventoryUnitMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    InventoryUnit toEntity(InventoryUnitRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget InventoryUnit entity, InventoryUnitRequest request);

    InventoryUnitResponse toResponse(InventoryUnit entity);
}
