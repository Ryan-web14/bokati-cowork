package com.sni.bokaticowork.features.inventory.intelligence.mapper.interfaces;

import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryAlertResponse;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryReorderRuleResponse;
import com.sni.bokaticowork.features.inventory.intelligence.mapper.decorator.InventoryIntelligenceMapperDecorator;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryAlert;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryReorderRule;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
@DecoratedWith(InventoryIntelligenceMapperDecorator.class)
public interface InventoryIntelligenceMapper {
    InventoryAlertResponse toAlertResponse(InventoryAlert alert);
    InventoryReorderRuleResponse toRuleResponse(InventoryReorderRule rule);
}
