# Lot F1 : socle comptable SYSCOHADA

Lot du [plan finance](../plan-finance-comptabilite/README.md), détaillé dans [pieces-comptables.md](../plan-finance-comptabilite/pieces-comptables.md) et [rapprochement-et-comptes-tiers.md](../plan-finance-comptabilite/rapprochement-et-comptes-tiers.md) pour le lettrage. Lots précédents : C0 à C4 et C6 (caisses). Lot suivant : F2, moteur d'imputation.

Branche `feature/finance-comptabilite`. Migrations : dev `V296__comptabilite_socle_f1.sql`, prod `V290__comptabilite_socle_f1.sql` (contenu identique). Code : `features/accounting`.

## Ce que le lot apporte

- Plan de comptes SYSCOHADA révisé (228 comptes chargés, référentiel `acc_reference_chart`), comptes auxiliaires de tiers, historique de chaque compte.
- Exercice 2026 et ses 12 périodes mensuelles ; création des exercices suivants.
- Journaux : `VT` ventes, `AC` achats, `OD` opérations diverses, `PF` paie, `ST` stocks, `AN` à-nouveaux, `EXT` extourne, `HB` hors bilan, `CAG` analytique, `PA` provisions et amortissements. Les journaux de trésorerie (caisse, banque, mobile money) se créent avec leur compte.
- Pièces : brouillard, validation, numéro définitif, contre-passation, rejet, régularisation.
- Lettrage manuel : complet, partiel, avec écart de règlement.
- États : journal, grand livre, balance à 6 colonnes, contrôle de continuité de la numérotation.
- Traitement de nuit : contrôle de continuité et relances des pièces en attente.

**La comptabilité est désactivée** (`accounting.enabled = false`) jusqu'à la bascule décidée avec l'utilisateur : rejeu des opérations depuis le 1er janvier 2026 au moment de l'activation (lot F2). La saisie manuelle et les états fonctionnent déjà.

## Règles

### Plan de comptes

- Un compte se crée sous un parent : le numéro proposé est le prochain libre (`GET /accounting/accounts/proposal?parentNumber=`), les propriétés (nature, sens, lettrable, type de tiers, trésorerie, analytique) sont héritées du parent et peuvent être précisées.
- Un numéro hors du référentiel SYSCOHADA doit commencer par un compte du référentiel.
- Un compte de trésorerie nouveau (`treasuryType`) crée automatiquement son journal.
- L'intitulé se modifie toujours ; les autres propriétés seulement tant qu'aucune écriture validée ne touche le compte. Chaque changement est historisé (`/accounts/{n}/history`).
- Un compte ne se supprime pas : il se désactive (motif obligatoire) et se réactive.
- Mode `accounting.account.creation-mode = AFTER_APPROVAL` : le compte naît en attente d'approbation.

### Pièces

- Une pièce a au moins deux lignes ; une ligne porte un débit **ou** un crédit, positif.
- Saisie manuelle : `POST /accounting/entries` crée un brouillard (numéro provisoire `BR-AAAAMM-0000001`) ; `validate: true` le valide aussitôt.
- Validation : période ouverte, comptes existants et actifs, tiers obligatoire sur un compte de tiers, comptes autorisés par le journal, ligne sur le compte de trésorerie pour un journal de trésorerie, comptes de classe 9 seulement dans `HB` et `CAG`.
- Chronologie stricte (`accounting.numbering.chronology = STRICT`) : une pièce ne peut pas être datée avant la dernière pièce validée du même journal. `PERIOD` relâche la règle à la période.
- Numéro définitif `JOURNAL-EXERCICE-000001` (largeur `accounting.numbering.width`), attribué à la validation, sans trou, compteur verrouillé ; chaque pièce porte une empreinte SHA-256 chaînée à la précédente du journal.
- Au-delà de `accounting.manual-entry.second-validation-threshold` XAF (0 = jamais), une pièce manuelle est validée par un autre comptable que son auteur.

