package com.sni.bokaticowork.features.document.kyc.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "kyc_case_note")
public class KycCaseNote {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kyc_case_id", nullable = false, foreignKey = @ForeignKey(name = "fk_kyc_case_note_case"))
    private KycCase kycCase;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(name = "internal", nullable = false)
    private Boolean internal = Boolean.TRUE;

    @PrePersist
    public void prePersist() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
        internal = internal == null ? Boolean.TRUE : internal;
    }
}
