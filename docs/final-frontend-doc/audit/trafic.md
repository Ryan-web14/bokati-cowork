# Trafic (lot A3)

Routes : [socle/routes/TrafficController.md](../socle/routes/TrafficController.md).

Chaque requête de l'API est mesurée (refus 401, 403 et 429 compris ; pas les WebSocket). Les données sont écrites chaque minute : la minute en cours n'apparaît qu'à la minute suivante.

## Série

`GET /sni/api/v1/admin/audit/traffic?from=&to=&granularity=&route=&method=` : `[{ period, requests, clientErrors, serverErrors, averageMs, p50Ms, p95Ms, maxMs }]`.

- Période par défaut : les dernières 24 heures. `granularity` : `MINUTE` (7 derniers jours seulement), `HOUR` ou `DAY` ; sans elle : minute jusqu'à 6 heures, heure jusqu'à 7 jours, jour au-delà. 2 000 points au plus.
- Seules les périodes avec du trafic sont renvoyées : compléter à zéro côté écran.
- `route` : gabarit exact (`/sni/api/v1/billing/documents/{documentNumber}`), comme renvoyé par la liste des routes. `method` : `GET`, `POST`...
- `p50Ms` et `p95Ms` sont lus dans un histogramme par tranches (5, 10, 25, 50, 75, 100, 150, 200, 300, 500, 750, 1 000, 1 500, 2 000, 3 000, 5 000, 10 000, 30 000 ms) : ce sont des bornes de tranche, pas des valeurs exactes. Afficher « moins de 300 ms ».

Écran conseillé : courbe des requêtes par période, erreurs 4xx et 5xx empilées, courbe du p95 sur un second axe.

## Routes

`GET /sni/api/v1/admin/audit/traffic/routes?from=&to=&sort=` : 100 routes au plus, `{ method, route, requests, clientErrors, serverErrors, averageMs, p50Ms, p95Ms, maxMs }`. `sort` : `requests` (défaut), `errors`, `slow`. Deux routes spéciales : `(refusee avant la route)` (refus de la sécurité avant le routage) et `(sans route)` (adresse inconnue).

## Requêtes gardées

`GET /sni/api/v1/admin/audit/traffic/requests?from=&to=&reason=&route=&user=&requestId=&minStatus=&page=0&size=50` : requêtes lentes (`reason` `SLOW`, à partir de `bokati.audit.traffic.slow-ms`, 2 000 ms) et en erreur serveur (`ERROR`), gardées 30 jours : `{ id, createdAt, requestId, method, path, route, status, durationMs, userEmail, clientIp, reason }`. `route` et `user` : « contient ».
