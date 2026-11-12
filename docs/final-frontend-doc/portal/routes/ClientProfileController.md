# ClientProfileController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [portal](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-client-me"></a>
### GET /sni/api/v1/client/me

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientProfileResponse`
- Modèles : [ClientProfileResponse](#m-ClientProfileResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/controller/ClientProfileController.java`, méthode `getProfile`

<a id="r-get-sni-api-v1-client-me-onboarding-status"></a>
### GET /sni/api/v1/client/me/onboarding-status

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `OnboardingStatusResponse`
- Modèles : [OnboardingStatusResponse](#m-OnboardingStatusResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/controller/ClientProfileController.java`, méthode `onboardingStatus`

<a id="r-get-sni-api-v1-client-profile"></a>
### GET /sni/api/v1/client/profile

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `ClientProfileResponse`
- Modèles : [ClientProfileResponse](#m-ClientProfileResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/controller/ClientProfileController.java`, méthode `getProfile`

<a id="r-put-sni-api-v1-client-profile"></a>
### PUT /sni/api/v1/client/profile

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Corps JSON (obligatoire) : `ClientUpdateProfileRequest`
- Réponse : `ClientProfileResponse`
- Modèles : [ClientProfileResponse](#m-ClientProfileResponse), [ClientUpdateProfileRequest](#m-ClientUpdateProfileRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/controller/ClientProfileController.java`, méthode `updateProfile`

<a id="r-post-sni-api-v1-client-profile-change-password"></a>
### POST /sni/api/v1/client/profile/change-password

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/portal/profile/controller/ClientProfileController.java`, méthode `requestPasswordChange`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ClientProfileResponse"></a>
#### ClientProfileResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberId` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `fullName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `status` | texte |  |  |
| `avatarUrl` | texte |  |  |
| `portalActivatedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-OnboardingStatusResponse"></a>
#### OnboardingStatusResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `emailVerified` | booléen |  |  |
| `profileComplete` | booléen |  |  |
| `kycStatus` | texte |  |  |
| `kycCompletionPercent` | entier |  |  |
| `gracePeriodEndsAt` | date-heure ISO 8601 UTC |  |  |
| `gracePeriodActive` | booléen |  |  |
| `gracePeriodDaysRemaining` | entier int64 ⚠ |  |  |
| `portalFullyActive` | booléen |  |  |
| `nextStep` | texte |  |  |
| `restrictedActions` | texte[] |  |  |

<a id="m-ClientUpdateProfileRequest"></a>
#### ClientUpdateProfileRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `firstname` | texte |  | longueur min `0`, longueur max `350` |
| `lastname` | texte |  | longueur min `0`, longueur max `350` |
| `phone` | texte |  | longueur min `0`, longueur max `30` |
| `whatsappPhone` | texte |  | longueur min `0`, longueur max `30` |
