# Lot D4 : autres pièces de fonds, d'achats, de comptabilité et de caisse

Branche `feature/modeles-documents`. Migrations : dev `V352__modeles_documents_d4.sql`, prod `V346__modeles_documents_d4.sql`.

## Ce qui change pour l'exploitant

- **Onze pièces passent en modèles :**

  | Pièce | Modèle |
  | --- | --- |
  | Bon de décaissement | `DISBURSEMENT_VOUCHER` |
  | Fiche de justification d'avance | `JUSTIFICATION_SHEET` |
  | Bon de commande fournisseur | `PURCHASE_ORDER` |
  | Déclaration de TVA | `VAT_RETURN` |
  | État de rapprochement bancaire | `BANK_RECONCILIATION` |
  | Tableau d'amortissement d'emprunt | `LOAN_SCHEDULE` |
  | Bordereau de remise en banque | `BANK_DEPOSIT_SLIP` |
  | Relevé fournisseur | `SUPPLIER_STATEMENT` |
  | Relevé de compte partenaire | `PARTNER_STATEMENT` |
  | Demande de caisse | `CASH_REQUEST` |
  | Rapport de clôture de caisse | `CASH_CLOSING_REPORT` |

  Chacune a un **brouillon de départ** dans le style des pièces validées : en-tête commun, toutes les sections de la pièce, cases de signature. Le bon de décaissement sort en deux exemplaires (caisse et bénéficiaire).
- **Rien ne change à l'impression tant qu'un brouillon n'est pas publié.** Aucune maquette n'a été validée pour ces pièces : elles sortent de leur gabarit d'origine jusqu'à ce que l'exploitant relise l'aperçu et publie depuis le back-office.
- **Demande de caisse publiée** à la demande de l'exploitant le 14/11/2026 : migrations dev `V355__modeles_documents_demande_caisse_publiee.sql`, prod `V349`. Une version déjà publiée depuis le back-office est gardée.
- **Nouveaux PDF :**
  - **bordereau de remise en banque** : il avait un numéro mais aucun PDF. Il imprime les espèces, la liste des chèques, les totaux et les cases du caissier, du déposant et de la banque. Une réimpression est marquée DUPLICATA ;
  - **relevés fournisseur et partenaire imprimables** : jusqu'ici en JSON seulement. Ils impriment la période, les écritures avec leur solde progressif, les soldes d'ouverture et de clôture, et les cases « pour accord ». Un relevé n'est pas une pièce comptable : il ne prend pas de numéro au registre et n'a pas de QR de vérification.

## Nouveau bloc `SECTIONS`

Il imprime toutes les sections de la pièce, dans l'ordre : les champs de la section, puis son tableau.
- `columns` : nombre de colonnes des champs, de 1 à 4 ;
- `exclude` : sections (clé du titre, par exemple `circuit_de_validation`) ou champs à retirer.

Il sert de point de départ : l'exploitant peut le remplacer par des blocs `FIELDS` et `LINES` choisis, comme sur les pièces maquettées.

## Aperçu avec une pièce réelle

- **Pièces produites par `VoucherService`** (commande, TVA, rapprochement, emprunt, bordereau, relevés) :
  - la pièce est imprimée dans une transaction séparée, annulée à la fin, en mode capture : le PDF n'est pas produit, ses données sont gardées ;
  - le numéro attribué et la trace d'impression sont annulés.
- **Demande de caisse et rapport de clôture :** leurs données sont lues directement.

Numéro attendu par `?number=` : voir `previewNumberHint` du catalogue. Ce peut être un numéro de commande, un identifiant de déclaration de TVA ou de rapprochement, un code d'emprunt, de fournisseur ou de partenaire, un numéro de bordereau, de demande de caisse ou de session.

## Routes ajoutées (droits des routes de lecture voisines)

| Route | Droit |
| --- | --- |
| `GET /accounting/deposit-slips/{number}/pdf` | `ACCOUNTING_REPORT:READ` |
| `GET /accounting/purchases/suppliers/{code}/statement/pdf?from=&to=` | `PURCHASE:READ` |
| `GET /accounting/partners/{code}/statement/pdf?from=&to=` | droit des routes `/accounting/partners` |

## Composants

| Composant | Rôle |
| --- | --- |
| `DocumentData.sections` | Sections de la pièce, dans l'ordre : clés des champs, clé du tableau |
| `TemplateRenderer` | Bloc `SECTIONS` |
| `VoucherService.capturing` | Capture des données d'une pièce au lieu du PDF (aperçu) |
| `VoucherService.TEMPLATE_BY_TYPE` | Types `BRB`, `RLF`, `RLP` ajoutés |
| `ChequeService.slipPdf` | Bordereau de remise en banque |
| `StatementPrintService` | Relevés fournisseur et partenaire |
| `AccountingPrintController` | Routes des trois PDF |
| `CashReportDocumentData` | Données de la demande de caisse et du rapport de clôture ; branchées dans `CashDocumentPdfServiceImpl` |
| `DocumentDataSources` | Données d'exemple et pièces réelles des onze modèles |

## Hors de ce lot

Les procès-verbaux d'immobilisation, les classements et pertes sur créances et la revue des provisions passent aussi par `VoucherService`. Ils gardent leur gabarit, faute de code de modèle. Il suffira d'ajouter leur type à `TEMPLATE_BY_TYPE` et d'enregistrer le modèle.

## Tests

- `DocumentTemplateD4IT` (schéma de production) :
  - chacun des onze modèles a un brouillon et aucune version publiée ;
  - son impression reste celle du gabarit d'origine ;
  - le brouillon se rend avec les données d'exemple, et le bloc `SECTIONS` imprime chaque section.
- `TemplateD4Test` : la capture rend des octets vides et garde les données, sections comprises.
