# Guide Frontend/API - Module Customer

## Vue d'ensemble

Ce document regroupe les endpoints du module `customer` utilisables par le frontend.

Base URL :

- `/sni/api/v1`

Le module `customer` sert a :

- creer un customer
- lire un customer par `customerId`
- retrouver un customer par email
- lister les customers en vue detail ou vue legere
- alimenter des selects/autocomplete
- mettre a jour ou changer le statut d'un customer
- supprimer un customer

---

## Champs generes par le backend

Ne pas demander ces champs dans les formulaires de creation :

- `customerId`
- `status` au moment de la creation
- `createdAt`

Le backend genere un `customerId` de type `CUS-...`.

---

## Enums utiles au frontend

### CustomerType

- `PERSON`
- `COMPANY`

### CustomerStatus

- `ACTIVE`
- `PENDING`
- `INACTIVE`
- `SUSPENDED`
- `ARCHIVED`

---

## Format de pagination

Les endpoints pagines retournent un `PaginatedResponse<T>` :

```json
{
  "data": [
    {
      "customerId": "CUS-COM-202604-00000003",
      "type": "COMPANY",
      "companyName": "Acme SARL",
      "email": "contact@acme.com",
      "phone": "670000000",
      "status": "ACTIVE",
      "createdAt": "2026-04-26T10:15:30"
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
    "hasPrevious": false,
    "sort": {
      "sorted": true,
      "unsorted": false,
      "direction": "DESC",
      "properties": [
        "createdAt"
      ]
    }
  }
}
```

Parametres standard :

- `page=0`
- `size=20`
- `sort=createdAt,desc`

---

## Payload customer

### Body de creation / mise a jour

`POST /customers`

`PATCH /customers/{customerId}`

```json
{
  "type": "COMPANY",
  "firstname": null,
  "lastname": null,
  "companyName": "Acme SARL",
  "email": "contact@acme.com",
  "billingEmail": "billing@acme.com",
  "phone": "670000000",
  "whatsappPhone": "670000000",
  "address": {
    "streetNumber": "12",
    "streetName": "Rue Exemple",
    "district": "Centre",
    "city": "Douala",
    "countryCode": "CMR"
  }
}
```

Regles importantes :

- `type` est obligatoire
- si `type=PERSON`, `firstname` et `lastname` doivent etre renseignes
- si `type=COMPANY`, `companyName` doit etre renseigne
- `email` est obligatoire
- `billingEmail` est optionnel
- `phone` est obligatoire
- `whatsappPhone` est optionnel
- `address` est optionnelle

### Response detail

```json
{
  "customerId": "CUS-COM-202604-00000003",
  "type": "COMPANY",
  "firstname": null,
  "lastname": null,
  "companyName": "Acme SARL",
  "email": "contact@acme.com",
  "billingEmail": "billing@acme.com",
  "phone": "670000000",
  "whatsappPhone": "670000000",
  "address": {
    "streetNumber": "12",
    "streetName": "Rue Exemple",
    "district": "Centre",
    "city": "Douala",
    "country": "Cameroon",
    "fullAddress": "12 Rue Exemple, Centre, Douala, Cameroon"
  },
  "status": "ACTIVE",
  "createdAt": "2026-04-26T10:15:30"
}
```

### Response summary

```json
{
  "id": 3,
  "customerId": "CUS-COM-202604-00000003",
  "displayName": "Acme SARL",
  "email": "contact@acme.com",
  "phone": "670000000",
  "status": "ACTIVE"
}
```

---

## Endpoints disponibles

### 1. Creer un customer

`POST /customers`

Usage frontend :

- formulaire admin/backoffice de creation customer
- creation d'un client entreprise ou particulier

Retour :

- `201 Created`
- body : `CustomerResponse`

---

### 2. Lire un customer par customerId

`GET /customers/{customerId}`

Exemple :

```http
GET /sni/api/v1/customers/CUS-COM-202604-00000003
```

Retour :

- `200 OK`
- body : `CustomerResponse`

---

### 3. Lire un customer par email

`GET /customers/by-email?email=contact@acme.com`

Retour :

- `200 OK`
- body : `CustomerResponse`

---

### 4. Lister les customers

`GET /customers`

Query params :

- `summary` : optionnel, `false` par defaut
- `page` : optionnel
- `size` : optionnel
- `sort` : optionnel

Comportement :

- `summary=false` retourne `PaginatedResponse<CustomerResponse>`
- `summary=true` retourne `PaginatedResponse<CustomerSummaryresponse>`

Exemples :

```http
GET /sni/api/v1/customers?page=0&size=20
```

```http
GET /sni/api/v1/customers?summary=true&page=0&size=20&sort=createdAt,desc
```

Usage frontend :

- tableau detail : `summary=false`
- autocomplete/select/tableau leger : `summary=true`

---

### 5. Lister les customers en summary

`GET /customers/summary`

Retour :

- `200 OK`
- body : `PaginatedResponse<CustomerSummaryresponse>`

Usage frontend :

- ecran liste leger quand on veut un endpoint explicite plutot que `summary=true`

---

### 6. Charger toutes les summaries customer

`GET /customers/summary/all`

Retour :

- `200 OK`
- body : `List<CustomerSummaryresponse>`

Usage frontend :

- prechargement d'un select
- cache local
- petit catalogue customer cote admin

Attention :

- endpoint non pagine, a utiliser seulement si le volume reste raisonnable

---

### 7. Recherche basique customer

`GET /customers/search/basic?query=acme`

`GET /customers/search/basic?query=acme&type=COMPANY`

Query params :

- `query` : obligatoire
- `type` : optionnel, `PERSON` ou `COMPANY`

Retour :

- `200 OK`
- body : `List<CustomerSummaryresponse>`

Usage frontend :

- autocomplete
- barre de recherche rapide
- rattachement d'un member a un customer existant

---

### 8. Mettre a jour un customer

`PATCH /customers/{customerId}`

Body :

- meme structure que `POST /customers`

Retour :

- `204 No Content`

Usage frontend :

- edition complete du customer

---

### 9. Changer le statut d'un customer

`PATCH /customers/{customerId}/status`

Body :

```json
{
  "status": "SUSPENDED"
}
```

Valeurs supportees :

- `ACTIVE`
- `PENDING`
- `INACTIVE`
- `SUSPENDED`
- `ARCHIVED`

Retour :

- `204 No Content`

Usage frontend :

- action admin de suspension
- reactivation
- archivage logique par statut

---

### 10. Supprimer un customer

`DELETE /customers/{customerId}`

Retour :

- `204 No Content`

Usage frontend :

- action destructive reservee au backoffice

---

## Recommandations frontend

- utiliser `customerId` comme identifiant fonctionnel dans l'UI
- utiliser les endpoints `summary` pour les selects et la recherche rapide
- utiliser `GET /customers/{customerId}` avant un ecran d'edition detaille
- ne pas envoyer `customerId`, `status` ou `createdAt` dans les formulaires de creation
