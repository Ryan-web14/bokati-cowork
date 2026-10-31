package com.sni.bokaticowork.features.billing.service.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Annexe de consommation imprimee apres la facture · ce que chaque collaborateur a pris sur
 * l'abonnement de son organisation pendant la periode ecoulee.
 *
 * <p>Figee en JSON sur le document a sa creation, puis scellee avec lui : la reimpression d'une
 * facture emise montre la meme annexe.</p>
 */
public record InvoiceUsageAnnex(String title,
                                /** Le numero de l'abonnement, ou du pass, dont la consommation est detaillee. */
                                String subscriptionNumber,
                                LocalDate periodStart,
                                LocalDate periodEnd,
                                String currency,
                                List<Row> rows,
                                /** {@code PASS} : pas de part ni de pot, seul le total pris compte · vide, un abonnement. */
                                String kind) {

    public static final String KIND_PASS = "PASS";

    public InvoiceUsageAnnex(String title, String subscriptionNumber, LocalDate periodStart, LocalDate periodEnd,
                             String currency, List<Row> rows) {
        this(title, subscriptionNumber, periodStart, periodEnd, currency, rows, null);
    }

    public boolean pass() {
        return KIND_PASS.equals(kind);
    }

    /**
     * @param fromQuota      pris sur la part nominative de la place
     * @param fromPool       pris sur le pot commun
     * @param consumed       total pris sur les droits de l'abonnement
     * @param overage        quantite facturee en depassement, sur des factures distinctes
     * @param overageAmount  montant de ce depassement
     */
    public record Row(String memberId,
                      String memberName,
                      String entitlementCode,
                      String entitlementName,
                      String unit,
                      BigDecimal fromQuota,
                      BigDecimal fromPool,
                      BigDecimal consumed,
                      BigDecimal overage,
                      BigDecimal overageAmount) {
    }
}
