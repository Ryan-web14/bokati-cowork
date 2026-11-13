package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashRequestResponse;
import com.sni.bokaticowork.features.payment.dto.response.CashSessionSummaryResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Donnees de la demande de caisse ({@code CASH_REQUEST}) et du rapport de cloture de session
 * ({@code CASH_CLOSING_REPORT}) pour les modeles de documents (lot D4). Les sections reprennent
 * celles des gabarits d'origine ; le bloc SECTIONS les imprime toutes.
 */
public final class CashReportDocumentData {

    public static final String REQUEST = "CASH_REQUEST";
    public static final String CLOSING = "CASH_CLOSING_REPORT";
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm").withZone(ZONE);

    private CashReportDocumentData() {
    }

    public static DocumentData request(CashRequestResponse request, String typeLabel, String statusLabel) {
        String attachments = request.attachments() == null || request.attachments().isEmpty() ? "Aucune"
                : String.join(", ", request.attachments().stream().map(file -> file.label() != null ? file.label() : file.fileName()).toList());
        DocumentData data = new DocumentData(REQUEST).title(typeLabel).number(request.requestNumber()).source(request.requestNumber())
                .amount(request.amount(), request.currency()).issued(VoucherService.date(request.requestedAt()), request.requestedBy())
                .field("numero_demande", "Demande", request.requestNumber())
                .field("type", "Type", typeLabel)
                .field("etat", "État", statusLabel)
                .field("caisse", "Caisse", request.registerCode())
                .field("session", "Session", request.sessionNumber())
                .field("demande_par", "Demandé par", request.requestedBy())
                .field("demande_le", "Demandé le", VoucherService.date(request.requestedAt()))
                .field("motif", "Motif", request.reason())
                .field("pieces_jointes", "Pièces jointes", attachments)
                .field("examine_par", "Examiné par", request.reviewedBy())
                .field("examine_le", "Examiné le", VoucherService.date(request.reviewedAt()))
                .field("note_examen", "Observation", request.reviewNote())
                .field("mouvement_execute", "Mouvement exécuté", request.executedMovementNumber())
                .field("execute_le", "Exécuté le", VoucherService.date(request.executedAt()))
                .signer("demandeur", "Demandeur", request.requestedBy(), VoucherService.date(request.requestedAt()))
                .signer("valideur", "Valideur", request.reviewedBy(), VoucherService.date(request.reviewedAt()));
        data.section("Demande", List.of("numero_demande", "type", "etat", "caisse", "session", "demande_par", "demande_le"), null)
                .section("Motif", List.of("motif", "pieces_jointes"), null)
                .section("Décision", List.of("examine_par", "examine_le", "note_examen", "mouvement_execute", "execute_le"), null);
        return data;
    }

