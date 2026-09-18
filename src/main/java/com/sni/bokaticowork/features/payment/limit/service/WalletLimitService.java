package com.sni.bokaticowork.features.payment.limit.service;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.limit.model.WalletLimitPolicy;
import com.sni.bokaticowork.features.payment.limit.repository.WalletLimitPolicyRepository;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Ce qu'un portefeuille a le droit de faire aujourd'hui.
 *
 * <p>Les plafonds sont adosses au niveau de verification, et ce n'est pas une formalite : c'est ce
 * qui donne au client une raison de completer son dossier. Un plafond qui se leve quand on fournit
 * une piece est une invitation ; un refus sans explication est un mur.</p>
 *
 * <p>C'est pourquoi un refus dit <b>toujours</b> ce qu'il faudrait pour passer. « Plafond
 * depasse » n'apprend rien ; « au-dela de 50 000 F par operation a votre niveau de verification »
 * dit au titulaire ce qu'il peut faire.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletLimitService {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");

    private final WalletLimitPolicyRepository policyRepository;
    private final WalletLedgerEntryRepository ledgerRepository;

    /**
     * Verdict d'un controle de plafond.
     *
     * @param upgradePath ce qu'il faudrait pour lever la limite · nul si elle est absolue
     */
    public record LimitVerdict(boolean allowed, String reason, String upgradePath) {

        static LimitVerdict ok() {
            return new LimitVerdict(true, null, null);
        }
    }

    /**
     * Politique applicable · celle du niveau atteint, ou du plus proche en dessous.
     *
     * <p>Une politique nommee sur le portefeuille l'emporte : c'est ainsi qu'on accorde une
     * derogation a un client particulier sans toucher aux paliers.</p>
     */
    @Transactional(readOnly = true)
    public Optional<WalletLimitPolicy> policyFor(WalletAccount wallet, int kycLevel) {
        if (StringUtils.hasText(wallet.getLimitPolicyCode())) {
            Optional<WalletLimitPolicy> named = policyRepository.findByCode(wallet.getLimitPolicyCode());
            if (named.isPresent()) {
                return named;
            }
        }
        return policyRepository.findForKycLevel(Math.max(1, kycLevel));
    }

    // -----------------------------------------------------------------------------------------

    /** Le portefeuille peut-il recevoir ce montant. */
    @Transactional(readOnly = true)
    public LimitVerdict checkTopUp(WalletAccount wallet, int kycLevel, BigDecimal amount) {
        WalletLimitPolicy policy = policyFor(wallet, kycLevel).orElse(null);
        if (policy == null || amount == null) {
            return LimitVerdict.ok();
        }

        LimitVerdict single = ceiling(amount, policy.getMaxSingleTopup(),
                "par rechargement", policy, kycLevel);
        if (!single.allowed()) {
            return single;
        }

        LimitVerdict balance = ceiling(
                nonNull(wallet.getLedgerBalance()).add(amount), policy.getMaxBalance(),
                "de solde", policy, kycLevel);
        if (!balance.allowed()) {
            return balance;
        }

        LimitVerdict daily = ceiling(
                consumed(wallet, WalletEntryType.TOPUP, startOfDay()).add(amount),
                policy.getMaxDailyTopup(), "de rechargement par jour", policy, kycLevel);
        if (!daily.allowed()) {
            return daily;
        }

        return ceiling(
                consumed(wallet, WalletEntryType.TOPUP, startOfMonth()).add(amount),
                policy.getMaxMonthlyTopup(), "de rechargement par mois", policy, kycLevel);
    }

    /** Le portefeuille peut-il envoyer ce montant. */
    @Transactional(readOnly = true)
    public LimitVerdict checkTransfer(WalletAccount wallet, int kycLevel, BigDecimal amount) {
        WalletLimitPolicy policy = policyFor(wallet, kycLevel).orElse(null);
        if (policy == null || amount == null) {
            return LimitVerdict.ok();
        }

        LimitVerdict single = ceiling(amount, policy.getMaxSingleTransfer(),
                "par transfert", policy, kycLevel);
        if (!single.allowed()) {
            return single;
        }

        LimitVerdict daily = ceiling(
                consumed(wallet, WalletEntryType.TRANSFER_OUT, startOfDay()).add(amount),
                policy.getMaxDailyTransfer(), "de transfert par jour", policy, kycLevel);
        if (!daily.allowed()) {
            return daily;
        }

        LimitVerdict monthly = ceiling(
                consumed(wallet, WalletEntryType.TRANSFER_OUT, startOfMonth()).add(amount),
                policy.getMaxMonthlyTransfer(), "de transfert par mois", policy, kycLevel);
        if (!monthly.allowed()) {
            return monthly;
        }

        return checkOperationCount(wallet, policy, kycLevel);
    }

    /**
     * Ce qui reste, et jusqu'ou.
     *
     * <p>Un titulaire a qui l'on refuse une operation veut savoir deux choses : combien il lui
     * restait, et quand cela se reouvre. Renvoyer la consommation en meme temps que le plafond
     * permet a l'interface de le dire avant le refus plutot qu'apres.</p>
     *
     * @param remaining nul quand le plafond correspondant est absent · « pas de limite », et non zero
     */
    public record LimitSnapshot(
            String policyCode,
            String policyName,
            int kycLevel,
            String currency,
            BigDecimal maxSingleTransfer,
            BigDecimal dailyTransferUsed,
            BigDecimal maxDailyTransfer,
            BigDecimal remainingDailyTransfer,
            BigDecimal monthlyTransferUsed,
            BigDecimal maxMonthlyTransfer,
            BigDecimal remainingMonthlyTransfer,
            BigDecimal dailyTopUpUsed,
            BigDecimal maxDailyTopUp,
            BigDecimal remainingDailyTopUp,
            BigDecimal maxBalance,
            Integer maxDailyOperations,
            long dailyOperationsUsed,
            String upgradePath
    ) {
    }

    /** Etat des plafonds d'un portefeuille, consommation comprise. */
    @Transactional(readOnly = true)
    public LimitSnapshot snapshot(WalletAccount wallet, int kycLevel) {
        WalletLimitPolicy policy = policyFor(wallet, kycLevel).orElse(null);
        if (policy == null) {
            // Aucun palier configure · le portefeuille n'est borne par rien, et le dire est plus
            // honnete que de renvoyer des zeros qui se liraient comme des plafonds atteints.
            return new LimitSnapshot(null, null, kycLevel, wallet.getCurrency(),
                    null, BigDecimal.ZERO, null, null, BigDecimal.ZERO, null, null,
                    BigDecimal.ZERO, null, null, null, null, 0L, null);
        }

        BigDecimal dailyTransfer = consumed(wallet, WalletEntryType.TRANSFER_OUT, startOfDay());
        BigDecimal monthlyTransfer = consumed(wallet, WalletEntryType.TRANSFER_OUT, startOfMonth());
        BigDecimal dailyTopUp = consumed(wallet, WalletEntryType.TOPUP, startOfDay());

        return new LimitSnapshot(
                policy.getCode(),
                policy.getName(),
                policy.getKycLevel(),
                policy.getCurrency(),
                policy.getMaxSingleTransfer(),
                dailyTransfer, policy.getMaxDailyTransfer(), remaining(policy.getMaxDailyTransfer(), dailyTransfer),
                monthlyTransfer, policy.getMaxMonthlyTransfer(), remaining(policy.getMaxMonthlyTransfer(), monthlyTransfer),
                dailyTopUp, policy.getMaxDailyTopup(), remaining(policy.getMaxDailyTopup(), dailyTopUp),
                policy.getMaxBalance(),
                policy.getMaxDailyOperations(),
                ledgerRepository.countDebitsSince(wallet.getId(), startOfDay()),
                upgradePath(kycLevel));
    }

    /** Nul quand il n'y a pas de plafond · jamais negatif quand il est deja depasse. */
    private BigDecimal remaining(BigDecimal ceiling, BigDecimal used) {
        if (ceiling == null) {
            return null;
        }
        BigDecimal left = ceiling.subtract(used);
        return left.signum() < 0 ? BigDecimal.ZERO : left;
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Compare a un plafond, et redige le refus.
     *
     * <p>Un plafond nul signifie « pas de limite », et c'est different de zero. Les confondre
     * bloquerait tout au niveau le plus eleve, qui est precisement celui ou l'on veut laisser
     * passer.</p>
     */
    private LimitVerdict ceiling(BigDecimal candidate, BigDecimal ceiling, String label,
                                 WalletLimitPolicy policy, int kycLevel) {
        if (ceiling == null || candidate == null || candidate.compareTo(ceiling) <= 0) {
            return LimitVerdict.ok();
        }
        return new LimitVerdict(false,
                "Plafond " + label + " atteint · " + format(ceiling) + " " + policy.getCurrency()
                        + " à votre niveau de vérification",
                upgradePath(kycLevel));
    }

    private LimitVerdict checkOperationCount(WalletAccount wallet, WalletLimitPolicy policy, int kycLevel) {
        if (policy.getMaxDailyOperations() == null) {
            return LimitVerdict.ok();
        }
        long done = ledgerRepository.countDebitsSince(wallet.getId(), startOfDay());
        if (done < policy.getMaxDailyOperations()) {
            return LimitVerdict.ok();
        }
        return new LimitVerdict(false,
                "Nombre d'opérations quotidiennes atteint · " + policy.getMaxDailyOperations() + " par jour",
                upgradePath(kycLevel));
    }

    /**
     * Ce qu'il faudrait pour lever la limite.
     *
     * <p>Sans cette phrase, le titulaire ne sait pas s'il doit attendre demain, fournir une piece,
     * ou renoncer. Les trois appellent des gestes tres differents.</p>
     */
    private String upgradePath(int kycLevel) {
        return policyRepository.findForKycLevel(kycLevel + 1)
                .filter(next -> next.getKycLevel() > kycLevel)
                .map(next -> "Complétez votre dossier de vérification pour accéder au niveau « "
                        + next.getName() + " »")
                .orElse(null);
    }

    private BigDecimal consumed(WalletAccount wallet, WalletEntryType entryType, Instant from) {
        BigDecimal sum = ledgerRepository.sumSince(wallet.getId(), entryType.name(), from);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private Instant startOfDay() {
        return LocalDate.now(APP_ZONE).atStartOfDay(APP_ZONE).toInstant();
    }

    private Instant startOfMonth() {
        return LocalDate.now(APP_ZONE).withDayOfMonth(1).atStartOfDay(APP_ZONE).toInstant();
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String format(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    /** Leve l'exception correspondant a un refus · les appelants ne redigent pas le message. */
    public void assertAllowed(LimitVerdict verdict) {
        if (verdict.allowed()) {
            return;
        }
        String message = verdict.upgradePath() == null
                ? verdict.reason()
                : verdict.reason() + ". " + verdict.upgradePath();
        throw new ConflictException("wallet", message);
    }
}
