package com.sni.bokaticowork.features.inventory.procurement.dto;

import com.sni.bokaticowork.features.inventory.procurement.enums.*;
import com.sni.bokaticowork.features.inventory.stock.enums.StockOwnershipType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class ProcurementDtos {
    private ProcurementDtos() {
    }

    @Data
    public static class SupplierRequest {
        private String supplierCode;
        @NotBlank
        private String name;
        private String email;
        private String phone;
        private String taxId;
        private String address;
        private SupplierStatus status;
    }

    @Data
    public static class PurchaseLineRequest {
        @NotBlank
        private String itemCode;
        @NotNull
        @Positive
        private BigDecimal quantity;
        private Long unitCost;
    }

    @Data
    public static class PurchaseRequestCreateRequest {
        private String locationCode;
        private String requestedBy;
        @Valid
        @NotEmpty
        private List<PurchaseLineRequest> lines;
    }

    @Data
    public static class PurchaseRequestApprovalRequest {
        private String approvedBy;
        private String supplierCode;
    }

    @Data
    public static class PurchaseOrderCreateRequest {
        @NotBlank
        private String supplierCode;
        private String locationCode;
        private String sourceRequestCode;
        private LocalDate expectedDeliveryDate;
        private String orderedBy;
        @Valid
        @NotEmpty
        private List<PurchaseLineRequest> lines;
    }

    @Data
    public static class GoodsReceiptLineRequest {
        @NotBlank
        private String itemCode;
        @NotNull
        @Positive
        private BigDecimal receivedQuantity;
        private BigDecimal rejectedQuantity;
        private String rejectionReason;
        private Boolean qualityAccepted;
        private Boolean quarantineAcceptedStock;
        private String quarantineReason;
        private StockOwnershipType ownershipType;
        private String ownerCode;
        private Long unitCost;
        private String lotNumber;
        private LocalDate expiryDate;
    }

    @Data
    public static class GoodsReceiptRequest {
        private String orderCode;
        @NotBlank
        private String locationCode;
        private String receivedBy;
        private String invoiceDocumentCode;
        private String deliveryNoteDocumentCode;
        private String proofDocumentCode;
        @Valid
        @NotEmpty
        private List<GoodsReceiptLineRequest> lines;
    }

    @Data
    public static class PurchaseApprovalRuleRequest {
        @NotNull
        private PurchaseApprovalLevel approvalLevel;
        @NotNull
        private Long minAmount;
        private Long maxAmount;
        @NotNull
        @Positive
        private Integer requiredApprovals;
        private Boolean active;
    }

    @Data
    @Builder
    public static class SupplierResponse {
        private String supplierCode;
        private String name;
        private String email;
        private String phone;
        private String taxId;
        private String address;
        private SupplierStatus status;
    }

    @Data
    @Builder
    public static class ProcurementLineResponse {
        private String itemCode;
        private String itemName;
        private BigDecimal quantity;
        private BigDecimal receivedQuantity;
        private BigDecimal rejectedQuantity;
        private BigDecimal backorderQuantity;
        private Boolean qualityAccepted;
        private String rejectionReason;
        private Long unitCost;
        private Long totalCost;
        private String lotNumber;
        private LocalDate expiryDate;
    }

    @Data
    @Builder
    public static class PurchaseRequestResponse {
        private String requestCode;
        private String locationCode;
        private String locationName;
        private PurchaseRequestStatus status;
        private String requestedBy;
        private String approvedBy;
        private String rejectionReason;
        private Instant createdAt;
        private List<ProcurementLineResponse> lines;
        private String autoCreatedOrderCode;
    }

    @Data
    @Builder
    public static class PurchaseOrderResponse {
        private String orderCode;
        private String supplierCode;
        private String supplierName;
        private String locationCode;
        private String sourceRequestCode;
        private PurchaseOrderStatus status;
        private PurchaseApprovalLevel approvalLevel;
        private String approvedBy;
        private Instant approvedAt;
        private Long totalAmount;
        private LocalDate expectedDeliveryDate;
        private String orderedBy;
        private Instant createdAt;
        private List<ProcurementLineResponse> lines;
        private List<PurchaseApprovalStepResponse> approvalSteps;
    }

    @Data
    @Builder
    public static class GoodsReceiptResponse {
        private String receiptCode;
        private String orderCode;
        private String locationCode;
        private GoodsReceiptStatus status;
        private String invoiceDocumentCode;
        private String deliveryNoteDocumentCode;
        private String proofDocumentCode;
        private String receivedBy;
        private Instant receivedAt;
        private Instant postedAt;
        private List<ProcurementLineResponse> lines;
    }

    @Data
    @Builder
    public static class SupplierPerformanceResponse {
        private String supplierCode;
        private String supplierName;
        private long purchaseOrderCount;
        private long receivedOrderCount;
        private long completeReceiptCount;
        private double completeReceiptRate;
        private Double averageDeliveryDelayDays;
        private Long averageUnitCost;
    }

    @Data
    @Builder
    public static class PurchaseApprovalRuleResponse {
        private PurchaseApprovalLevel approvalLevel;
        private Long minAmount;
        private Long maxAmount;
        private Integer requiredApprovals;
        private Boolean active;
    }

    @Data
    @Builder
    public static class PurchaseApprovalStepResponse {
        private PurchaseApprovalLevel approvalLevel;
        private String approvedBy;
        private Instant approvedAt;
    }
}
