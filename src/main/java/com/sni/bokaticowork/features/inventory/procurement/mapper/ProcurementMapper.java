package com.sni.bokaticowork.features.inventory.procurement.mapper;

import com.sni.bokaticowork.features.inventory.procurement.dto.ProcurementDtos.*;
import com.sni.bokaticowork.features.inventory.procurement.model.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProcurementMapper {

    public SupplierResponse toSupplierResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .supplierCode(supplier.getSupplierCode())
                .name(supplier.getName())
                .email(supplier.getEmail())
                .phone(supplier.getPhone())
                .taxId(supplier.getTaxId())
                .address(supplier.getAddress())
                .status(supplier.getStatus())
                .build();
    }

    public PurchaseRequestResponse toPurchaseRequestResponse(PurchaseRequest pr) {
        return PurchaseRequestResponse.builder()
                .requestCode(pr.getRequestCode())
                .locationCode(pr.getLocation() == null ? null : pr.getLocation().getLocationCode())
                .locationName(pr.getLocation() == null ? null : pr.getLocation().getName())
                .status(pr.getStatus())
                .requestedBy(pr.getRequestedBy())
                .approvedBy(pr.getApprovedBy())
                .rejectionReason(pr.getRejectionReason())
                .createdAt(pr.getCreatedAt())
                .lines(pr.getLines().stream().map(this::toPurchaseRequestLineResponse).toList())
                .build();
    }

    public PurchaseOrderResponse toPurchaseOrderResponse(PurchaseOrder po) {
        return PurchaseOrderResponse.builder()
                .orderCode(po.getOrderCode())
                .supplierCode(po.getSupplier().getSupplierCode())
                .supplierName(po.getSupplier().getName())
                .locationCode(po.getLocation() == null ? null : po.getLocation().getLocationCode())
                .sourceRequestCode(po.getSourceRequestCode())
                .status(po.getStatus())
                .approvalLevel(po.getApprovalLevel())
                .approvedBy(po.getApprovedBy())
                .approvedAt(po.getApprovedAt())
                .totalAmount(po.getTotalAmount())
                .expectedDeliveryDate(po.getExpectedDeliveryDate())
                .orderedBy(po.getOrderedBy())
                .createdAt(po.getCreatedAt())
                .lines(po.getLines().stream().map(this::toPurchaseOrderLineResponse).toList())
                .build();
    }

    public PurchaseApprovalRuleResponse toApprovalRuleResponse(PurchaseApprovalRule rule) {
        return PurchaseApprovalRuleResponse.builder()
                .approvalLevel(rule.getApprovalLevel())
                .minAmount(rule.getMinAmount())
                .maxAmount(rule.getMaxAmount())
                .requiredApprovals(rule.getRequiredApprovals())
                .active(rule.getActive())
                .build();
    }

    public PurchaseApprovalStepResponse toApprovalStepResponse(PurchaseApprovalStep step) {
        return PurchaseApprovalStepResponse.builder()
                .approvalLevel(step.getApprovalLevel())
                .approvedBy(step.getApprovedBy())
                .approvedAt(step.getApprovedAt())
                .build();
    }

    public GoodsReceiptResponse toGoodsReceiptResponse(GoodsReceipt receipt) {
        return GoodsReceiptResponse.builder()
                .receiptCode(receipt.getReceiptCode())
                .orderCode(receipt.getPurchaseOrder() == null ? null : receipt.getPurchaseOrder().getOrderCode())
                .locationCode(receipt.getLocation().getLocationCode())
                .status(receipt.getStatus())
                .invoiceDocumentCode(receipt.getInvoiceDocumentCode())
                .deliveryNoteDocumentCode(receipt.getDeliveryNoteDocumentCode())
                .proofDocumentCode(receipt.getProofDocumentCode())
                .receivedBy(receipt.getReceivedBy())
                .receivedAt(receipt.getReceivedAt())
                .postedAt(receipt.getPostedAt())
                .lines(receipt.getLines().stream().map(this::toGoodsReceiptLineResponse).toList())
                .build();
    }

    private ProcurementLineResponse toPurchaseRequestLineResponse(PurchaseRequestLine line) {

        var decimalValue = line.getQuantity().multiply(BigDecimal.valueOf(line.getEstimatedUnitCost()));
        var totalValue = decimalValue.longValue();

        return ProcurementLineResponse.builder()
                .itemCode(line.getItem().getItemCode())
                .itemName(line.getItem().getName())
                .quantity(line.getQuantity())
                .unitCost(line.getEstimatedUnitCost())
                .totalCost(totalValue)
                .build();
    }

    private ProcurementLineResponse toPurchaseOrderLineResponse(PurchaseOrderLine line) {
        return ProcurementLineResponse.builder()
                .itemCode(line.getItem().getItemCode())
                .itemName(line.getItem().getName())
                .quantity(line.getOrderedQuantity())
                .receivedQuantity(line.getReceivedQuantity())
                .backorderQuantity(line.getOrderedQuantity().subtract(line.getReceivedQuantity()))
                .unitCost(line.getUnitCost())
                .build();
    }

    private ProcurementLineResponse toGoodsReceiptLineResponse(GoodsReceiptLine line) {
        return ProcurementLineResponse.builder()
                .itemCode(line.getItem().getItemCode())
                .itemName(line.getItem().getName())
                .quantity(line.getReceivedQuantity())
                .receivedQuantity(line.getReceivedQuantity())
                .rejectedQuantity(line.getRejectedQuantity())
                .backorderQuantity(line.getBackorderQuantity())
                .qualityAccepted(line.getQualityAccepted())
                .rejectionReason(line.getRejectionReason())
                .unitCost(line.getUnitCost())
                .lotNumber(line.getLotNumber())
                .expiryDate(line.getExpiryDate())
                .build();
    }
}
