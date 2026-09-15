package com.sni.bokaticowork.features.inventory.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Rapport des mouvements passes en forcage de stock negatif.
 *
 * <p>La ventilation par type de reference est indispensable a la lecture : la validation d'un
 * inventaire physique force systematiquement le niveau de stock, ce qui est legitime. Les forcages
 * a surveiller sont ceux d'origine manuelle.</p>
 */
@Data
@Builder
public class InventoryOverrideReportResponse {

    private Instant generatedAt;

    private Instant fromDate;

    private Instant toDate;

    private String referenceTypeFilter;

    /** Nombre total de forcages sur la periode, toutes origines confondues. */
    private long totalOverrides;

    /** Forcages hors validation d'inventaire, ceux qui appellent reellement un controle. */
    private long manualOverrides;

    private List<Breakdown> breakdown;

    private List<Line> lines;

    @Data
    @Builder
    public static class Breakdown {
        private String referenceType;
        private String performedBy;
        private long count;
    }

    @Data
    @Builder
    public static class Line {
        private String movementCode;
        private String movementType;
        private String itemCode;
        private String itemName;
        private String locationCode;
        private BigDecimal quantity;
        private String referenceType;
        private String referenceCode;
        private String reasonCode;
        private String reason;
        private String performedBy;
        private Instant performedAt;
    }
}
