package com.sni.bokaticowork.features.payment.transfer.dto;

import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.security.enums.WalletChallengeType;
import com.sni.bokaticowork.features.payment.transfer.model.WalletBeneficiary;
import com.sni.bokaticowork.features.payment.transfer.model.WalletDevice;
import com.sni.bokaticowork.features.payment.transfer.model.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.transfer.model.WalletPaymentRequestStatus;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus;
import com.sni.bokaticowork.features.payment.transfer.service.WalletTransferService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Les contrats des usages du portefeuille, cote client.
 *
 * <p>Rassembles ici parce qu'ils sont petits et vont ensemble. Ce qui est renvoye du destinataire
 * est volontairement mince · un nom d'affichage, pas ses coordonnees : chercher par numero doit
 * confirmer une personne qu'on connait, pas permettre d'en decouvrir une.</p>
 */
public final class WalletUsageDtos {

    private WalletUsageDtos() {
    }

    // -------------------------------------------------------------------------------------
    // Requetes
    // -------------------------------------------------------------------------------------

    public record TransferRequest(
            @NotBlank(message = "Indiquez le destinataire") String counterparty,
            @NotNull(message = "Le montant est requis") BigDecimal amount,
            String message,
            String paymentRequestNumber
    ) {
        public WalletTransferService.TransferOrder toOrder() {
            return new WalletTransferService.TransferOrder(counterparty, amount, message, paymentRequestNumber);
        }
    }

    public record PinRequest(@NotBlank(message = "Le code secret est requis") String pin) {
    }

    public record BeneficiaryRequest(
            @NotBlank(message = "Indiquez le destinataire") String counterparty,
            @NotBlank(message = "Donnez un nom à ce destinataire") String alias
    ) {
    }

    public record PaymentRequestRequest(
            @NotBlank(message = "Indiquez à qui adresser la demande") String payer,
            @NotNull(message = "Le montant est requis") BigDecimal amount,
            String reason
    ) {
    }

    public record TopUpRequest(
            @NotNull(message = "Le montant est requis") BigDecimal amount,
            @NotBlank(message = "Le numéro mobile money est requis") String phoneNumber,
            @NotNull(message = "Choisissez l'opérateur") CongoCorrespondent correspondent
    ) {
    }

    public record PreferencesRequest(BigDecimal lowBalanceThreshold, Boolean notifyOnCredit, Boolean notifyOnDebit) {
    }

    public record DeviceUpdateRequest(String label, Boolean trusted) {
    }

    // -------------------------------------------------------------------------------------
    // Reponses
    // -------------------------------------------------------------------------------------

    public record TransferView(
            String transferNumber,
            UUID transferUuid,
            String direction,
            String counterpartyWallet,
            /** Le nom de l'autre partie · un numero de portefeuille ne se reconnait pas. */
            String counterpartyName,
            BigDecimal amount,
            BigDecimal feeAmount,
            BigDecimal totalDebit,
            String currency,
            WalletTransferStatus status,
            String message,
            String paymentRequestNumber,
            String failureReason,
            Instant expiresAt,
            Instant completedAt,
            Instant createdAt
    ) {
        public static TransferView of(WalletTransfer transfer, Long viewerWalletId) {
            return of(transfer, viewerWalletId, null);
        }

        public static TransferView of(WalletTransfer transfer, Long viewerWalletId, String counterpartyName) {
            boolean outgoing = transfer.getSourceWallet().getId().equals(viewerWalletId);
            return new TransferView(
                    transfer.getTransferNumber(),
                    transfer.getTransferUuid(),
                    outgoing ? "OUT" : "IN",
                    outgoing ? transfer.getTargetWallet().getWalletNumber() : transfer.getSourceWallet().getWalletNumber(),
                    counterpartyName,
                    transfer.getAmount(),
                    outgoing ? transfer.getFeeAmount() : BigDecimal.ZERO,
                    outgoing ? transfer.totalDebit() : transfer.getAmount(),
                    transfer.getCurrency(),
                    transfer.getStatus(),
                    transfer.getMessage(),
                    transfer.getPaymentRequestNumber(),
                    transfer.getFailureReason(),
                    transfer.getExpiresAt(),
                    transfer.getCompletedAt(),
                    transfer.getCreatedAt());
        }
    }

