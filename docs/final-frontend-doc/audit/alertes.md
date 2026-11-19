# Tableau de bord et alertes (lot A6)

Routes : [socle/routes/AlertController.md](../socle/routes/AlertController.md).

## Tableau de bord

`GET /sni/api/v1/admin/audit/dashboard` :

```json
{
  "checkedAt": "2026-11-16T14:05:00Z",
  "indicators": {
    "ERROR_RATE": { "value": 0.4, "threshold": 5, "detail": "2 erreurs sur 512 requêtes" },
    "TASK_FAILURES": { "value": 0, "threshold": 3, "detail": null }
  },
  "trafficLastHour": { "requests": 4210, "client_errors": 37, "server_errors": 2, "requests_per_minute": 70.2 },
  "lockedAccounts": 1,
  "failedLogins24h": 14,
  "openAlerts": 0
}
```

Une carte par indicateur : valeur, seuil, barre ou couleur selon `value / threshold` ; un seuil à `0` signifie règle coupée (griser). Rafraîchir toutes les minutes, page visible seulement. Les indicateurs sont ceux des règles ci-dessous.

## Règles

`GET /sni/api/v1/admin/audit/alerts/rules` : `[{ code, settingKey, threshold, enabled, severity, title, currentValue, detail }]`.

| `code` | Mesure | Seuil par défaut | `severity` |
| --- | --- | --- | --- |
| `ERROR_RATE` | % de requêtes en erreur serveur, 5 dernières minutes (20 requêtes au moins) | 5 | `CRITICAL` |
| `SERVER_ERRORS` | erreurs serveur, 15 minutes | 10 | `CRITICAL` |
| `SLOW_RESPONSES` | durée (ms) sous laquelle répondent 95 % des requêtes, 15 minutes (50 requêtes au moins) | 3 000 | `WARNING` |
| `FAILED_LOGINS` | échecs de connexion, 15 minutes | 20 | `WARNING` |
| `LOCKED_ACCOUNTS` | comptes verrouillés, 1 heure | 3 | `WARNING` |
| `OUTBOX_DEAD` | messages abandonnés de la boîte d'envoi, 1 heure | 1 | `CRITICAL` |
| `OUTBOX_WAITING` | attente du plus ancien message (minutes) | 15 | `CRITICAL` |
| `TASK_FAILURES` | échecs consécutifs d'une tâche planifiée | 3 | `WARNING` |
| `EMAIL_FAILURES` | courriels en échec, 1 heure | 5 | `WARNING` |
| `WHATSAPP_FAILURES` | messages WhatsApp refusés ou non remis, 1 heure | 5 | `WARNING` |
| `PAWAPAY_FAILURES` | rappels PawaPay en échec, 1 heure | 3 | `WARNING` |

Les seuils se changent dans l'écran des paramètres (clé `settingKey`, domaine `AUDIT`, `SUPER_ADMIN`). Prévoir un lien « Modifier le seuil » vers ce paramètre : `GET /sni/api/v1/admin/settings/{settingKey}` rend sa valeur et son domaine (`AUDIT`) pour ouvrir l'écran des paramètres sur la bonne ligne, `PUT /sni/api/v1/admin/settings/{settingKey}` le modifie. Lien réservé à `ROLE_SUPER_ADMIN`.

## Alertes

- `GET /sni/api/v1/admin/audit/alerts?status=&rule=&from=&to=&page=0&size=50` : page d'alertes, de la plus récente à la plus ancienne. `status` : `OPEN`, `ACKNOWLEDGED`, `RESOLVED`.
- `GET /sni/api/v1/admin/audit/alerts/{id}`.
- Une alerte : `{ id, ruleCode, severity, status, title, message, metricValue, threshold, openedAt, lastSeenAt, resolvedAt, acknowledgedAt, acknowledgedBy, acknowledgeNote, notifications }`.
- Cycle : `OPEN` à l'ouverture (mesure au seuil) → `ACKNOWLEDGED` après un accusé de lecture → `RESOLVED` **seule**, quand la mesure redescend. Une seule alerte en cours par règle ; tant qu'elle dure, `metricValue`, `message` et `lastSeenAt` sont tenus à jour sans nouvel envoi.
- `notifications` : compte rendu des envois à l'ouverture, par exemple `APP ok, EMAIL 2/2, WHATSAPP 0/1 (Aucun modele WhatsApp approuve ...)`.
- `POST /sni/api/v1/admin/audit/alerts/{id}/acknowledge` avec `{ "note": "Je regarde" }` (facultatif) : accusé de lecture, journalisé.
- `POST /sni/api/v1/admin/audit/alerts/evaluate` : évaluation immédiate (sinon chaque minute) ; rend les règles avec leur valeur.

## Alertes en direct

À l'ouverture d'une alerte, un message part sur le sujet WebSocket `/topic/admin/alerts` (connexion STOMP du back-office) : `{ alertType, module: "AUDIT", title, message, severity, payloadJson: "{\"reference\":\"ALR-12\"}", createdAt }`. Afficher un bandeau (rouge pour `CRITICAL`) avec un lien vers l'alerte (`ALR-<id>`). Les administrateurs listés dans `bokati.audit.alerts.email-recipients` reçoivent aussi une notification enregistrée (cloche, `GET /admin/notifications/unread`).

## Canaux et destinataires

Paramètres du domaine `AUDIT` : `bokati.audit.alerts.channels` (`APP,EMAIL,WHATSAPP`), `bokati.audit.alerts.email-recipients` (courriels), `bokati.audit.alerts.whatsapp-recipients` (numéros). Le courriel suit le modèle « Courriel : alerte d'administration » (modifiable dans l'éditeur de courriels). Le WhatsApp suit le modèle `alerte_centre_audit`, à créer et faire approuver par Meta avant le premier envoi.
