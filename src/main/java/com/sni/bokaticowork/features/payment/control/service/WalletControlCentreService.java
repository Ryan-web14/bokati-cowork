package com.sni.bokaticowork.features.payment.control.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.payment.control.model.WalletTreasuryReconciliation;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La vue d'ensemble, et l'export comptable.
 *
 * <p>Le tableau de bord ne calcule rien qu'on ne puisse recalculer depuis les tables : il agrege,
 * il n'invente pas. L'export comptable ventile chaque ecriture entre <em>avance recue</em> (l'argent
 * qui entre, dette envers le titulaire) et <em>produit acquis</em> (l'argent qui sort vers une
 * prestation, dette eteinte) · c'est cette ventilation qui permet a la comptabilite de tenir le
 * compte d'avances sans relire le journal ligne a ligne.</p>
 */
@Service
@RequiredArgsConstructor
public class WalletControlCentreService {

    private static final ZoneId APP_ZONE = ZoneId.of("Africa/Lagos");
    private static final DateTimeFormatter EXPORT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WalletAccountRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final WalletRiskFlagService flagService;
    private final WalletAdminActionService adminActionService;
    private final WalletTreasuryReconciliationService treasuryService;

    public record VolumeLine(WalletEntryType entryType, WalletEntryDirection direction, long count, BigDecimal amount) {
    }

    public record Dashboard(
            String currency,
            BigDecimal totalWalletBalance,
            BigDecimal totalHeldBalance,
            int openWallets,
            Map<String, Long> walletsByStatus,
            long frozenWallets,
            long lockedByOwner,
            long dormantWallets,
            long negativeBalances,
            List<VolumeLine> todayVolume,
            long openFlags,
            long pendingApprovals,
            WalletTreasuryReconciliation latestReconciliation
    ) {
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(String currency) {
        String code = currency == null || currency.isBlank() ? "XAF" : currency.trim().toUpperCase();
        WalletTreasuryReconciliationService.Snapshot snapshot = treasuryService.snapshot(code);

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : walletRepository.countByStatus()) {
            byStatus.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        Instant startOfDay = LocalDate.now(APP_ZONE).atStartOfDay(APP_ZONE).toInstant();
        List<VolumeLine> today = new ArrayList<>();
        for (Object[] row : ledgerRepository.volumeBetween(startOfDay, Instant.now())) {
            today.add(new VolumeLine(
                    WalletEntryType.valueOf(String.valueOf(row[0])),
                    WalletEntryDirection.valueOf(String.valueOf(row[1])),
                    ((Number) row[2]).longValue(),
                    new BigDecimal(String.valueOf(row[3]))));
        }

        return new Dashboard(
                code,
                snapshot.totalWalletBalance(),
                snapshot.totalHeldBalance(),
                snapshot.walletCount(),
                byStatus,
                walletRepository.countFrozen(),
                walletRepository.countLockedByOwner(),
                walletRepository.countDormant(),
                walletRepository.countNegative(),
                today,
                flagService.countOpen(),
                adminActionService.countPending(),
                treasuryService.latest().orElse(null));
    }

    /**
     * Export comptable des ecritures d'une periode, en CSV.
     *
     * <p>Une colonne « imputation » dit ce que l'ecriture fait au compte d'avances : {@code AVANCE_RECUE}
     * quand la dette envers le titulaire augmente, {@code PRODUIT_ACQUIS} quand elle s'eteint par une
     * prestation, {@code RESTITUTION} quand elle s'eteint par un remboursement, {@code INTERNE} pour
     * ce qui ne la change pas · retenues, transferts entre titulaires, qui deplacent la dette sans la
     * modifier en total.</p>
     */
    @Transactional(readOnly = true)
    public byte[] accountingExport(LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new BadRequestException("La période de l'export est invalide");
        }
        Instant start = from.atStartOfDay(APP_ZONE).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(APP_ZONE).toInstant();
        List<WalletLedgerEntry> entries = ledgerRepository.findAllBetween(start, end);

        StringBuilder csv = new StringBuilder();
        csv.append("date;transaction;ecriture;portefeuille;titulaire_type;titulaire_code;nature;sens;montant;devise;")
                .append("solde_apres;imputation;source_type;source_code;libelle;empreinte\n");
        for (WalletLedgerEntry entry : entries) {
            csv.append(EXPORT_DATE.format(entry.getCreatedAt().atZone(APP_ZONE))).append(';')
                    .append(cell(entry.getTransactionNumber())).append(';')
                    .append(cell(entry.getEntryNumber())).append(';')
                    .append(cell(entry.getWallet().getWalletNumber())).append(';')
                    .append(cell(entry.getWallet().getOwnerType())).append(';')
                    .append(cell(entry.getWallet().getOwnerCode())).append(';')
                    .append(entry.getEntryType()).append(';')
                    .append(entry.getDirection()).append(';')
                    .append(entry.getAmount().toPlainString()).append(';')
                    .append(cell(entry.getCurrency())).append(';')
                    .append(entry.getBalanceAfter().toPlainString()).append(';')
                    .append(imputation(entry)).append(';')
                    .append(cell(entry.getSourceType())).append(';')
                    .append(cell(entry.getSourceCode())).append(';')
                    .append(cell(entry.getReference())).append(';')
                    .append(cell(entry.getCurrentHash())).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Ce que l'ecriture fait au compte d'avances. */
    static String imputation(WalletLedgerEntry entry) {
        WalletEntryType type = entry.getEntryType();
        boolean credit = entry.getDirection() == WalletEntryDirection.CREDIT;
        return switch (type) {
            case HOLD, HOLD_RELEASE, TRANSFER_IN, TRANSFER_OUT -> "INTERNE";
            case TRANSFER_FEE -> credit ? "PRODUIT_ACQUIS" : "FRAIS";
            case PAYMENT -> "PRODUIT_ACQUIS";
            case REFUND, OVERPAYMENT_CREDIT -> credit ? "AVANCE_RECUE" : "RESTITUTION";
            case ADMIN_DEBIT -> "RESTITUTION";
            case TOPUP, ADMIN_TOPUP -> "AVANCE_RECUE";
            case CASHBACK, PROMOTIONAL_CREDIT -> "CREDIT_OFFERT";
            case ADJUSTMENT, REVERSAL -> credit ? "AVANCE_RECUE" : "RESTITUTION";
        };
    }

    private String cell(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        return clean.contains(";") || clean.contains("\"")
                ? "\"" + clean.replace("\"", "\"\"") + "\""
                : clean;
    }
}
