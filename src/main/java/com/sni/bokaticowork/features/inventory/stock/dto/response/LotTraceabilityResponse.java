package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.LotGenealogyRelation;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Remontee complete d un numero de lot, en amont et en aval.
 *
 * <p>Repond en un appel aux deux questions posees le jour d un rappel ou d un litige : d ou vient
 * ce lot, et ou est-il parti.</p>
 */
@Data
@Builder
public class LotTraceabilityResponse {

    private String lotNumber;

    /** Vrai lorsque le numero de lot existe reellement en stock ou dans le journal. */
    private boolean resolved;

    /** Occurrences physiques du lot, un enregistrement par emplacement. */
    private List<LotPosition> positions;

    /** D ou vient le lot : reception, fournisseur, commande. */
    private List<UpstreamEvent> upstream;

    /** Ou le lot est parti : sorties, transferts, consommations. */
    private List<DownstreamEvent> downstream;

    /** Lots dont celui-ci descend. */
    private List<GenealogyLink> parents;

    /** Lots qui descendent de celui-ci. */
    private List<GenealogyLink> children;

    @Data
    @Builder
    public static class LotPosition {
        private Long lotId;
        private String itemCode;
        private String itemName;
        private String locationCode;
        private BigDecimal initialQuantity;
        private BigDecimal remainingQuantity;
        private LocalDate expiryDate;
        private Instant receivedAt;
        private Boolean quarantined;
        private String quarantineReason;
        private Boolean blocked;
        private String blockReason;
        private String ownershipType;
        private String ownerCode;
    }

    @Data
    @Builder
    public static class UpstreamEvent {
        private String movementCode;
        private String movementType;
        private String itemCode;
        private String locationCode;
        private BigDecimal quantity;
        private Long unitCost;
        private String referenceType;
        private String referenceCode;
        private String performedBy;
        private Instant performedAt;
    }

    @Data
    @Builder
    public static class DownstreamEvent {
        private String movementCode;
        private String movementType;
        private String itemCode;
        private String fromLocationCode;
        private String toLocationCode;
        private BigDecimal quantity;
        private String reasonCode;
        private String referenceType;
        private String referenceCode;
        private String performedBy;
        private Instant performedAt;
        private Boolean reversed;
    }

    @Data
    @Builder
    public static class GenealogyLink {
        private String lotNumber;
        private String itemCode;
        private LotGenealogyRelation relation;
        private BigDecimal quantity;
        private String sourceMovementCode;
        private Instant createdAt;
    }
}
