package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentLineResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.service.implementation.BillingDocumentPdfServiceImpl.BillingDocumentTemplateFormatter;
import com.sni.bokaticowork.features.document.template.render.DocumentData;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Donnees des pieces de facturation pour les modeles de documents (lot D5) : facture, devis,
 * proforma, note de debit et facture rectificative ({@code INVOICE}), avoir ({@code CREDIT_NOTE}).
 * Le gabarit d'origine garde ses particularites (acomptes deduits, annexe de consommation, visa
 * signe) ; un modele publie les remplace par ses blocs.
 */
public final class BillingDocumentData {

    public static final String INVOICE = "INVOICE";
    public static final String CREDIT_NOTE = "CREDIT_NOTE";

    private BillingDocumentData() {
    }

    public static String templateCode(BillingDocumentType type) {
        return type == BillingDocumentType.CREDIT_NOTE ? CREDIT_NOTE : INVOICE;
    }

    public static String title(BillingDocumentType type) {
        if (type == null) {
            return "Facture";
        }
        return switch (type) {
            case QUOTE -> "Devis";
            case PROFORMA_INVOICE -> "Facture proforma";
            case CREDIT_NOTE -> "Avoir";
            case CORRECTIVE_INVOICE -> "Facture rectificative";
            case DEBIT_NOTE -> "Note de débit";
            default -> "Facture";
        };
    }

    public static DocumentData from(BillingDocumentResponse document, BillingDocumentTemplateFormatter fmt, String qrDataUri,
                                    String vatExemptionNote) {
        String code = templateCode(document.documentType());
        String number = document.fiscalNumber() != null && !document.fiscalNumber().isBlank() ? document.fiscalNumber() : document.documentNumber();
        DocumentData data = new DocumentData(code).title(title(document.documentType())).number(number).source(document.documentNumber())
                .amount(document.totalAmount(), document.currency())
                .verify(null, qrDataUri)
                .issued(fmt.date(document.issueDate()), document.sellerName())
                .field("client", fmt.partyLabel(document.customerType()), document.customerName())
                .field("client_code", "Code client", document.customerCode())
                .field("adresse", "Adresse", String.join(", ", fmt.addressLines(document.billingAddressJson())))
                .field("client_niu", "NIU du client", document.customerNiu())
                .field("client_email", "Courriel", document.customerEmail())
                .field("client_telephone", "Téléphone", document.customerPhone())
                .field("numero_document", "N° de pièce", document.documentNumber())
                .field("date", "Date", fmt.date(document.issueDate()))
                .field("echeance", "Échéance", fmt.date(document.dueDate()))
                .field("reference_client", "Votre référence", document.customerReference())
                .field("bon_de_commande", "Bon de commande", document.poNumber())
                .field("objet", "Objet", fmt.documentSubject(document))
                .field("sous_total", "Sous-total HT", fmt.money(document.subtotalAmount()))
                .field("remise", "Remise", fmt.money(document.discountAmount()))
                .field("base_taxable", "Base taxable", fmt.money(document.taxableAmount()))
                .field("tva", "TVA", fmt.money(document.vatAmount()))
                .field("centimes_additionnels", "Centimes additionnels", fmt.money(document.additionalCentAmount()))
                .field("total_ttc", "Total TTC", fmt.money(document.totalAmount()))
                .field("deja_paye", "Déjà payé", fmt.money(document.paidAmount()))
                .field("reste_du", "Reste dû", fmt.money(document.balanceDue()))
                .field("conditions", "Conditions", document.terms())
                .field("instructions_paiement", "Règlement", document.paymentInstructions())
                .field("mention_exoneration", "Exonération", vatExemptionNote)
                .field("facture_origine", "Pièce d'origine", document.originalDocumentNumber())
                .field("motif", "Motif", document.creditNoteReason())
                .signer("emetteur", "Pour " + (document.sellerName() == null ? "l'exploitant" : document.sellerName()), null, null)
                .signer("client", "Le client, pour accord", null, null);
        List<List<String>> rows = new ArrayList<>();
        int index = 1;
        for (BillingDocumentLineResponse line : document.lines() == null ? List.<BillingDocumentLineResponse>of() : document.lines()) {
            String designation = line.description() == null ? "" : line.description();
            if (line.servicePeriod() != null) {
                designation += " (" + line.servicePeriod() + ")";
            }
            if (Boolean.TRUE.equals(line.optional())) {
                designation += " (option)";
            }
            rows.add(List.of(String.valueOf(line.lineOrder() == null ? index : line.lineOrder()), designation, fmt.quantity(line.quantity()),
                    line.unit() == null ? "" : line.unit(), fmt.money(fmt.unitPriceHt(line)),
                    line.discountRate() == null || line.discountRate().signum() == 0 ? "" : fmt.percent(line.discountRate()),
                    fmt.money(fmt.netHt(line)), line.vatRate() == null ? "" : fmt.percent(line.vatRate()), fmt.money(fmt.totalTtc(line))));
            index++;
        }
        data.table("lignes", new DocumentData.Table("Lignes", List.of(new DocumentData.Column("n", "N°", false),
                new DocumentData.Column("designation", "Désignation", false), new DocumentData.Column("quantite", "Qté", true),
                new DocumentData.Column("unite", "Unité", false), new DocumentData.Column("pu_ht", "PU HT", true),
                new DocumentData.Column("remise", "Remise", true), new DocumentData.Column("total_ht", "Total HT", true),
                new DocumentData.Column("tva", "TVA", true), new DocumentData.Column("total_ttc", "Total TTC", true)), rows));
        data.section("Client", List.of("client", "client_code", "adresse", "client_niu", "client_email", "client_telephone"), null)
                .section("Pièce", code.equals(CREDIT_NOTE)
                        ? List.of("numero_document", "date", "facture_origine", "motif", "objet")
                        : List.of("numero_document", "date", "echeance", "reference_client", "bon_de_commande", "objet"), null)
                .section("Lignes", List.of(), "lignes")
                .section("Totaux", List.of("sous_total", "remise", "base_taxable", "tva", "centimes_additionnels", "total_ttc", "deja_paye", "reste_du"), null)
                .section("Conditions", List.of("conditions", "instructions_paiement", "mention_exoneration"), null);
        return data;
    }

