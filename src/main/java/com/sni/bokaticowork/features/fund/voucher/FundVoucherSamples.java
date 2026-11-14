package com.sni.bokaticowork.features.fund.voucher;

import com.sni.bokaticowork.features.document.template.render.DocumentData;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pieces de fonds d'exemple pour l'apercu des modeles (lot D2), reprises des maquettes validees.
 * Les libelles des sections sont ceux de {@link FundVoucherService} : les cles obtenues sont donc
 * celles d'une piece reelle (test {@code FundVoucherSamplesTest}).
 */
public final class FundVoucherSamples {

    private FundVoucherSamples() {
    }

    public static DocumentData sample(String templateCode) {
        return switch (templateCode) {
            case "NEED_STATEMENT" -> need();
            case "FUND_REQUEST" -> request();
            case "DISBURSEMENT_VOUCHER" -> disbursement();
            case "JUSTIFICATION_SHEET" -> justification();
            default -> null;
        };
    }

    /** Pieces comptables d'exemple (lot D4) : bon de commande, TVA, rapprochement, emprunt, bordereau, releves. */
    public static DocumentData accounting(String templateCode) {
        return switch (templateCode) {
            case "PURCHASE_ORDER" -> {
                VoucherService.Section lines = new VoucherService.Section("Lignes").columns(List.of("Désignation", "Quantité", "Prix unitaire", "HT", "TVA", "TTC"),
                        List.of(false, true, true, true, true, true));
                lines.row("Ramettes de papier A4", "20", "5 000", "100 000", "18 000", "118 000");
                yield view(templateCode, "Bon de commande", "BC-202611-000007", List.of(
                        new VoucherService.Section("Commande").pair("Numéro", "BC-202611-000007").pair("Fournisseur", "Papeterie du Centre")
                                .pair("Objet", "Fournitures de bureau").pair("Livraison souhaitée", "14/11/2026"),
                        lines), new BigDecimal("118000"), null, List.of(new VoucherService.Signature("Acheteur", "R. Ibara", "12/11/2026"),
                        new VoucherService.Signature("Fournisseur, pour acceptation", null, null)));
            }
            case "VAT_RETURN" -> view(templateCode, "Déclaration de TVA 2026-10", "DTV-202611-000001", List.of(
                    new VoucherService.Section("Période").pair("Période", "Octobre 2026").pair("Régime", "Réel normal"),
                    new VoucherService.Section("Calcul").pair("TVA collectée", "1 800 000 XAF").pair("TVA déductible", "450 000 XAF")
                            .pair("TVA due", "1 350 000 XAF")), new BigDecimal("1350000"), null,
                    List.of(new VoucherService.Signature("Comptable", "R. Ibara", null), new VoucherService.Signature("Gérant", null, null)));
            case "BANK_RECONCILIATION" -> view(templateCode, "État de rapprochement bancaire", "RAP-202611-000002", List.of(
                    new VoucherService.Section("Compte").pair("Banque", "Banque du Congo").pair("Date", "31/10/2026"),
                    new VoucherService.Section("Soldes").pair("Solde du relevé", "4 520 000 XAF").pair("Solde comptable", "4 470 000 XAF")
                            .pair("Écart expliqué", "50 000 XAF")), null, null,
                    List.of(new VoucherService.Signature("Comptable", "R. Ibara", null), new VoucherService.Signature("Contrôleur", null, null)));
            case "LOAN_SCHEDULE" -> {
                VoucherService.Section lines = new VoucherService.Section("Échéancier").columns(List.of("Échéance", "Date", "Capital", "Intérêts", "Annuité", "Restant dû"),
                        List.of(false, false, true, true, true, true));
                lines.row("1", "30/11/2026", "400 000", "60 000", "460 000", "5 600 000");
                yield view(templateCode, "Tableau d'amortissement EMP-1", "EMP-1-V1", List.of(
                        new VoucherService.Section("Emprunt").pair("Code", "EMP-1").pair("Prêteur", "Banque du Congo").pair("Taux", "12 %"),
                        lines), null, null, List.of());
            }
            case "BANK_DEPOSIT_SLIP" -> {
                VoucherService.Section lines = new VoucherService.Section("Chèques").columns(List.of("N° chèque", "Banque émettrice", "Tireur", "Transaction", "Montant"),
                        List.of(false, false, false, false, true));
                lines.row("0004521", "BGFI", "Société Kintele SARL", "PAY-202611-000881", "250 000");
                yield view(templateCode, "Bordereau de remise en banque", "BRB-202611-000003", List.of(
                        new VoucherService.Section("Remise").pair("Bordereau", "BRB-202611-000003").pair("Banque", "Banque du Congo, Pointe-Noire")
                                .pair("Caisse", "GUICHET-1").pair("Préparé par", "A. Mabiala").pair("État", "Préparé"),
                        lines,
                        new VoucherService.Section("Montants").pair("Espèces", "150 000 XAF").pair("Chèques (1)", "250 000 XAF").pair("Total remis", "400 000 XAF")),
                        new BigDecimal("400000"), null, List.of(new VoucherService.Signature("Caissier", "A. Mabiala", "12/11/2026"),
                                new VoucherService.Signature("Déposant", null, null), new VoucherService.Signature("Cachet de la banque", null, null)));
            }
            case "SUPPLIER_STATEMENT", "PARTNER_STATEMENT" -> {
                boolean supplier = templateCode.equals("SUPPLIER_STATEMENT");
                VoucherService.Section lines = new VoucherService.Section("Écritures").columns(
                        List.of("Date", "Écriture", "Libellé", "Référence", "Débit", "Crédit", "Solde"),
                        List.of(false, false, false, false, true, true, true));
                lines.row("05/10/2026", "AC-000412", "Facture F-2026-118", "F-2026-118", "0", "118 000", "118 000");
                lines.row("20/10/2026", "BQ-000233", "Règlement", "VIR-889", "118 000", "0", "0");
                yield view(templateCode, supplier ? "Relevé fournisseur" : "Relevé de compte partenaire", (supplier ? "RLF" : "RLP") + "-PAP01-20261112",
                        List.of(new VoucherService.Section(supplier ? "Fournisseur" : "Partenaire").pair("Code", "PAP01")
                                        .pair("Nom", supplier ? "Papeterie du Centre" : "Partenaire exemple").pair("Période", "Du 01/10/2026 au 31/10/2026"),
                                lines, new VoucherService.Section("Soldes").pair("Solde d'ouverture", "0 XAF").pair("Solde de clôture", "0 XAF")),
                        null, null, List.of(new VoucherService.Signature("Comptable", null, null),
                                new VoucherService.Signature(supplier ? "Fournisseur, pour accord" : "Partenaire, pour accord", null, null)));
            }
            default -> null;
        };
    }