    /** Ce que l'interface a besoin de savoir pour demander le code · rien du secret lui-meme. */
    public record InitiatedTransferView(
            TransferView transfer,
            String confirmationCode,
            WalletChallengeType challenge,
            Instant confirmationExpiresAt,
            int maxAttempts
    ) {
    }

    public record BeneficiaryView(Long id, String alias, String walletNumber,
                                  /** Le nom du destinataire · l'alias est celui que le titulaire lui a donne. */
                                  String name,
                                  Integer transferCount, Instant lastUsedAt, Instant createdAt) {
        public static BeneficiaryView of(WalletBeneficiary beneficiary) {
            return of(beneficiary, null);
        }

        public static BeneficiaryView of(WalletBeneficiary beneficiary, String name) {
            return new BeneficiaryView(beneficiary.getId(), beneficiary.getAlias(),
                    beneficiary.getBeneficiaryWallet().getWalletNumber(), name, beneficiary.getTransferCount(),
                    beneficiary.getLastUsedAt(), beneficiary.getCreatedAt());
        }
    }

    public record PaymentRequestView(
            String requestNumber,
            String direction,
            String counterpartyWallet,
            /** Le nom de l'autre partie · qui demande, ou a qui l'on demande. */
            String counterpartyName,
            BigDecimal amount,
            String currency,
            String reason,
            WalletPaymentRequestStatus status,
            String transferNumber,
            Instant expiresAt,
            Instant resolvedAt,
            Instant createdAt
    ) {
        public static PaymentRequestView of(WalletPaymentRequest request, Long viewerWalletId) {
            return of(request, viewerWalletId, null);
        }

        public static PaymentRequestView of(WalletPaymentRequest request, Long viewerWalletId, String counterpartyName) {
            boolean mine = request.getRequesterWallet().getId().equals(viewerWalletId);
            return new PaymentRequestView(
                    request.getRequestNumber(),
                    mine ? "SENT" : "RECEIVED",
                    mine ? request.getPayerWallet().getWalletNumber() : request.getRequesterWallet().getWalletNumber(),
                    counterpartyName,
                    request.getAmount(), request.getCurrency(), request.getReason(), request.getStatus(),
                    request.getTransferNumber(), request.getExpiresAt(), request.getResolvedAt(), request.getCreatedAt());
        }
    }

    public record DeviceView(Long id, String deviceId, String label, Instant firstSeenAt, Instant lastSeenAt,
                             String lastIpAddress, Integer useCount, Boolean trusted, Instant revokedAt) {
        public static DeviceView of(WalletDevice device) {
            return new DeviceView(device.getId(), device.getDeviceId(), device.getLabel(), device.getFirstSeenAt(),
                    device.getLastSeenAt(), device.getLastIpAddress(), device.getUseCount(), device.getTrusted(),
                    device.getRevokedAt());
        }
    }

    public record OwnerStateView(String walletNumber, boolean lockedByOwner, Instant lockedAt, boolean frozen,
                                 BigDecimal lowBalanceThreshold, Boolean notifyOnCredit, Boolean notifyOnDebit) {
        public static OwnerStateView of(com.sni.bokaticowork.features.payment.model.WalletAccount wallet) {
            return new OwnerStateView(wallet.getWalletNumber(), wallet.getLockedByOwnerAt() != null,
                    wallet.getLockedByOwnerAt(), wallet.getFrozenAt() != null, wallet.getLowBalanceThreshold(),
                    wallet.getNotifyOnCredit(), wallet.getNotifyOnDebit());
        }
    }
}
