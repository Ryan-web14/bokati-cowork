package com.sni.bokaticowork.features.fund.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ligne d'une demande de fonds. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fund_request_line")
public class FundRequestLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "nature_code")
    private String natureCode;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "supplier")
    private String supplier;

    @Column(name = "section")
    private String section;

    @PrePersist
    void prePersist() {
    }
}
