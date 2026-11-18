# ressource : fonctionnement

> Rédigé d'après le code, à jour du chantier ressources (lots R0 à R7, 18/11/2026). Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md). Les fonctions ajoutées ont chacune leur fiche dans [fonctions/](fonctions/README.md).

## En bref

Le module décrit ce qui se réserve (salles, postes, bureaux, espaces), où et quand :

- le catalogue des ressources et ses référentiels (types, groupes, politiques, équipements, sites et étages, lots) ;
- la galerie photo, les grilles tarifaires (un seul moteur de prix), les horaires d'ouverture, les jours fériés et jours particuliers, les fermetures et blocages ;
- surtout les **créneaux de disponibilité**, sur lesquels s'appuie le module [booking](../booking/README.md) ;
- l'exploitation (modèles de tâches, incidents, règlement d'usage) et le pilotage (coûts, marges, occupation).

Il sert le back-office et expose deux routes publiques sans jeton : le calendrier public d'une ressource, et le flux iCalendar (coupé par défaut). Le portail membre lit les ressources par ses propres routes `/sni/api/v1/client/resources/**` (module [portal](../portal/README.md)). Les horaires globaux et les réglages se font dans le module [parametres](../parametres/routes.md).

La recherche passe par la base (requête JPA et fonction SQL), pas par Elasticsearch.

## Notions

| Objet | Identifiant exposé | Liens et rôle |
| --- | --- | --- |
| Ressource (`Resource`) | `code` (`RES-...`) | Porte un type (obligatoire), un groupe, une politique, un site et un étage (facultatifs), une capacité, une durée de créneau, `active`, `bookingEnabled`, `portalVisible`, un `status`, un ordre d'affichage, un étage en texte (`floorLabel`), une position sur le plan (`planX`, `planY`), des réglages de vente (participants compris, prix d'un participant en plus, caution) et d'usage (règlement, points de l'état des lieux). |
| Type (`ResourceType`) | `code` (`RTY-...`) | Classe la ressource. `bookableSlots` : réservations simultanées admises. « Location de la pièce entière » (`wholeResourceBooking`, interne) change le calcul des places et du prix. |
| Groupe (`ResourceGroup`) | `code` (`RGP-...`) | Regroupement libre, `portalVisible`. Critère des droits d'abonnement. |
| Politique (`ResourcePolicy`) | `code` | Durées minimale et maximale, préavis, annulation (permise par défaut), dérogation de l'équipe, battement, avance maximale, tolérance de dépassement, frais de non-présentation. Désactivée, elle ne s'applique plus. |
| Équipement (`ResourceAmenities`) | `code` (`AMN-...`) | Lié aux ressources avec quantité, caractère optionnel et supplément (`extraPrice`), vendu en option à la réservation. |
| Site (`ResourceSite`) | `code` (`SIT-...`) | Adresse, ville, téléphone, fuseau, horaires affichés. |
| Étage (`ResourceFloor`) | `code` (`FLR-...`) | Étage d'un site, avec l'image de son plan (document de la GED). |
| Lot (`ResourceBundle`) | `code` (`LOT-...`) | Ressources réservées ensemble, avec une remise. |
| Audience (`ResourceAudience`) | aucun | Clients désignés d'une ressource réservée ou sur demande : membre, client ou organisation, entreprise, segment, plan. |
| Créneau (`ResourceAvailability`) | `id` (code texte) | Tranche de `slotDurationMinutes` (5 à 60 min) d'une ressource, avec `totalCapacity` et `remainingCapacity`. C'est ce que consomme une réservation. |
| Fermeture (`ResourceClosure`) | `id` (code texte) | Plage fermée, typée (fermeture, maintenance, ménage, événement privé), unique ou répétée chaque semaine ou chaque année. |
| Jour particulier (`ResourceCalendarDay`) | `id` (code texte) | Jour férié ajouté, horaires exceptionnels ou jour fermé, pour toutes les ressources ou une seule. Les fériés nationaux du Congo sont calculés. |
| Règle tarifaire (`ResourcePricingRule`) | `id` = numéro court | Prix de base ou ajustement par unité (`HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH`), avec conditions, éventuellement réservé à des clients (`audience` : prix au cas par cas) ; versions conservées. |
| Photo (`ResourcePhoto`) | `id` | Rattache une image de la GED à une ressource. |
| Horaires propres (`ResourceOpeningHours`) | aucun | Plages par jour de semaine qui remplacent les horaires globaux. |
| Modèle de tâche (`ResourceTaskTemplate`) | `id` | Tâche de l'équipe créée à chaque réservation confirmée, ou à la vente d'un service. |
| Incident (`ResourceIncident`) | `code` (`INC-...`) | Signalement d'un membre ou de l'équipe, blocage éventuel, ticket au support, résolution. |
| Coût (`ResourceCost`) | `id` | Coût d'une ressource, par mois, par réservation ou par heure réservée. |

