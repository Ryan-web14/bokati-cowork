# Guide Frontend/API — Module Resource

## Vue d'ensemble

Ce document explique comment le frontend doit utiliser les endpoints du module `resource`.

Il couvre l'ensemble des sous-modules :

- `resource` — ressources principales (salles, espaces, equipements)
- `resource-types` — types de ressources (classification)
- `resource-groups` — groupes de ressources (regroupement logique)
- `resource-policies` — politiques de reservation (contraintes duree, delai, annulation)
- `resource-amenities` — equipements et services associes aux ressources
- `resource-availabilities` — creneaux de disponibilite et reservations de capacite
- `resource-closures` — fermetures et blocages ponctuels
- `resource-pricing-rules` — regles de tarification par unite de reservation

Base URL :

- `/sni/api/v1`

---

## Concepts principaux

- `Resource`
  - represente une ressource reservable (salle de reunion, espace de coworking, bureau prive, equipement)
  - chaque ressource appartient a un type, peut appartenir a un groupe, et peut avoir une politique de reservation
- `ResourceType`
  - classifie une ressource (ex: SALLE_REUNION, BUREAU_PRIVE, ESPACE_COWORKING, EQUIPEMENT)
  - defini par l'administrateur, reutilisable sur plusieurs ressources
- `ResourceGroup`
  - regroupe plusieurs ressources sous une meme categorie logique
  - peut etre visible ou cache du portail client
- `ResourcePolicy`
  - definit les contraintes de reservation : duree min/max, delai minimum avant reservation, delai d'annulation
  - une politique peut autoriser ou interdire les annulations
- `ResourceAmenities`
  - equipements ou services disponibles sur une ressource (WiFi, projecteur, climatisation, tableau blanc, etc.)
  - associes aux ressources via un systeme de liens
- `ResourceAvailability`
  - creneaux de disponibilite pre-generes par le backend (slots de 30 minutes)
  - gere la capacite totale et la capacite restante avec gestion de la concurrence
- `ResourceClosure`
  - fermeture ponctuelle d'une ressource (maintenance, evenement exceptionnel)
  - bloque la reservation pendant la periode definie
- `ResourcePricingRule`
  - regle de tarification associee a une ressource pour une unite de reservation donnee (heure, demi-journee, jour, etc.)

Statuts de ressource (`ResourceStatus`) :

- `ACTIVE` — ressource disponible a la reservation
- `INACTIVE` — ressource desactivee temporairement
- `MAINTENANCE` — en cours de maintenance, non reservable
- `OUT_OF_SERVICE` — hors service
- `ARCHIVED` — archivee, n'apparait plus dans les listes

Unites de reservation (`ResourceBookingUnit`) :

- `HOUR` — facturation a l'heure
- `HALF_DAY` — facturation a la demi-journee
- `DAY` — facturation a la journee
- `WEEK` — facturation a la semaine
- `MONTH` — facturation au mois

---

## Champs generes par le backend

Ne jamais afficher ces champs en saisie libre dans les formulaires de creation. Ils sont generes et retournes par l'API.

- `code` (Resource, ResourceType, ResourceGroup, ResourcePolicy, ResourceAmenities)
- `id` (ResourceAvailability, ResourceClosure, ResourcePricingRule)
- `createdAt`, `updatedAt`

---

## Format de pagination

Toutes les listes retournent un objet pagine Spring :

```json
{
  "content": [...],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true, "unsorted": false }
  },
  "totalElements": 42,
  "totalPages": 3,
  "last": false,
  "first": true
}
```

Parametres de pagination : `page=0&size=20&sort=name,asc`

---

## 1. Ressources

### Creer une ressource

`POST /resources`

Body :

```json
{
  "typeId": "TYPE-001",
  "groupId": "GRP-001",
  "policyId": "POL-001",
  "name": "Salle Horizon",
  "description": "Grande salle de reunion avec vue panoramique",
  "capacity": 12,
  "zone": "Aile Nord",
  "locationLabel": "2eme etage, porte A201",
  "status": "ACTIVE",
  "bookingEnabled": true,
  "portalVisible": true
}
```