    private static DocumentData need() {
        VoucherService.Section lines = new VoucherService.Section("Besoins").columns(
                List.of("N°", "Désignation", "Quantité", "Unité", "Prix estimé", "Total", "Nature", "Fournisseur suggéré"),
                List.of(false, false, true, false, true, true, false, false));
        lines.row(1, "Ramettes de papier A4", "20", "ramette", "5 000", "100 000", "Fournitures de bureau", "Papeterie du Centre");
        lines.row(2, "Cartouches d'encre", "2", "pièce", "9 000", "18 000", "Fournitures de bureau", "Papeterie du Centre");
        lines.row(3, "Marqueurs et blocs-notes", "1", "lot", "12 500", "12 500", "Fournitures de bureau", "");
        lines.row(4, "Vidéoprojecteur de remplacement", "1", "pièce", "450 000", "450 000", "Immobilisation", "");
        List<VoucherService.Section> sections = List.of(
                new VoucherService.Section("Demandeur").pair("Demandeur", "Grâce Nkounkou").pair("Service", "Accueil et moyens généraux")
                        .pair("Site", "Pointe-Noire centre").pair("Centre de coût", "ACCUEIL").pair("État de besoin", "EDB-202611-000012")
                        .pair("Date", "08/11/2026 09:15"),
                new VoucherService.Section("Objet").pair("Objet", "Stock de fournitures épuisé à l'accueil ; événement client le 15 novembre.")
                        .pair("Urgence", "Normale").pair("Date souhaitée", "14/11/2026").pair("État", "Transformé"),
                lines,
                new VoucherService.Section("Décision").pair("Décidé par", "P. Moussa").pair("Le", "09/11/2026 11:02")
                        .pair("Commentaire", "Prévoir trois devis pour le vidéoprojecteur."),
                new VoucherService.Section("Suite donnée").pair("Demandes de fonds", "DDF-202611-000031"));
        return view("NEED_STATEMENT", "État de besoin", "EDB-202611-000012", sections, new BigDecimal("580500"), null, List.of(
                new VoucherService.Signature("Demandeur", null, null),
                new VoucherService.Signature("Chef de service", "P. Moussa", "09/11/2026 11:02"),
                new VoucherService.Signature("Responsable financier", null, null)));
    }

