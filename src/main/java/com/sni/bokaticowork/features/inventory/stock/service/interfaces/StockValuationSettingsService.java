package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.AdjustmentReasonRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.InventoryAdjustmentApprovalRuleRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.ItemValuationMethodRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.AdjustmentReasonResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.InventoryAdjustmentApprovalRuleResponse;

import java.util.List;

/**
 * Parametrage de la valorisation : motifs d'ajustement, seuils d'approbation, methode par article.
 */
public interface StockValuationSettingsService {

    AdjustmentReasonResponse createReason(AdjustmentReasonRequest request);

    List<AdjustmentReasonResponse> listReasons(Boolean active);

    InventoryAdjustmentApprovalRuleResponse createApprovalRule(InventoryAdjustmentApprovalRuleRequest request);

    List<InventoryAdjustmentApprovalRuleResponse> listApprovalRules();

    /**
     * Change la methode de valorisation d'un article et amorce les couches si besoin.
     *
     * @return nombre de couches amorcees
     */
    int changeItemValuationMethod(String itemCode, ItemValuationMethodRequest request);
}