> `typeId` est obligatoire. `groupId` et `policyId` sont optionnels.
> `status` par defaut : `ACTIVE`. `bookingEnabled` par defaut : `true`. `portalVisible` par defaut : `true`.

Response :

```json
{
  "code": "RES-0001",
  "typeCode": "TYPE-001",
  "groupCode": "GRP-001",
  "policyCode": "POL-001",
  "name": "Salle Horizon",
  "description": "Grande salle de reunion avec vue panoramique",
  "capacity": 12,
  "zone": "Aile Nord",
  "locationLabel": "2eme etage, porte A201",
  "status": "ACTIVE",
  "bookingEnabled": true,
  "portalVisible": true,
  "displayOrder": 0,
  "active": true
}
```

---

### Obtenir une ressource

`GET /resources/{code}`

Response identique au format ci-dessus.

---

### Lister les ressources

`GET /resources?typeCode=TYPE-001&groupCode=GRP-001&policyCode=POL-001&summary=false&page=0&size=20`

Parametres optionnels :
- `typeCode` — filtrer par type
- `groupCode` — filtrer par groupe
- `policyCode` — filtrer par politique
- `summary=true` — retourner uniquement les champs essentiels (vue allegee)

Response avec `summary=false` (defaut) : liste de `ResourceResponse`

Response avec `summary=true` : liste de `ResourceSummaryResponse`

```json
{
  "content": [
    {
      "code": "RES-0001",
      "typeCode": "TYPE-001",
      "name": "Salle Horizon",
      "status": "ACTIVE",
      "bookingEnabled": true,
      "portalVisible": true,
      "active": true
    }
  ],
  "totalElements": 5,
  "totalPages": 1
}
```

---

### Recherche rapide

`GET /resources/search/basic?query=horizon`

Retourne une liste allegee de ressources correspondant au terme de recherche (nom, zone, localisation).

---

### Recherche avancee

`POST /resources/search`

Body :

```json
{
  "code": "RES-0001",
  "typeId": 1,
  "groupId": 2,
  "policyId": 3,
  "name": "Horizon",
  "zone": "Aile Nord",
  "locationLabel": "A201",
  "status": "ACTIVE",
  "portalVisible": true,
  "bookingEnabled": true,
  "active": true,
  "minCapacity": 10
}
```

Tous les champs sont optionnels. Les filtres sont combines avec un operateur `AND`.

---

### Mettre a jour une ressource

`PUT /resources/{code}`

Body :

```json
{
  "name": "Salle Horizon Renovee",
  "description": "Salle renovee avec nouveau mobilier",
  "capacity": 14,
  "zone": "Aile Nord",
  "floorLabel": "2eme etage",
  "locationLabel": "Porte A201",
  "bookingEnabled": true,
  "portalVisible": true
}
```

---

### Changer le type, groupe ou politique

`PATCH /resources/{code}/classification`

Body :

```json
{
  "typeId": "TYPE-002",
  "groupId": "GRP-003",
  "policyId": "POL-002"
}
```

> `typeId` est obligatoire. `groupId` et `policyId` peuvent etre null pour les dissocier.

---

### Changer le statut

`PATCH /resources/{code}/status`

Body :

```json
{
  "status": "MAINTENANCE"
}
```

Valeurs : `ACTIVE`, `INACTIVE`, `MAINTENANCE`, `OUT_OF_SERVICE`, `ARCHIVED`

---

### Activer/desactiver la reservation

`PATCH /resources/{code}/booking-enabled`

Body :

```json
{
  "bookingEnabled": false
}
```

---

### Activer/desactiver la visibilite portail

`PATCH /resources/{code}/portal-visible`

Body :

```json
{
  "portalVisible": true
}
```

---

### Activer/desactiver la ressource

