package com.sni.bokaticowork.features.billing.service.fiscal;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Construit le contenu du QR code fiscal SEFC.
 * Le QR contient les données immuables du document validé, vérifiables
 * sans connexion à l'API (numéro, date, montant, empreinte partielle).
 */
@Service
public class FiscalQrCodeService {

    @Value("${app.verify-base-url:https://api.elleaose.com}")
    private String verifyBaseUrl;

    /**
     * Retourne le contenu à encoder dans le QR :
     * - Document validé → payload fiscal compact (JSON)
     * - Document non validé → URL de vérification standard
     */
    public String buildContent(BillingDocumentResponse doc) {
        if (doc.locked() != null && doc.locked()
                && StringUtils.hasText(doc.fiscalNumber())) {
            return buildFiscalPayload(doc);
        }
        return verifyBaseUrl.stripTrailing() + "/verify/doc/" + doc.documentNumber();
    }

    /**
     * Payload fiscal compact :
     * {"n":"FAC20260617-00001abc-BKTWK","d":"2026-06-17","t":295000,"h":"a1b2c3d4...","s":"e5f6a7b8..."}
     * Les 16 premiers caractères du hash et de la signature suffisent à la vérification visuelle.
     */
    private String buildFiscalPayload(BillingDocumentResponse doc) {
        return String.format(
                "{\"n\":\"%s\",\"d\":\"%s\",\"t\":%s,\"h\":\"%s\",\"s\":\"%s\"}",
                doc.fiscalNumber(),
                doc.fiscalDate() != null ? doc.fiscalDate() : "",
                doc.totalAmount() != null ? doc.totalAmount().toPlainString() : "0",
                abbrev(doc.currentHash(), 16),
                abbrev(doc.fiscalSignature(), 16)
        );
    }

    private String abbrev(String value, int len) {
        if (value == null || value.isBlank()) return "";
        return value.length() <= len ? value : value.substring(0, len);
    }
}
