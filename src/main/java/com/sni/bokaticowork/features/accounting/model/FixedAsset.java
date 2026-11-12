package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Fiche d'immobilisation (lot F18). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fixed_asset")
public class FixedAsset {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Builder.Default
    @Column(name = "company_code", nullable = false)
    private String companyCode = "MAIN";

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "category_code", nullable = false)
    private String categoryCode;

    @Column(name = "target_category_code")
    private String targetCategoryCode;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "serial_number")
    private String serialNumber;

    @Builder.Default
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Builder.Default
    @Column(name = "entry_mode", nullable = false)
    private String entryMode = "PURCHASE";

    @Column(name = "supplier_code")
    private String supplierCode;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "supplier_invoice_number")
    private String supplierInvoiceNumber;

    @Column(name = "supplier_invoice_line_no")
    private Integer supplierInvoiceLineNo;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "fund_request_ref")
    private String fundRequestRef;

    @Column(name = "justification_ref")
    private String justificationRef;

    @Column(name = "entry_number")
    private String entryNumber;

    @Builder.Default
    @Column(name = "booked", nullable = false)
    private Boolean booked = Boolean.FALSE;

    @Builder.Default
    @Column(name = "tracked_off_books", nullable = false)
    private Boolean trackedOffBooks = Boolean.FALSE;

    @Column(name = "acquisition_date")
    private LocalDate acquisitionDate;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "in_service_date")
    private LocalDate inServiceDate;

    @Builder.Default
    @Column(name = "entry_value", nullable = false)
    private BigDecimal entryValue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "residual_value", nullable = false)
    private BigDecimal residualValue = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "deducted_vat", nullable = false)
    private BigDecimal deductedVat = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "accounting_status", nullable = false)
    private String accountingStatus = "DRAFT";

    @Builder.Default
    @Column(name = "operational_status", nullable = false)
    private String operationalStatus = "IN_USE";

    @Column(name = "operational_since")
    private LocalDate operationalSince;

    @Column(name = "method")
    private String method;

    @Column(name = "duration_months")
    private Integer durationMonths;

    @Column(name = "coefficient")
    private BigDecimal coefficient;

    @Column(name = "total_units")
    private BigDecimal totalUnits;

    @Column(name = "tax_method")
    private String taxMethod;

    @Column(name = "tax_duration_months")
    private Integer taxDurationMonths;

    @Builder.Default
    @Column(name = "plan_version", nullable = false)
    private Integer planVersion = 0;

    @Builder.Default
    @Column(name = "accumulated_depreciation", nullable = false)
    private BigDecimal accumulatedDepreciation = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "accumulated_impairment", nullable = false)
    private BigDecimal accumulatedImpairment = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "accumulated_derogatory", nullable = false)
    private BigDecimal accumulatedDerogatory = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "subsidy_amount", nullable = false)
    private BigDecimal subsidyAmount = BigDecimal.ZERO;

    @Column(name = "subsidy_account")
    private String subsidyAccount;

    @Builder.Default
    @Column(name = "subsidy_released", nullable = false)
    private BigDecimal subsidyReleased = BigDecimal.ZERO;

    @Column(name = "organisation")
    private String organisation;

    @Column(name = "site")
    private String site;

    @Column(name = "resource_code")
    private String resourceCode;

    @Column(name = "amenity_link_id")
    private Long amenityLinkId;

    @Column(name = "location_label")
    private String locationLabel;

    @Column(name = "holder")
    private String holder;

    @Column(name = "holder_since")
    private LocalDate holderSince;

    @Column(name = "analytic_sections")
    private String analyticSections;

    @Column(name = "warranty_end")
    private LocalDate warrantyEnd;

    @Column(name = "maintenance_contract")
    private String maintenanceContract;

    @Column(name = "maintenance_end")
    private LocalDate maintenanceEnd;

    @Column(name = "insurer")
    private String insurer;

    @Column(name = "policy_number")
    private String policyNumber;

    @Column(name = "insured_value")
    private BigDecimal insuredValue;

    @Column(name = "document_refs")
    private String documentRefs;

    @Column(name = "photo_ref")
    private String photoRef;

    @Builder.Default
    @Column(name = "qr_active", nullable = false)
    private Boolean qrActive = Boolean.TRUE;

    @Builder.Default
    @Column(name = "label_print_count", nullable = false)
    private Integer labelPrintCount = 0;

    @Column(name = "notes")
    private String notes;

    @Column(name = "disposed_on")
    private LocalDate disposedOn;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Builder.Default
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
