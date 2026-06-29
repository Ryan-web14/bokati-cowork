ex# Admin Member / Customer

## But

Ce document donne au developpeur frontend les informations necessaires pour construire:

- le formulaire admin de creation d'un `member`
- le formulaire admin de creation d'un `customer`
- le select/autocomplete permettant de rattacher un `member` a un `customer` existant

Le document decrit le comportement backend actuel. Il ne contient pas de recommandations d'architecture backend.

---

## Concepts

### Customer

Le `customer` represente le client de reference.

Types disponibles:

- `PERSON`
- `COMPANY`

Un customer peut avoir plusieurs members.

### Member

Le `member` represente une personne utilisatrice du portail.

Un member:

- appartient a un seul `customer`
- est relie a un `user` pour l'authentification

### Relation

La relation metier disponible cote backend est:

- `1 customer -> N members`

Cela couvre le cas:

- une entreprise avec plusieurs employes

---

## Endpoints frontend utiles

### Customer

Creation:

- `POST /sni/api/v1/customers`

Lecture:

- `GET /sni/api/v1/customers/{customerId}`
- `GET /sni/api/v1/customers/by-email?email=...`

Liste:

- `GET /sni/api/v1/customers`
- `GET /sni/api/v1/customers?summary=true`

Recherche autocomplete:

- `GET /sni/api/v1/customers/search/basic?query=...`
- `GET /sni/api/v1/customers/search/basic?query=...&type=COMPANY`

Mise a jour:

- `PATCH /sni/api/v1/customers/{customerId}`

Suppression:

- `DELETE /sni/api/v1/customers/{customerId}`

### Member

Creation admin:

- `POST /sni/api/v1/members/admin/create`

Lecture:

- `GET /sni/api/v1/members/{id}`

Liste:

- `GET /sni/api/v1/members`
- `GET /sni/api/v1/members?customerId=...`

Recherche:

- `GET /sni/api/v1/members/search/basic?query=...`
- `POST /sni/api/v1/members/search`

Mise a jour:

- `PATCH /sni/api/v1/members/{id}`
- `PATCH /sni/api/v1/members/{id}/status`
- `PATCH /sni/api/v1/members/{id}/portal-access/enable`
- `PATCH /sni/api/v1/members/{id}/portal-access/disable`
- `PATCH /sni/api/v1/members/{id}/transfer-customer`

---

## Ecrans frontend a prevoir

## 1. Ecran liste customers

But:

- afficher les customers existants
- acceder rapidement a la creation
- servir de point d'entree pour le rattachement d'un member a un customer existant

Elements UI attendus:

- tableau ou liste avec:
  - `customerId`
  - `displayName` ou `companyName`
  - `type`
  - `email`
  - `phone`
  - `status`
- bouton `Creer un customer`
- bouton `Creer un member`
- champ de recherche
- filtre `type`

Actions utiles:

- ouvrir le detail customer
- lancer la creation d'un member a partir d'un customer existant

## 2. Ecran creation customer

But:

- creer un customer seul sans passer par la creation member

Structure simple recommandee:

- bloc `Type de customer`
- bloc `Informations principales`
- bloc `Contact`
- bloc `Facturation`
- bloc `Adresse`

Experience attendue:

- affichage dynamique selon `type=PERSON` ou `type=COMPANY`
- validation inline
- message de succes clair avec le `customerId` cree

## 3. Ecran liste members

But:

- afficher les members existants
- filtrer par customer
- acceder aux actions de gestion

Elements UI attendus:

- tableau ou liste avec:
  - `memberId`
  - `fullname`
  - `customerId`
  - `email`
  - `phone`
  - `status`
  - `portalAccess`
- filtre `customerId`
- recherche texte
- bouton `Creer un member`

Actions utiles:

- voir le detail member
- changer le statut
- activer/desactiver le portail
- transferer vers un autre customer

## 4. Ecran creation member

But:

- creer un member facilement en 1 seul parcours

Structure d'ecran recommandee:

- bloc `Informations du member`
- bloc `Rattachement customer`
- bloc `Acces portail`
- footer d'action

Ce doit etre l'ecran principal pour:

- creer un nouveau member independant
- ajouter un employe a une entreprise existante

## 5. Ecran ou drawer de transfert member -> customer

But:

- changer le customer rattache a un member existant

Structure minimale:

- rappel du member selectionne
- autocomplete customer
- bouton de confirmation

API associee:

- `PATCH /sni/api/v1/members/{id}/transfer-customer`

---

## Formulaire admin: creation customer

### Payload

```json
{
  "type": "PERSON",
  "firstname": "John",
  "lastname": "Doe",
  "companyName": "Acme",
  "email": "john@acme.com",
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

### Champs

- `type`: obligatoire
- `firstname`: requis si `type=PERSON`
- `lastname`: requis si `type=PERSON`
- `companyName`: requis si `type=COMPANY`
- `email`: obligatoire
- `billingEmail`: optionnel
- `phone`: obligatoire
- `whatsappPhone`: optionnel
- `address`: optionnel

### Règles utiles a l'UI

- `type` accepte `PERSON` ou `COMPANY`
- `email` doit etre valide
- `billingEmail` doit etre valide si renseigne
- `phone` doit etre valide
- si `type=PERSON`, `firstname` et `lastname` doivent etre saisis
- si `type=COMPANY`, `companyName` doit etre saisi

### Comportement de creation

- `customerId` est genere par le backend
- `status` est initialise a `ACTIVE`

---

## Formulaire admin: creation member

### Deux modes supportés

Le backend supporte deux modes de creation:

- creation d'un nouveau customer puis creation du member
- rattachement du member a un customer existant

### Experience UI recommandee

L'utilisateur doit choisir explicitement entre:

- `Nouveau customer`
- `Customer existant`

Ce choix doit etre visible tres tot dans l'ecran.

### Payload

```json
{
  "existingCustomerId": "CUS-0001",
  "customerType": "COMPANY",
  "firstname": "Alice",
  "lastname": "Doe",
  "email": "alice@company.com",
  "phone": "060000001",
  "whatsappPhone": "060000001",
  "password": "Secret123",
  "companyName": "ACME SARL",
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

### Champs du payload

- `existingCustomerId`: optionnel selon le mode
- `customerType`: optionnel, valeurs `PERSON` ou `COMPANY`
- `firstname`: obligatoire
- `lastname`: obligatoire
- `email`: obligatoire
- `phone`: obligatoire
- `whatsappPhone`: optionnel
- `password`: conditionnel
- `companyName`: utile pour le mode nouveau customer
- `billingEmail`: optionnel
- `address`: optionnel
- `generatePassword`: obligatoire fonctionnellement pour la logique UI

### Regles frontend a respecter

- `firstname` obligatoire
- `lastname` obligatoire
- `email` obligatoire et valide
- `phone` obligatoire et valide
- `billingEmail` valide si renseigne
- si `generatePassword=false`, `password` doit etre saisi
- si `generatePassword=true`, `password` peut etre masque ou non envoye

### Regles liees a `customerType`

Si `customerType=PERSON`:

- `existingCustomerId` peut etre vide
- le backend peut creer un nouveau customer `PERSON`

Si `customerType=COMPANY`:

- `existingCustomerId` est obligatoire
- le customer cible doit exister
- le customer cible doit etre de type `COMPANY`

Implication UI:

- si l'utilisateur est dans le cas entreprise/employe, l'ecran doit forcer le mode `Customer existant`
- il ne faut pas laisser l'utilisateur remplir un sous-formulaire entreprise a la place du rattachement

### Comportement backend

Si `existingCustomerId` est renseigne:

- le backend recupere le customer existant
- aucun nouveau customer n'est cree
- le member est rattache a ce customer

Si `existingCustomerId` est vide:

- le backend cree un nouveau customer
- puis cree le member rattache a ce nouveau customer

### Champs automatiquement generes

- `memberId`
- `status = PENDING`
- `portalAccess = true`
- `createByAdmin = true`

### Comportement sur `whatsappPhone`

Si `whatsappPhone` est vide:

- la valeur du `phone` est reprise

---

## Cas frontend: entreprise -> employe

### Flux attendu

Pour ajouter un employe a une entreprise existante:

1. chercher l'entreprise avec l'autocomplete customer
2. selectionner un customer de type `COMPANY`
3. envoyer `existingCustomerId`
4. envoyer `customerType=COMPANY`
5. envoyer les informations du member

### Endpoint de recherche conseillé

```http
GET /sni/api/v1/customers/search/basic?query=acme&type=COMPANY
```

### Donnees renvoyees par l'autocomplete

Chaque item de `CustomerSummaryresponse` contient:

- `id`
- `customerId`
- `displayName`
- `email`
- `phone`
- `status`

Cela suffit pour:

- afficher le libelle du select
- stocker `customerId`
- distinguer les clients avant selection

### Experience UI simple et intuitive

Pour ce cas, l'ecran le plus simple est:

1. rechercher l'entreprise
2. selectionner l'entreprise
3. afficher un resume de l'entreprise selectionnee
4. remplir uniquement les informations du member
5. configurer l'acces portail

Apres selection du customer entreprise, afficher au minimum:

- `customerId`
- `displayName`
- `email`
- `phone`

Cela evite les erreurs de rattachement.

---

## Transferer un member vers un autre customer

### Endpoint

- `PATCH /sni/api/v1/members/{id}/transfer-customer`

### Payload

```json
{
  "customerId": "CUS-0002"
}
```

### Usage frontend

Ce endpoint permet de changer le customer rattache a un member deja existant.

---

## Regles d'unicite a connaitre

### Email member

Lors de la creation d'un member:

- l'email doit etre unique dans `member`
- l'email doit etre unique dans `user`
- l'email n'est plus bloque parce qu'il existe deja dans `customer`

### Telephone member

Lors de la creation d'un member:

- le `phone` doit etre unique dans `member`

### Customer

Le customer a ses propres contraintes metier, mais pour le frontend du module member:

- la presence d'un email identique sur un customer n'empeche pas la creation d'un member

---

## Reponses utiles au frontend

### `MemberResponse`

```json
{
  "memberId": "MEM-0001",
  "customerId": "CUS-0001",
  "userId": "USR-0001",
  "firstname": "Alice",
  "lastname": "Doe",
  "fullname": "Alice Doe",
  "email": "alice@company.com",
  "phone": "060000001",
  "whatsappPhone": "060000001",
  "status": "PENDING",
  "portalAccess": true,
  "createdAt": "...",
  "updatedAt": "..."
}
```

### `CustomerResponse`

```json
{
  "customerId": "CUS-0001",
  "type": "COMPANY",
  "firstname": null,
  "lastname": null,
  "companyName": "ACME SARL",
  "email": "contact@acme.com",
  "billingEmail": "billing@acme.com",
  "phone": "060000000",
  "whatsappPhone": "060000000",
  "address": {
    "streetNumber": "12",
    "streetName": "Rue Exemple",
    "district": "Centre",
    "city": "Douala",
    "country": "Cameroon"
  },
  "status": "ACTIVE",
  "createdAt": "..."
}
```

---

## Structure UI minimale conseillee

### Ecran creation member

Sections:

- informations personnelles du member
- rattachement customer
- acces portail

Ordre recommande:

1. choix du mode customer
2. customer existant ou nouveau customer
3. informations personnelles du member
4. acces portail

Cet ordre reduit les erreurs parce qu'il fixe d'abord le contexte du member.

### Sous-mode rattachement customer

Deux choix UI:

- `Nouveau customer`
- `Customer existant`

Comportement attendu:

- si `Nouveau customer`
  - afficher les champs customer
- si `Customer existant`
  - afficher l'autocomplete customer
  - remplir `existingCustomerId`

Comportement visuel recommande:

- autocomplete avec debounce
- liste de resultats courte
- carte de confirmation apres selection
- possibilite de vider la selection

### Cas `COMPANY`

Si l'utilisateur cree un employe pour une entreprise:

- activer le mode `Customer existant`
- utiliser l'autocomplete filtre sur `type=COMPANY`
- envoyer `customerType=COMPANY`

Ce cas devrait idealement ressembler a:

- un champ `Entreprise`
- une recherche instantanee
- un bloc `Informations employe`
- un bloc `Acces portail`

L'utilisateur ne devrait pas avoir a se demander s'il faut creer l'entreprise depuis cet ecran.

### Ecran creation customer

Pour garder la creation intuitive, l'ecran `Creer un customer` doit etre separe de l'ecran `Creer un member`.

La logique la plus simple est:

- ecran `Creer un customer` pour creer une fiche client
- ecran `Creer un member` pour creer une personne
- mode `Customer existant` pour relier facilement les deux

---

## Résumé

- le backend supporte `1 customer -> N members`
- le create member admin supporte maintenant:
  - nouveau customer
  - customer existant
- pour une entreprise, il faut fournir `existingCustomerId` et `customerType=COMPANY`
- le frontend peut utiliser `GET /sni/api/v1/customers/search/basic?query=...&type=COMPANY` pour l'autocomplete entreprise
- l'email d'un customer n'empeche plus la creation d'un member
