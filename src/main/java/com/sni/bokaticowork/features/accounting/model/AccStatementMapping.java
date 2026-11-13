package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Correspondance d'un prefixe de compte vers un poste (lot F12). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_statement_mapping")
public class AccStatementMapping {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "line_code")
    private String lineCode;

    @Column(name = "account_prefix")
    private String accountPrefix;

    @Builder.Default
    @Column(name = "balance_side")
    private String balanceSide = "ANY";

    @Builder.Default
    @Column(name = "value_column")
    private String valueColumn = "GROSS";
}
