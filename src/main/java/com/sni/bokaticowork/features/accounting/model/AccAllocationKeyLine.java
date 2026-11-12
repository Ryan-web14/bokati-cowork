package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Part d'une section dans une cle. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_allocation_key_line")
public class AccAllocationKeyLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "key_id", nullable = false)
    private Long keyId;

    @Column(name = "section_code", nullable = false)
    private String sectionCode;

    @Column(name = "percentage", nullable = false)
    private java.math.BigDecimal percentage;

    @PrePersist
    void prePersist() {
    }
}
