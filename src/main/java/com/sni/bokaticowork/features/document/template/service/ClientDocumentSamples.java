package com.sni.bokaticowork.features.document.template.service;

import com.sni.bokaticowork.features.booking.service.support.BookingConfirmationDocumentService;
import com.sni.bokaticowork.features.document.template.render.DocumentData;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Pieces clients d'exemple pour l'apercu des modeles (lot D5) : releve et recu de portefeuille,
 * confirmation de reservation. Les cles sont celles que produisent les services (variables du
 * gabarit d'origine).
 */
final class ClientDocumentSamples {

    private ClientDocumentSamples() {
    }

    static DocumentData walletStatement() {
        DocumentData data = new DocumentData("WALLET_STATEMENT").title("Relevé de portefeuille").number("WAL-000042-31102026")
                .field("portefeuille", "Portefeuille", "WAL-000042").field("titulaire", "Titulaire", "Grâce Nkounkou")
                .field("periode", "Période", "du 01/10/2026 au 31/10/2026").field("devise", "Devise", "XAF")
                .field("edite_le", "Édité le", "12/11/2026 10:42").field("solde_ouverture", "Solde d'ouverture", "20 000")
                .field("total_credits", "Total des crédits", "50 000").field("total_debits", "Total des débits", "35 000")
                .field("solde_cloture", "Solde de clôture", "35 000").field("empreinte", "Empreinte du registre", "9f2c4a…e71b");
        data.table("operations", new DocumentData.Table("Opérations", List.of(new DocumentData.Column("date", "Date", false),
                new DocumentData.Column("reference", "Référence", false), new DocumentData.Column("nature", "Nature", false),
                new DocumentData.Column("libelle", "Libellé", false), new DocumentData.Column("credit", "Crédit", true),
                new DocumentData.Column("debit", "Débit", true), new DocumentData.Column("solde", "Solde", true)),
                List.of(List.of("05/10/2026 09:12", "WTX-000881", "Recharge", "Mobile money", "50 000", "", "70 000"),
                        List.of("06/10/2026 15:40", "WTX-000902", "Paiement", "Réservation BKG-000311", "", "35 000", "35 000"))));
        data.section("Portefeuille", List.of("portefeuille", "titulaire", "periode", "devise", "edite_le"), null)
                .section("Soldes", List.of("solde_ouverture", "total_credits", "total_debits", "solde_cloture"), null)
                .section("Opérations", List.of(), "operations")
                .section("Intégrité", List.of("empreinte"), null);
        return data;
    }

    static DocumentData walletReceipt() {
        DocumentData data = new DocumentData("WALLET_RECEIPT").title("Reçu d'opération").number("WTX-000881")
                .amount(new BigDecimal("50000"), "XAF")
                .field("titulaire", "Titulaire", "Grâce Nkounkou").field("date", "Date", "05/10/2026 09:12").field("nature", "Nature", "Recharge")
                .field("sens", "Sens", "Montant reçu").field("montant", "Montant", "50 000").field("solde_apres", "Solde après l'opération", "70 000")
                .field("portefeuille", "Portefeuille", "WAL-000042").field("reference", "Référence", "Mobile money MTN 2026100512");
        data.section("Opération", List.of("portefeuille", "titulaire", "date", "nature", "sens", "reference", "montant", "solde_apres"), null);
        return data;
    }

    static DocumentData bookingConfirmation() {
        Context ctx = new Context(Locale.FRENCH);
        ctx.setVariable("bookingNumber", "BKG-202611-000311");
        ctx.setVariable("recipientName", "Grâce Nkounkou");
        ctx.setVariable("resourceName", "Salle Baobab");
        ctx.setVariable("resourceType", "Salle de réunion");
        ctx.setVariable("status", "CONFIRMED");
        ctx.setVariable("bookingDate", "15/11/2026");
        ctx.setVariable("startTime", "09:00");
        ctx.setVariable("endTime", "12:00");
        ctx.setVariable("durationLabel", "3 h");
        ctx.setVariable("seatsLabel", "8 places");
        ctx.setVariable("address", "84 Boulevard du Général Charles de Gaulle");
        ctx.setVariable("city", "Pointe-Noire");
        ctx.setVariable("priceHT", "50 847 XAF");
        ctx.setVariable("priceTVA", "9 153 XAF");
        ctx.setVariable("priceCentimeAdditionnel", "0 XAF");
        ctx.setVariable("priceTTC", "60 000 XAF");
        ctx.setVariable("paymentMode", "WALLET");
        ctx.setVariable("verificationCode", "7KQ2-9XP");
        return BookingConfirmationDocumentService.confirmationData(ctx, new BigDecimal("60000"), "XAF");
    }
}