    /** Facture d'exemple pour l'apercu. */
    public static DocumentData sample(String code) {
        boolean credit = CREDIT_NOTE.equals(code);
        DocumentData data = new DocumentData(code).title(credit ? "Avoir" : "Facture").number(credit ? "AVR-2026-000003" : "FAC-2026-000042")
                .amount(new BigDecimal(credit ? "59000" : "590000"), "XAF").issued("12/11/2026", "ELLE A OSE")
                .field("client", "Client", "Société Kintele SARL").field("client_code", "Code client", "CUS-0042")
                .field("adresse", "Adresse", "Avenue de l'Indépendance, Pointe-Noire").field("client_niu", "NIU du client", "M2026000042Z")
                .field("client_email", "Courriel", "compta@kintele.cg").field("client_telephone", "Téléphone", "06 600 00 00")
                .field("numero_document", "N° de pièce", credit ? "AVR-2026-000003" : "INV-2026-000042").field("date", "Date", "12/11/2026")
                .field("echeance", "Échéance", "27/11/2026").field("reference_client", "Votre référence", "BC-KIN-118")
                .field("bon_de_commande", "Bon de commande", "").field("objet", "Objet", "Location de bureaux, novembre 2026")
                .field("sous_total", "Sous-total HT", credit ? "50 000" : "500 000").field("remise", "Remise", "0")
                .field("base_taxable", "Base taxable", credit ? "50 000" : "500 000").field("tva", "TVA", credit ? "9 000" : "90 000")
                .field("centimes_additionnels", "Centimes additionnels", "0").field("total_ttc", "Total TTC", credit ? "59 000" : "590 000")
                .field("deja_paye", "Déjà payé", "0").field("reste_du", "Reste dû", credit ? "0" : "590 000")
                .field("conditions", "Conditions", "Paiement à 15 jours.").field("instructions_paiement", "Règlement", "Virement ou mobile money")
                .field("mention_exoneration", "Exonération", "").field("facture_origine", "Pièce d'origine", credit ? "FAC-2026-000042" : "")
                .field("motif", "Motif", credit ? "Remise commerciale accordée" : "")
                .signer("emetteur", "Pour ELLE A OSE", null, null).signer("client", "Le client, pour accord", null, null);
        data.table("lignes", new DocumentData.Table("Lignes", List.of(new DocumentData.Column("n", "N°", false),
                new DocumentData.Column("designation", "Désignation", false), new DocumentData.Column("quantite", "Qté", true),
                new DocumentData.Column("unite", "Unité", false), new DocumentData.Column("pu_ht", "PU HT", true),
                new DocumentData.Column("remise", "Remise", true), new DocumentData.Column("total_ht", "Total HT", true),
                new DocumentData.Column("tva", "TVA", true), new DocumentData.Column("total_ttc", "Total TTC", true)),
                List.of(List.of("1", "Bureau privatif (du 01/11/2026 au 30/11/2026)", "1", "mois", credit ? "50 000" : "500 000", "",
                        credit ? "50 000" : "500 000", "18 %", credit ? "59 000" : "590 000"))));
        data.section("Client", List.of("client", "client_code", "adresse", "client_niu", "client_email", "client_telephone"), null)
                .section("Pièce", credit ? List.of("numero_document", "date", "facture_origine", "motif", "objet")
                        : List.of("numero_document", "date", "echeance", "reference_client", "bon_de_commande", "objet"), null)
                .section("Lignes", List.of(), "lignes")
                .section("Totaux", List.of("sous_total", "remise", "base_taxable", "tva", "centimes_additionnels", "total_ttc", "deja_paye", "reste_du"), null)
                .section("Conditions", List.of("conditions", "instructions_paiement", "mention_exoneration"), null);
        return data;
    }
}
