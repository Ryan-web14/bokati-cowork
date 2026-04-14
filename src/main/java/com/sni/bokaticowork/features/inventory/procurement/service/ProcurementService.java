package com.sni.bokaticowork.features.inventory.procurement.service;

import com.sni.bokaticowork.features.inventory.procurement.dto.ProcurementDtos.*;
import com.sni.bokaticowork.features.inventory.procurement.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProcurementService {
    SupplierResponse createSupplier(SupplierRequest request);

    SupplierResponse updateSupplier(String supplierCode, SupplierRequest request);

    Page<SupplierResponse> suppliers(String query, Pageable pageable);

    PurchaseRequestResponse createPurchaseRequest(PurchaseRequestCreateRequest request);

    PurchaseRequestResponse createPurchaseRequestFromReorderSuggestions(String locationCode, String supplierCode, String requestedBy);

    PurchaseRequestResponse submitPurchaseRequest(String requestCode);

    PurchaseRequestResponse approvePurchaseRequest(String requestCode, String approvedBy);

    PurchaseRequestResponse rejectPurchaseRequest(String requestCode, String reason);

    PurchaseOrderResponse createPurchaseOrder(PurchaseOrderCreateRequest request);

    PurchaseOrderResponse createPurchaseOrderFromRequest(String requestCode, String supplierCode);

    PurchaseOrderResponse approvePurchaseOrder(String orderCode, String approvedBy);

    PurchaseOrderResponse markOrdered(String orderCode);

    GoodsReceiptResponse receiveGoods(GoodsReceiptRequest request);

    Page<PurchaseRequestResponse> purchaseRequests(PurchaseRequestStatus status, String locationCode, String query, Pageable pageable);

    Page<PurchaseOrderResponse> purchaseOrders(PurchaseOrderStatus status, String supplierCode, String locationCode, String query, Pageable pageable);

    Page<GoodsReceiptResponse> goodsReceipts(GoodsReceiptStatus status, String locationCode, String orderCode, String query, Pageable pageable);

    SupplierPerformanceResponse supplierPerformance(String supplierCode);

    PurchaseApprovalRuleResponse saveApprovalRule(PurchaseApprovalRuleRequest request);

    java.util.List<PurchaseApprovalRuleResponse> approvalRules();
}
