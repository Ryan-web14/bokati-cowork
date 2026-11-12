package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Section proposee pour les comptes commencant par un prefixe. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_analytic_section_account")
public class AccAnalyticSectionAccount {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "axis_code", nullable = false)
    private String axisCode;

    @Column(name = "section_code", nullable = false)
    private String sectionCode;

    @Column(name = "account_prefix", nullable = false)
    private String accountPrefix;

    @PrePersist
    void prePersist() {
    }
}
