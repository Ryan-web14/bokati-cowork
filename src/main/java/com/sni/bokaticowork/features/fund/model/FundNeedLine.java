package com.sni.bokaticowork.features.fund.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ligne d'un etat de besoin. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fund_need_line")
public class FundNeedLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "statement_id", nullable = false)
    private Long statementId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "quantity", nullable = false)
    private java.math.BigDecimal quantity;

    @Column(name = "unit")
    private String unit;

    @Column(name = "unit_price", nullable = false)
    private java.math.BigDecimal unitPrice;

    @Column(name = "total_amount", nullable = false)
    private java.math.BigDecimal totalAmount;

    @Column(name = "nature_code")
    private String natureCode;

    @Column(name = "supplier")
    private String supplier;

    /** Fiche fournisseur d'inventaire (facultatif) ; supplier reprend alors son nom. */
    @Column(name = "supplier_code")
    private String supplierCode;

    @Column(name = "transformed_request_number")
    private String transformedRequestNumber;

    @PrePersist
    void prePersist() {
    }
}
