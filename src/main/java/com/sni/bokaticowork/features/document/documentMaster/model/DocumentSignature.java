package com.sni.bokaticowork.features.document.documentMaster.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSignatureStatus;
import jakarta.persistence.*;
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
@Table(name = "document_signature")
public class DocumentSignature {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", foreignKey = @ForeignKey(name = "fk_document"), nullable = false)
    private Document document;

    @Column(name = "signer_type", nullable = false, length = 30)
    private String signerType;

    @Column(name = "signer_id", nullable = false)
    private Long signerId;

    @Column(name = "signature_status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentSignatureStatus signatureStatus;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "signer_name")
    private String signerName;

    @Column(name = "signer_email")
    private String signerEmail;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "signature_data")
    private String signatureData;

    @Column(name = "signature_path")
    private String signaturePath;
}
