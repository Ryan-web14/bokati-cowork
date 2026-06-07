package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.enums.CashRequestStatus;
import com.sni.bokaticowork.features.payment.enums.CashRequestType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_request")
public class CashRequest {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true, length = 100)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_session_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cash_request_session"))
    private CashSession cashSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 40)
    private CashRequestType requestType;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CashRequestStatus status = CashRequestStatus.PENDING;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "requested_by", nullable = false, length = 120)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note")
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executed_movement_id", foreignKey = @ForeignKey(name = "fk_cash_request_executed_movement"))
    private CashMovement executedMovement;

    @Column(name = "executed_at")
    private Instant executedAt;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "attachments_json", columnDefinition = "jsonb")
    private String attachmentsJson;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @PrePersist
    public void prePersist() {
        requestedAt = Instant.now();
    }
}
