# Lot A4 : événements applicatifs

Branche `feature/centre-audit`. Migrations : dev `V363__centre_audit_a4_operations.sql`, prod `V357__centre_audit_a4_operations.sql`.

## Ce qui change

- **Tâches planifiées.** Le planificateur (`AsyncConfig.taskScheduler`) a un décorateur (`ScheduledRunDecorator`) : chaque exécution de chaque tâche (`@Scheduled` et `SchedulingConfigurer`, environ 90) est comptée et chronométrée ; un échec arrive par le gestionnaire d'erreurs du planificateur (`ScheduledRuns.fail`). Les cumuls restent en mémoire et sont écrits chaque minute par lots (`OperationsWriter`, sur chaque instance, hors de la planification, plus une écriture à l'arrêt).
  - `scheduled_task_status` : une ligne par tâche (dernière exécution, résultat, erreur, nombre d'exécutions et d'échecs, échecs consécutifs, durée moyenne et maximale, instance).
  - `scheduled_task_run` : une ligne par exécution **en échec** (`FAILURE`), **lente** (30 s et plus, `SLOW`) ou d'une tâche **rare** (rien depuis dix minutes, `PERIODIC`) : les tâches quotidiennes et horaires gardent toutes leurs exécutions, les tâches de quelques secondes seulement leurs échecs.
  - Nom d'une tâche : `Classe.methode` pour un `@Scheduled`, `Classe` pour une tâche d'un `SchedulingConfigurer`. Deux tâches enregistrées par la même classe partagent ce nom.
- **Erreurs serveur.** Chaque réponse 5xx (`ErrorResponse.build`) est gardée dans `app_error` : identifiant de trace et de requête, méthode, route, statut, code, exception, message, cause première, trace (8 000 caractères), utilisateur, adresse du client. Écriture hors du fil de la requête ; la même erreur sur la même route au plus une fois toutes les dix secondes.
- **Santé** : `GET /admin/audit/health` réunit les indicateurs de santé de Spring (base, Redis, RabbitMQ, disque, courriel), la boîte de sortie (nombre par statut, âge de la plus ancienne en attente), les intégrations sur 24 heures (courriels par statut, WhatsApp par groupe de statut, rappels PawaPay par résultat), les tâches en échec et les erreurs serveur de la dernière heure et des dernières 24 heures. `status` : `DOWN` (un service en panne), `WARNING` (message mort dans la boîte de sortie, attente de plus de 15 minutes, tâche en échec, erreur serveur dans l'heure) ou `UP`. Aucun appel aux services externes (Infobip, PawaPay) : leur état se lit dans leurs journaux.
- **Routes** (`OperationsController`, droit `SYSTEM:AUDIT`) : `GET /admin/audit/health`, `GET /admin/audit/jobs`, `GET /admin/audit/jobs/runs`, `GET /admin/audit/errors`, `GET /admin/audit/errors/{id}` (la trace seulement ici).
- Écritures en JDBC direct (tables techniques, hors du journal des actions).

## Limites

- Le nombre d'éléments traités par une exécution n'est pas connu : les tâches ne le rendent pas.
- Une tâche lancée hors du planificateur (fil créé à la main) n'est pas suivie.

## Tests

`ScheduledRunsA4Test` (nom tiré de la description, comptage et échecs avec le vrai planificateur de Spring, première exécution gardée) ; `OperationsA4IT` (vraie base : cumuls d'une écriture à l'autre, échecs consécutifs remis à zéro, exécutions relues, erreurs gardées avec cause et trace, limitation en rafale, santé, schéma validé).
