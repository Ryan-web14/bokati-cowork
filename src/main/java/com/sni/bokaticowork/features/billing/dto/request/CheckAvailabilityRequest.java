package com.sni.bokaticowork.features.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Verification des lignes avant creation (lot B7) : stock des articles, article actif, prix
 * plancher et remise maximale, disponibilite des ressources aux dates demandees.
 *
 * @param resources ressources a verifier, chacune rattachee a une ligne par son rang (0 pour la premiere)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CheckAvailabilityRequest(
        BillingDocumentType documentType,
        @Valid List<CreateBillingDocumentLineRequest> lines,
        @Valid List<Resource> resources
) {

    public record Resource(Integer lineIndex, @NotBlank String resourceCode, @NotNull LocalDateTime startAt, @NotNull LocalDateTime endAt,
                           Integer quantity) {
    }
}