`PATCH /resources/{code}/active`

Body :

```json
{
  "active": false
}
```

---

### Modifier l'ordre d'affichage

`PATCH /resources/{code}/display-order`

Body :

```json
{
  "displayOrder": 3
}
```

---

### Supprimer une ressource

`DELETE /resources/{code}`

Suppression logique (soft delete). La ressource n'apparait plus dans les listes mais reste en base.

---

## 2. Types de ressources

### Creer un type

`POST /resource-types`

Body :

```json
{
  "name": "Salle de reunion",
  "description": "Espaces dedies aux reunions",
  "active": true
}
```

Response :

```json
{
  "code": "TYPE-001",
  "name": "Salle de reunion",
  "description": "Espaces dedies aux reunions",
  "active": true
}
```

---

### Obtenir un type

`GET /resource-types/{code}`

---

### Lister les types

`GET /resource-types?page=0&size=20`

---

### Rechercher les types

`GET /resource-types/search?query=reunion`

`GET /resource-types/search/basic?query=reunion`

---

### Mettre a jour un type

`PUT /resource-types/{code}`

Body :

```json
{
  "name": "Salle de conference",
  "description": "Espaces grandes conferences",
  "active": true
}
```

---

### Supprimer un type

`DELETE /resource-types/{code}`

> Ne pas supprimer un type utilise par des ressources actives.

---

## 3. Groupes de ressources

### Creer un groupe

`POST /resource-groups`

Body :

```json
{
  "name": "Salles premium",
  "description": "Salles haut de gamme avec services inclus",
  "portalVisible": true,
  "active": true
}
```

Response :

```json
{
  "code": "GRP-001",
  "name": "Salles premium",
  "description": "Salles haut de gamme avec services inclus",
  "portalVisible": true,
  "active": true
}
```

---

### Obtenir un groupe

`GET /resource-groups/{code}`

---

### Lister les groupes

`GET /resource-groups?page=0&size=20`

---

### Rechercher les groupes

`GET /resource-groups/search?query=premium`

`GET /resource-groups/search/basic?query=premium`

---

### Mettre a jour un groupe

`PUT /resource-groups/{code}`

Body :

```json
{
  "name": "Salles premium+",
  "description": "Salles avec services etendus",
  "portalVisible": true,
  "active": true
}
```

---

### Supprimer un groupe

`DELETE /resource-groups/{code}`

---

## 4. Politiques de reservation

### Creer une politique

`POST /resource-policies`

Body :

```json
{
  "name": "Politique Standard",
  "description": "Politique pour salles de reunion classiques",
  "minBookingDurationMinutes": 30,
  "maxBookingDurationMinutes": 480,
  "minBookingNoticeMinutes": 60,
  "cancellationNoticeMinutes": 120,
  "allowCancellation": true,
  "active": true
}
```

> `minBookingDurationMinutes`, `maxBookingDurationMinutes` et `minBookingNoticeMinutes` sont obligatoires.
> `cancellationNoticeMinutes` n'est pertinent que si `allowCancellation = true`.

Response :

```json
{
  "code": "POL-001",
  "name": "Politique Standard",
  "description": "Politique pour salles de reunion classiques",
  "minBookingDurationMinutes": 30,
  "maxBookingDurationMinutes": 480,
  "minBookingNoticeMinutes": 60,
  "cancellationNoticeMinutes": 120,
  "allowCancellation": true,
  "active": true
}
```

---

### Obtenir une politique

`GET /resource-policies/{code}`

---

### Lister les politiques

`GET /resource-policies?page=0&size=20`

---

### Rechercher les politiques

`GET /resource-policies/search?query=standard`

`GET /resource-policies/search/basic?query=standard`

---

### Mettre a jour une politique

`PUT /resource-policies/{code}`

Body :

