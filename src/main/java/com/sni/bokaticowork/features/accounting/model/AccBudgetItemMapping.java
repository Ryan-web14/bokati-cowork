package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ce qui alimente un poste budgetaire : prefixe de compte, poste de depense, nature. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_budget_item_mapping")
public class AccBudgetItemMapping {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "item_code", nullable = false)
    private String itemCode;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "value", nullable = false)
    private String value;

    @PrePersist
    void prePersist() {
    }
}
