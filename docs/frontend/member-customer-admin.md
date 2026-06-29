# Module Admin Customer / Member

## But

Ce document decrit les endpoints backend utiles au developpement du back-office admin pour les modules `customer` et `member`.

Il couvre:

- les endpoints de listing
- les endpoints de recherche
- les endpoints `summary`
- les endpoints de details
- les endpoints d'edition
- les ajouts recents sur le profil member et les actions admin

Base URL:

- `/sni/api/v1`

---

## Vue d'ensemble

### Customer

Le module `customer` permet cote admin:

- creation
- consultation detail
- consultation par email
- listing pagine complet
- listing resume
- listing resume complet
- recherche basique
- mise a jour
- suppression logique

### Member

Le module `member` permet cote admin:

- creation standard
- creation par admin
- consultation detail
- consultation par email
- listing pagine complet
- listing resume
- listing resume par customer
- listing resume complet
- recherche basique
- recherche avancee multicritere
- mise a jour
- changement de statut
- activation / desactivation acces portail
- transfert vers un autre customer
- consultation / edition du profil
- completion onboarding portail
- archivage

---

## Endpoints Customer

### Creation

```http
POST /sni/api/v1/customers
```

Retour:

- `201 Created`
- body: `CustomerResponse`

### Detail

```http
GET /sni/api/v1/customers/{customerId}
```

Retour:

- `200 OK`
- body: `CustomerResponse`

### Recherche par email

```http
GET /sni/api/v1/customers/by-email?email=contact@acme.com
```

Retour:

- `200 OK`
- body: `CustomerResponse`

### Liste paginee

```http
GET /sni/api/v1/customers
```

Query params:

- `summary`: optionnel, `false` par defaut
- `page`
- `size`
- `sort`

Comportement:

- `summary=false` -> `PaginatedResponse<CustomerResponse>`
- `summary=true` -> `PaginatedResponse<CustomerSummaryresponse>`

Exemples:

```http
GET /sni/api/v1/customers?page=0&size=20
```

```http
GET /sni/api/v1/customers?summary=true&page=0&size=20&sort=createdAt,desc
```

### Liste summary explicite

```http
GET /sni/api/v1/customers/summary
```

Retour:

- `PaginatedResponse<CustomerSummaryresponse>`

### Liste summary complete

```http
GET /sni/api/v1/customers/summary/all
```

Retour:

- `List<CustomerSummaryresponse>`

Usage recommande:

- prechargement d'un select
- cache local admin
- ecrans de selection simple

### Recherche basique

```http
GET /sni/api/v1/customers/search/basic?query=acme
GET /sni/api/v1/customers/search/basic?query=acme&type=COMPANY
```

Query params:

- `query`: obligatoire
- `type`: optionnel

Valeurs supportees pour `type`:

- `PERSON`
- `COMPANY`

Retour:

- `List<CustomerSummaryresponse>`

### Mise a jour

```http
PATCH /sni/api/v1/customers/{customerId}
```

Retour:

- `204 No Content`

### Suppression

```http
DELETE /sni/api/v1/customers/{customerId}
```

Retour:

- `204 No Content`

---

## Endpoints Member

### Creation standard

```http
POST /sni/api/v1/members
```

Retour:

- `201 Created`
- body: `MemberResponse`

### Creation par admin

```http
POST /sni/api/v1/members/admin/create
```

Retour:

- `201 Created`
- body: `MemberResponse`

Usage admin:

- creation manuelle d'un member par le back-office
- onboarding supervise

### Detail

```http
GET /sni/api/v1/members/{id}
```

Retour:

- `200 OK`
- body: `MemberResponse`

### Recherche par email

```http
GET /sni/api/v1/members/by-email?email=user@acme.com
```

Retour:

- `200 OK`
- body: `MemberResponse`

### Liste paginee

```http
GET /sni/api/v1/members
```

Query params:

- `customerId`: optionnel
- `summary`: optionnel, `false` par defaut
- `page`
- `size`
- `sort`

Comportement:

- sans `customerId` et `summary=false` -> liste detaillee
- sans `customerId` et `summary=true` -> liste resumee
- avec `customerId` -> liste resumee filtree par customer

Exemples:

```http
GET /sni/api/v1/members?page=0&size=20
```

```http
GET /sni/api/v1/members?summary=true&page=0&size=20
```

```http
GET /sni/api/v1/members?customerId=CUS-0001&page=0&size=20
```

### Liste summary explicite

```http
GET /sni/api/v1/members/summary
```

Retour:

- `PaginatedResponse<MemberSummaryResponse>`

### Liste summary complete

```http
GET /sni/api/v1/members/summary/all
```

Retour:

- `List<MemberSummaryResponse>`

### Recherche basique

```http
GET /sni/api/v1/members/search/basic?query=alice
```

Retour:

- `List<MemberSummaryResponse>`

### Recherche avancee

```http
POST /sni/api/v1/members/search?page=0&size=20&sort=createdAt,desc
Content-Type: application/json
```

Body supporte:

```json
{
  "customerId": "CUS-0001",
  "memberId": "MEM-0001",
  "code": "M001",
  "firstname": "Alice",
  "lastname": "Doe",
  "email": "alice@acme.com",
  "phone": "680000000",
  "status": "ACTIVE",
  "portalAccess": true
}
```

