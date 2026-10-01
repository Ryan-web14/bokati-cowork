package com.sni.bokaticowork.features.portal.billing.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ClientSpendingSummaryResponse {

    private BigDecimal totalInvoiced;
    private BigDecimal totalPaid;
    /** Ce qui reste du sur les factures emises et non reglees · ni brouillons, ni avoirs. */
    private BigDecimal totalBalanceDue;
    /** Les avoirs scelles et pas encore consommes · ce que nous devons au client. */
    private BigDecimal totalCreditAvailable;
    /** Ce qu il reste a regler une fois les avoirs deduits · c est ce chiffre qu on affiche. */
    private BigDecimal netBalanceDue;
    /** Le nombre de factures qui lui ont ete presentees · les brouillons n en sont pas. */
    private long invoiceCount;
    private long unpaidCount;
    private long overdueCount;
}
