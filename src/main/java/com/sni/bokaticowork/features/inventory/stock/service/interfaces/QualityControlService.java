package com.sni.bokaticowork.features.inventory.stock.service.interfaces;

import com.sni.bokaticowork.features.inventory.stock.dto.request.NonConformanceRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.QualityControlPlanRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.QualityInspectionRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.NonConformanceResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.QualityControlPlanResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.QualityInspectionResponse;
import com.sni.bokaticowork.features.inventory.stock.model.StockLot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Controle qualite : plans, inspections et non-conformites.
 *
 * <p>Tant qu aucun plan n est declare, rien ne change : les receptions se comportent exactement
 * comme avant, et aucun lot n est immobilise automatiquement.</p>
 */
public interface QualityControlService {

    QualityControlPlanResponse createPlan(QualityControlPlanRequest request);

    QualityControlPlanResponse getPlan(String planCode);

    List<QualityControlPlanResponse> listPlans();

    /**
     * Execute un controle sur un lot : evalue les criteres, en deduit une decision, et applique ses
     * consequences sur le lot.
     */
    QualityInspectionResponse inspect(Long lotId, QualityInspectionRequest request);

    List<QualityInspectionResponse> inspectionsForLot(Long lotId);

    /**
     * Applique le plan de reception a un lot qui vient d entrer en stock.
     *
     * <p>Sans effet lorsque aucun plan actif ne demande la mise en quarantaine a la reception.</p>
     *
     * @return vrai si le lot a ete mis en quarantaine
     */
    boolean applyReceiptPlan(StockLot lot);

    NonConformanceResponse openNonConformance(NonConformanceRequest request);

    NonConformanceResponse closeNonConformance(String code, String closedBy, String correctiveAction);

    Page<NonConformanceResponse> searchNonConformances(String itemCode, Boolean openOnly, Pageable pageable);
}
