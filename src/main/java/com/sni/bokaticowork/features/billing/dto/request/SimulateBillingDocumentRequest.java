package com.sni.bokaticowork.features.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Charge utile d'une simulation.
 *
 * <p>Volontairement plus permissive que {@link CreateBillingDocumentRequest} : simuler n'engage
 * rien et ne cree aucun document, donc exiger un type de document, une devise ou un destinataire
 * n'a pas de sens. Seules les lignes comptent · ce sont elles qui portent le calcul.
 *
 * <p>L'interface peut continuer d'envoyer la charge utile complete de la creation : c'est le seul
 * point d'entree ou les proprietes inconnues restent ignorees, et c'est delibere · l'API refuse
 * partout ailleurs, mais imposer ici de retirer les champs du document empecherait justement le
 * parcours prevu, simuler a chaque saisie puis envoyer la meme charge utile pour de bon.
 *
 * @param documentType facultatif · repris tel quel dans la reponse, sinon {@code INVOICE}
 * @param currency     facultatif · repris tel quel dans la reponse, sinon la devise par defaut
 * @param title        facultatif · repris tel quel, pour que l'apercu affiche son en-tete
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SimulateBillingDocumentRequest(
        BillingDocumentType documentType,
        String currency,
        String title,
        @Valid @NotEmpty List<CreateBillingDocumentLineRequest> lines,
        @Valid List<CreateBillingDocumentDiscountRequest> discounts
) {

    /** Type retenu · l'immense majorite des simulations portent sur une facture. */
    public BillingDocumentType resolvedDocumentType() {
        return documentType == null ? BillingDocumentType.INVOICE : documentType;
    }

    public String resolvedCurrency(String defaultCurrency) {
        return currency == null || currency.isBlank() ? defaultCurrency : currency.trim();
    }

    /** Jamais {@code null} · le calcul tolere l'absence de remise document. */
    public List<CreateBillingDocumentDiscountRequest> resolvedDiscounts() {
        return discounts == null ? List.of() : discounts;
    }
}
