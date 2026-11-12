# Lot C1 : référentiel des caisses

Deuxième lot du [plan finance](../plan-finance-comptabilite/README.md) (détail dans [caisses-et-sessions.md](../plan-finance-comptabilite/caisses-et-sessions.md), sections 3, 4, 18, 19 et 21). Il donne à chaque caisse une fiche complète et décide de qui peut la tenir. Lot précédent : [C0](../finance-c0-caisse-correctifs/README.md).

Branche `feature/finance-comptabilite`. Migrations : dev `V292__caisse_referentiel_c1.sql`, prod `V286__caisse_referentiel_c1.sql` (contenu identique).

## Ce qui change pour l'exploitation

- **Seules les personnes affectées à une caisse y ouvrent une session.** La migration affecte automatiquement, comme remplaçant, chaque personne qui a ouvert une session sur une caisse dans les 90 derniers jours. **La liste est à revoir avant la mise en service** (désigner les titulaires). Le paramètre `bokati.cash.assignment.enforced` (vrai par défaut) permet de suspendre ce contrôle le temps de compléter les affectations.
- Une caisse suspendue ou fermée refuse toute session ; un coffre n'a pas de session.
- Si une caisse a des **horaires**, une ouverture en dehors exige un motif de la liste.
- Si une caisse a des **postes autorisés**, seule une ouverture depuis l'un d'eux est acceptée. Tant qu'aucun n'est autorisé, un poste inconnu est simplement enregistré comme « proposé ».

## Fiche d'une caisse

| Groupe | Champs | Origine |
| --- | --- | --- |
| Identité | code (généré), libellé, **type** (`COUNTER` guichet, `MAIN` principale, `PETTY_CASH` petite caisse, `SAFE` coffre, `AUTOMATIC`), **statut** (`ACTIVE`, `SUSPENDED`, `CLOSED`) et son motif, site (`locationCode`), organisation, **caisse mère** | Caisse mère déduite : la caisse principale du site |
| Moyens | moyens acceptés (`PaymentMethod`), devises acceptées | Hérités du type (guichet : espèces, chèque, virement déclaré, mobile money hors plateforme, carte ; principale : espèces, chèque, virement ; petite caisse et coffre : espèces) |
| Plafonds | détention (`maxCashAmount`, existant), comportement au plafond (`ALERT`, `BLOCK`), sortie par opération, sorties par jour, fonds standard | Hérités du type (50 000 XAF par opération pour un guichet et une petite caisse, décision 7b) |
| Comptabilité | compte de trésorerie, journal par défaut | Vides jusqu'au lot F3 ; un changement est daté (`effectiveAt`) |
| Personnes | destinataires des alertes (remplace `managerEmail`, repris), titulaire en vigueur, affectations | |
| Fonctionnement | horaires par jour, postes autorisés | |
| Suivi | dernier comptage (date et montant), auteur et date de dernière modification, date de fermeture | Le dernier comptage est mis à jour à chaque clôture comptée ; la migration le reprend des sessions closes |

Les caisses existantes deviennent des guichets (`COUNTER`), sauf les caisses automatiques ; leur statut vient de `active` (inactive : `SUSPENDED`). Le champ `active` reste et reflète le statut.

