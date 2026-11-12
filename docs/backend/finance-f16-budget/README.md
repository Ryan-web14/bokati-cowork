# Lot F16 : gestion budgétaire

Lot du [plan finance](../plan-finance-comptabilite/README.md), détaillé dans [analytique-et-budget.md](../plan-finance-comptabilite/analytique-et-budget.md), sections 3 à 5. S'appuie sur F5 et F6 (demandes de fonds) et F9 (postes de dépense, sections).

Branche `feature/finance-comptabilite`. Migrations : dev `V303__budget_f16.sql`, prod `V297__budget_f16.sql` (contenu identique). Code : `BudgetEngine` (contrôle), `BudgetService` (gestion), `BudgetReportService` (suivi, alertes), `BudgetController` dans `features/accounting` ; contrat `core/accounting/BudgetControl` utilisé par les demandes de fonds.

## Nomenclature budgétaire (postes budgétaires)

- Arbre défini par l'utilisateur (`acc_budget_item`) : code, libellé, parent, type (`CHARGE`, `PRODUCT`, `INVESTMENT`, `TREASURY`), responsable, niveau de contrôle par défaut (`BLOCKING`, `ALERT`, `INFO`), tolérance en %, contrôle au niveau du parent.
- **Correspondances** (`acc_budget_item_mapping`) : ce qui alimente le poste : préfixes de comptes (`ACCOUNT`), postes de dépense analytiques (`EXPENSE_ITEM`), natures d'opération (`NATURE`). Une valeur n'alimente qu'un poste.
- Livré : Fonctionnement (Locaux, Énergie et eau, Accueil, Technique, Administratif), Personnel (Missions et déplacements, bloquant), Commercial, Investissement (comptes `2`, bloquant), Chiffre d'affaires (comptes `7`, informatif), reliés aux postes de dépense du lot F9.
- Création : immédiate avec `BUDGET_ITEM:MANAGE` ; en attente de validation avec `BUDGET:PREPARE`. Actions : `approve`, `reject`, `close`, `reopen`, `merge?into=` (les correspondances suivent la cible).

**Rattachement d'une dépense** : nature (`NATURE`), puis poste de dépense de la nature (`EXPENSE_ITEM`), puis compte (`ACCOUNT`, le préfixe le plus long), puis poste de dépense du compte. Un poste « contrôlé au parent » remonte au parent. La ligne est celle du poste et de la section de la dépense, à défaut celle du poste sans section.

## Budget, versions, lignes

- Un budget par exercice, en **versions** (`acc_budget`) ; une seule `ACTIVE`. Circuit : `DRAFT` → `submit` (`BUDGET:PREPARE`) → `SUBMITTED` → `consolidate` (`BUDGET:CONSOLIDATE`) → `CONSOLIDATED` → `approve` (`BUDGET:APPROVE`) → `ACTIVE` (l'ancienne version active est archivée, ses engagements ouverts suivent la nouvelle, ligne à ligne) ; `reject` avec motif.
- Création d'une version : `EMPTY`, `ACTIVE` (révision, motif obligatoire, copie de la version active) ou `PREVIOUS_ACTUAL` (réalisé de l'exercice précédent par poste, avec `evolutionPct`).
- **Ligne** : poste, section (facultative), montant annuel, mensualisation (12 montants saisis, ou répartition `EQUAL`, ou profil saisonnier « 12 poids séparés par des virgules »), niveau de contrôle et tolérance (sinon ceux du poste), responsable, gel. Une ligne ne se modifie que dans une version `DRAFT` ou `SUBMITTED`.
- **Gel** d'une ligne (`BUDGET:CONSOLIDATE`) : plus aucun engagement (contrôle rouge et bloquant).

## Les montants d'une ligne

