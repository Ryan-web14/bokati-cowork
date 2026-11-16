# Lot A7 : conservation, intégrité, confidentialité

Branche `feature/centre-audit`. Migrations : dev `V366__centre_audit_a7_integrite.sql`, prod `V360__centre_audit_a7_integrite.sql`.

## Ce qui change

- **Chaînage par empreinte** du journal des actions (`audit_log`) et des événements de sécurité (`security_event`), comme les journaux des contrats et de la conformité : à l'insertion, un déclencheur de la base (sous verrou, donc sûr avec plusieurs dynos) donne à la ligne son rang (`chain_seq`), l'empreinte précédente (`previous_hash`) et la sienne (`current_hash`, SHA-256 du contenu et de l'empreinte précédente, fonctions `audit_log_hash` et `security_event_hash`). Le chaînage commence au déploiement : les lignes antérieures ne sont pas reprises (une reprise allongerait le démarrage).
- **Ajout seul** : la base refuse toute modification d'une ligne de ces deux journaux, et toute suppression hors de la purge de conservation (`SET LOCAL bokati.audit_purge = 'on'` dans sa transaction). La purge totale de maintenance (`TRUNCATE`) reste possible.
- **Vérification** : `GET /admin/audit/integrity` recalcule chaque empreinte et relie chaque ligne à la précédente ; une ligne modifiée (`CONTENT`) ou un lien rompu par une insertion ou une suppression (`LINK`) est signalé (20 au plus). La première ligne gardée n'est pas reliée à la précédente, qui a pu être purgée. La vérification est journalisée.
- **Conservation** (`AuditRetention`, chaque nuit à 3 h 30 sur le dyno désigné, par lots de 5 000) : actions 5 ans, sécurité 1 an, requêtes une par une 30 jours, trafic par minute 7 jours, trafic par heure 13 mois, erreurs 90 jours, exécutions de tâches 90 jours, alertes fermées 1 an ; paramètres `bokati.audit.retention.*-days` (domaine `AUDIT`, `0` : sans limite). Pour un journal chaîné, la purge retire un début de chaîne (tout ce qui précède la première ligne à garder), jamais une ligne au milieu. `GET /admin/audit/retention` : durée, nombre de lignes et plus ancienne ligne de chaque journal ; `POST /admin/audit/retention/run` : application immédiate, journalisée.
- **Masquage centralisé** (`AuditValues`) : en plus de l'avant / après (lot A1), les paramètres d'adresse sensibles (`token`, `code otp`, `apiKey`...) sont écrits `***` dans les métadonnées du journal des actions.
- **Consultations journalisées** (module `AUDIT`) : liste du journal des actions (`VIEW_AUDIT_LOGS`), événements de sécurité (`VIEW_SECURITY_EVENTS`), sessions d'un utilisateur (`VIEW_USER_SESSIONS`), chronologie (`VIEW_TIMELINE`), erreurs serveur (`VIEW_ERRORS`, `VIEW_ERROR`), requêtes gardées (`VIEW_REQUESTS`), alertes (`VIEW_ALERTS`), en plus des exports, de l'accusé de lecture et de la vérification. Le tableau de bord, la santé et les tâches, rafraîchis souvent, ne le sont pas.

## Tests

`IntegrityA7IT` (vraie base : chaînage à l'insertion, modification et suppression refusées, altération détectée, suppression au milieu détectée, événements de sécurité chaînés, conservation par lots, purge d'un journal chaîné, masquage des paramètres d'adresse, schéma validé) ; tests des lots A2 et A5 adaptés (suppression par la purge).
