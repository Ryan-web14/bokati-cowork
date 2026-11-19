# Super-administrateur par invitation à usage unique

> 19/11/2026, `hotfix/super-admin-initial`. Doc frontend : [securite/super-admin-invitation.md](../../final-frontend-doc/securite/super-admin-invitation.md).

## Besoin

L'exploitant est `ADMIN` en production et ne peut pas se donner plus de droits (`PrivilegeGuard`). Il faut créer un `SUPER_ADMIN` sans ouvrir de porte permanente.

## Conception

- Table `super_admin_invitation` (migration dev V398, prod V392) ; une ligne `PENDING` pour `admin@sni-cg.com`, insérée seulement si absente.
- `SuperAdminInvitationService.issueOnLogin` : appelé après une connexion réussie (`AuthenticationServiceImpl.createSessionAndResponse`, dans sa propre transaction, sans jamais faire échouer la connexion). Jeton aléatoire de 32 octets, empreinte SHA-256 en base, 30 minutes, statut `ISSUED`, compteur d'émissions ; lien dans `LoginResponse.superAdminSetupUrl` et par courriel.
- `SuperAdminSetupFormController` (`/sni/api/v1/auth/super-admin-setup/form`, chemin public) : page Thymeleaf avec les protections de la page de réinitialisation (CSRF double cookie, CSP à nonce, `no-referrer`, `no-store`).
- `complete` : verrou pessimiste sur l'invitation, contrôles (lien valide, adresse libre, mot de passe), création, rôle `SUPER_ADMIN` attribué par `INVITATION:<adresse invitée>`, activation, invitation `USED`, empreinte effacée, adresse IP et compte créé gardés. Journal en `WARN` à l'émission et à l'usage.

## Pour rouvrir une invitation plus tard

Insérer une nouvelle ligne `PENDING` pour un compte précis (nouvelle migration ou requête SQL de l'exploitant).

## Tests

`SuperAdminInvitationServiceTest` : lien réservé au compte invité, seule l'empreinte gardée, création unique puis invitation close, lien expiré ou inconnu refusé, adresse existante refusée.
