# MemberSettingsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [parametres](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-members-memberid-settings"></a>
### GET /sni/api/v1/members/{memberId}/settings

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `MemberSettingsResponse`
- Modèles : [MemberSettingsResponse](#m-MemberSettingsResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/settings/userSettings/controller/MemberSettingsController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-members-memberid-settings"></a>
### PATCH /sni/api/v1/members/{memberId}/settings

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_SETTINGS_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateMemberSettingsRequest`
- Réponse : `MemberSettingsResponse`
- Modèles : [MemberSettingsResponse](#m-MemberSettingsResponse), [UpdateMemberSettingsRequest](#m-UpdateMemberSettingsRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/settings/userSettings/controller/MemberSettingsController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

<a id="r-get-sni-api-v1-members-memberid-settings-notifications"></a>
### GET /sni/api/v1/members/{memberId}/settings/notifications

- Accès : back-office : rôle d'administration et droit `CLIENT:READ`
- Réponse : `List<NotificationPreferenceResponse>`
- Modèles : [NotificationPreferenceResponse](#m-NotificationPreferenceResponse)
- Code : `src/main/java/com/sni/bokaticowork/core/settings/userSettings/controller/MemberSettingsController.java`, méthode `notifications`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-members-memberid-settings-notifications"></a>
### PATCH /sni/api/v1/members/{memberId}/settings/notifications

- Accès : back-office : rôle d'administration et droit `CLIENT:UPDATE`
- En-tête `Idempotency-Key` : conseillé (opération `MEMBER_NOTIFICATION_PREFERENCE_UPDATE`, voir le README racine)
- Corps JSON (obligatoire) : `UpdateNotificationPreferenceRequest`
- Réponse : `List<NotificationPreferenceResponse>`
- Modèles : [NotificationPreferenceResponse](#m-NotificationPreferenceResponse), [UpdateNotificationPreferenceRequest](#m-UpdateNotificationPreferenceRequest)
- Code : `src/main/java/com/sni/bokaticowork/core/settings/userSettings/controller/MemberSettingsController.java`, méthode `updateNotification`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |
| `reason` | requête | `String` |  |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-MemberSettingsResponse"></a>
#### MemberSettingsResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `langage` | texte |  |  |
| `timeZone` | texte |  |  |
| `showNameOnDisplay` | booléen |  |  |
| `showEmail` | booléen |  |  |
| `showPhone` | booléen |  |  |
| `billingPref` | [BillingPreferencesResponse](#m-BillingPreferencesResponse) |  |  |
| `walletPref` | [WalletPreferencesResponse](#m-WalletPreferencesResponse) |  |  |
| `notificationPref` | [NotificationPreferenceResponse](#m-NotificationPreferenceResponse)[] |  |  |

<a id="m-UpdateMemberSettingsRequest"></a>
#### UpdateMemberSettingsRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `langage` | texte |  |  |
| `timeZone` | texte |  |  |
| `showNameOnDisplay` | booléen |  |  |
| `showEmail` | booléen |  |  |
| `showPhone` | booléen |  |  |
| `billingPref` | [BillingPreferences](#m-BillingPreferences) |  |  |
| `walletPref` | [WalletPreferencesRequest](#m-WalletPreferencesRequest) |  |  |
| `notificationPref` | [NotificationPreferences](#m-NotificationPreferences)[] |  |  |

<a id="m-NotificationPreferenceResponse"></a>
#### NotificationPreferenceResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `preferenceName` | texte |  |  |
| `preferenceDescription` | texte |  |  |
| `eventType` | texte |  |  |
| `channel` | texte |  |  |
| `enabled` | booléen |  |  |

<a id="m-UpdateNotificationPreferenceRequest"></a>
#### UpdateNotificationPreferenceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `eventType` | enum : `BOOKING_CONFIRMED`, `BOOKING_CANCELLED`, `BOOKING_REMINDER`, `BOOKING_FAILED`, `INVOICE_ISSUED`, `PAYMENT_RECEIVED`, `PAYMENT_FAILED` |  |  |
| `channel` | enum : `SMS`, `WHATSAPP`, `IN_APP`, `PUSH`, `EMAIL` |  |  |
| `enabled` | booléen |  |  |

<a id="m-BillingPreferencesResponse"></a>
#### BillingPreferencesResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `billingEntityType` | texte |  |  |
| `billingCompanyName` | texte |  | longueur min `0`, longueur max `255` |
| `billingEmail` | texte |  | longueur min `0`, longueur max `255` |
| `billlingAddress` | texte |  | longueur min `0`, longueur max `255` |

<a id="m-WalletPreferencesResponse"></a>
#### WalletPreferencesResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `autoTopupEnabled` | booléen |  |  |
| `autoTopupThreshold` | entier |  |  |
| `autoTopupAmount` | entier |  |  |
| `spendingDailyLimit` | entier |  |  |
| `spendingWeeklyLimit` | entier |  |  |

<a id="m-BillingPreferences"></a>
#### BillingPreferences

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `billingEntityType` | enum : `COMPANY`, `PERSONAL` |  |  |
| `billingCompanyName` | texte |  | longueur min `0`, longueur max `255` |
| `billingEmail` | texte |  | longueur min `0`, longueur max `255` |
| `billlingAddress` | texte |  | longueur min `0`, longueur max `255` |

<a id="m-WalletPreferencesRequest"></a>
#### WalletPreferencesRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `autoTopupEnabled` | booléen |  |  |
| `autoTopupThreshold` | entier |  | min `0` |
| `autoTopupAmount` | entier |  | min `0` |
| `spendingDailyLimit` | entier |  | min `0` |
| `spendingWeeklyLimit` | entier |  | min `0` |

<a id="m-NotificationPreferences"></a>
#### NotificationPreferences

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `member` | [Member](#m-Member) |  |  |
| `eventType` | enum : `BOOKING_CONFIRMED`, `BOOKING_CANCELLED`, `BOOKING_REMINDER`, `BOOKING_FAILED`, `INVOICE_ISSUED`, `PAYMENT_RECEIVED`, `PAYMENT_FAILED` |  |  |
| `notificationChannel` | enum : `SMS`, `WHATSAPP`, `IN_APP`, `PUSH`, `EMAIL` |  |  |
| `enabled` | booléen |  |  |
| `preferenceName` | texte |  |  |
| `preferenceDescription` | texte |  |  |

<a id="m-Member"></a>
#### Member

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `memberId` | texte |  |  |
| `customer` | [Customer](#m-Customer) |  |  |
| `user` | [Users](#m-Users) |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `status` | enum : `ACTIVE`, `PENDING`, `UNDER_REVIEW`, `PENDING_CORRECTION`, `REJECTED`, `INACTIVE`, `SUSPENDED`, `ARCHIVED` |  |  |
| `portalAccess` | booléen |  |  |
| `portalActivatedAt` | date-heure ISO 8601 UTC |  |  |
| `kycGracePeriodEndAt` | date-heure ISO 8601 UTC |  |  |
| `avatarUrl` | texte |  |  |
| `createByAdmin` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `deleted` | booléen |  |  |
| `displayName` | texte |  |  |

<a id="m-Customer"></a>
#### Customer

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `customerId` | texte |  |  |
| `type` | enum : `PERSON`, `COMPANY` |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `companyName` | texte |  |  |
| `email` | texte |  |  |
| `billingEmail` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `address` | [Address](#m-Address) |  |  |
| `status` | enum : `ACTIVE`, `PENDING`, `INACTIVE`, `SUSPENDED`, `ARCHIVED` |  |  |
| `businessEntity` | [BusinessEntity](#m-BusinessEntity) |  |  |
| `paymentTermsDays` | entier |  |  |
| `creditLimit` | nombre décimal |  |  |
| `billingAddress` | [Address](#m-Address) |  |  |
| `website` | texte |  |  |
| `defaultInvoiceLinkMode` | enum : `AUTOMATIC`, `ON_CHOICE` |  |  |
| `blockedReason` | texte |  |  |
| `blockedBy` | texte |  |  |
| `blockedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `deleted` | booléen |  |  |
| `member` | booléen |  |  |

<a id="m-Users"></a>
#### Users

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `userId` | texte |  |  |
| `email` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `passwordHash` | texte |  |  |
| `lastLogin` | date-heure locale sans fuseau |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |
| `deleted` | booléen |  |  |
| `isAccountExpired` | booléen |  |  |
| `isAccountLocked` | booléen |  |  |
| `isAccountEnabled` | booléen |  |  |
| `failedLoginAttempts` | entier |  |  |
| `pendingEmail` | texte |  |  |
| `emailVerifiedAt` | date-heure ISO 8601 UTC |  |  |
| `emailVerifiedBy` | texte |  |  |
| `roleUsers` | [RoleUser](#m-RoleUser)[] |  |  |

<a id="m-Address"></a>
#### Address

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `country` | [Country](#m-Country) |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-BusinessEntity"></a>
#### BusinessEntity

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `legalForm` | texte | oui | longueur min `1` |
| `niuNumber` | texte |  | longueur min `0`, longueur max `18` |
| `rccmNumber` | texte |  |  |
| `organizationType` | enum : `SOCIETE`, `ETABLISSEMENT`, `ENTREPRENEUR_INDIVIDUEL`, `PROFESSION_LIBERALE`, `ASSOCIATION`, `ONG`, `INSTITUTION_PUBLIQUE` |  |  |
| `legalRepresentativeName` | texte |  |  |
| `legalRepresentativeTitle` | texte |  |  |
| `vatRegime` | enum : `SUBJECT`, `EXEMPT` |  |  |
| `vatExemptionNote` | texte |  |  |
| `taxId` | texte |  |  |
| `activity` | texte |  |  |
| `address` | [Address](#m-Address) |  |  |
| `phone` | texte |  |  |
| `email` | string (email) |  |  |
| `baseCurrency` | [Currency](#m-Currency) |  |  |
| `status` | enum : `ACTIVE`, `INACTIVE`, `DELETED`, `BLOCKED` |  |  |
| `deleted` | booléen |  |  |
| `operator` | booléen |  |  |
| `created_by` | texte |  |  |
| `updated_by` | texte |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-RoleUser"></a>
#### RoleUser

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | [RoleUserId](#m-RoleUserId) |  |  |
| `role` | [Role](#m-Role) |  |  |
| `users` | [Users](#m-Users) |  |  |
| `assignedBy` | texte |  |  |
| `assignedAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-Country"></a>
#### Country

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte |  |  |
| `countryCode` | texte |  | longueur min `2`, longueur max `3` |
| `defaultCurrency` | [Currency](#m-Currency) |  |  |
| `phoneCode` | texte |  |  |
| `isOhadaMember` | booléen |  |  |
| `deleted` | booléen |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-Currency"></a>
#### Currency

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `currencyCode` | texte |  | longueur min `3`, longueur max `3` |
| `currencyName` | texte |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |

<a id="m-RoleUserId"></a>
#### RoleUserId

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userId` | entier int64 ⚠ |  |  |
| `roleId` | entier int64 ⚠ |  |  |

<a id="m-Role"></a>
#### Role

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte | oui | longueur min `0`, longueur max `250`, motif `^[A-Z_]+$` |
| `displayName` | texte |  |  |
| `description` | texte |  |  |
| `isSystemRole` | booléen |  |  |
| `version` | entier |  |  |
| `createdAt` | date-heure locale sans fuseau |  |  |
| `updatedAt` | date-heure locale sans fuseau |  |  |
| `isActive` | booléen |  |  |
| `rolePermissions` | [RolePermission](#m-RolePermission)[] |  |  |

<a id="m-RolePermission"></a>
#### RolePermission

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `role` | [Role](#m-Role) |  |  |
| `permission` | [Permission](#m-Permission) |  |  |
| `created_by` | texte |  |  |
| `isActive` | booléen |  |  |
| `id` | [RolePermissionId](#m-RolePermissionId) |  |  |

<a id="m-Permission"></a>
#### Permission

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte | oui | longueur min `0`, longueur max `100`, motif `^[A-Z_]+$` |
| `displayName` | texte | oui | longueur min `0`, longueur max `100` |
| `module` | texte | oui | longueur min `0`, longueur max `50` |
| `action` | texte | oui | longueur min `0`, longueur max `60` |
| `isSystemPermission` | booléen |  |  |
| `isActive` | booléen |  |  |
| `fullPermissionName` | texte |  |  |

<a id="m-RolePermissionId"></a>
#### RolePermissionId

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `roleId` | entier int64 ⚠ |  |  |
| `permissionId` | entier int64 ⚠ |  |  |
