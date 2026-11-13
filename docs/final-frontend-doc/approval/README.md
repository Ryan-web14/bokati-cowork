# approval : centres d'approbation (lot B15)

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Un moteur commun d'approbation sert plusieurs **centres** : la facturation (`BILLING` : modifications de facture, dérogations de prix et de remise) et la caisse (`CASH` : demandes de fonds). Chaque centre a ses écrans et sa file ; tous partagent la même hiérarchie, les mêmes circuits, la même validation en avance et le même historique. Un nouveau centre se branche sans nouvelle table.

Toutes les routes sont du back-office, sous `/sni/api/v1/approvals`. La caisse garde aussi ses propres routes (`/fund-requests/...`, voir [fund](../fund/README.md)), branchées sur ce moteur.

## Notions

| Objet | Identifiant | Ce que c'est |
| --- | --- | --- |
| Demande d'approbation (`RequestView`) | `number` (`APR-AAAAMM-NNNNNN`) | Ce qu'un centre fait approuver : objet (`subjectType`, `subjectRef`), intitulé, montant, demandeur, niveaux |
| Niveau (`StepView`) | `level` (1, 2...) | Qui doit décider : décideur attendu (N+1, N+2 ou personne nommée) et/ou droit requis |
| Événement (`EventView`) | aucun | Frise : soumission, approbations, approbation en avance, refus, renvoi, rappel, escalade |
| Circuit (`CircuitView`) | `code` | Niveaux d'un type de demande d'un centre, chacun requis à partir d'un montant |
| Délégation (`DelegationView`) | `id` | Délégation datée d'un décideur à un autre (absence, intérim) |
| Hiérarchie (`HierarchyNode`) | `userCode` | Chaque collaborateur et son supérieur direct (N+1) |

## Statuts

Demande : `PENDING` (en attente), `APPROVED`, `REJECTED`, `RETURNED` (renvoyée pour correction), `CANCELLED` (retirée ou remplacée par une nouvelle soumission).

Niveau : `WAITING` (pas encore atteint), `PENDING` (niveau courant), `APPROVED`, `APPROVED_BY_HIGHER` (approuvé en avance par un niveau supérieur), `REJECTED`, `RETURNED`, `CANCELLED`.

## Qui peut décider d'un niveau

1. Le **décideur attendu** du niveau (le N+1 ou le N+2 du demandeur, ou la personne nommée par le circuit) ;
2. tout détenteur du **droit** du niveau (par exemple `BILLING:APPROVE`, `FUND_REQUEST:APPROVE_L2`) ;
3. tout **supérieur hiérarchique** du décideur attendu ;
4. un **délégué** du jour (délégation de la personne, ou du niveau pour les délégations par niveau de la caisse).

**On ne décide jamais de sa propre demande** (`403`).

**Validation en avance** (décision D11) : celui qui peut décider d'un niveau supérieur approuve d'un coup tous les niveaux jusqu'au sien. Les niveaux sautés passent à `APPROVED_BY_HIGHER` avec le commentaire « Approuvé en avance par Nom (libellé du niveau) ». Exemple : demande à deux niveaux (N+1 puis N+2) approuvée d'abord par le N+2 : elle passe à `APPROVED`, le niveau 1 est noté approuvé en avance, et la demande figure dans l'historique des deux approbateurs.

## Parcours

### Files d'un centre

- `GET /approvals/{center}/to-approve` : les demandes qui attendent l'utilisateur connecté (à leur niveau courant ou en avance). `canDecide = true` sur chacune.
- `GET /approvals/{center}/mine` : mes demandes, où chacune en est (`currentLabel`), **qui doit agir** (`waitingFor`) et **depuis quand** (`levelSince`).
- `GET /approvals/{center}/history?type=&requester=&approver=&status=&from=&to=&page=&size=` : historique filtrable. Sans le droit `APPROVAL:MANAGE`, seulement les demandes où l'on est demandeur, décideur attendu ou auteur d'une décision.
- `GET /approvals/requests/{number}` : une demande, ses niveaux et sa frise complète (`timeline`), avec le montant avant et après une réduction.

`{center}` : `BILLING` ou `CASH`.

### Décider

