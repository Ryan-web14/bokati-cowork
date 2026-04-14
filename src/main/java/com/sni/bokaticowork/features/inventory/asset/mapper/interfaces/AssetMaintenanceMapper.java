package com.sni.bokaticowork.features.inventory.asset.mapper.interfaces;

import com.sni.bokaticowork.features.inventory.asset.dto.request.AssetMaintenanceRequest;
import com.sni.bokaticowork.features.inventory.asset.dto.response.AssetMaintenanceResponse;
import com.sni.bokaticowork.features.inventory.asset.mapper.decorator.AssetMaintenanceMapperDecorator;
import com.sni.bokaticowork.features.inventory.asset.model.AssetMaintenance;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(AssetMaintenanceMapperDecorator.class)
public interface AssetMaintenanceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "maintenanceCode", ignore = true)
    @Mapping(target = "asset", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "resolution", ignore = true)
    AssetMaintenance toEntity(AssetMaintenanceRequest request);

    AssetMaintenanceResponse toResponse(AssetMaintenance maintenance);
}
