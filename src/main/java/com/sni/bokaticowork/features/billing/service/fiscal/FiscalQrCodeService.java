package com.sni.bokaticowork.features.billing.service.fiscal;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Construit le contenu du QR code fiscal SEFC.
 *
 * <p>Toujours une URL de vérification, jamais une charge utile brute. Le QR d'un document scellé
 * portait auparavant un JSON compact : scanné avec un téléphone, il affichait du texte que
 * personne n'allait recopier à la main. Le document qui avait le plus besoin d'être vérifiable
 * par un tiers était précisément celui qu'on ne pouvait pas vérifier.
 *
 * <p>Sur un document scellé, l'URL porte en outre {@code k}, le préfixe de la signature fiscale.
 * Il prouve la détention du document et débloque l'affichage détaillé de la page de vérification.
 * C'est bien la <b>signature</b> qui est transportée, pas le hash : le hash se recalcule à partir
 * de données imprimées sur la facture, donc connues d'un faussaire, alors que produire la
 * signature exige la clé HMAC du serveur.
 */
@Service
public class FiscalQrCodeService {

    /** 16 caractères hexadécimaux, soit 64 bits · assez contre la devinette, et le QR reste dense. */
    private static final int SIGNATURE_PREFIX_LENGTH = 16;

    @Value("${app.verify-base-url:https://api.elleaose.com}")
    private String verifyBaseUrl;

    /**
     * URL de vérification du document. Un document scellé y ajoute le préfixe de signature.
     */
    public String buildContent(BillingDocumentResponse doc) {
        String url = verifyBaseUrl.stripTrailing() + "/verify/doc/" + doc.documentNumber();
        if (doc.locked() != null && doc.locked()
                && StringUtils.hasText(doc.fiscalNumber())
                && StringUtils.hasText(doc.fiscalSignature())) {
            return url + "?k=" + abbrev(doc.fiscalSignature(), SIGNATURE_PREFIX_LENGTH);
        }
        return url;
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
