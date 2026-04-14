# Recherche Customer / Member

## But

Ce document explique au developpeur frontend comment utiliser les endpoints de recherche et de listing pour:

- rechercher des `customers`
- rechercher des `members`
- alimenter une autocomplete
- construire les ecrans liste
- brancher les filtres, la pagination et le tri

Le contenu decrit le comportement backend actuel expose par l'application.

---

## Base URL

Les routes utilisent le prefixe versionne:

- `/sni/api/v1`

Endpoints concernes:

- `GET /sni/api/v1/customers`
- `GET /sni/api/v1/customers/search/basic`
- `GET /sni/api/v1/members`
- `GET /sni/api/v1/members/search/basic`
- `POST /sni/api/v1/members/search`

---

## Vue d'ensemble

### Customer

Le module `customer` expose deux usages principaux cote frontend:

- listing pagine
- recherche basique pour autocomplete ou recherche rapide

### Member

Le module `member` expose trois usages principaux:

- listing pagine
- recherche basique pour autocomplete ou recherche rapide
- recherche avancee multicritere pour les ecrans riches de filtre

---

## Endpoints disponibles

### Customer

Liste:

- `GET /sni/api/v1/customers`

Recherche basique:

- `GET /sni/api/v1/customers/search/basic?query=...`
- `GET /sni/api/v1/customers/search/basic?query=...&type=COMPANY`

### Member

Liste:

- `GET /sni/api/v1/members`
- `GET /sni/api/v1/members?customerId=...`

Recherche basique:

- `GET /sni/api/v1/members/search/basic?query=...`

Recherche avancee:

- `POST /sni/api/v1/members/search`

---

## Customer: liste paginee

### Endpoint

```http
GET /sni/api/v1/customers
```

### Query params

- `summary`: optionnel, `false` par defaut
- `page`: optionnel, index de page commence a `0`
- `size`: optionnel, taille de page
- `sort`: optionnel, format Spring `champ,direction`

### Tri par defaut

Si aucun tri n'est envoye:

- `createdAt,DESC`

### Comportement

Si `summary=false`:

- le backend retourne `PaginatedResponse<CustomerResponse>`

Si `summary=true`:

- le backend retourne `PaginatedResponse<CustomerSummaryresponse>`

### Exemples

Liste standard:

```http
GET /sni/api/v1/customers?page=0&size=20
```

Liste resume:

```http
GET /sni/api/v1/customers?summary=true&page=0&size=20&sort=createdAt,desc
```

### Reponse type avec `summary=true`

