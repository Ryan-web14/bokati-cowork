# Guide Frontend/API - Module Member

## Vue d'ensemble

Ce document regroupe les endpoints du module `member` utilisables par le frontend.

Base URL :

- `/sni/api/v1`

Le module `member` couvre deux usages frontend :

- backoffice/admin
- portail member authentifie

Il permet de :

- creer un member depuis le portail
- creer un member depuis le backoffice
- lire, lister et rechercher les members
- modifier les informations du member
- changer le statut ou l'acces portail
- transferer un member vers un autre customer
- lire et mettre a jour le profil member
- finaliser l'activation d'un member portail

---

## Champs generes par le backend

Ne pas demander ces champs dans les formulaires de creation :

- `memberId`
- `customerId` si le backend cree automatiquement un customer de type `PERSON`
- `userId`
- `status`
- `portalAccess`
- `createdAt`
- `updatedAt`

Le backend genere un `memberId` de type `MBR-...`.

---

## Enums utiles au frontend

### MemberStatus

- `ACTIVE`
- `PENDING`
- `UNDER_REVIEW`
- `PENDING_CORRECTION`
- `REJECTED`
- `INACTIVE`
- `SUSPENDED`
- `ARCHIVED`

### CustomerType utilise a la creation

- `PERSON`
- `COMPANY`

---

## Format de pagination

Les listes retournent un `PaginatedResponse<T>` :

```json
{
  "data": [
    {
      "memberId": "MBR-202604-00000004",
      "customerId": "CUS-PER-202604-00000007",
      "fullName": "John Doe",
      "email": "john@acme.com",
      "phone": "670000000",
      "whatsapp_phone": "670000000",
      "status": "PENDING",
      "portalAccess": true
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalPages": 1,
    "totalElements": 1,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false
  }
}
```

Parametres standard :

- `page=0`
- `size=20`
- `sort=createdAt,desc`

---

## Payloads member

### Creation member

`POST /members`

`POST /members/admin/create`

```json
{
  "existingCustomerId": "CUS-COM-202604-00000003",
  "customerType": "COMPANY",
  "firstname": "John",
  "lastname": "Doe",
  "email": "john@acme.com",
  "phone": "670000000",
  "whatsappPhone": "670000000",
  "password": "TempPass123!",
  "companyName": "Acme SARL",
  "billingEmail": "billing@acme.com",
  "address": {
    "streetNumber": "12",
    "streetName": "Rue Exemple",
    "district": "Centre",
    "city": "Douala",
    "countryCode": "CMR"
  },
  "generatePassword": false
}
```

Regles importantes :

- `firstname`, `lastname`, `email` et `phone` sont requis
- `POST /members` exige `password`
- `POST /members/admin/create` exige soit `generatePassword=true`, soit un `password`
- si `existingCustomerId` est vide, le backend cree un customer de type `PERSON`
- si `customerType=COMPANY`, `existingCustomerId` doit pointer vers un customer `COMPANY`
- `companyName` seul ne suffit pas pour creer un nouveau customer `COMPANY`

### Response detail

```json
{
  "memberId": "MBR-202604-00000004",
  "customerId": "CUS-COM-202604-00000003",
  "userId": "USR-202604-00000011",
  "firstname": "John",
  "lastname": "Doe",
  "fullname": "John Doe",
  "email": "john@acme.com",
  "phone": "670000000",
  "whatsappPhone": "670000000",
  "status": "PENDING",
  "portalAccess": true,
  "createdAt": "2026-04-26T10:35:00Z",
  "updatedAt": "2026-04-26T10:35:00Z"
}
```

### Response summary

```json
{
  "memberId": "MBR-202604-00000004",
  "customerId": "CUS-COM-202604-00000003",
  "fullName": "John Doe",
  "email": "john@acme.com",
  "phone": "670000000",
  "whatsapp_phone": "670000000",
  "status": "PENDING",
  "portalAccess": true
}
```

### Update member

`PATCH /members/{id}`

```json
{
  "firstname": "John",
  "lastname": "Doe",
  "email": "john@acme.com",
  "phone": "670000000",
  "whatsappPhone": "670000000"
}
```

### Update status

`PATCH /members/{id}/status`

```json
{
  "status": "ACTIVE"
}
```

### Transfer customer

`PATCH /members/{id}/transfer-customer`

```json
{
  "customerId": "CUS-COM-202604-00000005"
}
```

### Update profile

`PUT /members/{id}/profile`

`PUT /members/me/profile`

```json
{
  "birthDate": "1995-10-18",
  "jobTitle": "Product Designer",
  "companyRole": "Team Lead",
  "address": "12 Rue Exemple",
  "city": "Douala",
  "country": "Cameroon"
}
```

### Profile response

```json
{
  "memberId": "MBR-202604-00000004",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@acme.com",
  "phone": "670000000",
  "whatsappPhone": "670000000",
  "birthDate": "1995-10-18",
  "jobTitle": "Product Designer",
  "companyRole": "Team Lead",
  "address": "12 Rue Exemple",
  "city": "Douala",
  "country": "Cameroon"
}
```

---

## Endpoints backoffice/admin

### 1. Creer un member par admin

`POST /members/admin/create`

Retour :

- `201 Created`
- body : `MemberResponse`

Usage :

- creation manuelle par le backoffice
- ajout d'un employe a un customer entreprise existant

---

### 2. Lire un member

