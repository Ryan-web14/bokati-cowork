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

/** Engagement hors bilan, tenu en classe 90 (lot F17). */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "acc_commitment")
public class AccCommitment {
    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "type_code", nullable = false)
    private String typeCode;

    @Column(name = "direction", nullable = false)
    private String direction;

    @Column(name = "third_party_code")
    private String thirdPartyCode;

    @Column(name = "third_party_name", nullable = false)
    private String thirdPartyName;

    @Column(name = "description")
    private String description;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "monthly_amount")
    private BigDecimal monthlyAmount;

    @Builder.Default
    @Column(name = "currency", nullable = false)
    private String currency = "XAF";

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "renewal_date")
    private LocalDate renewalDate;

    @Column(name = "notice_date")
    private LocalDate noticeDate;

    @Column(name = "linked_type")
    private String linkedType;

    @Column(name = "linked_code")
    private String linkedCode;

    @Column(name = "document_refs")
    private String documentRefs;

    @Builder.Default
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(name = "booked_amount", nullable = false)
    private BigDecimal bookedAmount = BigDecimal.ZERO;

    @Column(name = "last_entry_number")
    private String lastEntryNumber;

    @Column(name = "released_on")
    private LocalDate releasedOn;

    @Column(name = "release_reference")
    private String releaseReference;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}