### Pièce déséquilibrée : politique `accounting.unbalanced-entry.policy`

| Mode | Effet |
| --- | --- |
| `HOLD` (défaut) | La pièce passe en `PENDING_BALANCE`, sans numéro, hors comptabilité ; les comptables sont prévenus ; relances J+1, J+3, J+7 puis escalade hebdomadaire |
| `SUSPENSE_ACCOUNT` | Une ligne d'équilibrage sur `4718` (paramétrable) est ajoutée et la pièce est validée ; au-delà de `suspense-max-difference`, retour à `HOLD` |
| `ACCEPT` | La pièce est validée avec l'écart, marquée `acceptedUnbalanced` ; la clôture de la période est bloquée tant qu'une régularisation (`POST /entries/{ref}/regularize`) ne l'a pas compensée |

Un autre problème que le déséquilibre (compte absent ou inactif sur une pièce automatique, tiers manquant, compte non autorisé) met toujours la pièce en attente. Compléter une pièce en attente demande `ACCOUNTING_ENTRY:BALANCE`.

### Corrections

- Brouillard : modifié ou supprimé librement.
- Pièce en attente : complétée (`PUT`, puis validation automatique) ou rejetée (`/reject`, motif).
- Pièce validée : **jamais modifiée ni supprimée**. Elle se contre-passe (`/reverse`, motif, date) : pièce inverse dans le même journal, les deux sont liées et leurs lignes de tiers lettrées ensemble.

### Garanties de la base (déclencheurs)

- Une pièce ne passe en `VALIDATED` qu'avec au moins deux lignes, équilibrée (sauf marqueur `accepted_unbalanced`) et numérotée.
- Une pièce validée garde son numéro, journal, date, période, empreinte, montants et libellé ; elle ne se supprime pas et ne revient pas en brouillard ; ses lignes ne changent plus.
- Un compteur de numérotation n'avance que d'un pas et ne se supprime pas.

### Périodes

- `OPEN` → `LOCKED` (`/lock`, réversible par `/unlock`) → `CLOSED` (`/close`, définitif).
- La clôture refuse une période qui a des brouillards, des pièces en attente, ou (si `blocks-period-close`) des pièces acceptées déséquilibrées non régularisées.
- Une période verrouillée n'admet plus que la saisie comptable manuelle (corrections) : les pièces produites par le logiciel y sont refusées. Une période close refuse tout.
- Les périodes se closent dans l'ordre chronologique.

### Lettrage

- Sur un compte lettrable, entre lignes validées du même tiers.
- Complet : débits = crédits. Partiel (`partial: true`) : montants imputés ligne par ligne, le reste reste ouvert.
- Avec écart (`withDifference: true`) sous `accounting.lettering.difference-threshold` XAF (500) : une pièce d'écart est passée sur `658` (perte) ou `758` (gain).
- Codes `AA`, `AB`… par compte et tiers. Délettrer (`/unletter`, motif) contre-passe la pièce d'écart éventuelle.

## Routes

Préfixe `/api/v1/accounting`. Toutes les écritures passent par le journal d'audit (module `ACCOUNTING`).

| Route | Droit |
| --- | --- |
| `GET` toutes routes, `POST /numbering-checks` | `ACCOUNTING_REPORT:READ` |
| `POST /accounts`, `PATCH /accounts/{n}` | `ACCOUNTING_ACCOUNT:CREATE` |
| `PATCH /accounts/{n}/deactivate`, `/activate` | `ACCOUNTING_ACCOUNT:MANAGE` |
| `POST /fiscal-years`, `PATCH /periods/{code}/close` | `ACCOUNTING_PERIOD:CLOSE` |
| `PATCH /periods/{code}/lock`, `/unlock` | `ACCOUNTING_PERIOD:LOCK` |
| `POST /journals` / `PATCH /journals/{code}` | `ACCOUNTING_JOURNAL:CREATE` / `MANAGE` |
| `POST /entries`, `PUT /entries/{ref}`, `DELETE /entries/{ref}` | `ACCOUNTING_ENTRY:CREATE` |
| `PATCH /entries/{ref}/validate` | `ACCOUNTING_ENTRY:VALIDATE` |
| `PATCH /entries/{ref}/reverse` | `ACCOUNTING_ENTRY:REVERSE` |
| `PATCH /entries/{ref}/reject`, `POST /entries/{ref}/regularize` | `ACCOUNTING_ENTRY:BALANCE` |
| `POST /lettering` / `PATCH /lettering/{id}/unletter` | `ACCOUNTING_LETTERING:MANUAL` / `UNLETTER` |

