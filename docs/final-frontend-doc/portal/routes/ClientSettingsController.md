# ClientSettingsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-client-settings"></a>
### GET /sni/api/v1/client/settings

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `MemberSettingsResponse`
- Modèles : [MemberSettingsResponse](#m-MemberSettingsResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/controller/ClientSettingsController.java`, méthode `get`

<a id="r-patch-sni-api-v1-client-settings"></a>
### PATCH /sni/api/v1/client/settings

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `UpdateMemberSettingsRequest`
- Réponse : `MemberSettingsResponse`
- Modèles : [MemberSettingsResponse](#m-MemberSettingsResponse), [UpdateMemberSettingsRequest](#m-UpdateMemberSettingsRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/controller/ClientSettingsController.java`, méthode `update`

<a id="r-get-sni-api-v1-client-settings-notifications"></a>
### GET /sni/api/v1/client/settings/notifications

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<NotificationPreferenceResponse>`
- Modèles : [NotificationPreferenceResponse](#m-NotificationPreferenceResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/controller/ClientSettingsController.java`, méthode `notifications`

<a id="r-patch-sni-api-v1-client-settings-notifications"></a>
### PATCH /sni/api/v1/client/settings/notifications

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `UpdateNotificationPreferenceRequest`
- Réponse : `List<NotificationPreferenceResponse>`
- Modèles : [NotificationPreferenceResponse](#m-NotificationPreferenceResponse), [UpdateNotificationPreferenceRequest](#m-UpdateNotificationPreferenceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/controller/ClientSettingsController.java`, méthode `updateNotification`

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
| `notificationPref` | [UpdateNotificationPreferenceRequest](#m-UpdateNotificationPreferenceRequest)[] |  |  |

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
| `billingEmail` | texte |  | longueur min `0`, longueur max `500` |
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
| `receiptPreference` | texte |  |  |

<a id="m-BillingPreferences"></a>
#### BillingPreferences

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `billingEntityType` | enum : `COMPANY`, `PERSONAL` |  |  |
| `billingCompanyName` | texte |  | longueur min `0`, longueur max `150` |
| `billingEmail` | texte |  | longueur min `0`, longueur max `500` |
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
| `receiptPreference` | enum : `EVERY_TRANSACTION`, `INVOICE_ONLY` |  |  |
