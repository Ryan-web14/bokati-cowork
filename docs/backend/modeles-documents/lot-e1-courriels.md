# Lot E1 : courriels en blocs, socle

Branche `feature/modeles-documents`. Migrations : dev `V354__modeles_documents_e1_courriels.sql`, prod `V348__modeles_documents_e1_courriels.sql`.

## Ce qui change pour l'exploitant

- **Un courriel devient un modèle comme une pièce PDF** (famille `EMAIL`), avec brouillon, aperçu, publication, versions, restauration et retour à l'original.
- **Un courriel publié remplace son gabarit d'origine sans toucher au code d'envoi.** Le service qui envoie demande toujours `email/billing-document` ; s'il existe un modèle publié pour ce gabarit, c'est lui qui est rendu.
- **L'objet du modèle remplace celui du code.** Sans bloc d'objet publié, l'objet écrit dans le code reste.
- **Deux brouillons de départ, non publiés :** `EMAIL_BILLING_DOCUMENT` (envoi d'une pièce de facturation) et `EMAIL_PAYMENT_CONFIRMATION` (paiement reçu). Rien ne change à l'envoi tant qu'ils ne sont pas publiés.
- **Aperçu et envoi d'essai :** l'aperçu rend le HTML avec des valeurs d'exemple « ‹variable› » ; l'envoi d'essai part à l'adresse de l'utilisateur connecté, objet préfixé « [Essai] ».

## Blocs

| Type | Contenu |
| --- | --- |
| `SUBJECT` | Objet du courriel ; un et un seul par modèle |
| `PREHEADER` | Texte d'aperçu affiché par la messagerie, invisible dans le corps |
| `TITLE` | Titre |
| `GREETING` | Salutation (« Bonjour {{customerName}}, ») |
| `TEXT` | Paragraphe ; les sauts de ligne sont gardés |
| `DETAILS` | Tableau libellé / valeur (`rows`) |
| `AMOUNT` | Montant mis en avant |
| `BUTTON` | Bouton de lien (`label`, `url` en `https://`, `mailto:` ou jeton) |
| `ATTACHMENT_NOTE` | Mention de la pièce jointe |
| `SIGNATURE` | Signature |
| `LEGAL` | Mentions légales de l'exploitant |

Le cadre commun (en-tête, logo, pied) reste celui des courriels de la plateforme.

## Jetons et sécurité

- Un jeton s'écrit `{{variable}}` ou `{{objet.champ}}`.
- **Seules les variables du courriel d'origine sont permises.** Elles sont lues dans le gabarit Thymeleaf d'origine ; un jeton inconnu refuse l'enregistrement (« jeton inconnu de ce courriel »).
- **Propriétés interdites :** `class`, `classLoader`, `module`, `runtime` et voisines sont refusées, à tout niveau du chemin.
- **Le texte saisi n'est jamais interprété.** Il est échappé, y compris `[ ] $ { } |` : une expression Thymeleaf ou une balise HTML saisie s'affiche telle quelle. Un jeton devient une lecture sûre (`${a?.b}`), une valeur absente rend un vide.
- **Jetons obligatoires** (colonne `required_tokens`) : un courriel de sécurité (code, lien de réinitialisation) ne peut pas être publié sans eux. Aucun courriel de E1 n'en a ; ils servent au lot E2.
- **Un modèle qui ne se rend pas avec les valeurs d'exemple n'est pas publié.**

## Fonctionnement

- `EmailTemplateRegistry` garde les gabarits générés des modèles publiés, relus au plus une fois par minute et aussitôt après une publication. Quand l'ensemble change, le cache de Thymeleaf est vidé, sinon un courriel déjà envoyé une fois resterait servi par son fichier d'origine.
- `EmailTemplateOverrideResolver` est un résolveur Thymeleaf d'ordre 0 : il sert le gabarit généré à la place du fichier, sinon il laisse passer au résolveur des fichiers.
- `EmailSubjectOverride` lit l'objet dans un élément caché `x-email-subject` du HTML rendu et le retire avant l'envoi ; il est appliqué dans les 15 méthodes publiques de `DefaultEmailSender`.
- Une lecture en échec des modèles laisse les gabarits d'origine servir.

## Routes (droit `SYSTEM:DOCUMENT_TEMPLATES`)

| Route | Effet |
| --- | --- |
| `POST /document-templates/{code}/preview-email` | Aperçu HTML : blocs du corps, sinon brouillon, sinon version publiée |
| `POST /document-templates/{code}/test-send` | Envoi d'essai à l'utilisateur connecté ; réponse `{ "sentTo": ... }` |
| `GET /document-templates/{code}/fields/catalog` | Pour un courriel : les variables permises et les types de blocs |

Les autres routes (brouillon, publication, restauration, retour à l'original) sont celles du lot D2.

## Tests

- `EmailTemplatesE1Test` : contrôle de chaque erreur, rendu d'un courriel publié à la place du fichier, texte saisi non interprété, objet du modèle appliqué, variables lues dans le gabarit d'origine.
- `DocumentTemplateE1IT` : brouillons de départ, aperçu, publication et envoi d'essai sur une vraie base.
