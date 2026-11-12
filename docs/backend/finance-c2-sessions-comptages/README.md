# Lot C2 : sessions et comptages

Troisième lot du [plan finance](../plan-finance-comptabilite/README.md), détaillé dans [caisses-et-sessions.md](../plan-finance-comptabilite/caisses-et-sessions.md), sections 5, 6 et 18. Lots précédents : [C0](../finance-c0-caisse-correctifs/README.md), [C1](../finance-c1-referentiel-caisses/README.md).

Branche `feature/finance-comptabilite`. Migrations : dev `V293__caisse_sessions_comptages_c2.sql`, prod `V287__caisse_sessions_comptages_c2.sql` (contenu identique).

## Compatibilité au déploiement

Les **caisses existantes gardent leur comportement actuel**, pour que l'interface en place continue de fonctionner :

- `blind_close = false` : l'attendu reste affiché ;
- `count_lines_required = false` : un total suffit, sans billetage ;
- `month_end_policy = CARRY_OVER` : la clôture de fin de mois sans comptage, avec report, continue.

Les **nouvelles caisses** suivent le plan : clôture à l'aveugle, billetage obligatoire, `REQUIRE_COUNT` en fin de mois. On bascule une caisse existante par `PATCH /cash-registers/{code}` quand le frontend est prêt. Ces trois champs sont modifiables sur la fiche, avec la tolérance (`countTolerance`, `countTolerancePercent`), le seuil de double comptage (`doubleCountThreshold`, 10 000 XAF) et la durée maximale (`maxSessionHours`, 24 h).

Les routes historiques continuent de marcher :

- `PATCH .../close` et `POST .../closing-request` (total et motif libre) passent par le nouveau calcul de tolérance, de double comptage et de revue ;
- `PATCH .../approve-variance` applique la décision par défaut.

## États d'une session

`OPENING`, `OPEN`, `PAUSED`, `SUSPENDED`, `COUNTING`, `CLOSING_REVIEW`, `RECOUNT`, `CLOSED`, `CANCELLED`. Une session non close ni annulée occupe sa caisse et son caissier.

