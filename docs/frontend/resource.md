# Module Ressource — API Frontend

> Base URL: `/sni/api/v1`
> Auth: Bearer JWT requis sur tous les endpoints sauf ceux marqués **PUBLIC**.
> Content-Type: `application/json`

---

## Table des matières

### PARTIE 1 — Ressources

1. [Patterns communs](#1-patterns-communs)
2. [Enums de référence](#2-enums-de-référence)
3. [Types de ressource](#3-types-de-ressource)
4. [Groupes de ressource](#4-groupes-de-ressource)
5. [Politiques de réservation](#5-politiques-de-réservation)
6. [Ressources — CRUD](#6-ressources--crud)
7. [Recherche de ressources](#7-recherche-de-ressources)
8. [Classification et paramétrage](#8-classification-et-paramétrage)
9. [Équipements (Amenities)](#9-équipements-amenities)

### PARTIE 2 — Disponibilité & Planning

10. [Disponibilités](#10-disponibilités)
11. [Fermetures exceptionnelles](#11-fermetures-exceptionnelles)
12. [Calendrier public](#12-calendrier-public)
13. [Calendrier admin (avec occupation)](#13-calendrier-admin-avec-occupation)
14. [Créneaux occupés par ressource](#14-créneaux-occupés-par-ressource)

### PARTIE 3 — Tarification

15. [Règles de tarification](#15-règles-de-tarification)
16. [Devis de prix](#16-devis-de-prix)

### PARTIE 4 — Galerie photos

17. [Photos de ressource](#17-photos-de-ressource)

### PARTIE 5 — Intégration

18. [Workflows recommandés](#18-workflows-recommandés)
19. [Écrans recommandés](#19-écrans-recommandés)
20. [Codes d'erreur](#20-codes-derreur)

---

## 1. Patterns communs

### Réponse paginée

```json
{
  "content": [ ... ],
  "totalElements": 42,
  "totalPages": 3,
  "page": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

| Param  | Type   | Défaut   | Description                        |
|--------|--------|----------|------------------------------------|
| `page` | int    | `0`      | Numéro de page (0-indexed)         |
| `size` | int    | `20`     | Éléments par page (50 pour dispo)  |
| `sort` | string | variable | Champ de tri, ex: `createdAt,desc` |

### Résolution automatique

Le backend résout les codes (`typeCode`, `groupCode`, `policyCode`, `resourceCode`, `amenityCode`) vers les entités correspondantes. Le frontend utilise uniquement des codes, jamais des IDs numériques.

---

## 2. Enums de référence

### ResourceStatus — Statut de la ressource

| Valeur           | Description                                |
|------------------|--------------------------------------------|
| `ACTIVE`         | Ressource disponible et opérationnelle     |
| `INACTIVE`       | Désactivée temporairement                  |
| `MAINTENANCE`    | En maintenance                             |
| `OUT_OF_SERVICE` | Hors service                               |
| `ARCHIVED`       | Archivée (n'apparaît plus dans les listes) |

### ResourceBookingUnit — Unité de réservation

| Valeur     | Description       |
|------------|-------------------|
| `HOUR`     | À l'heure         |
| `HALF_DAY` | À la demi-journée |
| `DAY`      | À la journée      |
| `WEEK`     | À la semaine       |
| `MONTH`    | Au mois            |

### ResourcePriceAdjustmentType — Type d'ajustement de prix

| Valeur          | Description                                        |
|-----------------|----------------------------------------------------|
| `FIXED_PRICE`   | Prix fixe (remplace le prix de base)               |
| `AMOUNT_DELTA`  | Ajustement en montant (ex: +5000 ou -2000 FCFA)   |
| `PERCENT_DELTA` | Ajustement en pourcentage (ex: -10%, +20%)         |

### OccupancyLevel — Niveau d'occupation

| Valeur               | Seuil               | Description                         | Couleur suggérée |
|----------------------|----------------------|-------------------------------------|------------------|
| `FREE`               | 0%                   | Totalement libre                    | Vert             |
| `PARTIALLY_OCCUPIED` | 1% – 49%            | Partiellement occupé                | Jaune            |
| `HIGHLY_OCCUPIED`    | 50% – 84%           | Très occupé                         | Orange           |
| `FULL`               | 85% – 100%          | Complet ou quasi complet            | Rouge            |

---

# PARTIE 1 — Ressources

---

## 3. Types de ressource

### 3.1 Créer un type de ressource

```
POST /api/v1/resource-types
```

**Body**

```json
{
  "name": "Bureau privé",
  "description": "Bureau fermé individuel ou partagé",
  "active": true
}
```

| Champ         | Type    | Requis | Description                  |
|---------------|---------|--------|------------------------------|
| `name`        | string  | oui    | Nom du type                  |
| `description` | string  | non    | Description                  |
| `active`      | boolean | non    | Actif (défaut: true)         |

**Réponse** `201 Created`

### 3.2 Récupérer un type

```
GET /api/v1/resource-types/{code}
```

**Réponse** `200 OK`

```json
{
  "code": "RTYP-00001",
  "name": "Bureau privé",
  "description": "Bureau fermé individuel ou partagé",
  "active": true
}
```

### 3.3 Lister les types (paginé)

```
GET /api/v1/resource-types
```

| Param  | Type | Requis | Description                |
|--------|------|--------|----------------------------|
| `page` | int  | non    | Page (défaut: 0)           |
| `size` | int  | non    | Taille page (défaut: 20)   |

**Réponse** `200 OK` → `PaginatedResponse<ResourceTypeResponse>`

### 3.4 Rechercher des types

```
GET /api/v1/resource-types/search?query=bureau
```

| Param   | Type   | Requis | Description          |
|---------|--------|--------|----------------------|
| `query` | string | oui    | Terme de recherche   |

**Réponse** `200 OK` → `PaginatedResponse<ResourceTypeResponse>`

### 3.5 Recherche basique (autocomplete)

```
GET /api/v1/resource-types/search/basic?query=bur
```

**Réponse** `200 OK` → `PaginatedResponse<ResourceTypeResponse>`

### 3.6 Modifier un type

```
PUT /api/v1/resource-types/{code}
```

**Body** — Champs optionnels

```json
{
  "name": "Bureau privé premium",
  "description": "Bureau fermé avec équipements premium",
  "active": true
}
```

**Réponse** `204 No Content`

### 3.7 Supprimer un type

```
DELETE /api/v1/resource-types/{code}
```

**Réponse** `204 No Content`

---

## 4. Groupes de ressource

### 4.1 Créer un groupe

```
POST /api/v1/resource-groups
```

**Body**

```json
{
  "name": "Étage 3 - Zone A",
  "description": "Bureaux du 3ème étage, zone A",
  "portalVisible": true,
  "active": true
}
```

| Champ           | Type    | Requis | Description                           |
|-----------------|---------|--------|---------------------------------------|
| `name`          | string  | oui    | Nom du groupe                         |
| `description`   | string  | non    | Description                           |
| `portalVisible` | boolean | non    | Visible sur le portail client         |
| `active`        | boolean | non    | Actif (défaut: true)                  |

**Réponse** `201 Created`

### 4.2 Récupérer un groupe

```
GET /api/v1/resource-groups/{code}
```

**Réponse** `200 OK`

```json
{
  "code": "RGRP-00001",
  "name": "Étage 3 - Zone A",
  "description": "Bureaux du 3ème étage, zone A",
  "portalVisible": true,
  "active": true
}
```

### 4.3 Lister les groupes

```
GET /api/v1/resource-groups
```

**Réponse** `200 OK` → `PaginatedResponse<ResourceGroupResponse>`

### 4.4 Rechercher des groupes

```
GET /api/v1/resource-groups/search?query=étage
```

**Réponse** `200 OK` → `PaginatedResponse<ResourceGroupResponse>`

### 4.5 Recherche basique

```
GET /api/v1/resource-groups/search/basic?query=éta
```

**Réponse** `200 OK` → `PaginatedResponse<ResourceGroupResponse>`

### 4.6 Modifier un groupe

```
PUT /api/v1/resource-groups/{code}
```

**Body** — Champs optionnels

```json
{
  "name": "Étage 3 - Zone B",
  "portalVisible": false
}
```

**Réponse** `204 No Content`

### 4.7 Supprimer un groupe

```
DELETE /api/v1/resource-groups/{code}
```

**Réponse** `204 No Content`

---

## 5. Politiques de réservation

### 5.1 Créer une politique

```
POST /api/v1/resource-policies
```

**Body**

```json
{
  "name": "Politique standard",
  "description": "Politique de réservation par défaut",
  "minBookingDurationMinutes": 30,
  "maxBookingDurationMinutes": 480,
  "minBookingNoticeMinutes": 60,
  "cancellationNoticeMinutes": 120,
  "allowCancellation": true,
  "active": true
}
```

| Champ                        | Type    | Requis | Description                                   |
|------------------------------|---------|--------|-----------------------------------------------|
| `name`                       | string  | oui    | Nom de la politique                           |
| `description`                | string  | non    | Description                                   |
| `minBookingDurationMinutes`  | number  | oui    | Durée minimale de réservation (en minutes)    |
| `maxBookingDurationMinutes`  | number  | oui    | Durée maximale de réservation (en minutes)    |
| `minBookingNoticeMinutes`    | number  | oui    | Préavis minimum avant réservation (minutes)   |
| `cancellationNoticeMinutes`  | number  | non    | Préavis d'annulation (minutes)                |
| `allowCancellation`          | boolean | non    | Annulation autorisée (défaut: false)          |
| `active`                     | boolean | non    | Active (défaut: true)                         |

**Réponse** `201 Created`

### 5.2 Récupérer une politique

```
GET /api/v1/resource-policies/{code}
```

**Réponse** `200 OK`

```json
{
  "code": "RPOL-00001",
  "name": "Politique standard",
  "description": "Politique de réservation par défaut",
  "minBookingDurationMinutes": 30,
  "maxBookingDurationMinutes": 480,
  "minBookingNoticeMinutes": 60,
  "cancellationNoticeMinutes": 120,
  "allowCancellation": true,
  "active": true
}
```

### 5.3 Lister les politiques (paginé)

```
GET /api/v1/resource-policies
```

**Réponse** `200 OK` → `PaginatedResponse<ResourcePolicyResponse>`

### 5.4 Rechercher des politiques

```
GET /api/v1/resource-policies/search?query=standard
```

**Réponse** `200 OK` → `PaginatedResponse<ResourcePolicyResponse>`

### 5.5 Recherche basique

```
GET /api/v1/resource-policies/search/basic?query=sta
```

**Réponse** `200 OK` → `PaginatedResponse<ResourcePolicyResponse>`

### 5.6 Modifier une politique

```
PUT /api/v1/resource-policies/{code}
```

**Body** — Champs optionnels

```json
{
  "maxBookingDurationMinutes": 960,
  "allowCancellation": false
}
```

**Réponse** `204 No Content`

### 5.7 Changer le statut actif/inactif

```
PATCH /api/v1/resource-policies/{code}/status?active=true
```

| Param    | Type    | Requis | In    | Description         |
|----------|---------|--------|-------|---------------------|
| `active` | boolean | oui    | query | Nouvel état actif   |

**Réponse** `204 No Content`

### 5.8 Changer la politique d'annulation

```
PATCH /api/v1/resource-policies/{code}/allow-cancellation?allowed=true
```

| Param     | Type    | Requis | In    | Description               |
|-----------|---------|--------|-------|---------------------------|
| `allowed` | boolean | oui    | query | Annulation autorisée      |

**Réponse** `204 No Content`

### 5.9 Supprimer une politique

```
DELETE /api/v1/resource-policies/{code}
```

**Réponse** `204 No Content`

---

## 6. Ressources — CRUD

### 6.1 Créer une ressource

```
POST /api/v1/resources
```

**Body**

```json
{
  "typeId": "RTYP-00001",
  "groupId": "RGRP-00001",
  "policyId": "RPOL-00001",
  "name": "Bureau B-301",
  "description": "Bureau privé 3ème étage, vue sur jardin",
  "capacity": 4,
  "zone": "Zone A",
  "locationLabel": "3ème étage, aile droite",
  "status": "ACTIVE",
  "bookingEnabled": true,
  "portalVisible": true
}
```

| Champ            | Type    | Requis | Description                              |
|------------------|---------|--------|------------------------------------------|
| `typeId`         | string  | oui    | Code du type de ressource                |
| `groupId`        | string  | non    | Code du groupe                           |
| `policyId`       | string  | non    | Code de la politique de réservation      |
| `name`           | string  | oui    | Nom de la ressource                      |
| `description`    | string  | non    | Description                              |
| `capacity`       | number  | non    | Capacité (nombre de places)              |
| `zone`           | string  | non    | Zone géographique                        |
| `locationLabel`  | string  | non    | Label de localisation                    |
| `status`         | string  | non    | Statut initial (défaut: ACTIVE)          |
| `bookingEnabled` | boolean | non    | Réservable (défaut: true)                |
| `portalVisible`  | boolean | non    | Visible portail client (défaut: true)    |

**Réponse** `201 Created`

### 6.2 Récupérer une ressource

```
GET /api/v1/resources/{code}
```

**Réponse** `200 OK`

```json
{
  "code": "RES-00001",
  "typeCode": "RTYP-00001",
  "groupCode": "RGRP-00001",
  "policyCode": "RPOL-00001",
  "name": "Bureau B-301",
  "description": "Bureau privé 3ème étage, vue sur jardin",
  "capacity": 4,
  "zone": "Zone A",
  "locationLabel": "3ème étage, aile droite",
  "status": "ACTIVE",
  "bookingEnabled": true,
  "portalVisible": true,
  "displayOrder": 1,
  "active": true
}
```

### 6.3 Lister les ressources (paginé, avec filtres)

```
GET /api/v1/resources
```

| Param        | Type    | Requis | Description                                        |
|--------------|---------|--------|----------------------------------------------------|
| `summary`    | boolean | non    | `true` pour retourner `ResourceSummaryResponse`    |
| `typeCode`   | string  | non    | Filtrer par type de ressource                      |
| `groupCode`  | string  | non    | Filtrer par groupe                                 |
| `policyCode` | string  | non    | Filtrer par politique                              |
| `page`       | int     | non    | Page (défaut: 0)                                   |
| `size`       | int     | non    | Taille page (défaut: 20)                           |

**Réponse `summary=false`** `200 OK` → `PaginatedResponse<ResourceResponse>`

**Réponse `summary=true`** `200 OK` → `PaginatedResponse<ResourceSummaryResponse>`

```json
{
  "code": "RES-00001",
  "typeCode": "RTYP-00001",
  "name": "Bureau B-301",
  "status": "ACTIVE",
  "bookingEnabled": true,
  "portalVisible": true,
  "active": true
}
```

### 6.4 Modifier une ressource

```
PUT /api/v1/resources/{code}
```

**Body** — Champs optionnels

```json
{
  "name": "Bureau B-301 Premium",
  "description": "Bureau rénové avec vue sur jardin",
  "capacity": 6,
  "zone": "Zone A",
  "floorLabel": "3ème étage",
  "locationLabel": "Aile droite, porte 301",
  "bookingEnabled": true,
  "portalVisible": true
}
```

| Champ            | Type    | Requis | Description                     |
|------------------|---------|--------|---------------------------------|
| `name`           | string  | non    | Nouveau nom                     |
| `description`    | string  | non    | Nouvelle description            |
| `capacity`       | number  | non    | Nouvelle capacité               |
| `zone`           | string  | non    | Nouvelle zone                   |
| `floorLabel`     | string  | non    | Label d'étage                   |
| `locationLabel`  | string  | non    | Nouveau label localisation      |
| `bookingEnabled` | boolean | non    | Réservable                      |
| `portalVisible`  | boolean | non    | Visible portail                 |

**Réponse** `204 No Content`

### 6.5 Supprimer une ressource

```
DELETE /api/v1/resources/{code}
```

**Réponse** `204 No Content`

> Suppression logique (soft delete).

---

## 7. Recherche de ressources

### 7.1 Recherche basique (autocomplete)

```
GET /api/v1/resources/search/basic?query=bureau
```

| Param   | Type   | Requis | Description         |
|---------|--------|--------|---------------------|
| `query` | string | oui    | Terme de recherche  |

**Réponse** `200 OK` → `List<ResourceSummaryResponse>`

### 7.2 Recherche avancée

```
POST /api/v1/resources/search
```

**Body** (optionnel) — `ResourceSearchCriteria`

```json
{
  "query": "bureau",
  "typeCode": "RTYP-00001",
  "groupCode": "RGRP-00001",
  "status": "ACTIVE",
  "bookingEnabled": true,
  "portalVisible": true,
  "zone": "Zone A",
  "minCapacity": 2,
  "maxCapacity": 10
}
```

| Param  | Type | Requis | In    | Description              |
|--------|------|--------|-------|--------------------------|
| `page` | int  | non    | query | Page (défaut: 0)         |
| `size` | int  | non    | query | Taille page (défaut: 20) |

**Réponse** `200 OK` → `PaginatedResponse<ResourceSummaryResponse>`

---

## 8. Classification et paramétrage

### 8.1 Changer la classification (type/groupe/politique)

```
PATCH /api/v1/resources/{code}/classification
```

**Body**

```json
{
  "typeId": "RTYP-00002",
  "groupId": "RGRP-00003",
  "policyId": "RPOL-00002"
}
```

| Champ      | Type   | Requis | Description                      |
|------------|--------|--------|----------------------------------|
| `typeId`   | string | oui    | Nouveau code de type             |
| `groupId`  | string | non    | Nouveau code de groupe           |
| `policyId` | string | non    | Nouveau code de politique        |

**Réponse** `204 No Content`

### 8.2 Changer le statut

```
PATCH /api/v1/resources/{code}/status?status=MAINTENANCE
```

| Param    | Type           | Requis | In    | Description       |
|----------|----------------|--------|-------|-------------------|
| `status` | ResourceStatus | oui    | query | Nouveau statut    |

**Réponse** `204 No Content`

### 8.3 Activer/désactiver la réservation

```
PATCH /api/v1/resources/{code}/booking-enabled
```

**Body**

```json
{
  "bookingEnabled": false
}
```

**Réponse** `204 No Content`

### 8.4 Activer/désactiver la visibilité portail

```
PATCH /api/v1/resources/{code}/portal-visible
```

**Body**

```json
{
  "visible": false
}
```

**Réponse** `204 No Content`

### 8.5 Activer/désactiver la ressource

```
PATCH /api/v1/resources/{code}/active
```

**Body**

```json
{
  "active": false
}
```

**Réponse** `204 No Content`

### 8.6 Modifier l'ordre d'affichage

```
PATCH /api/v1/resources/{code}/display-order
```

**Body**

```json
{
  "displayOrder": 5
}
```

**Réponse** `204 No Content`

---

## 9. Équipements (Amenities)

### 9.1 Créer un équipement

```
POST /api/v1/resource-amenities
```

**Body**

```json
{
  "name": "Vidéoprojecteur",
  "description": "Projecteur HD avec câble HDMI",
  "active": true
}
```

| Champ         | Type    | Requis | Description              |
|---------------|---------|--------|--------------------------|
| `name`        | string  | oui    | Nom de l'équipement      |
| `description` | string  | non    | Description              |
| `active`      | boolean | non    | Actif (défaut: true)     |

**Réponse** `201 Created`

### 9.2 Récupérer un équipement

```
GET /api/v1/resource-amenities/{code}
```

**Réponse** `200 OK`

```json
{
  "code": "AMEN-00001",
  "name": "Vidéoprojecteur",
  "description": "Projecteur HD avec câble HDMI",
  "active": true,
  "quantity": null,
  "optional": null,
  "extraPrice": null
}
```

### 9.3 Lister les équipements (paginé, trié par nom ASC)

```
GET /api/v1/resource-amenities
```

**Réponse** `200 OK` → `PaginatedResponse<AmenityResponse>`

### 9.4 Rechercher des équipements

```
GET /api/v1/resource-amenities/search?query=vidéo
```

**Réponse** `200 OK` → `PaginatedResponse<AmenityResponse>`

### 9.5 Recherche basique

```
GET /api/v1/resource-amenities/search/basic?query=vid
```

**Réponse** `200 OK` → `PaginatedResponse<AmenityResponse>`

### 9.6 Modifier un équipement

```
PUT /api/v1/resource-amenities/{code}
```

**Body** — Champs optionnels

```json
{
  "name": "Vidéoprojecteur 4K",
  "description": "Projecteur 4K avec câble HDMI et USB-C"
}
```

**Réponse** `204 No Content`

### 9.7 Supprimer un équipement

```
DELETE /api/v1/resource-amenities/{code}
```

**Réponse** `204 No Content`

### 9.8 Lier un équipement à une ressource

```
POST /api/v1/resources/amenities/link
```

**Body**

```json
{
  "resourceCode": "RES-00001",
  "amenityCode": "AMEN-00001",
  "quantity": 1,
  "optional": false,
  "extraPrice": 5000
}
```

| Champ          | Type    | Requis | Description                          |
|----------------|---------|--------|--------------------------------------|
| `resourceCode` | string  | oui    | Code de la ressource                 |
| `amenityCode`  | string  | oui    | Code de l'équipement                 |
| `quantity`     | number  | non    | Quantité disponible                  |
| `optional`     | boolean | non    | Optionnel (défaut: false)            |
| `extraPrice`   | number  | non    | Supplément de prix (en centimes)     |

**Réponse** `201 Created`

### 9.9 Délier un équipement d'une ressource

```
DELETE /api/v1/resources/{code}/amenities/{amenityCode}
```

**Réponse** `204 No Content`

### 9.10 Lister les équipements d'une ressource

```
GET /api/v1/resources/{code}/amenities
```

**Réponse** `200 OK`

```json
[
  {
    "code": "AMEN-00001",
    "name": "Vidéoprojecteur",
    "description": "Projecteur HD avec câble HDMI",
    "active": true,
    "quantity": 1,
    "optional": false,
    "extraPrice": 5000
  }
]
```

---

# PARTIE 2 — Disponibilité & Planning

---

## 10. Disponibilités

### 10.1 Créer un créneau de disponibilité

```
POST /api/v1/resource-availabilities
```

**Body**

```json
{
  "resourceCode": "RES-00001",
  "startedAt": "2026-07-01T08:00:00",
  "endedAt": "2026-07-01T18:00:00",
  "capacity": 4,
  "active": true
}
```

| Champ          | Type          | Requis | Description                         |
|----------------|---------------|--------|-------------------------------------|
| `resourceCode` | string        | oui    | Code de la ressource                |
| `startedAt`    | LocalDateTime | oui    | Début du créneau                    |
| `endedAt`      | LocalDateTime | oui    | Fin du créneau                      |
| `capacity`     | number        | non    | Capacité pour ce créneau            |
| `active`       | boolean       | non    | Actif (défaut: true)                |

**Réponse** `201 Created`

### 10.2 Lister les disponibilités (paginé)

```
GET /api/v1/resource-availabilities
```

| Param          | Type   | Requis | Description                       |
|----------------|--------|--------|-----------------------------------|
| `resourceCode` | string | non    | Filtrer par ressource             |
| `page`         | int    | non    | Page (défaut: 0)                  |
| `size`         | int    | non    | Taille page (défaut: 50)          |

**Réponse** `200 OK` → `PaginatedResponse<ResourceAvailabilityResponse>`

```json
{
  "id": 1,
  "resourceCode": "RES-00001",
  "startedAt": "2026-07-01T08:00:00",
  "endedAt": "2026-07-01T18:00:00",
  "slotDurationMinutes": 30,
  "totalCapacity": 4,
  "remainingCapacity": 3,
  "available": true,
  "active": true
}
```

### 10.3 Lister groupé par ressource

```
GET /api/v1/resource-availabilities/grouped
```

**Réponse** `200 OK`

```json
[
  {
    "resourceCode": "RES-00001",
    "resourceName": "Bureau B-301",
    "availabilities": [
      {
        "id": 1,
        "resourceCode": "RES-00001",
        "startedAt": "2026-07-01T08:00:00",
        "endedAt": "2026-07-01T18:00:00",
        "slotDurationMinutes": 30,
        "totalCapacity": 4,
        "remainingCapacity": 3,
        "available": true,
        "active": true
      }
    ]
  }
]
```

### 10.4 Rechercher les créneaux restants

```
GET /api/v1/resource-availabilities/remaining
```

| Param             | Type          | Requis | Description                            |
|-------------------|---------------|--------|----------------------------------------|
| `resourceCode`    | string        | oui    | Code de la ressource                   |
| `startedAt`       | LocalDateTime | oui    | Début de la fenêtre de recherche       |
| `endedAt`         | LocalDateTime | oui    | Fin de la fenêtre de recherche         |
| `durationMinutes` | number        | oui    | Durée souhaitée (en minutes)           |
| `quantity`        | number        | oui    | Nombre de places nécessaires           |

**Réponse** `200 OK`

```json
[
  {
    "resourceCode": "RES-00001",
    "startedAt": "2026-07-01T10:00:00",
    "endedAt": "2026-07-01T12:00:00",
    "durationMinutes": 120,
    "remainingCapacity": 3,
    "slotCount": 4
  }
]
```

### 10.5 Réserver un créneau

```
POST /api/v1/resource-availabilities/reserve
```

**Body**

```json
{
  "resourceCode": "RES-00001",
  "startedAt": "2026-07-01T10:00:00",
  "endedAt": "2026-07-01T12:00:00",
  "quantity": 1
}
```

| Champ          | Type          | Requis | Description                       |
|----------------|---------------|--------|-----------------------------------|
| `resourceCode` | string        | oui    | Code de la ressource              |
| `startedAt`    | LocalDateTime | oui    | Début de la réservation           |
| `endedAt`      | LocalDateTime | oui    | Fin de la réservation             |
| `quantity`     | number        | oui    | Nombre de places à réserver       |

**Réponse** `204 No Content`

> Décrémente `remainingCapacity` sur les créneaux concernés. Utilise le verrouillage optimiste pour la concurrence.

### 10.6 Libérer un créneau réservé

```
PATCH /api/v1/resource-availabilities/release
```

**Body**

```json
{
  "resourceCode": "RES-00001",
  "startedAt": "2026-07-01T10:00:00",
  "endedAt": "2026-07-01T12:00:00",
  "quantity": 1
}
```

| Champ          | Type          | Requis | Description                       |
|----------------|---------------|--------|-----------------------------------|
| `resourceCode` | string        | oui    | Code de la ressource              |
| `startedAt`    | LocalDateTime | oui    | Début du créneau à libérer        |
| `endedAt`      | LocalDateTime | oui    | Fin du créneau à libérer          |
| `quantity`     | number        | oui    | Nombre de places à libérer        |

**Réponse** `204 No Content`

> Ré-incrémente `remainingCapacity`.

---

## 11. Fermetures exceptionnelles

### 11.1 Créer une fermeture

```
POST /api/v1/resource-closures
```

**Body**

```json
{
  "resourceCode": "RES-00001",
  "startedAt": "2026-12-25T00:00:00",
  "endedAt": "2026-12-26T00:00:00",
  "reason": "Jour férié - Noël",
  "active": true
}
```

| Champ          | Type          | Requis | Description                       |
|----------------|---------------|--------|-----------------------------------|
| `resourceCode` | string        | oui    | Code de la ressource              |
| `startedAt`    | LocalDateTime | oui    | Début de la fermeture             |
| `endedAt`      | LocalDateTime | oui    | Fin de la fermeture               |
| `reason`       | string        | non    | Motif de la fermeture             |
| `active`       | boolean       | non    | Active (défaut: true)             |

**Réponse** `201 Created`

### 11.2 Récupérer une fermeture

```
GET /api/v1/resource-closures/{id}
```

**Réponse** `200 OK`

```json
{
  "id": 1,
  "resourceCode": "RES-00001",
  "startedAt": "2026-12-25T00:00:00",
  "endedAt": "2026-12-26T00:00:00",
  "active": true
}
```

### 11.3 Lister les fermetures (paginé)

```
GET /api/v1/resource-closures
```

| Param          | Type   | Requis | Description                      |
|----------------|--------|--------|----------------------------------|
| `resourceCode` | string | non    | Filtrer par ressource            |
| `page`         | int    | non    | Page (défaut: 0)                 |
| `size`         | int    | non    | Taille page (défaut: 20)         |

**Réponse** `200 OK` → `PaginatedResponse<ResourceClosureResponse>`

### 11.4 Activer/désactiver une fermeture

```
PATCH /api/v1/resource-closures/{id}/active
```

**Body**

```json
{
  "active": false
}
```

**Réponse** `204 No Content`

### 11.5 Supprimer une fermeture

```
DELETE /api/v1/resource-closures/{id}
```

**Réponse** `204 No Content`

---

## 12. Calendrier public

### 12.1 Consulter le calendrier d'une ressource

```
GET /api/v1/public/resources/{resourceCode}/calendar
```

> **PUBLIC** — Aucune authentification requise.

| Param          | Type      | Requis | In    | Description                          |
|----------------|-----------|--------|-------|--------------------------------------|
| `resourceCode` | string    | oui    | path  | Code de la ressource                 |
| `fromDate`     | LocalDate | non    | query | Date de début (défaut: aujourd'hui)  |
| `toDate`       | LocalDate | non    | query | Date de fin (défaut: +7 jours)       |

**Réponse** `200 OK`

```json
{
  "resourceCode": "RES-00001",
  "resourceName": "Bureau B-301",
  "fromDate": "2026-07-01",
  "toDate": "2026-07-07",
  "days": [
    {
      "date": "2026-07-01",
      "totalSlots": 20,
      "availableSlots": 15,
      "totalCapacity": 80,
      "remainingCapacity": 60,
      "status": "AVAILABLE",
      "windows": [
        {
          "startedAt": "2026-07-01T08:00:00",
          "endedAt": "2026-07-01T08:30:00",
          "slotDurationMinutes": 30,
          "totalCapacity": 4,
          "remainingCapacity": 3,
          "available": true,
          "active": true
        }
      ]
    }
  ]
}
```

**Structure imbriquée :**

| Niveau  | Champ               | Type            | Description                         |
|---------|---------------------|-----------------|-------------------------------------|
| Root    | `resourceCode`      | string          | Code de la ressource                |
|         | `resourceName`      | string          | Nom de la ressource                 |
|         | `fromDate`          | date            | Date de début de la période         |
|         | `toDate`            | date            | Date de fin de la période           |
|         | `days`              | Day[]           | Jours de la période                 |
| Day     | `date`              | date            | Date du jour                        |
|         | `totalSlots`        | number          | Nombre total de créneaux            |
|         | `availableSlots`    | number          | Créneaux disponibles                |
|         | `totalCapacity`     | number          | Capacité totale                     |
|         | `remainingCapacity` | number          | Capacité restante                   |
|         | `status`            | string          | AVAILABLE, FULL, CLOSED             |
|         | `windows`           | Window[]        | Fenêtres de disponibilité           |
| Window  | `startedAt`         | LocalDateTime   | Début du créneau                    |
|         | `endedAt`           | LocalDateTime   | Fin du créneau                      |
|         | `slotDurationMinutes`| number         | Durée du créneau (min)              |
|         | `totalCapacity`     | number          | Capacité du créneau                 |
|         | `remainingCapacity` | number          | Places restantes                    |
|         | `available`         | boolean         | Disponible                          |
|         | `active`            | boolean         | Actif                               |

---

## 13. Calendrier admin (avec occupation)

> Endpoints authentifiés sous `/api/v1/resource-availabilities/calendar/`.
> Même structure que le calendrier public mais enrichie avec les données d'occupation pour l'admin.

### 13.1 Consulter le calendrier d'une ressource (admin)

```
GET /api/v1/resource-availabilities/calendar/{resourceCode}
```

| Param          | Type      | Requis | In    | Description                          |
|----------------|-----------|--------|-------|--------------------------------------|
| `resourceCode` | string    | oui    | path  | Code de la ressource                 |
| `fromDate`     | LocalDate | non    | query | Date de début (défaut: aujourd'hui)  |
| `toDate`       | LocalDate | non    | query | Date de fin (défaut: +1 mois)        |

**Réponse** `200 OK`

```json
{
  "resourceCode": "RES-00001",
  "resourceName": "Bureau B-301",
  "fromDate": "2026-07-01",
  "toDate": "2026-07-07",
  "days": [
    {
      "date": "2026-07-01",
      "totalSlots": 20,
      "availableSlots": 15,
      "occupiedSlots": 5,
      "totalCapacity": 80,
      "remainingCapacity": 60,
      "usedCapacity": 20,
      "occupancyRate": 25.0,
      "occupancyLevel": "PARTIALLY_OCCUPIED",
      "status": "PARTIAL",
      "windows": [
        {
          "id": 101,
          "startedAt": "2026-07-01T08:00:00",
          "endedAt": "2026-07-01T08:30:00",
          "slotDurationMinutes": 30,
          "totalCapacity": 4,
          "remainingCapacity": 4,
          "usedCapacity": 0,
          "occupancyRate": 0.0,
          "occupancyLevel": "FREE",
          "available": true,
          "active": true
        },
        {
          "id": 102,
          "startedAt": "2026-07-01T10:00:00",
          "endedAt": "2026-07-01T10:30:00",
          "slotDurationMinutes": 30,
          "totalCapacity": 4,
          "remainingCapacity": 1,
          "usedCapacity": 3,
          "occupancyRate": 75.0,
          "occupancyLevel": "HIGHLY_OCCUPIED",
          "available": true,
          "active": true
        },
        {
          "id": 103,
          "startedAt": "2026-07-01T14:00:00",
          "endedAt": "2026-07-01T14:30:00",
          "slotDurationMinutes": 30,
          "totalCapacity": 4,
          "remainingCapacity": 0,
          "usedCapacity": 4,
          "occupancyRate": 100.0,
          "occupancyLevel": "FULL",
          "available": false,
          "active": true
        }
      ]
    }
  ]
}
```

**Structure de réponse :**

| Niveau  | Champ               | Type    | Description                                      |
|---------|---------------------|---------|--------------------------------------------------|
| Root    | `resourceCode`      | string  | Code de la ressource                             |
|         | `resourceName`      | string  | Nom de la ressource                              |
|         | `fromDate`          | date    | Date de début de la période                      |
|         | `toDate`            | date    | Date de fin de la période                        |
|         | `days`              | Day[]   | Jours de la période                              |
| Day     | `date`              | date    | Date du jour                                     |
|         | `totalSlots`        | number  | Nombre total de créneaux                         |
|         | `availableSlots`    | number  | Créneaux encore disponibles                      |
|         | `occupiedSlots`     | number  | Créneaux partiellement ou totalement occupés     |
|         | `totalCapacity`     | number  | Capacité totale cumulée                          |
|         | `remainingCapacity` | number  | Capacité restante cumulée                        |
|         | `usedCapacity`      | number  | Capacité utilisée cumulée                        |
|         | `occupancyRate`     | number  | Taux d'occupation en % (0.0 – 100.0)            |
|         | `occupancyLevel`    | string  | FREE / PARTIALLY_OCCUPIED / HIGHLY_OCCUPIED / FULL |
|         | `status`            | string  | AVAILABLE / PARTIAL / FULL                       |
|         | `windows`           | Window[]| Détail de chaque créneau                         |
| Window  | `id`                | number  | ID du créneau                                    |
|         | `startedAt`         | datetime| Début du créneau                                 |
|         | `endedAt`           | datetime| Fin du créneau                                   |
|         | `slotDurationMinutes`| number | Durée du créneau (min)                           |
|         | `totalCapacity`     | number  | Capacité du créneau                              |
|         | `remainingCapacity` | number  | Places restantes                                 |
|         | `usedCapacity`      | number  | Places utilisées                                 |
|         | `occupancyRate`     | number  | Taux d'occupation du créneau (%)                 |
|         | `occupancyLevel`    | string  | FREE / PARTIALLY_OCCUPIED / HIGHLY_OCCUPIED / FULL |
|         | `available`         | boolean | Disponible pour réservation                      |
|         | `active`            | boolean | Actif                                            |

> **Usage frontend** : Afficher un calendrier mensuel avec un code couleur par jour basé sur `occupancyLevel`. Lorsqu'un jour est cliqué, afficher la liste des `windows` avec leur niveau d'occupation individuel.

---

## 14. Créneaux occupés par ressource

### 14.1 Lister les créneaux occupés

```
GET /api/v1/resource-availabilities/occupied/{resourceCode}
```

> Retourne uniquement les créneaux où au moins une place est occupée (`usedCapacity > 0`).

| Param          | Type      | Requis | In    | Description                          |
|----------------|-----------|--------|-------|--------------------------------------|
| `resourceCode` | string    | oui    | path  | Code de la ressource                 |
| `fromDate`     | LocalDate | non    | query | Date de début (défaut: aujourd'hui)  |
| `toDate`       | LocalDate | non    | query | Date de fin (défaut: +1 mois)        |

**Réponse** `200 OK`

```json
[
  {
    "id": 102,
    "resourceCode": "RES-00001",
    "resourceName": "Bureau B-301",
    "startedAt": "2026-07-01T10:00:00",
    "endedAt": "2026-07-01T10:30:00",
    "slotDurationMinutes": 30,
    "totalCapacity": 4,
    "remainingCapacity": 1,
    "usedCapacity": 3,
    "occupancyRate": 75.0,
    "occupancyLevel": "HIGHLY_OCCUPIED",
    "active": true
  },
  {
    "id": 103,
    "resourceCode": "RES-00001",
    "resourceName": "Bureau B-301",
    "startedAt": "2026-07-01T14:00:00",
    "endedAt": "2026-07-01T14:30:00",
    "slotDurationMinutes": 30,
    "totalCapacity": 4,
    "remainingCapacity": 0,
    "usedCapacity": 4,
    "occupancyRate": 100.0,
    "occupancyLevel": "FULL",
    "active": true
  }
]
```

| Champ              | Type     | Description                                              |
|--------------------|----------|----------------------------------------------------------|
| `id`               | number   | ID du créneau                                            |
| `resourceCode`     | string   | Code de la ressource                                     |
| `resourceName`     | string   | Nom de la ressource                                      |
| `startedAt`        | datetime | Début du créneau                                         |
| `endedAt`          | datetime | Fin du créneau                                           |
| `slotDurationMinutes` | number| Durée du créneau (min)                                   |
| `totalCapacity`    | number   | Capacité totale du créneau                               |
| `remainingCapacity`| number   | Places restantes                                         |
| `usedCapacity`     | number   | Places occupées                                          |
| `occupancyRate`    | number   | Taux d'occupation (%)                                    |
| `occupancyLevel`   | string   | FREE / PARTIALLY_OCCUPIED / HIGHLY_OCCUPIED / FULL       |
| `active`           | boolean  | Créneau actif                                            |

> **Usage frontend** : Afficher un tableau ou une timeline des créneaux occupés pour une ressource donnée. Permet à l'admin de voir en un coup d'œil quels créneaux sont les plus chargés.

---

# PARTIE 3 — Tarification

---

## 15. Règles de tarification

### 15.1 Créer une règle de prix

```
POST /api/v1/resource-pricing-rules
```

**Body**

```json
{
  "resourceCode": "RES-00001",
  "bookingUnit": "HOUR",
  "price": 15000,
  "label": "Tarif horaire standard",
  "dayOfWeek": null,
  "startsAt": "08:00",
  "endsAt": "18:00",
  "adjustmentType": null,
  "adjustmentValue": null,
  "validFrom": "2026-07-01",
  "validUntil": "2027-06-30",
  "lastMinuteMinutes": null,
  "priority": 1,
  "active": true
}
```

| Champ              | Type                       | Requis | Description                                       |
|--------------------|----------------------------|--------|---------------------------------------------------|
| `resourceCode`     | string                     | oui    | Code de la ressource                              |
| `bookingUnit`      | ResourceBookingUnit        | oui    | Unité de réservation                              |
| `price`            | number                     | oui    | Prix de base (en centimes FCFA)                   |
| `label`            | string                     | non    | Libellé de la règle                               |
| `dayOfWeek`        | number (1-7)               | non    | Jour spécifique (1=Lundi, 7=Dimanche)             |
| `startsAt`         | LocalTime                  | non    | Heure de début d'application                      |
| `endsAt`           | LocalTime                  | non    | Heure de fin d'application                        |
| `adjustmentType`   | ResourcePriceAdjustmentType| non    | Type d'ajustement                                 |
| `adjustmentValue`  | number                     | non    | Valeur de l'ajustement                            |
| `validFrom`        | date (ISO)                 | non    | Date de début de validité                         |
| `validUntil`       | date (ISO)                 | non    | Date de fin de validité                           |
| `lastMinuteMinutes`| number                     | non    | Applicable si réservé dans les N dernières min    |
| `priority`         | number                     | non    | Priorité (plus bas = plus prioritaire)            |
| `active`           | boolean                    | non    | Active (défaut: true)                             |

**Réponse** `201 Created`

### 15.2 Récupérer une règle

```
GET /api/v1/resource-pricing-rules/{id}
```

**Réponse** `200 OK`

```json
{
  "id": 1,
  "resourceCode": "RES-00001",
  "bookingUnit": "HOUR",
  "price": 15000,
  "label": "Tarif horaire standard",
  "dayOfWeek": null,
  "startsAt": "08:00",
  "endsAt": "18:00",
  "adjustmentType": null,
  "adjustmentValue": null,
  "validFrom": "2026-07-01",
  "validUntil": "2027-06-30",
  "lastMinuteMinutes": null,
  "priority": 1,
  "active": true
}
```

### 15.3 Lister les règles (paginé)

```
GET /api/v1/resource-pricing-rules
```

| Param          | Type   | Requis | Description                |
|----------------|--------|--------|----------------------------|
| `resourceCode` | string | non    | Filtrer par ressource      |
| `page`         | int    | non    | Page (défaut: 0)           |
| `size`         | int    | non    | Taille page (défaut: 20)   |

**Réponse** `200 OK` → `PaginatedResponse<ResourcePricingRuleResponse>`

### 15.4 Modifier une règle

```
PUT /api/v1/resource-pricing-rules/{id}
```

**Body** — Champs optionnels

```json
{
  "price": 18000,
  "label": "Tarif horaire premium",
  "priority": 2
}
```

**Réponse** `200 OK` → `ResourcePricingRuleResponse`

### 15.5 Activer/désactiver une règle

```
PATCH /api/v1/resource-pricing-rules/{id}/active?active=true
```

| Param    | Type    | Requis | In    | Description        |
|----------|---------|--------|-------|--------------------|
| `active` | boolean | oui    | query | Nouvel état actif  |

**Réponse** `204 No Content`

### 15.6 Supprimer une règle

```
DELETE /api/v1/resource-pricing-rules/{id}
```

**Réponse** `204 No Content`

---

## 16. Devis de prix

### 16.1 Obtenir un devis de prix

```
GET /api/v1/resource-pricing-rules/quote
```

| Param          | Type               | Requis | Description                      |
|----------------|--------------------|--------|----------------------------------|
| `resourceCode` | string             | oui    | Code de la ressource             |
| `bookingUnit`  | ResourceBookingUnit| oui    | Unité de réservation             |
| `startedAt`    | LocalDateTime      | oui    | Début de la réservation          |
| `endedAt`      | LocalDateTime      | oui    | Fin de la réservation            |

**Réponse** `200 OK`

```json
{
  "resourceCode": "RES-00001",
  "bookingUnit": "HOUR",
  "startedAt": "2026-07-01T10:00:00",
  "endedAt": "2026-07-01T12:00:00",
  "basePrice": 15000,
  "finalPrice": 30000,
  "appliedRuleId": 1,
  "appliedRuleLabel": "Tarif horaire standard",
  "adjustmentType": null,
  "adjustmentValue": null
}
```

| Champ              | Type   | Description                                         |
|--------------------|--------|-----------------------------------------------------|
| `basePrice`        | number | Prix unitaire de base (centimes FCFA)               |
| `finalPrice`       | number | Prix total calculé après ajustements                |
| `appliedRuleId`    | number | ID de la règle appliquée                            |
| `appliedRuleLabel` | string | Libellé de la règle appliquée                       |
| `adjustmentType`   | string | Type d'ajustement appliqué (null si aucun)          |
| `adjustmentValue`  | number | Valeur de l'ajustement appliqué (null si aucun)     |

---

# PARTIE 4 — Galerie photos

---

## 17. Photos de ressource

> **Sécurité** : Les endpoints d'écriture requièrent l'autorité `RESOURCE:GALLERY` ou `RESOURCE_GALLERY`.

### 17.1 Ajouter une photo

```
POST /api/v1/resources/{resourceCode}/photos
```

**Body**

```json
{
  "documentCode": "DOC-20260620-00001",
  "caption": "Vue d'ensemble du bureau B-301",
  "cover": true,
  "displayOrder": 1,
  "active": true
}
```

| Champ          | Type    | Requis | Description                                    |
|----------------|---------|--------|------------------------------------------------|
| `documentCode` | string  | oui    | Code du document (photo uploadée via GED)      |
| `caption`      | string  | non    | Légende de la photo                            |
| `cover`        | boolean | non    | Photo de couverture (défaut: false)            |
| `displayOrder` | number  | non    | Ordre d'affichage                              |
| `active`       | boolean | non    | Active (défaut: true)                          |

**Réponse** `201 Created`

```json
{
  "id": 1,
  "resourceCode": "RES-00001",
  "documentCode": "DOC-20260620-00001",
  "caption": "Vue d'ensemble du bureau B-301",
  "cover": true,
  "displayOrder": 1,
  "active": true
}
```

### 17.2 Lister les photos d'une ressource

```
GET /api/v1/resources/{resourceCode}/photos
```

**Réponse** `200 OK` → `List<ResourcePhotoResponse>`

### 17.3 Modifier une photo

```
PATCH /api/v1/resources/{resourceCode}/photos/{id}
```

**Body**

```json
{
  "documentCode": "DOC-20260620-00002",
  "caption": "Nouvelle vue bureau B-301",
  "cover": false,
  "displayOrder": 2
}
```

**Réponse** `200 OK` → `ResourcePhotoResponse`

### 17.4 Supprimer une photo

```
DELETE /api/v1/resources/{resourceCode}/photos/{id}
```

**Réponse** `204 No Content`

---

# PARTIE 5 — Intégration

---

## 18. Workflows recommandés

### Workflow 1 — Configurer une nouvelle ressource complète

```
1. POST /resource-types                        → Créer le type (si nécessaire)
2. POST /resource-groups                       → Créer le groupe (si nécessaire)
3. POST /resource-policies                     → Créer la politique (si nécessaire)
4. POST /resources                             → Créer la ressource
5. POST /resource-amenities                    → Créer des équipements (si nécessaire)
6. POST /resources/amenities/link              → Lier les équipements
7. POST /resource-pricing-rules                → Ajouter les règles de prix
8. POST /resource-availabilities               → Définir les créneaux de disponibilité
9. POST /resources/{code}/photos               → Ajouter des photos
```

### Workflow 2 — Réserver une ressource

```
1. GET  /resources/search/basic?query=...      → Rechercher une ressource
2. GET  /resource-availabilities/remaining      → Vérifier les créneaux disponibles
3. GET  /resource-pricing-rules/quote           → Obtenir le prix
4. POST /resource-availabilities/reserve        → Réserver le créneau
   --- Si annulation ---
5. PATCH /resource-availabilities/release       → Libérer le créneau
```

### Workflow 3 — Consulter la disponibilité (portail client)

```
1. GET /public/resources/{code}/calendar        → [PUBLIC] Voir le calendrier
```

### Workflow 4 — Gestion des fermetures

```
1. POST  /resource-closures                    → Créer une fermeture
2. GET   /resource-closures?resourceCode=...   → Voir les fermetures
3. PATCH /resource-closures/{id}/active         → Activer/désactiver
4. DELETE /resource-closures/{id}              → Supprimer
```

### Workflow 5 — Configurer la tarification

```
1. POST /resource-pricing-rules                → Créer une règle standard
2. POST /resource-pricing-rules                → Créer une règle week-end (dayOfWeek=6 ou 7)
3. POST /resource-pricing-rules                → Créer une règle last-minute
4. GET  /resource-pricing-rules/quote           → Tester le calcul de prix
```

### Workflow 6 — Modifier la classification

```
1. PATCH /resources/{code}/classification      → Changer type/groupe/politique
2. PATCH /resources/{code}/status              → Changer le statut
3. PATCH /resources/{code}/booking-enabled     → Activer/désactiver la réservation
4. PATCH /resources/{code}/portal-visible      → Activer/désactiver la visibilité
```

### Workflow 7 — Consulter l'occupation admin

```
1. GET /resource-availabilities/calendar/{code}                     → Calendrier mensuel avec niveaux d'occupation
   --- Clic sur un jour dans le frontend ---
2. → Les windows du jour sélectionné sont déjà dans la réponse     → Afficher chaque créneau avec son occupancyLevel
```

### Workflow 8 — Analyser les créneaux occupés

```
1. GET /resource-availabilities/occupied/{code}?fromDate=...&toDate=... → Voir tous les créneaux occupés
   --- Filtrer côté frontend par occupancyLevel ---
2. → Identifier les créneaux FULL pour ajuster les disponibilités
3. POST /resource-availabilities                                       → Ajouter des créneaux aux heures de forte demande
```

---

## 19. Écrans recommandés

### 19.1 Liste des ressources

**Route suggérée** : `/admin/resources`

| Composant          | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| Tableau paginé     | Colonnes : Code, Nom, Type, Groupe, Statut, Capacité, Réservable  |
| Filtres            | Par type, groupe, politique, statut                                |
| Recherche          | Barre de recherche avec autocomplete (endpoint `/search/basic`)    |
| Badges couleur     | Vert=ACTIVE, Jaune=INACTIVE, Orange=MAINTENANCE, Rouge=OUT_OF_SERVICE |
| Vue grille         | Option d'affichage en cartes avec photo de couverture              |
| Actions            | Voir, Modifier, Changer statut                                    |
| Bouton             | « + Nouvelle ressource »                                          |

### 19.2 Détail d'une ressource

**Route suggérée** : `/admin/resources/:code`

| Section            | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| En-tête            | Nom, code, statut (badge), type, groupe                            |
| Informations       | Capacité, zone, localisation, politique, actif, réservable, portail|
| Onglet Équipements | Liste des amenities liées avec quantité et prix                    |
| Onglet Planning    | Calendrier des disponibilités (vue semaine)                        |
| Onglet Tarifs      | Tableau des règles de prix actives                                 |
| Onglet Photos      | Galerie avec drag & drop pour réordonner                           |
| Onglet Fermetures  | Liste des fermetures programmées                                   |
| Actions            | Modifier, Changer statut, Changer classification                  |

### 19.3 Formulaire de création / modification de ressource

**Route suggérée** : `/admin/resources/new` et `/admin/resources/:code/edit`

| Section            | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| Infos générales    | Nom, description, capacité                                         |
| Classification     | Type (dropdown), Groupe (dropdown), Politique (dropdown)           |
| Localisation       | Zone, étage, label de localisation                                 |
| Paramètres         | Réservable (switch), Portail visible (switch), Statut (select)     |

### 19.4 Gestion des types / groupes / politiques

**Route suggérée** : `/admin/resources/settings`

| Onglet     | Description                                                            |
|------------|------------------------------------------------------------------------|
| Types      | CRUD des types de ressource avec tableau paginé + recherche            |
| Groupes    | CRUD des groupes avec toggle portail visible                           |
| Politiques | CRUD des politiques avec paramètres de durée et annulation             |
| Équipements| CRUD des amenities globales                                            |

### 19.5 Planning & Disponibilités

**Route suggérée** : `/admin/resources/:code/availability`

| Composant          | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| Calendrier semaine | Vue semaine avec créneaux colorés (vert=dispo, rouge=réservé)      |
| Calendrier mois    | Vue mois avec indicateurs de disponibilité par jour                |
| Formulaire ajout   | Modal pour créer un créneau (date/heure début, fin, capacité)      |
| Filtres            | Par ressource, par période                                         |
| Vue groupée        | Toutes les ressources côte à côte (endpoint `/grouped`)           |

### 19.6 Gestion des tarifs

**Route suggérée** : `/admin/resources/:code/pricing`

| Composant          | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| Tableau            | Règles de prix avec unité, prix, plage horaire, jours, priorité   |
| Formulaire         | Modal de création/édition de règle                                 |
| Simulateur         | Formulaire de devis rapide (sélection dates → calcul prix)        |
| Timeline           | Visualisation des validités temporelles des règles                 |

### 19.7 Calendrier public (portail client)

**Route suggérée** : `/resources/:code/calendar`

| Composant          | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| Calendrier         | Vue mois avec statut par jour (AVAILABLE, FULL, CLOSED)            |
| Détail jour        | Clic sur un jour → liste des fenêtres horaires disponibles         |
| Légende            | Couleurs : Vert=Disponible, Gris=Complet, Rouge=Fermé             |
| Sélection          | Clic créneau → redirige vers le formulaire de réservation          |

### 19.8 Galerie photos

**Route suggérée** : `/admin/resources/:code/photos`

| Composant          | Description                                                        |
|--------------------|--------------------------------------------------------------------|
| Grille photos      | Miniatures avec badge « Couverture » sur la photo principale       |
| Upload             | Bouton upload → utilise le module Document (GED) pour stocker      |
| Réordonner         | Drag & drop pour changer le `displayOrder`                         |
| Actions par photo  | Modifier légende, définir comme couverture, supprimer              |

### 19.9 Calendrier admin (occupation)

**Route suggérée** : `/admin/resources/:code/occupation`

| Composant            | Description                                                        |
|----------------------|--------------------------------------------------------------------|
| Calendrier mensuel   | Vue mois, chaque jour coloré selon `occupancyLevel`                |
| Légende couleurs     | Vert=FREE, Jaune=PARTIALLY_OCCUPIED, Orange=HIGHLY_OCCUPIED, Rouge=FULL |
| Détail jour (clic)   | Panel latéral ou modal listant les `windows` du jour sélectionné   |
| Par créneau          | Barre de progression d'occupation + badge occupancyLevel           |
| Stats jour           | Taux d'occupation global, capacité utilisée / totale               |
| Navigation           | Boutons mois précédent / suivant, sélecteur de date               |
| Sélecteur ressource  | Dropdown ou autocomplete pour changer de ressource                 |

**Détail d'un créneau (dans le panel jour) :**

| Info                 | Description                                                        |
|----------------------|--------------------------------------------------------------------|
| Plage horaire        | `10:00 – 10:30`                                                    |
| Jauge d'occupation   | Barre colorée `usedCapacity / totalCapacity`                       |
| Taux                 | `75.0%` avec badge HIGHLY_OCCUPIED                                 |
| Places restantes     | `1 / 4 places disponibles`                                         |

### 19.10 Tableau des créneaux occupés

**Route suggérée** : `/admin/resources/:code/occupied`

| Composant            | Description                                                        |
|----------------------|--------------------------------------------------------------------|
| Tableau              | Colonnes : Date, Horaire, Capacité totale, Utilisée, Restante, Taux, Niveau |
| Filtres              | Par période (fromDate/toDate), par niveau d'occupation             |
| Tri                  | Par date, par taux d'occupation (décroissant pour voir les plus chargés) |
| Badges               | Couleurs par niveau : PARTIALLY_OCCUPIED, HIGHLY_OCCUPIED, FULL    |
| Export               | Option export CSV pour analyse                                     |
| Vue timeline         | Alternative au tableau : timeline horizontale avec blocs colorés   |

---

## 20. Codes d'erreur

| Code HTTP | Situation                                           |
|-----------|-----------------------------------------------------|
| `400`     | Données invalides, champs requis manquants          |
| `404`     | Ressource, type, groupe, politique non trouvé       |
| `409`     | Conflit de réservation (capacité insuffisante)      |
| `422`     | Règle métier violée (ex: créneau qui chevauche)     |

**Format d'erreur standard**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Le champ 'name' est obligatoire",
  "errorCode": "VALIDATION_ERROR",
  "timestamp": "2026-06-20T10:00:00Z"
}
```