| Montant | Source |
| --- | --- |
| Budgété | Montant annuel de la version active |
| Engagé | Engagements ouverts (`acc_budget_commitment`) moins leur part soldée |
| Réalisé | Comptabilité générale : pièces comptabilisées de l'exercice sur les comptes et postes de dépense rattachés au poste et à ses sous-postes, filtrées sur la section de la ligne par la ventilation analytique (le journal `CAG` est exclu) |
| Disponible | Budgété moins engagé moins réalisé |

## Contrôle dans les demandes de fonds

| Moment | Effet |
| --- | --- |
| Saisie (`insight.budget`) | Pour chaque ligne touchée : poste, budgété, engagé, réalisé, disponible avant et après, état vert, orange (dans la tolérance), rouge ; prévisions ouvertes du poste à deux mois près (ou récurrentes) ; lignes sans budget |
| Soumission | Budget actif : la demande cite sa **prévision** (`forecastNumber`) ou se déclare **non prévue** avec motif (`unplanned`, `unplannedCode`, `unplannedReason`), sinon refus (`fund.budget.require-forecast-choice`). Dépassement d'une ligne **bloquante** : refus (`fund.budget.blocking-mode = REFUSE`) ou **niveau de validation budgétaire en plus** (`EXTRA_APPROVAL`, défaut), qui exige `BUDGET:OVERRIDE`. Dépense non prévue : niveau en plus avec `BUDGET:APPROVE_UNPLANNED` si `fund.budget.unplanned-approval` |
| Approbation finale | **Engagement** sur chaque ligne (verrou sur la ligne) ; la dérogation est tracée (`acc_budget_override`) ; la prévision passe `COMMITTED` |
| Décaissement d'une dépense directe | Engagement **soldé** pour le montant décaissé |
| Avance soldée | Engagement soldé pour le **justifié** : l'écart est libéré |
| Rejet, annulation, expiration | Engagement **libéré** (`RELEASED` ou `EXPIRED`) ; la prévision redevient `PLANNED` |

Une prévision réalisée passe `REALIZED` (ponctuelle) ou reste `PLANNED` (récurrente), avec le montant réalisé cumulé.

## Virements de crédits

Demande (`BUDGET:TRANSFER_REQUEST`) avec motif, entre deux lignes du même budget, dans la limite du disponible de la source ; approbation (`BUDGET:TRANSFER_APPROVE`, pas par le demandeur), qui réduit la source et augmente la cible sur les mois restants. Le total ne change pas.

## Prévisions

`acc_budget_forecast` (`PRV-AAAA-000001`) : poste, section, objet, montant, mois prévu (`AAAA-MM`), récurrence (`NONE`, `MONTHLY`, `QUARTERLY`, `YEARLY`) et nombre d'occurrences, fournisseur, responsable, priorité (`ESSENTIAL`, `IMPORTANT`, `DESIRABLE`), état (`PLANNED`, `COMMITTED`, `REALIZED`, `POSTPONED`, `CANCELLED`). Motifs de dépense non prévue paramétrables (`acc_unplanned_reason` : urgence, panne, opportunité, obligation réglementaire, oubli de prévision, autre).

## Suivi et alertes

| Route | Contenu |
| --- | --- |
| `GET /reports/follow-up?fiscalYearCode=&month=` | Par ligne : budgété, engagé, réalisé, disponible, taux de consommation ; par mois : budget, budget cumulé, réalisé cumulé ; projection de fin d'exercice (rythme du réalisé plus engagé) et écart projeté |
| `GET /reports/commitments` | Engagements ouverts avec ancienneté |
| `GET /reports/planned` | Prévu et non prévu par poste, part du non prévu, motifs |
| `GET /reports/forecasts` | Prévisions, échues sans demande, écart prévu et réalisé |
| `GET /reports/reserve?fiscalYearCode=` | Réserve pour imprévus : budget non couvert par une prévision |
| `POST /check` | Simulation d'une dépense (liste de `natureCode`, `accountNumber`, `sectionCode`, `amount`) |

