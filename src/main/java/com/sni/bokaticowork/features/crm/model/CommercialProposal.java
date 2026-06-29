package com.sni.bokaticowork.features.crm.model;

import com.sni.bokaticowork.features.crm.enums.ProposalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "crm_commercial_proposal")
public class CommercialProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_number", nullable = false, unique = true, length = 80)
    private String proposalNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @Column(name = "title", nullable = false, length = 300)
    private String title;

    @Column(name = "recipient_type", length = 60)
    private String recipientType;

    @Column(name = "recipient_code", length = 120)
    private String recipientCode;

    @Column(name = "recipient_name", length = 200)
    private String recipientName;

    @Column(name = "recipient_email", length = 200)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private ProposalStatus status = ProposalStatus.DRAFT;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "subtotal_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal subtotalAmount = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "internal_notes", columnDefinition = "text")
    private String internalNotes;

    @Column(name = "converted_invoice_number", length = 120)
    private String convertedInvoiceNumber;

    @Column(name = "converted_contract_code", length = 120)
    private String convertedContractCode;

    @Column(name = "converted_subscription_number", length = 120)
    private String convertedSubscriptionNumber;

    @Column(name = "converted_pass_number", length = 120)
    private String convertedPassNumber;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "viewed_at")
    private Instant viewedAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "text")
    private String rejectionReason;

    @Column(name = "document_code", length = 120)
    private String documentCode;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "assigned_to")
    private Long assignedTo;

    @Builder.Default
    @OneToMany(mappedBy = "proposal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProposalLineItem> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    public void preUpdate() { updatedAt = Instant.now(); }
}