```json
{
  "name": "Politique Standard+",
  "minBookingDurationMinutes": 30,
  "maxBookingDurationMinutes": 720,
  "minBookingNoticeMinutes": 30,
  "cancellationNoticeMinutes": 60,
  "allowCancellation": true,
  "active": true
}
```

---

### Activer/desactiver une politique

`PATCH /resource-policies/{code}/status`

Body :

```json
{
  "active": false
}
```

---

### Modifier la politique d'annulation

`PATCH /resource-policies/{code}/allow-cancellation`

Body :

```json
{
  "allowCancellation": true
}
```

---

### Supprimer une politique

`DELETE /resource-policies/{code}`

---

## 5. Equipements (Amenities)

### Creer un equipement

`POST /resource-amenities`

Body :

```json
{
  "name": "Projecteur",
  "description": "Projecteur HD 4K avec telecommande",
  "active": true
}
```

Response :

```json
{
  "code": "AME-001",
  "name": "Projecteur",
  "description": "Projecteur HD 4K avec telecommande",
  "active": true
}
```

---

### Obtenir un equipement

`GET /resource-amenities/{code}`

---

### Lister les equipements

`GET /resource-amenities?page=0&size=20`

---

### Rechercher les equipements

`GET /resource-amenities/search?query=projecteur`

`GET /resource-amenities/search/basic?query=projecteur`

---

### Mettre a jour un equipement

`PUT /resource-amenities/{code}`

Body :

```json
{
  "name": "Projecteur laser",
  "description": "Projecteur laser ultra-lumineux",
  "active": true
}
```

---

### Supprimer un equipement

`DELETE /resource-amenities/{code}`

---

## 6. Liens ressource-equipement

### Associer un equipement a une ressource

`POST /resources/amenities/link`

Body :

```json
{
  "resourceCode": "RES-0001",
  "amenityCode": "AME-001"
}
```

---

### Lister les equipements d'une ressource

`GET /resources/{code}/amenities`

Response :

```json
[
  {
    "code": "AME-001",
    "name": "Projecteur",
    "description": "Projecteur HD 4K",
    "active": true
  },
  {
    "code": "AME-002",
    "name": "WiFi dedié",
    "description": "Connexion WiFi haut debit",
    "active": true
  }
]
```

---

### Dissocier un equipement d'une ressource

`DELETE /resources/{code}/amenities/{amenityCode}`

---

## 7. Disponibilites

### Creer un creneau de disponibilite

`POST /resource-availabilities`

Body :

```json
{
  "resourceCode": "RES-0001",
  "startedAt": "2026-04-20T08:00:00",
  "endedAt": "2026-04-20T18:00:00",
  "capacity": 1,
  "active": true
}
```

> Les dates utilisent le format `LocalDateTime` sans timezone : `"2026-04-20T08:00:00"`.
> Le backend genere automatiquement des slots de 30 minutes entre `startedAt` et `endedAt`.
> `capacity` par defaut : 1 (pour les salles non-partagees).

Response :

```json
[
  {
    "id": 1,
    "resourceCode": "RES-0001",
    "startedAt": "2026-04-20T08:00:00",
    "endedAt": "2026-04-20T08:30:00",
    "slotDurationMinutes": 30,
    "totalCapacity": 1,
    "remainingCapacity": 1,
    "available": true,
    "active": true
  },
  {
    "id": 2,
    "resourceCode": "RES-0001",
    "startedAt": "2026-04-20T08:30:00",
    "endedAt": "2026-04-20T09:00:00",
    "slotDurationMinutes": 30,
    "totalCapacity": 1,
    "remainingCapacity": 1,
    "available": true,
    "active": true
  }
]
```

---

### Lister les creneaux de disponibilite

`GET /resource-availabilities?resourceCode=RES-0001&page=0&size=50`

Filtre optionnel par `resourceCode`.

---

### Fenetres de disponibilite restantes

`GET /resource-availabilities/remaining?resourceCode=RES-0001`

Retourne les plages horaires continues encore disponibles pour une ressource.

Response :