    private static DocumentData request() {
        VoucherService.Section lines = new VoucherService.Section("Lignes").columns(List.of("Nature", "Désignation", "Fournisseur", "Section", "Montant"),
                List.of(false, false, false, false, true));
        lines.row("Fournitures de bureau", "Ramettes de papier A4 (20)", "Papeterie du Centre", "ACCUEIL", "100 000");
        lines.row("Fournitures de bureau", "Cartouches d'encre (2)", "Papeterie du Centre", "ACCUEIL", "18 000");
        lines.row("Fournitures de bureau", "Marqueurs et blocs-notes", "", "SALLES", "12 500");
        VoucherService.Section timeline = new VoucherService.Section("Circuit de validation").columns(
                List.of("Niveau", "Nom", "Décision", "Montant", "Date", "Commentaire"), List.of(false, false, false, true, false, false));
        timeline.row("Soumission", "Grâce Nkounkou", "Soumis", "130 500", "10/11/2026 08:40", "");
        timeline.row(1, "P. Moussa", "Approuvé", "130 500", "10/11/2026 14:05", "");
        timeline.row(2, "R. Ibara", "Approuvé", "130 500", "11/11/2026 09:30", "");
        List<VoucherService.Section> sections = List.of(
                new VoucherService.Section("Demande").pair("Numéro", "DDF-202611-000031").pair("Type", "Dépense directe")
                        .pair("Objet", "Réassort de fournitures de bureau pour l'accueil et les salles de réunion.")
                        .pair("Date", "10/11/2026 08:40").pair("État de besoin", "EDB-202611-000012").pair("État", "Approuvé"),
                new VoucherService.Section("Personnes").pair("Demandeur", "Grâce Nkounkou").pair("Service", "Accueil et moyens généraux")
                        .pair("Bénéficiaire", "Papeterie du Centre").pair("Caisse de remise", "Guichet accueil").pair("Mode de remise", "CASH")
                        .pair("Date souhaitée", "14/11/2026"),
                lines,
                new VoucherService.Section("Au jour de la demande").pair("Niveaux requis", "Chef de service, Comptable")
                        .pair("Avances non soldées du bénéficiaire", "Aucune"),
                timeline);
        return view("FUND_REQUEST", "Demande de fonds", "DDF-202611-000031", sections, new BigDecimal("130500"), null, List.of(
                new VoucherService.Signature("Demandeur", "Grâce Nkounkou", "10/11/2026 08:40"),
                new VoucherService.Signature("Niveau 1 : APPROVED", "P. Moussa", "10/11/2026 14:05"),
                new VoucherService.Signature("Niveau 2 : APPROVED", "R. Ibara", "11/11/2026 09:30")));
    }

    private static DocumentData disbursement() {
        VoucherService.Section lines = new VoucherService.Section("Lignes").columns(List.of("Nature", "Désignation", "Fournisseur", "Section", "Montant"),
                List.of(false, false, false, false, true));
        lines.row("Fournitures de bureau", "Ramettes de papier A4 (20)", "Papeterie du Centre", "ACCUEIL", "100 000");
        List<VoucherService.Section> sections = List.of(
                new VoucherService.Section("Demande").pair("Numéro", "DDF-202611-000031").pair("Type", "Dépense directe")
                        .pair("Objet", "Réassort de fournitures de bureau").pair("Date", "10/11/2026 08:40").pair("État", "Décaissé"),
                new VoucherService.Section("Personnes").pair("Demandeur", "Grâce Nkounkou").pair("Bénéficiaire", "Papeterie du Centre")
                        .pair("Caisse de remise", "Guichet accueil"),
                lines,
                new VoucherService.Section("Décaissement").pair("Mouvement de caisse", "CSM-202611-000127").pair("Session", "CSS-202611-0042")
                        .pair("Caissière", "A. Mabiala").pair("Date", "12/11/2026 10:42").pair("Montant approuvé", "130 500 XAF")
                        .pair("Montant décaissé", "100 000 XAF").pair("Reste à décaisser", "30 500 XAF"));
        return view("DISBURSEMENT_VOUCHER", "Bon de décaissement de fonds", "BDF-202611-000009", sections, new BigDecimal("100000"),
                "Je soussigné(e) Papeterie du Centre reconnais avoir reçu la somme ci-dessus.", List.of(
                        new VoucherService.Signature("Bénéficiaire", null, null),
                        new VoucherService.Signature("Caissière", "A. Mabiala", "12/11/2026 10:42")));
    }

    private static DocumentData justification() {
        VoucherService.Section table = new VoucherService.Section("Dépenses").columns(
                List.of("Date", "Fournisseur", "Nature", "Désignation", "HT", "TVA", "TTC", "Pièce", "Statut"),
                List.of(false, false, false, false, true, true, true, false, false));
        table.row("13/11/2026", "Hôtel Atlantic", "Hébergement", "Deux nuits", "84 746", "15 254", "100 000", "F-889", "Validée");
        List<VoucherService.Section> sections = List.of(
                new VoucherService.Section("Avance").pair("Demande", "DDF-202611-000040").pair("Objet", "Mission à Brazzaville")
                        .pair("Bénéficiaire", "Grâce Nkounkou").pair("Échéance", "25/11/2026"),
                table,
                new VoucherService.Section("Solde").pair("Avance reçue", "150 000 XAF").pair("Total justifié", "100 000 XAF")
                        .pair("Reste à rendre", "50 000 XAF"));
        return view("JUSTIFICATION_SHEET", "Fiche de justification d'avance", "JUS-202611-000004", sections, new BigDecimal("100000"), null,
                List.of(new VoucherService.Signature("Bénéficiaire", null, null), new VoucherService.Signature("Comptable valideur", "R. Ibara", null)));
    }

    private static DocumentData view(String code, String title, String number, List<VoucherService.Section> sections, BigDecimal amount,
                                     String commitment, List<VoucherService.Signature> signatures) {
        VoucherService.View view = new VoucherService.View(title, number, null, null, false, "", null, sections,
                VoucherService.money(amount) + " XAF", null, commitment, signatures, "12/11/2026 10:42", "A. Mabiala", "", "", null, null);
        return DocumentData.fromVoucher(code, view, amount);
    }
}