```json
{
  "data": [
    {
      "id": 1,
      "customerId": "CUS-0001",
      "displayName": "ACME SARL",
      "email": "contact@acme.com",
      "phone": "670000000",
      "status": "ACTIVE"
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
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

### Reponse type avec `summary=false`

```json
{
  "data": [
    {
      "customerId": "CUS-0001",
      "type": "COMPANY",
      "firstname": null,
      "lastname": null,
      "companyName": "ACME SARL",
      "email": "contact@acme.com",
      "billingEmail": "billing@acme.com",
      "phone": "670000000",
      "whatsappPhone": "670000000",
      "address": {
        "streetNumber": "12",
        "streetName": "Rue Exemple",
        "district": "Centre",
        "city": "Douala",
        "country": "Cameroon"
      },
      "status": "ACTIVE",
      "createdAt": "2026-04-01T10:00:00"
    }
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
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

### Usage frontend recommande

Pour un ecran liste customer:

- utiliser `summary=true` si l'ecran est un tableau leger
- utiliser `summary=false` si l'ecran affiche les details de contact complets
- piloter la pagination avec `page`, `size`, `sort`
- recharger la liste a chaque changement de filtre/pagination

---

## Customer: recherche basique

### Endpoint

```http
GET /sni/api/v1/customers/search/basic
```

### Query params

- `query`: obligatoire
- `type`: optionnel

### Valeurs supportees pour `type`

- `PERSON`
- `COMPANY`

### Exemples

Recherche libre:

```http
GET /sni/api/v1/customers/search/basic?query=jean
```

Recherche entreprise:

```http
GET /sni/api/v1/customers/search/basic?query=acme&type=COMPANY
```

### Reponse

Le backend retourne une liste non paginee de `CustomerSummaryresponse`.

```json
[
  {
    "id": 12,
    "customerId": "CUS-0012",
    "displayName": "ACME SARL",
    "email": "contact@acme.com",
    "phone": "670000000",
    "status": "ACTIVE"
  }
]
```

### Cas d'usage frontend

Cet endpoint est adapte pour:

- autocomplete customer
- select searchable
- recherche rapide dans une barre globale
- selection d'un customer avant creation d'un member

### Recommandations UI

- debounce de `250ms` a `400ms`
- ne pas lancer la recherche si la saisie est vide
- lancer idealement a partir de `2` ou `3` caracteres
- afficher `displayName` comme libelle principal
- afficher `customerId`, `email` ou `phone` comme meta-informations

### Gestion des erreurs

Si `query` est vide:

- le backend renvoie une erreur

Si `type` est invalide:

- le backend renvoie une `BadRequestException`

Implication frontend:

- restreindre le filtre `type` a une liste fermee
- ne jamais envoyer une valeur libre pour `type`

---

## Member: liste paginee

### Endpoint

```http
GET /sni/api/v1/members
```

### Query params

- `customerId`: optionnel
- `page`: optionnel
- `size`: optionnel
- `sort`: optionnel

### Tri par defaut

- `createdAt,DESC`

### Comportement

Sans `customerId`:

- retourne la liste paginee globale des members

Avec `customerId`:

- retourne les members du customer cible

### Exemples

Tous les members:

```http
GET /sni/api/v1/members?page=0&size=20
```

Members d'un customer:

```http
GET /sni/api/v1/members?customerId=CUS-0001&page=0&size=20
```

### Reponse type

Le backend retourne un `PaginatedResponse`, avec des items `MemberResponse` ou `MemberSummaryResponse` selon l'implementation de service utilisee par cet ecran.

Exemple exploitable frontend:

```json
{
  "data": [
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
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
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

### Usage frontend recommande

Pour un ecran liste member:

- utiliser `customerId` si la page est contextualisee par customer
- sinon utiliser `POST /members/search` si vous avez besoin de filtres avances
- conserver l'etat de pagination dans l'URL si possible

---

## Member: recherche basique

### Endpoint

```http
GET /sni/api/v1/members/search/basic
```

### Query params

- `query`: obligatoire

### Exemple

```http
GET /sni/api/v1/members/search/basic?query=alice
```

### Reponse

Liste non paginee de `MemberSummaryResponse`.

```json
[
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
]
```

### Cas d'usage frontend

Cet endpoint convient pour:

- autocomplete member
- champ de recherche rapide
- selection de member dans un drawer ou une modale

### Recommandations UI

- debounce de `250ms` a `400ms`
- ne pas appeler l'API si le champ est vide
- afficher `fullName` en premier
- afficher `memberId` et `customerId` en sous-texte

---

## Member: recherche avancee

### Endpoint

```http
POST /sni/api/v1/members/search
```

### Body JSON

Le body est optionnel. Vous pouvez envoyer un objet vide ou seulement les champs filtres utiles.

Champs supportes:

- `customerId`
- `memberId`
- `code`
- `firstname`
- `lastname`
- `email`
- `phone`
- `status`
- `portalAccess`

### Exemple de body minimal

```json
{
  "firstname": "Alice"
}
```

### Exemple de body complet

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

### Pagination et tri

Comme pour les endpoints listes:

- `page`
- `size`
- `sort`

Exemple:

```http
POST /sni/api/v1/members/search?page=0&size=20&sort=createdAt,desc
Content-Type: application/json

{
  "customerId": "CUS-0001",
  "status": "ACTIVE",
  "portalAccess": true
}
```

### Reponse

`PaginatedResponse<MemberSummaryResponse>`

```json
{
  "data": [
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
  ],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
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

### Quand utiliser `POST /members/search`

Utiliser cet endpoint pour:

- un ecran liste avec plusieurs filtres simultanes
- un back-office admin avec panneau de recherche
- une grille filtrable avec statut et acces portail

Eviter de l'utiliser pour:

- une simple autocomplete
- une saisie instantanee courte

Dans ces cas, preferer `GET /members/search/basic`

---

## Structures de reponse utiles

### CustomerSummaryresponse

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

```json
{
  "customerId": "CUS-0001",
  "type": "COMPANY",
  "firstname": null,
  "lastname": null,
  "companyName": "ACME SARL",
  "email": "contact@acme.com",
  "billingEmail": "billing@acme.com",
  "phone": "670000000",
  "whatsappPhone": "670000000",
  "address": {
    "streetNumber": "12",
    "streetName": "Rue Exemple",
    "district": "Centre",
    "city": "Douala",
    "country": "Cameroon"
  },
  "status": "ACTIVE",
  "createdAt": "2026-04-01T10:00:00"
}
```

### MemberSummaryResponse

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

```json
{
  "memberId": "MEM-0001",
  "customerId": "CUS-0001",
  "userId": "USR-0001",
  "firstname": "Alice",
  "lastname": "Doe",
  "fullname": "Alice Doe",
  "email": "alice@acme.com",
  "phone": "680000000",
  "whatsappPhone": "680000000",
  "status": "ACTIVE",
  "portalAccess": true,
  "createdAt": "2026-04-01T10:00:00Z",
  "updatedAt": "2026-04-01T10:05:00Z"
}
```

### PaginatedResponse

Tous les endpoints pagines renvoient:

```json
{
  "data": [],
  "pageable": {
    "page": 0,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0,
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

### Point d'attention sur `sort.direction`

Le code backend qui construit `PageInfo.SortInfo` contient une logique incoherente:

- `direction` peut etre `null` meme si `sorted=true`

Implication frontend:

- ne pas dependre strictement de `pageable.sort.direction`
- considerer `properties` comme plus fiable que `direction`

---

## Recommandations d'implementation frontend

### 1. Autocomplete customer

Usage recommande:

- endpoint: `GET /customers/search/basic`
- debounce: `300ms`
- min chars: `2`

Affichage suggere:

- ligne 1: `displayName`
- ligne 2: `customerId • email • phone`

Pour la recherche entreprise:

- toujours envoyer `type=COMPANY`

### 2. Autocomplete member

Usage recommande:

- endpoint: `GET /members/search/basic`
- debounce: `300ms`
- min chars: `2`

Affichage suggere:

- ligne 1: `fullName`
- ligne 2: `memberId • customerId • email`

### 3. Ecran liste customer

Approche simple:

- appeler `GET /customers?summary=true`
- stocker `page`, `size`, `sort`
- rafraichir sur changement de pagination

Filtres possibles cote UI:

- texte libre local
- type si l'ecran combine recherche basique et liste

### 4. Ecran liste member

Approche simple:

- si un seul filtre `customerId`, utiliser `GET /members?customerId=...`
- si plusieurs filtres, utiliser `POST /members/search`

Filtres utiles a exposer:

- `customerId`
- `memberId`
- `firstname`
- `lastname`
- `email`
- `phone`
- `status`
- `portalAccess`

### 5. Serialisation des filtres

Recommandation:

- retirer les champs vides avant envoi
- ne pas envoyer de `null` inutilement
- envoyer `status` uniquement via une liste fermee cote UI

---

## Exemples d'integration frontend

### Exemple fetch: recherche customer

```ts
async function searchCustomers(query: string, type?: "PERSON" | "COMPANY") {
  const params = new URLSearchParams({ query });
  if (type) params.set("type", type);

  const response = await fetch(`/sni/api/v1/customers/search/basic?${params.toString()}`);
  if (!response.ok) {
    throw new Error("Customer search failed");
  }

  return response.json();
}
```

### Exemple fetch: liste customer paginee

```ts
async function listCustomers(page = 0, size = 20, summary = true) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    summary: String(summary)
  });

  const response = await fetch(`/sni/api/v1/customers?${params.toString()}`);
  if (!response.ok) {
    throw new Error("Customer list failed");
  }

  return response.json();
}
```

### Exemple fetch: recherche member basique

```ts
async function searchMembers(query: string) {
  const params = new URLSearchParams({ query });

  const response = await fetch(`/sni/api/v1/members/search/basic?${params.toString()}`);
  if (!response.ok) {
    throw new Error("Member search failed");
  }

  return response.json();
}
```

### Exemple fetch: recherche member avancee

```ts
type MemberSearchPayload = {
  customerId?: string;
  memberId?: string;
  code?: string;
  firstname?: string;
  lastname?: string;
  email?: string;
  phone?: string;
  status?: string;
  portalAccess?: boolean;
};