```json
[
  {
    "resourceCode": "RES-0001",
    "startedAt": "2026-04-20T09:00:00",
    "endedAt": "2026-04-20T12:00:00",
    "durationMinutes": 180,
    "remainingCapacity": 1,
    "slotCount": 6
  },
  {
    "resourceCode": "RES-0001",
    "startedAt": "2026-04-20T14:00:00",
    "endedAt": "2026-04-20T18:00:00",
    "durationMinutes": 240,
    "remainingCapacity": 1,
    "slotCount": 8
  }
]
```

---

### Reserver une disponibilite

`POST /resource-availabilities/reserve`

Body :

```json
{
  "resourceCode": "RES-0001",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "quantity": 1
}
```

> Decrement atomique de la `remainingCapacity` sur tous les slots couverts par la plage.
> Utilise le versioning optimiste pour gerer la concurrence.

---

### Liberer une disponibilite

`PATCH /resource-availabilities/release`

Body :

```json
{
  "resourceCode": "RES-0001",
  "startedAt": "2026-04-20T09:00:00",
  "endedAt": "2026-04-20T11:00:00",
  "quantity": 1
}
```

---

## 8. Fermetures (Closures)

### Creer une fermeture

`POST /resource-closures`

Body :

```json
{
  "resourceCode": "RES-0001",
  "startedAt": "2026-04-25T00:00:00",
  "endedAt": "2026-04-25T23:59:00",
  "reason": "Maintenance annuelle",
  "active": true
}
```

Response :

```json
{
  "id": 1,
  "resourceCode": "RES-0001",
  "startedAt": "2026-04-25T00:00:00",
  "endedAt": "2026-04-25T23:59:00",
  "active": true
}
```

---

### Obtenir une fermeture

`GET /resource-closures/{id}`

---

### Lister les fermetures

`GET /resource-closures?resourceCode=RES-0001&page=0&size=20`

Filtre optionnel par `resourceCode`.

---

### Activer/desactiver une fermeture

`PATCH /resource-closures/{id}/active`

Body :

```json
{
  "active": false
}
```

---

### Supprimer une fermeture

`DELETE /resource-closures/{id}`

---

## 9. Regles de tarification

### Creer une regle de tarification

`POST /resource-pricing-rules`

Body :

```json
{
  "resourceCode": "RES-0001",
  "bookingUnit": "HOUR",
  "price": 5000,
  "active": true
}
```

> `bookingUnit` : `HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH`
> `price` en centimes ou dans la monnaie locale selon la convention du projet.

Response :

```json
{
  "id": 1,
  "resourceCode": "RES-0001",
  "bookingUnit": "HOUR",
  "price": 5000,
  "active": true
}
```

---

### Obtenir une regle de tarification

`GET /resource-pricing-rules/{id}`

---

### Lister les regles de tarification

`GET /resource-pricing-rules?resourceCode=RES-0001&page=0&size=20`

Filtre optionnel par `resourceCode`.

---

### Activer/desactiver une regle

`PATCH /resource-pricing-rules/{id}/active`

Body :

```json
{
  "active": false
}
```

---

### Supprimer une regle

`DELETE /resource-pricing-rules/{id}`

---

## Workflows recommandes

### Workflow de creation complete d'une ressource

1. S'assurer qu'au moins un type existe (`GET /resource-types`)
2. Creer ou selectionner un groupe (`POST /resource-groups` ou `GET /resource-groups`)
3. Creer ou selectionner une politique (`POST /resource-policies` ou `GET /resource-policies`)
4. Creer la ressource (`POST /resources`) avec `typeId`, `groupId`, `policyId`
5. Associer les equipements disponibles (`POST /resources/amenities/link`)
6. Definir les regles de tarification (`POST /resource-pricing-rules`)
7. Generer les creneaux de disponibilite (`POST /resource-availabilities`)

---

### Workflow de mise hors service