`GET /members/{id}`

Retour :

- `200 OK`
- body : `MemberResponse`

---

### 3. Lire un member par email

`GET /members/by-email?email=user@acme.com`

Retour :

- `200 OK`
- body : `MemberResponse`

---

### 4. Lister les members

`GET /members`

Query params :

- `customerId` : optionnel
- `summary` : optionnel, `false` par defaut
- `page`
- `size`
- `sort`

Comportement :

- sans `customerId` et avec `summary=false` : `PaginatedResponse<MemberResponse>`
- sans `customerId` et avec `summary=true` : `PaginatedResponse<MemberSummaryResponse>`
- avec `customerId` : `PaginatedResponse<MemberSummaryResponse>`

Exemples :

```http
GET /sni/api/v1/members?page=0&size=20
```

```http
GET /sni/api/v1/members?summary=true&page=0&size=20
```

```http
GET /sni/api/v1/members?customerId=CUS-COM-202604-00000003&page=0&size=20
```

Usage :

- tableau admin
- vue des members d'un customer

---

### 5. Lister les summaries member

`GET /members/summary`

Retour :

- `200 OK`
- body : `PaginatedResponse<MemberSummaryResponse>`

---

### 6. Charger toutes les summaries member

`GET /members/summary/all`

Retour :

- `200 OK`
- body : `List<MemberSummaryResponse>`

Attention :

- endpoint non pagine, a reserver aux petits volumes ou au prechargement de reference

---

### 7. Recherche basique member

`GET /members/search/basic?query=john`

Retour :

- `200 OK`
- body : `List<MemberSummaryResponse>`

Usage :

- autocomplete
- recherche instantanee

---

### 8. Recherche avancee member

`POST /members/search`

Body optionnel :

```json
{
  "customerId": "CUS-COM-202604-00000003",
  "memberId": "MBR-202604-00000004",
  "code": "MBR-202604-00000004",
  "firstname": "John",
  "lastname": "Doe",
  "email": "john@acme.com",
  "phone": "670000000",
  "status": "ACTIVE",
  "portalAccess": true
}
```

Retour :

- `200 OK`
- body : `PaginatedResponse<MemberSummaryResponse>`

Usage :

- ecran admin avec filtres avances

---

### 9. Mettre a jour un member

`PATCH /members/{id}`

Retour :

- `204 No Content`

---

### 10. Changer le statut d'un member

`PATCH /members/{id}/status`

Valeurs supportees :

- `ACTIVE`
- `PENDING`
- `UNDER_REVIEW`
- `PENDING_CORRECTION`
- `REJECTED`
- `INACTIVE`
- `SUSPENDED`
- `ARCHIVED`

Retour :

- `204 No Content`

---

### 11. Activer l'acces portail

`PATCH /members/{id}/portal-access/enable`

Retour :

- `204 No Content`

Effet metier actuel :

- `portalAccess` passe a `true`
- si le member etait `INACTIVE` ou `SUSPENDED`, le statut repasse a `ACTIVE`

---

### 12. Desactiver l'acces portail

`PATCH /members/{id}/portal-access/disable`

Retour :

- `204 No Content`

Effet metier actuel :

- `portalAccess` passe a `false`
- le statut passe a `INACTIVE`

---

### 13. Transferer un member vers un autre customer

`PATCH /members/{id}/transfer-customer`

Retour :

- `200 OK`
- body : `MemberResponse`

---

### 14. Lire le profil d'un member

`GET /members/{id}/profile`

Retour :

- `200 OK`
- body : `MemberProfileResponse`

Note :

- si aucun profil detaille n'existe encore, le backend renvoie quand meme les informations de base du member

---

### 15. Mettre a jour le profil d'un member

`PUT /members/{id}/profile`

Retour :

- `200 OK`
- body : `MemberProfileResponse`

---

### 16. Archiver un member

`DELETE /members/{id}`

Retour :

- `204 No Content`

Effet metier actuel :

- `status` passe a `ARCHIVED`
- `deleted` passe a `true`

---

## Endpoints portail member

### 17. Inscription member depuis le portail

`POST /members`

Retour :

- `201 Created`
- body : `MemberResponse`

Usage :

- inscription d'un member cote portail

Notes :

- `password` est obligatoire
- si `existingCustomerId` est absent, le backend cree un customer `PERSON`

---

### 18. Lire mon profil

`GET /members/me/profile`

Retour :

- `200 OK`
- body : `MemberProfileResponse`

Usage :

- page "Mon profil" du portail member

---

### 19. Mettre a jour mon profil

`PUT /members/me/profile`

Retour :

- `200 OK`
- body : `MemberProfileResponse`

---

### 20. Finaliser l'activation portail

`POST /members/portal/complete`

Body :

```json
{
  "email": "john@acme.com"
}
```

Retour :

- `200 OK`
- body : `MemberResponse`

Usage :

- finalisation d'un onboarding portail apres verification/activation

---

## Recommandations frontend

- utiliser `memberId` comme identifiant fonctionnel principal
- preferer les endpoints `summary` pour les listes legeres et les selects
- utiliser `GET /members/{id}` pour le detail complet
- utiliser `GET /members/{id}/profile` ou `GET /members/me/profile` pour les ecrans de profil
- ne pas supposer qu'un nouveau member entreprise peut creer un nouveau customer `COMPANY` via `companyName` seul : il faut un `existingCustomerId`
