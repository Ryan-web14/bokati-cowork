package com.sni.bokaticowork.features.payment.transfer.model;

import com.sni.bokaticowork.core.annotation.IdGeneration;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Une demande de paiement d'un abonne a un autre.
 *
 * <p>Elle ne deplace rien. Elle attend que le payeur la regle par un transfert ordinaire, avec son
 * code et ses plafonds · aucune demande ne peut contourner ce que le transfert exige. Le payeur peut
 * aussi la decliner, et le demandeur la retirer.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_payment_request")
public class WalletPaymentRequest {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "request_number", nullable = false, unique = true, length = 100)
    private String requestNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_wallet_id", nullable = false)
    private WalletAccount requesterWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_wallet_id", nullable = false)
    private WalletAccount payerWallet;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "reason", length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private WalletPaymentRequestStatus status = WalletPaymentRequestStatus.PENDING;

    @Column(name = "transfer_number", length = 100)
    private String transferNumber;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public boolean openAt(Instant moment) {
        return status == WalletPaymentRequestStatus.PENDING && moment.isBefore(expiresAt);
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