1. Desactiver la reservation : `PATCH /resources/{code}/booking-enabled` avec `{"bookingEnabled": false}`
2. Changer le statut : `PATCH /resources/{code}/status` avec `{"status": "MAINTENANCE"}`
3. Creer une fermeture : `POST /resource-closures` avec les dates de maintenance
4. Une fois le travail termine, reactiver : `PATCH /resources/{code}/status` avec `{"status": "ACTIVE"}` + `PATCH /resources/{code}/booking-enabled` avec `{"bookingEnabled": true}`

---

### Workflow de consultation de disponibilite

1. Appeler `GET /resource-availabilities/remaining?resourceCode=RES-0001` pour obtenir les fenetres libres
2. Afficher les plages dans un composant calendrier ou timeline
3. A la selection d'une plage, appeler `POST /resource-availabilities/reserve` pour bloquer la capacite
4. En cas d'annulation, appeler `PATCH /resource-availabilities/release`

---

## Types TypeScript recommandes

```typescript
type ResourceStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE' | 'OUT_OF_SERVICE' | 'ARCHIVED'
type ResourceBookingUnit = 'HOUR' | 'HALF_DAY' | 'DAY' | 'WEEK' | 'MONTH'

interface ResourceResponse {
  code: string
  typeCode: string
  groupCode: string | null
  policyCode: string | null
  name: string
  description: string | null
  capacity: number
  zone: string | null
  locationLabel: string | null
  status: ResourceStatus
  bookingEnabled: boolean
  portalVisible: boolean
  displayOrder: number
  active: boolean
}

interface ResourceSummaryResponse {
  code: string
  typeCode: string
  name: string
  status: ResourceStatus
  bookingEnabled: boolean
  portalVisible: boolean
  active: boolean
}

interface ResourceTypeResponse {
  code: string
  name: string
  description: string | null
  active: boolean
}

interface ResourceGroupResponse {
  code: string
  name: string
  description: string | null
  portalVisible: boolean
  active: boolean
}

interface ResourcePolicyResponse {
  code: string
  name: string
  description: string | null
  minBookingDurationMinutes: number
  maxBookingDurationMinutes: number
  minBookingNoticeMinutes: number
  cancellationNoticeMinutes: number | null
  allowCancellation: boolean
  active: boolean
}

interface AmenityResponse {
  code: string
  name: string
  description: string | null
  active: boolean
}

interface ResourceAvailabilityResponse {
  id: number
  resourceCode: string
  startedAt: string   // "2026-04-20T09:00:00"
  endedAt: string
  slotDurationMinutes: number
  totalCapacity: number
  remainingCapacity: number
  available: boolean
  active: boolean
}

interface ResourceAvailabilityWindowResponse {
  resourceCode: string
  startedAt: string
  endedAt: string
  durationMinutes: number
  remainingCapacity: number
  slotCount: number
}

interface ResourceClosureResponse {
  id: number
  resourceCode: string
  startedAt: string
  endedAt: string
  active: boolean
}

interface ResourcePricingRuleResponse {
  id: number
  resourceCode: string
  bookingUnit: ResourceBookingUnit
  price: number
  active: boolean
}
```

---

## Composants UI recommandes

- `ResourceStatusBadge` — badge couleur selon le statut (vert=ACTIVE, orange=MAINTENANCE, rouge=OUT_OF_SERVICE, gris=ARCHIVED)
- `ResourceCard` — carte de ressource avec photo, capacite, equipements, disponibilite en temps reel
- `ResourceFormPage` — formulaire de creation/edition avec selects pour type, groupe, politique
- `AvailabilityCalendar` — vue calendrier des creneaux disponibles (vert=libre, rouge=occupe, gris=fermeture)
- `AvailabilityTimeline` — vue timeline par jour avec fenetres libres et reservations
- `AmenityChipList` — liste de chips pour les equipements associes a une ressource
- `PricingRuleTable` — tableau des tarifs par unite de reservation
- `ClosureCard` — carte fermeture avec dates et motif
- `PolicySummaryCard` — resume de la politique de reservation (durees, delais, annulation)
- `ResourceCapacityIndicator` — indicateur circulaire ou barre de capacite
- `ResourceSearchBar` — barre de recherche avec filtres (type, groupe, statut, capacite min)

