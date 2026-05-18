package com.sni.bokaticowork.features.billing.service.interfaces;

import com.sni.bokaticowork.features.billing.dto.request.ConfirmSignatureRequest;
import com.sni.bokaticowork.features.billing.dto.request.RequestSignatureRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentSignatureResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSignature;

public interface QuoteSignatureService {
    /** Génère un token, persiste la demande et envoie l'email au client. */
    BillingDocumentSignatureResponse requestSignature(String quoteNumber, RequestSignatureRequest request);
    /** Résout le token et retourne les données nécessaires à la page publique. */
    BillingDocumentSignature resolveToken(String token);
    /** Valide la signature, met le devis en ACCEPTED et retourne le document mis à jour. */
    BillingDocumentResponse confirmSignature(String token, ConfirmSignatureRequest request);
}
