package com.sni.bokaticowork.features.inventory.stock.service.implementation;

import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReconciliationLineResponse;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReconciliationReportResponse;
import com.sni.bokaticowork.features.inventory.stock.repository.StockLevelRepository;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockReconciliationServiceImpl implements StockReconciliationService {

    private final StockLevelRepository stockLevelRepository;

    @Override
    public StockReconciliationReportResponse reconcile(String itemCode, String locationCode) {
        String normalizedItem = normalize(itemCode);
        String normalizedLocation = normalize(locationCode);

        List<StockReconciliationLineResponse> divergences =
                stockLevelRepository.findReconciliationDivergences(normalizedItem, normalizedLocation)
                        .stream()
                        .map(this::toLine)
                        .toList();

        BigDecimal totalAbsolute = divergences.stream()
                .map(line -> line.getDifference().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return StockReconciliationReportResponse.builder()
                .generatedAt(Instant.now())
                .itemCodeFilter(normalizedItem)
                .locationCodeFilter(normalizedLocation)
                .pairsChecked(stockLevelRepository.countReconciliationPairs())
                .divergenceCount(divergences.size())
                .totalAbsoluteDifference(totalAbsolute)
                .consistent(divergences.isEmpty())
                .divergences(divergences)
                .build();
    }

    @Override
    public String reconcileCsv(String itemCode, String locationCode) {
        StockReconciliationReportResponse report = reconcile(itemCode, locationCode);
        StringBuilder csv = new StringBuilder();

        csv.append("Reconciliation des niveaux de stock\n");
        csv.append("Genere le:,").append(report.getGeneratedAt()).append('\n');
        csv.append("Article:,").append(escapeCsv(report.getItemCodeFilter())).append('\n');
        csv.append("Emplacement:,").append(escapeCsv(report.getLocationCodeFilter())).append('\n');
        csv.append("Couples examines:,").append(report.getPairsChecked()).append('\n');
        csv.append("Divergences:,").append(report.getDivergenceCount()).append('\n');
        csv.append("Ecart absolu cumule:,").append(report.getTotalAbsoluteDifference()).append('\n');
        csv.append('\n');

        csv.append("Article,Libelle,Emplacement,Nom emplacement,Quantite enregistree,Quantite recalculee,Ecart,Dernier mouvement\n");
        for (StockReconciliationLineResponse line : report.getDivergences()) {
            csv.append(escapeCsv(line.getItemCode())).append(',')
                    .append(escapeCsv(line.getItemName())).append(',')
                    .append(escapeCsv(line.getLocationCode())).append(',')
                    .append(escapeCsv(line.getLocationName())).append(',')
                    .append(line.getRecordedQuantity()).append(',')
                    .append(line.getExpectedQuantity()).append(',')
                    .append(line.getDifference()).append(',')
                    .append(line.getLastMovementAt() == null ? "" : line.getLastMovementAt()).append('\n');
        }
        return csv.toString();
    }

    private StockReconciliationLineResponse toLine(Object[] row) {
        return StockReconciliationLineResponse.builder()
                .itemCode(asString(row[0]))
                .itemName(asString(row[1]))
                .locationCode(asString(row[2]))
                .locationName(asString(row[3]))
                .recordedQuantity(asDecimal(row[4]))
                .expectedQuantity(asDecimal(row[5]))
                .difference(asDecimal(row[6]))
                .lastMovementAt(asInstant(row[7]))
                .build();
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal asDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private Instant asInstant(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        return null;
    }

    /**
     * Applique la meme normalisation de code que {@code StockServiceImpl}, pour que les filtres de
     * reconciliation se comportent comme le reste du module.
     */
    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim()
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "")
                .toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
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
