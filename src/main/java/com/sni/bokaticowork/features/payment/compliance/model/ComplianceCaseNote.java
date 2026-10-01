package com.sni.bokaticowork.features.payment.compliance.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Une note dans un dossier · qui, quand, quoi. Les notes s'ajoutent, ne se corrigent pas. */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "compliance_case_note")
public class ComplianceCaseNote {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private ComplianceCase complianceCase;

    @Column(name = "author", nullable = false, length = 120)
    private String author;

    @Column(name = "note", nullable = false, columnDefinition = "text")
    private String note;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
