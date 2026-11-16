# Lot A3 : trafic

Branche `feature/centre-audit`. Migrations : dev `V364__centre_audit_a3_trafic.sql`, prod `V358__centre_audit_a3_trafic.sql`.

## Ce qui change

- **Mesure de chaque requête** (`TrafficFilter`, juste après l'identifiant de requête et avant Spring Security, donc refus 401, 403 et 429 compris). Par minute et par route : nombre, erreurs 4xx et 5xx, durée totale et maximale, histogramme des durées (18 tranches de 5 ms à 30 s, plus une au-delà). La route est le gabarit trouvé par Spring MVC (`/sni/api/v1/billing/documents/{documentNumber}`), jamais l'adresse brute ; une requête refusée par la sécurité avant le routage est notée `(refusee avant la route)`, une adresse inconnue `(sans route)`. Non mesurés : WebSocket et SockJS (`/ws`), Actuator.
- **En mémoire, écrit chaque minute** (`TrafficRecorder`, `OperationsWriter`) : seules les minutes terminées sont écrites ; deux écritures de la même minute (deux dynos, arrêt) s'additionnent.
- **Requêtes gardées une par une** (`request_log`) : lentes (`SLOW`, seuil `bokati.audit.traffic.slow-ms`, 2 000 ms, modifiable à chaud) et en erreur serveur (`ERROR`), avec l'identifiant de requête, l'adresse, la route, le statut, la durée, l'utilisateur et l'adresse du client.
- **Conservation** : minutes 7 jours (`traffic_minute`), cumul horaire 13 mois (`traffic_hour`, recalculé chaque heure pour les trois dernières heures, sans doublon, tâche `TrafficService.rollupHours` du dyno désigné), requêtes une par une 30 jours. La purge est faite par le lot A7.
- **Routes** (`TrafficController`, droit `SYSTEM:AUDIT`) : `GET /admin/audit/traffic` (série : requêtes, 4xx, 5xx, durée moyenne, p50, p95, maximale), `GET /admin/audit/traffic/routes` (100 routes au plus, triées par nombre, erreurs ou lenteur), `GET /admin/audit/traffic/requests` (requêtes gardées). Les percentiles sont lus dans l'histogramme : borne haute de la tranche, la durée maximale au-delà de la dernière.
- Nouveau domaine de paramètres `AUDIT` (« Centre d'audit », `SUPER_ADMIN`), avec `bokati.audit.traffic.slow-ms` et `bokati.audit.export.max-rows`.
- Les métriques Actuator ne sont pas exposées : les routes ci-dessus les remplacent, sans ouvrir de point d'accès.

## Vérifié

En conteneur avec l'application complète : requêtes comptées par gabarit, refus de la sécurité comptés, écriture de la minute, tâche de relecture des paramètres suivie sous son nom.

## Tests

`TrafficA3Test` (route en gabarit, erreur serveur gardée avec l'utilisateur, refus et adresse inconnue, WebSocket ignoré, minutes terminées seules, percentiles) ; `TrafficA3IT` (vraie base : minutes additionnées, cumul horaire sans doublon, série, routes, requêtes gardées, schéma validé).