Les identifiants `id` des créneaux, fermetures et jours particuliers sortent en codes texte réversibles (17/11/2026) et se renvoient tels quels dans les chemins ([README racine](../README.md#identifiants--codes-en-texte)).

### Dates et heures du module

Plages des créneaux, fermetures, calendriers et devis : `LocalDateTime` **sans fuseau** (`2026-11-09T09:00:00`), heure murale de l'espace. Ne pas y ajouter `Z`. Heures seules : `HH:mm`. Dates : `AAAA-MM-JJ`.

## Statuts et cycle de vie

### Ressource

Réservable (créneaux, réservation, recherche) seulement si `status = ACTIVE`, `active = true` et `bookingEnabled = true`.

| Statut | Signification | On y arrive par | Remarque |
| --- | --- | --- | --- |
| `ACTIVE` | En service | Création (défaut) ; `PATCH /resources/{code}/status?status=ACTIVE` ; résolution d'un incident bloquant avec `reopen` | |
| `INACTIVE`, `OUT_OF_SERVICE` | Hors service | `PATCH .../status` | Refusé s'il reste des réservations à venir, sauf `cancelBookings=true` |
| `MAINTENANCE` | En maintenance | `PATCH .../status` ; incident signalé par l'équipe avec `block` | Même règle |
| `ARCHIVED` | Supprimée | `DELETE /resources/{code}` | Même règle ; la ressource disparaît des lectures |

Réservations à venir (décision D4) : la mise hors service, la maintenance, la suppression ou une fermeture sont **refusées** (`409`, code `RESOURCE_HAS_BOOKINGS`, numéros des réservations dans le message) ; `cancelBookings=true` les annule et prévient les clients (remboursement selon la politique). Aperçu : `GET /resources/{code}/impacted-bookings`. Voir [fonctions/cycle-de-vie.md](fonctions/cycle-de-vie.md).

### Créneau de disponibilité

| État | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| ouvert (`active`, `available`, `remainingCapacity > 0`) | Réservable | Ouverture quotidienne ou création manuelle | plein, fermé, désactivé, expiré |
| plein (`remainingCapacity = 0`) | Pris | Réservation, blocage temporaire (`booking`), réservation manuelle | ouvert (annulation, libération) |
| fermé (`available = false`) | Couvert par une fermeture (unique ou occurrence d'une répétition) | Création ou activation d'une fermeture | ouvert à la levée de la fermeture |
| désactivé (`active = false`) | Retiré par l'équipe | `PATCH /resource-availabilities/{id}/active?active=false` (refusé s'il est réservé) | ouvert (`active=true`) |
| expiré (`expiredAt` renseigné) | Passé | Tâche horaire (plus les lectures) | aucun |

`subscribersOnly = true` : créneaux qui commencent à 18:00 ou plus tard, masqués au calendrier public et réservés aux abonnés au portail.

### Incident

`OPEN` → `IN_PROGRESS` (`POST /resource-incidents/{code}/start`) → `RESOLVED` (`POST .../resolve`).

## Parcours

### Mettre en place les référentiels

1. Types, groupes, politiques, équipements : `POST /resource-types`, `/resource-groups`, `/resource-policies`, `/resource-amenities`. Réponse **`201` avec l'objet créé, code compris** (lot R4).
2. Politique : `minBookingDurationMinutes`, `maxBookingDurationMinutes`, `minBookingNoticeMinutes`, `cancellationNoticeMinutes`, `allowCancellation` (défaut **vrai**), `staffOverrideAllowed` (défaut vrai), `bufferMinutes` (battement entre deux réservations d'une salle), `maxAdvanceDays` (avance maximale), `overrunGraceMinutes` (tolérance de dépassement, au-delà facturé), `noShowFee` (frais de non-présentation), `active`.
3. Sites et étages : [fonctions/sites-et-plans.md](fonctions/sites-et-plans.md).
4. Suppression d'un type, d'un groupe ou d'une politique encore rattachés (ressources archivées comprises) : `409` avec le nombre de ressources.
5. Listes de choix : `GET` de chaque référentiel, ou `GET .../search/basic?query=` (page de 20).

### Créer et configurer une ressource

1. `POST /resources` avec `typeId` = code du type, `groupId` et `policyId` = codes, `name`, `capacity`, `zone`, `locationLabel`, `floorLabel`, `status`, `bookingEnabled`, `portalVisible`. Réponse `201` avec la ressource.
2. Modifier : `PUT /resources/{code}` ; classer : `PATCH .../classification` ; bascules : `PATCH .../booking-enabled`, `.../portal-visible`, `.../active`, `.../display-order`, `.../status?status=` (avec `cancelBookings`).
3. Équipements : `POST /resources/amenities/link` (`quantity`, `optional`, `extraPrice`) ; un équipement optionnel se vend à la réservation.
4. Placement : `PUT /resources/{code}/placement` `{ siteCode, floorCode, planX, planY }`.
5. Vente : `PUT /resources/{code}/sales-settings` `{ includedParticipants, extraParticipantPrice, depositAmount }`.
6. Usage : `PUT /resources/{code}/usage-settings` `{ usageRules, endChecklist }`.
7. Visibilité au portail : `PUT /resources/{code}/visibility` `{ mode: PUBLIC|RESTRICTED|ON_REQUEST, audiences: [{ type, code }] }` ([fonctions/visibilite.md](fonctions/visibilite.md)).
8. Liste : `GET /resources?summary=&typeCode=&groupCode=&policyCode=&siteCode=` (filtres combinables, en résumé comme en détail) ; recherche : `POST /resources/search` (`typeCode`, `groupCode`, `policyCode`, `siteCode`, `floorCode`, `name`, `status`…) ; `GET /resources/search/basic?query=` paginé.

### Ouvrir et gérer les créneaux

Voir [fonctions/creneaux.md](fonctions/creneaux.md). L'essentiel : le serveur ouvre chaque jour les créneaux jusqu'à l'horizon (90 jours, `app.resources.booking-horizon-days`), jour par jour, selon les horaires du jour (semaine type, jours fériés, horaires exceptionnels) ; l'équipe peut créer, désactiver, réactiver ou supprimer des créneaux à venir non réservés, et supprimer tous les créneaux libres d'une ressource pour changer sa durée de créneau.

### Consulter l'occupation

1. Calendrier admin : `GET /resources/{code}/calendar/admin?fromDate=&toDate=` (92 jours au plus) : par jour, créneaux, capacités, taux et niveau d'occupation, statut, **`dayLabel`** (jour férié, horaires exceptionnels, fermé) ; et **`blocks`** : fermetures et occurrences de la plage avec leur nature.
2. Fenêtres réservables : `GET /resource-availabilities/remaining-slots?resourceCode=&startedAt=&endedAt=&durationMinutes=&quantity=` : tiennent compte de la politique (durée, préavis, avance maximale) et du battement.
3. Plan d'étage en direct : `GET /resource-floors/{code}/plan?at=`.
4. Pilotage (occupation, chiffre d'affaires, marges) : [fonctions/pilotage.md](fonctions/pilotage.md).

### Fermer, bloquer, jours particuliers

Voir [fonctions/calendrier.md](fonctions/calendrier.md) : fermetures typées et répétées, jours fériés du Congo, horaires exceptionnels, jours fermés.

### Horaires d'ouverture

1. Globaux : paramètres `bokati.resource.availability.generation.weekday-hours` (`08:00-18:00`), `...weekday-subscriber-hours` (`18:00-22:00`), `...saturday-hours` (`08:00-12:00`), `...sunday-hours` (vide = fermé).
2. D'une ressource : `GET|PUT|DELETE /resources/{code}/opening-hours` (tableau `[{ dayOfWeek, open, close }]`, un jour absent est fermé).
3. Ordre d'application d'une date : jour particulier de la ressource, jour particulier général, jour férié national, semaine type (propre ou globale).
4. La création manuelle de créneaux respecte ces horaires (dérogation : `outsideOpeningHours=true`).

### Tarifer une ressource

Un seul moteur de prix sert la réservation, la disponibilité, le devis, le portail et le catalogue : **le devis annonce ce qui sera facturé**. Tarif membre, tarif des abonnés d'un plan et **prix au cas par cas pour un ou plusieurs clients** : champ `audience` des règles. Détail : [fonctions/tarifs.md](fonctions/tarifs.md) (règles, conditions dont les jours fériés, unité la moins chère, simulateur, versions, conflits, historique des prix appliqués).

### Galerie photo

1. Téléverser l'image dans la GED, puis `POST /resources/{code}/photos` (`documentCode`, `caption`, `cover`, `displayOrder`). Le document doit être une image.
2. Une seule couverture ; `PATCH` et `DELETE` vérifient que la photo appartient à la ressource.

### Calendrier public (sans jeton)

`GET /public/resources/{code}/calendar?fromDate=&toDate=` : ressource visible au portail, non réservée (`RESTRICTED`), active, réservable et `ACTIVE` (sinon `404`), 92 jours au plus, sans les créneaux des abonnés. Statut du jour `AVAILABLE`, `PARTIAL`, `FULL` ou `CLOSED` ; `dayLabel` pour un férié ou un jour particulier.

### Vendre, exploiter, piloter

- Trouver une salle par besoin, iCalendar : [fonctions/sites-et-plans.md](fonctions/sites-et-plans.md).
- Suppléments, caution, dépassement, codes promo, lots : [fonctions/vente.md](fonctions/vente.md) (routes sous `/bookings`).
- Tâches, incidents, règlement d'usage, état des lieux : [fonctions/exploitation.md](fonctions/exploitation.md).
- Coûts, marges, occupation, heures pleines et creuses : [fonctions/pilotage.md](fonctions/pilotage.md).
- Visibilité par client et réservation sur demande : [fonctions/visibilite.md](fonctions/visibilite.md).

## Règles et erreurs à refléter dans l'écran

- **Alignement** : début, fin et durée multiples de la durée de créneau, sans secondes (`400`).
- **Politique** : durée, préavis, avance maximale ; battement autour d'une salle louée entière (`409` « Le battement de n minutes… »). L'équipe passe outre si la politique l'autorise.
- **Places** : sur un espace partagé, `quantity` consomme des places ; sur une pièce entière, la réservation prend tout le créneau.
- **Créneaux** : une réservation demande des créneaux existants, contigus et couvrant la plage.
- **Messages en français** (lot R6), sans le préfixe anglais « Could not process » ; s'appuyer sur le `code` de l'erreur plutôt que sur le texte.

| Situation | Statut | Message (début) |
| --- | --- | --- |
| Créneau dans le passé | `409` | `Impossible d'ouvrir des créneaux dans le passé` |
| Au-delà de l'horizon | `409` | `Créneaux ouverts au plus 90 jours à l'avance…` |
| Hors horaires d'ouverture | `409` | `Plage hors des horaires d'ouverture de RES-… le … : ajustez-la, ou posez outsideOpeningHours=true…` |
| Chevauche une fermeture | `409` | `La plage demandée chevauche une fermeture active` |
| Chevauche des créneaux | `409` | `La plage demandée chevauche des créneaux existants` |
| Ressource non réservable | `409` | `La ressource n'est pas réservable` (et variantes) |
| Réservations à venir | `409` `RESOURCE_HAS_BOOKINGS` | liste des numéros et rappel de `cancelBookings=true` |
| Référentiel encore rattaché | `409` | `Le type RTY-… a encore n ressource(s)…` (et variantes) |
| Nom déjà pris | `409` | `Un type de ressource porte déjà ce nom` (et variantes) |
| Champs invalides | `400` | `VALIDATION_FAILED`, détail dans `errors` |
| Code inconnu | `404` | `Ressource introuvable : RES-…` (et variantes) |
| Créneau réservé à supprimer ou désactiver | `409` | `Créneau réservé : déplacez ou annulez d'abord les réservations…` |
| Opération manuelle sans motif | `400` | `Le motif est obligatoire…` |
| Libération d'une place occupée | `409` | `Libération refusée…` ou `La plage est occupée par n réservation(s)…` |

## À prévoir côté frontend

- Écrans back-office : référentiels ; sites, étages et plans ; lots ; liste des ressources avec filtres combinés ; fiche ressource (description, classification, placement, équipements, photos, horaires, tarifs, fermetures, réglages de vente et d'usage, coûts, incidents, historique de maintenance, calendrier) ; gestion des créneaux ; jours particuliers ; plan en direct ; « Trouver une salle » ; modèles de tâches ; incidents ; tableaux de pilotage.
- Droits : `RESOURCE:READ` (lectures, recherches, trouver une salle, calendrier iCalendar), `RESOURCE:CREATE`, `RESOURCE:UPDATE` (dont saisie des coûts), `RESOURCE:DELETE`, `RESOURCE:PRICE` (tarifs, historique des prix), `RESOURCE:GALLERY` (photos), `REPORT:VIEW` (occupation, marges, lecture des coûts), `BOOKING:CREATE` (réserver un lot).
- Les routes d'écriture acceptent `Idempotency-Key`.
- Avant une mise hors service, une maintenance, une suppression ou une fermeture : afficher `GET /resources/{code}/impacted-bookings` et proposer « annuler et prévenir » (`cancelBookings=true`).
- Ne pas utiliser `/resource-availabilities/reserve` et `/release` dans un écran ordinaire : rattrapage seulement, avec motif obligatoire.
- `GET /resources/search/basic` rend une page (lot R4), plus une liste nue.

## Réglages (module parametres)

Tous modifiables à chaud par `PUT /sni/api/v1/admin/settings/{key}` (droit `SYSTEM:SETTINGS`, domaine `RESOURCES`), effet sans redémarrage. Une valeur absente ou mal écrite ne bloque jamais le démarrage : elle est signalée dans les journaux et le défaut s'applique.

| Clé | Défaut | Rôle |
| --- | --- | --- |
| `app.resources.booking-horizon-days` | `90` | Horizon des créneaux, en jours |
| `app.resources.holidays.national` | `true` | Jours fériés nationaux du Congo calculés |
| `app.resources.holidays.hours` | `NORMAL` | Horaires des jours fériés : `NORMAL` (semaine type), `CLOSED`, ou plages `HH:mm-HH:mm,HH:mm-HH:mm` |
| `app.resources.ical.public-feed-enabled` | `false` | Flux iCalendar public par lien à jeton (« Occupé », « Fermé » seulement) |
| `app.resources.peak-hours` | `09:00-12:00,14:00-17:00` | Heures pleines du lundi au vendredi (pilotage) |
| `app.resources.pricing.half-day-hours`, `...day-hours` | `5`, `10` | Durées facturées de la demi-journée et de la journée |
| `bokati.resource.availability.generation.*-hours` | voir « Horaires d'ouverture » | Horaires globaux |
| `bokati.resource.workers.monthly-availability-cron` | `0 0 3 * * *` | Heure de l'ouverture quotidienne des créneaux |
| `bokati.resource.bulk-max-resources` | `20` | Ressources par opération groupée |

## Effets automatiques

- **Ouverture des créneaux** (`MonthlyAvailabilityWorker`) : au démarrage puis chaque jour à 03:00 (`bokati.resource.workers.monthly-availability-cron`), jusqu'à l'horizon ; un jour déjà ouvert est laissé tel quel ; une fermeture répétée rend ses créneaux indisponibles au lieu de refuser la journée.
- **Créneaux passés et capacités** (`SharedSlotCapacityWorker`, toutes les heures) : clôt les créneaux passés (par lots de 200) et réaligne les capacités sur celles des ressources.
- **Réservations touchées** (lot R2) : annulées et prévenues seulement sur demande explicite (`cancelBookings=true`).
- **Tâches** : créées à la confirmation selon les modèles, annulées avec la réservation ([booking](../booking/README.md)).
- **Incident** : ticket au support ; blocage de la ressource si l'équipe le demande.
- Journal d'audit sur toutes les écritures.

## Écarts relevés

Tous les écarts relevés sur le module sont réglés. Détail par lot : [plan ressources](../../backend/ressources/README.md).

- **Réglé (lot R1)** : un seul moteur de prix ; devis égal à la facture ; recalcul d'une réservation modifiée (facture complémentaire ou avoir) ; versions des règles ; conflits signalés.
- **Réglé (lots R0 et R4)** : `POST /resources/search` en lecture, filtres par codes.
- **Réglé (lot R4)** : `GET /resources` filtre aussi la liste détaillée.
- **Réglé (lot R2)** : une politique désactivée ne s'applique plus et ne se rattache plus ; `staffOverrideAllowed` lisible et modifiable (14/11).
- **Réglé (lot R2)** : `allowCancellation` vaut vrai par défaut.
- **Réglé (lot R2)** : suppression d'un groupe, d'un type ou d'une politique refusée s'il reste des ressources.
- **Réglé (lot R2)** : suppression, maintenance, mise hors service et fermeture d'une ressource contrôlent les réservations à venir.
- **Réglé (lot R0)** : photos rattachées à la ressource du chemin, image vérifiée, une seule couverture.
- **Réglé (lot R3)** : `/reserve` et `/release` exigent un motif ; la libération ne rend jamais une place occupée.
- **Réglé (lot R3)** : routes pour désactiver, réactiver et supprimer des créneaux ; changement de durée de créneau débloqué.
- **Réglé (lot R3)** : le champ `capacity` de création de créneaux est documenté comme ignoré (la capacité suit la ressource).
- **Réglé (lot R2)** : la réponse d'une fermeture rend `reason`.
- **Réglé (lot R0)** : calendrier public borné et réservé aux ressources visibles.
- **Réglé (lot R3)** : les lectures n'écrivent plus (expiration par la tâche horaire).
- **Réglé (lot R2)** : `floorLabel` enregistré.
- **Réglé (lot R6)** : messages en français.
- **Réglé (lot R4)** : créations en `201` avec l'objet ; identifiants des fermetures et créneaux en codes texte.
- **Réglé (lot R5c)** : `optional` et `extraPrice` des équipements servent (options vendues).
- **Réglé (lot R7)** : une règle tarifaire modifiée est revalidée, et une condition se vide (`clear`).
