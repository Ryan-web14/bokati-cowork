package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "billing_tax_rule")
public class TaxRule {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 120)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "tax_type", nullable = false, length = 60)
    private String taxType;

    @Column(name = "rate", nullable = false, precision = 9, scale = 4)
    private BigDecimal rate;

    @Column(name = "applies_on", nullable = false, length = 60)
    private String appliesOn;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;
}
