package com.sni.bokaticowork.features.inventory.stock.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Resultat d'une reconciliation entre les niveaux de stock et le journal de mouvements.
 */
@Data
@Builder
public class StockReconciliationReportResponse {

    private Instant generatedAt;

    private String itemCodeFilter;

    private String locationCodeFilter;

    /** Nombre de couples article et emplacement examines. */
    private long pairsChecked;

    /** Nombre de couples divergents. */
    private long divergenceCount;

    /** Somme des ecarts en valeur absolue, toutes unites confondues. */
    private BigDecimal totalAbsoluteDifference;

    /** Vrai lorsque aucun ecart n'a ete detecte sur le perimetre demande. */
    private boolean consistent;

    private List<StockReconciliationLineResponse> divergences;
}