Rôles : `SUPER_ADMIN` et `ADMIN` ont tout. `FINANCE` a tout sauf `ACCOUNTING_ACCOUNT:MANAGE`, `ACCOUNTING_JOURNAL:MANAGE`, `ACCOUNTING_PERIOD:CLOSE` et `ACCOUNTING_SETTINGS:MANAGE`. `MANAGER` a la lecture.

## Paramètres (domaine Comptabilité, modifiables à chaud)

| Clé | Défaut |
| --- | --- |
| `accounting.enabled` | `false` |
| `accounting.analytic.enabled` | `false` |
| `accounting.unbalanced-entry.policy` | `HOLD` |
| `accounting.unbalanced-entry.suspense-account` | `4718` |
| `accounting.unbalanced-entry.suspense-max-difference` | `0` (sans limite) |
| `accounting.unbalanced-entry.blocks-period-close` | `true` |
| `accounting.numbering.chronology` | `STRICT` |
| `accounting.numbering.width` | `6` |
| `accounting.lettering.difference-threshold` / `loss-account` / `gain-account` | `500` / `658` / `758` |
| `accounting.account.creation-mode` | `IMMEDIATE` |
| `accounting.manual-entry.second-validation-threshold` | `0` (jamais) |
| `accounting.notify-emails` | vide (adresses séparées par des virgules) |
| `accounting.night-worker-cron` (redémarrage) | `0 30 2 * * *` |

## Pour le frontend

- **Plan de comptes** : arbre par classe (`GET /accounts?classCode=&search=&includeInactive=`), fiche avec historique et tiers rattachés ; création en deux champs (parent, intitulé) après appel de `/accounts/proposal`.
- **Saisie de pièce** : grille de lignes (compte, tiers, libellé, débit, crédit) avec totaux et écart en direct ; boutons « Enregistrer le brouillard » et « Valider ». La réponse porte `messages` (mise en attente, compte d'attente, numéro attribué) à afficher tels quels.
- **Pièces en attente** (`GET /entries/pending`) : liste avec motif (`pendingReason`, `pendingDetail`), écart et ancienneté ; actions compléter ou rejeter.
- **Pièce validée** : lecture seule, numéro et empreinte ; action « Contre-passer » (motif, date) ; liens `reversalOf`, `reversedByEntry`, `regularizationOf`.
- **Lettrage** : choisir compte et tiers, cocher les lignes ouvertes (`/lettering/open-lines`), voir la différence ; options partiel ou avec écart ; liste des lettrages avec délettrage.
- **États** : journal, grand livre (préfixe de compte, tiers, dates), balance (`scope` `1-8` ou `9`, indicateur `balanced` et pièces en cause si déséquilibre), contrôle de continuité (trous, doublons, chaîne d'empreintes).
- **Périodes** : tableau des 12 mois avec statut et boutons verrouiller, déverrouiller, clôturer ; le refus de clôture explique ce qui bloque.

## Tests

- `AccountingFoundationF1IT` (schéma de production) : plan chargé, numéros consécutifs et continuité, modes `HOLD` et compte d'attente, contre-passation lettrée, lettrage client, balance équilibrée, refus par la base de modifier, supprimer, valider déséquilibré, sauter un numéro.
- `AdminApiAuthorizationManagerTest.shouldGuardTheAccountingRoutes`.
