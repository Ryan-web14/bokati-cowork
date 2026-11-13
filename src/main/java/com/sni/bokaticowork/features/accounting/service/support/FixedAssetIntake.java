package com.sni.bokaticowork.features.accounting.service.support;

import com.sni.bokaticowork.features.accounting.dto.AccountingRequests;
import com.sni.bokaticowork.features.accounting.model.AccSupplierInvoice;
import com.sni.bokaticowork.features.accounting.model.AccSupplierInvoiceLine;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ce que les achats (lot F13) et la saisie des pieces demandent aux immobilisations (lot F18).
 * Absent (contexte sans le lot F18), les achats et la saisie fonctionnent comme avant.
 */
public interface FixedAssetIntake {

    /**
     * Controle d'une ligne de facture sur un compte de classe 2 : categorie connue, montant
     * unitaire au moins egal au seuil d'immobilisation, fiche rattachee existante.
     */
    void checkInvoiceLine(String account, BigDecimal unitPrice, String fixedAssetCode);

    /** Compte de la TVA d'une ligne d'immobilisation : nul pour la TVA recuperable habituelle, le compte du bien si elle ne l'est pas. */
    String vatAccountFor(String account);

    /** Compte du fournisseur credite par la facture : 481 si toutes ses lignes sont des immobilisations, sinon celui du fournisseur. */
    String payableAccount(List<AccSupplierInvoiceLine> lines, String supplierAccount);

    /** Facture ou avoir comptabilise : fiches creees ou valeurs rattachees. */
    void onInvoicePosted(AccSupplierInvoice invoice, List<AccSupplierInvoiceLine> lines, String supplierName);

    /** Saisie manuelle sur un compte suivi par les fiches (classes 2, 28, 29) : refusee sans le droit et le motif. */
    void checkManualEntry(List<AccountingRequests.EntryLine> lines, String label);
}
