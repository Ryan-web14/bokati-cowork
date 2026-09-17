package com.sni.bokaticowork.features.inventory.stock.dto.response;

import com.sni.bokaticowork.features.inventory.stock.enums.ProductRecallStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ProductRecallResponse {

    private String recallCode;

    private String itemCode;

    private String lotNumberFrom;

    private String lotNumberTo;

    private ProductRecallStatus status;

    private String reason;

    private Integer frozenLotCount;

    private BigDecimal quantityInStock;

    private BigDecimal quantityIssued;

    private BigDecimal quantityRecovered;

    /** Part de la quantite sortie deja recuperee, entre zero et un. */
    private BigDecimal recoveryRate;

    private String launchedBy;

    private Instant launchedAt;

    private String closedBy;

    private Instant closedAt;

    /** Numeros de lot concernes par le rappel. */
    private List<String> affectedLotNumbers;

    /**
     * Ou la marchandise est partie, reconstitue depuis la tracabilite descendante.
     *
     * <p>C est la reponse a la seule question qui compte le jour d un rappel : qui detient encore
     * du produit rappele.</p>
     */
    private List<Holder> holders;

    @Data
    @Builder
    public static class Holder {
        private String lotNumber;
        private String movementCode;
        private String locationCode;
        private BigDecimal quantity;
        private String referenceType;
        private String referenceCode;
        private Instant issuedAt;
    }
}
