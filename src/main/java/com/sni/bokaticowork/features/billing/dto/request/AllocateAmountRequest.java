package com.sni.bokaticowork.features.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Saisie d'un total a ventiler (lot B8) : le systeme calcule HT, TVA et centimes de chaque ligne et
 * repartit le total au prorata des prix de reference, au franc pres. Rien n'est cree.
 *
 * @param taxIncluded  vrai par defaut : le total saisi est TTC ; faux : HT
 * @param lineIndexes  rangs (0 pour la premiere) des lignes qui se partagent le total ; vide : toutes
 *                     les lignes non optionnelles. Les autres lignes gardent leur prix.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AllocateAmountRequest(
        @Valid @NotEmpty List<CreateBillingDocumentLineRequest> lines,
        @NotNull @Positive BigDecimal targetAmount,
        Boolean taxIncluded,
        List<Integer> lineIndexes,
        BillingDocumentType documentType,
        String currency,
        String customerType,
        String customerCode,
        String companyCustomerCode
) {
}
