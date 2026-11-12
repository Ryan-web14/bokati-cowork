# Lot F11 : fiscalité (TVA)

Lot du [plan finance](../plan-finance-comptabilite/README.md), section 12 « Fiscalité ». S'appuie sur F1 (pièces), F2 (moteur d'imputation), F4 (ventes et encaissements), F7 (pièces numérotées et PDF) et le complément multidevise de [F10](../finance-f10-banque-rapprochement/README.md).

Critère d'acceptation du plan : **l'état de TVA d'un mois égale la somme des TVA des factures du mois.** Vérifié par `VatF11IT` (voir Tests).

Branche `feature/finance-comptabilite`. Migrations : dev `V307__fiscalite_tva_f11.sql`, prod `V301__fiscalite_tva_f11.sql` ; prorata de déduction : dev `V308__tva_prorata_f11.sql`, prod `V302__tva_prorata_f11.sql` (contenus identiques deux à deux). Code : `VatService`, `VatController`, `AccVatReturn` dans `features/accounting` ; l'alimentation des ventes (`SalesAccountingService`) est complétée.

## Codes de taxe sur les écritures

- Les schémas `SALES_INVOICE` et `SALES_CREDIT_NOTE` ont une nouvelle version : la ligne de TVA et la ligne de centime additionnel portent la **référence** du document, leur **code de taxe** et leur **base**.
- Code de la TVA : `TVA` suivi du taux de la première ligne taxée (`TVA18`) ; `EXO` quand la facture n'a pas de TVA et que le client porte une mention d'exonération (`customerVatExemptionNote`, déjà figée sur la facture) ; rien sinon. Base : le montant hors taxe après remises, en XAF.
- Code du centime additionnel : `CA` (`accounting.vat.extra-tax-code`), base : le montant de la TVA.
- Un avoir rattache ses lignes de TVA à la facture d'origine (référence de lettrage).
- Les taux restent ceux de la facturation (`TaxRule`) : le lot n'en fige aucun.

## Exigibilité