Alertes de nuit (traitement comptable) : franchissement de 75, 90 et 100 % de consommation par ligne (une fois par seuil) ; prévision dont le mois approche ou est passé sans demande (rappel hebdomadaire).

## Routes

Préfixe `/api/v1/accounting/budget`. Droit d'entrée : `GET` et `/check` : `BUDGET_REPORT:READ` ; `/items` : `BUDGET:PREPARE` ; `/forecasts` : `BUDGET_FORECAST:MANAGE` ; `/transfers` : `BUDGET:TRANSFER_REQUEST` ; le reste : `BUDGET:PREPARE`. Les droits fins (consolider, approuver, approuver un virement, geler, gérer la nomenclature) sont vérifiés par le service.

| Route | Rôle |
| --- | --- |
| `GET` / `POST /items`, `PATCH /items/{code}/{approve, reject, close, reopen, merge}` | Nomenclature |
| `GET ?fiscalYearCode=`, `GET /{id}`, `POST` (`fiscalYearCode`, `label`, `reason`, `basis`, `evolutionPct`) | Versions |
| `POST /{id}/lines`, `PATCH /lines/{id}/freeze?frozen=` | Lignes |
| `PATCH /{id}/{submit, consolidate, approve, reject}` (corps `{"reason": ...}`) | Circuit |
| `GET` / `POST /transfers`, `PATCH /transfers/{id}/{approve, reject}` | Virements |
| `GET` / `POST /forecasts`, `PUT /forecasts/{n}`, `PATCH /forecasts/{n}/status?status=`, `GET /unplanned-reasons` | Prévisions |

Rôles : `ADMIN` et `SUPER_ADMIN` tout ; `FINANCE` tout sauf approuver le budget et la dépense non prévue ; `MANAGER` prévisions, préparation, demande de virement, lecture.

## Paramètres

`fund.budget.blocking-mode` (`EXTRA_APPROVAL` ou `REFUSE`), `fund.budget.require-forecast-choice` (`true`), `fund.budget.unplanned-approval` (`false`), domaine Paiements, au redémarrage.

## Pour le frontend

- **Nomenclature budgétaire** : arbre comme les postes analytiques, fiche avec correspondances (comptes, postes de dépense, natures), niveau de contrôle et tolérance.
- **Élaboration** : assistant de création (vide, révision de l'actif, depuis le réalisé N-1 avec %), grille des lignes (poste, section, annuel, 12 mois, niveau), totaux charges et produits, boutons du circuit selon le droit.
- **Suivi** : tableau par ligne avec jauge de consommation (vert, orange au-delà de 75 %, rouge au-delà de 100 %), courbe budget cumulé et réalisé cumulé, projection ; engagements ouverts ; prévu et non prévu ; prévisions échues.
- **Demande de fonds** : encadré budgétaire depuis `insight.budget` (lignes, disponible avant et après, couleur), choix d'une prévision ouverte (`openForecasts`) ou case « dépense non prévue » avec motif (`GET /unplanned-reasons`) ; mention « validation budgétaire » dans les niveaux (`insight.levelLabels`).
- **Virements** et **prévisions** : écrans de saisie et de décision.

## Tests

`BudgetF16IT` (schéma de production) : budget créé, soumis, consolidé, approuvé ; une demande de 80 000 sur la ligne bloquante Missions (50 000) part avec un troisième niveau que seul `BUDGET:OVERRIDE` valide, puis l'engagement de 80 000 laisse un disponible de -30 000 ; un engagement de 100 000 soldé pour 85 000 libère 15 000 et la ligne redevient verte.

## Évolutions apportées par les lots suivants

- **Lot F13** : la validation d'une commande fournisseur contrôle le budget et engage les montants hors taxe (source `PURCHASE_ORDER`) ; la commande entièrement facturée réalise l'engagement, son annulation le libère ([finance-f13-achats](../finance-f13-achats/README.md)).