**Historique** : chaque modification de la fiche (champ, ancienne et nouvelle valeur, auteur, date, date d'effet, motif), chaque changement de statut, d'affectations, de titulaire et de poste est inscrit dans `cash_register_history`.

**Cycle de vie** : `ACTIVE` vers `SUSPENDED` (motif de la liste `SUSPENSION_REASON`) puis `ACTIVE` ou `CLOSED`. Suspendre ou fermer exige qu'il n'y ait ni session en cours ou en revue, ni mouvement en attente de visa. Fermer exige en plus que le **dernier comptage vaille zéro**. La fermeture est définitive, révoque les affectations, et la caisse reste consultable.

## Affectations

| Qualité | Sens |
| --- | --- |
| `HOLDER` | Titulaire : un seul à la fois (deux pour un coffre) |
| `BACKUP` | Remplaçant permanent, classé (`backupRank`) |
| `INTERIM` | Intérimaire : période obligatoire, active et éteinte seule aux dates prévues |

Rôles : `CASHIER`, `HEAD_CASHIER`, `SUPERVISOR`, `VIEWER` (le rôle lecture n'ouvre pas de session). Une affectation n'est jamais supprimée : elle est révoquée ou se termine, ce qui garde la succession des titulaires. **Affectation d'urgence** : un responsable affecte quelqu'un comme intérimaire pour 1 à 24 heures (12 par défaut), avec un motif ; le responsable de la caisse est prévenu.

## Transfert de responsabilité (PV de remise `PRC-`)

1. Le responsable choisit le nouveau titulaire, la date d'effet, le motif (liste `RESPONSIBILITY_TRANSFER_REASON`), définitif ou temporaire (date de fin), le montant compté.
2. Le système liste les **éléments en cours** : session ouverte (à clore avant, bloquant), écart en revue, demandes et mouvements en attente (transférés au nouveau titulaire). La liste est figée dans le PV.
3. Définitif : l'affectation du titulaire sortant s'arrête à la date d'effet (il peut rester remplaçant), le nouveau devient titulaire. Temporaire : le nouveau est intérimaire jusqu'à la date de fin.
4. Le PV est signé par le sortant et l'entrant (`PATCH .../responsibility-transfers/{n}/sign`, chacun pour soi). Sortant absent : un **témoin** est exigé, différent du superviseur et de l'entrant ; le superviseur signe pour le sortant.
5. Les deux sont prévenus par courriel ; l'historique de la caisse garde le changement de titulaire.

Le comptage par coupures et l'écart de remise arrivent avec le lot C2 ; ici, le montant compté et l'écart avec le dernier comptage sont enregistrés.

## Listes préenregistrées

Table `cash_reference_value` : motifs d'écart, d'ajustement, de contre-passation, de suspension, d'annulation de session, d'absence, de transfert de responsabilité, de session hors horaires ; types de pièce externe ; banques du Congo. Chaque liste a une valeur `OTHER` (« Autre, préciser ») qui exige un texte et se désactive liste par liste. Une valeur retirée est désactivée, jamais supprimée. Chaque emploi est compté (`usageCount`) pour faire remonter les plus utilisées et repérer les « Autre » fréquents.

## Routes

| Méthode et chemin | Rôle | Permission |
| --- | --- | --- |
| `POST /cash-registers` | Créer (type, et facultativement titulaire, remplaçants, horaires ; le reste hérité ou déduit) | `CASH_REGISTER:MANAGE` |
| `GET /cash-registers/{code}` | Fiche complète | `CASH:READ` |
| `PATCH /cash-registers/{code}` | Modifier (historisé) | `CASH_REGISTER:MANAGE` |
| `PATCH /cash-registers/{code}/suspend`, `/reactivate`, `/close` | Cycle de vie ; `activate` et `deactivate` restent | `CASH_REGISTER:MANAGE` |
| `GET /cash-registers/{code}/history` | Historique | `CASH:READ` |
| `GET`, `PUT /cash-registers/{code}/assignments` | Affectations (liste complète) | lecture / `CASH_STAFF:MANAGE` |
| `POST /cash-registers/{code}/emergency-assignments` | Affectation d'urgence | `CASH_STAFF:MANAGE` |
| `PATCH /cash-registers/{code}/devices/{device}` | Autoriser ou refuser un poste | `CASH_REGISTER:MANAGE` |
| `GET /cash-registers/{code}/responsibility-transfers/pending-items` | Éléments en cours | `CASH:READ` |
| `GET`, `POST /cash-registers/{code}/responsibility-transfers` | Remises de caisse | lecture / `CASH_STAFF:MANAGE` |
| `PATCH /cash-registers/responsibility-transfers/{n}/sign` | Signer le PV | `CASH_SESSION:OPEN` |
| `GET /cash-registers/reference-lists[/{list}]`, `PUT .../{list}` | Listes | lecture / `CASH_REGISTER:MANAGE` |
| `GET /cash-registers/form-defaults?context=REGISTER_CREATE&registerType=&locationCode=` | Valeurs proposées à la création | `CASH:READ` |
| `GET /cash-registers/form-defaults?context=SESSION_OPEN` | Caisse proposée à l'ouverture, fonds attendu, horaires | `CASH:READ` |
| `GET /cash-registers/mine` | Caisses où l'utilisateur est affecté | `CASH:READ` |

`POST /cash-registers/sessions` accepte en plus `deviceCode`, `outOfHoursReasonCode`, `outOfHoursReasonText`.

Nouvelle permission `CASH_STAFF_MANAGE`, donnée à `SUPER_ADMIN`, `ADMIN`, `MANAGER`.

## Tests

- `CashRegisterAccessGuardTest` : non affecté, rôle lecture, affectation échue refusés ; titulaire accepté ; caisse suspendue et coffre refusés ; postes autorisés ; poste inconnu proposé ; motif hors horaires.
- `CashRegisterAdminServiceImplTest` : historique des modifications, pas d'historique sans changement, un seul titulaire, intérim daté, révocation au lieu de suppression, suspension refusée avec une session, fermeture à zéro, remise refusée avec une session ouverte, remise définitive, témoin exigé, signatures.
- `CashRegisterReferentialC1IT` (PostgreSQL, migrations de production, schéma validé) : type et statut, contrainte sur l'intérim, recherche par courriel, listes initiales.
- `AdminApiAuthorizationManagerTest` : permissions des nouvelles routes.

## Pour le frontend

À reporter dans la doc frontend finale :

- formulaire de création : demander le libellé et le type, puis afficher les valeurs de `form-defaults?context=REGISTER_CREATE` ;
- ouverture de session : utiliser `form-defaults?context=SESSION_OPEN` (caisse proposée, fonds attendu, motif hors horaires) et envoyer `deviceCode` ;
- écran des affectations (titulaire, remplaçants classés, intérims datés), bouton d'affectation d'urgence ;
- écran de remise de caisse : éléments en cours, puis PV à signer par chacun ;
- écran d'administration des listes ;
- messages 403 en français quand la personne n'est pas affectée.