async function searchMembersAdvanced(payload: MemberSearchPayload, page = 0, size = 20) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
    sort: "createdAt,desc"
  });

  const response = await fetch(`/sni/api/v1/members/search?${params.toString()}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(payload)
  });

  if (!response.ok) {
    throw new Error("Advanced member search failed");
  }

  return response.json();
}
```

---

## Cas d'usage frontend recommandes

### 1. Recherche entreprise avant creation d'un employe

Flux recommande:

1. l'utilisateur saisit le nom de l'entreprise
2. le frontend appelle `GET /customers/search/basic?query=...&type=COMPANY`
3. l'utilisateur selectionne un resultat
4. le frontend stocke `customerId`
5. les ecrans member reutilisent cette valeur

### 2. Liste des membres d'un customer

Flux recommande:

1. ouvrir un detail customer
2. appeler `GET /members?customerId={customerId}`
3. afficher la table des members associes

### 3. Back-office member avec filtres

Flux recommande:

1. formulaire de filtres
2. construction d'un payload partiel
3. appel `POST /members/search`
4. mise a jour de la table paginee

---

## Gestion des erreurs cote frontend

### Erreurs a anticiper

- query vide pour les endpoints de recherche basique
- type customer invalide
- customerId inexistant sur la liste member filtree
- status invalide sur la recherche avancee

### Bonnes pratiques UI

- valider les enums cote frontend
- ne pas envoyer de requete si le champ de recherche est vide
- afficher un etat `Aucun resultat`
- dissocier `Aucun resultat` et `Erreur serveur`

---

## Resume

- `GET /customers` sert au listing pagine des customers
- `GET /customers/search/basic` sert a la recherche rapide et aux autocomplete
- `GET /members` sert au listing pagine simple des members
- `GET /members/search/basic` sert a la recherche rapide et aux autocomplete member
- `POST /members/search` sert aux filtres avances multicriteres
- le frontend doit utiliser les endpoints basiques pour l'autocomplete et l'endpoint `POST /members/search` pour les vraies vues de recherche riche
