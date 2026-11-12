package com.sni.bokaticowork.features.marketplace.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Une commande de la marketplace · d'abord le panier du membre, puis, validee, une facture a
 * regler, enfin une remise a l'accueil.
 *
 * <p>L'acheteur est toujours le membre ; le payeur est lui-meme ou l'organisation pour laquelle il
 * commande ({@link #billingCompanyCode}).</p>
 */
@Entity
@Table(name = "marketplace_order")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketplaceOrder {

    public enum Status {
        /** Le panier, modifiable · un seul par membre. */
        CART,
        /** Validee : prix figes, stock retenu, facture emise. */
        PENDING_PAYMENT,
        /** Reglee : le stock est sorti, la commande attend son retrait. */
        PAID,
        /** Remise a l'accueil. */
        FULFILLED,
        CANCELLED,
        /** Non reglee dans le delai · le stock est rendu, la facture annulee. */
        EXPIRED
    }

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @org.hibernate.annotations.Generated
    @Column(name = "short_id", insertable = false, updatable = false)
    private Long shortId;

    /** Attribue a la validation · un panier n'en a pas. */
    @Column(name = "order_number", length = 60)
    private String orderNumber;

    @Column(name = "buyer_member_code", nullable = false, length = 120)
    private String buyerMemberCode;

    @Column(name = "billing_company_code", length = 120)
    private String billingCompanyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "XAF";

    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "invoice_number", length = 120)
    private String invoiceNumber;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "placed_at")
    private Instant placedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    @Column(name = "fulfilled_by", length = 150)
    private String fulfilledBy;

    /** Qui a retire la commande au comptoir. */
    @Column(name = "collected_by", length = 200)
    private String collectedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 150)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "credit_note_number", length = 120)
    private String creditNoteNumber;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<MarketplaceOrderLine> lines = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
