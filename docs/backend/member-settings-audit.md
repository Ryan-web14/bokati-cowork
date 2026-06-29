# Member Settings Et Settings Audit Admin

Ce document decrit les ecrans admin/frontend pour les nouveaux modules `member settings` et `settings audit`.

## Objectif

- permettre au back-office de consulter et corriger les preferences d’un member
- tracer tous les changements de settings
- rendre les preferences de notification et de confidentialite pilotables

## Ecran Member Settings

### Route UI recommandee

- `/admin/members/:memberId/settings`

### Donnees a charger

- `GET /sni/api/v1/members/{memberId}/settings`
- `GET /sni/api/v1/members/{memberId}/settings/notifications`

### Sections d’ecran

- bloc `Preferences generales`
  - langue
  - fuseau horaire
- bloc `Visibilite profil`
  - afficher le nom
  - afficher email
  - afficher telephone
- bloc `Facturation`
  - type d’entite
  - societe de facturation
  - email de facturation
  - adresse de facturation
- bloc `Wallet`
  - auto topup active
  - seuil auto topup
  - montant auto topup
  - limite journaliere
  - limite hebdomadaire
- bloc `Notifications`
  - matrice `eventType x channel`
  - switch `enabled`

### Actions UI

- bouton `Enregistrer les settings`
- switch instantane sur chaque preference de notification
- historique des changements accessible via l’ecran audit

### Requetes de mise a jour

- `PATCH /sni/api/v1/members/{memberId}/settings?reason=...`
- `PATCH /sni/api/v1/members/{memberId}/settings/notifications?reason=...`

### UX recommandee

- formulaire en sections
- bouton sticky `Enregistrer`
- indication claire des champs modifies
- message de succes discret apres sauvegarde

## Ecran Settings Audit

### Route UI recommandee

- `/admin/settings-audit`

### Endpoint

- `GET /sni/api/v1/admin/settings-audit-logs`

### Filtres disponibles

- `settingKey`
- `changedBy`
- `changedFrom`
- `changedTo`
- `page`
- `size`
- `sort`

### Colonnes recommandees

- date de changement
- cle de setting
- ancienne valeur
- nouvelle valeur
- utilisateur ayant modifie
- raison

### Interactions utiles

- filtre par membre via `changedBy` si l’admin agit en son nom
- filtre date debut / fin
- recherche par prefixe de `settingKey`
- tiroir de detail pour valeurs longues

## Ecran Role / Permission Back-Office

### Routes UI recommandees

- `/admin/security/roles`
- `/admin/security/permissions`
- `/admin/users/:userId/roles`

### Endpoints utiles

- `GET /sni/api/v1/admin/roles`
- `POST /sni/api/v1/admin/roles`
- `PUT /sni/api/v1/admin/roles/{name}`
- `PATCH /sni/api/v1/admin/roles/{name}/activate`
- `PATCH /sni/api/v1/admin/roles/{name}/deactivate`
- `DELETE /sni/api/v1/admin/roles/{name}`

- `GET /sni/api/v1/admin/permissions`
- `POST /sni/api/v1/admin/permissions`
- `PUT /sni/api/v1/admin/permissions/{name}`
- `PATCH /sni/api/v1/admin/permissions/{name}/activate`
- `PATCH /sni/api/v1/admin/permissions/{name}/deactivate`
- `DELETE /sni/api/v1/admin/permissions/{name}`

- `GET /sni/api/v1/admin/users/{userId}/roles`
- `POST /sni/api/v1/admin/users/{userId}/roles`
- `DELETE /sni/api/v1/admin/users/{userId}/roles/{roleId}`

- `GET /sni/api/v1/admin/roles/{roleId}/permissions`
- `PATCH /sni/api/v1/admin/roles/{roleId}/permissions`
- `DELETE /sni/api/v1/admin/roles/{roleId}/permissions`

### Presentation recommandee

- liste des roles a gauche
- detail du role a droite
- onglet `Permissions`
- onglet `Utilisateurs assignes`
- ecran permissions avec filtre `activeOnly` et `systemOnly`

## Points d’attention frontend

- utiliser `Idempotency-Key` sur les actions d’assignation et de creation
- afficher les erreurs de validation role/permission telles quelles
- pour les permissions d’un role, preferer une UX de remplacement complet avec checklist
- toujours demander confirmation avant suppression ou desactivation
