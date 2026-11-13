package com.sni.bokaticowork.features.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Simulation d'une ligne en cours de saisie (lot B13) : la ligne, sa place, et le reste du document
 * pour recalculer aussi les totaux. Rien n'est cree.
 *
 * @param lineIndex rang de la ligne dans {@code lines} (0 pour la premiere) ; vide ou hors des
 *                  lignes : la ligne est ajoutee a la fin
 * @param lines     les autres lignes du document, telles qu'a l'ecran (la ligne saisie y est
 *                  remplacee a son rang)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SimulateBillingLineRequest(
        Integer lineIndex,
        @Valid @NotNull CreateBillingDocumentLineRequest line,
        BillingDocumentType documentType,
        String currency,
        @Valid List<CreateBillingDocumentLineRequest> lines,
        @Valid List<CreateBillingDocumentDiscountRequest> discounts,
        String customerType,
        String customerCode,
        String companyCustomerCode
) {
}
