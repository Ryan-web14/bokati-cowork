# Documentation frontend de référence

Tout ce qu'un frontend doit savoir pour appeler l'API de Bokati Cowork : chaque route, qui peut l'appeler, ce qu'elle attend, ce qu'elle renvoie, et comment les modules fonctionnent.

Cette documentation est écrite pour être lue par un humain **et** par un agent (un autre Claude) qui code le frontend. Elle remplace, pour le frontend, les anciens fichiers de `docs/frontend/` : en cas de désaccord, c'est celle-ci qui suit le code.

## Comment s'en servir

| Je cherche | Je lis |
| --- | --- |
| Les règles communes à toutes les routes (connexion, erreurs, pagination, identifiants, dates) | ce fichier, en entier, avant tout le reste |
| La liste des modules et leur taille | [MODULES.md](MODULES.md) |
| À quoi sert un module, ses statuts, ses parcours, ce qu'il faut côté écran | `<module>/README.md` |
| Toutes les routes d'un module, en une table | `<module>/routes.md` |
| Une route précise : accès, paramètres, corps, réponse, et chaque champ JSON | `<module>/routes/<Contrôleur>.md` |
| Les mêmes informations pour un programme (génération de types, client API) | `<module>/routes.json` |

Méthode conseillée pour un agent : lire ce fichier, puis le `README.md` du module visé, puis seulement le fichier `routes/<Contrôleur>.md` des routes utilisées. Chaque fichier `routes/<Contrôleur>.md` se suffit à lui-même : il contient, en fin de fichier, tous les modèles JSON qu'il cite.

## Ce qui est généré et ce qui est rédigé

