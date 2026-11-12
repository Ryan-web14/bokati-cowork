# Lots F5 et F6 : demandes de fonds, état de besoin, avances et justification

Lots du [plan finance](../plan-finance-comptabilite/README.md), détaillés dans [demandes-et-justification.md](../plan-finance-comptabilite/demandes-et-justification.md). Écritures : [catalogue, section 9](../plan-finance-comptabilite/catalogue-ecritures.md#9-demandes-de-fonds-et-avances).

Branche `feature/finance-comptabilite`. Migrations : dev `V300__demandes_fonds_avances_f5_f6.sql`, prod `V294__demandes_fonds_avances_f5_f6.sql` (contenu identique). Code : nouveau module `features/fund`.

## Ce qui change

- **Tout employé** crée une demande (droit `FUND_REQUEST:CREATE`), **sans session de caisse**. L'ancienne `cash_request` reste en lecture.
- **Approuver** et **payer** sont distincts : l'approbation met la demande dans la file « À décaisser » ; la caissière décaisse depuis sa session.
- Demandes **multi-lignes** (nature, désignation, montant, fournisseur, section), types, bénéficiaire, mission, dépense non prévue avec motif.
- **Validation à plusieurs niveaux** selon le montant, délégations, rappels, escalade.
- **Avances** suivies jusqu'au solde : justification ligne par ligne, reliquat rendu ou complément versé, relances, blocage.

## Objets et numéros

| Objet | Numéro | Table |
| --- | --- | --- |
| État de besoin | `EDB-AAAAMM-000001` | `fund_need_statement`, `fund_need_line` |
| Demande de fonds | `DDF-AAAAMM-000001` | `fund_request`, `fund_request_line`, `fund_request_approval` (frise) |
| Décaissement, complément, retour | numéro du mouvement de caisse | `fund_disbursement` |
| Ligne de justification | `JUS-AAAAMM-000001` | `expense_justification_line` |
| Délégation de validation | | `fund_approval_delegation` |

## État de besoin

`DRAFT` → `SUBMITTED` → `VALIDATED` ou `REJECTED` (motif) → `PARTIALLY_TRANSFORMED` / `TRANSFORMED`. Total calculé (quantité × prix estimé), montant en lettres, **nature suggérée** par les mots de la désignation (mots-clés des natures, la plus utilisée d'abord). Validation : droit `FUND_REQUEST:APPROVE_L1`, jamais par son auteur. Transformation en demande de fonds en brouillon, avec toutes les lignes ou celles choisies (`lineNos`).

## Demande de fonds

### Types

`DIRECT_EXPENSE` (dépense directe), `ADVANCE` (avance à justifier), `EXPENSE_REFUND` (remboursement de frais), `SUPPLIER_PAYMENT` (paiement fournisseur), `STAFF_ADVANCE` (acompte au personnel), `CASH_SUPPLY` (approvisionnement de caisse), `FUND_REMITTANCE` (remise de fonds).

### Cycle

```
DRAFT → IN_APPROVAL (niveau 1..n) → APPROVED → PARTIALLY_DISBURSED → DISBURSED
  ↑          ├→ renvoi (retour en DRAFT avec commentaire)       dépense directe : SETTLED
  │          └→ REJECTED (motif)                                 avance : TO_JUSTIFY → PARTIALLY_JUSTIFIED → JUSTIFIED → SETTLED
  └ CANCELLED (par le demandeur, avant décaissement)
APPROVED sans décaissement après fund.request.expiry-days (15) → EXPIRED
```

### Validation

| Montant (approuvé, sinon demandé) | Niveaux | Droit par niveau |
| --- | --- | --- |
| ≤ `fund.approval.level1-max` (50 000) | 1 | `FUND_REQUEST:APPROVE_L1` (chef de service) |
| ≤ `fund.approval.level2-max` (500 000) | 2 | puis `APPROVE_L2` (responsable financier) |
| au-delà | 3 | puis `APPROVE_L3` (direction) |

- Chaque niveau : **approuver** (option **réduire le montant**, motif obligatoire), **rejeter** (motif), **renvoyer pour correction** (commentaire).
- Le demandeur et le bénéficiaire ne valident jamais leur demande.
- **Délégation** : un détenteur d'un niveau le délègue à une personne pour une période (`POST /fund-requests/delegations`).
- Rappel au valideur après `fund.approval.reminder-hours` (24) ; escalade au responsable après `fund.approval.escalation-days` (3).
- Chaque décision est datée et nominative (frise `timeline`).
- À la soumission : refus si le bénéficiaire a une avance en retard de justification (`fund.advance.overdue-policy = BLOCK`, ou `ALERT` pour un simple avertissement).

### Décaissement

`POST /fund-requests/{n}/disburse` par la caissière (`FUND_REQUEST:DISBURSE`), depuis sa session, avec le nom du signataire et la signature (image) :

- **dépense directe** (et remboursement, paiement fournisseur) : un mouvement `CASH_OUT` par ligne avec sa nature, en une fois ;
- **avance**, acompte : un mouvement `CASH_OUT` sur la nature `AVANCE_A_JUSTIFIER` (`421` du bénéficiaire), total ou partiel ;
- refus si la session n'a pas les fonds ; la caissière ne se décaisse pas sa propre demande ;
- approvisionnement et remise de fonds : ordre de transfert entre caisses (lot C3), rattaché par `PATCH /fund-requests/{n}/transfer`.

Chaque mouvement porte la référence `FUND_REQUEST` et le numéro de la demande, le bénéficiaire (nom, pièce, téléphone) : la comptabilité de caisse (F3) l'écrit (`421` pour une avance, charge pour une dépense directe), avec le numéro de demande en référence de lettrage.

## Avances et justification (F6)

- Échéance : fin de la mission (ou jour du décaissement) + `fund.justification.days` (7).
- **Saisie** d'une ligne par le bénéficiaire, le demandeur, la caisse ou un comptable : date (pas avant l'avance ou la mission), nature, fournisseur, désignation, montant TTC, TVA, section, pièce. **Pièce obligatoire**, sauf attestation sur l'honneur jusqu'à `fund.justification.receipt-exempt-max` (5 000) par ligne.
- **Validation** par un comptable (`FUND_JUSTIFICATION:VALIDATE`), jamais par le bénéficiaire : la ligne validée produit son écriture (événement `EXPENSE_JUSTIFIED`, journal `AV`) : D charge de la nature HT, D `4452` TVA, C `421` du bénéficiaire (référence : numéro de demande). Ligne rejetée : motif, le bénéficiaire la remplace ou rembourse.
- **Reliquat** : `POST /fund-requests/{n}/return` (session de la caisse) : bon d'entrée `CASH_IN` nature `RETOUR_AVANCE` (D caisse, C `421`).
- **Complément** quand les dépenses validées dépassent l'avance : `POST /fund-requests/{n}/complement` : `CASH_OUT` sur `421`.
- **Soldée** quand avance + complément = justifié + rendu ; le `421` du bénéficiaire revient à zéro et se lettre par numéro de demande (lettrage automatique, lot F4, étendu au `421`).
- **Relances** au bénéficiaire à J-1, J, J+3, puis chaque semaine avec copie au responsable (traitement `fund.worker-cron`, toutes les heures de 8 h à 18 h).

## Reprise de l'existant

`POST /fund-requests/import-legacy` : les `cash_request` en attente deviennent des demandes en validation (niveau 1) ; les avances exécutées deviennent des avances à justifier du montant décaissé (échéance à 7 jours). Rejouable sans doublon (`legacyRequestNumber`).

## Routes

Préfixe `/api/v1`. Écritures auditées (module `FUND`).

| Route | Rôle | Droit d'entrée |
| --- | --- | --- |
| `GET /fund-needs/mine`, `/fund-needs/{n}`, `POST /fund-needs`, `PUT /fund-needs/{n}`, `PATCH .../submit`, `POST .../transform` | États de besoin | `FUND_REQUEST:CREATE` |
| `GET /fund-needs/submitted`, `PATCH /fund-needs/{n}/validate`, `/reject` | Validation des états de besoin | `FUND_REQUEST:APPROVE_L1` (sauf la liste : `CREATE`) |
| `GET /fund-requests/mine`, `/to-approve`, `/{n}`, `/{n}/insight` | Mes demandes, ma file de validation, fiche, ce que la demande deviendra | `FUND_REQUEST:CREATE` (contrôle fin dans le service) |
| `POST /fund-requests` (`submit: true` pour soumettre aussitôt), `PUT /{n}`, `POST /{n}/duplicate`, `PATCH /{n}/submit`, `/cancel` | Saisie | `FUND_REQUEST:CREATE` |
| `PATCH /{n}/approve` (`reducedAmount` facultatif), `/reject`, `/send-back` | Décisions | `FUND_REQUEST:CREATE`, puis le niveau ou la délégation |
| `GET /fund-requests/to-disburse?registerCode=` | File « À décaisser » | `FUND_REQUEST:DISBURSE` |
| `POST /{n}/disburse`, `/return`, `/complement`, `PATCH /{n}/transfer` | Mouvements d'argent | `FUND_REQUEST:DISBURSE` |
| `POST /{n}/expenses`, `DELETE /{n}/expenses/{ligne}` | Justification | `FUND_REQUEST:CREATE` (bénéficiaire, demandeur, caisse, comptable) |
| `PATCH /{n}/expenses/{ligne}/validate`, `/reject` | Validation des lignes | `FUND_JUSTIFICATION:VALIDATE` |
| `GET /fund-requests/justifications-to-validate` | File du comptable | `FUND_JUSTIFICATION:VALIDATE` |
| `GET /fund-requests`, `/fund-requests/advances` | Toutes les demandes, état des avances ouvertes | `FUND_REQUEST:READ` |
| `GET` / `POST /fund-requests/delegations`, `DELETE /fund-requests/delegations/{id}` | Délégations | `FUND_REQUEST:CREATE` (le service exige de détenir le niveau délégué) |
| `POST /fund-requests/import-legacy` | Reprise | `FUND_REQUEST:READ` |

Rôles : `STAFF` crée ; `CASHIER` crée et décaisse ; `MANAGER` crée, lit, niveau 1 ; `FINANCE` crée, lit, niveau 2, décaisse, valide les justifications ; `ADMIN` et `SUPER_ADMIN` tout.

## Pour le frontend

- **Nouvelle demande** : grandes tuiles par type, puis formulaire adapté (mission pour une avance de déplacement). Bénéficiaire « moi » par défaut. Encadré `insight` avant l'envoi : niveaux requis et leurs libellés, solde de la caisse et possibilité de couvrir, avances ouvertes du bénéficiaire, avertissement ou blocage. Total et montant en lettres (`totalInWords`). Boutons « Enregistrer » et « Envoyer » (`submit: true`), « Dupliquer ».
- **Fiche** : frise des étapes (`timeline`), lignes, mouvements (`disbursements`), justification avec le reste en direct (`remainingToJustify`), actions selon le rôle.
- **Ma file de validation** (`/to-approve`) : approuver, réduire, renvoyer, rejeter.
- **Caisse** : file `/to-disburse` triée par date souhaitée ; décaissement avec signature (tablette), puis impression de la pièce ; retour du reliquat et complément.
- **Comptable** : file `/justifications-to-validate`, validation ligne par ligne ; état des avances (`/advances`) avec retard en jours.
- **États de besoin** : saisie en grille, nature suggérée, validation, transformation (toutes les lignes ou une sélection).

## Tests

`FundRequestF5F6IT` (schéma de production) :
- avance de 100 000 validée sur deux niveaux, le demandeur et le niveau 1 ne pouvant pas valider à la place du niveau suivant ;
- décaissement sur la nature `AVANCE_A_JUSTIFIER` ;
- trois justifications validées pour 85 000, avec trois événements comptables ;
- retour de 15 000 et avance soldée ;
- renvoi pour correction ;
- rejet sans motif refusé ;
- état de besoin validé puis transformé en demande.

`AdminApiAuthorizationManagerTest` couvre les routes.

## Évolutions apportées par les lots suivants

- **Lot F7** : pièces PDF de l'état de besoin, de la demande, du bon de décaissement et de la fiche de justification ([finance-f7-pieces-comptables](../finance-f7-pieces-comptables/README.md)).
- **Lot F9** : la ligne de justification validée porte le poste de dépense de sa nature (axe `POSTE`).
- **Lot F16** (contrôle budgétaire, [finance-f16-budget](../finance-f16-budget/README.md)) :
  - la saisie accepte `forecastNumber` (prévision couverte) et `unplannedCode` (motif de la liste des dépenses non prévues) ;
  - `insight` porte `budget` (impact par ligne de budget) et `budgetApproval` ;
  - à la soumission, avec un budget actif, une demande cite sa prévision ou se déclare non prévue ; un dépassement de ligne bloquante ajoute un niveau de validation budgétaire (`BUDGET:OVERRIDE`) ou refuse la demande selon `fund.budget.blocking-mode` ;
  - l'approbation finale engage le budget ; le décaissement d'une dépense directe le solde ; l'avance soldée le solde pour le justifié ; rejet, annulation et expiration le libèrent ;
  - la file `/fund-requests/to-approve` montre le niveau budgétaire aux détenteurs du droit budgétaire concerné.
