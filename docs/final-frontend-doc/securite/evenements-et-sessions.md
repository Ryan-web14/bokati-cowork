# Événements de sécurité et sessions (centre d'audit, lot A2)

> Rédigé d'après le code (16/11/2026). Routes exactes : `routes/SecurityAuditController.md`. Droit `SYSTEM:AUDIT`, donné à `SUPER_ADMIN`, `ADMIN` et `AUDITOR`.

## Événements de sécurité

`GET /sni/api/v1/admin/audit/security-events` : page d'événements, du plus récent au plus ancien.

| Filtre | Effet |
| --- | --- |
| `type` | Un ou plusieurs types séparés par des virgules |
| `login` | Identifiant saisi (courriel), recherche partielle sans casse |
| `userCode` | Compte (`USR-...`) |
| `ip` | Adresse exacte du client |
| `requestId` | Requête (en-tête `X-Request-Id`), commune aux journaux et au journal d'audit |
| `from`, `to` | Période (`2026-11-16T00:00:00Z`), 366 jours au plus (`PERIODE_TROP_LONGUE`) |
| `page`, `size` | Défaut 0 et 50, 200 au plus |

Un événement : `{ id, createdAt, eventType, userCode, login, ipAddress, userAgent, requestId, path, detailsJson }`. `detailsJson` est un objet JSON en texte.

| Type | Quand | Détail |
| --- | --- | --- |
| `LOGIN_SUCCESS` | Connexion réussie | `method` : `PASSWORD` ou `ONE_TIME_CODE` |
| `LOGIN_FAILURE` | Identifiants faux, compte inconnu ou désactivé, code faux | `method`, `reason` |
| `LOGIN_REFUSED_LOCKED` | Connexion sur un compte verrouillé | `method` |
| `LOGIN_REFUSED_BACK_OFFICE` | Compte sans rôle d'équipe sur l'espace d'administration | |
| `ACCOUNT_LOCKED` | Trop d'échecs | `failedAttempts`, `lockedUntil` (ou `manuel`) |
| `ACCOUNT_UNLOCKED` | Déverrouillage | `by` : `SELF` ou le courriel de l'administrateur |
| `REFRESH_TOKEN_REUSE` | Jeton de rafraîchissement déjà utilisé présenté de nouveau : toutes les sessions du compte sont fermées | `revokedAt` |
| `LOGOUT` | Déconnexion | |
| `SESSION_REVOKED` | Session fermée par un administrateur | `sessionId` ou `scope: ALL`, `reason`, `by` |
| `PASSWORD_RESET_REQUESTED`, `PASSWORD_RESET_COMPLETED`, `PASSWORD_RESET_BY_ADMIN` | Mot de passe | `triggeredBy`, `generated` |
| `ACCESS_DENIED` | Refus d'accès (403) | `reason` : `ROUTE`, `METHOD` ou `SERVICE` |
| `RATE_LIMITED` | Trop de requêtes (429) | `rule`, `limit`, `windowSeconds` |

`ACCESS_DENIED` et `RATE_LIMITED` sont écrits au plus une fois par minute pour une même adresse, une même route et un même compte : une rafale ne remplit pas le journal.

## Sessions d'un utilisateur

- `GET /sni/api/v1/admin/audit/users/{userCode}/sessions?activeOnly=true` : `{ sessionId, createdAt, lastAccessAt, expiredAt, active, revoked, ipAddress, deviceType, userAgent, location, roles, revokedAt, revokedBy, revokedReason }`. `location` : ville et pays estimés de l'adresse à la connexion. `activeOnly=false` donne aussi les sessions fermées.
- `DELETE /sni/api/v1/admin/audit/sessions/{sessionId}?reason=...` : ferme une session. Ses jetons (accès et rafraîchissement) cessent aussitôt de fonctionner.
- `DELETE /sni/api/v1/admin/audit/users/{userCode}/sessions?reason=...` : ferme toutes les sessions ouvertes du compte ; réponse `{ "revoked": n }`.

## Écran conseillé

- Journal de sécurité filtrable, avec des pastilles par type (échecs et refus en rouge), et le lien de l'identifiant de requête vers le journal d'audit.
- Sur la fiche d'un utilisateur : ses sessions ouvertes (appareil, lieu, dernière activité) avec « Fermer » et « Fermer toutes », et ses derniers événements de sécurité (`userCode`).
