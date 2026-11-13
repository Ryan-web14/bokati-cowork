# Produits et charges constatés d'avance

Contrat exact de chaque route (paramètres, corps, réponse, champs JSON) : [routes.md](routes.md). Ce fichier explique le fonctionnement et ce qu'il faut à l'écran. Index du module : [README.md](README.md).

## Période de service d'une ligne (`DeferralService.sync`, chaque nuit)

| Source | Période | Méthode |
| --- | --- | --- |
| Période saisie sur la ligne | `service_start` à `service_end` | Celle de la ligne, sinon `DAILY` (ou `EVENT_DATE` si un seul jour) |
| Élément facturable (abonnement, pass, option...), code de l'article de la ligne | `billingPeriodStart` à `billingPeriodEnd` de l'élément | `DAILY` |
| Réservation (`sourceType` `BOOKING`) | Jour du créneau | `EVENT_DATE` |
| Ligne d'avoir | Échéancier de la ligne corrigée (lien `original_line_id`, sinon même code d'article sur la facture d'origine), gels compris | Celle de la ligne corrigée |
| Autres lignes | Aucune : reconnue à la date de la facture | `POINT_IN_TIME` |

- La période trouvée est écrite sur la ligne.
- Une ligne d'abonnement ou de pass sans période est marquée `MISSING` et signalée.
- Côté charges : une ligne de facture fournisseur comptabilisée, sur un compte `6`, au-dessus du seuil (50 000 HT), avec une période saisie. À défaut de période, une nature d'opération marquée périodique donne une durée en mois depuis la date de la facture.

## Calcul (part non reconnue à une date, le « besoin »)

- `DAILY` : montant HT × jours restants / jours de service. Les dates sont incluses, les jours de gel sont exclus. Le reconnu est arrondi au franc, la dernière échéance absorbe l'arrondi.
- `MONTHLY` : par mois entièrement écoulé.
- `EVENT_DATE` : tout reste en `477` jusqu'à la date du service.
- `USAGE` : montant × (1 - crédits consommés / crédits achetés). Le solde est reconnu à l'expiration.
- Un échéancier terminé (résiliation, annulation, expiration) est entièrement reconnu à sa date de fin.

## Traitement de fin de période

- Le besoin de chaque échéancier à la fin du mois est comparé au dernier besoin comptabilisé. Une seule pièce passe la différence, regroupée par compte de produit ou de charge :
  - produits : D produit, C `477` si le besoin monte ; l'inverse s'il baisse ;
  - charges : D `476`, C charge si le besoin monte ; l'inverse s'il baisse.
- Relancer un mois ne produit aucune écriture.
- Une période verrouillée renvoie la pièce sur la première période ouverte.
- Rythme : `MONTHLY` par défaut, `YEAR_END` (décembre seulement) ou `NONE`, séparément pour les produits et pour les charges.
- Le traitement tourne le 1er du mois à 4 h pour le mois écoulé, et à la demande avec simulation.

## Événements

| Événement | Effet |
| --- | --- |
| `FREEZE` (gel de N jours) | La fin recule de N jours, rien n'est reconnu pendant le gel |
| `TERMINATION`, `CANCELLATION` | Le reste est reconnu à la date (part conservée sans remboursement) |
| `EXPIRY` | Pass expiré : solde reconnu |
| `RESCHEDULE` | Service ponctuel déplacé à une nouvelle date |

- Un avoir de résiliation crée un échéancier négatif qui suit celui de la ligne corrigée : le `477` de la ligne retombe à zéro.
- `freezeDocuments` applique un gel à toutes les factures en cours d'un abonnement.

## Factures à établir et non parvenues

- Estimation proposée : facture à établir sur un compte `7`, facture non parvenue sur un compte `6`, avec TVA si elle est connue.
- Comptabilisation en fin de mois, avec son extourne au premier jour du mois suivant :
  - à établir : D `4181`, C produit ;
  - non parvenue : D charge, D `4454`, C `408`.
- Rapprochement avec la facture réelle : l'écart est tracé.

## Contrôles et clôture

Contrôles quotidiens :
- solde du `477` (et du `476`) égal au détail des échéanciers (bloquant) ;
- lignes d'abonnement ou de pass sans période ;
- estimation non rapprochée d'une facture réelle après 60 jours.

La liste de clôture (`closing-check`) exige :
- le traitement de chaque mois de l'exercice où un échéancier existait, décembre compris ;
- un écart nul.

