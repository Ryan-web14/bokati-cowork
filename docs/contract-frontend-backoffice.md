# Module Contract

## Objectif

Le module `contract` permet au frontend et au back-office de :
- creer un contrat metier persistant
- gerer ses parties signataires et beneficiaires
- suivre son workflow de validation et de signature
- generer un brouillon PDF a partir d'un template HTML
- rattacher le document signe final au contrat

## Endpoints

Base path : `/sni/api/v1/contracts`

### Templates et generation

- `GET /templates`
- `POST /preview`
- `POST /generate`
- `POST /{contractCode}/generate-draft`

### CRUD metier

- `POST /`
- `GET /{contractCode}`
- `GET /`
- `PUT /{contractCode}`
- `DELETE /{contractCode}`

### Workflow

- `PATCH /{contractCode}/status`
- `PATCH /{contractCode}/activate`
- `PATCH /{contractCode}/suspend`
- `PATCH /{contractCode}/terminate`
- `PATCH /{contractCode}/cancel`
- `PATCH /{contractCode}/signed-document`

## Creation d'un contrat

`POST /sni/api/v1/contracts`

Payload :

```json
{
  "title": "Contrat de domiciliation SNI",
  "description": "Contrat principal du client",
  "templateCode": "business-service-agreement",
  "ownerType": "BUSINESS",
  "ownerCode": "BUS-00001",
  "businessCode": "BUS-00001",
  "renewalType": "FIXED_TERM",
  "effectiveDate": "2026-04-10",
  "startDate": "2026-04-10",
  "endDate": "2027-04-09",
  "createdBy": 1,
  "parties": [
    {
      "partyType": "BUSINESS",
      "partyCode": "BUS-00001",
      "displayName": "SNI Congo",
      "email": "contact@test.sni-cg.com",
      "phone": "+242060000000",
      "role": "SIGNATORY",
      "signOrder": 1,
      "mustSign": true
    }
  ]
}
```

Regles importantes :
- `ownerType` supporte `MEMBER`, `CUSTOMER`, `BUSINESS`
- il faut au moins un signataire
- `endDate` doit etre apres `startDate`
- `effectiveDate` ne doit pas etre apres `startDate`
- le contrat est cree en statut `DRAFT`

## Listing

`GET /sni/api/v1/contracts`

Filtres :
- `ownerType`
- `ownerCode`
- `status`
- `businessCode`
- `templateCode`
- pagination Spring standard `page`, `size`, `sort`

Exemple :

```http
GET /sni/api/v1/contracts?ownerType=BUSINESS&ownerCode=BUS-00001&status=ACTIVE&page=0&size=20&sort=createdAt,desc
```

## Workflow recommande

### Parcours back-office

1. creer le contrat
2. modifier les parties si necessaire
3. generer le brouillon PDF
4. passer en `UNDER_REVIEW` ou `AWAITING_SIGNATURE`
5. rattacher le document signe final
6. activer le contrat

### Transitions utiles

- `DRAFT -> GENERATED`
- `DRAFT -> UNDER_REVIEW`
- `DRAFT -> CANCELLED`
- `GENERATED -> UNDER_REVIEW`
- `GENERATED -> AWAITING_SIGNATURE`
- `AWAITING_SIGNATURE -> SIGNED`
- `SIGNED -> ACTIVE`
- `ACTIVE -> SUSPENDED`
- `ACTIVE -> TERMINATED`
- `ACTIVE -> EXPIRED`

Les transitions invalides sont rejetees par le backend.

## Generation de brouillon

### Brouillon libre

`POST /sni/api/v1/contracts/generate`

Utilisable quand on veut juste produire un PDF sans creer un contrat persistant.

### Brouillon a partir d'un contrat stocke

`POST /sni/api/v1/contracts/{contractCode}/generate-draft`

Payload :

```json
{
  "uploadedBy": 1
}
```

Comportement :
- le backend reprend les informations du contrat
- genere le PDF
- cree un document `CONTRACT_DRAFT`
- rattache le `draftDocumentCode` au contrat
- passe le contrat en `GENERATED`

## Rattachement du document signe

`PATCH /sni/api/v1/contracts/{contractCode}/signed-document`

Payload :

```json
{
  "documentCode": "DOC-202604-000123"
}
```

Comportement :
- renseigne `signedDocumentCode`
- renseigne `signedAt`
- passe le contrat en `SIGNED`

## Ecrans frontend recommandes

### 1. Liste des contrats

Colonnes :
- code contrat
- titre
- proprietaire
- business
- template
- statut
- date debut
- date fin
- document brouillon
- document signe

Actions ligne :
- voir detail
- generer brouillon
- changer statut
- activer
- suspendre
- terminer
- annuler

### 2. Formulaire creation/edition

Blocs :
- informations generales
- proprietaire du contrat
- entreprise rattachee
- dates
- type de renouvellement
- parties et signataires

### 3. Detail contrat

Sections :
- resume
- parties
- timeline de statut
- documents lies
- actions workflow

### 4. Ecran generation / preview

Affichage :
- preview HTML
- choix du template
- variables injectees
- bouton de generation PDF

## Bonnes pratiques frontend

- toujours afficher les transitions disponibles selon le statut courant
- ne pas laisser l'utilisateur saisir manuellement `ownerId`
- utiliser `ownerCode` et `businessCode` depuis les modules existants
- afficher clairement si le contrat a un `draftDocumentCode` ou un `signedDocumentCode`
- demander un motif pour `suspend`, `terminate` et `cancel`

## Retour API utile

`ContractResponse` contient deja :
- `contractCode`
- `ownerType`
- `ownerCode`
- `businessCode`
- `status`
- `renewalType`
- `effectiveDate`
- `startDate`
- `endDate`
- `draftDocumentCode`
- `signedDocumentCode`
- `signedAt`
- `activatedAt`
- `terminatedAt`
- `terminationReason`
- `parties`

Le frontend peut donc piloter tout le workflow sans appel supplementaire au module document hors consultation du fichier lui-meme.
