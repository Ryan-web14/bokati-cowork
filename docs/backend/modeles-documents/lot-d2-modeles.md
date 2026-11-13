# Lot D2 : modèles en base

Branche `feature/modeles-documents`. Migrations : dev `V350__modeles_documents_d2.sql`, prod `V344__modeles_documents_d2.sql`.

## Ce qui change pour l'exploitant

- Les bons de sortie et d'entrée de caisse, la demande de fonds et l'état de besoin s'impriment avec le modèle validé (maquettes du 12/11/2026).
  - Les bons de caisse sortent en deux exemplaires A5 sur une feuille A4, avec une ligne de découpe. L'original reste à la caisse ; la copie va au bénéficiaire ou au déposant.
  - Les cases de signature mesurent 160 x 164 px et sont centrées. Au-delà de quatre cases, elles passent à la rangée suivante.
  - La case du cachet porte le nom de l'exploitant, tiré de sa fiche (jeton `{{exploitant}}`).
- Chaque modèle se modifie depuis le back-office : brouillon, aperçu PDF, publication. Une ancienne version peut être reprise, et le modèle peut revenir au gabarit d'origine.
- **Repli sur le gabarit d'origine :** sans version publiée, ou si le rendu d'une version échoue, la pièce sort comme avant. L'échec est journalisé. Une pièce de caisse sort donc toujours.
- **Libellés en français :** sur les pièces de fonds, états et décisions s'impriment en français (« Approuvé », « Transformé »…) au lieu des valeurs brutes, et les dates simples au format `14/11/2026`. Ce changement vaut aussi pour le gabarit d'origine.

## Principe

Un modèle est une suite de blocs JSON. Chaque bloc est rendu par un fragment fixe (`templates/document-template/render.html`) : aucun HTML ni aucune expression ne vient de l'utilisateur. Les textes des blocs acceptent des jetons `{{clé}}`. Une valeur substituée reste du texte : elle est échappée à l'impression et jamais interprétée.