**Réglé** : la liste de clôture du lot F12 appelle ce contrôle (étape `DEFERRALS`).

## Routes (`/sni/api/v1`)

| Route | Usage |
| --- | --- |
| `GET /accounting/deferrals?kind=REVENUE\|EXPENSE&date=` | Détail par ligne, justificatif du `477` ou du `476` |
| `GET /accounting/deferrals/schedules/{id}` | Échéancier, événements, consommations, besoin mois par mois |
| `POST /accounting/deferrals/schedules/{id}/events`, `/usage` | Gel, résiliation, report, expiration ; crédits consommés |
| `PATCH /billing/documents/{number}/lines/{lineId}/service-period` | Corriger la période d'une ligne, avec motif |
| `POST /accounting/deferral-runs/preview?period=&kind=`, `POST /accounting/deferral-runs?period=&kind=` | Simulation, comptabilisation |
| `GET`, `POST /accounting/accruals`, `POST /accounting/accruals/{id}/post`, `/match` | Factures à établir et non parvenues |
| `GET /accounting/deferrals/controls`, `/closing-check?year=` | Contrôles, clôture |
| `GET /accounting/deferrals/reports/{report}?year=&date=&format=json\|csv\|pdf` | États |

États disponibles :

| Code | Contenu |
| --- | --- |
| `revenue-detail` | Détail des produits constatés d'avance |
| `expense-detail` | Détail des charges constatées d'avance |
| `revenue-forecast` | Chiffre d'affaires facturé à reconnaître sur 12 mois |
| `billed-vs-recognized` | Chiffre d'affaires facturé et reconnu par mois |
| `accruals` | Factures à établir et non parvenues |
| `pass-credits` | Crédits de pass non consommés |
| `anomalies` | Anomalies de période et écarts |

## Paramètres (`ACCOUNTING`, tous lus à chaud)

| Thème | Clés |
| --- | --- |
| Traitement | `accounting.deferral.cron`, `accounting.deferral.journal` |
| Rythme | `accounting.revenue.deferral`, `accounting.expense.deferral` |
| Comptes | `accounting.revenue.deferred-account`, `accounting.expense.deferred-account` |
| Seuil | `accounting.expense.deferral-threshold` |
| Estimations | `accounting.accrual.revenue-account`, `expense-account`, `expense-vat-account`, `reversal-journal`, `alert-days` |

## Limites connues

- **Réglé** : gel automatique. Le gel d'un abonnement (`SubscriptionFreezeService`) recule les échéanciers des factures de ses éléments facturables, par l'interface `core/accounting/DeferralEvents`. Une reprise anticipée ramène la fin à la durée réellement gelée.
- **Réglé** : consommations de pass. Un pass à crédits (droits non illimités) est reconnu à l'usage. Les crédits achetés viennent de ses droits, les consommations du journal des droits (`entitlement_ledger`, mouvements `CONSUME`). La route `usage` reste disponible pour une saisie complémentaire.
- **Réglé en partie** : estimations automatiques. Le 1er du mois, `proposeAccruals` propose deux types d'estimation, une seule fois par référence et par période :
  - les dépassements d'abonnement non facturés, en factures à établir ;
  - les commandes fournisseurs validées, attendues dans la période et pas entièrement facturées, en factures non parvenues (par ligne).

  Le comptable les comptabilise. Les réservations facturées à terme échu et les contrats récurrents restent à saisir.
- **Réglé** : analytique. La section de la ligne d'origine est portée sur la ligne de produit ou de charge de l'écriture de fin de période, sur l'axe `accounting.deferral.analytic-axis` (à défaut, celui des achats).
- **Lignes de justification d'avance** : elles ont leurs colonnes de période, mais pas encore d'échéancier.
- **Remises** : une remise de document est étalée avec la ligne au prorata de son brut.
- **Réglé** : facture PDF. Chaque ligne à période affiche « Période : du 15/10/2026 au 14/10/2027 » (ou « le 10/01/2027 »). La réponse de la facture porte `serviceStart`, `serviceEnd` et `servicePeriod` sur chaque ligne.

## Pour le frontend

Écrans à prévoir :
- détail du `477` et du `476` à une date ;
- fiche d'échéancier, avec son graphique mois par mois et ses événements ;
- traitement de fin de période : simulation puis comptabilisation ;
- factures à établir et non parvenues ;
- états ;
- sur la facture, la période de service de chaque ligne, modifiable avec le droit `EDIT_PERIOD`.