Retour:

- `PaginatedResponse<MemberSummaryResponse>`

Usage admin:

- grilles de recherche avancee
- filtres multicriteres
- ecrans support / operations

### Mise a jour

```http
PATCH /sni/api/v1/members/{id}
```

Retour:

- `204 No Content`

### Changement de statut

```http
PATCH /sni/api/v1/members/{id}/status
```

Retour:

- `204 No Content`

Usage:

- passage manuel de statut
- traitement administratif

### Profil member

Consultation:

```http
GET /sni/api/v1/members/{id}/profile
```

Edition:

```http
PUT /sni/api/v1/members/{id}/profile
```

Retour:

- `200 OK`
- body: `MemberProfileResponse`

Ajouts importants:

- lecture d'un profil vide enrichi des infos member si le profil n'existe pas encore
- mise a jour propre du profil via un endpoint dedie

### Acces portail

Activation:

```http
PATCH /sni/api/v1/members/{id}/portal-access/enable
```

Desactivation:

```http
PATCH /sni/api/v1/members/{id}/portal-access/disable
```

Retour:

- `204 No Content`

### Transfert vers un autre customer

```http
PATCH /sni/api/v1/members/{id}/transfer-customer
```

Retour:

- `200 OK`
- body: `MemberResponse`

Usage admin:

- correction d'affectation
- rattachement a une autre entreprise / structure

### Completion portail

```http
POST /sni/api/v1/members/portal/complete
```

Retour:

- `200 OK`
- body: `MemberResponse`

Usage admin:

- finaliser une activation portail
- accompagner un member en onboarding

### Archivage

```http
DELETE /sni/api/v1/members/{id}
```

Retour:

- `204 No Content`

Comportement:

- suppression logique / archivage

---

## DTOs utiles cote frontend

### CustomerSummaryresponse

Usage:

- autocomplete
- dropdown admin
- tableaux legers

Exemple:

```json
{
  "id": 1,
  "customerId": "CUS-0001",
  "displayName": "ACME SARL",
  "email": "contact@acme.com",
  "phone": "670000000",
  "status": "ACTIVE"
}
```

### CustomerResponse

Usage:

- vue detail
- edition
- drawer detail admin

### MemberSummaryResponse

Usage:

- listing rapide
- resultats de recherche
- autocomplete

Exemple:

```json
{
  "memberId": "MEM-0001",
  "customerId": "CUS-0001",
  "fullName": "Alice Doe",
  "email": "alice@acme.com",
  "phone": "680000000",
  "whatsapp_phone": "680000000",
  "status": "ACTIVE",
  "portalAccess": true
}
```

### MemberResponse

Usage:

- fiche member admin
- edition complete
- retour apres creation / transfert

### MemberProfileResponse

Usage:

- ecran profil
- onglet informations personnelles / professionnelles

Contient notamment:

- informations personnelles
- `jobTitle`
- `companyRole`
- `profilePictureUrl`
- adresse / ville / pays

---

## Recommandations frontend admin

### Customer admin

Approche recommandee:

- tableau principal avec `GET /customers/summary`
- detail a l'ouverture via `GET /customers/{customerId}`
- recherche rapide via `GET /customers/search/basic`

### Member admin

Approche recommandee:

- tableau principal avec `POST /members/search`
- detail via `GET /members/{id}`
- edition profil via `GET/PUT /members/{id}/profile`
- operations secondaires via endpoints dedies:
  - statut
  - acces portail
  - transfert customer
  - archivage

### Quand utiliser `summary/all`

A utiliser seulement pour:

- listes relativement petites
- prechargement de select
- ecrans de liaison ou pickers

Eviter pour:

- gros tableaux admin
- pages avec pagination serveur

### Recherche

Bon usage:

- `search/basic` pour autocomplete
- `POST /members/search` pour les vrais filtres admin
- `summary` pour les tableaux legers

---

## Exemple de flux admin

### Creation member par admin

1. rechercher ou selectionner un customer
2. appeler `POST /members/admin/create`
3. afficher la fiche retournee
4. si necessaire, ouvrir le profil avec `GET /members/{id}/profile`

### Gestion du profil member

1. ouvrir la fiche member
2. charger `GET /members/{id}/profile`
3. afficher formulaire profil
4. sauvegarder avec `PUT /members/{id}/profile`

### Support admin

1. rechercher un member via `POST /members/search`
2. consulter `GET /members/{id}`
3. corriger statut / acces portail / customer de rattachement

---

## Resume des ajouts recents

Ajouts visibles cote admin:

- `GET /customers/by-email`
- `GET /customers/summary`
- `GET /customers/summary/all`
- `GET /members/by-email`
- `GET /members?summary=true`
- `GET /members/summary`
- `GET /members/summary/all`
- `GET /members/{id}/profile`
- `PUT /members/{id}/profile`
- `PATCH /members/{id}/portal-access/enable`
- `PATCH /members/{id}/portal-access/disable`
- `PATCH /members/{id}/transfer-customer`
- `POST /members/admin/create`
- `POST /members/portal/complete`
- `DELETE /members/{id}`

Ce document complete la doc de recherche existante en la replaçant dans un usage back-office admin plus large.
