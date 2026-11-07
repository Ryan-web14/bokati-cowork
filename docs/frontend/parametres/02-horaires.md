# Horaires d'ouverture

Les horaires restent globaux, et une ressource peut avoir les siens, qui remplacent les globaux pour
elle seule.

## Horaires globaux

Ce sont des paramètres du domaine `RESOURCES` (voir [01-parametres.md](./01-parametres.md)), au format
`HH:mm-HH:mm`. Vide, le jour est fermé.

| Clé `bokati.resource.availability.generation.` | Valeur actuelle |
| --- | --- |
| `weekday-hours` | 08:00-18:00 |
| `weekday-subscriber-hours` | 18:00-22:00 |
| `saturday-hours` | 08:00-12:00 |
| `sunday-hours` | fermé |

## Horaires d'une ressource

`SUPER_ADMIN` et `ADMIN`.

| Méthode et chemin | Effet |
| --- | --- |
| `GET /resources/{code}/opening-hours` | les horaires qui s'appliquent ; `custom` dit si ce sont les siens |
| `PUT /resources/{code}/opening-hours` | remplace ses horaires |
| `DELETE /resources/{code}/opening-hours` | la ressource revient aux horaires globaux |

```json
[
  { "dayOfWeek": "MONDAY", "open": "09:00", "close": "13:00" },
  { "dayOfWeek": "MONDAY", "open": "14:00", "close": "18:00" }
]
```

- Un jour absent de la liste est fermé pour cette ressource.
- Plusieurs plages sont possibles le même jour, sans chevauchement.
- La plage réservée aux abonnés suit `subscriber-hours-from` (18:00) : un créneau qui commence après
  cette heure leur reste réservé, sur une ressource comme sur les horaires globaux.

## Quand ça s'applique

Un changement vaut pour les créneaux générés ensuite. Les créneaux déjà ouverts ne bougent pas, pour ne
pas toucher aux réservations prises.

Le générateur saute une ressource qui a déjà des créneaux à venir dans le mois : pour elle, les
nouveaux horaires s'appliquent à l'ouverture du mois suivant.
