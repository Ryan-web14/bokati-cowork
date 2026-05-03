package com.sni.bokaticowork.features.document.kyc.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "kyc_document_ocr_result")
public class KycDocumentOcrResult {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kyc_document_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_kyc_ocr_document"))
    private KycDocument kycDocument;

    @Column(name = "extracted_first_name")
    private String extractedFirstName;

    @Column(name = "extracted_last_name")
    private String extractedLastName;

    @Column(name = "extracted_date_of_birth")
    private LocalDate extractedDateOfBirth;

    @Column(name = "extracted_expiry_date")
    private LocalDate extractedExpiryDate;

    @Column(name = "extracted_document_number")
    private String extractedDocumentNumber;

    @Column(name = "extracted_nationality")
    private String extractedNationality;

    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(name = "raw_ocr_json", columnDefinition = "TEXT")
    private String rawOcrJson;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    public void prePersist() {
        processedAt = processedAt == null ? Instant.now() : processedAt;
    }
}
