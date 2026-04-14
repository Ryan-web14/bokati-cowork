# Admin Users

## Objectif

L'API `admin users` est concue pour etre majoritairement automatisee :
- creation rapide d'un compte staff
- mot de passe genere par defaut
- role `STAFF` applique par defaut si aucun role n'est fourni
- peu de saisie manuelle cote back-office

Base path : `/sni/api/v1/admin/users`

## Endpoints

- `POST /`
- `GET /`
- `GET /{id}`
- `GET /by-email`
- `PUT /{id}`
- `PATCH /{id}/activate`
- `PATCH /{id}/deactivate`
- `PATCH /{id}/password/reset`
- `DELETE /{id}`

## Creation intuitive

`POST /sni/api/v1/admin/users`

Payload minimal recommande :

```json
{
  "email": "staff@test.sni-cg.com"
}
```

Comportement :
- le backend genere le mot de passe
- le backend assigne `STAFF`
- le compte est active

Payload complet :

```json
{
  "email": "manager@test.sni-cg.com",
  "generatePassword": false,
  "password": "Temp@1234",
  "roleNames": ["MANAGER", "STAFF"]
}
```

## Listing

`GET /sni/api/v1/admin/users`

Filtres :
- `email`
- `enabled`
- `locked`
- `deleted`
- pagination standard `page`, `size`, `sort`

Exemple :

```http
GET /sni/api/v1/admin/users?enabled=true&page=0&size=20&sort=createdAt,desc
```

## Reponse utile

`AdminUserResponse` contient :
- `id`
- `userId`
- `email`
- `accountEnabled`
- `accountLocked`
- `accountExpired`
- `deleted`
- `failedLoginAttempts`
- `lastLogin`
- `createdAt`
- `updatedAt`
- `roleNames`
- `generatedPassword`

`generatedPassword` est renseigne uniquement sur creation ou reset automatique.

## Ecrans back-office recommandes

### 1. Liste users

Colonnes :
- user id
- email
- roles
- enabled
- locked
- deleted
- last login

Actions :
- voir detail
- activer
- desactiver
- reset password
- archiver

### 2. Formulaire creation rapide

Champs minimum :
- email
- toggle `mot de passe automatique`
- roles optionnels

Comportement UX :
- pre-cocher `mot de passe automatique`
- preselectionner `STAFF`
- afficher le mot de passe genere une seule fois apres creation

### 3. Detail user

Sections :
- informations compte
- roles
- securite
- dernier acces

Actions :
- changer email
- changer roles
- activer / desactiver
- reset password

## Bonnes pratiques frontend

- privilegier la creation minimale avec email seul
- n'exiger un mot de passe manuel que pour des cas specifiques
- afficher un avertissement clair quand `generatedPassword` est retourne
- eviter de faire gerer les roles un par un si une simple liste peut etre remplacee en une fois
- utiliser les endpoints role existants pour afficher les options de roles disponibles
