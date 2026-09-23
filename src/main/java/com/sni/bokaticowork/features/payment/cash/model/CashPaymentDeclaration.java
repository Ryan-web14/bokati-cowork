package com.sni.bokaticowork.features.payment.cash.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.cash.enums.CashDeclarationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * « Je passerai payer en especes » · une annonce, pas un paiement.
 *
 * <p>Le mobile money et le portefeuille aboutissent seuls : quand la reponse arrive, l argent est
 * la. Les especes non · elles arrivent avec la personne. Entre le moment ou le client l annonce
 * depuis son espace et le moment ou la caisse compte les billets, il n y a rien d encaisse, et
 * c est exactement ce qu il fallait pouvoir representer.</p>
 *
 * <p>L annonce est adossee a une facture, qui est le denominateur commun : une reservation reglee
 * en direct passe elle aussi par une facture. Le numero de reservation est conserve a part, parce
 * qu une annonce qui expire doit rendre le creneau.</p>
 *
 * <p>Tant que l annonce est en attente, <b>la facture reste due</b>. Annoncer un paiement ne
 * l eteint pas · sinon une annonce jamais honoree effacerait une creance reelle.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "cash_payment_declaration")
public class CashPaymentDeclaration {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "declaration_number", nullable = false, unique = true, length = 60)
    private String declarationNumber;

    /** La facture a regler · l ancrage commun aux factures et aux reservations. */
    @Column(name = "document_number", nullable = false, length = 100)
    private String documentNumber;

    /** La reservation concernee, s il y en a une · c est elle qu on rend a l expiration. */
    @Column(name = "booking_number", length = 100)
    private String bookingNumber;

    @Column(name = "customer_type", nullable = false, length = 60)
    private String customerType;

    @Column(name = "customer_code", nullable = false, length = 120)
    private String customerCode;

    @Column(name = "customer_name", length = 250)
    private String customerName;

    /** Ce que le client annonce vouloir regler · la caisse peut encaisser autre chose. */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CashDeclarationStatus status;

    /** Ce que le client a voulu dire en passant · « je passe vers 14h ». */
    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "declared_at", nullable = false)
    private Instant declaredAt;

    @Column(name = "declared_by", length = 150)
    private String declaredBy;

    /** Au-dela, l annonce tombe · une reservation ne peut pas etre tenue indefiniment. */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by", length = 150)
    private String confirmedBy;

    /** Ce que la caisse a reellement compte · pas forcement ce qui etait annonce. */
    @Column(name = "confirmed_amount", precision = 19, scale = 4)
    private BigDecimal confirmedAmount;

    @Column(name = "cash_session_number", length = 100)
    private String cashSessionNumber;

    @Column(name = "transaction_number", length = 100)
    private String transactionNumber;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by", length = 150)
    private String closedBy;

    @Column(name = "close_reason", length = 500)
    private String closeReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** L annonce attend encore l argent. */
    public boolean open() {
        return status != null && status.open();
    }

    /** Le delai est passe · personne ne s est presente. */
    public boolean overdue(Instant now) {
        return open() && expiresAt != null && expiresAt.isBefore(now);
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (declaredAt == null) {
            declaredAt = createdAt;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
