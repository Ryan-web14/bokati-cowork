# Lot F9 : comptabilité analytique et analyse des dépenses

Lot du [plan finance](../plan-finance-comptabilite/README.md), détaillé dans [analytique-et-budget.md](../plan-finance-comptabilite/analytique-et-budget.md), sections 1 et 2. Écritures de classe 9 : [catalogue, section 13](../plan-finance-comptabilite/catalogue-ecritures.md#13-classe-9--analytique-et-hors-bilan). Le budget (section 3) est le lot F16.

Branche `feature/finance-comptabilite`. Migrations : dev `V302__comptabilite_analytique_f9.sql`, prod `V296__comptabilite_analytique_f9.sql` (contenu identique). Code : `AnalyticAllocator`, `AnalyticService`, `AnalyticReportService`, `AnalyticController` dans `features/accounting`.

**Activation** : `accounting.analytic.enabled` (faux par défaut). Inactive, rien ne change : aucune ventilation n'est demandée ni enregistrée.

## Axes et sections

| Axe | Obligatoire sur | Section par défaut | Sections livrées |
| --- | --- | --- | --- |
| `SITE` | | | à créer (siège, étages...) |
| `ACTIVITE` | charges et produits | `STRUCTURE` (auxiliaire) | bureaux privés, open space, salles, domiciliation, événements, marketplace, services |
| `CENTRE` | | | accueil, entretien, informatique, direction (auxiliaires), commercial |
| `POSTE` (postes de dépense) | charges | `NON_CLASSE` | 21 postes en familles : locaux, équipement et technique, accueil et vie du lieu, personnel, commercial, administratif, financier |
| `PROJET`, `RESSOURCE`, `CLIENT` | | | à créer |

- L'obligation (`requiredOn` : `NONE`, `CHARGE`, `PRODUCT`, `BOTH`) et la section par défaut se règlent par axe. Sans section par défaut, un axe obligatoire **impose** la ventilation.
- Une section a : code (proposé depuis le libellé, modifiable), libellé, parent (arbre), famille, nature fixe ou variable, auxiliaire (répartition secondaire), responsable, dates de validité, couleur, ordre, comptes généraux rattachés.
- **Définies par l'utilisateur** : l'administrateur finance (`ANALYTIC_SETTINGS:MANAGE`) crée immédiatement (ou après validation si `accounting.analytic.creation-mode = AFTER_APPROVAL`) ; un détenteur de `ANALYTIC_ITEM:CREATE` (responsable de centre, dans sa branche, ou proposition à la volée) crée **en attente de validation**, la dépense peut déjà y être imputée.
- Garde-fous : libellé en double refusé ; une section ne se supprime pas, elle se **ferme** (plus d'imputation, historique gardé) et se rouvre ; **fusion** dans une autre section (imputations futures redirigées, passées déplacées si `restate=true`, enfants rattachés à la cible) ; une section ne se place pas sous elle-même.
- Les **natures d'opération** de caisse portent leur poste de dépense (`defaultSection`) : la caissière et le demandeur classent la dépense sans le savoir.

## Ventilation

Chaque ligne de classe 6 ou 7 (et de compte marqué analytique) reçoit, pour chaque axe actif, une section, par priorité :

1. ce que porte la ligne : saisie manuelle ou fait métier (`analytic` de la ligne, `{"ACTIVITE": "SALLES"}`, ou `"KEY:SURFACES"` pour une clé) ; les schémas de caisse et de justification d'avance passent le poste de la nature (versions 2 des schémas `CASH_OUT`, `CASH_IN`, `EXPENSE_JUSTIFIED`) ;
2. une **règle** (`acc_analytic_rule`, par priorité) : préfixe de compte, journal, type de fait, tiers, vers une section ou une clé ;
3. le **compte** : correspondance section et préfixes de comptes (le plus long gagne) ; 18 correspondances livrées pour les postes (loyer `622`, électricité `6052`, fournitures `6055`...) ;
4. la section par défaut de l'axe.

- Une **clé de répartition** (`acc_allocation_key`, versions datées, parts à 100 %) partage la ligne entre sections, le dernier reçoit l'arrondi.
- Section manquante sur un axe obligatoire, inconnue, fermée ou hors validité : **saisie manuelle refusée** ; pièce automatique mise **en attente** (motif `ANALYTIC_INCOMPLETE`), régénérable après correction (lot F2).
- À la validation, la ventilation est enregistrée (`acc_analytic_allocation`, avec son origine : `INPUT`, `RULE`, `ACCOUNT`, `DEFAULT`, `..._KEY`, `RECLASS`).
- **Classe 9 en partie double** : l'axe `accounting.analytic.class9-axis` (`ACTIVITE`) est réfléchi au journal `CAG` : charge D `93` (référence : section) C `91`, produit D `91` C `93`. La pièce de reflet porte la référence de la pièce générale (`sourceType = ANALYTIC_REFLECTION`).

## Reclassement et répartition secondaire

- **Reclassement** (`ANALYTIC:RECLASS`) : déplace tout ou partie d'une ventilation d'une section à une autre, avec motif, sans toucher à l'écriture générale ; tracé (`acc_analytic_reclass`) ; sur l'axe réfléchi, pièce `CAG` D `93` nouvelle section C `93` ancienne.
- **Répartition secondaire** d'une période : les charges des sections **auxiliaires** d'un axe sont déversées sur les autres sections selon une clé ; rejouable (remplace la précédente de la période) ; visible dans le résultat analytique (charges reçues et cédées).

## États

| Route | Contenu |
| --- | --- |
| `GET /reports/result?axis=&from=&to=` | Compte de résultat analytique par section : produits, charges directes, charges reçues et cédées par répartition, marge sur coûts directs, résultat ; total |
| `GET /reports/items/{poste}?from=&to=` | Analyse d'un poste : montant de la période, cumul de l'exercice, même période N-1 et écart en %, projection de fin d'exercice, 12 mois glissants, décomposition par site, centre, fournisseur (tiers de la pièce), journal ; détail jusqu'à la pièce (500 lignes) ; alerte de hausse de plus de 30 % sur la moyenne des 3 mois précédents |
| `GET /reports/dashboard?from=&to=` | Tableau de bord des dépenses : chaque poste avec la période, la période précédente de même durée, l'écart en %, la moyenne des 3 mois, trié par dérive (les premiers dérivent le plus) |
| `GET /reports/balance?axis=&from=&to=` | Balance analytique par section et par compte |
| `GET /reports/control?axis=&from=&to=` | Contrôle : montant ventilé face au total des classes 6 et 7 de la comptabilité générale |

Les comparaisons au budget arrivent avec le lot F16.

## Routes

Préfixe `/api/v1/accounting/analytic`. Écritures auditées.

| Route | Droit |
| --- | --- |
| tous les `GET` | `ANALYTIC_REPORT:READ` |
| `POST /axes/{axe}/sections` | `ANALYTIC_ITEM:CREATE` (le service distingue administrateur et proposition) |
| `POST /reclass` | `ANALYTIC:RECLASS` |
| `POST /axes`, `PATCH /axes/{axe}/sections/{code}`, `.../approve`, `/reject`, `/close`, `/reopen`, `/merge?into=&restate=`, `POST /rules`, `PUT` / `DELETE /rules/{id}`, `POST /keys`, `POST /distribution?periodCode=&axisCode=&keyCode=` | `ANALYTIC_SETTINGS:MANAGE` |

Autres lectures : `GET /axes`, `/axes/{axe}/sections?includeClosed=` (arbre), `/sections/pending`, `/rules`, `/keys`, `/entries/{référence}/allocations`.

Rôles : `ADMIN` et `SUPER_ADMIN` tout ; `FINANCE` proposer, reclasser, lire ; `MANAGER` proposer, lire.

## Paramètres (domaine Comptabilité, à chaud)

`accounting.analytic.enabled` (`false`), `accounting.analytic.class9-axis` (`ACTIVITE`, vide : pas de reflet), `accounting.analytic.centre-account` (`93`), `accounting.analytic.reflected-account` (`91`), `accounting.analytic.creation-mode` (`IMMEDIATE`).

## Pour le frontend

- **Écran « Postes analytiques »** : onglets par axe, arbre (glisser-déposer : `PATCH` avec `parentCode`), création en un champ (libellé, le code est proposé), fiche (famille, fixe ou variable, responsable, comptes rattachés, validité, couleur), fermer, rouvrir, fusionner (case « retraiter le passé »), file des propositions à valider.
- **Saisie d'écriture** : pour chaque ligne de classe 6 ou 7, sélecteurs de section par axe actif (ou clé), l'erreur « section … obligatoire » s'affiche sur la ligne.
- **Fiche d'une pièce** : onglet « Analytique » (`/entries/{réf}/allocations`) et action « Reclasser ».
- **Analyse d'un poste** : courbe 12 mois, comparatifs, camemberts site, centre, fournisseurs, tableau de détail cliquable vers la pièce, alertes.
- **Tableau de bord des dépenses** : tableau trié par dérive, mise en avant des 5 premiers, carte de chaleur à partir de `/reports/items` sur 12 mois.
- **Paramétrage** : règles (grille avec priorité), clés de répartition (versions, total à 100 %), axes (obligation, section par défaut), répartition secondaire d'une période.

## Tests

`AnalyticF9IT` (schéma de production) : dépense sans section refusée, ventilation sur activité et poste (par le compte), reflet en classe 9 au journal `CAG` ; résultat par section (produits, charges, marge), clé de répartition 60/40, reclassement, analyse d'un poste, tableau de bord ; poste proposé en attente puis validé, libellé en double refusé, fusion.

## Évolutions apportées par les lots suivants

- **Lot F16** : les postes de dépense sont reliés aux postes budgétaires (`acc_budget_item_mapping`, type `EXPENSE_ITEM`) ; le réalisé budgétaire d'une ligne avec section filtre les lignes d'écriture par leur ventilation analytique. La comparaison au budget se lit dans le suivi budgétaire ([finance-f16-budget](../finance-f16-budget/README.md)).
