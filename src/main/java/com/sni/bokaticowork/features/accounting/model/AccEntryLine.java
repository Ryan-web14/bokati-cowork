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

/** Ligne d'une piece : un debit ou un credit, jamais les deux. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_entry_line")
public class AccEntryLine {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "entry_id", nullable = false)
    private Long entryId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "third_party_type", length = 20)
    private com.sni.bokaticowork.features.accounting.enums.ThirdPartyType thirdPartyType;

    @Column(name = "third_party_code", length = 120)
    private String thirdPartyCode;

    @Column(name = "label", length = 500)
    private String label;

    @Builder.Default
    @Column(name = "debit", nullable = false, precision = 19, scale = 4)
    private BigDecimal debit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "credit", nullable = false, precision = 19, scale = 4)
    private BigDecimal credit = BigDecimal.ZERO;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "foreign_amount", precision = 19, scale = 4)
    private BigDecimal foreignAmount;

    @Column(name = "rate", precision = 19, scale = 8)
    private BigDecimal rate;

    @Column(name = "tax_code", length = 30)
    private String taxCode;

    @Column(name = "tax_base", precision = 19, scale = 4)
    private BigDecimal taxBase;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "reference", length = 120)
    private String reference;
}
