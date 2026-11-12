# Lot C0 : correctifs des caisses

Premier lot du [plan finance](../plan-finance-comptabilite/README.md) (lot F8, détail dans [caisses-et-sessions.md](../plan-finance-comptabilite/caisses-et-sessions.md#20-découpage-en-lots-et-critères-dacceptation)). Il corrige les défauts D1 à D6 relevés dans l'existant, sans changer le fonctionnement des caisses.

Branche `feature/finance-comptabilite`. Migrations : dev `V291__caisse_correctifs_c0.sql`, prod `V285__caisse_correctifs_c0.sql` (contenu identique).

## Ce qui change

| Défaut | Correction | Où |
| --- | --- | --- |
| **D1** Acteurs saisis dans le corps de la requête | L'acteur est l'utilisateur connecté (nom du principal, c'est-à-dire son courriel), pour : ouverture, clôture, validation d'écart, mouvements, bons, pièces jointes, signature, demandes de caisse (demandeur et décideur), revue d'anomalie, encaissement en espèces au guichet, encaissement hors plateforme, confirmation d'une annonce d'espèces. Les champs `openedBy`, `closedBy`, `reviewedBy`, `createdBy`, `uploadedBy`, `requestedBy`, `receivedBy` et le paramètre `signedBy` restent acceptés mais sont **ignorés** en contexte authentifié ; ils ne servent qu'aux appels internes sans utilisateur (traitements, workers). Ils ne sont plus obligatoires. | `CashActorResolver` ; `CashRegisterServiceImpl`, `CashRequestServiceImpl`, `CashAnomalyDetectionServiceImpl`, `PaymentServiceImpl`, `CashPaymentDeclarationService` |
| **D2** Droits trop grossiers | Une permission par action (tableau ci-dessous) au lieu de `CASH:ADJUST` pour tout | `AdminApiAuthorizationManager#cashPermission` ; migration |
| **D3** Le caissier peut valider son propre écart | Refus (403) si le validateur est celui qui a ouvert ou clos la session. Même règle pour une demande de caisse : le demandeur ne l'approuve ni ne la rejette | `approveVariance`, `CashRequestServiceImpl#reviewer` |
| **D4** Solde courant faux (fonds d'ouverture compté deux fois) | Le solde courant part de zéro et additionne les mouvements selon la règle unique ; il est désormais posé sur **tous** les mouvements (bons, paiements, fonds), pas seulement sur les mouvements libres. La migration recalcule le solde courant de tout l'historique | `CashRegisterServiceImpl#computeRunningBalance` ; migration |
| **D5** Ajustement sans sens | `ADJUSTMENT` exige `direction` = `IN` ou `OUT` (400 sinon) ; il entre ou sort de l'attendu, du solde courant, des statistiques et des états. Les ajustements existants reçoivent `IN`, qui était leur effet sur l'attendu : aucun attendu passé ne change. Contrainte en base : un `ADJUSTMENT` sans sens est refusé | `cash_movement.direction`, `ck_cash_movement_direction` |
| **D6** Totaux sans filtre de statut | Seuls `CONFIRMED` et `REVERSED` comptent, partout | requêtes de `CashMovementRepository`, `FinancialReportRepository` |
| Règle unique de sens | Une seule table de vérité : entrée (`OPENING_FLOAT`, `PAYMENT`, `CASH_IN`, `TRANSFER_IN`, ajustement `IN`), sortie (`REFUND`, `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_OUT`, ajustement `OUT`), neutre (`CLOSING_COUNT`). Le champ `flowDirection` de la réponse d'un mouvement montre maintenant `IN` ou `OUT` pour un ajustement ; seul un comptage reste `ADJUSTMENT` | `CashFlowRules`, `CashRegisterMapperDecorator` |

Les états financiers de caisse (`/reports/.../cash-registers`, `/cash-flow`) suivent la même règle : un ajustement en entrée compte dans les entrées, en sortie dans les sorties ; il était auparavant classé en sortie dans la répartition par type et exclu de la série quotidienne.

## Permissions

| Route | Permission |
| --- | --- |
| Toute lecture (`GET /cash-registers/...`) | `CASH:READ` |
| `POST /cash-registers`, `PATCH /cash-registers/{code}/activate`, `/deactivate` | `CASH_REGISTER:MANAGE` |
| `POST /cash-registers/sessions` (ouverture) | `CASH_SESSION:OPEN` |
| `PATCH .../sessions/{n}/close`, `POST .../sessions/{n}/closing-request` | `CASH_SESSION:CLOSE` |
| `PATCH .../sessions/{n}/approve-variance`, `/cash-registers/anomalies/...` (analyse, revue) | `CASH_VARIANCE:DECIDE` |
| `PATCH /cash-registers/requests/{n}/approve`, `/reject` | `CASH_MOVEMENT:APPROVE` |
| `.../reverse` (contre-passation, lot C3) | `CASH_MOVEMENT:REVERSE` |
| Toute autre écriture : mouvements, bons, demandes, pièces jointes, signature, impression | `CASH_MOVEMENT:CREATE` |

Attribution par la migration :

- reprise des anciens droits : qui avait `CASH_OPEN_SESSION` reçoit `CASH_SESSION_OPEN`, `CASH_CLOSE_SESSION` reçoit `CASH_SESSION_CLOSE`, `CASH_ADJUST` reçoit `CASH_MOVEMENT_CREATE` ;
- `CASH_REGISTER_MANAGE` et `CASH_MOVEMENT_REVERSE` : `SUPER_ADMIN`, `ADMIN`, `MANAGER` ;
- `CASH_VARIANCE_DECIDE` et `CASH_MOVEMENT_APPROVE` : `SUPER_ADMIN`, `ADMIN`, `MANAGER`, `FINANCE`. **Pas** le rôle `CASHIER` : un caissier ne valide plus d'écart ni de demande. Si un caissier principal doit le faire, lui donner ces droits explicitement depuis l'écran des rôles.

Les anciennes permissions `CASH_OPEN_SESSION`, `CASH_CLOSE_SESSION`, `CASH_ADJUST` restent en base mais ne sont plus exigées par aucune route.

## Points d'attention

- **Un caissier qui n'a que le rôle `CASHIER` ne peut plus créer de caisse** (il le pouvait avec `CASH_ADJUST`). C'est voulu.
- **Les identifiants d'acteur deviennent des courriels** pour les nouvelles sessions et les nouveaux mouvements. Les anciennes valeurs restent telles quelles. Le contrôle « une seule session active par caissier » compare désormais le courriel du jeton ; une session ouverte avant le déploiement sous un autre identifiant ne bloque donc pas une nouvelle ouverture du même caissier.
- **Demande de caisse de type `JUSTIFICATIF`** : elle produit un `ADJUSTMENT` de sens `IN`, comme avant (l'approbation ajoutait le montant à l'attendu). Ce comportement discutable sera revu avec les natures d'opération (lot C3).

## Tests

- `CashCorrectionsC0Test` (unitaire) : acteur du jeton à l'ouverture, refus de l'auto-validation d'écart, validation par un tiers, solde courant 10 000 + 5 000 = 15 000, ajustement sans sens refusé, ajustement en sortie qui baisse le solde, statuts qui comptent.
- `AdminApiAuthorizationManagerTest` : une permission par route de caisse ; un caissier qui n'a que les anciens droits ne valide pas d'écart ni ne crée de caisse.
- `CashCorrectionsC0IT` (PostgreSQL réel, migrations de production) : totaux signés et filtrés, contrainte sur le sens, permissions présentes.
- Suite unitaire complète : 1 980 tests, 0 échec.

## Pour le frontend

À reporter dans la doc frontend finale (rédigée à la fin de toute l'implémentation) :

- ne plus envoyer d'acteur ; s'il est envoyé, il est ignoré ;
- un ajustement (`movementType = ADJUSTMENT`) doit envoyer `direction` : `IN` ou `OUT` ;
- `flowDirection` d'un ajustement vaut `IN` ou `OUT` ;
- 403 sur `approve-variance` pour le caissier de la session, et sur la décision d'une demande par son demandeur ; message en français dans la réponse ;
- masquer les actions selon les nouvelles permissions.
