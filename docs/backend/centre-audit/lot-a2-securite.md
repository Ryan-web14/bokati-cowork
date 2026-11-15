# Lot A2 : sécurité et utilisateurs

Branche `feature/centre-audit`. Migrations : dev `V361__centre_audit_a2_securite.sql`, prod `V355__centre_audit_a2_securite.sql`.

## Ce qui change

- **Verrouillage de compte réparé.** Il ne fonctionnait pas : `login` était `@Transactional` sans `noRollbackFor`, et l'exception qui répond 401 annulait l'incrément des échecs et le verrou. Il est maintenant enregistré, pour `app.security.failed-login.lock-minutes` minutes (paramètre à chaud, 30 par défaut, 0 : jusqu'au déverrouillage manuel ; colonne `users.locked_until`). Un verrou échu ne bloque plus : la connexion suivante le lève, et un échec après l'échéance repart de zéro.
- **Événements de sécurité** (table `security_event`) : types dans `docs/final-frontend-doc/securite/evenements-et-sessions.md`. Chaque événement porte l'adresse réelle du client, le navigateur, la route et l'identifiant de requête. Il est écrit dans sa propre transaction : il reste même quand l'opération qui l'a produit est annulée (échec de connexion). Une écriture en échec n'interrompt jamais l'opération. Refus d'accès et limitations : au plus un par minute pour une même clé.
- **Sessions** : liste, fermeture d'une session ou de toutes, avec date, auteur et motif (`user_sessions.revoked_at`, `revoked_by`, `revoked_reason`). Une session fermée coupe ses jetons d'accès (vérifiés par le filtre) et de rafraîchissement.
- **Droit** : `SYSTEM:AUDIT` donné aussi à `ADMIN` (décision du 14/11/2026 : tous les administrateurs voient le centre).
- Les changements de rôles et de droits restent tracés dans le journal d'audit (`@Audited`) ; le centre unifié (lot A5) les montrera avec ces événements.

## Points d'appel

`AuthenticationServiceImpl` (connexion par mot de passe et par code, refus, déconnexion, réutilisation d'un jeton, demande de réinitialisation), `UserServiceImpl` (verrou, déverrouillage par le membre), `UserAdminServiceImpl` (déverrouillage et réinitialisation par un administrateur), `PasswordResetServiceImpl` (réinitialisation terminée), `SecurityConfig` et `GlobalExceptionHandler` (403), `RateLimitingFilter` (429).

## Code

`core/audit/security` : `SecurityEvent`, `SecurityEventType`, `SecurityEventRepository`, `SecurityEvents` (écriture), `SecurityAuditService` et `SecurityAuditController` (`/admin/audit/security-events`, `/admin/audit/users/{userCode}/sessions`, `/admin/audit/sessions/{sessionId}`).

## Tests

`SecurityA2Test` (verrou de 30 minutes après trop d'échecs, verrou échu, transaction de connexion, écriture limitée en rafale) ; `SecurityEventsA2IT` (événement gardé malgré l'annulation de l'opération, schéma validé sur une vraie base) ; tests existants de sécurité, de connexion et d'administration des comptes (97 au total).

## Limites

- La géolocalisation n'est portée que par les sessions (connexions réussies), pas par chaque événement.
- La dernière activité d'une session n'est mise à jour qu'au rafraîchissement du jeton.
