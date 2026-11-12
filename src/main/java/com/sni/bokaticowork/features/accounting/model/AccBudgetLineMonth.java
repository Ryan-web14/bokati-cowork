package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Mensualisation d'une ligne. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_budget_line_month")
public class AccBudgetLineMonth {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @PrePersist
    void prePersist() {
    }
}