| Bloc | Options |
| --- | --- |
| `HEADER` | `qr` (le QR s'imprime seulement si la pièce est vérifiable) |
| `TITLE` | `title` (sinon le titre de la pièce), `copyLabel` (mention de l'exemplaire) |
| `FIELDS` | `title`, `columns` (1 à 4), `fields` : des clés, ou `{key, label}` pour changer le libellé |
| `LINES` | `title`, `table` (clé du tableau), `columns` : des clés, ou `{key, label, width}` ; sans `columns`, toutes les colonnes |
| `TOTALS` | `label`, `words` (montant en lettres), `wordsLabel` |
| `TEXT` | `title`, `text` (avec jetons), `boxed` |
| `SIGNATURES` | `boxes` : `{label, signer, note, stamp}` ; `fromData` (tous les signataires de la pièce) ; `width` (80 à 260 px), `height` (60 à 260 px) |
| `FOOTER` | `text` ; le numéro, l'adresse de vérification et l'identité de l'exploitant s'impriment toujours |

**Contenu d'une version :**
- `pageFormat` : `A4`, `A5_TWO_UP` (deux exemplaires par feuille) ou `A6` ;
- `copies` : au plus quatre, chacune avec sa mention ;
- `style.accent` : couleur du filet, au format `#RRGGBB` ;
- `blocks` : au plus quarante.

Le contrôle (`TemplateContent.validate`) refuse un type ou une option inconnus, une clé mal formée ou une taille hors bornes. Il nomme chaque erreur.

## Clés des pièces

Le catalogue (`GET /document-templates/{code}/fields/catalog`) donne les clés de chaque pièce avec une valeur d'exemple. Il donne aussi les jetons communs : `numero`, `titre`, `montant`, `date_emission`, `emis_par`, `exploitant`.

- **Bons de caisse :** les clés sont fixées par `CashVoucherDocumentData`.
  - Champs : `date_heure`, `caisse_session`, `moyen`, `beneficiaire` ou `deposant`, `nature`, `demande_liee`, `motif`...
  - Signataires : `caissier`, `responsable`.
- **Pièces de fonds :** les clés dérivent des libellés de `FundVoucherService`, en minuscules sans accents (« Date souhaitée » donne `date_souhaitee`).
  - Une clé en double dans une autre section est préfixée par la section.
  - Tableaux : clé du titre de section (`lignes`, `besoins`, `circuit_de_validation`).
  - Signataires : `demandeur`, `niveau_1`, `niveau_2`...
  - `FundVoucherSamplesTest` vérifie que les pièces d'exemple ont les clés des pièces réelles.

## Données

- **`document_template` :**
  - colonnes : code, libellé, famille (`PDF`, `EMAIL`), domaine, gabarit de repli, actif ;
  - modèles enregistrés : `CASH_OUT_VOUCHER`, `CASH_IN_VOUCHER`, `FUND_REQUEST`, `NEED_STATEMENT`, `DISBURSEMENT_VOUCHER`, `JUSTIFICATION_SHEET` ;
  - les deux derniers n'ont pas de version publiée : ils sortent de leur gabarit d'origine jusqu'au lot D4 ou jusqu'à une publication.
- **`document_template_version` :**
  - colonnes : numéro, statut (`DRAFT`, `PUBLISHED`, `ARCHIVED`), format, exemplaires, style et blocs (JSON), note, auteur, publication, archivage ;
  - des index partiels garantissent un seul brouillon et une seule version publiée par modèle.
- **Versions de départ :** les quatre pièces maquettées sont publiées en version 1 par la migration.

## Routes (back-office, droit `SYSTEM:DOCUMENT_TEMPLATES`, donné à `SUPER_ADMIN` et `ADMIN`)

| Route | Rôle |
| --- | --- |
| `GET /document-templates` | Liste : version publiée, brouillon en cours |
| `GET /document-templates/{code}` | Fiche : version publiée, brouillon, historique |
| `GET /document-templates/{code}/fields/catalog` | Clés utilisables, valeurs d'exemple, types de blocs, formats |
| `PUT /document-templates/{code}/draft` | Enregistre le brouillon (créé ou remplacé), après contrôle |
| `DELETE /document-templates/{code}/draft` | Abandonne le brouillon |
| `POST /document-templates/{code}/preview?number=` | PDF d'aperçu (voir ci-dessous) ; rien n'est écrit |
| `POST /document-templates/{code}/publish` | Publie le brouillon ; la version publiée précédente passe aux archives |
| `POST /document-templates/{code}/restore/{version}` | Recopie une ancienne version dans le brouillon |
| `POST /document-templates/{code}/reset` | Retour au gabarit d'origine : la version publiée est archivée |

**Aperçu :**
- contenu rendu : celui du corps de la requête s'il a des blocs, sinon le brouillon, sinon la version publiée ;
- données : la pièce `number` si elle est donnée, sinon les données d'exemple. Pour un bon de décaissement, `number` vaut `DEMANDE/MOUVEMENT`.

**Publication :** un brouillon qui ne se rend pas avec les données d'exemple est refusé.

**Codes d'erreur :** `MODELE_INVALIDE`, `BROUILLON_ABSENT`, `AUCUNE_VERSION_PUBLIEE`, `AUCUN_CONTENU`, `APERCU_INDISPONIBLE`, `VERSION_BROUILLON`.

Les actions qui modifient sont tracées au journal d'audit (module `DOCUMENT_TEMPLATE`).

## Composants

| Composant | Rôle |
| --- | --- |
| `document/template/render/DocumentData` | Données d'une pièce : champs, tableaux, signataires, montant, mentions |
| `document/template/render/TemplateContent` | Contenu d'une version et son contrôle |
| `document/template/render/TemplateRenderer` | Résout les blocs en vues de texte, puis rend le gabarit fixe en PDF |
| `document/template/service/DocumentTemplatePrinter` | Impression avec la version publiée (gardée une minute en mémoire, vidée à la publication), repli sinon |
| `document/template/service/DocumentTemplateService` | Brouillon, publication, restauration, retour au gabarit, aperçu, catalogue |
| `document/template/service/DocumentDataSources` | Données d'exemple et pièces réelles pour l'aperçu |
| `fund/voucher/VoucherService` | `view` (vue sans rendu), `peek` (pièce sans numéro attribué), `render` essaie d'abord le modèle |
| `fund/voucher/FundVoucherService` | Pièces construites une fois, imprimées ou montrées en aperçu (`previewData`) |
| `payment/service/support/CashVoucherDocumentData` | Données des bons de caisse ; `CashDocumentPdfServiceImpl` essaie d'abord le modèle |

## Tests

- `DocumentTemplateD2IT` (schéma de production) :
  - les quatre versions de départ sont valides et se rendent ;
  - les bons de caisse tiennent sur une feuille ;
  - les signataires des niveaux de validation sont reportés dans leurs cases ;
  - cycle complet : brouillon, publication, nouvelle version, restauration, retour au gabarit ;
  - un bloc inconnu est refusé.
- `TemplateD2Test` :
  - erreurs de contrôle nommées ;
  - jetons non interprétés ;
  - clés dérivées des libellés ;
  - le modèle publié passe avant le gabarit d'origine ;
  - choix du modèle d'un mouvement de caisse.
- `FundVoucherSamplesTest` :
  - pièces d'exemple et pièces réelles portent les mêmes clés ;
  - l'aperçu n'attribue aucun numéro.