- **Généré depuis le code** : `MODULES.md`, `<module>/routes.md`, `<module>/routes/*.md`, `<module>/routes.json`. Chemins, verbes, droits exigés, paramètres et champs JSON y sont exacts au moment de la génération. Ne pas les modifier à la main : voir [Régénérer](#régénérer).
- **Rédigé** : ce fichier et les `<module>/README.md`, écrits en lisant les contrôleurs et les services.

Les modules suivent les paquets du code : `features/<module>` pour les modules métier, `securite` pour `security/` (connexion, comptes, rôles), `parametres` pour `core/settings`, `socle` pour le reste de `core/` (audit, adresses, pays, devises, maintenance, file d'événements, courriels).

## Adresse de l'API

- Toutes les routes de l'API commencent par `/sni/api/v1`. Quelques pages publiques n'ont pas ce préfixe (`/verify/**`, `/public/**`).
- En développement : `http://localhost:8080`. La description OpenAPI est sur `/v3/api-docs` (avec un jeton) et l'interface Swagger sur `/swagger-ui.html`. Toutes deux sont désactivées en production.
- Les origines autorisées (CORS) sont configurées côté serveur (`CORS_ALLOWED_ORIGINS`). Un nouveau domaine de frontend doit y être ajouté.

## Connexion et jetons

1. `POST /sni/api/v1/auth/login` avec `{ "email": "...", "password": "..." }`. La réponse contient `accessToken`, `refreshToken`, `emailVerified` et `roles` (la liste des rôles **et** des droits du compte). Détails : [securite/README.md](securite/README.md).
2. Chaque appel porte l'en-tête `Authorization: Bearer <accessToken>`. En production, le jeton d'accès vit 15 minutes et le jeton de rafraîchissement 7 jours. En développement, le jeton d'accès vit 7 jours par défaut (`APP_JWT_ACCESS_EXPIRATION_MS`) : tester le rafraîchissement avec une durée courte avant de livrer.
3. Sur une réponse `401`, appeler `POST /sni/api/v1/auth/refresh` avec `{ "refreshToken": "..." }`, remplacer les jetons, puis rejouer l'appel une fois. Si le rafraîchissement échoue, renvoyer vers l'écran de connexion.
4. `POST /sni/api/v1/auth/logout` met fin à la session. `GET /sni/api/v1/auth/me` renvoie le compte connecté et ses droits.

Le serveur ne garde pas de session : tout passe par le jeton.

## Qui peut appeler quoi

Chaque route indique son accès sous l'une de ces formes :

| Accès | Règle |
| --- | --- |
| `public, sans jeton` | Aucune authentification. La route a souvent son propre contrôle : jeton de partage, signature, rappel d'un opérateur de paiement. |
| `tout compte connecté` | N'importe quel jeton valide. |
| `membre connecté au portail` | Chemins `/sni/api/v1/client/**` et `/sni/api/v1/portal/kyc/**`. Le compte doit avoir `ROLE_MEMBER`, être rattaché à un membre non supprimé, et ce membre doit avoir l'accès portail activé. Sinon : `403`. |
| `back-office : rôle d'administration et droit X` | Tout le reste de `/sni/api/v1/**`. Le compte doit avoir un rôle d'administration (liste dans [MODULES.md](MODULES.md)) **et** le droit `X`. Le droit s'écrit indifféremment `MODULE:ACTION` ou `MODULE_ACTION`. `ROLE_SUPER_ADMIN` passe cette règle sans condition. |
| `contrôle supplémentaire sur la méthode` | Une règle `@PreAuthorize` s'ajoute à la précédente. Elle s'applique à tout le monde, `ROLE_SUPER_ADMIN` compris : il doit porter le droit cité. |

Pour afficher ou masquer un bouton, comparer le droit exigé par la route à la liste `roles` reçue à la connexion (ou renvoyée par `/auth/me`). Le serveur reste juge : un refus renvoie `403`, avec le droit manquant dans `requiredPermission` quand la règle d'URL est en cause.

## Format des réponses

- **Pas d'enveloppe.** Une route renvoie directement l'objet décrit dans sa ligne « Réponse » : un objet, une liste, ou une page. Le type `ApiResponse<T>` existe dans le code mais aucune route ne l'utilise.
- **Page maison** `PaginatedResponse<T>` (123 routes) :

```json
{
  "data": [ { } ],
  "pageable": {
    "page": 0, "size": 20, "totalPages": 3, "totalElements": 47,
    "first": true, "last": false, "hasNext": true, "hasPrevious": false,
    "sort": { }
  }
}
```

- **Page Spring** `Page<T>` (17 routes) : objet de pagination de Spring Data, dont les éléments sont dans `content`. Sa forme exacte dépend de la configuration de Spring Data : la relever une fois sur une vraie réponse avant de l'exploiter.
- **Fichiers** : une réponse « fichier (octets) » renvoie le contenu brut (PDF, image, CSV…). Le nom proposé est dans l'en-tête `Content-Disposition`.
- **Vide** : « vide » signifie un corps vide, en général avec le statut `204` ou `200`.

## Paramètres de pagination

Les routes paginées acceptent `page` (à partir de `0`), `size` et `sort=champ,ASC|DESC` (répétable). La taille et le tri par défaut de chaque route sont indiqués dans sa table de paramètres.

## Identifiants : attention aux grands nombres

Les identifiants techniques (`id`, `assignedTo`, `createdBy`…) sont des entiers 64 bits, marqués **`entier int64 ⚠`** dans les modèles. Ils partent en **nombres JSON**, et certains dépassent `2^53` (exemple constaté : `7315645027609924000`). `JSON.parse` en JavaScript les **arrondit en silence**, et l'appel suivant cible alors un objet qui n'existe pas (`not found`).

Règles pour le frontend :

1. Préférer les **codes lisibles** quand la route en propose (`ticketNumber`, `walletNumber`, codes de contrat, de plan, de version…). La plupart des routes récentes n'exposent que des codes.
2. Quand un identifiant numérique est inévitable, lire le JSON avec un analyseur qui préserve les grands entiers (`lossless-json`, `json-bigint`, ou une conversion en texte des nombres de 16 chiffres et plus avant `JSON.parse`), et le garder **en texte** dans le frontend.
3. Dans un chemin (`/{id}`) ou un paramètre de requête, envoyer ce texte tel quel : le serveur le lit comme un nombre.
4. Un champ décrit « nombre envoyé en texte » arrive déjà en texte.

## Dates et heures

- Un champ « date-heure ISO 8601 UTC » (`Instant`) arrive en UTC, par exemple `2026-11-08T10:15:30.123Z`. **Dans un corps JSON envoyé au serveur**, il doit porter son fuseau (`Z` ou `+01:00`), sinon la requête est refusée en `400`.
- Dans un **paramètre de requête**, une date-heure sans fuseau est acceptée et lue dans le fuseau de l'application (`app.time-zone`).
- Un champ « date-heure locale sans fuseau » (`LocalDateTime`, par exemple les plages de réservation) s'écrit `2026-11-08T10:15:00`, sans `Z` ni décalage : c'est l'heure du lieu, à afficher telle quelle.
- Un champ « date AAAA-MM-JJ » (`LocalDate`) est une date sans heure, par exemple `2026-11-08`.
- Une heure seule (`LocalTime`, horaires d'ouverture) s'écrit `HH:mm` ou `HH:mm:ss`.

## Corps des requêtes

- **JSON strict.** Une propriété inconnue est refusée en `400` (`VALIDATION_FAILED`), et le message la nomme avec la liste des propriétés attendues. N'envoyer que les champs du modèle : pas de champ d'affichage, pas d'objet complet renvoyé tel quel.
- Une énumération vide (`""`) est refusée en `400` : omettre le champ ou envoyer `null`.
- **Fichiers** : les routes marquées `multipart/form-data` reçoivent leurs fichiers en parties de formulaire, avec un nom indiqué dans la table des paramètres. Taille maximale : **10 Mo** par fichier et par requête.

## Idempotence

Les routes qui créent un mouvement sensible (paiement, stock, portefeuille…) acceptent l'en-tête `Idempotency-Key`, indiqué sur la route (« conseillé » ou « obligatoire »). Le frontend génère une clé unique (UUID) **par intention de l'utilisateur**, la garde pendant les nouvelles tentatives du même envoi, et en crée une nouvelle pour une nouvelle action. Une même clé rejouée renvoie la première réponse au lieu de refaire l'opération. Une route « conseillé » peut devenir obligatoire par configuration du serveur (`IDEMPOTENCY_REQUIRED_OPERATIONS`) : l'envoyer toujours.

## Erreurs

Les erreurs métier et de validation ont toutes la même forme :

```json
{
  "errorCode": "VALIDATION_FAILED",
  "status": 400,
  "message": "Invalid request parameters.",
  "traceId": "3f0c...",
  "errors": ["email : must not be blank"]
}
```

- `message` : la phrase à afficher (souvent en français pour les règles métier).
- `errors` : le détail champ par champ, quand il existe.
- `traceId` : à afficher ou à journaliser, il permet de retrouver l'erreur dans les journaux du serveur.
- En développement seulement : `path`, `debugMessage`, `exceptionName`.

Codes d'erreur (`errorCode`) : `BAD_REQUEST`, `VALIDATION_FAILED`, `CONSTRAINT_VIOLATION`, `TYPE_MISMATCH`, `MISSING_PARAMETER`, `RESOURCE_NOT_FOUND`, `RESOURCE_ALREADY_EXISTS`, `CONFLICT`, `OPERATION_NOT_ALLOWED`, `UNAUTHORIZED`, `FORBIDDEN`, `ACCESS_DENIED`, `INSUFFICIENT_PRIVILEGES`, `INVALID_CREDENTIALS`, `BAD_CREDENTIALS`, `INVALID_TOKEN`, `TOKEN_EXPIRED`, `ACCOUNT_LOCKED`, `FILE_STORAGE_ERROR`, `FILE_SIZE_EXCEEDED`, `INVALID_FILE_FORMAT`, `UNSUPPORTED_MEDIA_TYPE`, `MEDIA_TYPE_NOT_SUPPORTED`, `METHOD_NOT_SUPPORTED`, `ENDPOINT_NOT_FOUND`, `SERVICE_UNAVAILABLE`, `INTERNAL_SERVER_ERROR`.

Les refus de la couche de sécurité ont une autre forme :

```json
{ "status": 401, "error": "Unauthorized", "message": "Authentication is required to access this resource.", "path": "/sni/api/v1/..." }
{ "status": 403, "error": "Forbidden", "message": "You do not have permission to access this resource.", "path": "/sni/api/v1/...", "requiredPermission": "BILLING:READ" }
```

Un trop grand nombre d'appels renvoie `429` avec `{ "errorCode": "TOO_MANY_REQUESTS", "status": 429, "message": "Too many requests. Please try again later." }` : attendre avant de réessayer.

## Temps réel

Un canal WebSocket STOMP est ouvert sur `/ws` (avec SockJS). La trame `CONNECT` doit porter l'en-tête `Authorization: Bearer <accessToken>`. Abonnements sur `/topic/...` et `/user/queue/...`, envois sur `/app/...`. Les sujets `/topic/admin/`, `/topic/inventory/` et `/topic/support/` sont réservés au back-office. Les sujets publiés sont décrits dans les README des modules qui les utilisent (`notification`, `support`, `inventory`).

## Régénérer

Après un changement de routes ou de DTO, relancer la génération décrite dans [tools/frontend-doc/README.md](../../tools/frontend-doc/README.md). Les README rédigés ne sont pas touchés : les relire si le fonctionnement a changé.
