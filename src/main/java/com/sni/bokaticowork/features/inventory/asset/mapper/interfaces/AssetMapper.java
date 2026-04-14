package com.sni.bokaticowork.features.inventory.asset.mapper.interfaces;

import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetAssignmentResponse;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetResponse;
import com.sni.bokaticowork.features.inventory.asset.mapper.decorator.AssetMapperDecorator;
import com.sni.bokaticowork.features.inventory.asset.model.Asset;
import com.sni.bokaticowork.features.inventory.asset.model.AssetAssignment;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(AssetMapperDecorator.class)
public interface AssetMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assetCode", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "assignedToType", ignore = true)
    @Mapping(target = "assignedToCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Asset toEntity(AssetRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "assetCode", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "assignedToType", ignore = true)
    @Mapping(target = "assignedToCode", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Asset asset, AssetRequest request);

    AssetResponse toResponse(Asset asset);

    AssetAssignmentResponse toAssignmentResponse(AssetAssignment assignment);
}
