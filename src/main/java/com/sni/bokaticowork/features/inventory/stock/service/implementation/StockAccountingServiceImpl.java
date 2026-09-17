package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockJournalEntryResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.StockJournalDirection;
import com.sni.bokaticowork.features.inventory.stock.enums.StockMovementType;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.model.StockJournalEntry;
import com.sni.bokaticowork.features.inventory.stock.model.StockMovement;
import com.sni.bokaticowork.features.inventory.stock.repository.AdjustmentReasonRepository;
import com.sni.bokaticowork.features.inventory.stock.repository.StockJournalEntryRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryPeriodService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockAccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StockAccountingServiceImpl implements StockAccountingService {

    private final StockJournalEntryRepository journalRepository;
    private final AdjustmentReasonRepository adjustmentReasonRepository;
    private final InventoryPeriodService periodService;

    /** Compte de stock. Parametrable, car le plan comptable varie d'une entreprise a l'autre. */
    @Value("${inventory.accounting.stock-account:31}")
    private String stockAccount;

    /** Contrepartie d'une entree d'achat. */
    @Value("${inventory.accounting.purchase-account:601}")
    private String purchaseAccount;

    /** Contrepartie d'une sortie en consommation. */
    @Value("${inventory.accounting.consumption-account:6031}")
    private String consumptionAccount;

    /** Contrepartie d'une perte, d'une casse ou d'un vol. */
    @Value("${inventory.accounting.loss-account:6581}")
    private String lossAccount;

    /** Contrepartie par defaut, lorsque rien de plus precis ne s'applique. */
    @Value("${inventory.accounting.default-counterpart-account:6031}")
    private String defaultCounterpartAccount;

    @Override
    public void recordMovement(StockMovement movement) {
        if (movement.getTotalCost() == null || movement.getTotalCost() == 0L) {
            // Mouvement non valorise : rien a ecrire. Cela reste possible lorsqu'aucun cout n'est connu.
            return;
        }
        if (movement.getMovementType() == StockMovementType.TRANSFER) {
            // Un transfert interne ne change pas la valeur du patrimoine.
            return;
        }
        if (journalRepository.existsByMovementCode(movement.getMovementCode())) {
            return;
        }

        LocalDate accountingDate = movement.getPerformedAt().atZone(ZoneOffset.UTC).toLocalDate();
        boolean increases = movement.getMovementType() == StockMovementType.IN
                || movement.getMovementType() == StockMovementType.ADJUSTMENT_IN;

        StockJournalEntry entry = StockJournalEntry.builder()
                .movementCode(movement.getMovementCode())
                .itemCode(movement.getItem().getItemCode())
                .locationCode(locationCodeOf(movement, increases))
                .stockAccount(stockAccount)
                .counterpartAccount(counterpartFor(movement, increases))
                .direction(increases ? StockJournalDirection.DEBIT : StockJournalDirection.CREDIT)
                .amount(Math.abs(movement.getTotalCost()))
                .accountingDate(accountingDate)
                .periodCode(periodService.periodCodeFor(accountingDate))
                .label(labelFor(movement))
                .build();

        journalRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockJournalEntryResponse> export(LocalDate fromDate, LocalDate toDate, String periodCode) {
        return journalRepository.findForExport(fromDate, toDate, normalize(periodCode))
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCsv(LocalDate fromDate, LocalDate toDate, String periodCode) {
        List<StockJournalEntryResponse> entries = export(fromDate, toDate, periodCode);

        StringBuilder csv = new StringBuilder();
        csv.append("Ecritures de stock\n");
        csv.append("Du:,").append(fromDate == null ? "" : fromDate).append('\n');
        csv.append("Au:,").append(toDate == null ? "" : toDate).append('\n');
        csv.append("Periode:,").append(escapeCsv(normalize(periodCode))).append('\n');
        csv.append("Nombre d ecritures:,").append(entries.size()).append('\n');
        csv.append('\n');

        csv.append("Date,Mouvement,Article,Emplacement,Compte stock,Contrepartie,Sens,Debit,Credit,Periode,Libelle\n");
        long totalDebit = 0;
        long totalCredit = 0;
        for (StockJournalEntryResponse entry : entries) {
            boolean debit = entry.getDirection() == StockJournalDirection.DEBIT;
            long debitAmount = debit ? entry.getAmount() : 0L;
            long creditAmount = debit ? 0L : entry.getAmount();
            totalDebit += debitAmount;
            totalCredit += creditAmount;

            csv.append(entry.getAccountingDate()).append(',')
                    .append(escapeCsv(entry.getMovementCode())).append(',')
                    .append(escapeCsv(entry.getItemCode())).append(',')
                    .append(escapeCsv(entry.getLocationCode())).append(',')
                    .append(escapeCsv(entry.getStockAccount())).append(',')
                    .append(escapeCsv(entry.getCounterpartAccount())).append(',')
                    .append(entry.getDirection()).append(',')
                    .append(debitAmount).append(',')
                    .append(creditAmount).append(',')
                    .append(escapeCsv(entry.getPeriodCode())).append(',')
                    .append(escapeCsv(entry.getLabel())).append('\n');
        }

        csv.append('\n');
        // Le total debit et le total credit doivent s'equilibrer : la contrepartie de chaque
        // ecriture de stock porte le meme montant en sens inverse.
        csv.append("TOTAL,,,,,,,").append(totalDebit).append(',').append(totalCredit).append('\n');
        return csv.toString();
    }

    /**
     * Compte de contrepartie, du plus precis au plus general : le motif d'ajustement s'il est
     * codifie, puis la nature du mouvement, puis le defaut configure.
     */
    private String counterpartFor(StockMovement movement, boolean increases) {
        if (StringUtils.hasText(movement.getAdjustmentReasonCode())) {
            String fromReason = adjustmentReasonRepository.findByReasonCode(movement.getAdjustmentReasonCode())
                    .map(reason -> reason.getCounterpartAccount())
                    .orElse(null);
            if (StringUtils.hasText(fromReason)) {
                return fromReason;
            }
        }

        if (increases) {
            boolean purchase = movement.getReferenceType() == StockReferenceType.GOODS_RECEIPT
                    || movement.getReferenceType() == StockReferenceType.PURCHASE_ORDER;
            return purchase ? purchaseAccount : defaultCounterpartAccount;
        }

        if (movement.getReasonCode() == null) {
            return consumptionAccount;
        }
        return switch (movement.getReasonCode()) {
            case DAMAGE, LOSS -> lossAccount;
            case CONSUMPTION, INTERNAL_USE -> consumptionAccount;
            default -> defaultCounterpartAccount;
        };
    }

    private String locationCodeOf(StockMovement movement, boolean increases) {
        if (increases) {
            return movement.getLocationTo() == null ? null : movement.getLocationTo().getLocationCode();
        }
        return movement.getLocationFrom() == null ? null : movement.getLocationFrom().getLocationCode();
    }

    private String labelFor(StockMovement movement) {
        StringBuilder label = new StringBuilder(movement.getMovementType().name());
        label.append(' ').append(movement.getItem().getItemCode());
        if (StringUtils.hasText(movement.getReferenceCode())) {
            label.append(" · ").append(movement.getReferenceCode());
        }
        return label.length() > 255 ? label.substring(0, 255) : label.toString();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(java.util.Locale.ROOT) : null;
    }

    private StockJournalEntryResponse toResponse(StockJournalEntry entry) {
        return StockJournalEntryResponse.builder()
                .movementCode(entry.getMovementCode())
                .itemCode(entry.getItemCode())
                .locationCode(entry.getLocationCode())
                .stockAccount(entry.getStockAccount())
                .counterpartAccount(entry.getCounterpartAccount())
                .direction(entry.getDirection())
                .amount(entry.getAmount())
                .accountingDate(entry.getAccountingDate())
                .periodCode(entry.getPeriodCode())
                .label(entry.getLabel())
                .build();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
