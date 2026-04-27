package com.sni.bokaticowork.features.inventory.intelligence.service.interfaces;

import com.sni.bokaticowork.features.inventory.intelligence.dto.request.InventoryReorderRuleRequest;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.InventoryReorderRuleResponse;
import com.sni.bokaticowork.features.inventory.intelligence.dto.response.ReorderSuggestionResponse;
import com.sni.bokaticowork.features.inventory.intelligence.enums.ReorderSuggestionSeverity;

import java.util.List;

public interface InventoryReorderRuleService {
    InventoryReorderRuleResponse createOrUpdate(InventoryReorderRuleRequest request);
    List<InventoryReorderRuleResponse> list();
    List<ReorderSuggestionResponse> suggestions(String itemCode, String locationCode, ReorderSuggestionSeverity severity);
}