    public static DocumentData closing(CashSessionSummaryResponse summary, List<CashMovementResponse> movements, String reviewedBy,
                                       java.util.function.Function<CashMovementResponse, String> typeLabel) {
        DocumentData data = new DocumentData(CLOSING).title("Rapport de clôture de caisse").number(summary.sessionNumber()).source(summary.sessionNumber())
                .issued(VoucherService.date(summary.closedAt()), summary.closedBy())
                .field("session", "Session", summary.sessionNumber())
                .field("caisse", "Caisse", summary.registerCode())
                .field("etat", "État", summary.status() == null ? "" : switch (summary.status()) {
                    case CLOSED -> "Clôturée";
                    case CLOSING_REVIEW -> "Écart en examen";
                    case COUNTING, RECOUNT -> "Comptage en cours";
                    case CANCELLED -> "Annulée";
                    default -> "Ouverte";
                })
                .field("ouvert_par", "Ouverte par", summary.openedBy())
                .field("ouvert_le", "Ouverte le", VoucherService.date(summary.openedAt()))
                .field("cloture_par", "Clôturée par", summary.closedBy())
                .field("cloture_le", "Clôturée le", VoucherService.date(summary.closedAt()))
                .field("revu_par", "Revue par", reviewedBy)
                .field("fonds_ouverture", "Fonds d'ouverture", money(summary.openingAmount()))
                .field("encaissements", "Encaissements", money(summary.totalPayments()))
                .field("remboursements", "Remboursements", money(summary.totalRefunds()))
                .field("entrees", "Entrées de caisse", money(summary.totalCashIn()))
                .field("sorties", "Sorties de caisse", money(summary.totalCashOut()))
                .field("ajustements", "Ajustements", money(summary.totalAdjustments()))
                .field("solde_attendu", "Solde attendu", money(summary.expectedClosingAmount()))
                .field("solde_compte", "Solde compté", money(summary.countedClosingAmount()))
                .field("ecart", "Écart", money(summary.varianceAmount()))
                .field("motif_ecart", "Motif de l'écart", summary.varianceReason())
                .signer("caissier", "Caissier", summary.closedBy(), VoucherService.date(summary.closedAt()))
                .signer("superviseur", "Superviseur", reviewedBy, null);
        List<List<String>> rows = new ArrayList<>();
        for (CashMovementResponse movement : movements) {
            rows.add(List.of(movement.createdAt() == null ? "" : TIME.format(movement.createdAt()), nullToEmpty(movement.movementNumber()),
                    typeLabel.apply(movement), nullToEmpty(movement.counterpartyName() != null ? movement.counterpartyName() : movement.reason()),
                    signed(movement), VoucherService.money(movement.runningBalance())));
        }
        data.table("mouvements", new DocumentData.Table("Mouvements", List.of(new DocumentData.Column("heure", "Heure", false),
                new DocumentData.Column("numero", "N°", false), new DocumentData.Column("type", "Type", false),
                new DocumentData.Column("contrepartie", "Contrepartie ou motif", false), new DocumentData.Column("montant", "Montant", true),
                new DocumentData.Column("solde", "Solde", true)), rows));
        List<List<String>> nonCash = new ArrayList<>();
        if (summary.nonCashPayments() != null) {
            summary.nonCashPayments().forEach(total -> nonCash.add(List.of(nullToEmpty(total.label() != null ? total.label() : String.valueOf(total.paymentMethod())),
                    String.valueOf(total.count()), VoucherService.money(total.amount()))));
        }
        data.table("paiements_hors_especes", new DocumentData.Table("Paiements hors espèces", List.of(new DocumentData.Column("mode", "Mode", false),
                new DocumentData.Column("nombre", "Nombre", true), new DocumentData.Column("montant", "Montant", true)), nonCash));
        data.section("Session", List.of("session", "caisse", "etat", "ouvert_par", "ouvert_le", "cloture_par", "cloture_le", "revu_par"), null)
                .section("Comptes de la session", List.of("fonds_ouverture", "encaissements", "remboursements", "entrees", "sorties", "ajustements",
                        "solde_attendu", "solde_compte", "ecart", "motif_ecart"), null)
                .section("Mouvements", List.of(), "mouvements");
        if (!nonCash.isEmpty()) {
            data.section("Paiements hors espèces", List.of(), "paiements_hors_especes");
        }
        return data;
    }

    public static DocumentData sampleRequest() {
        CashRequestResponse request = new CashRequestResponse("CRQ-202611-000014", "CSS-202611-0042", "GUICHET-1", null, null,
                new BigDecimal("25000"), "XAF", "Achat urgent de piles pour les télécommandes des salles.", "A. Mabiala",
                Instant.parse("2026-11-12T08:30:00Z"), "P. Moussa", Instant.parse("2026-11-12T09:05:00Z"), "Accordé",
                "CSM-202611-000140", Instant.parse("2026-11-12T09:20:00Z"), List.of());
        return request(request, "Demande d'avance de caisse", "Exécutée");
    }

    public static DocumentData sampleClosing() {
        CashSessionSummaryResponse summary = new CashSessionSummaryResponse("CSS-202611-0042", "GUICHET-1", null, "A. Mabiala", "A. Mabiala",
                new BigDecimal("50000"), new BigDecimal("320000"), BigDecimal.ZERO, new BigDecimal("45000"), new BigDecimal("118000"),
                BigDecimal.ZERO, new BigDecimal("297000"), new BigDecimal("296500"), new BigDecimal("-500"), "Pièce de 500 introuvable",
                Instant.parse("2026-11-12T07:00:00Z"), Instant.parse("2026-11-12T17:30:00Z"), Instant.parse("2026-11-12T17:45:00Z"), List.of());
        return closing(summary, List.of(), "P. Moussa", movement -> "");
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "" : VoucherService.money(amount) + " XAF";
    }

    private static String signed(CashMovementResponse movement) {
        if (movement.amount() == null) {
            return "";
        }
        String value = VoucherService.money(movement.amount());
        return movement.flowDirection() == com.sni.bokaticowork.features.payment.enums.CashFlowDirection.OUT ? "-" + value : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
