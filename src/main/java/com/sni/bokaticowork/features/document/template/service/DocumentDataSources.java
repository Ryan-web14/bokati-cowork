package com.sni.bokaticowork.features.document.template.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.document.template.render.DocumentData;
import com.sni.bokaticowork.features.fund.voucher.FundVoucherSamples;
import com.sni.bokaticowork.features.fund.voucher.FundVoucherService;
import com.sni.bokaticowork.features.payment.service.interfaces.CashDocumentPdfService;
import com.sni.bokaticowork.features.payment.service.support.CashVoucherDocumentData;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * Donnees des pieces pour l'apercu des modeles (lot D2) : une piece d'exemple, ou une piece reelle
 * designee par son numero, lue sans rien ecrire (ni numero attribue, ni impression tracee).
 */
@Component
public class DocumentDataSources {

    private static final Set<String> CASH = Set.of(CashVoucherDocumentData.CASH_OUT, CashVoucherDocumentData.CASH_IN);
    private static final Set<String> FUND = Set.of("NEED_STATEMENT", "FUND_REQUEST", "DISBURSEMENT_VOUCHER", "JUSTIFICATION_SHEET");
    /** Numero attendu pour l'apercu avec une piece reelle. */
    private static final Map<String, String> HINTS = Map.of(
            CashVoucherDocumentData.CASH_OUT, "Numéro du mouvement de caisse (CSM-...)",
            CashVoucherDocumentData.CASH_IN, "Numéro du mouvement de caisse (CSM-...)",
            "NEED_STATEMENT", "Numéro de l'état de besoin (EDB-...)",
            "FUND_REQUEST", "Numéro de la demande de fonds (DDF-...)",
            "DISBURSEMENT_VOUCHER", "DEMANDE/MOUVEMENT (DDF-.../CSM-...)",
            "JUSTIFICATION_SHEET", "Numéro de la demande d'avance (DDF-...)");

    private final ObjectProvider<CashDocumentPdfService> cash;
    private final ObjectProvider<FundVoucherService> fund;

    public DocumentDataSources(ObjectProvider<CashDocumentPdfService> cash, ObjectProvider<FundVoucherService> fund) {
        this.cash = cash;
        this.fund = fund;
    }

    public boolean supports(String code) {
        return CASH.contains(code) || FUND.contains(code);
    }

    public String hint(String code) {
        return HINTS.get(code);
    }

    public DocumentData sample(String code) {
        if (CASH.contains(code)) {
            return CashVoucherDocumentData.sample(code);
        }
        if (FUND.contains(code)) {
            return FundVoucherSamples.sample(code);
        }
        throw BadRequestException.coded("APERCU_INDISPONIBLE", "Aucune donnée d'exemple pour le modèle " + code);
    }

    public DocumentData load(String code, String number) {
        if (CASH.contains(code)) {
            return cash.getObject().previewData(code, number.trim());
        }
        if (FUND.contains(code)) {
            return fund.getObject().previewData(code, number.trim());
        }
        throw BadRequestException.coded("APERCU_INDISPONIBLE", "L'aperçu avec une pièce réelle n'existe pas encore pour le modèle " + code);
    }
}
