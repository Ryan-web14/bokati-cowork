# securite : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module `securite` (`src/main/java/com/sni/bokaticowork/security/`) ouvre et ferme les sessions, crée les comptes et décide de qui peut appeler quoi.

- **Public, sans jeton** : connexion par mot de passe ou par code à usage unique (OTT), inscription d'un membre et vérification de son adresse, mot de passe oublié (avec une page servie par le serveur), déverrouillage de compte.
- **Back-office** : écrans de gestion des comptes, des rôles et des droits, consultation des codes à usage unique pour le support, création d'agents.

Il travaille avec :
- `client` : l'inscription crée un membre et un client ;
- `socle` : les sessions (`core/audit`, `UserSessionServiceImpl`) et les courriels ;
- `notification` : l'avis à l'administrateur quand un utilisateur termine une réinitialisation qu'il a lancée.

Le module ne contient aucun worker planifié.

Sommaire : [Notions](#notions) · [Statuts](#statuts-et-cycle-de-vie) · [Parcours publics](#parcours) · [Gestion des comptes](#gérer-les-comptes-back-office) · [Rôles et droits](#gérer-les-rôles-et-les-droits-back-office) · [Règles et erreurs](#règles-et-erreurs-à-refléter-dans-lécran) · [À prévoir](#à-prévoir-côté-frontend) · [Effets automatiques](#effets-automatiques) · [Écarts](#écarts-relevés)

Fiche collaborateur (fonction, signature du visa, supérieur direct N+1) : [fiche-collaborateur.md](fiche-collaborateur.md).

## Notions

| Objet | Identifiant exposé | Ce qu'il faut savoir |
| --- | --- | --- |
| Compte (`Users`, table `users`) | `userId`, un code lisible de la forme `USR-ddMMyy-XXXXXXXX` (`UserServiceImpl.java:374`). Les routes `/admin/users/{userCode}` et `/admin/users/{userId}/roles` l'attendent toutes les deux. | Le champ `id` de `AdminUserResponse` est un int64 qui peut être arrondi par JavaScript ([règle](../README.md#identifiants--attention-aux-grands-nombres)). Ne jamais s'en servir : toujours utiliser `userId`. Un compte peut être un membre (rôle `MEMBER`) ou un agent de la maison. La liste d'administration les montre tous. |
| Rôle (`role`) | Son `name` en majuscules (`ADMIN`, `STAFF`…) pour `/admin/roles/{name}`. Son `id`, un petit entier auto-incrémenté, pour `/admin/roles/{roleId}/...` et `/admin/users/{userId}/roles/{roleId}`. | Un rôle porte des droits. Un rôle désactivé (`isActive=false`) ne donne plus ni son nom ni ses droits à ses titulaires. |
| Droit, appelé « permission » dans le code (`permission`) | Son `name` en majuscules et soulignés, par exemple `SYSTEM_USERS`. Il a aussi un `fullPermissionName` de la forme `MODULE:ACTION`, par exemple `SYSTEM:USERS`. Son `id` sert à `PATCH /admin/roles/{roleId}/permissions`. | Le compte reçoit **les deux écritures** de chaque droit, d'où des listes `roles` qui contiennent à la fois `SYSTEM_USERS` et `SYSTEM:USERS` (`UserPrincipal.java:92`). |
| Attribution d'un rôle à un compte (`role_user`) | Le couple `userId` et `roleId`. | Un compte peut porter plusieurs rôles. |
| Session (`user_session`, module `socle`) | Un identifiant UUID inscrit dans les jetons. Il n'est pas exposé par ce module. | Elle naît à chaque connexion. Elle meurt à la déconnexion, au changement de mot de passe, à la désactivation du compte, ou quand un vol de jeton est détecté. |
| Jeton d'accès, jeton de rafraîchissement, jeton de vérification | Des JWT opaques pour le frontend. | Seul le jeton d'accès authentifie un appel (`JWTFilter.java:114`). Le jeton de vérification (`verificationToken`) accompagne un code à usage unique. |
| Code à usage unique (OTT, table `one_time_token`) | Six chiffres envoyés par courriel. | Il sert à la connexion sans mot de passe, à la vérification d'adresse et au déverrouillage. Il vit 15 minutes (`APP_OTT_EXPIRATION_MS`). Un nouveau code annule les précédents du même compte. |
| Jeton de réinitialisation (table `password_reset_token`) | Un UUID, présent seulement dans le lien du courriel. Le serveur n'en garde que l'empreinte. | Il vit 15 minutes (`APP_PASSWORD_RESET_EXPIRATION_MS`), ne sert qu'une fois, et une nouvelle demande annule les précédents. |

### Ce que contient la liste `roles` (ou `authorities`)

Pour chaque rôle **actif** du compte, elle contient :
- `ROLE_<NOM>` ;
- pour chaque droit actif lié au rôle par un lien actif, `<NAME>` et `<MODULE>:<ACTION>`.

Exemple pour un administrateur : `["ADMIN_ACCESS", "ADMIN:ACCESS", "ROLE_ADMIN", "SYSTEM_USERS", "SYSTEM:USERS", ...]`, trié par ordre alphabétique.

### Qui entre dans le back-office

Il faut porter un des rôles de `SecurityRoles.ADMIN_REALM` (`src/main/java/com/sni/bokaticowork/security/authorization/SecurityRoles.java:27`) : `SUPER_ADMIN`, `ADMIN`, `MANAGER`, `FINANCE`, `CASHIER`, `STAFF`, `SUPPORT`, `KYC_REVIEWER`, `AUDITOR`, `VIEWER`, `OPERATIONS_AGENT`.

**Un rôle créé par l'API sous un autre nom n'ouvre pas le back-office**, même avec tous les droits. Ses titulaires reçoivent `403` sur toutes les routes d'administration.

## Statuts et cycle de vie

### Compte

Le compte n'a pas de champ de statut. Son état se lit dans cinq champs de `AdminUserResponse` : `accountEnabled`, `accountLocked`, `accountExpired`, `deleted` et `failedLoginAttempts`. Côté connexion, `emailVerified` complète ce tableau.

| État | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| Actif (`accountEnabled=true`, `accountLocked=false`) | Le compte peut se connecter. | Création par n'importe quelle route. Réactivation par `PATCH .../activate` ou `.../enable`. | Verrouillé, désactivé, archivé. |
| Verrouillé (`accountLocked=true`, `accountEnabled=true`) | La connexion par mot de passe est refusée avec `ACCOUNT_LOCKED`. Les jetons déjà émis restent valables : le filtre ne regarde volontairement pas le verrou (`JWTFilter.java:127`). | `PUT /admin/users/{userCode}` avec `isAccountLocked=true`. En théorie aussi après `FAILED_LOGIN_MAX_ATTEMPTS` échecs (5 par défaut), mais voir les [écarts](#écarts-relevés). | Actif, par `PATCH .../unlock`, par `POST /auth/unlock-account/confirm`, par une réinitialisation de mot de passe (lien ou administrateur), ou par `.../activate`. |
| Désactivé (`accountEnabled=false`, et `accountLocked=true` quand on passe par `.../deactivate`) | Plus de connexion. Avec `.../deactivate`, toutes les sessions sont fermées tout de suite (`UserServiceImpl.java:211`). Tout jeton encore présenté reçoit `401` (`JWTFilter.java:131`). | `PATCH .../deactivate` ou `.../disable`. Ou `PUT` avec `isAccountEnabled=false`, qui ne ferme pas les sessions. | Actif, par `.../activate` ou `.../enable`. |
| Archivé (`deleted=true`) | Suppression logique. Le compte disparaît de toutes les lectures, à cause de `@SQLRestriction("deleted = false")` sur `Users`. Ses sessions sont fermées. | `DELETE /admin/users/{userCode}`. | Aucune route ne le restaure. |
| Adresse non vérifiée (`emailVerified=false`) | Le compte peut se connecter, mais le portail membre le refuse s'il n'a pas encore `ROLE_MEMBER`. | Inscription en libre-service. | Vérifiée, par `POST /auth/ott/validate`, ou par l'activation du membre en back-office (`POST /sni/api/v1/members/portal/complete`, module `client`). |

### Code à usage unique (vu par `GET /admin/one-time-tokens`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `VALID` | Le code n'est ni utilisé ni échu. | `POST /auth/ott/request`, `/auth/register`, `/auth/unlock-account` ou `/auth/email/verify/resend`. | `USED` ou `EXPIRED`. |
| `USED` | Le code a servi, ou il a été annulé par un code plus récent : `invalidateAllTokensForUser` le marque comme utilisé. | Une validation réussie, ou une nouvelle demande de code. | Aucun, c'est un état final. |
| `EXPIRED` | Les 15 minutes sont passées. | L'échéance, sans traitement planifié. | Aucun, c'est un état final. |

## Parcours

### Se connecter par mot de passe

1. `POST /sni/api/v1/auth/login` avec `{ "email", "password" }` ([route](routes/AuthenticationController.md#r-post-sni-api-v1-auth-login)). La comparaison de l'adresse ignore la casse.
   - **Écran de connexion du back-office** : envoyer aussi `"space": "BACK_OFFICE"`. Un compte sans rôle d'équipe (membre, client) est alors refusé par le serveur avec `403 BACK_OFFICE_ACCESS_DENIED`, message « Ce compte n'a pas accès à l'espace d'administration. », après la vérification du mot de passe et sans ouvrir de session. Afficher ce message, pas « mot de passe incorrect ». Le portail client n'envoie pas `space` : rien ne change pour lui.
2. La réponse `200` contient `{ accessToken, refreshToken, emailVerified, roles }`. Stocker les deux jetons et `roles`.
3. Choisir l'espace :
   - si `roles` contient un rôle du back-office (voir [Notions](#qui-entre-dans-le-back-office)), ouvrir le back-office ;
   - sinon, si `roles` contient `ROLE_MEMBER`, ouvrir le portail membre ;
   - sinon, et si `emailVerified=false`, proposer la vérification de l'adresse (voir [S'inscrire](#sinscrire-comme-membre)). Les routes `/client/**` répondraient `403`.
4. Erreurs :
   - `401 INVALID_CREDENTIALS`, message `"Invalid credentials provided."` : mauvais mot de passe, compte inconnu ou compte désactivé, sans distinction ;
   - `401 ACCOUNT_LOCKED`, message `"Your account is locked. Use the unlock-account flow to regain access."` : proposer le [déverrouillage](#déverrouiller-son-compte) ;
   - `429` au-delà de 5 tentatives par minute depuis la même adresse IP.
5. Une connexion ferme la plus ancienne session ouverte du compte s'il en a déjà au moins deux (`UserSessionServiceImpl.invalidateOldestSessionByUser`). Un compte garde donc au plus deux sessions : une troisième connexion déconnecte le plus ancien appareil.

### Rester connecté (rafraîchissement)

1. Sur un `401` d'une route protégée, appeler `POST /sni/api/v1/auth/refresh` avec `{ "refreshToken" }` ([route](routes/AuthenticationController.md#r-post-sni-api-v1-auth-refresh)).
2. La réponse `200` ne contient que `accessToken` et `refreshToken` : `emailVerified` et `roles` valent `null`. Garder `roles` de la connexion, ou appeler `GET /sni/api/v1/auth/me` pour les relire.
3. **Rotation** : l'ancien jeton de rafraîchissement est consommé. Le réutiliser dans les 30 secondes donne un simple `401` (`APP_JWT_REFRESH_REUSE_GRACE_SECONDS`). Au-delà, le serveur y voit un vol et **ferme toutes les sessions du compte** (`AuthenticationServiceImpl.java:159` à `173`).
4. Échecs, tous en `401 INVALID_CREDENTIALS` : jeton inconnu, expiré (7 jours) ou consommé, session fermée (déconnexion, changement de mot de passe, désactivation), compte désactivé. Renvoyer vers l'écran de connexion.

### Connaître le compte connecté

`GET /sni/api/v1/auth/me` ([route](routes/AuthenticationController.md#r-get-sni-api-v1-auth-me)) renvoie `{ userId, email, accountEnabled, authorities }`. `authorities` contient la même chose que `roles` à la connexion, mais le champ porte un autre nom. Les droits sont relus en base à chaque requête (`JWTFilter.processToken`). Après un changement de rôle, `/auth/me` reflète tout de suite le nouvel état.

### Se déconnecter

1. `POST /sni/api/v1/auth/logout` avec l'en-tête `Authorization: Bearer <accessToken>`. Il n'y a pas de corps. La réponse est `204`.
2. La session portée par le jeton d'accès est fermée : son jeton de rafraîchissement ne marche plus.
3. La route exige un jeton d'accès valide. Avec un jeton expiré, elle répond `401`. Le frontend efface ses jetons dans tous les cas.

### Se connecter par code (sans mot de passe)

1. `POST /sni/api/v1/auth/ott/request` avec `{ "email" }` ([route](routes/AuthenticationController.md#r-post-sni-api-v1-auth-ott-request)). La réponse `200` est `{ verificationToken, message: "OTP sent to email" }`. Un code à six chiffres part par courriel, sous l'objet « Votre code de connexion ». Garder `verificationToken` en mémoire.
2. Afficher six cases pour le code.
3. `POST /sni/api/v1/auth/ott/validate` avec `{ "ottToken": "<code>", "verificationToken" }` ([route](routes/AuthenticationController.md#r-post-sni-api-v1-auth-ott-validate)). La réponse `200` a la même forme que la connexion : `accessToken`, `refreshToken`, `emailVerified`, `roles`.
4. Effet de bord : si le compte est rattaché à un membre, le membre est activé (statut `ACTIVE`, `ROLE_MEMBER` ajouté) et l'adresse est marquée vérifiée (`UserProvisioningServiceImpl.completePortalVerification`).
5. Erreurs :
   - adresse inconnue à l'étape 1 : `404 RESOURCE_NOT_FOUND`, message `"No user found"` ;
   - code faux ou échu à l'étape 3 : `500`, voir les [écarts](#écarts-relevés). Afficher « code invalide ou expiré » et proposer un nouvel envoi ;
   - `429` : les routes `/auth/ott/request` et `/auth/ott/validate` ont **chacune** un plafond de 3 appels par 10 minutes et par adresse IP. Au bout de trois codes mal saisis, il faut attendre.

### S'inscrire comme membre

1. `POST /sni/api/v1/auth/register` ([route](routes/AuthenticationController.md#r-post-sni-api-v1-auth-register)). Le corps contient :
   - obligatoires : `firstname`, `lastname`, `email`, `password` (8 à 45 caractères), `phone` (6 à 30 caractères parmi chiffres, `+`, espaces, points, tirets et parenthèses) ;
   - facultatifs : `phoneCountry` (`CG`, `+242`…, ignoré si le numéro commence par `+`), `whatsappPhone`, `address`, `birthDate`, `gender`, `preferredCommunicationChannel`.
2. La réponse est `201` avec `{ verificationToken, message }`. Le serveur a créé un compte actif, sans rôle, et un membre `PENDING` avec l'accès portail. Un code part par courriel.
3. Écran de saisie du code, puis `POST /sni/api/v1/auth/ott/validate` comme ci-dessus. L'utilisateur est alors connecté, avec `ROLE_MEMBER` et `emailVerified=true`.
4. « Je n'ai pas reçu le code » : `POST /sni/api/v1/auth/email/verify/resend` avec `{ "email" }`. La réponse `200` donne un nouveau `verificationToken` (à mettre à la place de l'ancien) et `message`. Pour une adresse inconnue, `verificationToken` vaut `null` et le message reste neutre.
5. L'inscrit qui ferme l'onglet peut se connecter par mot de passe. Il reçoit alors `emailVerified=false` et `roles=[]`. Lui proposer l'étape 4, puis l'étape 3.
6. Erreurs :
   - `400 BAD_REQUEST` `"Cette adresse e-mail n'est pas acceptee. Utilisez une adresse personnelle ou professionnelle valide."` : domaine réservé, boîte jetable ou domaine sans serveur de courrier (`EmailDomainPolicy`) ;
   - `400` `"User with email ... already exists"` ou un message du module `client` en cas de doublon ;
   - `400 VALIDATION_FAILED` sur les champs ;
   - `429` au-delà de 5 inscriptions par heure et par adresse IP.

### Mot de passe oublié

1. `POST /sni/api/v1/auth/password-reset/request` avec `{ "email" }`. La réponse est `204`. Un courriel « Réinitialisation de mot de passe » part avec un lien valable 15 minutes.
2. **Le lien mène à une page servie par le serveur**, pas au frontend : `{APP_BASE_URL}/sni/api/v1/auth/password-reset/form?token=...` (`PasswordResetMailServiceImpl.java:23`). La page vérifie le jeton (`GET`), puis affiche un formulaire avec deux champs et une protection CSRF par cookie. Elle reçoit la saisie (`POST`), puis affiche une page de succès ou d'erreur ([routes](routes/PasswordResetFormController.md)). Le frontend n'a rien à construire pour ce chemin. Après le succès, l'utilisateur revient à l'écran de connexion.
3. Pour une page propre au frontend, la route JSON existe : `POST /sni/api/v1/auth/password-reset/confirm` avec `{ "token", "newPassword" }`, réponse `204`. Mais le courriel pointe toujours vers la page du serveur. Changer `FORM_PATH` est une décision côté serveur.
4. Effets d'une réinitialisation réussie :
   - toutes les sessions sont fermées ;
   - le compte est déverrouillé et son compteur d'échecs est remis à zéro ;
   - le courriel « Votre mot de passe a été modifié » part ;
   - si un administrateur avait lancé la réinitialisation, il reçoit la notification `PASSWORD_RESET_COMPLETED`.
5. Erreurs :
   - adresse inconnue à l'étape 1 : `404` `"No user found"` ;
   - `400 BAD_REQUEST` à la confirmation : `"Lien de réinitialisation invalide ou expiré"`, `"Ce lien de réinitialisation a déjà été utilisé"`, `"Ce lien de réinitialisation a expiré. Veuillez en demander un nouveau"`, ou une règle de mot de passe (voir [Règles](#règles-et-erreurs-à-refléter-dans-lécran)) ;
   - `429` : tout `/auth/password-reset/**` est plafonné à 3 appels par 10 minutes, par chemin et par adresse IP. Le `GET` et le `POST` de la page partagent le même chemin `/form`. Ouvrir la page, se tromper une fois et réessayer épuise déjà ce plafond.

Le membre connecté au portail a sa propre route, dans le module `portal` : `POST /sni/api/v1/client/profile/change-password`, sans corps. Elle envoie le même type de lien. Aucune route ne change le mot de passe en donnant l'ancien.

### Déverrouiller son compte

1. Après un `401 ACCOUNT_LOCKED`, proposer « Débloquer mon compte ».
2. `POST /sni/api/v1/auth/unlock-account` avec `{ "email" }`. La réponse `200` est `{ verificationToken, message: "If an account exists for this email, an unlock code has been sent." }`. Pour une adresse inconnue, `verificationToken` vaut `null` : afficher quand même l'écran du code, sans rien révéler.
3. `POST /sni/api/v1/auth/unlock-account/confirm` avec `{ "ottToken": "<code>", "verificationToken" }`. La réponse est `204`. Renvoyer alors vers la connexion.
4. Erreurs :
   - code faux, échu ou déjà utilisé : `401 INVALID_CREDENTIALS` `"Invalid credentials provided."`. **Ce `401` ne doit pas déclencher le rafraîchissement automatique** ;
   - `429` : 3 appels par 10 minutes, par chemin et par adresse IP.
5. Un compte désactivé par un administrateur porte aussi `accountLocked=true`, donc il reçoit le même message `ACCOUNT_LOCKED`. Le déverrouillage le débloque, mais la connexion échoue ensuite en `INVALID_CREDENTIALS`, car le compte reste désactivé. Seul un administrateur peut le réactiver.

## Gérer les comptes (back-office)

Toutes ces routes exigent `SYSTEM:USERS`. Pour les sous-chemins `/roles`, il faut **aussi** `SYSTEM:ROLES` ([table](routes.md)). En plus, `PrivilegeGuard` (`src/main/java/com/sni/bokaticowork/security/admin/support/PrivilegeGuard.java`) ajoute ces règles :
- seuls `ADMIN` et `SUPER_ADMIN` créent des comptes ou changent des rôles ;
- personne ne modifie ses propres rôles ;
- on n'attribue ni ne retire `SUPER_ADMIN` sans être `SUPER_ADMIN`, ni un rôle qui donne un droit qu'on n'a pas soi-même ;
- seul un `SUPER_ADMIN` agit sur le compte d'un `SUPER_ADMIN` ;
- le dernier `SUPER_ADMIN` actif ne peut être ni désactivé, ni verrouillé, ni archivé, ni privé de son rôle.

### Liste et recherche

1. `GET /sni/api/v1/admin/users?email=&enabled=&locked=&deleted=&page=&size=&sort=` renvoie une page maison de `AdminUserResponse`. `email` cherche une sous-chaîne, sans tenir compte de la casse. Le tri accepte les propriétés de l'entité (`email`, `createdAt`, `lastLogin`…). Par défaut, la page fait 20 lignes et n'est pas triée.
2. Recherche libre : `GET /sni/api/v1/admin/users/search/by-name?query=...` renvoie une liste, pas une page, triée par pertinence (fonction SQL `search_admin_user`). Avec une requête vide, elle renvoie les 20 premiers comptes.
3. Accès direct : `GET /sni/api/v1/admin/users/{userCode}` ou `GET /sni/api/v1/admin/users/by-email?email=...`.
4. Colonnes utiles :
   - `userId`, `email`, `firstname`, `lastname` ;
   - `roleNames`, sans préfixe `ROLE_`, qui permet de séparer agents et membres ;
   - `accountEnabled`, `accountLocked`, `failedLoginAttempts`, `lastLogin`, `createdAt`.
5. `lastLogin`, `createdAt` et `updatedAt` sont des `LocalDateTime` **sans fuseau**.

### Créer un compte d'agent

1. `POST /sni/api/v1/admin/users` avec `{ email, firstname, lastname, generatePassword, password, roleNames }` ([route](routes/AdminUserController.md#r-post-sni-api-v1-admin-users)) :
   - si `generatePassword` est absent, il vaut `true` ;
   - `password` ne sert que si `generatePassword=false` ;
   - si `roleNames` est absent ou vide, le compte reçoit `["STAFF"]`.
2. La réponse `201` est un `AdminUserResponse`. **`generatedPassword` n'y apparaît qu'une seule fois** : l'afficher avec un bouton de copie et prévenir qu'il ne sera plus visible. Le courriel de bienvenue (« Votre acces administrateur Bokati ») ne le contient pas.
3. L'en-tête `Idempotency-Key` est conseillé.
4. Erreurs :
   - `400` `"User with email X already exists"` ;
   - `404` `"Role introuvable : X"` ;
   - `403` si l'auteur n'est ni `ADMIN` ni `SUPER_ADMIN`, ou si un rôle demandé dépasse ses droits. Voir l'[écart sur les messages 403](#écarts-relevés).
5. Une route plus ancienne existe : `POST /sni/api/v1/admin/provisioning/staff`, avec le droit `SYSTEM:SETTINGS` et le corps `UserRequest`. Elle crée un compte `STAFF`, mais perd le mot de passe généré (voir les [écarts](#écarts-relevés)). Préférer `POST /admin/users`.

### Modifier un compte

`PUT /sni/api/v1/admin/users/{userCode}` avec `{ email, firstname, lastname, isAccountEnabled, isAccountLocked, roleNames }`. Tous les champs sont facultatifs :
- un champ absent ou vide ne change rien ;
- `email` est enregistré en minuscules, sans vérification de la nouvelle adresse ;
- `roleNames` absent laisse les rôles tels quels. Une liste **remplace** les rôles : elle ajoute les manquants et retire les autres. Renvoyer la liste inchangée ne déclenche aucun contrôle.

Les erreurs :
- `400` `"Un utilisateur existe deja avec cet email"` ;
- `403` en modifiant ses propres rôles, ou en dépassant ses droits.

Pour désactiver, préférer la route dédiée : ce `PUT` ne ferme pas les sessions.

### Activer, désactiver, déverrouiller, archiver

Ces routes renvoient le compte à jour, sauf l'archivage qui répond `204`.

| Action | Route | Effet |
| --- | --- | --- |
| Activer | `PATCH /sni/api/v1/admin/users/{userCode}/activate` (`/enable` est un alias) | `enabled=true`, `locked=false`, `expired=false`. |
| Désactiver | `PATCH .../deactivate` (`/disable` est un alias) | `enabled=false`, `locked=true`, et toutes les sessions sont fermées. |
| Déverrouiller | `PATCH .../unlock` | `locked=false`, `failedLoginAttempts=0`. |
| Archiver | `DELETE /sni/api/v1/admin/users/{userCode}` | `deleted=true`, compte désactivé et verrouillé, sessions fermées. Le compte disparaît de toutes les listes. Demander confirmation. |

### Réinitialiser le mot de passe d'un compte

Deux possibilités :

1. **Envoyer un lien**, ce qui est conseillé : `POST /sni/api/v1/admin/users/{userCode}/reset-password`, sans corps, réponse `202`. L'utilisateur reçoit le courriel de réinitialisation. Quand il le termine, l'administrateur qui a lancé la demande reçoit la notification `PASSWORD_RESET_COMPLETED` (module `notification`).
2. **Fixer le mot de passe tout de suite** : `PATCH /sni/api/v1/admin/users/{userCode}/password/reset` :
   - corps facultatif `{ generatePassword, newPassword }`, avec `generatePassword` à `true` par défaut ;
   - la réponse est un `AdminUserResponse`, avec `generatedPassword` s'il a été généré ;
   - les sessions du compte sont fermées, et le compte est déverrouillé ;
   - `newPassword` n'est soumis à aucune règle de solidité. Appliquer côté écran les règles décrites dans [Règles](#règles-et-erreurs-à-refléter-dans-lécran).

### Rôles d'un compte, un par un

- `GET /sni/api/v1/admin/users/{userCode}/roles` renvoie une liste de `RoleResponse`.
- `POST /sni/api/v1/admin/users/{userCode}/roles` avec `{ "roleName": "MANAGER" }` répond `201`, sans corps. Attribuer un rôle déjà porté ne fait rien.
- `DELETE /sni/api/v1/admin/users/{userCode}/roles/{roleId}` répond `204`. `roleId` est l'`id` du rôle.

Ces trois routes exigent `SYSTEM:USERS` **et** `SYSTEM:ROLES`, et suivent les règles de `PrivilegeGuard`.

### Codes à usage unique (support)

`GET /sni/api/v1/admin/one-time-tokens?email=&used=&page=&size=` renvoie une page de `{ token, email, userId, firstname, lastname, createdAt, expiredAt, used, expired, status }`, les plus récents d'abord. **Le code apparaît en clair**, pour qu'un agent puisse le lire au téléphone à un membre qui n'a pas reçu le courriel. Chaque consultation est journalisée côté serveur.

Accès : `SYSTEM:SETTINGS`, avec en plus le rôle `ADMIN` ou `SUPER_ADMIN`. Le tri demandé est ignoré.

### Premier administrateur (installation)

`POST /sni/api/v1/admin/provisioning/bootstrap-admin` est public. Corps : `{ email, password, firstname, lastname, bootstrapSecret }`. Il crée un compte `ADMIN`, pas `SUPER_ADMIN`.

La route ne marche qu'à trois conditions :
- `BOOTSTRAP_SECRET` est configuré ;
- le secret envoyé est le bon ;
- aucun compte ne porte encore `ADMIN`.

Sinon, elle répond `403`. Cette route sert à l'installation : elle n'a pas sa place dans un écran.

## Gérer les rôles et les droits (back-office)

### Rôles

| Action | Route | Détail |
| --- | --- | --- |
| Lister | `GET /sni/api/v1/admin/roles` | Tous les rôles, actifs ou non, triés par nom. C'est une liste, pas une page. `permissions` y vaut toujours `null`. |
| Rechercher | `GET /sni/api/v1/admin/roles/search/by-name?query=` | Une liste. Avec une requête vide, la même chose que la liste complète. |
| Lire | `GET /sni/api/v1/admin/roles/{name}` | `404` si le rôle n'existe pas. |
| Créer | `POST /sni/api/v1/admin/roles` avec `{ name, displayName, description, isActive }` | `name` doit suivre `^[A-Z_]+$` et ne pas dépasser 250 caractères. `displayName` est obligatoire. Envoyer `isActive` explicitement. La réponse est `201`. Le rôle naît sans droits. Doublon : `400` `"Role with name X already exist"`. |
| Modifier | `PUT /sni/api/v1/admin/roles/{name}` avec le même corps | Envoyer `isActive` et `description`, sinon l'enregistrement échoue (voir les [écarts](#écarts-relevés)). `displayName` est enregistré en majuscules. |
| Activer ou désactiver | `PATCH .../{name}/activate` ou `.../deactivate` | Réponse `204`. Un rôle désactivé retire, dès la requête suivante, son nom et ses droits à tous ses titulaires. |
| Supprimer | `DELETE /sni/api/v1/admin/roles/{name}` | Réponse `204`. Un rôle système répond `400` `"Role with name X is a system role and cannot be deleted"`. Retirer d'abord ses droits et ses titulaires : voir les [écarts](#écarts-relevés). |

Pour modifier, activer, désactiver ou supprimer un rôle, ou changer ses droits, il faut être `ADMIN` ou `SUPER_ADMIN`. Il faut aussi ne pas porter soi-même ce rôle et, si l'on n'est pas `SUPER_ADMIN`, ne pas toucher au rôle `SUPER_ADMIN` (`PrivilegeGuard.assertMayEditRole`). Masquer ces boutons sur les rôles que l'utilisateur porte : ils figurent dans sa liste `roles` sous la forme `ROLE_<NOM>`.

### Droits d'un rôle (matrice)

1. Charger le catalogue : `GET /sni/api/v1/admin/permissions?activeOnly=true` ([route](routes/PermissionAdminController.md#r-get-sni-api-v1-admin-permissions)). Avec `activeOnly=true`, tout le catalogue arrive dans une seule page et les paramètres `page` et `size` sont ignorés. Grouper par `module` pour l'affichage.
2. Charger les droits du rôle : `GET /sni/api/v1/admin/roles/{roleId}/permissions`. La liste peut contenir des droits désactivés : les griser.
3. Enregistrer la sélection en **remplaçant tout**, par l'une de ces deux routes :
   - `PATCH /sni/api/v1/admin/roles/{roleId}/permission-names` avec `{ "permissionNames": ["SYSTEM:USERS", "BILLING_READ"] }`. Le serveur accepte les deux écritures et les normalise. Un nom inconnu donne `400` `"Permission with name X not found"` ;
   - `PATCH /sni/api/v1/admin/roles/{roleId}/permissions` avec `{ "permissionIds": [..] }`. Un identifiant inconnu donne `404`.
   Dans les deux cas, la liste ne peut pas être vide (`400 VALIDATION_FAILED`).
4. Tout retirer : `DELETE /sni/api/v1/admin/roles/{roleId}/permissions`, réponse `204`.
5. Hors `SUPER_ADMIN`, on ne peut donner à un rôle que des droits qu'on porte soi-même. Sinon, la réponse est `403`. Désactiver côté écran les cases des droits absents de sa propre liste `roles`.
6. Ces routes exigent `SYSTEM:ROLES` par l'URL **et** `SYSTEM:PERMISSIONS` sur la méthode.

### Catalogue des droits

Toutes ces routes exigent `SYSTEM:PERMISSIONS`. L'écriture exige en plus d'être `ADMIN` ou `SUPER_ADMIN`.

- `GET /sni/api/v1/admin/permissions?page=&size=&sort=` : page maison, triée par `name` et de 20 lignes par défaut. Avec `systemOnly=true`, les droits système, en une seule page.
- `GET /sni/api/v1/admin/permissions/search/by-name?query=` renvoie une liste.
- `GET /sni/api/v1/admin/permissions/{name}` : `name` est la forme à soulignés (`SYSTEM_USERS`). Un nom absent donne `400`, pas `404`.
- Créer, modifier, activer, désactiver ou supprimer passe par `POST`, `PUT /{name}`, `PATCH /{name}/activate`, `PATCH /{name}/deactivate` et `DELETE /{name}`.

**Créer un droit n'a d'effet que si le code le vérifie.** La correspondance entre chemins et droits est écrite en dur dans `src/main/java/com/sni/bokaticowork/security/authorization/AdminApiAuthorizationManager.java`. L'écran du catalogue sert donc surtout à consulter, et à activer ou désactiver. Désactiver un droit le retire à tous les comptes dès la requête suivante.

## Règles et erreurs à refléter dans l'écran

- **Mot de passe, lors d'une réinitialisation** (`PasswordPolicyValidator`) :
  - au moins 6 caractères, une lettre et un chiffre ;
  - plus de deux caractères différents ;
  - absent d'une liste de mots de passe courants (`123456`, `azerty`, `password1`…).

  Messages exacts : `"Le mot de passe doit contenir au moins 6 caractères."`, `"... au moins une lettre."`, `"... au moins un chiffre."`, `"Le mot de passe est trop simple. Variez les caractères."`, `"Ce mot de passe est trop courant. Choisissez-en un autre."`
- **Mot de passe, à l'inscription** : 8 à 45 caractères, sans autre règle (`"Password must be between 8 and 45 characters"`). Appliquer aussi les règles de solidité ci-dessus côté écran, pour que le mot de passe reste acceptable lors d'une future réinitialisation.
- **Adresse** : format courriel exigé partout (`@Email`). À l'inscription, les domaines jetables, réservés ou sans serveur de courrier sont refusés.
- **Nom de rôle** : `^[A-Z_]+$`. **Nom de droit** : lettres et soulignés seulement.
- **Plafonds d'appels** (`RateLimitRules`, par adresse IP, réglables) :

| Chemins | Plafond |
| --- | --- |
| `/auth/login` | 5 par minute |
| `/auth/register` | 5 par heure |
| `/auth/ott/**`, `/auth/password-reset/**`, `/auth/unlock-account*`, `/auth/email/verify/resend` | 3 par 10 minutes, **par chemin** |
| Tout le reste de `/sni/api/v1/**`, `/auth/refresh` compris | 600 par minute |

  Un `429` porte l'en-tête `Retry-After`, en secondes : l'afficher comme un délai d'attente.
- **Erreurs fréquentes** :
  - `401 INVALID_CREDENTIALS` sur `/auth/login`, `/auth/refresh` et `/auth/unlock-account/confirm` ;
  - `401 ACCOUNT_LOCKED` sur `/auth/login` ;
  - `403 BACK_OFFICE_ACCESS_DENIED` sur `/auth/login` avec `space: BACK_OFFICE`, pour un compte sans rôle d'équipe ;
  - `403 FORBIDDEN` sur une règle de `PrivilegeGuard` ;
  - `404 RESOURCE_NOT_FOUND` pour un compte (`"Utilisateur introuvable : USR-..."`) ou un rôle introuvable ;
  - `400 BAD_REQUEST` pour les doublons et les règles métier.
- **Un `401` avec un jeton expiré ou invalide** est produit par `response.sendError` (`JWTFilter.java:102`), pas par le format d'erreur commun. Se fier au statut, pas au corps.

## À prévoir côté frontend

- **Écrans publics** :
  - connexion, avec un lien « connexion par code » ;
  - saisie du code à six chiffres, partagée par la connexion par code, l'inscription et le déverrouillage ;
  - inscription ;
  - mot de passe oublié : un seul champ, puis le message « vérifiez votre boîte » ;
  - déverrouillage.

  Il n'y a pas d'écran de nouveau mot de passe à construire : le serveur sert la page.
- **Écrans de back-office** :

| Écran | Droit à vérifier dans `roles` | Condition supplémentaire |
| --- | --- | --- |
| Comptes (liste, fiche, création, modification, actions d'état, rôles) | `SYSTEM:USERS` | `SYSTEM:ROLES` pour l'onglet des rôles. `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` pour créer un compte ou changer des rôles. |
| Rôles | `SYSTEM:ROLES` | `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN` pour écrire. |
| Matrice des droits d'un rôle | `SYSTEM:ROLES` et `SYSTEM:PERMISSIONS` | |
| Catalogue des droits | `SYSTEM:PERMISSIONS` | |
| Codes à usage unique | `SYSTEM:SETTINGS` | `ROLE_ADMIN` ou `ROLE_SUPER_ADMIN`. |

  `ROLE_SUPER_ADMIN` passe la règle d'URL, mais **pas** les `@PreAuthorize` : il doit porter lui-même `SYSTEM:USERS`, `SYSTEM:ROLES` et `SYSTEM:PERMISSIONS`.
- **Masquer les actions interdites par `PrivilegeGuard`** :
  - les actions sur un compte `SUPER_ADMIN` quand l'utilisateur n'est pas `SUPER_ADMIN` ;
  - la modification de ses propres rôles ;
  - l'attribution de `SUPER_ADMIN` ;
  - la modification d'un rôle qu'on porte.
- **Stockage des jetons** :
  - garder `accessToken`, `refreshToken` et `roles` ;
  - `refresh` ne renvoie pas `roles` ;
  - après un changement de droits fait par l'utilisateur sur lui-même ou sur son rôle, rappeler `/auth/me`.
- **Rafraîchissement** :
  - n'avoir qu'un seul rafraîchissement en cours à la fois, partagé entre les onglets (verrou dans `localStorage`, ou `BroadcastChannel`). Un jeton de rafraîchissement rejoué plus de 30 secondes après sa rotation ferme toutes les sessions ;
  - ne jamais lancer le rafraîchissement automatique sur une réponse de `/auth/*` : `login`, `refresh` et `unlock-account/confirm` répondent `401` pour de mauvais identifiants.
- **Durée des jetons d'accès** : 15 minutes en production (`application-prod.yml`), mais 7 jours par défaut en développement (`application-dev.yml`, `APP_JWT_ACCESS_EXPIRATION_MS`). Tester le rafraîchissement en développement demande de réduire cette valeur.
- **Identifiants** : toujours `userId` (`USR-...`) pour les comptes, `name` pour les rôles et les droits dans les chemins `/{name}`, `id` pour les chemins `/{roleId}`. Ne pas utiliser le champ `id` d'un compte.
- **Mot de passe généré** : l'afficher une seule fois, avec un bouton de copie. Ne jamais le stocker.
- **Champs à ne pas envoyer** : `bootstrapSecret` hors installation, et `id`, `fullPermissionName`, `permissions` dans les corps de rôle et de droit. Le JSON est strict, un champ en trop donne `400`.

## Effets automatiques

Le module n'a pas de worker. Ce que le serveur fait seul :

- **Courriels** :
  - « Votre code de connexion » : code à usage unique ;
  - « Réinitialisation de mot de passe » : lien de 15 minutes ;
  - « Votre mot de passe a été modifié » : après une réinitialisation par lien ;
  - « Votre acces administrateur Bokati » : création par `POST /admin/users`.

  Ils partent tous après la validation de la transaction, par le module `socle`.
- **Alertes aux super-administrateurs** (`PrivilegeChangeNotifier`) : un courriel « Bokati · changement de droits : ... » part pour la création d'un compte d'administration, une attribution ou un retrait de rôle, la création, l'activation, la désactivation ou la suppression d'un rôle ou d'un droit, et le remplacement des droits d'un rôle.
- **Notification** `PASSWORD_RESET_COMPLETED` à l'administrateur qui a lancé une réinitialisation, quand l'utilisateur la termine.
- **Sessions** :
  - fermeture de la plus ancienne à chaque connexion, au-delà de deux ;
  - fermeture de toutes les sessions au changement de mot de passe, à la désactivation, à l'archivage, et quand un vol de jeton de rafraîchissement est détecté.
- **Droits relus à chaque requête** : un changement de rôle ou de droit, ou une désactivation, s'applique dès l'appel suivant, sans reconnexion.
- **Journal d'audit** (`@Audited`) sur les écritures d'administration des comptes, des rôles et des droits. Il se consulte dans le module `socle`.

## Écarts relevés

1. **Corrigé le 2026-11-08** : un code faux ou échu, un jeton de vérification échu et une adresse inconnue répondent `401` ; un compte verrouillé répond `401 ACCOUNT_LOCKED`, un compte désactivé `401`, comme la connexion par mot de passe. Constat d'origine : **Un code à usage unique faux ou échu donne `500` à la connexion par code.** `validateOneTimeToken` renvoie `Map.of(false, "")`, puis `.get(true)` donne `null`, et `email.isEmpty()` lève une `NullPointerException` (`src/main/java/com/sni/bokaticowork/security/authentication/service/implementation/AuthenticationServiceImpl.java:209` et `:211`). Le déverrouillage traite bien ce cas (`:295`). De même, un `verificationToken` expiré lève une `ExpiredJwtException` qu'aucun gestionnaire ne traduit, ce qui donne probablement `500` (`OneTimeTokenServiceImpl.java:63`, non vérifié à l'exécution).
2. **Le verrouillage après cinq échecs ne s'enregistre probablement jamais.** `login` est `@Transactional` sans `noRollbackFor` (`AuthenticationServiceImpl.java:75`). L'exception `BadCredentialException` qui suit `recordLoginFailure` (`:339`) annule donc l'incrément de `failedLoginAttempts` (`UserServiceImpl.java:311`). Ce point est déduit de la lecture et n'a pas été vérifié à l'exécution.
3. **Les messages des `403` métier sont perdus.** `GlobalExceptionHandler.handleForbidden` remplace le message par `"Access to this resource is forbidden."` (`src/main/java/com/sni/bokaticowork/core/exception/GlobalExceptionHandler.java:80`). Les messages français de `PrivilegeGuard` (« Vous ne pouvez pas modifier vos propres roles. »…) et de l'amorçage n'arrivent qu'en développement, dans `debugMessage`. De même, tout `BadCredentialException` devient `"Invalid credentials provided."` (`:103`).
4. **Les routes publiques révèlent si une adresse existe.** `POST /auth/ott/request` et `POST /auth/password-reset/request` répondent `404 "No user found"` pour une adresse inconnue (`AuthenticationServiceImpl.java:197` et `:223`), alors que `unlock-account` et `email/verify/resend` répondent de façon neutre.
5. **La connexion par code ignore le verrou et la désactivation.** `validateOttLogin` crée la session sans passer par `AuthenticationManager` (`AuthenticationServiceImpl.java:212`). Un compte verrouillé se connecte, et un compte désactivé reçoit des jetons, puis un `401` à chaque appel.
6. **Un compte désactivé reçoit le message « compte verrouillé ».** `deactivateUser` pose aussi `isAccountLocked=true` (`UserServiceImpl.java:206`), d'où `ACCOUNT_LOCKED` et une invitation à un déverrouillage qui ne suffit pas.
7. **`GET /admin/users?deleted=true` ne renvoie jamais rien**, à cause de `@SQLRestriction("deleted = false")` sur `Users` (`src/main/java/com/sni/bokaticowork/security/admin/user/model/Users.java:26`). Le filtre de `UserAdminServiceImpl.java:116` est sans effet.
8. **`PUT /admin/users/{userCode}` avec `isAccountEnabled=false` ne ferme pas les sessions**, contrairement à `/deactivate` (`UserAdminServiceImpl.java:171` à `177`). Les jetons d'accès tombent quand même à la requête suivante, car le filtre vérifie `isEnabled`. Le jeton de rafraîchissement, lui, est refusé.
9. **`POST /admin/provisioning/staff` perd le mot de passe généré.** Il est posé sur un objet de réponse jeté (`UserServiceImpl.java:71` à `76`), et le contrôleur relit le compte (`UserProvisioningController.java`, `getUserByEmail`). Le mot de passe reste inconnu de tous. Par ailleurs, `password` est exigé même avec `generatePassword=true` (`UserRequest`).
10. **Modifier un droit échoue toujours si son nom ne change pas.** `isPermissionValid` refuse un nom qui existe déjà (`src/main/java/com/sni/bokaticowork/security/admin/role/service/implementation/PermissionServiceImpl.java:218`), et l'erreur est enveloppée dans une `RuntimeException`, donc `500` (`:79`). La création enveloppe aussi ses erreurs de validation en `500` (`:54`). Dans `PermissionMapperDecorator.java:44`, `action` n'est mise à jour que si `module` change.
11. **La modification d'un rôle est fragile.** Elle lève une `NullPointerException` si `isActive` est absent (`RoleMapperDecorator.java:71`) ou si la description en base est nulle (`:67`). Elle ne réactive jamais un rôle (`isActive=true` est ignoré), et elle met `displayName` en majuscules (`:62`). Une erreur de validation devient `500` (`RoleServiceImpl.java:76`), et un rôle introuvable `400` au lieu de `404` (`:79`).
12. **Supprimer un rôle ou un droit encore utilisé échoue probablement en `500`.** Les clés étrangères de `role_user` et `role_permission` n'ont pas de `ON DELETE CASCADE` (`V3__create_customer_member.sql:36` et `:166`). La suppression directe est à `RoleServiceImpl.java:92` et `PermissionServiceImpl.java:90`. Ce point n'a pas été vérifié à l'exécution.
13. **`RoleResponse.permissions` est toujours `null`.** Le champ est ignoré par le mapper (`RoleMapper.java:18`).
14. **Les règles de mot de passe divergent.** Il faut 8 caractères sans autre règle à l'inscription (`RegisterMemberRequest.java:36`), 6 caractères avec lettre et chiffre à la réinitialisation, et aucune règle pour la réinitialisation par un administrateur avec `newPassword` (`UserAdminServiceImpl.java:231`) ni pour la création par `POST /admin/users`.
15. **Les codes à six chiffres ne sont pas uniques entre comptes.** `findByToken` attend une seule ligne (`OneTimeTokenRepository.findByToken`). Deux codes identiques valides en même temps donneraient une erreur. Le cas est rare, et il n'a pas été vérifié.
16. **Les messages sont en anglais** sur les routes publiques : `"OTP sent to email"`, `"If an account exists..."`, `"Registration successful..."`, `"Your account is locked..."`, et sur la plupart des erreurs de rôles et de droits.
