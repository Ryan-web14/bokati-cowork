package com.sni.bokaticowork.features.domiciliation.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Le contrat de domiciliation.
 *
 * <p>Trois regles le structurent, et elles sont tenues par la base autant que par le code. La
 * qualite fiscale de l'adresse se <b>deduit</b> de l'engagement · douze mois au moins · et ne se
 * saisit jamais. L'attestation fiscale n'existe que sur un contrat eligible. Et le contrat n'est
 * pleinement opposable qu'une fois enregistre et timbre : l'enregistrement est une etape du cycle
 * de vie, pas une piece jointe.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "domiciliation_contract")
public class DomiciliationContract {

    public enum Status {
        DRAFT, PENDING_DOCUMENTS, PENDING_SIGNATURE, PENDING_REGISTRATION, ACTIVE, SUSPENDED, TERMINATED, EXPIRED;

        public boolean live() {
            return this == ACTIVE || this == SUSPENDED;
        }

        public boolean ended() {
            return this == TERMINATED || this == EXPIRED;
        }
    }

    public enum CertificateScope {
        COMMERCIAL, FISCAL
    }

    public enum MailForwardingMode {
        HOLD, FORWARD, SCAN_AND_FORWARD, SCAN_ONLY
    }

    public static final int FISCAL_COMMITMENT_MONTHS = 12;

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "contract_number", nullable = false, unique = true, length = 100)
    private String contractNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_service_id", nullable = false)
    private SubscriptionService subscriptionService;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "business_entity_id")
    private BusinessEntity businessEntity;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "legal_form", length = 100)
    private String legalForm;

    @Column(name = "registration_number", length = 120)
    private String registrationNumber;

    @Column(name = "tax_number", length = 120)
    private String taxNumber;

    @Column(name = "legal_representative_type", length = 40)
    private String legalRepresentativeType;

    @Column(name = "legal_representative_code", length = 120)
    private String legalRepresentativeCode;

    @Column(name = "legal_representative_name")
    private String legalRepresentativeName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_address_id", nullable = false)
    private DomiciliationAddress assignedAddress;

    @Column(name = "suite_number", length = 40)
    private String suiteNumber;

    // -- Engagement et qualite de l'adresse ---------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "commitment_months", nullable = false)
    private Integer commitmentMonths;

    /** Deduit · vrai seulement si l'engagement est de douze mois et l'adresse le permet. */
    @Column(name = "fiscal_address_eligible", nullable = false)
    @Builder.Default
    private Boolean fiscalAddressEligible = Boolean.FALSE;

    @Column(name = "fiscal_address_granted_at")
    private Instant fiscalAddressGrantedAt;

    @Column(name = "fiscal_address_revoked_at")
    private Instant fiscalAddressRevokedAt;

    @Column(name = "fiscal_address_revocation_reason", length = 500)
    private String fiscalAddressRevocationReason;

    // -- Cycle de vie ---------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private Status status = Status.DRAFT;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "notice_period_days", nullable = false)
    @Builder.Default
    private Integer noticePeriodDays = 30;

    // -- Pieces ---------------------------------------------------------------------------------

    @Column(name = "contract_document_code", length = 120)
    private String contractDocumentCode;

    @Column(name = "signed_document_code", length = 120)
    private String signedDocumentCode;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "certificate_document_code", length = 120)
    private String certificateDocumentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_scope", length = 20)
    private CertificateScope certificateScope;

    @Column(name = "certificate_issued_at")
    private Instant certificateIssuedAt;

    @Column(name = "certificate_valid_until")
    private LocalDate certificateValidUntil;

    @Column(name = "certificate_revoked_at")
    private Instant certificateRevokedAt;

    @Column(name = "certificate_revocation_reason", length = 500)
    private String certificateRevocationReason;

    // -- Courrier -------------------------------------------------------------------------------

    @Enumerated(EnumType.STRING)
    @Column(name = "mail_forwarding_mode", nullable = false, length = 30)
    @Builder.Default
    private MailForwardingMode mailForwardingMode = MailForwardingMode.HOLD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forwarding_address_id")
    private Address forwardingAddress;

    @Column(name = "forwarding_frequency", length = 30)
    private String forwardingFrequency;

    // -- Fin ------------------------------------------------------------------------------------

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "termination_reason", length = 500)
    private String terminationReason;

    @Column(name = "termination_notified_at")
    private Instant terminationNotifiedAt;

    @Column(name = "administration_notified_at")
    private Instant administrationNotifiedAt;

    /** Conservation des pieces · posee a la fin du contrat, jamais raccourcie. */
    @Column(name = "retain_documents_until")
    private LocalDate retainDocumentsUntil;

    @Column(name = "created_by", length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean certificateInForce() {
        return certificateDocumentCode != null && certificateRevokedAt == null
                && (certificateValidUntil == null || !LocalDate.now().isAfter(certificateValidUntil));
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
