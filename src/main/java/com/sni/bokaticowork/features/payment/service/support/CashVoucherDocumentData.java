package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Donnees des bons d'entree et de sortie de caisse pour les modeles de documents (lot D2).
 *
 * <p>Cles : {@code date_heure}, {@code caisse}, {@code session}, {@code caisse_session},
 * {@code moyen}, {@code beneficiaire} (sortie) ou {@code deposant} (entree), {@code contrepartie},
 * {@code nature}, {@code categorie}, {@code reference}, {@code piece_justificative},
 * {@code demande_liee}, {@code motif}, {@code emis_par}, {@code signe_par}. Signataires :
 * {@code caissier}, {@code responsable}, et {@code beneficiaire} ou {@code deposant} (case vide).</p>
 */
public final class CashVoucherDocumentData {

    public static final String CASH_OUT = "CASH_OUT_VOUCHER";
    public static final String CASH_IN = "CASH_IN_VOUCHER";
    private static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm").withZone(ZONE);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZONE);

    private CashVoucherDocumentData() {
    }

    /** Modele d'un mouvement : bon de sortie, bon d'entree, ou aucun (le gabarit d'origine sert). */
    public static String templateCode(CashMovementResponse movement) {
        if (movement.documentType() == CashDocumentType.EXIT_VOUCHER || movement.movementType() == CashMovementType.CASH_OUT) {
            return CASH_OUT;
        }
        if (movement.documentType() == CashDocumentType.ENTRY_VOUCHER || movement.movementType() == CashMovementType.CASH_IN) {
            return CASH_IN;
        }
        return null;
    }

    public static DocumentData from(String code, CashMovementResponse movement, String title, boolean duplicate, String reversedBy) {
        boolean out = CASH_OUT.equals(code);
        CashMovementResponse.Accounting accounting = movement.accounting();
        String party = firstNonBlank(accounting == null ? null : accounting.beneficiaryName(), movement.counterpartyName(),
                movement.relatedCustomerName(), movement.counterpartyCode());
        String request = firstNonBlank(accounting == null ? null : accounting.requestNumber(), movement.transferOrderNumber());
        DocumentData data = new DocumentData(code).title(title).number(movement.movementNumber()).source(movement.movementNumber())
                .amount(movement.amount(), movement.currency())
                .duplicate(duplicate, "DUPLICATA").cancelledBy(reversedBy)
                .issued(movement.createdAt() == null ? "" : VoucherService.date(movement.createdAt()), movement.createdBy())
                .field("date_heure", "Date et heure", movement.createdAt() == null ? "" : STAMP.format(movement.createdAt()))
                .field("caisse", "Caisse", movement.registerName())
                .field("session", "Session", movement.sessionNumber())
                .field("caisse_session", "Caisse · session", join(movement.registerName(), movement.sessionNumber()))
                .field("moyen", "Moyen", "Espèces " + (movement.currency() == null ? "" : movement.currency()))
                .field(out ? "beneficiaire" : "deposant", out ? "Bénéficiaire" : "Déposant", party)
                .field("contrepartie", "Contrepartie", party)
                .field("nature", "Nature", firstNonBlank(movement.relatedSourceLabel(), movement.natureCode(), movement.flowCategory()))
                .field("categorie", "Catégorie", movement.flowCategory())
                .field("reference", "Référence", movement.referenceCode())
                .field("piece_justificative", "Pièce justificative", movement.documentNumber())
                .field("demande_liee", out ? "Demande liée" : "Pièce réglée",
                        out ? firstNonBlank(request, movement.referenceCode()) : firstNonBlank(movement.relatedSourceCode(), movement.referenceCode(), request))
                .field("motif", "Motif", movement.reason())
                .field("emis_par", "Émis par", movement.createdBy())
                .field("signe_par", "Signé par", movement.signedBy())
                .signer("caissier", "Caissier", movement.createdBy(), day(movement.createdAt()))
                .signer("responsable", "Responsable", firstNonBlank(movement.signedBy(), movement.approvedBy()),
                        day(movement.signedAt() != null ? movement.signedAt() : movement.approvedAt()))
                .signer(out ? "beneficiaire" : "deposant", out ? "Bénéficiaire" : "Déposant", null, null);
        return data;
    }

    /** Bon de sortie d'exemple, celui des maquettes validees. */
    public static DocumentData sample(String code) {
        boolean out = CASH_OUT.equals(code);
        Instant at = LocalDate.of(2026, 11, 12).atTime(out ? 10 : 15, out ? 42 : 8).atZone(ZONE).toInstant();
        BigDecimal amount = new BigDecimal(out ? "118000" : "45000");
        String party = out ? "Papeterie du Centre" : "Société Kintele SARL";
        return new DocumentData(code).title(out ? "Bon de sortie" : "Bon d'entrée").number(out ? "CSM-202611-000127" : "CSM-202611-000318")
                .amount(amount, "XAF").issued(VoucherService.date(at), "A. Mabiala")
                .field("date_heure", "Date et heure", STAMP.format(at))
                .field("caisse", "Caisse", "Guichet accueil")
                .field("session", "Session", "CSS-202611-0042")
                .field("caisse_session", "Caisse · session", "Guichet accueil · CSS-202611-0042")
                .field("moyen", "Moyen", "Espèces XAF")
                .field(out ? "beneficiaire" : "deposant", out ? "Bénéficiaire" : "Déposant", party)
                .field("contrepartie", "Contrepartie", party)
                .field("nature", "Nature", out ? "Achat de fournitures de bureau" : "Règlement de facture")
                .field("categorie", "Catégorie", out ? "EXPENSE" : "PAYMENT")
                .field("reference", "Référence", out ? "DDF-202611-000031" : "FAC-2026-000042")
                .field("piece_justificative", "Pièce justificative", out ? "F-2026-118" : "")
                .field("demande_liee", out ? "Demande liée" : "Pièce réglée", out ? "DDF-202611-000031" : "FAC-2026-000042")
                .field("motif", "Motif", out ? "Ramettes de papier et cartouches pour l'accueil (facture fournisseur F-2026-118 jointe)."
                        : "Règlement partiel de la facture FAC-2026-000042.")
                .field("emis_par", "Émis par", "A. Mabiala")
                .field("signe_par", "Signé par", "")
                .signer("caissier", "Caissier", "A. Mabiala", "12/11/2026")
                .signer("responsable", "Responsable", null, null)
                .signer(out ? "beneficiaire" : "deposant", out ? "Bénéficiaire" : "Déposant", null, null);
    }

    private static String day(Instant instant) {
        return instant == null ? null : DAY.format(instant);
    }

    private static String join(String first, String second) {
        if (first == null || first.isBlank()) {
            return second == null ? "" : second;
        }
        return second == null || second.isBlank() ? first : first + " · " + second;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
