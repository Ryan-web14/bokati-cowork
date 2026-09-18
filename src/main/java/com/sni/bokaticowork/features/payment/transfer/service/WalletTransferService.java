package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.generator.uuid.TimeOrderedUuid;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.limit.service.WalletKycLevelResolver;
import com.sni.bokaticowork.features.payment.limit.service.WalletLimitService;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.service.WalletRiskFlagService;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.security.enums.WalletOperationType;
import com.sni.bokaticowork.features.payment.security.model.WalletTransactionConfirmation;
import com.sni.bokaticowork.features.payment.security.service.WalletConfirmationService;
import com.sni.bokaticowork.features.payment.security.service.WalletSecurityService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import com.sni.bokaticowork.features.payment.transfer.model.WalletBeneficiary;
import com.sni.bokaticowork.features.payment.transfer.model.WalletPaymentRequest;
import com.sni.bokaticowork.features.payment.transfer.model.WalletPaymentRequestStatus;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransferStatus;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletBeneficiaryRepository;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletPaymentRequestRepository;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletTransferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Le transfert entre abonnes.
 *
 * <p>Trois temps, et l'ordre est la regle : <b>simuler</b> ne touche a rien et dit ce qui se
 * passerait ; <b>initier</b> pose le transfert et demande le code ; <b>confirmer</b> execute. Entre
 * les deux derniers, rien ne peut changer · l'empreinte de la confirmation fige montant, devise et
 * destinataire.</p>
 *
 * <p>L'execution est une seule transaction de base : debit, frais, credit. Il n'y a pas d'etat
 * « en cours » parce qu'on est soit avant, soit apres · un transfert qu'on observerait entre les
 * deux n'existe pas. Chaque ecriture porte une cle d'idempotence derivee du numero de transfert :
 * un rejeu apres un commit partiel retrouve ses ecritures au lieu de les refaire.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletTransferService {

    private static final String SOURCE_TYPE = "WALLET_TRANSFER";

    private final WalletTransferRepository transferRepository;
    private final WalletBeneficiaryRepository beneficiaryRepository;
    private final WalletPaymentRequestRepository paymentRequestRepository;
    private final WalletAccountRepository walletRepository;
    private final WalletLedgerService ledgerService;
    private final WalletLimitService limitService;
    private final WalletKycLevelResolver kycLevelResolver;
    private final WalletSecurityService securityService;
    private final WalletConfirmationService confirmationService;
    private final WalletCounterpartyResolver counterpartyResolver;
    private final WalletDeviceService deviceService;
    private final WalletNotifier notifier;
    private final WalletRiskFlagService flagService;
    private final SequenceGeneratorFacade sequenceGenerator;

    /** Au-dela, un transfert depuis un appareil jamais vu est signale. */
    @Value("${bokati.wallet.risk.large-transfer-amount:100000}")
    private BigDecimal largeTransferAmount;

    /** Taux de frais preleve sur l'emetteur · zero par defaut, l'etablissement decide. */
    @Value("${bokati.wallet.transfer.fee-rate:0}")
    private BigDecimal feeRate;

    @Value("${bokati.wallet.transfer.fee-min:0}")
    private BigDecimal feeMin;

    @Value("${bokati.wallet.transfer.fee-max:0}")
    private BigDecimal feeMax;

    /** Portefeuille qui recoit les frais · sans lui, aucun frais n'est preleve. */
    @Value("${bokati.wallet.transfer.fee-wallet-number:}")
    private String feeWalletNumber;

    @Value("${bokati.wallet.transfer.confirmation-validity-minutes:5}")
    private int confirmationValidityMinutes;

    // -----------------------------------------------------------------------------------------
    // Contrats
    // -----------------------------------------------------------------------------------------

    public record TransferOrder(String counterparty, BigDecimal amount, String message, String paymentRequestNumber) {
    }

    /**
     * Ce qui se passerait.
     *
     * @param allowed        le transfert passerait tel quel
     * @param blockingReason pourquoi il ne passerait pas · nul si {@code allowed}
     * @param pinMissing     le titulaire doit d'abord creer son code
     * @param newDevice      l'appareil n'a jamais opere sur ce portefeuille
     */
    public record TransferPreview(
            String counterpartyWallet,
            String counterpartyName,
            BigDecimal amount,
            BigDecimal fee,
            BigDecimal totalDebit,
            BigDecimal balanceAfter,
            String currency,
            boolean allowed,
            String blockingReason,
            String upgradePath,
            boolean pinRequired,
            boolean pinMissing,
            boolean otpRequired,
            boolean knownBeneficiary,
            boolean newDevice
    ) {
    }

    public record InitiatedTransfer(WalletTransfer transfer, WalletTransactionConfirmation confirmation) {
    }

    // -----------------------------------------------------------------------------------------
    // 1. Simuler
    // -----------------------------------------------------------------------------------------

    /**
     * Dit ce qui se passerait, sans rien faire.
     *
     * <p>L'appareil n'est pas note ici : une simulation n'est pas une operation, et un titulaire qui
     * hesite sur un montant ne doit pas voir arriver un courriel « nouvel appareil ».</p>
     *
     * <p>Pas en lecture seule : resoudre le destinataire peut ouvrir son portefeuille s'il n'en a
     * pas encore, et c'est la seule ecriture que la simulation s'autorise.</p>
     */
    @Transactional
    public TransferPreview simulate(WalletAccount source, TransferOrder order, String deviceId) {
        BigDecimal amount = money(order.amount());
        WalletCounterpartyResolver.Counterparty counterparty =
                counterpartyResolver.resolve(order.counterparty(), source.getCurrency());
        WalletAccount target = counterparty.wallet();
        BigDecimal fee = fee(amount);
        BigDecimal total = amount.add(fee);

        String blocking = null;
        String upgrade = null;
        WalletLimitService.LimitVerdict limit = null;
        try {
            assertTransferable(source, target, amount);
            limit = limitService.checkTransfer(source, kycLevelResolver.levelOf(source), amount);
            if (!limit.allowed()) {
                blocking = limit.reason();
                upgrade = limit.upgradePath();
            } else if (source.getAvailableBalance().compareTo(total) < 0) {
                blocking = "Solde disponible insuffisant · il manque "
                        + plain(total.subtract(source.getAvailableBalance())) + " " + source.getCurrency();
            }
        } catch (BadRequestException | ConflictException ex) {
            blocking = ex.getMessage();
        }

        WalletSecurityService.SecurityVerdict security =
                securityService.evaluate(source, WalletOperationType.TRANSFER, amount);
        boolean known = beneficiaryRepository
                .findByOwnerWallet_IdAndBeneficiaryWallet_Id(source.getId(), target.getId()).isPresent()
                || transferRepository.countCompletedBetween(source.getId(), target.getId()) > 0;
        boolean newDevice = StringUtils.hasText(deviceId)
                && deviceService.list(source).stream().noneMatch(device -> device.getDeviceId().equals(deviceId.trim()));

        return new TransferPreview(
                target.getWalletNumber(), counterparty.displayName(),
                amount, fee, total, source.getAvailableBalance().subtract(total), source.getCurrency(),
                blocking == null, blocking, upgrade,
                security.pinRequired(), security.pinMissing(), security.otpRequired(),
                known, newDevice);
    }

    // -----------------------------------------------------------------------------------------
    // 2. Initier
    // -----------------------------------------------------------------------------------------

    /**
     * Pose le transfert et demande le code.
     *
     * <p>Les plafonds sont controles ici <em>et</em> a l'execution. Ici pour refuser tot, avec
     * l'explication ; a l'execution parce qu'un autre transfert a pu passer entre-temps et que
     * seule la verification sous verrou fait foi.</p>
     */
    @Transactional
    public InitiatedTransfer initiate(WalletAccount source, TransferOrder order,
                                      String initiatedBy, String ipAddress, String deviceId) {
        BigDecimal amount = money(order.amount());
        WalletCounterpartyResolver.Counterparty counterparty =
                counterpartyResolver.resolve(order.counterparty(), source.getCurrency());
        WalletAccount target = counterparty.wallet();

        assertTransferable(source, target, amount);
        limitService.assertAllowed(source, limitService.checkTransfer(source, kycLevelResolver.levelOf(source), amount));
        BigDecimal fee = fee(amount);
        if (source.getAvailableBalance().compareTo(amount.add(fee)) < 0) {
            throw new ConflictException("wallet", "solde disponible insuffisant");
        }

        WalletPaymentRequest request = null;
        if (StringUtils.hasText(order.paymentRequestNumber())) {
            request = openPaymentRequest(order.paymentRequestNumber(), source, target, amount);
        }

        WalletDeviceService.DeviceVerdict device = deviceService.touch(source, deviceId, ipAddress);
        if (device.firstUse() && largeTransferAmount != null && amount.compareTo(largeTransferAmount) >= 0) {
            // Le signal le plus simple et le plus fiable : un appareil jamais vu, un gros montant.
            flagService.raise(source, WalletRiskFlag.Type.NEW_DEVICE_LARGE_TRANSFER, WalletRiskFlag.Severity.HIGH,
                    plain(amount) + " " + source.getCurrency() + " vers " + target.getWalletNumber()
                            + " depuis l'appareil " + deviceId, deviceId);
        }

        WalletTransactionConfirmation confirmation = confirmationService.request(source,
                new WalletConfirmationService.OperationToConfirm(WalletOperationType.TRANSFER, amount,
                        source.getCurrency(), target.getWalletNumber(), counterparty.displayName()),
                ipAddress, deviceId);

        WalletTransfer transfer = transferRepository.save(WalletTransfer.builder()
                .transferNumber(sequenceGenerator.next("wallet_transfer"))
                .transferUuid(TimeOrderedUuid.next())
                .sourceWallet(source)
                .targetWallet(target)
                .amount(amount)
                .feeAmount(fee)
                .currency(source.getCurrency())
                .status(WalletTransferStatus.PENDING_CONFIRMATION)
                .message(trim(order.message()))
                .confirmationCode(confirmation.getConfirmationCode())
                .paymentRequestNumber(request == null ? null : request.getRequestNumber())
                .initiatedBy(initiatedBy)
                .ipAddress(ipAddress)
                .deviceId(trim(deviceId))
                .expiresAt(Instant.now().plus(Duration.ofMinutes(confirmationValidityMinutes)))
                .build());
        return new InitiatedTransfer(transfer, confirmation);
    }

    // -----------------------------------------------------------------------------------------
    // 3. Confirmer et executer
    // -----------------------------------------------------------------------------------------

    @Transactional
    public WalletTransfer confirm(WalletAccount source, String transferNumber, String pin) {
        WalletTransfer transfer = owned(source, transferNumber);
        if (transfer.getStatus() != WalletTransferStatus.PENDING_CONFIRMATION) {
            throw new BadRequestException("Ce transfert n'est plus en attente de confirmation");
        }
        if (transfer.getExpiresAt() != null && !Instant.now().isBefore(transfer.getExpiresAt())) {
            transfer.setStatus(WalletTransferStatus.CANCELLED);
            transfer.setFailureReason("Délai de confirmation dépassé");
            transferRepository.save(transfer);
            throw new BadRequestException("Le délai de confirmation est dépassé, recommencez le transfert");
        }

        WalletAccount target = transfer.getTargetWallet();
        // La confirmation recalcule l'empreinte sur ce qui va reellement s'executer · si le
        // transfert enregistre differait de ce qui a ete montre, elle refuserait.
        confirmationService.confirm(transfer.getConfirmationCode(),
                new WalletConfirmationService.OperationToConfirm(WalletOperationType.TRANSFER,
                        transfer.getAmount(), transfer.getCurrency(), target.getWalletNumber(), null),
                pin);

        try {
            execute(transfer);
        } catch (BadRequestException | ConflictException ex) {
            // L'echec est un fait a garder, pas une exception a laisser remonter nue : le titulaire
            // doit retrouver ce transfert dans son historique avec la raison.
            transfer.setStatus(WalletTransferStatus.FAILED);
            transfer.setFailureReason(ex.getMessage());
            transferRepository.save(transfer);
            throw ex;
        }
        return transfer;
    }

    /**
     * Les trois ecritures, sous les deux verrous.
     *
     * <p>Les deux comptes sont verrouilles <em>avant</em> la premiere ecriture, dans l'ordre de
     * leurs identifiants quel que soit le sens du transfert : deux transferts croises entre les memes
     * portefeuilles se serialisent alors au lieu de s'interbloquer. Les verrous que le grand livre
     * reprend ensuite sur chaque compte sont les memes, dans la meme transaction · ils ne coutent
     * rien.</p>
     *
     * <p>La re-verification des plafonds et du solde a lieu sous ces verrous. C'est celle-ci qui
     * fait foi : celle de l'initiation n'etait qu'une politesse pour refuser tot.</p>
     */
    private void execute(WalletTransfer transfer) {
        Long sourceId = transfer.getSourceWallet().getId();
        Long targetId = transfer.getTargetWallet().getId();
        Long first = Math.min(sourceId, targetId);
        Long second = Math.max(sourceId, targetId);
        WalletAccount firstLocked = walletRepository.findByIdForUpdate(first)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));
        WalletAccount secondLocked = walletRepository.findByIdForUpdate(second)
                .orElseThrow(() -> new ResourceNotFoundException("Portefeuille introuvable"));
        WalletAccount source = sourceId.equals(first) ? firstLocked : secondLocked;
        WalletAccount target = targetId.equals(first) ? firstLocked : secondLocked;

        assertTransferable(source, target, transfer.getAmount());
        // Sans signalement ici : les deux comptes sont verrouilles, et l'initiation a deja signale
        // une tentative au-dela du plafond s'il y en avait une.
        limitService.assertAllowed(limitService.checkTransfer(source,
                kycLevelResolver.levelOf(source), transfer.getAmount()));

        String number = transfer.getTransferNumber();
        String reference = "Transfert " + number
                + (StringUtils.hasText(transfer.getMessage()) ? " · " + transfer.getMessage() : "");

        WalletLedgerEntry debit = debitSource(source, transfer, reference, number);
        WalletLedgerEntry credit = creditTarget(target, transfer, reference, number);

        if (transfer.getFeeAmount() != null && transfer.getFeeAmount().signum() > 0) {
            WalletLedgerEntry fee = ledgerService.debit(source, transfer.getFeeAmount(), WalletEntryType.TRANSFER_FEE,
                    SOURCE_TYPE, number, "Frais de transfert " + number, transfer.getInitiatedBy(),
                    "WALLET_TRANSFER_FEE:" + number);
            transfer.setFeeEntryNumber(fee.getEntryNumber());
            feeWallet().ifPresent(house -> ledgerService.credit(house, transfer.getFeeAmount(),
                    WalletEntryType.TRANSFER_FEE, SOURCE_TYPE, number, "Frais de transfert " + number,
                    transfer.getInitiatedBy(), "WALLET_TRANSFER_FEE_IN:" + number));
        }

        transfer.setDebitEntryNumber(debit.getEntryNumber());
        transfer.setCreditEntryNumber(credit.getEntryNumber());
        transfer.setStatus(WalletTransferStatus.COMPLETED);
        transfer.setCompletedAt(Instant.now());
        transferRepository.save(transfer);

        rememberBeneficiary(source, target);
        settlePaymentRequest(transfer);

        WalletAccount freshSource = walletRepository.findById(source.getId()).orElse(source);
        WalletAccount freshTarget = walletRepository.findById(target.getId()).orElse(target);
        notifier.transferSent(freshSource, freshTarget, transfer.getAmount(), number);
        notifier.transferReceived(freshTarget, freshSource, transfer.getAmount(), number);
        log.info("Transfert {} · {} {} de {} vers {}", number, plain(transfer.getAmount()),
                transfer.getCurrency(), source.getWalletNumber(), target.getWalletNumber());
    }

    private WalletLedgerEntry debitSource(WalletAccount source, WalletTransfer transfer, String reference, String number) {
        return ledgerService.debit(source, transfer.getAmount(), WalletEntryType.TRANSFER_OUT,
                SOURCE_TYPE, number, reference, transfer.getInitiatedBy(), "WALLET_TRANSFER_OUT:" + number);
    }

    private WalletLedgerEntry creditTarget(WalletAccount target, WalletTransfer transfer, String reference, String number) {
        return ledgerService.credit(target, transfer.getAmount(), WalletEntryType.TRANSFER_IN,
                SOURCE_TYPE, number, reference, transfer.getInitiatedBy(), "WALLET_TRANSFER_IN:" + number);
    }

    // -----------------------------------------------------------------------------------------
    // Annulation, consultation, echeance
    // -----------------------------------------------------------------------------------------

    @Transactional
    public WalletTransfer cancel(WalletAccount source, String transferNumber) {
        WalletTransfer transfer = owned(source, transferNumber);
        if (transfer.getStatus() != WalletTransferStatus.PENDING_CONFIRMATION) {
            throw new BadRequestException("Seul un transfert en attente de confirmation peut être annulé");
        }
        transfer.setStatus(WalletTransferStatus.CANCELLED);
        transfer.setFailureReason("Annulé par le titulaire");
        return transferRepository.save(transfer);
    }

    @Transactional(readOnly = true)
    public WalletTransfer get(WalletAccount wallet, String transferNumber) {
        WalletTransfer transfer = transferRepository.findByTransferNumber(transferNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transfert introuvable"));
        boolean involved = transfer.getSourceWallet().getId().equals(wallet.getId())
                || transfer.getTargetWallet().getId().equals(wallet.getId());
        if (!involved) {
            throw new ResourceNotFoundException("Transfert introuvable");
        }
        return transfer;
    }

    /** Ferme les transferts jamais confirmes. Ils ne bloquent rien, mais ils encombrent l'historique. */
    @Transactional
    public int expireStale() {
        List<WalletTransfer> expired = transferRepository.findExpired(WalletTransferStatus.PENDING_CONFIRMATION, Instant.now());
        expired.forEach(transfer -> {
            transfer.setStatus(WalletTransferStatus.CANCELLED);
            transfer.setFailureReason("Jamais confirmé");
            transferRepository.save(transfer);
        });
        return expired.size();
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Ce qui rend un transfert impossible quel que soit le montant.
     *
     * <p>Un portefeuille verrouille par son titulaire ne peut pas envoyer, mais peut recevoir : c'est
     * le titulaire qui s'est protege, pas l'etablissement qui l'a sanctionne. Un portefeuille gele
     * ne fait ni l'un ni l'autre.</p>
     */
    private void assertTransferable(WalletAccount source, WalletAccount target, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Le montant doit être positif");
        }
        if (source.getId().equals(target.getId())) {
            throw new BadRequestException("Un portefeuille ne peut pas se transférer à lui-même");
        }
        if (!source.getCurrency().equalsIgnoreCase(target.getCurrency())) {
            throw new BadRequestException("Les deux portefeuilles n'ont pas la même devise");
        }
        if (source.getLockedByOwnerAt() != null) {
            throw new ConflictException("wallet", "votre portefeuille est verrouillé · déverrouillez-le pour envoyer");
        }
        if (!source.spendable()) {
            throw new ConflictException("wallet", "votre portefeuille ne peut pas émettre pour le moment");
        }
        if (!target.receivable()) {
            throw new ConflictException("wallet", "le portefeuille du destinataire ne peut pas recevoir pour le moment");
        }
    }

    private WalletPaymentRequest openPaymentRequest(String requestNumber, WalletAccount payer,
                                                    WalletAccount requester, BigDecimal amount) {
        WalletPaymentRequest request = paymentRequestRepository.findByRequestNumber(requestNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Demande de paiement introuvable"));
        if (!request.openAt(Instant.now())) {
            throw new BadRequestException("Cette demande de paiement n'est plus ouverte");
        }
        if (!request.getPayerWallet().getId().equals(payer.getId())
                || !request.getRequesterWallet().getId().equals(requester.getId())) {
            throw new BadRequestException("Cette demande de paiement ne correspond pas à ce transfert");
        }
        if (request.getAmount().compareTo(amount) != 0) {
            throw new BadRequestException("Le montant doit être celui de la demande · "
                    + plain(request.getAmount()) + " " + request.getCurrency());
        }
        return request;
    }

    private void settlePaymentRequest(WalletTransfer transfer) {
        if (!StringUtils.hasText(transfer.getPaymentRequestNumber())) {
            return;
        }
        paymentRequestRepository.findByRequestNumber(transfer.getPaymentRequestNumber()).ifPresent(request -> {
            request.setStatus(WalletPaymentRequestStatus.PAID);
            request.setTransferNumber(transfer.getTransferNumber());
            request.setResolvedAt(Instant.now());
            paymentRequestRepository.save(request);
        });
    }

    /** Un destinataire deja enregistre voit son compteur avancer · un inconnu n'est pas enregistre d'office. */
    private void rememberBeneficiary(WalletAccount source, WalletAccount target) {
        beneficiaryRepository.findByOwnerWallet_IdAndBeneficiaryWallet_Id(source.getId(), target.getId())
                .ifPresent(beneficiary -> {
                    beneficiary.setTransferCount((beneficiary.getTransferCount() == null ? 0 : beneficiary.getTransferCount()) + 1);
                    beneficiary.setLastUsedAt(Instant.now());
                    beneficiaryRepository.save(beneficiary);
                });
    }

    /**
     * Frais preleves sur l'emetteur.
     *
     * <p>Aucun frais sans portefeuille pour les recevoir : prelever de l'argent qui n'atterrit nulle
     * part creerait un ecart au rapprochement, et ce serait le rapprochement qui aurait raison.</p>
     */
    private BigDecimal fee(BigDecimal amount) {
        if (feeRate == null || feeRate.signum() <= 0 || feeWallet().isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal fee = amount.multiply(feeRate).setScale(4, RoundingMode.HALF_UP);
        if (feeMin != null && feeMin.signum() > 0 && fee.compareTo(feeMin) < 0) {
            fee = feeMin;
        }
        if (feeMax != null && feeMax.signum() > 0 && fee.compareTo(feeMax) > 0) {
            fee = feeMax;
        }
        return fee.setScale(4, RoundingMode.HALF_UP);
    }

    private Optional<WalletAccount> feeWallet() {
        return StringUtils.hasText(feeWalletNumber)
                ? walletRepository.findByWalletNumber(feeWalletNumber.trim())
                : Optional.empty();
    }

    private WalletTransfer owned(WalletAccount source, String transferNumber) {
        WalletTransfer transfer = transferRepository.findByTransferNumber(transferNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Transfert introuvable"));
        if (!transfer.getSourceWallet().getId().equals(source.getId())) {
            throw new ResourceNotFoundException("Transfert introuvable");
        }
        return transfer;
    }

    private BigDecimal money(BigDecimal amount) {
        if (amount == null) {
            throw new BadRequestException("Le montant est requis");
        }
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private String plain(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** Expose pour les tests · la configuration des frais n'a pas de setter naturel. */
    void configureFees(BigDecimal rate, BigDecimal min, BigDecimal max, String walletNumber) {
        this.feeRate = rate;
        this.feeMin = min;
        this.feeMax = max;
        this.feeWalletNumber = walletNumber;
    }

    /** Expose pour les tests. */
    void configureRisk(BigDecimal largeTransferAmount) {
        this.largeTransferAmount = largeTransferAmount;
    }

    /** Utile aux services voisins qui doivent lister les enregistres. */
    List<WalletBeneficiary> beneficiariesOf(WalletAccount wallet) {
        return beneficiaryRepository.findByOwnerWallet_IdOrderByAliasAsc(wallet.getId());
    }
}
