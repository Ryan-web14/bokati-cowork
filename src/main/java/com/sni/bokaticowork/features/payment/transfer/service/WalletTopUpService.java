package com.sni.bokaticowork.features.payment.transfer.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.dto.request.CreatePaymentIntentRequest;
import com.sni.bokaticowork.features.payment.dto.request.InitiateMobileMoneyDepositRequest;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentIntentResponse;
import com.sni.bokaticowork.features.payment.enums.PaymentTransactionStatus;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.limit.service.WalletKycLevelResolver;
import com.sni.bokaticowork.features.payment.limit.service.WalletLimitService;
import com.sni.bokaticowork.features.payment.model.PaymentIntent;
import com.sni.bokaticowork.features.payment.model.PaymentTransaction;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.provider.pawaypay.CongoCorrespondent;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.PaymentService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Le rechargement a l'initiative du titulaire, par mobile money.
 *
 * <p>Deux temps separes par un rappel de l'operateur. <b>Initier</b> cree une intention de paiement
 * marquee {@code WALLET_TOPUP} et lance le depot ; <b>regler</b> credite le portefeuille quand la
 * transaction est confirmee, depuis le flux asynchrone des paiements · donc rejouable, et
 * idempotent par la cle derivee du numero de transaction.</p>
 *
 * <p>Un rechargement n'est pas une vente. L'argent recu est une dette envers le titulaire, pas un
 * chiffre d'affaires : la facturation automatique l'ignore, et c'est ici que la distinction est
 * portee, par la nature de la source.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletTopUpService {

    public static final String SOURCE_TYPE = "WALLET_TOPUP";

    private final PaymentService paymentService;
    private final WalletAccountRepository walletRepository;
    private final WalletLedgerService ledgerService;
    private final WalletLimitService limitService;
    private final WalletKycLevelResolver kycLevelResolver;
    private final WalletNotifier notifier;

    public record TopUpOrder(BigDecimal amount, String phoneNumber, CongoCorrespondent correspondent) {
    }

    /**
     * Lance le depot.
     *
     * <p>Les plafonds sont controles avant d'engager l'operateur : refuser apres que l'argent a
     * quitte le telephone du titulaire obligerait a rembourser, et un remboursement mobile money
     * n'est ni instantane ni gratuit.</p>
     */
    @Transactional
    public MobileMoneyDepositResponse initiate(WalletAccount wallet, TopUpOrder order, String initiatedBy) {
        if (order.amount() == null || order.amount().signum() <= 0) {
            throw new BadRequestException("Le montant doit être positif");
        }
        if (order.correspondent() == null) {
            throw new BadRequestException("Choisissez l'opérateur mobile money");
        }
        if (!wallet.receivable()) {
            throw new ConflictException("wallet", "ce portefeuille ne peut pas recevoir pour le moment");
        }
        BigDecimal amount = order.amount().setScale(4, RoundingMode.HALF_UP);
        limitService.assertAllowed(limitService.checkTopUp(wallet, kycLevelResolver.levelOf(wallet), amount));

        PaymentIntentResponse intent = paymentService.createIntent(new CreatePaymentIntentRequest(
                wallet.getOwnerType(),
                wallet.getOwnerCode(),
                amount,
                wallet.getCurrency(),
                SOURCE_TYPE,
                SOURCE_TYPE,
                wallet.getWalletNumber(),
                "WALLET_TOPUP:" + wallet.getWalletNumber() + ":" + UUID.randomUUID(),
                Instant.now().plus(Duration.ofMinutes(30)),
                null));

        return paymentService.initiateMobileMoneyDeposit(intent.intentNumber(),
                new InitiateMobileMoneyDepositRequest(intent.intentNumber(), order.phoneNumber(),
                        order.correspondent(), amount, initiatedBy, null));
    }

    /** Est-ce un rechargement · le flux des paiements demande avant d'appeler {@link #settle}. */
    public boolean isTopUp(PaymentIntent intent) {
        return intent != null && SOURCE_TYPE.equalsIgnoreCase(intent.getSourceType());
    }

    /**
     * Credite le portefeuille une fois l'argent recu.
     *
     * <p>Idempotent : un rappel d'operateur rejoue, ou un retraitement de la boite de sortie,
     * retrouve l'ecriture d'origine par sa cle au lieu de crediter deux fois.</p>
     */
    @Transactional
    public void settle(PaymentTransaction transaction) {
        PaymentIntent intent = transaction.getPaymentIntent();
        if (!isTopUp(intent) || transaction.getStatus() != PaymentTransactionStatus.SUCCEEDED) {
            return;
        }
        WalletAccount wallet = walletRepository.findByWalletNumber(intent.getSourceCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Rechargement " + transaction.getTransactionNumber() + " · portefeuille "
                                + intent.getSourceCode() + " introuvable"));

        ledgerService.credit(wallet, transaction.getAmount(), WalletEntryType.TOPUP,
                SOURCE_TYPE, transaction.getTransactionNumber(),
                "Rechargement " + transaction.getTransactionNumber(),
                "SYSTEM_TOPUP", "WALLET_TOPUP:" + transaction.getTransactionNumber());

        WalletAccount fresh = walletRepository.findById(wallet.getId()).orElse(wallet);
        notifier.topUpCompleted(fresh, transaction.getAmount(), transaction.getTransactionNumber());
        log.info("Rechargement {} · {} {} sur {}", transaction.getTransactionNumber(),
                transaction.getAmount().stripTrailingZeros().toPlainString(), fresh.getCurrency(), fresh.getWalletNumber());
    }
}
