package com.sni.bokaticowork.features.payment.compliance.detector;

import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Les detecteurs · une facon de lire une regle, pas une regle.
 *
 * <p>Chaque detecteur repond a une question avec les seuils que la regle lui donne : combien, sur
 * quelle fenetre, a partir de quel montant. Il ne connait aucun chiffre en propre. Ajouter une
 * famille de detection, c'est ajouter une entree ici ; changer un seuil, c'est changer une ligne
 * en base.</p>
 *
 * <p>Ce qu'un detecteur renvoie est une explication, pas un booleen : la revue doit comprendre
 * pourquoi la regle a parle, sinon elle ne peut ni la confirmer ni l'ecarter.</p>
 */
@Component
public class ComplianceDetectors {

    /** Ce qu'une regle a vu, quand elle a vu quelque chose. */
    public record Detection(WalletRiskFlag.Type flagType, String details, String reference) {
    }

    private final Map<String, Function<DetectionContext, Optional<Detection>>> detectors = new LinkedHashMap<>();

    public ComplianceDetectors() {
        detectors.put("VELOCITY_COUNT", this::velocityCount);
        detectors.put("VELOCITY_AMOUNT", this::velocityAmount);
        detectors.put("STRUCTURING", this::structuring);
        detectors.put("RAPID_IN_OUT", this::rapidInOut);
        detectors.put("FAN_OUT", this::fanOut);
        detectors.put("FAN_IN", this::fanIn);
        detectors.put("CIRCULARITY", this::circularity);
        detectors.put("DORMANT_AMOUNT", this::dormantAmount);
        detectors.put("IDENTITY_LIMIT", this::identityLimit);
        detectors.put("SHARED_DEVICE", this::sharedDevice);
    }

    public boolean knows(String detector) {
        return detector != null && detectors.containsKey(detector);
    }

    public Set<String> names() {
        return detectors.keySet();
    }

    public Optional<Detection> run(String detector, DetectionContext context) {
        Function<DetectionContext, Optional<Detection>> function = detectors.get(detector);
        return function == null ? Optional.empty() : function.apply(context);
    }

    // -----------------------------------------------------------------------------------------
    // Velocite
    // -----------------------------------------------------------------------------------------

    /** Trop d'operations sortantes sur la fenetre. */
    private Optional<Detection> velocityCount(DetectionContext ctx) {
        long debits = ctx.entriesInWindow().stream().filter(this::movesMoneyOut).count();
        int threshold = ctx.countThreshold(10);
        if (debits < threshold) {
            return Optional.empty();
        }
        return Optional.of(new Detection(WalletRiskFlag.Type.UNUSUAL_VOLUME,
                debits + " opérations sortantes en " + minutes(ctx) + " min · seuil " + threshold, null));
    }

    /** Cumul des sorties au-dela du seuil sur la fenetre. */
    private Optional<Detection> velocityAmount(DetectionContext ctx) {
        BigDecimal threshold = ctx.amountThreshold();
        if (threshold == null) {
            return Optional.empty();
        }
        BigDecimal total = ctx.entriesInWindow().stream().filter(this::movesMoneyOut)
                .map(WalletLedgerEntry::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(threshold) < 0) {
            return Optional.empty();
        }
        return Optional.of(new Detection(WalletRiskFlag.Type.UNUSUAL_VOLUME,
                DetectionContext.plain(total) + " " + ctx.wallet().getCurrency() + " sortis en " + minutes(ctx)
                        + " min · seuil " + DetectionContext.plain(threshold), null));
    }

    // -----------------------------------------------------------------------------------------
    // Motifs
    // -----------------------------------------------------------------------------------------

