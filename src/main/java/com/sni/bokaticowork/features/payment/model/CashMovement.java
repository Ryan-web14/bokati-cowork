package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementChannel;
import com.sni.bokaticowork.features.payment.enums.CashMovementStatus;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_movement")
public class CashMovement {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "movement_number", nullable = false, unique = true, length = 100)
    private String movementNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_session_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cash_movement_session"))
    private CashSession cashSession;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 40)
    private CashMovementType movementType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 40)
    private CashDocumentType documentType;

    @Column(name = "document_number", length = 120)
    private String documentNumber;

    @Column(name = "flow_category", length = 120)
    private String flowCategory;

    @Column(name = "reference_type", length = 80)
    private String referenceType;

    @Column(name = "reference_code", length = 120)
    private String referenceCode;

    @Column(name = "counterparty_type", length = 80)
    private String counterpartyType;

    @Column(name = "counterparty_code", length = 120)
    private String counterpartyCode;

    @Column(name = "counterparty_name", length = 255)
    private String counterpartyName;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @org.hibernate.annotations.ColumnTransformer(write = "?::jsonb")
    @Column(name = "metadata_json", columnDefinition = "jsonb")
    private String metadataJson;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CashMovementStatus status = CashMovementStatus.CONFIRMED;

    @Column(name = "related_movement_id")
    private Long relatedMovementId;

    @Column(name = "batch_id", length = 100)
    private String batchId;

    @Column(name = "running_balance", precision = 19, scale = 4)
    private BigDecimal runningBalance;

    @Column(name = "exchange_rate", precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 40)
    private CashMovementChannel channel = CashMovementChannel.MANUAL;

    @Column(name = "device_code", length = 120)
    private String deviceCode;

    @Column(name = "device_ip", length = 64)
    private String deviceIp;

    @Column(name = "sub_category", length = 120)
    private String subCategory;

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "risk_score", precision = 6, scale = 2)
    private BigDecimal riskScore;

    @Builder.Default
    @Column(name = "requires_signature", nullable = false)
    private Boolean requiresSignature = false;

    @Column(name = "signed_by", length = 120)
    private String signedBy;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "printed_at")
    private Instant printedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
