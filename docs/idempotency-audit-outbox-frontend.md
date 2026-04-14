# Idempotency, Audit et Outbox

Ce document explique comment le frontend doit utiliser les mecanismes `idempotency`, ce qu’il peut attendre des APIs d’`audit`, et comment exploiter la file `outbox` cote back-office.

## Objectif

- eviter les doubles creations lors des doubles clics ou retries reseau
- rendre les actions sensibles tracables
- donner une visibilite back-office sur les evenements techniques et metier

## Idempotency

### Principe

Pour les endpoints sensibles de creation et de mutation, le backend accepte un header optionnel:

`Idempotency-Key: <valeur-unique>`

Si ce header est envoye:
- la premiere requete execute l’action
- une requete identique rejouee avec la meme cle et le meme payload rejoue la meme reponse metier
- une requete avec la meme cle mais un payload different renvoie un conflit

Si ce header n’est pas envoye:
- la requete n’est pas bloquee
- le backend execute normalement l’action

Point important:
- l’absence du header ne doit jamais bloquer une creation

### Quand le frontend doit envoyer la cle

A utiliser sur:
- creation `customer`
- creation et mutation `member`
- creation et mutation `business`
- upload/remplacement/validation document
- creation et validation KYC
- generation contrat
- creation et mutation `resource`

### Format recommande

Le frontend doit generer une cle unique par intention utilisateur.

Exemples:
- `customer-create-1712662000123`
- `resource-create-6f4f0c1f-1f7a-4b7c-8d39-11c9d6b2a0aa`

Bonnes pratiques:
- generer une nouvelle cle pour une nouvelle tentative utilisateur
- conserver la meme cle pendant un retry automatique du meme envoi
- ne pas reutiliser une cle sur deux formulaires differents

### Exemple fetch

```ts
const idempotencyKey = crypto.randomUUID();

await fetch("/sni/api/v1/resources", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    "Idempotency-Key": idempotencyKey
  },
  body: JSON.stringify(payload)
});
```

### Gestion UI

- desactiver le bouton pendant l’envoi
- conserver la cle jusqu’au succes ou a l’echec final
- en cas de retry reseau automatique, reutiliser la meme cle
- en cas de correction manuelle du formulaire, regenerer une nouvelle cle

## Audit

### Ce qui est audite

Les endpoints sensibles ajoutes dans les modules principaux enregistrent:
- module
- action
- ressource
- utilisateur
- email acteur
- statut succes/echec
- IP
- user agent
- sessionId si disponible
- metadonnees HTTP minimales

### API admin

`GET /sni/api/v1/admin/audit-logs`

Filtres disponibles:
- `module`
- `action`
- `actorEmail`
- `status`
- `createdFrom`
- `createdTo`
- pagination Spring `page`, `size`, `sort`

Exemple:

```http
GET /sni/api/v1/admin/audit-logs?module=RESOURCE&status=SUCCESS&createdFrom=2026-04-09T00:00:00Z
```

### Usage frontend back-office

Ecran recommande:
- tableau avec filtres
- colonne date
- colonne module
- colonne action
- colonne acteur
- colonne statut
- colonne message erreur

Interactions utiles:
- filtre rapide par module
- filtre date debut/fin
- recherche par email acteur
- consultation detail ligne pour `metadata` et `diff`

## Outbox

### Principe

Le backend publie des evenements techniques/metier dans `outbox_event`.
Un worker les consomme ensuite de maniere fiable.

Evenements deja couverts:
- `DOCUMENT_*`
- `KYC_CASE_*`
- `CONTRACT_DRAFT_GENERATED`

### Ce que fait le worker

- recupere les evenements `PENDING` et `FAILED`
- les passe aux processors metier par aggregate
- marque `PUBLISHED` si le traitement passe
- marque `FAILED` avec retry differe si le traitement echoue

### API admin

`GET /sni/api/v1/admin/outbox-events`

Filtres:
- `eventType`
- `aggregateType`
- `aggregateId`
- `status`
- `createdFrom`
- `createdTo`

Actions:
- `POST /sni/api/v1/admin/outbox-events/process?batchSize=25`
- `POST /sni/api/v1/admin/outbox-events/{id}/requeue`

### Usage frontend back-office

Ecran recommande:
- tableau des evenements
- badge de statut `PENDING / PROCESSING / PUBLISHED / FAILED`
- colonne `eventType`
- colonne `aggregateType`
- colonne `aggregateId`
- colonne `attemptCount`
- colonne `lastError`

Actions UI:
- bouton `Reprocess`
- bouton `Requeue`
- filtre `FAILED`
- detail JSON du payload

## Idempotency Records

### API admin

`GET /sni/api/v1/admin/idempotency-records`

Filtres:
- `operation`
- `idempotencyKey`
- `status`
- `createdFrom`
- `createdTo`

### Usage frontend back-office

Cet ecran sert a comprendre:
- pourquoi une creation a ete rejouee
- pourquoi une requete est restee en `PROCESSING`
- pourquoi une cle a fini en `FAILED`

Colonnes conseillees:
- `operation`
- `idempotencyKey`
- `status`
- `createdAt`
- `completedAt`
- `errorBody`

## Recommandations UX

- les ecrans `audit`, `outbox` et `idempotency` doivent vivre dans une section admin technique
- afficher les dates en local utilisateur mais conserver l’UTC dans les filtres/requetes
- toujours permettre l’ouverture d’un panneau detail JSON
- distinguer visuellement `FAILED` et `PROCESSING`

## Cas d’erreur a gerer cote frontend

- `409 Conflict` sur reuse d’une meme cle avec payload different
- `401/403` sur les routes admin
- `500` si un event outbox est mal forme
- pagination vide sur filtres trop restrictifs

## Resume pratique

- envoyer `Idempotency-Key` sur les actions sensibles, mais ce n’est jamais obligatoire
- utiliser `audit-logs` pour la tracabilite admin
- utiliser `outbox-events` pour surveiller les traitements asynchrones
- utiliser `idempotency-records` pour diagnostiquer les doubles soumissions
