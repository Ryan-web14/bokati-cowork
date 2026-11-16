# Lot A6 : tableaux de bord et alertes

Branche `feature/centre-audit`. Migrations : dev `V365__centre_audit_a6_alertes.sql`, prod `V359__centre_audit_a6_alertes.sql`.

## Ce qui change

- **Règles à seuil** (`AlertRule`), évaluées chaque minute sur le dyno désigné (`AlertService.evaluate`). Chaque seuil est un paramètre modifiable à chaud du domaine `AUDIT` ; `0` coupe la règle.

| Règle | Mesure | Seuil par défaut | Gravité |
| --- | --- | --- | --- |
| `ERROR_RATE` | % de requêtes en erreur serveur sur 5 minutes (20 requêtes au moins) | 5 % | critique |
| `SERVER_ERRORS` | erreurs serveur en 15 minutes | 10 | critique |
| `SLOW_RESPONSES` | p95 des requêtes sur 15 minutes (50 requêtes au moins) | 3 000 ms | attention |
| `FAILED_LOGINS` | échecs de connexion en 15 minutes | 20 | attention |
| `LOCKED_ACCOUNTS` | comptes verrouillés dans l'heure | 3 | attention |
| `OUTBOX_DEAD` | messages abandonnés de la boîte d'envoi dans l'heure | 1 | critique |
| `OUTBOX_WAITING` | attente du plus ancien message (minutes) | 15 | critique |
| `TASK_FAILURES` | échecs consécutifs d'une tâche planifiée | 3 | attention |
| `EMAIL_FAILURES` | courriels en échec dans l'heure | 5 | attention |
| `WHATSAPP_FAILURES` | messages WhatsApp refusés ou non remis dans l'heure | 5 | attention |
| `PAWAPAY_FAILURES` | rappels PawaPay en échec dans l'heure | 3 | attention |

- **Alertes enregistrées** (`audit_alert`) : une seule en cours par règle (index unique). À l'ouverture, envoi unique ; ensuite la mesure et le message sont tenus à jour (`last_seen_at`) sans nouvel envoi ; quand la mesure redescend sous le seuil, l'alerte passe `RESOLVED` seule. Un administrateur en accuse lecture (`ACKNOWLEDGED`, auteur, note) sans la fermer.
- **Canaux** (`AlertDispatcher`, paramètre `bokati.audit.alerts.channels`, `APP,EMAIL,WHATSAPP` par défaut) :
  - application : bandeau en direct (`/topic/admin/alerts`) et notification enregistrée pour chaque administrateur de `bokati.audit.alerts.email-recipients` ;
  - courriel : modèle `ADMIN_ALERT` (`email/admin-alert`, modifiable dans l'éditeur de courriels) aux mêmes adresses ;
  - WhatsApp : modèle `alerte_centre_audit` (objet `AUDIT_ALERT`) aux numéros de `bokati.audit.alerts.whatsapp-recipients`. **Le modèle est à créer chez Infobip et à faire approuver par Meta** (route de création des modèles manquants) ; d'ici là l'envoi échoue proprement et l'échec est noté.
  - Un canal en échec n'empêche pas les autres ; le compte rendu (`APP ok, EMAIL 2/2, WHATSAPP 0/1 (...)`) est gardé sur l'alerte.
- **Tableau de bord** : indicateurs courants avec leur seuil, trafic de la dernière heure (requêtes, erreurs, requêtes par minute), comptes verrouillés, échecs de connexion sur 24 heures, alertes en cours.
- **Routes** (`AlertController`, droit `SYSTEM:AUDIT`) : `GET /admin/audit/dashboard`, `GET /admin/audit/alerts/rules`, `GET /admin/audit/alerts`, `GET /admin/audit/alerts/{id}`, `POST /admin/audit/alerts/{id}/acknowledge` (`{ "note": "..." }`, journalisé), `POST /admin/audit/alerts/evaluate` (évaluation immédiate, journalisée).

## À faire par l'exploitant

1. Renseigner `bokati.audit.alerts.email-recipients` et `bokati.audit.alerts.whatsapp-recipients`.
2. Créer le modèle WhatsApp `alerte_centre_audit` et attendre l'approbation de Meta.

## Tests

`AlertA6IT` (vraie base : ouverture au seuil, envoi unique par les trois canaux avec un WhatsApp refusé, accusé de lecture, fermeture seule, seuil à 0, tableau de bord, liste des destinataires, schéma validé).
