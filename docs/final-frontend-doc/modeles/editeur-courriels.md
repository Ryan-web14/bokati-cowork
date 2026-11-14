# Éditeur des courriels (lots E1 et E2)

> Rédigé d'après le code (`EmailTemplates`, `EmailTemplateRegistry`, `DocumentTemplateService`, 14/11/2026).

Un modèle de la famille `EMAIL` remplace, une fois publié, le gabarit d'origine de son courriel (`fallbackTemplate`), sans changement dans le code d'envoi. Le cadre commun (en-tête, logo, pied) reste celui de la plateforme.

## Contenu

Même corps que pour une pièce (`PUT /document-templates/{code}/draft`), avec seulement `blocks`, `style` et `note` utiles :

```json
{ "blocks": [
    { "type": "SUBJECT", "text": "Votre facture {{documentNumber}}" },
    { "type": "PREHEADER", "text": "Votre facture est disponible" },
    { "type": "GREETING", "text": "Bonjour {{customerName}}," },
    { "type": "TEXT", "text": "Vous trouverez ci-joint votre facture." },
    { "type": "DETAILS", "title": "Récapitulatif", "rows": [ { "label": "Référence", "value": "{{documentNumber}}" } ] },
    { "type": "AMOUNT", "label": "Montant", "value": "{{totalAmount}}" },
    { "type": "BUTTON", "label": "Voir mon espace", "url": "https://espace.elleaose.com" },
    { "type": "LEGAL" } ],
  "note": "Ton plus direct" }
```

## Blocs

| Type | Options | Rôle |
| --- | --- | --- |
| `SUBJECT` | `text` | Objet du courriel ; un et un seul par modèle ; remplace l'objet écrit dans le code |
| `PREHEADER` | `text` | Aperçu affiché par la messagerie, invisible dans le corps |
| `TITLE` | `text` | Titre |
| `GREETING` | `text` | Salutation |
| `TEXT` | `text`, `tone` | Paragraphe ; les sauts de ligne sont gardés |
| `DETAILS` | `title`, `rows` (`{ label, value }`) | Tableau libellé / valeur |
| `AMOUNT` | `label`, `value` | Montant mis en avant |
| `BUTTON` | `label`, `url` | Bouton ; `url` en `https://`, `mailto:` ou un jeton |
| `ATTACHMENT_NOTE` | `text` | Mention de la pièce jointe |
| `SIGNATURE` | `text` | Signature |
| `LEGAL` | aucune | Mentions légales de l'exploitant |

## Jetons

- `{{variable}}` ou `{{objet.champ}}`. Les jetons permis sont ceux du courriel d'origine : `GET /document-templates/{code}/fields/catalog` les donne (`fields`, avec `sample` « ‹variable› »), ainsi que `blockTypes`.
- Un jeton inconnu de ce courriel est refusé à l'enregistrement ; les propriétés techniques (`class`, `classLoader`, `module`, `runtime`...) aussi.
- Le texte saisi n'est jamais interprété : une balise ou une expression saisie s'affiche telle quelle.
- **Jetons obligatoires** : un courriel de sécurité ne peut pas être enregistré ni publié sans eux. Le catalogue l'indique dans `previewNumberHint` (« Jetons obligatoires : ... ») :

  | Modèle | Jeton obligatoire |
  | --- | --- |
  | `EMAIL_OTT_LOGIN`, `EMAIL_EMAIL_CONFIRMATION` | `{{token}}` (le code) |
  | `EMAIL_PASSWORD_RESET` | `{{resetLink}}` |
  | `EMAIL_GENERATED_PASSWORD` | `{{generatedPassword}}` |
  | `EMAIL_CONTRACT_SIGNING_REQUEST` | `{{signingUrl}}` |
  | `EMAIL_QUOTE_SIGNATURE_REQUEST` | `{{signatureUrl}}` |
  | `EMAIL_PAYMENT_LINK` | `{{checkoutUrl}}` |

## Aperçu et essai

- `POST /document-templates/{code}/preview-email` : HTML (`text/html`), rendu avec des valeurs d'exemple « ‹variable› ». Corps facultatif : le contenu en cours d'édition ; sans corps, le brouillon, sinon la version publiée. À afficher dans un cadre isolé (`iframe` avec `sandbox`).
- `POST /document-templates/{code}/test-send` : envoie le courriel, objet préfixé « [Essai] », à l'adresse de l'utilisateur connecté. Réponse `{ "sentTo": "..." }`. Sans adresse : `ADRESSE_ABSENTE` ; échec d'envoi : `ENVOI_IMPOSSIBLE`.
- Un courriel qui ne se rend pas avec les valeurs d'exemple n'est pas publié (`MODELE_INVALIDE`).

Les 64 courriels inscrits au lot E2 n'ont pas de version : l'éditeur part d'un brouillon vide et de la liste des jetons du catalogue.
