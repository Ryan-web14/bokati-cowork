package com.sni.bokaticowork.features.payment.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_session")
public class CashSession {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "session_number", nullable = false, unique = true, length = 100)
    private String sessionNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cash_register_id", nullable = false, foreignKey = @ForeignKey(name = "fk_cash_session_register"))
    private CashRegister cashRegister;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private CashSessionStatus status;

    @Column(name = "opened_by", nullable = false, length = 120)
    private String openedBy;

    @Column(name = "closed_by", length = 120)
    private String closedBy;

    @Column(name = "reviewed_by", length = 120)
    private String reviewedBy;

    @Column(name = "opening_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal openingAmount;

    @Column(name = "closing_amount", precision = 19, scale = 4)
    private BigDecimal closingAmount;

    @Column(name = "expected_closing_amount", precision = 19, scale = 4)
    private BigDecimal expectedClosingAmount;

    @Column(name = "counted_closing_amount", precision = 19, scale = 4)
    private BigDecimal countedClosingAmount;

    @Column(name = "variance_amount", precision = 19, scale = 4)
    private BigDecimal varianceAmount;

    @Column(name = "variance_reason")
    private String varianceReason;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closing_requested_at")
    private Instant closingRequestedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @PrePersist
    public void prePersist() {
        openedAt = Instant.now();
    }
}
