# Caisse comptable : natures, journaux de caisse, écarts

Contrat exact de chaque route (paramètres, corps, réponse, champs JSON) : [routes.md](routes.md). Ce fichier explique le fonctionnement et ce qu'il faut à l'écran. Index du module : [README.md](README.md).

## Principe

Le module caisse ne connaît pas la comptabilité. Chaque mouvement porte un **état comptable** (`accountingStatus`, `PENDING` à la création). Le traitement du moteur (toutes les 30 s, comptabilité active) :

1. lit les mouvements `PENDING` (hors mouvements en attente de visa) et les écarts constatés ou décidés ;
2. publie l'événement comptable correspondant dans le registre du moteur (idempotent) ;
3. le moteur applique le schéma et produit la pièce ;
4. le résultat revient sur le mouvement ou l'écart : `POSTED` avec le numéro de pièce, `UNBALANCED`, `FAILED` avec le motif, `IGNORED`.

| État comptable d'un mouvement | Sens |
| --- | --- |
| `NOT_APPLICABLE` | Pas d'écriture : paiement et remboursement client (écrits par l'événement de paiement, lot F4), comptage, échange, mouvement annulé |
| `PENDING` | À transmettre (ou comptabilité inactive) |
| `QUEUED` | Transmis au moteur |
| `POSTED`, `UNBALANCED`, `FAILED`, `IGNORED` | Résultat du moteur, voir F2 |

## Comptes et journaux des caisses

- Chaque caisse reçoit, à son premier fait comptabilisé (ou à la première consultation de ses journaux), un compte sous `571` (`accounting.cash.treasury-parent-account`, numéro libre suivant : `5711`, `5712`...) et son journal `CA-<compte>` (type caisse, validation automatique). La fiche de la caisse les retient (`treasuryAccountNumber`, `defaultJournalCode`, déjà présents depuis C1). Un compte ou un journal saisi sur la fiche est respecté.
- La caissière peut choisir un autre journal **autorisé pour sa caisse** (`journalCode` à la saisie du mouvement) : le journal rattaché à la caisse, ou un journal autorisé par l'administrateur (`acc_journal_register`). Un journal non autorisé met le mouvement en `FAILED` avec le motif.
- Une caisse n'écrit jamais dans le journal de caisse d'une autre caisse (autorisation refusée).
- Créer un journal depuis la caisse : `POST /accounting/cash/registers/{code}/journals` (libellé, type `CASH` ou `MISC`), droit `ACCOUNTING_JOURNAL:CREATE` ; code proposé `CA-<compte>-1`, `OD-<compte>-1`...

## Écritures (schémas initiaux, modifiables dans l'éditeur F2)

Événement `CASH_MOVEMENT` (famille `CASH`) :

