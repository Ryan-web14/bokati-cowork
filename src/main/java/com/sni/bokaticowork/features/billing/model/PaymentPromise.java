package com.sni.bokaticowork.features.billing.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
import java.time.LocalDate;

/**
 * Lot C12 : promesse de paiement d'un client sur une facture. Les relances attendent sa date ;
 * ensuite elle est tenue (le reste du a baisse du montant promis) ou rompue, et les relances
 * reprennent. Statuts : OPEN, KEPT, BROKEN, CANCELLED.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "billing_payment_promise")
public class PaymentPromise {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "promise_number", nullable = false, unique = true, length = 40)
    private String promiseNumber;

    @Column(name = "document_number", nullable = false, length = 80)
    private String documentNumber;

    @Column(name = "customer_type", length = 60)
    private String customerType;

    @Column(name = "customer_code", length = 120)
    private String customerCode;

    @Column(name = "promised_date", nullable = false)
    private LocalDate promisedDate;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_at_promise", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAtPromise;

    /** Canal par lequel la promesse a ete faite : PHONE, EMAIL, WHATSAPP, VISIT, PORTAL. */
    @Column(name = "channel", nullable = false, length = 20)
    private String channel;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_by", length = 180)
    private String createdBy;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
