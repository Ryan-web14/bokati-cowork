# AuthenticationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [securite](../README.md) · 12 route(s).

## Routes

<a id="r-post-sni-api-v1-auth-email-verify-resend"></a>
### POST /sni/api/v1/auth/email/verify/resend

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `EmailRequest`
- Réponse : `OttResponse`
- Modèles : [OttResponse](#m-OttResponse), [EmailRequest](#m-EmailRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `resendEmailVerification`

<a id="r-post-sni-api-v1-auth-login"></a>
### POST /sni/api/v1/auth/login

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `LoginRequest`
- Réponse : `LoginResponse`
- Modèles : [LoginResponse](#m-LoginResponse), [LoginRequest](#m-LoginRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `login`

<a id="r-post-sni-api-v1-auth-logout"></a>
### POST /sni/api/v1/auth/logout

- Accès : tout compte connecté
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `logout`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `Authorization` | en-tête | `String` |  |  |

<a id="r-get-sni-api-v1-auth-me"></a>
### GET /sni/api/v1/auth/me

- Accès : tout compte connecté
- Réponse : `CurrentUserResponse`
- Modèles : [CurrentUserResponse](#m-CurrentUserResponse)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `me`

<a id="r-post-sni-api-v1-auth-ott-request"></a>
### POST /sni/api/v1/auth/ott/request

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `EmailRequest`
- Réponse : `OttResponse`
- Modèles : [OttResponse](#m-OttResponse), [EmailRequest](#m-EmailRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `requestOtt`

<a id="r-post-sni-api-v1-auth-ott-validate"></a>
### POST /sni/api/v1/auth/ott/validate

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `ValidateOttRequest`
- Réponse : `LoginResponse`
- Modèles : [LoginResponse](#m-LoginResponse), [ValidateOttRequest](#m-ValidateOttRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `validateOtt`

<a id="r-post-sni-api-v1-auth-password-reset-confirm"></a>
### POST /sni/api/v1/auth/password-reset/confirm

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `PasswordResetConfirmationRequest`
- Réponse : `vide`
- Modèles : [PasswordResetConfirmationRequest](#m-PasswordResetConfirmationRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `confirmPasswordReset`

<a id="r-post-sni-api-v1-auth-password-reset-request"></a>
### POST /sni/api/v1/auth/password-reset/request

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `EmailRequest`
- Réponse : `vide`
- Modèles : [EmailRequest](#m-EmailRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `requestPasswordReset`

<a id="r-post-sni-api-v1-auth-refresh"></a>
### POST /sni/api/v1/auth/refresh

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `RefreshTokenRequest`
- Réponse : `LoginResponse`
- Modèles : [LoginResponse](#m-LoginResponse), [RefreshTokenRequest](#m-RefreshTokenRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `refresh`

<a id="r-post-sni-api-v1-auth-register"></a>
### POST /sni/api/v1/auth/register

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `RegisterMemberRequest`
- Réponse : `OttResponse`
- Modèles : [OttResponse](#m-OttResponse), [RegisterMemberRequest](#m-RegisterMemberRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `register`

<a id="r-post-sni-api-v1-auth-unlock-account"></a>
### POST /sni/api/v1/auth/unlock-account

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `EmailRequest`
- Réponse : `OttResponse`
- Modèles : [OttResponse](#m-OttResponse), [EmailRequest](#m-EmailRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `requestUnlockAccount`

<a id="r-post-sni-api-v1-auth-unlock-account-confirm"></a>
### POST /sni/api/v1/auth/unlock-account/confirm

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `ValidateOttRequest`
- Réponse : `vide`
- Modèles : [ValidateOttRequest](#m-ValidateOttRequest)
- Code : `src/main/java/com/sni/bokaticowork/security/authentication/controller/AuthenticationController.java`, méthode `confirmUnlockAccount`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-OttResponse"></a>
#### OttResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `verificationToken` | texte |  |  |
| `message` | texte |  |  |

<a id="m-EmailRequest"></a>
#### EmailRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `email` | string (email) | oui | longueur min `1` |

<a id="m-LoginResponse"></a>
#### LoginResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accessToken` | texte |  |  |
| `refreshToken` | texte |  |  |
| `emailVerified` | booléen |  |  |
| `roles` | texte[] |  |  |

<a id="m-LoginRequest"></a>
#### LoginRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `email` | string (email) | oui | longueur min `1` |
| `password` | texte | oui | longueur min `1` |

<a id="m-CurrentUserResponse"></a>
#### CurrentUserResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `userId` | texte |  |  |
| `email` | texte |  |  |
| `accountEnabled` | booléen |  |  |
| `authorities` | texte[] |  |  |

<a id="m-ValidateOttRequest"></a>
#### ValidateOttRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ottToken` | texte | oui | longueur min `1` |
| `verificationToken` | texte | oui | longueur min `1` |

<a id="m-PasswordResetConfirmationRequest"></a>
#### PasswordResetConfirmationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `token` | texte | oui | longueur min `1` |
| `newPassword` | texte | oui | longueur min `1` |

<a id="m-RefreshTokenRequest"></a>
#### RefreshTokenRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `refreshToken` | texte | oui | longueur min `1` |

<a id="m-RegisterMemberRequest"></a>
#### RegisterMemberRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `firstname` | texte | oui | longueur min `0`, longueur max `350` |
| `lastname` | texte | oui | longueur min `0`, longueur max `350` |
| `email` | string (email) | oui | longueur min `0`, longueur max `250` |
| `password` | texte | oui | longueur min `8`, longueur max `45` |
| `phone` | texte | oui | longueur min `0`, longueur max `30`, motif `^[+0-9()\s.\-]{6,30}$` |
| `phoneCountry` | texte |  | longueur min `0`, longueur max `10` |
| `whatsappPhone` | texte |  | longueur min `0`, longueur max `30` |
| `address` | [AddressRequest](#m-AddressRequest) |  |  |
| `birthDate` | date AAAA-MM-JJ |  |  |
| `gender` | texte |  | longueur min `0`, longueur max `20` |
| `preferredCommunicationChannel` | texte |  | longueur min `0`, longueur max `30` |
| `referralCode` | texte |  | longueur min `0`, longueur max `40` |

<a id="m-AddressRequest"></a>
#### AddressRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `countryCode` | texte |  | longueur min `0`, longueur max `3` |