    /**
     * Fractionnement · plusieurs transferts juste sous un seuil.
     *
     * <p>Tout seuil finit par produire ce motif : des montants entre {@code ratio × seuil} et le
     * seuil, repetes. Un seul est une coincidence ; a partir de {@code countThreshold}, c'est une
     * intention.</p>
     */
    private Optional<Detection> structuring(DetectionContext ctx) {
        BigDecimal ceiling = ctx.amountThreshold();
        if (ceiling == null) {
            return Optional.empty();
        }
        BigDecimal floor = ceiling.multiply(ctx.ratioThreshold("0.8"));
        List<WalletTransfer> nearCeiling = ctx.outgoingInWindow().stream()
                .filter(t -> t.getAmount().compareTo(floor) >= 0 && t.getAmount().compareTo(ceiling) < 0)
                .toList();
        int threshold = ctx.countThreshold(3);
        if (nearCeiling.size() < threshold) {
            return Optional.empty();
        }
        BigDecimal sum = nearCeiling.stream().map(WalletTransfer::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return Optional.of(new Detection(WalletRiskFlag.Type.STRUCTURING,
                nearCeiling.size() + " transferts entre " + DetectionContext.plain(floor) + " et "
                        + DetectionContext.plain(ceiling) + " en " + minutes(ctx) + " min · total "
                        + DetectionContext.plain(sum), nearCeiling.get(0).getTransferNumber()));
    }

    /**
     * Aller-retour · le portefeuille sert de tuyau, pas de reserve.
     *
     * <p>Une entree d'au moins {@code amountThreshold}, suivie dans la fenetre d'une sortie qui en
     * represente au moins {@code ratio}. L'ordre compte : une sortie avant l'entree n'est pas un
     * aller-retour.</p>
     */
    private Optional<Detection> rapidInOut(DetectionContext ctx) {
        BigDecimal minimum = ctx.amountThreshold() == null ? BigDecimal.ZERO : ctx.amountThreshold();
        BigDecimal ratio = ctx.ratioThreshold("0.8");
        List<WalletLedgerEntry> entries = ctx.entriesInWindow();
        for (int i = 0; i < entries.size(); i++) {
            WalletLedgerEntry in = entries.get(i);
            if (!isInflow(in) || in.getAmount().compareTo(minimum) < 0) {
                continue;
            }
            BigDecimal outAfter = BigDecimal.ZERO;
            for (int j = i + 1; j < entries.size(); j++) {
                WalletLedgerEntry later = entries.get(j);
                if (later.getEntryType() == WalletEntryType.TRANSFER_OUT) {
                    outAfter = outAfter.add(later.getAmount());
                }
            }
            if (outAfter.compareTo(in.getAmount().multiply(ratio)) >= 0) {
                return Optional.of(new Detection(WalletRiskFlag.Type.RAPID_IN_OUT,
                        "Entrée de " + DetectionContext.plain(in.getAmount()) + " (" + in.getEntryType()
                                + ") suivie de " + DetectionContext.plain(outAfter) + " en transferts sortants en moins de "
                                + minutes(ctx) + " min", in.getTransactionNumber()));
            }
        }
        return Optional.empty();
    }

    /** Un emetteur qui arrose · trop de destinataires distincts. */
    private Optional<Detection> fanOut(DetectionContext ctx) {
        Set<Long> targets = new HashSet<>();
        ctx.outgoingInWindow().forEach(t -> targets.add(t.getTargetWallet().getId()));
        int threshold = ctx.countThreshold(6);
        if (targets.size() < threshold) {
            return Optional.empty();
        }
        return Optional.of(new Detection(WalletRiskFlag.Type.MANY_COUNTERPARTIES,
                targets.size() + " destinataires distincts en " + minutes(ctx) + " min · seuil " + threshold, null));
    }

    /** Un destinataire qui collecte · trop d'emetteurs distincts. */
    private Optional<Detection> fanIn(DetectionContext ctx) {
        Set<Long> sources = new HashSet<>();
        ctx.incomingInWindow().forEach(t -> sources.add(t.getSourceWallet().getId()));
        int threshold = ctx.countThreshold(6);
        if (sources.size() < threshold) {
            return Optional.empty();
        }
        return Optional.of(new Detection(WalletRiskFlag.Type.MANY_COUNTERPARTIES,
                "Reçoit de " + sources.size() + " émetteurs distincts en " + minutes(ctx) + " min · seuil " + threshold, null));
    }

    /**
     * Circularite · A vers B vers C vers A.
     *
     * <p>Profondeur trois, sur la fenetre. Un cycle a deux (A vers B vers A) est un remboursement
     * entre amis et ne dit rien ; a trois, il ne sert plus qu'a brouiller l'origine.</p>
     */
    private Optional<Detection> circularity(DetectionContext ctx) {
        Long a = ctx.wallet().getId();
        for (WalletTransfer ab : ctx.outgoingInWindow()) {
            Long b = ab.getTargetWallet().getId();
            if (b.equals(a)) {
                continue;
            }
            for (WalletTransfer bc : ctx.outgoingFrom(b)) {
                Long c = bc.getTargetWallet().getId();
                if (c.equals(a) || c.equals(b)) {
                    continue;
                }
                boolean closes = ctx.outgoingFrom(c).stream().anyMatch(ca -> ca.getTargetWallet().getId().equals(a));
                if (closes) {
                    return Optional.of(new Detection(WalletRiskFlag.Type.RAPID_IN_OUT,
                            "Cycle " + ctx.wallet().getWalletNumber() + " → " + ab.getTargetWallet().getWalletNumber()
                                    + " → " + bc.getTargetWallet().getWalletNumber() + " → " + ctx.wallet().getWalletNumber()
                                    + " en " + minutes(ctx) + " min", ab.getTransferNumber()));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Reveil avec un montant sans rapport avec l'histoire.
     *
     * <p>Le compte a ete signale dormant recemment, et une sortie de la fenetre depasse a la fois
     * le plancher et {@code ratio} fois la sortie moyenne des six mois qui precedent. Sans
     * historique, le plancher seul decide · un compte qui n'a jamais rien fait et sort gros au
     * reveil merite un regard.</p>
     */
    private Optional<Detection> dormantAmount(DetectionContext ctx) {
        if (!ctx.flaggedRecently(WalletRiskFlag.Type.DORMANT_REACTIVATION)) {
            return Optional.empty();
        }
        BigDecimal floor = ctx.amountThreshold() == null ? BigDecimal.ZERO : ctx.amountThreshold();
        BigDecimal ratio = ctx.ratioThreshold("3");
        List<BigDecimal> history = ctx.entriesSince(Duration.ofDays(180)).stream()
                .filter(this::movesMoneyOut).map(WalletLedgerEntry::getAmount).toList();
        BigDecimal average = history.isEmpty() ? BigDecimal.ZERO
                : history.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(history.size()), 4, RoundingMode.HALF_UP);
        BigDecimal bar = average.multiply(ratio).max(floor);
        Optional<WalletLedgerEntry> big = ctx.entriesInWindow().stream()
                .filter(this::movesMoneyOut).filter(e -> e.getAmount().compareTo(bar) >= 0 && e.getAmount().compareTo(floor) >= 0)
                .findFirst();
        return big.map(e -> new Detection(WalletRiskFlag.Type.DORMANT_REACTIVATION,
                "Sortie de " + DetectionContext.plain(e.getAmount()) + " au réveil · moyenne des six mois précédents "
                        + DetectionContext.plain(average), e.getTransactionNumber()));
    }

    // -----------------------------------------------------------------------------------------
    // Identite
    // -----------------------------------------------------------------------------------------

    /** Tentatives repetees au-dela de ce que le niveau autorise · une demande de pieces vaut mieux qu'un refus sec. */
    private Optional<Detection> identityLimit(DetectionContext ctx) {
        long attempts = ctx.flagsInWindow(WalletRiskFlag.Type.LIMIT_BREACH_ATTEMPT);
        int threshold = ctx.countThreshold(3);
        if (attempts < threshold) {
            return Optional.empty();
        }
        return Optional.of(new Detection(WalletRiskFlag.Type.LIMIT_BREACH_ATTEMPT,
                attempts + " tentatives au-delà du plafond du niveau de vérification en " + minutes(ctx) + " min", null));
    }

    /** Un meme appareil pour des portefeuilles sans lien. */
    private Optional<Detection> sharedDevice(DetectionContext ctx) {
        String deviceId = ctx.triggerDeviceId();
        if (deviceId == null || deviceId.isBlank()) {
            return Optional.empty();
        }
        long wallets = ctx.walletsUsingDevice(deviceId);
        int threshold = ctx.countThreshold(3);
        if (wallets < threshold) {
            return Optional.empty();
        }
        return Optional.of(new Detection(WalletRiskFlag.Type.NEW_DEVICE_LARGE_TRANSFER,
                "L'appareil " + deviceId + " a piloté " + wallets + " portefeuilles distincts en " + minutes(ctx) + " min",
                deviceId));
    }

    // -----------------------------------------------------------------------------------------

    private boolean movesMoneyOut(WalletLedgerEntry entry) {
        return entry.getDirection() == WalletEntryDirection.DEBIT
                && entry.getEntryType() != WalletEntryType.HOLD
                && entry.getEntryType() != WalletEntryType.HOLD_RELEASE;
    }

    private boolean isInflow(WalletLedgerEntry entry) {
        return entry.getEntryType() == WalletEntryType.TOPUP
                || entry.getEntryType() == WalletEntryType.ADMIN_TOPUP
                || entry.getEntryType() == WalletEntryType.TRANSFER_IN;
    }

    private long minutes(DetectionContext ctx) {
        return Duration.between(ctx.windowStart(), ctx.now()).toMinutes();
    }
}
