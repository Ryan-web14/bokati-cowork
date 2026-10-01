package com.sni.bokaticowork.features.payment.compliance.detector;

import com.sni.bokaticowork.features.payment.compliance.model.ComplianceRule;
import com.sni.bokaticowork.features.payment.control.model.WalletRiskFlag;
import com.sni.bokaticowork.features.payment.control.repository.WalletRiskFlagRepository;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.transfer.model.WalletTransfer;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletDeviceRepository;
import com.sni.bokaticowork.features.payment.transfer.repository.WalletTransferRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Ce qu'un detecteur a sous la main pour une regle et un portefeuille.
 *
 * <p>Les donnees sont chargees a la demande et gardees : plusieurs regles regardent la meme fenetre
 * et les memes ecritures, il serait absurde de les relire pour chacune. La fenetre est celle de la
 * regle · chaque regle a la sienne, et c'est une donnee, pas une constante.</p>
 */
public final class DetectionContext {

    private final WalletAccount wallet;
    private final ComplianceRule rule;
    private final Instant now;
    private final Instant windowStart;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final WalletTransferRepository transferRepository;
    private final WalletDeviceRepository deviceRepository;
    private final WalletRiskFlagRepository flagRepository;

    /** Le dernier evenement qui a declenche l'evaluation · nul lors d'un passage periodique. */
    private final String triggerDeviceId;

    private List<WalletLedgerEntry> entriesInWindow;
    private List<WalletTransfer> outgoingInWindow;
    private List<WalletTransfer> incomingInWindow;

    public DetectionContext(WalletAccount wallet, ComplianceRule rule, Instant now, String triggerDeviceId,
                            WalletLedgerEntryRepository ledgerRepository, WalletTransferRepository transferRepository,
                            WalletDeviceRepository deviceRepository, WalletRiskFlagRepository flagRepository) {
        this.wallet = wallet;
        this.rule = rule;
        this.now = now;
        this.windowStart = now.minus(Duration.ofMinutes(rule.getWindowMinutes() == null ? 1440 : Math.max(1, rule.getWindowMinutes())));
        this.triggerDeviceId = triggerDeviceId;
        this.ledgerRepository = ledgerRepository;
        this.transferRepository = transferRepository;
        this.deviceRepository = deviceRepository;
        this.flagRepository = flagRepository;
    }

    public WalletAccount wallet() {
        return wallet;
    }

    public ComplianceRule rule() {
        return rule;
    }

    public Instant now() {
        return now;
    }

    public Instant windowStart() {
        return windowStart;
    }

    public String triggerDeviceId() {
        return triggerDeviceId;
    }

    public List<WalletLedgerEntry> entriesInWindow() {
        if (entriesInWindow == null) {
            entriesInWindow = ledgerRepository.findBetween(wallet.getId(), windowStart, now.plusSeconds(1));
        }
        return entriesInWindow;
    }

    /** Les ecritures d'une periode plus longue que la fenetre · pour comparer a l'historique. */
    public List<WalletLedgerEntry> entriesSince(Duration back) {
        return ledgerRepository.findBetween(wallet.getId(), now.minus(back), windowStart);
    }

    public List<WalletTransfer> outgoingInWindow() {
        if (outgoingInWindow == null) {
            outgoingInWindow = transferRepository.findCompletedFromSince(wallet.getId(), windowStart);
        }
        return outgoingInWindow;
    }

    public List<WalletTransfer> incomingInWindow() {
        if (incomingInWindow == null) {
            incomingInWindow = transferRepository.findCompletedToSince(wallet.getId(), windowStart);
        }
        return incomingInWindow;
    }

    public List<WalletTransfer> outgoingFrom(Long walletId) {
        return transferRepository.findCompletedFromSince(walletId, windowStart);
    }

    public long walletsUsingDevice(String deviceId) {
        return deviceRepository.countDistinctWalletsUsingDevice(deviceId, windowStart);
    }

    public long flagsInWindow(WalletRiskFlag.Type type) {
        return flagRepository.countByWallet_IdAndFlagTypeAndDetectedAtAfter(wallet.getId(), type, windowStart);
    }

    public boolean flaggedRecently(WalletRiskFlag.Type type) {
        return flagRepository.findFirstByWallet_IdAndFlagTypeOrderByDetectedAtDesc(wallet.getId(), type)
                .map(flag -> flag.getDetectedAt() != null && flag.getDetectedAt().isAfter(windowStart))
                .orElse(false);
    }

    // -- Lecture des seuils, avec leur absence assumee ----------------------------------------

    public BigDecimal amountThreshold() {
        return rule.getAmountThreshold();
    }

    public int countThreshold(int fallback) {
        return rule.getCountThreshold() == null ? fallback : rule.getCountThreshold();
    }

    public BigDecimal ratioThreshold(String fallback) {
        return rule.getRatioThreshold() == null ? new BigDecimal(fallback) : rule.getRatioThreshold();
    }

    public static String plain(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }
}
