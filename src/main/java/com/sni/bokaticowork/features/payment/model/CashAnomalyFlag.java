package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.enums.CashAnomalySeverity;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyStatus;
import com.sni.bokaticowork.features.payment.enums.CashAnomalyType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_anomaly_flag")
public class CashAnomalyFlag {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "flag_number", nullable = false, unique = true, length = 100)
    private String flagNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_session_id", foreignKey = @ForeignKey(name = "fk_cash_anomaly_flag_session"))
    private CashSession cashSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_movement_id", foreignKey = @ForeignKey(name = "fk_cash_anomaly_flag_movement"))
    private CashMovement cashMovement;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_type", nullable = false, length = 60)
    private CashAnomalyType anomalyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private CashAnomalySeverity severity;

    @Column(name = "score", precision = 6, scale = 2)
    private BigDecimal score;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CashAnomalyStatus status = CashAnomalyStatus.OPEN;

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note")
    private String reviewNote;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @PrePersist
    public void prePersist() {
        detectedAt = Instant.now();
    }
}
