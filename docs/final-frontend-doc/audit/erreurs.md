# Erreurs du centre d'audit

Toutes au format commun (`errorCode`, `message`, `traceId`), voir le [README racine](../README.md).

| Cas | Statut | Message |
| --- | --- | --- |
| Compte sans `SYSTEM:AUDIT` | `403` | `requiredPermission: "SYSTEM:AUDIT"` |
| Chronologie : `from` après `to` | `400` | `from doit preceder to` |
| Chronologie : plus de 366 jours | `400` | `PERIODE_TROP_LONGUE : 366 jours au plus` |
| Chronologie : source inconnue | `400` | `Source inconnue : FOO (attendu : ACTION, SECURITY, ...)` |
| Chronologie : curseur abîmé | `400` | `Curseur invalide` (repartir sans `cursor`) |
| Trafic : `from` après `to` | `400` | `from doit preceder to` |
| Trafic : granularité inconnue | `400` | `granularity doit valoir MINUTE, HOUR ou DAY` |
| Trafic : minutes au-delà de 7 jours | `400` | `Les minutes ne sont gardees que 7 jours : choisir HOUR ou DAY` |
| Trafic : trop de points | `400` | `Trop de points (n) : choisir une granularite plus large` |
| Pages : `page` négative ou `size` nulle | `400` | `page doit etre positive et size au moins 1` (`size` est ramenée à 200 au plus) |
| Erreur ou alerte inconnue | `404` | `Erreur introuvable : id`, `Alerte introuvable : id` |
| Événements de sécurité : plus de 366 jours | `400` | `PERIODE_TROP_LONGUE` |