- `PATCH /approvals/{number}/approve` avec `{ "comment": "...", "reducedAmount": null }` (corps facultatif). `reducedAmount` : montant réduit (positif, inférieur au montant demandé, motif obligatoire).
- `PATCH /approvals/{number}/reject` avec `{ "comment": "motif" }` (obligatoire).
- `PATCH /approvals/{number}/send-back` avec `{ "comment": "ce qu'il faut corriger" }` (obligatoire).

Réponse : la demande à jour. Le centre de l'objet applique la décision (modification de facture appliquée, demande de fonds approuvée ou rejetée).

### Hiérarchie

- `GET /approvals/hierarchy` : organigramme (collaborateurs du back-office, fonction, supérieur).
- `PUT /approvals/hierarchy` avec `{ "userCode": "USR-...", "managerUserCode": "USR-..." }` (vide : retire le supérieur). Droit `APPROVAL:MANAGE`. Refus si le choix crée une boucle. Même donnée que la fiche collaborateur ([securite](../securite/fiche-collaborateur.md)).

### Circuits

- `GET /approvals/circuits?center=BILLING`.
- `POST /approvals/circuits` (droit `APPROVAL:MANAGE`) : crée ou remplace un circuit et ses niveaux.

```json
{
  "code": "BILLING_INVOICE_CHANGE", "center": "BILLING", "requestType": "INVOICE_CHANGE", "label": "Modification d'une facture", "active": true,
  "levels": [
    { "label": "Responsable (N+1)", "rule": "MANAGER", "depth": 1, "permission": "BILLING:APPROVE" },
    { "label": "Direction (N+2)", "rule": "MANAGER", "depth": 2, "permission": "BILLING:APPROVE_L2", "minAmount": 500000 }
  ]
}
```

`rule` : `MANAGER` (`depth` 1 pour le N+1, 2 pour le N+2), `PERMISSION` (`permission` obligatoire), `USER` (`userLogin` obligatoire). `minAmount` : le niveau n'est requis qu'à partir de ce montant. Circuits livrés : `BILLING_INVOICE_CHANGE` (N+1, puis N+2 au-delà de 500 000) et `BILLING_DISCOUNT_OVERRIDE` (N+1). La caisse calcule ses niveaux à partir de ses paramètres (`fund.approval.level1-max`, `fund.approval.level2-max`).

### Délégations

- `POST /approvals/delegations` avec `{ "delegate": "adjoint@elleaose.com", "center": null, "level": null, "startsOn": "2026-12-20", "endsOn": "2027-01-05", "reason": "Congé" }`. Sans centre ni niveau : le délégué agit partout où le délégant est attendu.
- `GET /approvals/delegations?center=`, `DELETE /approvals/delegations/{id}` (le délégant ou `APPROVAL:MANAGE`).

## Effets automatiques

- Notifications dans l'application et par courriel à chaque étape : aux décideurs du niveau courant (décideur attendu, détenteurs du droit, délégués), au demandeur à la décision.
- `ApprovalWorker` (planification `approval.worker-cron`, de 8 h à 18 h) : rappel aux décideurs après `approval.reminder-hours` heures (24 par défaut), puis à chaque intervalle ; escalade après `approval.escalation-hours` heures (72) au supérieur du décideur attendu (à défaut au responsable de permanence). Les deux sont inscrits dans la frise (`REMINDED`, `ESCALATED`).

## Droits

| Droit | Pour |
| --- | --- |
| `APPROVAL:MANAGE` | Modifier la hiérarchie et les circuits, voir tout l'historique |
| `BILLING:APPROVE` | Décider d'un niveau de facturation (responsable) |
| `BILLING:APPROVE_L2` | Décider d'un niveau de facturation (direction) |
| `FUND_REQUEST:APPROVE_L1` à `L3`, `BUDGET:OVERRIDE`, `BUDGET:APPROVE_UNPLANNED` | Niveaux de la caisse (voir [fund](../fund/README.md)) |

Le reste des routes `/approvals` est ouvert au back-office : le moteur contrôle qui peut décider de quoi.

## À prévoir côté frontend

- Deux écrans distincts, « Centre d'approbation de la facturation » et « Centre d'approbation de la caisse », sur les mêmes routes avec `center` différent : onglets À approuver, Mes demandes, Historique.
- Sur chaque demande : la frise (qui, quand, décision, commentaire, montant avant et après), le bouton Approuver (avec réduction possible pour la caisse), Refuser, Renvoyer ; masquer les boutons si `canDecide = false`.
- Un écran Organigramme (hiérarchie), un écran Circuits et un écran Délégations.
