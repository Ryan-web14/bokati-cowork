# QuoteSignaturePublicController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [billing](../README.md) · 2 route(s).

## Routes

<a id="r-get-public-quotes-sign-token"></a>
### GET /public/quotes/sign/{token}

- Accès : public, sans jeton
- Renvoie : `text/html`
- Réponse : `String`
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteSignaturePublicController.java`, méthode `signaturePage`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |

<a id="r-post-public-quotes-sign-token-confirm"></a>
### POST /public/quotes/sign/{token}/confirm

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `ConfirmSignatureRequest`
- Réponse : `PublicSignatureConfirmation`
- Modèles : [PublicSignatureConfirmation](#m-PublicSignatureConfirmation), [ConfirmSignatureRequest](#m-ConfirmSignatureRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/billing/controller/QuoteSignaturePublicController.java`, méthode `confirmSignature`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-PublicSignatureConfirmation"></a>
#### PublicSignatureConfirmation

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `quoteNumber` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `signatureStatus` | texte |  |  |
| `signerName` | texte |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `revision` | entier |  |  |

<a id="m-ConfirmSignatureRequest"></a>
#### ConfirmSignatureRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signerName` | texte | oui | longueur min `1` |
| `signerEmail` | texte |  |  |
| `signatureImageBase64` | texte | oui | longueur min `1` |
| `ipAddress` | texte |  |  |
| `userAgent` | texte |  |  |
