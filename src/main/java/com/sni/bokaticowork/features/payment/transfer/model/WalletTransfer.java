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
import java.util.UUID;

/**
 * Un transfert entre deux abonnes.
 *
 * <p>Deux ecritures au grand livre, une par portefeuille, mais un seul fait : c'est lui qui porte
 * le statut, la confirmation, la raison d'un echec. Les ecritures ne sont que ses traces, et
 * chacune est referencee ici pour qu'on retrouve le fait depuis la trace.</p>
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Entity
@Table(name = "wallet_transfer")
public class WalletTransfer {

    @Id
    @IdGeneration
    @Column(name = "id")
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 100)
    private String transferNumber;

    @Column(name = "transfer_uuid", nullable = false, unique = true)
    private UUID transferUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_wallet_id", nullable = false)
    private WalletAccount sourceWallet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_wallet_id", nullable = false)
    private WalletAccount targetWallet;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private WalletTransferStatus status = WalletTransferStatus.PENDING_CONFIRMATION;

    @Column(name = "message", length = 255)
    private String message;

    @Column(name = "confirmation_code", length = 100)
    private String confirmationCode;

    /** Demande de paiement que ce transfert regle, s'il y en a une. */
    @Column(name = "payment_request_number", length = 100)
    private String paymentRequestNumber;

    @Column(name = "debit_entry_number", length = 100)
    private String debitEntryNumber;

    @Column(name = "credit_entry_number", length = 100)
    private String creditEntryNumber;

    @Column(name = "fee_entry_number", length = 100)
    private String feeEntryNumber;

    @Column(name = "initiated_by", length = 120)
    private String initiatedBy;

    @Column(name = "ip_address", length = 60)
    private String ipAddress;

    @Column(name = "device_id", length = 120)
    private String deviceId;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Ce qui sort reellement du portefeuille emetteur. */
    public BigDecimal totalDebit() {
        return amount.add(feeAmount == null ? BigDecimal.ZERO : feeAmount);
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