---

## Bonnes pratiques UI

- afficher toujours la capacite totale et la capacite restante lors de la selection d'une ressource
- mettre en evidence visuellement les ressources non reservables (`bookingEnabled=false`) dans la liste
- masquer les ressources avec `portalVisible=false` dans les vues client, les afficher dans le backoffice admin avec un badge "non visible"
- distinguer clairement le statut operationnel (`status`) de l'etat actif (`active`)
- afficher les equipements d'une ressource sous forme de chips ou icones dans la fiche detail
- dans le formulaire de creation, pré-remplir `bookingEnabled=true` et `portalVisible=true` par defaut
- afficher les durees en heures/minutes plutot qu'en minutes brutes (ex: "2h30" au lieu de "150 minutes")
- sur la page de recherche, afficher les fenetres de disponibilite directement dans les resultats (appel `remaining`)
- bloquer la selection d'une ressource avec `status=MAINTENANCE` ou `bookingEnabled=false` dans le flow de reservation
- afficher un message clair si aucune disponibilite n'est trouvee avec un lien vers la gestion des creneaux (admin)

---

## Proposition de structure d'ecrans

### Cote client / portail

- `ResourceCatalogPage` — liste des ressources visibles (`portalVisible=true`, `status=ACTIVE`), avec filtres type/groupe/capacite
- `ResourceDetailPage` — fiche detail d'une ressource avec photos, equipements, tarifs, fenetres de disponibilite
- `ResourceBookingPage` — selection de creneau et formulaire de reservation (integrate avec le module booking)

### Cote operateur / gestionnaire

- `ResourceDashboardPage` — tableau de bord des ressources (statuts, taux d'occupation, alertes)
- `ResourceListPage` — liste complete avec filtres avances (statut, type, groupe, disponibilite, capacite)
- `ResourceDetailAdminPage` — fiche complete admin avec onglets : infos, equipements, tarifs, creneaux, fermetures
- `ResourceFormPage` — formulaire de creation/edition (infos generales, classification, options)
- `ResourceAvailabilityPage` — gestion des creneaux de disponibilite par ressource (vue calendrier + formulaire ajout)
- `ResourceClosurePage` — gestion des fermetures ponctuelles par ressource
- `ResourcePricingPage` — gestion des regles de tarification par ressource

### Cote administration / backoffice

- `AdminResourceTypePage` — CRUD des types de ressources
- `AdminResourceGroupPage` — CRUD des groupes de ressources
- `AdminResourcePolicyPage` — CRUD des politiques de reservation
- `AdminAmenitiesPage` — CRUD des equipements globaux
- `AdminResourceBulkPage` — gestion en masse (activation/desactivation, changement de statut)

---

## Priorite de construction frontend

Ordre recommande :

1. `AdminResourceTypePage` + `AdminResourceGroupPage` — prerequis pour creer des ressources
2. `AdminResourcePolicyPage` — prerequis pour contraindre les reservations
3. `AdminAmenitiesPage` — prerequis pour enrichir les fiches ressources
4. `ResourceFormPage` + `ResourceListPage` — creation et gestion de base des ressources
5. `ResourceDetailAdminPage` (onglet infos + equipements)
6. `ResourcePricingPage` — configuration des tarifs
7. `ResourceAvailabilityPage` — generation et gestion des creneaux
8. `ResourceClosurePage` — gestion des fermetures
9. `ResourceCatalogPage` — portail client (apres stabilisation des donnees)
10. `ResourceDetailPage` — fiche client avec disponibilites en temps reel
11. `ResourceDashboardPage` — tableau de bord avec metriques
12. `ResourceBookingPage` — integration complete avec le module booking
