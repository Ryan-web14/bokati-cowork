# Lot A0 : socle du centre d'audit

Branche `feature/centre-audit`. Migrations : dev `V360__centre_audit_a0_requete.sql`, prod `V354__centre_audit_a0_requete.sql`.

## Ce qui change

- **Identifiant de requête.** Chaque requête reçoit un identifiant :
  - celui de l'en-tête `X-Request-Id` s'il est sain (8 à 64 caractères, lettres, chiffres, `.`, `_`, `-`), sinon un nouvel identifiant ;
  - il est renvoyé dans la réponse (`X-Request-Id`) ;
  - il est posé dans le contexte des journaux (MDC `requestId`), avec l'adresse du client (`clientIp`) et le compte connecté (`user`) ;
  - il est enregistré dans chaque ligne d'audit (`audit_log.request_id`) et dans chaque événement de la boîte de sortie (`outbox_event.request_id`) ; le traitement d'un événement reprend l'identifiant de la requête qui l'a publié ;
  - une tâche asynchrone (`taskExecutor`) garde le contexte de la requête qui l'a lancée.
- **Adresse réelle du client.** L'adresse est résolue une fois par requête par `ClientIpResolver` (la valeur de droite de `X-Forwarded-For`, ajoutée par le proxy Heroku), et lue partout par `RequestContext.clientIp`. Avant, l'audit, les sessions, les signatures de contrats et de devis et les journaux d'accès lisaient soit l'adresse du proxy, soit la première valeur de `X-Forwarded-For`, que l'appelant écrit lui-même et peut donc falsifier.
- **Journaux.** Dev : `[requestId compte]` en tête de chaque ligne. Production : une ligne JSON (format logstash) par événement, avec `requestId`, `clientIp` et `user` ; variable `LOG_STRUCTURED_FORMAT` pour un autre format reconnu par Spring Boot (`ecs`, `gelf`).
- **Attribution.** Déjà juste : l'acteur de l'audit vient du jeton. Le mode « administrateur automatique » qui attribuait tout à un administrateur (relevé dans `docs/a-realiser/audit-securite.md`) a été retiré avant ce chantier, et un garde au démarrage refuse son réglage.

## Pour le frontend

- Lire l'en-tête `X-Request-Id` de chaque réponse et l'afficher avec un message d'erreur (« Référence : ... ») : il permet de retrouver la requête dans les journaux et l'audit.
- Le frontend peut envoyer son propre `X-Request-Id` (par exemple un identifiant d'action) ; il est gardé s'il est sain.

## Code

`core/observability` : `RequestContext` (constantes, lecture de l'identifiant et de l'adresse), `RequestContextFilter` (premier filtre), `RequestUserMdcFilter` (compte, après la sécurité), `MdcTaskDecorator` (tâches asynchrones). Modifiés : `AspectAudit`, `AuditLog`, `OutboxEvent`, `OutboxServiceImpl`, `AsyncConfig`, et les lectures d'adresse (`UserSessionServiceImpl`, signatures de contrats et de devis, documents, portefeuille, mobile money, authentification).

## Tests

`RequestContextA0Test` (identifiant gardé ou créé, en-tête de réponse, MDC limité à la requête, adresse du proxy et non de l'appelant, tâche asynchrone) ; tests existants de l'adresse et de la limitation ; `BillingElementR1IT` (schéma validé avec la migration sur une vraie base).
