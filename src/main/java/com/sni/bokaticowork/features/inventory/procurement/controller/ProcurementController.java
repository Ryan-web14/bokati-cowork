package com.sni.bokaticowork.features.inventory.procurement.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.idempotency.aop.Idempotent;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.inventory.procurement.dto.ProcurementDtos.*;
import com.sni.bokaticowork.features.inventory.procurement.enums.*;
import com.sni.bokaticowork.features.inventory.procurement.service.interfaces.ProcurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/inventory/procurement")
public class ProcurementController {

    private final ProcurementService service;

    @PostMapping("/suppliers")
    @Audited(module = "INVENTORY", action = "SUPPLIER_CREATE", ressource = "inventory_supplier")
    @Idempotent(operation = "INVENTORY_SUPPLIER_CREATE", required = false)
    public ResponseEntity<SupplierResponse> createSupplier(@Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(service.createSupplier(request));
    }

    @PutMapping("/suppliers/{supplierCode}")
    public ResponseEntity<SupplierResponse> updateSupplier(@PathVariable String supplierCode,
                                                           @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(service.updateSupplier(supplierCode, request));
    }

    @GetMapping("/suppliers")
    public ResponseEntity<Page<SupplierResponse>> suppliers(@RequestParam(required = false) String q,
                                                            @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.suppliers(q, pageable));
    }

    @PostMapping("/purchase-requests")
    @Audited(module = "INVENTORY", action = "PURCHASE_REQUEST_CREATE", ressource = "purchase_request")
    @Idempotent(operation = "INVENTORY_PURCHASE_REQUEST_CREATE", required = false)
    public ResponseEntity<PurchaseRequestResponse> createPurchaseRequest(@Valid @RequestBody PurchaseRequestCreateRequest request) {
        return ResponseEntity.ok(service.createPurchaseRequest(request));
    }

    @PostMapping("/purchase-requests/from-reorder-suggestions")
    @Audited(module = "INVENTORY", action = "PURCHASE_REQUEST_FROM_REORDER", ressource = "purchase_request")
    @Idempotent(operation = "INVENTORY_PURCHASE_REQUEST_FROM_REORDER", required = false)
    public ResponseEntity<PurchaseRequestResponse> createPurchaseRequestFromReorderSuggestions(@RequestParam(required = false) String locationCode,
                                                                                                @RequestParam(required = false) String supplierCode,
                                                                                                @RequestParam(required = false) String requestedBy) {
        return ResponseEntity.ok(service.createPurchaseRequestFromReorderSuggestions(locationCode, supplierCode, requestedBy));
    }

    @PatchMapping("/purchase-requests/{requestCode}/submit")
    public ResponseEntity<PurchaseRequestResponse> submitPurchaseRequest(@PathVariable String requestCode) {
        return ResponseEntity.ok(service.submitPurchaseRequest(requestCode));
    }

    @PatchMapping("/purchase-requests/{requestCode}/approve")
    public ResponseEntity<PurchaseRequestResponse> approvePurchaseRequest(@PathVariable String requestCode,
                                                                          @RequestParam(required = false) String approvedBy) {
        return ResponseEntity.ok(service.approvePurchaseRequest(requestCode, approvedBy));
    }

    @PatchMapping("/purchase-requests/{requestCode}/reject")
    public ResponseEntity<PurchaseRequestResponse> rejectPurchaseRequest(@PathVariable String requestCode,
                                                                         @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(service.rejectPurchaseRequest(requestCode, reason));
    }

    @GetMapping("/purchase-requests")
    public ResponseEntity<Page<PurchaseRequestResponse>> purchaseRequests(@RequestParam(required = false) PurchaseRequestStatus status,
                                                                          @RequestParam(required = false) String locationCode,
                                                                          @RequestParam(required = false) String q,
                                                                          @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.purchaseRequests(status, locationCode, q, pageable));
    }

    @PostMapping("/purchase-orders")
    @Audited(module = "INVENTORY", action = "PURCHASE_ORDER_CREATE", ressource = "inventory_purchase_order")
    @Idempotent(operation = "INVENTORY_PURCHASE_ORDER_CREATE", required = false)
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(@Valid @RequestBody PurchaseOrderCreateRequest request) {
        return ResponseEntity.ok(service.createPurchaseOrder(request));
    }

    @PostMapping("/purchase-requests/{requestCode}/purchase-order")
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrderFromRequest(@PathVariable String requestCode,
                                                                                @RequestParam String supplierCode) {
        return ResponseEntity.ok(service.createPurchaseOrderFromRequest(requestCode, supplierCode));
    }

    @PatchMapping("/purchase-orders/{orderCode}/approve")
    public ResponseEntity<PurchaseOrderResponse> approvePurchaseOrder(@PathVariable String orderCode,
                                                                      @RequestParam(required = false) String approvedBy) {
        return ResponseEntity.ok(service.approvePurchaseOrder(orderCode, approvedBy));
    }

    @PatchMapping("/purchase-orders/{orderCode}/mark-ordered")
    public ResponseEntity<PurchaseOrderResponse> markOrdered(@PathVariable String orderCode) {
        return ResponseEntity.ok(service.markOrdered(orderCode));
    }

    @GetMapping("/purchase-orders")
    public ResponseEntity<Page<PurchaseOrderResponse>> purchaseOrders(@RequestParam(required = false) PurchaseOrderStatus status,
                                                                      @RequestParam(required = false) String supplierCode,
                                                                      @RequestParam(required = false) String locationCode,
                                                                      @RequestParam(required = false) String q,
                                                                      @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(service.purchaseOrders(status, supplierCode, locationCode, q, pageable));
    }

    @PostMapping("/goods-receipts")
    @Audited(module = "INVENTORY", action = "GOODS_RECEIPT_POST", ressource = "goods_receipt")
    @Idempotent(operation = "INVENTORY_GOODS_RECEIPT_POST", required = false)
    public ResponseEntity<GoodsReceiptResponse> receiveGoods(@Valid @RequestBody GoodsReceiptRequest request) {
        return ResponseEntity.ok(service.receiveGoods(request));
    }

    @GetMapping("/goods-receipts")
    public ResponseEntity<Page<GoodsReceiptResponse>> goodsReceipts(@RequestParam(required = false) GoodsReceiptStatus status,
                                                                    @RequestParam(required = false) String locationCode,
                                                                    @RequestParam(required = false) String orderCode,
                                                                    @RequestParam(required = false) String q,
                                                                    @PageableDefault(size = 20, sort = "receivedAt") Pageable pageable) {
        return ResponseEntity.ok(service.goodsReceipts(status, locationCode, orderCode, q, pageable));
    }

    @GetMapping("/suppliers/{supplierCode}/performance")
    public ResponseEntity<SupplierPerformanceResponse> supplierPerformance(@PathVariable String supplierCode) {
        return ResponseEntity.ok(service.supplierPerformance(supplierCode));
    }

    @PostMapping("/approval-rules")
    @Audited(module = "INVENTORY", action = "PURCHASE_APPROVAL_RULE_SAVE", ressource = "purchase_approval_rule")
    public ResponseEntity<PurchaseApprovalRuleResponse> saveApprovalRule(@Valid @RequestBody PurchaseApprovalRuleRequest request) {
        return ResponseEntity.ok(service.saveApprovalRule(request));
    }

    @GetMapping("/approval-rules")
    public ResponseEntity<List<PurchaseApprovalRuleResponse>> approvalRules() {
        return ResponseEntity.ok(service.approvalRules());
    }
}
