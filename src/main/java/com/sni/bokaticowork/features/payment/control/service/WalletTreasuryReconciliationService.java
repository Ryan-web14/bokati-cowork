package com.sni.bokaticowork.features.payment.control.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.payment.control.model.WalletTreasuryReconciliation;
import com.sni.bokaticowork.features.payment.control.repository.WalletTreasuryReconciliationRepository;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * Le rapprochement entre l'encours des portefeuilles et la tresorerie.
 *
 * <p>Ce n'est pas optionnel. Aucune autorite n'impose de cantonner les fonds · c'est la contrepartie
 * du choix de ne pas etre emetteur de monnaie electronique · donc c'est a l'etablissement de
 * verifier qu'il peut honorer les prestations deja payees. Un ratio de couverture sous le seuil ne
 * figure pas seulement dans un rapport : il alerte la direction.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletTreasuryReconciliationService {

    private final WalletTreasuryReconciliationRepository reconciliationRepository;
    private final WalletAccountRepository walletRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final OutboxService outboxService;

    /** En dessous, la direction est alertee · 1,0 signifie que chaque franc du est disponible. */
    @Value("${bokati.wallet.treasury.coverage-alert-ratio:1.0}")
    private BigDecimal coverageAlertRatio;

    /** Une ou plusieurs adresses, separees par des virgules · direction, finance, qui doit savoir. */
    @Value("${bokati.wallet.treasury.alert-email:}")
    private String alertEmail;

    /** Tolerance sous laquelle un ecart entre encours et compte d'avances est un arrondi, pas une anomalie. */
    @Value("${bokati.wallet.treasury.variance-tolerance:1}")
    private BigDecimal varianceTolerance;

    public record Inputs(LocalDate date, String currency, BigDecimal ledgerAccountBalance,
                         BigDecimal availableCash, String explanation) {
    }

    /** L'encours tel que le systeme le calcule, sans rien saisir · pour l'ecran, avant la saisie. */
    public record Snapshot(String currency, BigDecimal totalWalletBalance, BigDecimal totalHeldBalance, int walletCount) {
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot(String currency) {
        Object[] row = aggregate(currency);
        return new Snapshot(currency, (BigDecimal) row[0], (BigDecimal) row[1], ((Number) row[2]).intValue());
    }

    /**
     * Etablit le rapprochement du jour.
     *
     * <p>L'encours est calcule ; le compte d'avances et la tresorerie sont saisis, parce qu'ils
     * vivent ailleurs. Un rapprochement deja etabli pour la meme date est remplace : on refait un
     * rapprochement, on ne le corrige pas a la main.</p>
     */
    @Transactional
    public WalletTreasuryReconciliation reconcile(Inputs inputs, String preparedBy) {
        if (inputs.date() == null) {
            throw new BadRequestException("La date du rapprochement est requise");
        }
        if (inputs.availableCash() == null) {
            throw new BadRequestException("La trésorerie disponible est requise · c'est tout l'objet du rapprochement");
        }
        String currency = StringUtils.hasText(inputs.currency()) ? inputs.currency().trim().toUpperCase() : "XAF";
        Object[] row = aggregate(currency);
        BigDecimal total = (BigDecimal) row[0];
        BigDecimal held = (BigDecimal) row[1];
        int count = ((Number) row[2]).intValue();

        BigDecimal variance = inputs.ledgerAccountBalance() == null
                ? null
                : inputs.ledgerAccountBalance().subtract(total).setScale(4, RoundingMode.HALF_UP);
        BigDecimal ratio = total.signum() == 0
                ? BigDecimal.ONE
                : inputs.availableCash().divide(total, 4, RoundingMode.HALF_UP);
        boolean explained = StringUtils.hasText(inputs.explanation());
        WalletTreasuryReconciliation.Status status = status(variance, ratio, explained);

        WalletTreasuryReconciliation reconciliation = reconciliationRepository
                .findByReconciliationDateAndCurrency(inputs.date(), currency)
                .orElseGet(() -> WalletTreasuryReconciliation.builder()
                        .reconciliationNumber(sequenceGenerator.next("wallet_treasury_reconciliation"))
                        .reconciliationDate(inputs.date())
                        .currency(currency)
                        .build());
        reconciliation.setTotalWalletBalance(total);
        reconciliation.setTotalHeldBalance(held);
        reconciliation.setWalletCount(count);
        reconciliation.setLedgerAccountBalance(inputs.ledgerAccountBalance());
        reconciliation.setAvailableCash(inputs.availableCash());
        reconciliation.setCoverageRatio(ratio);
        reconciliation.setVariance(variance);
        reconciliation.setVarianceExplained(explained);
        reconciliation.setExplainedBy(explained ? inputs.explanation().trim() : null);
        reconciliation.setStatus(status);
        reconciliation.setPreparedBy(preparedBy);
        WalletTreasuryReconciliation saved = reconciliationRepository.save(reconciliation);

        if (ratio.compareTo(coverageAlertRatio) < 0) {
            alertDirection(saved);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<WalletTreasuryReconciliation> history(Pageable pageable) {
        return reconciliationRepository.findAllByOrderByReconciliationDateDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<WalletTreasuryReconciliation> latest() {
        return reconciliationRepository.findFirstByOrderByReconciliationDateDesc();
    }

    @Transactional(readOnly = true)
    public WalletTreasuryReconciliation get(String number) {
        return reconciliationRepository.findByReconciliationNumber(number)
                .orElseThrow(() -> new ResourceNotFoundException("Rapprochement introuvable"));
    }

    // -----------------------------------------------------------------------------------------

    /**
     * Le statut.
     *
     * <p>Un ratio insuffisant est {@code UNDER_REVIEW} quoi qu'on ait explique : une explication
     * justifie un ecart comptable, pas une tresorerie qui manque. Un ecart comptable explique est
     * {@code VARIANCE} · visible, mais pas alarmant.</p>
     */
    private WalletTreasuryReconciliation.Status status(BigDecimal variance, BigDecimal ratio, boolean explained) {
        if (ratio.compareTo(coverageAlertRatio) < 0) {
            return WalletTreasuryReconciliation.Status.UNDER_REVIEW;
        }
        if (variance != null && variance.abs().compareTo(varianceTolerance) > 0) {
            return explained ? WalletTreasuryReconciliation.Status.VARIANCE : WalletTreasuryReconciliation.Status.UNDER_REVIEW;
        }
        return WalletTreasuryReconciliation.Status.BALANCED;
    }

    private void alertDirection(WalletTreasuryReconciliation reconciliation) {
        log.error("Tresorerie · couverture des portefeuilles a {} % le {} · encours {} {}, disponible {} {}",
                reconciliation.getCoverageRatio().multiply(BigDecimal.valueOf(100)).setScale(1, RoundingMode.HALF_UP),
                reconciliation.getReconciliationDate(), reconciliation.getTotalWalletBalance(), reconciliation.getCurrency(),
                reconciliation.getAvailableCash(), reconciliation.getCurrency());
        // Une adresse ou plusieurs · un courriel par destinataire, la boite de sortie n'en fusionne pas
        for (String recipient : com.sni.bokaticowork.core.utils.mail.Recipients.split(alertEmail)) {
            outboxService.publish("WALLET_TREASURY_COVERAGE_ALERT", "WALLET", reconciliation.getReconciliationNumber() + ":" + recipient, Map.of(
                    "adminEmail", recipient,
                    "subject", "Couverture des portefeuilles insuffisante · " + reconciliation.getReconciliationDate(),
                    "templateCode", "WALLET_TREASURY_COVERAGE_ALERT",
                    "reconciliationNumber", reconciliation.getReconciliationNumber(),
                    "coverageRatio", reconciliation.getCoverageRatio().toPlainString(),
                    "totalWalletBalance", reconciliation.getTotalWalletBalance().toPlainString(),
                    "availableCash", reconciliation.getAvailableCash().toPlainString(),
                    "currency", reconciliation.getCurrency()));
        }
    }

    private Object[] aggregate(String currency) {
        Object[] row = walletRepository.aggregateOpen(currency);
        // Une requete d'agregat native renvoie parfois la ligne enveloppee dans un tableau.
        if (row != null && row.length == 1 && row[0] instanceof Object[] inner) {
            row = inner;
        }
        if (row == null || row.length < 3) {
            return new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, 0L};
        }
        return new Object[]{
                row[0] == null ? BigDecimal.ZERO : new BigDecimal(row[0].toString()),
                row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString()),
                row[2] == null ? 0L : ((Number) row[2]).longValue()};
    }
}
