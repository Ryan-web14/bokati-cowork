# Centre des modèles de documents et de courriels

> Rédigé d'après le code (lots D1 à D5, E1, E2 ; new-main 0044b67e, 14/11/2026). Routes exactes et champs JSON : [../document/routes/DocumentTemplateController.md](../document/routes/DocumentTemplateController.md) et [../document/routes/DocumentFieldController.md](../document/routes/DocumentFieldController.md) (les deux contrôleurs sont rangés dans le module `document`).

L'exploitant adapte lui-même ses pièces imprimées (PDF) et ses courriels, sans développement : il compose un modèle en blocs, le prévisualise, le publie. Tant qu'un modèle n'a pas de version publiée, la pièce ou le courriel sort de son gabarit d'origine, comme avant.

## Fichiers

| Je cherche | Je lis |
| --- | --- |
| Liste, cycle de vie, versions, droits (ce fichier) | ici |
| Éditeur des pièces PDF : blocs, formats, exemplaires, jetons, catalogue, aperçu | [editeur-pieces.md](editeur-pieces.md) |
| Champs ajoutés (imprimés vides ou saisis) et leur saisie sur les pièces | [champs-ajoutes.md](champs-ajoutes.md) |
| Éditeur des courriels : blocs, jetons permis, jetons obligatoires, aperçu, envoi d'essai | [editeur-courriels.md](editeur-courriels.md) |
| Codes d'erreur et messages | [erreurs.md](erreurs.md) |

## Droits

Toutes les routes `/sni/api/v1/document-templates/**` demandent le droit `SYSTEM:DOCUMENT_TEMPLATES` (rôles `SUPER_ADMIN` et `ADMIN` par défaut). La saisie des champs ajoutés sur une pièce suit les droits de la pièce (caisse, fonds), voir [champs-ajoutes.md](champs-ajoutes.md).

## Les modèles

`GET /document-templates` : liste `{ code, label, family, domain, active, publishedVersion, publishedAt, hasDraft }`.

- `family` : `PDF` (pièce imprimée) ou `EMAIL` (courriel). L'écran sépare les deux familles : l'éditeur n'est pas le même.
- `domain` : regroupement à afficher (`CASH`, `FUND`, `ACCOUNTING`, `BILLING`, `PAYMENT`, `BOOKING`, `SUBSCRIPTION`, `WALLET`, `DOMICILIATION`, `CONTRACT`, `DOCUMENT`, `SECURITY`, `SUPPORT`, `TASK`, `CRM`, `CLIENT`, `EVENT`, `SYSTEM`).
- `publishedVersion` nul : le gabarit d'origine sert.

État de départ :

| Famille | Modèles | État livré |
| --- | --- | --- |
| PDF | Bon de sortie, bon d'entrée, demande de fonds, état de besoin, demande de caisse | Publiés |
| PDF | Bon de décaissement, fiche de justification, bon de commande, déclaration de TVA, état de rapprochement, tableau d'emprunt, bordereau de remise en banque, relevés fournisseur et partenaire, rapport de clôture de caisse | Brouillon de départ, à relire et publier |
| PDF | Facture (et devis, proforma, note de débit, facture rectificative), avoir, reçu de paiement, relevé et reçu de portefeuille, confirmation de réservation | Brouillon de départ ; comparer l'aperçu sur des factures réelles avant de publier la facture |
| EMAIL | Document de facturation, paiement reçu | Brouillon de départ |
| EMAIL | 64 autres courriels (facturation, paiement, réservation, abonnements, portefeuille, domiciliation, sécurité...) | Inscrits sans version : à rédiger |

## Fiche d'un modèle

`GET /document-templates/{code}` : `{ code, label, family, domain, fallbackTemplate, description, active, published, draft, versions[] }`.

- `published` et `draft` : une version complète `{ versionNumber, status, pageFormat, copies, style, blocks, fields, note, createdBy, createdAt, updatedAt, publishedBy, publishedAt, archivedAt }`, ou `null`.
- `versions` : historique `{ versionNumber, status, note, createdBy, createdAt, publishedBy, publishedAt, archivedAt }`.

## Cycle de vie

Une version est `DRAFT` (un seul brouillon par modèle), `PUBLISHED` (une seule) ou `ARCHIVED`.

| Geste | Route | Effet |
| --- | --- | --- |
| Enregistrer le brouillon | `PUT /document-templates/{code}/draft` | Crée ou remplace le brouillon. Corps : le contenu (voir les éditeurs). Contrôlé à l'enregistrement : `400 MODELE_INVALIDE` avec la liste des erreurs. |
| Supprimer le brouillon | `DELETE /document-templates/{code}/draft` | `204`. |
| Prévisualiser | `POST /document-templates/{code}/preview` (PDF) ou `/preview-email` (HTML) | Rien n'est écrit. Corps facultatif : le contenu en cours d'édition ; sans corps, le brouillon, sinon la version publiée. |
| Publier | `POST /document-templates/{code}/publish` | Le brouillon devient la version publiée, l'ancienne est archivée. Un modèle qui ne se rend pas avec les données d'exemple n'est pas publié. Prend effet en moins d'une minute. |
| Restaurer une ancienne version | `POST /document-templates/{code}/restore/{version}` | La copie dans le brouillon, à relire puis publier. |
| Revenir à l'original | `POST /document-templates/{code}/reset` | La version publiée est archivée : le gabarit d'origine sert de nouveau. |

Écran conseillé : liste par famille et domaine, puis une fiche avec trois zones : éditeur du brouillon, aperçu (rafraîchi à la demande), historique avec « restaurer ».

## Ce que le changement d'un modèle touche

- Une pièce de caisse ou de fonds imprimée après la publication prend le nouveau modèle, réimpressions comprises.
- Une facture ou un avoir certifié garde la version de modèle qui l'a produit (`billing_document.template_version`, `0` pour le gabarit d'origine) : une réimpression ou un duplicata ressort à l'identique, même après une nouvelle publication.
- Un courriel publié remplace son gabarit à l'envoi suivant ; l'objet du modèle remplace celui du code.
