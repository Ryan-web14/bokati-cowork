package com.sni.bokaticowork.features.company.membership.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.enums.InvoiceLinkMode;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Le rattachement d'un membre a une organisation · avec son role, ses dates et ses droits.
 *
 * <p>Un membre peut etre rattache a plusieurs organisations, une fois chacune. Ce lien ne remplace
 * pas {@code member.customer_id}, le payeur par defaut du membre ; il dit qui est qui dans
 * l'organisation et ce que chacun peut faire en son nom.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_member_link")
public class CustomerMemberLink {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    /** Expose comme {@code id} par l'API · l'identifiant genere depasse ce que lit JavaScript. */
    @org.hibernate.annotations.Generated
    @Column(name = "short_id", insertable = false, updatable = false)
    private Long shortId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MemberLinkRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberLinkStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "ended_reason", length = 500)
    private String endedReason;

    /** Vide : le mode par defaut de l'organisation s'applique. */
    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_link_mode", length = 20)
    private InvoiceLinkMode invoiceLinkMode;

    /** Peut signer les contrats au nom de l'organisation (tranche 6). */
    @Builder.Default
    @Column(name = "can_sign", nullable = false)
    private boolean canSign = false;

    /** Peut voir et retirer le courrier domicilie (tranche 6). */
    @Builder.Default
    @Column(name = "can_collect_mail", nullable = false)
    private boolean canCollectMail = false;

    @Column(name = "created_by", nullable = false, length = 150)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public boolean active() {
        return status == MemberLinkStatus.ACTIVE;
    }

    /** Le mode qui s'applique · celui du lien, sinon celui de l'organisation, sinon au choix. */
    public InvoiceLinkMode effectiveInvoiceLinkMode() {
        if (invoiceLinkMode != null) {
            return invoiceLinkMode;
        }
        InvoiceLinkMode companyDefault = customer == null ? null : customer.getDefaultInvoiceLinkMode();
        return companyDefault == null ? InvoiceLinkMode.ON_CHOICE : companyDefault;
    }
}
