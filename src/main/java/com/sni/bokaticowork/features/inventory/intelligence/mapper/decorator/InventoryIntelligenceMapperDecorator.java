package com.sni.bokaticowork.features.inventory.intelligence.mapper.decorator;

import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryAlertResponse;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryReorderRuleResponse;
import com.sni.bokaticowork.features.inventory.intelligence.mapper.interfaces.InventoryIntelligenceMapper;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryAlert;
import com.sni.bokaticowork.features.inventory.intelligence.model.InventoryReorderRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public abstract class InventoryIntelligenceMapperDecorator implements InventoryIntelligenceMapper {
    @Autowired
    @Qualifier("delegate")
    private InventoryIntelligenceMapper delegate;

    @Override
    public InventoryAlertResponse toAlertResponse(InventoryAlert alert) {
        InventoryAlertResponse response = delegate.toAlertResponse(alert);
        if (alert == null) return response;
        if (alert.getItem() != null) {
            response.setItemCode(alert.getItem().getItemCode());
            response.setItemName(alert.getItem().getName());
        }
        if (alert.getLocation() != null) {
            response.setLocationCode(alert.getLocation().getLocationCode());
            response.setLocationName(alert.getLocation().getName());
        }
        return response;
    }

    @Override
    public InventoryReorderRuleResponse toRuleResponse(InventoryReorderRule rule) {
        InventoryReorderRuleResponse response = delegate.toRuleResponse(rule);
        if (rule == null) return response;
        if (rule.getItem() != null) {
            response.setItemCode(rule.getItem().getItemCode());
            response.setItemName(rule.getItem().getName());
        }
        if (rule.getLocation() != null) {
            response.setLocationCode(rule.getLocation().getLocationCode());
            response.setLocationName(rule.getLocation().getName());
        }
        return response;
    }
}
