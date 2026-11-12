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

/** Fournisseur (lot F13). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_supplier")
public class AccSupplier {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "niu")
    private String niu;

    @Column(name = "rccm")
    private String rccm;

    @Column(name = "address")
    private String address;

    @Column(name = "phone")
    private String phone;

    @Column(name = "email")
    private String email;

    @Column(name = "bank_details")
    private String bankDetails;

    @Builder.Default
    @Column(name = "payment_terms_days", nullable = false)
    private Integer paymentTermsDays = 30;

    @Builder.Default
    @Column(name = "account_number", nullable = false)
    private String accountNumber = "401";

    @Column(name = "default_account")
    private String defaultAccount;

    @Builder.Default
    @Column(name = "vat_exempt", nullable = false)
    private Boolean vatExempt = Boolean.FALSE;

    @Builder.Default
    @Column(name = "withholding_rate", nullable = false)
    private BigDecimal withholdingRate = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "withholding_account", nullable = false)
    private String withholdingAccount = "447";

    @Builder.Default
    @Column(name = "withholding_base", nullable = false)
    private String withholdingBase = "HT";

    @Column(name = "withholding_label")
    private String withholdingLabel;

    /** Fiche fournisseur d'inventaire reliee (facultatif, au plus un fournisseur comptable par fiche). */
    @Column(name = "inventory_supplier_id")
    private Long inventorySupplierId;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
