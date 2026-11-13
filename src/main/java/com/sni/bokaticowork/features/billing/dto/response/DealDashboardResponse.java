package com.sni.bokaticowork.features.billing.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Tableau d'une affaire (lot B11) : ce qui a ete devise, accepte, facture, encaisse, et ce qui reste.
 *
 * @param collected        encaisse : factures d'acompte reglees et reglements des factures, sans
 *                         compter deux fois un acompte deduit de la facture finale
 * @param plannedMargin    marge prevue : HT des devis acceptes moins le cout des articles connus
 * @param realizedMargin   marge realisee : HT facture moins le cout des articles connus
 * @param costedShare      part (en %) du HT dont le cout est connu ; la marge n'est fiable que si
 *                         elle est proche de 100
 */
public record DealDashboardResponse(
        DealResponse deal,
        BigDecimal quoted,
        BigDecimal accepted,
        BigDecimal invoiced,
        BigDecimal creditNotes,
        BigDecimal invoicedNet,
        BigDecimal depositsInvoiced,
        BigDecimal depositsPaid,
        BigDecimal collected,
        BigDecimal remainingToInvoice,
        BigDecimal remainingToCollect,
        BigDecimal plannedMargin,
        BigDecimal realizedMargin,
        BigDecimal costedShare,
        List<Document> documents
) {

    public record Document(String documentNumber, String documentType, String status, boolean deposit, LocalDate issueDate,
                           BigDecimal totalAmount, BigDecimal paidAmount, BigDecimal balanceDue, String currency, Instant createdAt) {
    }
}
