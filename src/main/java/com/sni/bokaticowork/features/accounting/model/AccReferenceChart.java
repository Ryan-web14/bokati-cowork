package com.sni.bokaticowork.features.accounting.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nomenclature SYSCOHADA de reference, en lecture : bornes de la creation de comptes principaux. */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "acc_reference_chart")
public class AccReferenceChart {
    @Id
    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "label", nullable = false, length = 255)
    private String label;

    @Column(name = "class_code", nullable = false, length = 1)
    private String classCode;
}