| Schéma | Condition | Écriture |
| --- | --- | --- |
| `CASH_OPENING_FIRST` | `OPENING_FLOAT` de la toute première session de la caisse | D caisse ; C `585` |
| `CASH_OPENING_CARRY` | Autre `OPENING_FLOAT` (session suivante, passation, report de fin de mois) | Aucune |
| `CASH_OUT` | Sortie | D compte de la nature (HT) ; D `4452` TVA ; C caisse |
| `CASH_IN` | Entrée hors encaissement client | D caisse ; C compte de la nature (HT) ; C `4431` TVA |
| `CASH_SAFE_DEPOSIT` | Remise en banque ou au coffre | D `585` ; C caisse |
| `CASH_TRANSFER_OUT` / `CASH_TRANSFER_IN` | Transfert entre caisses | D `585` ; C caisse / D caisse ; C `585` (référence : l'ordre de transfert) |
| `CASH_ADJUSTMENT` | Ajustement | Caisse contre le compte de la nature, à défaut `471`, dans le sens du mouvement |
| (contre-passation) | Mouvement de contre-passation (lot C3) | Contre-passation de la pièce du mouvement d'origine |

- Sans nature d'opération : compte `accounting.cash.default-out-account` ou `default-in-account` (`471`, à reclasser).
- Le tiers est porté quand le compte de la nature est un compte de tiers (`421` avance, `165` dépôt de garantie) : fournisseur, contrepartie ou bénéficiaire du mouvement. S'il manque, la pièce est mise en attente.
- Les données disponibles pour un schéma : `movementNumber`, `type`, `direction`, `amount`, `signedAmount`, `netAmount`, `vatAmount`, `vatAccount`, `treasuryAccount`, `journalCode`, `label`, `natureCode`, `natureAccount`, `natureThirdParty`, `counterAccount`, `registerCode`, `registerName`, `sessionNumber`, `transferReference`, `requestNumber`, `supplierCode`, `counterpartyCode`, `firstSession`.

Écarts (revues d'écart du lot C2) :

| Événement | Écriture |
| --- | --- |
| `CASH_VARIANCE_RECORDED` (constat, dès la clôture, l'ouverture, la passation, le comptage inopiné) | Manquant : D `471` ; C caisse. Excédent : D caisse ; C `471`. Journal de la caisse |
| `CASH_VARIANCE_DECIDED` charge (ou tolérance d'un manquant) | D `658` ; C `471`, journal `OD` |
| produit (ou tolérance d'un excédent) | D `471` ; C `758`, journal `OD` |
| caissier | D `421` (tiers : le caissier) ; C `471`, journal `OD` |
| mise en recherche | Aucune : l'écart reste en `471` |
| `CASH_VARIANCE_CANCELLED` (écart remplacé par un recomptage après constat) | Contre-passation du constat |

Le journal `OD` ne valide pas automatiquement : les imputations d'écart y attendent en brouillard la validation d'un comptable (paramètre du journal).

Ainsi, le solde comptable d'une caisse suit le montant réellement compté : attendu, plus ou moins les écarts constatés.

## Nouveaux champs du mouvement

À la saisie (`CreateCashMovementRequest`) : `journalCode`, `vatAmount` (comprise dans le montant), `vatCode`, `supplierCode`, `beneficiaryName`, `beneficiaryIdNumber`, `beneficiaryPhone`, `requestNumber`. En réponse, un objet `accounting` : `journalCode`, `status`, `entryNumber`, `note`, et ces mêmes champs.

## Routes

Préfixe `/api/v1/accounting/cash`.

| Route | Rôle | Droit |
| --- | --- | --- |
| `GET /registers/{code}/journals` | Journaux proposés à la caissière (celui de la caisse en premier) | `CASH_MOVEMENT:CREATE` |
| `POST /registers/{code}/journals` | Créer un journal rattaché à la caisse (`label`, `type`) | `ACCOUNTING_JOURNAL:CREATE` |
| `PUT` / `DELETE /registers/{code}/journals/{journal}` | Autoriser ou retirer un journal existant pour la caisse | `ACCOUNTING_JOURNAL:MANAGE` |
| `GET /reconciliation` | Par caisse : solde comptable du compte, solde opérationnel (session en cours, sinon dernier comptage), différence, mouvements pas encore comptabilisés | `ACCOUNTING_REPORT:READ` |
| `POST /feed` | Transmettre tout de suite les faits de caisse | `ACCOUNTING_POSTING:REPLAY` |

Tant que le lot F4 n'est pas actif, les encaissements clients en espèces ne sont pas écrits : le rapprochement montre alors une différence égale aux paiements de la session.

## Paramètres (domaine Comptabilité, à chaud)

| Clé | Défaut |
| --- | --- |
| `accounting.cash.treasury-parent-account` | `571` |
| `accounting.cash.default-out-account` / `default-in-account` | `471` |
| `accounting.cash.vat-recoverable-account` / `vat-collected-account` | `4452` / `4431` |
| `accounting.replay-until` | vide : les faits datés avant ce jour produisent des brouillards (reprise, F2) |

## Pour le frontend

- **Saisie d'un mouvement** : sélecteur « Journal » prérempli (premier élément de `GET /accounting/cash/registers/{code}/journals`), bouton « Nouveau journal » si l'utilisateur a `ACCOUNTING_JOURNAL:CREATE` ; champs TVA, fournisseur, bénéficiaire (nom, pièce, téléphone), demande.
- **Liste et fiche d'un mouvement** : pastille d'état comptable (`accounting.status`) avec le numéro de pièce (lien vers la pièce) ou le motif (`accounting.note`).
- **Revue d'écart** : afficher le numéro de pièce du constat (`entryNumber`) et de l'imputation (`decisionEntryNumber`) avec leurs états.
- **Trésorerie** : tableau `GET /accounting/cash/reconciliation`, différence en rouge si non nulle, nombre de mouvements non comptabilisés.
- **Fiche de caisse (admin)** : liste des journaux autorisés, ajout et retrait.

## Évolutions apportées par les lots suivants

- **Lots F5 et F6** : les décaissements de demandes de fonds, compléments et retours de reliquat sont des mouvements de caisse ordinaires (référence `FUND_REQUEST`, nature `AVANCE_A_JUSTIFIER` ou `RETOUR_AVANCE`) : ils sont comptabilisés par ce lot.
- **Lot F9** : les schémas `CASH_OUT` et `CASH_IN` passent en version 2 : la ligne de nature porte le poste de dépense de la nature (`natureSection`, axe `POSTE`).
- **Lot C5** (avec F10) : un mouvement de caisse en devise est comptabilisé en XAF au taux du mouvement, sinon au taux du jour ([finance-f10-banque-rapprochement](../../backend/finance-f10-banque-rapprochement/README.md)).
- **Complément F10 (écarts de change)** : les schémas `CASH_OUT`, `CASH_SAFE_DEPOSIT` et `CASH_TRANSFER_OUT` ont une nouvelle version : une sortie d'espèces en devise est valorisée au coût moyen des entrées, et l'écart avec le taux du jour passe en `676` ou `776` dans la même pièce.
