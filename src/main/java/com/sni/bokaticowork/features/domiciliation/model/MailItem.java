package com.sni.bokaticowork.features.domiciliation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un pli recu pour un domicilie.
 *
 * <p>Le recommande et les actes demandent une rigueur particuliere : notification immediate, et
 * remise contre identite et signature. La base l'exige pour ces types · une remise sans identite du
 * porteur n'est pas une remise. Le journal des evenements, lui, fait la preuve, et ne se reecrit
 * pas.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "mail_item")
public class MailItem {

    public enum Type {
        LETTER, REGISTERED_LETTER, PARCEL, ADMINISTRATIVE, LEGAL_NOTICE, OTHER;

        /** Ce qui se remet contre identite · un simple pli ne l'exige pas. */
        public boolean requiresIdentityOnCollection() {
            return this == REGISTERED_LETTER || this == ADMINISTRATIVE || this == LEGAL_NOTICE;
        }
    }

    public enum Status {
        RECEIVED, NOTIFIED, SCANNED, COLLECTED, FORWARDED, RETURNED, DESTROYED;

        public boolean closed() {
            return this == COLLECTED || this == FORWARDED || this == RETURNED || this == DESTROYED;
        }
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "item_number", nullable = false, unique = true, length = 100)
    private String itemNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domiciliation_contract_id", nullable = false)
    private DomiciliationContract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "mail_type", nullable = false, length = 30)
    private Type mailType;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_reference", length = 120)
    private String senderReference;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "received_by", nullable = false, length = 120)
    private String receivedBy;

    @Column(name = "weight_grams")
    private Integer weightGrams;

    @Column(name = "dimensions", length = 60)
    private String dimensions;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private Status status = Status.RECEIVED;

    @Column(name = "scan_document_code", length = 120)
    private String scanDocumentCode;

    @Column(name = "notified_at")
    private Instant notifiedAt;

    @Column(name = "notification_channel", length = 30)
    private String notificationChannel;

    @Column(name = "collected_at")
    private Instant collectedAt;

    @Column(name = "collected_by")
    private String collectedBy;

    @Column(name = "collector_id_document", length = 120)
    private String collectorIdDocument;

    @Column(name = "collector_signature_url", length = 500)
    private String collectorSignatureUrl;

    @Column(name = "handed_over_by", length = 120)
    private String handedOverBy;

    @Column(name = "forwarded_at")
    private Instant forwardedAt;

    @Column(name = "forwarding_tracking_number", length = 120)
    private String forwardingTrackingNumber;

    @Column(name = "forwarding_cost", precision = 19, scale = 4)
    private BigDecimal forwardingCost;

    /** Au-dela, relance puis destruction ou retour. */
    @Column(name = "storage_deadline")
    private LocalDate storageDeadline;

    @Column(name = "billable", nullable = false)
    @Builder.Default
    private Boolean billable = Boolean.FALSE;

    @Column(name = "billed_amount", precision = 19, scale = 4)
    private BigDecimal billedAmount;

    @Column(name = "usage_number", length = 100)
    private String usageNumber;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        receivedAt = receivedAt == null ? createdAt : receivedAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
