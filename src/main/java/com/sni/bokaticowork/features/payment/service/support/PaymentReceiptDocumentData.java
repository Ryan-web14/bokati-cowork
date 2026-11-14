package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptAllocationResponse;
import com.sni.bokaticowork.features.payment.dto.response.PaymentReceiptResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Donnees du recu de paiement ({@code PAYMENT_RECEIPT}) pour les modeles de documents (lot D5).
 * Le QR du recu (verification) passe tel quel.
 */
public final class PaymentReceiptDocumentData {

    public static final String CODE = "PAYMENT_RECEIPT";

    private PaymentReceiptDocumentData() {
    }

    public static DocumentData from(PaymentReceiptResponse receipt, ZoneId zone, String qrBase64) {
        DateTimeFormatter stamp = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(zone);
        DocumentData data = new DocumentData(CODE).title("Reçu de paiement").number(receipt.receiptNumber()).source(receipt.receiptNumber())
                .amount(receipt.paidAmount(), receipt.currency())
                .verify(null, qrBase64 == null ? null : "data:image/png;base64," + qrBase64)
                .issued(time(stamp, receipt.receiptIssuedAt()), receipt.receivedBy())
                .field("numero_recu", "Reçu", receipt.receiptNumber())
                .field("date_paiement", "Payé le", time(stamp, receipt.paidAt()))
                .field("client", "Client", receipt.customerName())
                .field("client_code", "Code client", receipt.customerCode())
                .field("client_email", "Courriel", receipt.customerEmail())
                .field("client_telephone", "Téléphone", receipt.customerPhone())
                .field("moyen", "Moyen de paiement", receipt.paymentMethodLabel())
                .field("transaction", "Transaction", receipt.transactionNumber())
                .field("reference_operateur", "Référence de l'opérateur", receipt.operatorReference())
                .field("payeur", "Payeur", receipt.payerName())
                .field("banque", "Banque", receipt.bankName())
                .field("objet", "Objet", receipt.purpose())
                .field("montant_paye", "Montant payé", money(receipt.paidAmount(), receipt.currency()))
                .field("montant_affecte", "Affecté aux pièces", money(receipt.allocatedAmount(), receipt.currency()))
                .field("avance", "Avance conservée", money(receipt.advanceAmount(), receipt.currency()))
                .field("remis", "Montant remis", money(receipt.tenderedAmount(), receipt.currency()))
                .field("rendu", "Monnaie rendue", money(receipt.changeAmount(), receipt.currency()))
                .field("encaisse_par", "Encaissé par", receipt.receivedBy())
                .signer("encaisse_par", "Encaissé par", receipt.receivedBy(), time(stamp, receipt.paidAt()))
                .signer("client", "Le client", null, null);
        List<List<String>> rows = new ArrayList<>();
        for (PaymentReceiptAllocationResponse allocation : receipt.allocations() == null ? List.<PaymentReceiptAllocationResponse>of() : receipt.allocations()) {
            rows.add(List.of(nullToEmpty(allocation.documentNumber()), nullToEmpty(allocation.title()),
                    VoucherService.money(allocation.documentTotalAmount()), VoucherService.money(allocation.allocatedAmount()),
                    VoucherService.money(allocation.remainingBalanceDue())));
        }
        data.table("affectations", new DocumentData.Table("Pièces réglées", List.of(new DocumentData.Column("piece", "Pièce", false),
                new DocumentData.Column("objet", "Objet", false), new DocumentData.Column("total", "Total", true),
                new DocumentData.Column("affecte", "Réglé", true), new DocumentData.Column("reste_du", "Reste dû", true)), rows));
        data.section("Paiement", List.of("numero_recu", "date_paiement", "moyen", "transaction", "reference_operateur", "payeur", "banque", "objet"), null)
                .section("Client", List.of("client", "client_code", "client_email", "client_telephone"), null)
                .section("Pièces réglées", List.of(), rows.isEmpty() ? null : "affectations")
                .section("Montants", List.of("montant_paye", "montant_affecte", "avance", "remis", "rendu", "encaisse_par"), null);
        return data;
    }

    public static DocumentData sample() {
        PaymentReceiptResponse receipt = new PaymentReceiptResponse("RCP-202611-000088", Instant.parse("2026-11-12T10:00:00Z"), "PAY-202611-000881",
                null, null, null, null, null, null, null, null, "Règlement de la facture FAC-2026-000042", "BILLING_DOCUMENT", "INV-2026-000042",
                "CUSTOMER", "CUS-0042", "Société Kintele SARL", "compta@kintele.cg", "06 600 00 00", null, new BigDecimal("50000"),
                new BigDecimal("45000"), new BigDecimal("5000"), "XAF", Instant.parse("2026-11-12T10:00:00Z"), "A. Mabiala",
                List.of(new PaymentReceiptAllocationResponse("FAC-2026-000042", null, "Location de bureaux", new BigDecimal("590000"),
                        new BigDecimal("45000"), new BigDecimal("545000"), "XAF")),
                "Espèces", new BigDecimal("50000"), BigDecimal.ZERO, null, "Société Kintele SARL");
        return from(receipt, ZoneId.of("Africa/Brazzaville"), null);
    }

    private static String time(DateTimeFormatter stamp, Instant instant) {
        return instant == null ? "" : stamp.format(instant);
    }

    private static String money(BigDecimal amount, String currency) {
        return amount == null ? "" : VoucherService.money(amount) + " " + (currency == null ? "XAF" : currency);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
