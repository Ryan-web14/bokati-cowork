package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