Paramètre `accounting.vat.chargeability` (à confirmer avec l'expert-comptable, plan section 20, point 4) :

- `INVOICE` (par défaut, régime des débits) : la TVA est exigible à la facture ; elle va directement en `4431`.
- `PAYMENT` (régime des encaissements) : la TVA et le centime additionnel d'une facture vont d'abord au compte d'attente `4438` « TVA facturée en attente d'exigibilité » (créé par la migration, `accounting.vat.pending-account`). À chaque encaissement imputé sur la facture, la part réglée passe de `4438` au compte de TVA facturée, **au prorata** du montant réglé sur le total de la facture ; le règlement qui solde la facture prend le reste (écart d'arrondi d'un franc au plus). Un remboursement remet la part correspondante en attente.
  - Les schémas `SALES_PAYMENT` et `SALES_REFUND` ont une nouvelle version avec ces lignes (liste `vatTransfers` du fait, vide en régime des débits).
  - Un encaissement sur une facture pas encore comptabilisée attend le cycle suivant (« la TVA de l'encaissement attend »), pour ne pas laisser de TVA en attente.
  - Un avoir reprend l'attente de sa facture tant qu'elle la couvre ; sinon (facture déjà réglée), il diminue la TVA déjà exigible.
- Changer de régime ne reprend pas le passé : les factures déjà comptabilisées gardent leur traitement.

## Déclaration mensuelle

Table `acc_vat_return`, une déclaration par mois (`AAAA-MM`).

- **TVA collectée** : mouvement net (crédit moins débit) du mois des comptes `accounting.vat.collected-accounts` (`4431,4432`).
- **TVA déductible** : mouvement net (débit moins crédit) du mois des comptes `accounting.vat.deductible-accounts` (`4451,4452,4454`), alimentés par les dépenses de caisse (F3) et les justificatifs d'avance (F6) qui portent une TVA récupérable, ou par saisie.
- **Prorata de déduction** (`accounting.vat.deduction-prorata`, en %, 100 par défaut) : la TVA récupérable du mois (`deductibleGross`) n'est déductible qu'à hauteur du prorata (`deductible`, arrondi au franc) ; le reste (`nonDeductible`) passe en charge à la liquidation, au compte `accounting.vat.non-deductible-account` (`658`). Le prorata appliqué est gardé sur la déclaration (`deductionProrata`) ; il est saisi par la direction d'après le calcul de l'expert-comptable (part des recettes ouvrant droit à déduction).
- **Crédit reporté** : solde débiteur du compte de crédit de TVA (`4449`) au premier jour du mois.
- **TVA due** = collectée moins déductible moins crédit reporté, si positive ; sinon **crédit à reporter**.
- **Chiffre d'affaires HT** du mois : mouvement net des comptes `70`.
- **TVA encore en attente** (régime des encaissements) : solde de `4438` à la fin du mois.
- La pièce de liquidation (source `VAT_RETURN`) est exclue des calculs.
- **Détail** (`GET /vat-returns/{id}/detail`) :
  - par compte (collectée, déductible) ;
  - TVA collectée **par code de taxe**, avec sa base ;
  - **par document** : chaque facture et avoir émis dans le mois (date d'émission), et chaque document annulé dans le mois (en négatif), avec son numéro fiscal, s'il est scellé, sa base, sa TVA, son centime additionnel, sa taxe en XAF (convertie comme en comptabilité pour un document en devise), la taxe de ses pièces comptables et l'écart. La contre-passation d'un document annulé est retrouvée par `reversal_of`.
  - `sealedTaxTotal` : taxes des seuls documents **scellés** (validés, numéro fiscal). La comptabilité enregistre une facture dès son émission : un écart avec le total de tous les documents signale des factures émises mais pas encore scellées.
- **Contrôle** (régime des débits) : `invoiceTaxTotal` (somme des taxes des documents du mois) et `controlDifference` = TVA collectée moins `invoiceTaxTotal`, nul attendu. Un écart vient d'une écriture de TVA saisie à la main ou d'un document non encore comptabilisé (colonne « Comptabilisé » vide). En régime des encaissements, le contrôle par facture ne s'applique pas (`controlDifference` vide).

### Circuit

1. `POST /vat-returns?period=AAAA-MM` : prépare la déclaration, ou la recalcule tant qu'elle est en brouillon (`VAT_RETURN:PREPARE`).
2. `PATCH /vat-returns/{id}/validate` : par une **autre personne** que le préparateur (`VAT_RETURN:VALIDATE`) ; refusée si la déclaration du mois précédent existe sans être liquidée (le crédit reporté serait faux). L'état est recalculé puis figé.
3. `POST /vat-returns/{id}/liquidate` (`VAT_RETURN:VALIDATE`) : pièce au journal `accounting.vat.journal` (`OD`), datée du dernier jour du mois, source `VAT_RETURN` : débit de chaque compte de TVA collectée, crédit de chaque compte de TVA récupérable (montant avant prorata), débit du compte de charge pour la part non déductible, crédit de `4449` pour le crédit reporté, crédit de `4441` « État, TVA due » ou débit de `4449` pour le nouveau crédit. La période doit être ouverte.
4. `DELETE /vat-returns/{id}` : un brouillon seulement.

### Éditions

- `GET /vat-returns/{id}/pdf` : déclaration (période, régime, état, pièce de liquidation, chiffre d'affaires), calcul, détail par compte, par code de taxe et par document, total des documents scellés, contrôle ; signatures « Préparé par » et « Validé par ». Une déclaration validée ou liquidée reçoit un numéro de pièce `DTV-` (pièces du lot F7) à la première impression, DUPLICATA ensuite ; un brouillon s'imprime marqué BROUILLON.
- `GET /vat-returns/{id}/export` : CSV (point-virgule, UTF-8 avec BOM) : synthèse, puis une ligne par document (numéro fiscal, scellé, base, TVA, centime additionnel, taxe en XAF, comptabilisé, écart).

## Routes

Préfixe `/api/v1/accounting/vat-returns`. Écritures auditées.

| Route | Droit |
| --- | --- |
| `GET /`, `/{id}`, `/{id}/detail`, `/{id}/pdf`, `/{id}/export` | `VAT_RETURN:READ` |
| `POST /?period=`, `DELETE /{id}` | `VAT_RETURN:PREPARE` |
| `PATCH /{id}/validate`, `POST /{id}/liquidate` | `VAT_RETURN:VALIDATE` |

Rôles : `SUPER_ADMIN`, `ADMIN` et `FINANCE` tout ; `MANAGER` la lecture.

## Paramètres (domaine Comptabilité, à chaud)

`accounting.vat.chargeability` (`INVOICE` ou `PAYMENT`), `accounting.vat.pending-account` (`4438`), `accounting.vat.collected-accounts` (`4431,4432`), `accounting.vat.deductible-accounts` (`4451,4452,4454`), `accounting.vat.due-account` (`4441`), `accounting.vat.credit-account` (`4449`), `accounting.vat.extra-tax-code` (`CA`), `accounting.vat.journal` (`OD`), `accounting.vat.deduction-prorata` (`100`), `accounting.vat.non-deductible-account` (`658`).

## Hors de ce lot

- Retenues à la source sur prestataires (plan section 12) : traitées avec le lot F13 (fournisseurs et factures fournisseurs), où le montant brut de la facture du prestataire est connu ; compte de retenue `447`.
- TVA sur immobilisations (`4451`) : lot F18 (immobilisations). Elle entre déjà dans la déclaration dès qu'elle est passée en `4451`.
- Liasse fiscale : lot F15.
- Télédéclaration : l'export CSV et le PDF servent de support à la saisie sur le portail de l'administration fiscale.

## Pour le frontend

- **Écran « Déclarations de TVA »** : liste par mois (période, régime, collectée, déductible, crédit reporté, due ou crédit à reporter, état), bouton « Préparer le mois » (choix du mois) ; un brouillon se recalcule par le même bouton.
- **Fiche d'une déclaration** : calcul en tableau (collectée, récupérable, prorata, déductible, non déductible, crédit reporté, due ou crédit), chiffre d'affaires, TVA en attente (régime des encaissements) ; onglets « Par compte », « Par code de taxe », « Par document » (écart en rouge s'il n'est pas nul, document non comptabilisé signalé, badge « non scellé ») ; encart de contrôle en régime des débits (total des documents, total des documents scellés, écart).
- **Actions** : « Valider » (masqué pour le préparateur, message si le mois précédent n'est pas liquidé), « Liquider » (affiche le numéro de la pièce), « Supprimer » (brouillon), « Imprimer » (PDF en ligne), « Exporter » (CSV).
- **Paramètres** : le régime d'exigibilité et les comptes sont dans l'écran des paramètres, domaine Comptabilité.

## Tests

`VatF11IT` (schéma de production) :
- régime des débits, juillet 2026 : deux factures (dont une avec centime additionnel), un avoir, une facture annulée dans le mois, un achat avec TVA récupérable ; la TVA collectée (2 610) égale la somme des taxes des documents, l'écart de contrôle est nul, chaque document a un écart nul, la TVA par code (`TVA18`, `CA`) et le total des documents scellés sont justes ; TVA due 2 110 ; la validation est refusée au préparateur puis faite par une autre personne ; un brouillon ne se recalcule plus une fois validé ; la liquidation crédite `4441` de 2 110 et solde `4431` sur le mois ; l'export et le PDF (`DTV-`) sont produits ;
- prorata de 80 %, octobre 2026 : 900 de TVA récupérable donnent 720 déductibles et 180 non déductibles ; TVA due 1 080 ; la liquidation débite `658` de 180 et crédite `4452` de 900 ;
- régime des encaissements, août 2026 : la TVA d'une facture attend en `4438`, la moitié devient exigible avec un premier règlement de moitié, le reste avec le second ; la déclaration suit (900 puis 1 800).

`AdminApiAuthorizationManagerTest` couvre les routes.
