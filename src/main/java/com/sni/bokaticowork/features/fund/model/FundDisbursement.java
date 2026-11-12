package com.sni.bokaticowork.features.fund.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Remise, complement ou retour d'argent lie a une demande. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "fund_disbursement")
public class FundDisbursement {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false)
    private com.sni.bokaticowork.features.fund.enums.FundDisbursementKind kind;

    @Column(name = "movement_number", nullable = false)
    private String movementNumber;

    @Column(name = "session_number")
    private String sessionNumber;

    @Column(name = "amount", nullable = false)
    private java.math.BigDecimal amount;

    @Column(name = "signed_by_name")
    private String signedByName;

    @Column(name = "signature", columnDefinition = "TEXT")
    private String signature;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
