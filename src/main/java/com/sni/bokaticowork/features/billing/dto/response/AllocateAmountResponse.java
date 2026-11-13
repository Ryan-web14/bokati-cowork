package com.sni.bokaticowork.features.billing.dto.response;

import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;

import java.math.BigDecimal;
import java.util.List;

/**
 * Total ventile (lot B8).
 *
 * @param lines            lignes calculees, a reprendre telles quelles dans la creation du document
 * @param referenceAmount  ce que valaient les lignes ventilees au prix de reference
 * @param implicitDiscount ecart entre la reference et le total saisi, porte en remise sur les lignes
 *                         (controle par le plancher et la remise maximale) ; negatif si le total
 *                         saisi depasse la reference
 * @param simulation       calcul complet du document obtenu, avec ses alertes
 */
public record AllocateAmountResponse(
        BigDecimal targetAmount,
        boolean taxIncluded,
        BigDecimal referenceAmount,
        BigDecimal implicitDiscount,
        List<CreateBillingDocumentLineRequest> lines,
        SimulateBillingDocumentResponse simulation
) {
}
