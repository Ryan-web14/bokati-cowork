package com.sni.bokaticowork.features.accounting.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Evenement de la vie d'un emprunt (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_loan_event")
public class AccLoanEvent {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "event_date", nullable = false)
    private LocalDate eventDate;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "rate")
    private BigDecimal rate;

    @Column(name = "detail")
    private String detail;

    @Column(name = "from_version")
    private Integer fromVersion;

    @Column(name = "to_version")
    private Integer toVersion;

    @Column(name = "entry_number")
    private String entryNumber;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