| Étape | Route | Qui | Effet |
| --- | --- | --- | --- |
| Fonds attendu | `GET /cash-registers/{code}/expected-opening` | Caissier | Dernier comptage de la caisse, fonds standard, coupures |
| Ouverture | `POST /cash-registers/sessions` (+ `openingCount` : billetage) | Caissier affecté | Fonds compté égal au dernier comptage : `OPEN`. Sinon : `OPENING` avec l'écart (billetage exigé si la caisse l'impose) |
| Confirmation d'un écart d'ouverture | `PATCH .../sessions/{n}/confirm-opening` (motif de la liste `VARIANCE_REASON`) | Le caissier | `OPEN` ; revue d'écart `OPENING` ; responsable prévenu |
| Pause, reprise | `PATCH .../pause`, `/resume` (`password`) | Le caissier | Durée de pause cumulée ; mot de passe redemandé à la reprise |
| Suspension, levée | `PATCH .../suspend` (motif `SUSPENSION_REASON`), `/unsuspend` | Superviseur, pas le caissier | Plus d'opération ; caissier prévenu |
| Demande de clôture | `PATCH .../start-closing` | Le caissier | `COUNTING` : plus d'opération |
| Comptage de clôture | `POST .../sessions/{n}/counts` (billetage, fonds gardé, motif d'écart) | Le caissier ; en recomptage, une **autre personne** | Voir ci-dessous |
| Recomptage | `PATCH .../recount` | Réviseur | `RECOUNT` |
| Décision d'écart | `PATCH .../variance-decision` ou `PATCH /cash-registers/variance-reviews/{n}/decision` | Réviseur, **jamais le caissier** | `EXPENSE` (charge), `INCOME` (produit), `CASHIER` (imputation au caissier), `INVESTIGATION` (en recherche, échéance de 30 jours) ; par défaut : charge pour un manquant, produit pour un excédent. La session passe `CLOSED` |
| Comptage inopiné | `POST .../spot-checks` | Superviseur, pas le caissier | Session non interrompue ; écart en revue s'il sort de la tolérance ; PV `PCI` |
| Clôture d'office | `PATCH .../force-close` (comptage avec `witnessEmail`, motif) | Superviseur, avec un **témoin** différent de lui et du caissier | PV `PCO` ; s'il y a écart, revue `FORCED_CLOSE` contestable pendant 3 jours |
| Contestation | `PATCH .../contest` | Le caissier | Tant que le délai court sans contestation, l'écart ne peut pas être décidé |
| Annulation | `PATCH .../cancel` (motif `SESSION_CANCEL_REASON`) | Caissier ou superviseur | Seulement sans opération : `CANCELLED`, fonds d'ouverture annulé |

**Évaluation d'un comptage de clôture** :

1. Attendu = fonds d'ouverture + mouvements (règle unique de C0). Le comptage est enregistré (`cash_count`, jamais modifié) avec l'attendu et l'écart, qui ne sont révélés qu'après validation.
2. Écart au-delà du seuil de double comptage, au premier comptage : `RECOUNT`. Une autre personne recompte.
3. Écart nul ou dans la tolérance (montant, ou pourcentage de l'attendu) : `CLOSED`. Un écart toléré est enregistré comme revue `TOLERATED`.
4. Au-delà : motif obligatoire, `CLOSING_REVIEW`, revue `PENDING`, réviseurs prévenus, ainsi que leurs délégués.
5. Le fonds gardé (proposé : le fonds standard de la caisse) et le montant à remettre sont enregistrés. Le transfert de la remise arrive au lot C3. Le dernier comptage de la caisse est mis à jour.

**Clôture à l'aveugle** : sur une caisse `blindClose`, tant que la session n'est pas comptée, `expectedClosingAmount`, `closingAmount`, `countedClosingAmount`, `varianceAmount` et le solde courant des mouvements sont vides pour qui n'a pas `CASH_VARIANCE:DECIDE` ; `expectedHidden` vaut alors vrai.

## Billetage

Coupures datées par devise (`cash_denomination`). Pour le XAF : billets de 10 000, 5 000, 2 000, 1 000 et 500 ; pièces de 500, 100, 50, 25, 10, 5, 2 et 1. Les billets abîmés ou suspects se saisissent à part (`damagedQuantity`) et sont exclus du total. Une coupure retirée reçoit une date de fin, sans perte d'historique (`GET`, `PUT /cash-registers/denominations[/{devise}]`).

## Passation

1. `POST .../sessions/{n}/handover` : le sortant compte et désigne l'entrant, qui doit être affecté à la caisse. La session du sortant passe `COUNTING`. PV `PVP`, signé par le sortant.
2. `PATCH /cash-registers/handovers/{n}/accept` : l'entrant confirme, éventuellement avec son propre comptage (s'il diffère, c'est une contestation). La session du sortant se clôt sur ce montant, celle de l'entrant s'ouvre avec lui. Le PV est signé par les deux.
3. `PATCH .../dispute` : l'entrant compte et explique le désaccord. La session du sortant passe en revue `HANDOVER`, et l'entrant ouvre avec son montant.
4. `PATCH .../cancel` : le sortant annule tant que l'entrant n'a pas répondu.

## Absences

- **Déclaration** : `POST /cash-registers/absences`, motif de la liste `ABSENCE_REASON`, dates, remplaçant facultatif.
  - Faite par l'intéressé, elle est `PLANNED` et attend une validation. Faite par un responsable (`CASH_STAFF:MANAGE`), elle est validée d'office.
  - Le remplaçant est proposé d'office : premier remplaçant disponible, sinon un caissier d'une autre caisse du site (`GET .../absences/replacement-suggestions`).
- **Validation** : l'intérim du remplaçant est créé sur toutes les caisses de l'absent, du premier jour au lendemain du dernier. Il dure 30 jours, à prolonger, si la fin n'est pas connue.
- **Pendant l'absence** : l'absent ne peut ouvrir aucune session. Le traitement planifié (`bokati.cash.staff-worker-cron`) fait trois choses :
  - il relance l'absent deux jours avant ;
  - au premier jour, il suspend sa session encore ouverte (à clore d'office) ;
  - il clôt l'absence à sa fin.
- **Fin** : `PATCH .../end` ou `/cancel` arrête les intérims. Au retour, le titulaire reprend la caisse par une passation.
- **Départ** (`DEPARTURE`) :
  - il est bloqué tant qu'une session n'est pas close ou qu'un écart n'est pas décidé, sauf forçage motivé (`force`, `forceNote`) ;
  - à la date de départ, toutes les affectations prennent fin, et une alerte rappelle de faire une remise de caisse s'il était titulaire.
- **Sans remplaçant** : la règle choisie (`REMIT_TO_PARENT`, `SEAL`, `SIMPLE_SUSPENSION`) est enregistrée. Les actions correspondantes se font avec les transferts (lot C3) ou les scellés.

## Scellés

`POST /cash-registers/{code}/seal` : il faut qu'aucune session ne soit en cours. Le fonds est compté à deux (témoin différent), avec un numéro d'enveloppe et un lieu de dépôt ; la caisse est suspendue ; PV `PVS`. `POST .../unseal` : comptage contradictoire, revue `UNSEAL` en cas d'écart, caisse réactivée.

## Délégations

`POST /cash-registers/delegations` : un réviseur délègue `CASH_VARIANCE_DECIDE` et `CASH_MOVEMENT_APPROVE` pour une période. La délégation **ne donne aucun droit** : le délégué doit déjà avoir ces permissions. Elle redirige vers lui les notifications de revue.

## Fin de mois et durée maximale

- `REQUIRE_COUNT` : le premier du mois, une session de caisse manuelle encore ouverte est suspendue, et un superviseur la clôt d'office. `CARRY_OVER` : ancien comportement.
- Une session ouverte au-delà de la durée maximale déclenche une alerte unique, au caissier et au responsable de la caisse.

## Droits

| Permission | Routes | Attribution par la migration |
| --- | --- | --- |
| `CASH_SESSION:CLOSE` | démarrage de clôture, comptages, passation, confirmation d'ouverture, annulation, contestation | (C0) |
| `CASH_SESSION:PAUSE` | pause, reprise | qui a `CASH_SESSION_OPEN` |
| `CASH_SESSION:SUSPEND` | suspension, levée | qui a `CASH_VARIANCE_DECIDE` |
| `CASH_SESSION:FORCE_CLOSE` | clôture d'office | qui a `CASH_VARIANCE_DECIDE` |
| `CASH_SESSION:SPOT_CHECK` | comptage inopiné | qui a `CASH_VARIANCE_DECIDE` |
| `CASH_VARIANCE:DECIDE` | décision, recomptage, liste des revues, délégations | (C0) |
| `CASH_SESSION:OPEN` | réponse de l'entrant à une passation | (C0) |
| `CASH_STAFF:DECLARE_ABSENCE` | déclarer, annuler une absence | qui a `CASH_SESSION_OPEN` |
| `CASH_STAFF:MANAGE` | valider et terminer une absence, scellés | (C1) |
| `CASH_REGISTER:MANAGE` | coupures | (C0) |

## Tests

- `CashSessionLifecycleServiceImplTest` :
  - clôture exacte, écart toléré, motif exigé hors tolérance ;
  - recomptage par une autre personne ;
  - décision refusée au caissier, charge par défaut ;
  - clôture d'office avec témoin et contestation ;
  - annulation refusée avec des opérations ;
  - reprise avec mot de passe ;
  - passation acceptée et contestée ;
  - écart d'ouverture ;
  - tolérance en pourcentage.
- `CashStaffServiceImplTest` :
  - congé avec intérim aux bonnes dates ;
  - auto-validation refusée ;
  - suspension au premier jour ;
  - départ bloqué puis forcé ;
  - scellés.
- `CashRegisterAccessGuardTest` : un absent ne peut pas ouvrir.
- `CashCorrectionsC0Test` : ouverture en `OPENING` sur écart.
- `CashSessionsC2IT` (PostgreSQL) : coupures, paramètres des nouvelles caisses, schéma validé.
- `CashRoutesMappingTest` : toutes les routes de caisse cohabitent sans ambiguïté.
- Suite unitaire complète : 2 018 tests, 0 échec.

## Pour le frontend

À reporter dans la doc frontend finale :

- **Ouverture** : afficher le fonds attendu ; saisir le billetage par coupure ; si la réponse est `OPENING`, demander le motif puis appeler `confirm-opening`.
- **Clôture** : appeler `start-closing`, puis faire saisir le billetage sans afficher l'attendu, puis `counts`. La réponse indique l'issue : `CLOSED`, `RECOUNT` (une autre personne recompte) ou `CLOSING_REVIEW`, avec des `messages` à afficher.
- **Écrans** :
  - revues d'écart (liste, décision avec proposition, échéance de contestation, `decidableNow`) ;
  - pause avec verrou et mot de passe ;
  - passation des deux côtés ;
  - calendrier des absences et remplaçants ;
  - scellés ;
  - délégations ;
  - coupures.
- **Respecter `expectedHidden`.**
