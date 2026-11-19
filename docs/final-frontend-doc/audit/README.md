# Centre d'audit : écrans du back-office

> Rédigé d'après le code (16/11/2026, lots A0 à A7). Routes exactes, paramètres et champs JSON : [socle/routes.md](../socle/routes.md) et les fichiers `socle/routes/<Contrôleur>.md` cités dans chaque page. Règles communes (connexion, erreurs, pagination) : [README racine](../README.md).

Un centre unique où l'exploitant et l'auditeur voient **qui a fait quoi** (actions avec avant / après), **les connexions et les refus** (sécurité), **ce que fait l'application** (tâches planifiées, erreurs serveur, boîte d'envoi, intégrations), **le trafic** (requêtes, temps de réponse), le tout sur **une chronologie commune**, avec des **alertes**.

## Droit et accès

- Toutes les routes exigent `SYSTEM:AUDIT`, donné à `SUPER_ADMIN`, `ADMIN` et `AUDITOR` (décision du 14/11/2026 : tous les administrateurs voient tout le centre). Masquer l'entrée de menu sans ce droit ; un appel sans lui répond `403` avec `requiredPermission: "SYSTEM:AUDIT"`.
- Les réglages (seuils d'alerte, destinataires, durées de conservation) sont des paramètres du domaine `AUDIT`, modifiables par `SUPER_ADMIN` et `ADMIN` dans l'écran des paramètres ([parametres/README.md](../parametres/README.md)). Les 24 clés du domaine : [parametres/cles.md](../parametres/cles.md#audit). Lien direct vers un paramètre : `GET /sni/api/v1/admin/settings/{key}` (le `domain` rendu ouvre l'onglet `AUDIT`) ; liste du domaine : `GET /sni/api/v1/admin/settings?domain=AUDIT`. Afficher le lien « Modifier » pour `ROLE_SUPER_ADMIN` et `ROLE_ADMIN`.
- **Le centre s'audite lui-même** : chaque consultation des listes (journal, sécurité, chronologie, erreurs, requêtes, alertes), chaque export, chaque accusé de lecture et chaque vérification ajoute une ligne au journal des actions (module `AUDIT`).

## Écrans et pages de cette doc

| Écran conseillé | Page | Routes |
| --- | --- | --- |
| Tableau de bord et alertes | [alertes.md](alertes.md) | `/admin/audit/dashboard`, `/admin/audit/alerts...` |
| Chronologie (utilisateur, élément, requête) | [chronologie.md](chronologie.md) | `/admin/audit/timeline` |
| Journal des actions (avant / après) | [journal-actions.md](journal-actions.md) | `/admin/audit-logs` |
| Sécurité : connexions, refus, sessions | [../securite/evenements-et-sessions.md](../securite/evenements-et-sessions.md) | `/admin/audit/security-events`, `/admin/audit/users/{userCode}/sessions` |
| Santé, tâches planifiées, erreurs serveur | [exploitation.md](exploitation.md) | `/admin/audit/health`, `/jobs`, `/errors` |
| Trafic | [trafic.md](trafic.md) | `/admin/audit/traffic...` |
| Intégrité et conservation | [integrite-conservation.md](integrite-conservation.md) | `/admin/audit/integrity`, `/admin/audit/retention` |

Erreurs propres au centre : [erreurs.md](erreurs.md).

## Identifiant de requête

Chaque réponse de l'API porte l'en-tête `X-Request-Id` (reçu du frontend s'il en envoie un sain, sinon créé). Il se retrouve dans le journal des actions, les événements de sécurité, les erreurs serveur, les requêtes gardées et la boîte d'envoi. Conseil : afficher l'identifiant dans les messages d'erreur du frontend (« Référence : ... ») et en faire partout un lien vers la chronologie filtrée sur la requête.

## Navigation conseillée

- Depuis une ligne de n'importe quel journal : liens « Chronologie de l'utilisateur » (`user`), « Chronologie de l'élément » (`entityCode`), « Que s'est-il passé dans cette requête » (`requestId`).
- Depuis la fiche d'un utilisateur, d'une facture, d'une réservation, d'un membre... : onglet « Historique » qui appelle la chronologie filtrée sur ce code.
