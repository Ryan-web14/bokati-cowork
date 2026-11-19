# Intégrité et conservation (lot A7)

Routes : [socle/routes/RetentionController.md](../socle/routes/RetentionController.md).

## Intégrité

Le journal des actions et les événements de sécurité sont **chaînés par empreinte** (chaque ligne porte l'empreinte SHA-256 de son contenu et de la ligne précédente) et **en ajout seul** : la base refuse toute modification, et toute suppression hors de la purge de conservation. Le chaînage commence au déploiement du lot A7 : les lignes plus anciennes ne sont pas chaînées.

`GET /sni/api/v1/admin/audit/integrity` (journalisé) : un rapport par journal :

```json
[
  { "journal": "actions", "checked": 18233, "firstSeq": 1, "lastSeq": 18233, "unchained": 52011, "intact": true, "breaks": [] },
  { "journal": "security", "checked": 4120, "firstSeq": 1, "lastSeq": 4120, "unchained": 0, "intact": false,
    "breaks": [ { "chainSeq": 3017, "kind": "CONTENT" } ] }
]
```

- `intact: false` : au moins une rupture. `CONTENT` : la ligne a été modifiée ; `LINK` : une ligne a été insérée ou supprimée avant elle. 20 ruptures au plus.
- `unchained` : lignes antérieures au chaînage.
- La vérification relit tout le journal : prévoir quelques secondes, un bouton « Vérifier » plutôt qu'un appel automatique, et un bandeau rouge si `intact` est faux.

## Conservation

`GET /sni/api/v1/admin/audit/retention` : `[{ name, table, settingKey, days, rows, oldest }]`.

| `name` | Journal | Durée par défaut |
| --- | --- | --- |
| `actions` | journal des actions | 1 825 jours (5 ans) |
| `security` | événements de sécurité | 365 jours |
| `requests` | requêtes gardées une par une | 30 jours |
| `trafficMinutes` | trafic par minute | 7 jours |
| `trafficHours` | trafic par heure | 395 jours (13 mois) |
| `errors` | erreurs serveur | 90 jours |
| `jobRuns` | exécutions de tâches gardées | 90 jours |
| `alerts` | alertes fermées | 365 jours |

- Les durées sont des paramètres (`settingKey`, domaine `AUDIT`, `0` : sans limite), ouverts par `GET /sni/api/v1/admin/settings/{settingKey}` (liste : [parametres/cles.md](../parametres/cles.md#audit)). La purge passe chaque nuit à 3 h 30.
- `POST /sni/api/v1/admin/audit/retention/run` (journalisé) : purge immédiate ; rend `{ "actions": 0, "requests": 1204, ... }` (lignes supprimées par journal). Demander confirmation avant l'appel.
