# ressource : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module décrit ce qui se réserve (salles, postes, bureaux, espaces) et quand : le catalogue des ressources et ses référentiels (types, groupes, politiques, équipements), la galerie photo, les grilles tarifaires, les horaires d'ouverture, les fermetures, et surtout les **créneaux de disponibilité** sur lesquels s'appuie le module [booking](../booking/README.md). Il sert le back-office (77 routes) et expose un seul calendrier public sans jeton. Le portail membre lit les ressources par ses propres routes `/sni/api/v1/client/resources/**` (module [portal](../portal/routes.md)). Les horaires globaux se règlent dans le module [parametres](../parametres/routes.md).

Recherche : la recherche des ressources passe par la base (requête JPA et fonction SQL), pas par Elasticsearch ; aucune trace d'Elasticsearch dans `features/ressource/`.

## Notions

| Objet | Identifiant exposé | Liens et rôle |
| --- | --- | --- |
| Ressource (`Resource`) | `code` généré (`RES-...`) | Porte un type (obligatoire), un groupe et une politique (facultatifs), une capacité, une durée de créneau, des indicateurs `active`, `bookingEnabled`, `portalVisible`, un `status`, un ordre d'affichage. |
| Type (`ResourceType`) | `code` (`RTY-...`) | Classe la ressource. `bookableSlots` : nombre de réservations simultanées admises (prioritaire sur la capacité). Un indicateur interne « location de la pièce entière » (`wholeResourceBooking`, non modifiable par l'API) change le calcul des places et du prix, voir [Règles](#règles-et-erreurs-à-refléter-dans-lécran). |
| Groupe (`ResourceGroup`) | `code` (`RGP-...`) | Regroupement libre (étage, zone). `portalVisible` du groupe. Sert aussi de critère aux droits d'abonnement et aux politiques d'audience de `booking`. |
| Politique (`ResourcePolicy`) | `code` (généré par séquence) | Durées minimale et maximale, préavis de réservation, préavis et autorisation d'annulation. |
| Équipement (`ResourceAmenities`) | `code` (`AMN-...`) | Catalogue d'équipements, lié aux ressources avec quantité, caractère optionnel et supplément (`extraPrice`). |
| Créneau (`ResourceAvailability`) | `id` numérique ⚠ int64 | Tranche de `slotDurationMinutes` (5 à 60 min) d'une ressource, avec `totalCapacity` et `remainingCapacity`. C'est ce que consomme une réservation. |
| Fermeture (`ResourceClosure`) | `id` numérique ⚠ int64 | Plage où la ressource est fermée ; rend les créneaux recouverts indisponibles. |
| Règle tarifaire (`ResourcePricingRule`) | `id` = numéro court (petit entier sûr) | Prix par unité (`HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH`) avec conditions facultatives (jour, heures, validité, ajustement, priorité). |
| Photo (`ResourcePhoto`) | `id` numérique (séquence base) | Rattache un document du module `document` (`documentCode`) à une ressource, avec légende, couverture, ordre. |
| Horaires propres (`ResourceOpeningHours`) | aucun | Plages par jour de semaine qui remplacent les horaires globaux pour une ressource. |

Attention aux noms trompeurs : dans `ResourceRequest` (création) et `ChangeTypeRequest` (classification), les champs `typeId`, `groupId`, `policyId` attendent des **codes** (`RTY-...`, `RGP-...`, code de politique), pas des identifiants numériques. À l'inverse, `ResourceSearchCriteria` (`POST /resources/search`) attend de vrais identifiants numériques que l'API n'expose nulle part (voir [Écarts](#écarts-relevés)).

Les identifiants `id` des créneaux et des fermetures sont des entiers 64 bits générés : les lire et les renvoyer en texte ([README racine](../README.md#identifiants--codes-en-texte)).

### Dates et heures du module

Les plages des créneaux, fermetures, calendriers et devis sont des `LocalDateTime` : une date-heure **sans fuseau** (`2026-11-09T09:00:00`), heure murale de l'espace de coworking. Le générateur de documentation les décrit « date-heure ISO 8601 », mais il ne faut pas y ajouter `Z` ni de décalage. Les heures seules (horaires, règles tarifaires) s'écrivent `HH:mm`.

## Statuts et cycle de vie

### Ressource

Le statut (`status`) se combine avec deux indicateurs. Une ressource n'est réservable (création de créneaux, réservation, recherche de fenêtres) que si **les trois** conditions tiennent : `status = ACTIVE`, `active = true`, `bookingEnabled = true`.

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `ACTIVE` | En service | Création (défaut si `status` absent) ; `PATCH /sni/api/v1/resources/{code}/status?status=ACTIVE` | tout autre statut |
| `INACTIVE` | Hors service | `PATCH .../status` | tout autre statut |
| `MAINTENANCE` | En maintenance | `PATCH .../status` | tout autre statut |
| `OUT_OF_SERVICE` | Hors d'usage | `PATCH .../status` | tout autre statut |
| `ARCHIVED` | Supprimée | `DELETE /sni/api/v1/resources/{code}` (suppression logique : `deleted = true`, `active = false`) ; aussi possible par `PATCH .../status` | aucun après suppression : la ressource disparaît de toutes les lectures |

Aucune transition automatique. Changer le statut ne touche ni les créneaux ni les réservations existantes.

### Créneau de disponibilité

| État | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| ouvert (`active = true`, `available = true`, `remainingCapacity > 0`) | Réservable | Création manuelle ou génération mensuelle | plein, fermé, expiré |
| plein (`remainingCapacity = 0`, `available = false`) | Entièrement pris | Réservation confirmée, blocage temporaire (`booking`), ou `POST /resource-availabilities/reserve` | ouvert (annulation, libération) |
| fermé (`available = false`) | Couvert par une fermeture active | Création ou activation d'une fermeture | ouvert quand la fermeture est désactivée ou supprimée (si encore à venir et pas plein) |
| expiré (`active = false`, `available = false`, `expiredAt` renseigné) | Passé | Automatique, à chaque lecture ou réservation de créneaux (par lots de 200) | aucun ; seule une réservation saisie après coup par l'équipe peut encore l'utiliser |
| inactif (`active = false` sans `expiredAt`) | Désactivé à la création (`active: false`) | `POST /resource-availabilities` avec `active: false` | aucun : pas de route pour le réactiver |

`subscribersOnly = true` marque les créneaux qui commencent à 18:00 ou plus tard (`bokati.resource.availability.subscriber-hours-from`) : ils sont masqués au calendrier public et réservés aux abonnés sur le portail ; le back-office peut les réserver pour n'importe qui.

### Fermeture, règle tarifaire, photo, politique, type, groupe, équipement

Pas de statut, seulement `active`. Fermeture : `PATCH /resource-closures/{id}/active` ou suppression. Règle tarifaire : `PATCH /resource-pricing-rules/{id}/active`. Photo : `DELETE` la passe à `active = false` (elle n'apparaît plus dans la galerie). Politique : `PATCH /resource-policies/{code}/status?active=...` (sans effet sur les réservations, voir [Écarts](#écarts-relevés)).

## Parcours

### Mettre en place les référentiels

1. Types : `POST /sni/api/v1/resource-types` avec `name` (unique sans casse), `description`, `active`, `bookableSlots`. Réponse `201` **vide** : recharger la liste (`GET /sni/api/v1/resource-types`) pour obtenir le `code`.
2. Groupes : `POST /sni/api/v1/resource-groups` avec `name` (unique), `portalVisible`, `active`. Même principe, puis `GET /sni/api/v1/resource-groups` (liste non paginable : renvoie une `PaginatedResponse` sans paramètre de page).
3. Politiques : `POST /sni/api/v1/resource-policies` avec `name` (unique), `minBookingDurationMinutes` et `maxBookingDurationMinutes` (> 0, max ≥ min), `minBookingNoticeMinutes` (≥ 0), `cancellationNoticeMinutes` (≥ 0 ; à toujours envoyer, la colonne est non nulle), `allowCancellation` (défaut `false`), `staffOverrideAllowed` (défaut `true` : l'équipe réserve hors préavis et sur un début passé ; `false` garde le préavis pour tout le monde, modifiable par `PUT` et lu dans la réponse). Raccourcis : `PATCH /resource-policies/{code}/allow-cancellation?allowed=true`, `PATCH /resource-policies/{code}/status?active=false`.
4. Équipements : `POST /sni/api/v1/resource-amenities` avec `name` (unique), `description`, `active`.
5. Listes de choix des formulaires : `GET /resource-types`, `GET /resource-groups`, `GET /resource-policies`, `GET /resource-amenities` (pages maison), ou `GET .../search?query=...` pour l'autocomplétion (`/search` et `/search/basic` sont identiques ; `query` obligatoire).

### Créer et configurer une ressource

1. `POST /sni/api/v1/resources` ([détail](routes/ResourceController.md#r-post-sni-api-v1-resources)) avec `typeId` = code du type (obligatoire), `groupId` et `policyId` = codes (facultatifs), `name`, `capacity` (≥ 1), `zone`, `locationLabel`, `status` (texte d'une valeur de `ResourceStatus`, défaut `ACTIVE`), `bookingEnabled`, `portalVisible`. Réponse `201` vide.
2. Retrouver la ressource : `GET /sni/api/v1/resources?summary=true&typeCode=RTY-...` ou `GET /sni/api/v1/resources/search/basic?query=<nom>`, puis `GET /sni/api/v1/resources/{code}`.
3. Modifier les champs descriptifs : `PUT /sni/api/v1/resources/{code}` (`name`, `description`, `capacity`, `zone`, `locationLabel`, `bookingEnabled`, `portalVisible` ; un champ absent n'est pas modifié).
4. Changer type, groupe ou politique : `PATCH /sni/api/v1/resources/{code}/classification` avec `typeId` (code, obligatoire), `groupId`, `policyId` (codes ; absents, ils sont **retirés**).
5. Bascules unitaires, chacune avec un corps d'un seul champ : `PATCH .../booking-enabled` (`bookingEnabled`), `.../portal-visible` (`visible`), `.../active` (`active`), `.../display-order` (`displayOrder`) ; statut par paramètre : `PATCH .../status?status=MAINTENANCE`.
6. Équipements de la ressource : `POST /sni/api/v1/resources/amenities/link` avec `resourceCode`, `amenityCode`, `quantity`, `optional`, `extraPrice` ; liste : `GET /resources/{code}/amenities` ; retrait : `DELETE /resources/{code}/amenities/{amenityCode}`.
7. La durée de créneau (`slotDurationMinutes`, lue dans la réponse, défaut 30) ne se modifie qu'à la création de créneaux (voir plus bas).

### Ouvrir des créneaux

Les créneaux du mois en cours sont normalement créés par le serveur (voir [Effets automatiques](#effets-automatiques)). Créer à la main sert à ouvrir le mois suivant en avance, une plage exceptionnelle ou une nouvelle ressource sans attendre.

1. `POST /sni/api/v1/resource-availabilities` avec `resourceCode`, `startedAt`, `endedAt` (sans fuseau, alignés sur la durée de créneau), `active` (défaut `true`), `slotDurationMinutes` facultatif.
2. Le serveur découpe la plage en créneaux de la durée de la ressource, **jour par jour entre 08:00 et 22:00** (ce qui sort de cette fenêtre est ignoré ; une plage multi-jours est donc possible). La capacité de chaque créneau est celle de la ressource : le champ `capacity` (ou `totalCapacity`) de la requête est accepté mais ignoré.
3. Réponse `201` vide. Vérifier avec `GET /resource-availabilities?resourceCode=...` ou le calendrier admin.
4. Plusieurs ressources d'un coup : `POST /sni/api/v1/resource-availabilities/bulk` avec `resourceCodes` (20 au plus, réglage `bokati.resource.bulk-max-resources`) et la même plage. Réponse `200` : `processed`, `skipped` et une ligne par ressource (`status` `PROCESSED` avec le nombre de créneaux dans `detail`, ou `SKIPPED` avec `reason`). Afficher ce compte rendu ressource par ressource.
5. Changer la durée de créneau : envoyer `slotDurationMinutes` (5, 10, 15, 20, 30 ou 60). Accepté seulement si la ressource n'a **aucun créneau futur** ; sinon `409` avec le nombre de créneaux à supprimer. Aucune route ne supprime de créneau : ce changement n'est donc possible que sur une ressource sans créneau à venir.

### Consulter l'occupation

1. Calendrier admin : `GET /sni/api/v1/resources/{resourceCode}/calendar/admin?fromDate=2026-11-01&toDate=2026-11-30` (dates `AAAA-MM-JJ`, défaut : aujourd'hui sur un mois ; 92 jours au plus, fin avant le début refusée en `400`). Par jour : nombres de créneaux, capacité totale et restante, `occupancyRate` (%), `occupancyLevel` (`FREE` à 0 %, `PARTIALLY_OCCUPIED` < 50 %, `HIGHLY_OCCUPIED` < 85 %, `FULL` au-delà), `status` (`AVAILABLE`, `PARTIAL`, `FULL`) et la liste des créneaux.
2. Créneaux occupés seulement : `GET /sni/api/v1/resources/occupied-slots?resourceCode=...&from=...&to=...` (date-heures, seule la date est retenue).
3. Fenêtres réservables : `GET /sni/api/v1/resource-availabilities/remaining-slots?resourceCode=...&startedAt=...&endedAt=...&durationMinutes=60&quantity=1`. Rend les fenêtres de créneaux contigus de la durée demandée, ouvertes, avec assez de capacité et respectant le préavis de la politique (une recherche hors politique rend une liste vide, pas une erreur). Sans `startedAt`, `endedAt` ni `durationMinutes` : toutes les plages contiguës à venir. `durationMinutes` doit être un multiple de la durée de créneau.
4. Liste brute : `GET /resource-availabilities?resourceCode=...` (créneaux non terminés, triés par `startedAt`, 50 par page) ou `GET /resource-availabilities/grouped` (tous les créneaux futurs groupés par ressource, non paginé).

### Fermer une ressource sur une période

1. `POST /sni/api/v1/resource-closures` avec `resourceCode`, `startedAt`, `endedAt`, `reason`, `active` (défaut `true`). Réponse `201` vide.
2. Les créneaux recouverts passent à `available = false`. Les **réservations existantes ne sont pas annulées** : les lister via `booking` (`GET /sni/api/v1/bookings?resourceCode=...&startedFrom=...&startedTo=...`) et les traiter.
3. Liste : `GET /resource-closures?resourceCode=...` ; réactiver ou suspendre : `PATCH /resource-closures/{id}/active` avec `{ "active": false }` ; supprimer : `DELETE /resource-closures/{id}`. La suspension ou la suppression rouvre les créneaux à venir non pleins et non couverts par une autre fermeture.
4. La réponse d'une fermeture ne contient pas `reason` : le motif saisi n'est pas relisible.

### Horaires d'ouverture

1. Horaires globaux : clés de paramètres (domaine `RESOURCES`, module [parametres](../parametres/routes.md)) via `PUT /sni/api/v1/admin/settings/{key}` (droit `SYSTEM:SETTINGS`) : `bokati.resource.availability.generation.weekday-hours` (défaut `08:00-18:00`, lundi à vendredi), `...weekday-subscriber-hours` (défaut `18:00-22:00`, soirées abonnés), `...saturday-hours` (`08:00-12:00`), `...sunday-hours` (vide = fermé). Format `HH:mm-HH:mm`, effet immédiat sur les **prochaines** générations.
2. Horaires d'une ressource : `GET /sni/api/v1/resources/{resourceCode}/opening-hours` rend `{ resourceCode, custom, hours: [{ dayOfWeek, open, close }] }` ; `custom = false` signifie que `hours` sont les horaires globaux.
3. Remplacer : `PUT /sni/api/v1/resources/{resourceCode}/opening-hours` avec un **tableau** JSON, par exemple `[{ "dayOfWeek": "MONDAY", "open": "08:00", "close": "12:00" }, { "dayOfWeek": "MONDAY", "open": "14:00", "close": "18:00" }]`. Un jour absent est fermé. Liste vide refusée ; plages qui se chevauchent le même jour refusées.
4. Revenir aux horaires globaux : `DELETE /sni/api/v1/resources/{resourceCode}/opening-hours`.
5. Les créneaux déjà ouverts ne bougent pas : les nouveaux horaires valent pour les générations suivantes. Les créneaux créés à la main ignorent les horaires (seule la fenêtre 08:00-22:00 s'applique).

### Tarifer une ressource

Un seul moteur de prix (lot R1, 18/11/2026) sert la réservation, la vérification de disponibilité, le devis, le portail et le catalogue : **le devis annonce ce qui sera facturé**.

1. Créer : `POST /sni/api/v1/resource-pricing-rules` avec `resourceCode`, `bookingUnit` (`HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH`), `label`, et selon le rôle de la règle :
   - **prix de base** : `price` (entier ≥ 0, en XAF), sans `adjustmentType` ;
   - **ajustement** : `adjustmentType` (`FIXED_PRICE` remplace la base, `AMOUNT_DELTA` ajoute un montant, éventuellement négatif, `PERCENT_DELTA` un pourcentage) et `adjustmentValue` ; `stackable = true` pour qu'il se cumule avec les autres (sinon seul le meilleur ajustement non cumulable s'applique).
   - **Conditions**, toutes facultatives et combinables : `daysOfWeek` (« 1,2,3,4,5 », 1 = lundi … 7 = dimanche ; remplace `dayOfWeek`), `startsAt`/`endsAt` (`HH:mm`, une plage qui passe minuit est admise), `validFrom`/`validUntil` (`AAAA-MM-JJ`), `customerSegment` (`PUBLIC` client hors membre, `MEMBER`, `SUBSCRIBER` abonnement en cours, `COMPANY`, `PARTNER`), `planCode` (plan d'abonnement), `minParticipants`/`maxParticipants`, `minDurationMinutes`/`maxDurationMinutes`, `minLeadMinutes` (réservation anticipée : au moins ce délai avant le début), `lastMinuteMinutes` (dernier moment : au plus ce délai), `channel` (`PORTAL` ou `TEAM`).
   - `perParticipant = true` : prix par participant, même pour une salle louée entière. `priority` (défaut 0) départage, puis la règle la plus précise (le plus de conditions), puis la plus récente.
2. Liste : `GET /resource-pricing-rules?resourceCode=...`, mêmes champs en réponse. L'`id` (petit numéro) sert dans `GET`, `PUT`/`PATCH`, `DELETE /resource-pricing-rules/{id}` et `PATCH /{id}/active?active=...`. Mise à jour partielle ; une chaîne vide efface une condition texte (`daysOfWeek`, `customerSegment`, `planCode`, `channel`).
3. Plusieurs ressources : `POST /resource-pricing-rules/bulk/preview` (aucune écriture), puis `POST /resource-pricing-rules/bulk`.
4. Devis : `GET /resource-pricing-rules/quote?resourceCode=...&startedAt=...&endedAt=...` avec, facultatifs, `quantity` (postes ou participants), `ownerType` (`MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`) et `ownerCode` pour le prix de ce client, `bookingUnit` pour imposer une unité (équipe). Réponse : `bookingUnit` retenue, `units`, `quantity`, `amount` (montant exact), `finalPrice` (arrondi), `currency`, `lines` (tranches : début, fin, unités, prix d'une unité, règle de base, ajustements, montant), `alternatives` (coût de chaque unité possible, `chosen` sur la retenue) ; `basePrice`, `appliedRuleId`, `appliedRuleLabel`, `adjustmentType`, `adjustmentValue` restent pour la première tranche.
5. Simulateur : `POST /sni/api/v1/resource-pricing-rules/simulate` avec `{ resourceCode, startedAt, endedAt, quantity, bookingUnit, ownerType, ownerCode, channel }` : même réponse, pour vérifier une grille avant de la publier (droit `RESOURCE:PRICE`).

Règles de calcul :

- **Unité** : imposée par l'équipe (`bookingUnit`, un forfait imposé vaut un forfait), sinon **la moins chère pour le client** parmi les unités qui ont un prix de base applicable ; à égalité, celle de la durée. Exemple : 8 heures à 5000 l'heure (40000) coûtent plus qu'une journée à 30000 : la journée est retenue.
- **Durées** : demi-journée 5 h, journée 10 h (paramètres `app.resources.pricing.half-day-hours`, `app.resources.pricing.day-hours`) ; sur plusieurs jours, un forfait journée par jour touché (deux demi-journées par jour) ; semaine par tranche de 7 jours ; mois calendaire.
- **À l'heure**, la réservation est découpée aux bornes horaires des règles et à minuit : heures creuses et heures pleines au prorata (`lines`). Un forfait est tarifé à l'heure de début.
- **Quantité** : une salle louée entière se facture une fois ; un poste partagé, par place ; une règle `perParticipant`, par participant.
- **Client** : le portail et la réservation appliquent le segment du titulaire (membre, abonné et son plan, entreprise, client hors membre) et le canal (`PORTAL` pour l'espace client, `TEAM` pour le back-office).
- Le détail du calcul est enregistré avec la réservation (`priceTrace`, même forme que le devis) : une facture reste explicable après un changement de grille.

### Galerie photo

1. Téléverser l'image dans le module `document` (`POST /sni/api/v1/documents/upload`, multipart) et récupérer son code.
2. `POST /sni/api/v1/resources/{resourceCode}/photos` avec `documentCode`, `caption`, `cover`, `displayOrder`, `active`. Le document doit exister dans la GED et être une image (`400` sinon). Réponse `201` : la photo avec son `id`.
3. Galerie : `GET /resources/{resourceCode}/photos` (photos actives, triées par `displayOrder`). Afficher l'image via les routes de lecture du module `document` (`GET /sni/api/v1/documents/{code}/preview` ou `/download`, avec jeton).
4. Modifier : `PATCH /resources/{resourceCode}/photos/{id}` avec le **corps complet** (`documentCode` obligatoire ; `cover` absent vaut `false`, `active` absent vaut `true`). Retirer : `DELETE /resources/{resourceCode}/photos/{id}`. La photo doit appartenir à la ressource du chemin (`404` sinon).
5. Une seule photo de couverture : poser `cover = true` sur une photo retire la couverture de l'ancienne (lot R0).

### Calendrier public (sans jeton)

1. `GET /sni/api/v1/public/resources/{resourceCode}/calendar?fromDate=...&toDate=...` : même structure par jour que le calendrier admin, sans les taux d'occupation, et **sans les créneaux réservés aux abonnés** (ni dans les fenêtres, ni dans les totaux). Seulement pour une ressource visible au portail, active, réservable et au statut `ACTIVE` : sinon `404`, comme une ressource inconnue. Plage de 92 jours au plus (`400` au-delà, ou si la fin précède le début).
2. Statut du jour : `AVAILABLE`, `PARTIAL`, `FULL` (des créneaux, tous pris) ou `CLOSED` (aucun créneau : fermé, hors horaires, ou au-delà de l'horizon ouvert). Avant le lot R0, un jour sans créneau était `FULL`.

## Règles et erreurs à refléter dans l'écran

Règles de réservabilité, appliquées par `booking` et par les routes de créneaux :

- **Alignement** : début, fin et durée multiples de la durée de créneau de la ressource, sans secondes. Sinon `400` `La plage doit etre alignee sur des creneaux de <n> minutes`.
- **Politique** : durée entre `minBookingDurationMinutes` et `maxBookingDurationMinutes`, début au moins `minBookingNoticeMinutes` après maintenant (sauf réservation de l'équipe, voir `booking`). Une politique désactivée continue de s'appliquer.
- **Places** : sur une ressource partagée (open space), `quantity` consomme des places et ne peut dépasser `bookableSlots` du type, ou à défaut la capacité. Sur une pièce louée entière, `quantity` compte des participants (plafonnés par la capacité), la réservation prend tout le créneau et le prix n'est pas multiplié.
- **Créneaux existants** : une réservation n'est possible que sur des créneaux existants, contigus et couvrant exactement la plage. Pas de créneau, pas de réservation.

Création manuelle de créneaux (`409` `CONFLICT`, message `Could not process resource availability: ...`) :

| Cause | Message (fin) |
| --- | --- |
| Début dans le passé | `availability cannot be created in the past` |
| Fin au-delà d'un mois (aujourd'hui + 1 mois, 22:00) | `availability can only be created up to one month in advance` |
| Début ou fin hors 08:00-22:00 | `availability must be created within working hours from 08:00 to 22:00` |
| Chevauche une fermeture active | `the requested range overlaps an active closure` |
| Chevauche des créneaux existants | `the requested range overlaps existing availability slots` |
| Ressource non réservable | `the resource is not bookable` / `the resource is inactive` / `the resource status does not allow bookings` |
| Durée de créneau changée avec des créneaux futurs | message en français donnant le nombre de créneaux |

Autres erreurs fréquentes :

| Situation | Statut | Message |
| --- | --- | --- |
| Nom de type, groupe, politique ou équipement déjà pris | `409` | `A resource type with this name already exists` (et variantes) |
| Supprimer un type, une politique ou un équipement encore utilisé | `409` | `Could not process resource type: it is currently assigned to one or more resources` (et variantes) |
| Champs invalides (nom, description, capacité < 1...) | `400` | `VALIDATION_FAILED`, détail dans `errors` |
| Équipement déjà lié | `409` | `This amenity is already linked to the resource` |
| Code inconnu | `404` | `Resource with code <code> not found` (et variantes) |
| Horaires incohérents | `400` | message en français (`Deux plages se chevauchent le MONDAY.`...) |
| Lot de plus de 20 ressources | `400` | `Lot de <n> ressources · maximum autorise : 20. Decoupez l'appel.` |
| `durationMinutes` non multiple ou trop court | `400` | `Duration must be a multiple of <n> minutes` |
| Devis sans aucun prix de base applicable | `404` | `Aucun prix de base actif pour la ressource <code>` |
| Unité imposée sans prix de base applicable | `400` | `La ressource <code> n'a aucun prix de base en <unité> applicable à ce créneau…` |

Prix : un seul calcul (lot R1), voir « Tarifer une ressource ».

## À prévoir côté frontend

- Écrans back-office : référentiels (types, groupes, politiques, équipements) ; liste des ressources avec filtres ; fiche ressource (onglets description, classification, équipements, photos, horaires, tarifs, fermetures, calendrier) ; ouverture de créneaux (unitaire et groupée) ; calendrier d'occupation. Écran public : calendrier d'une ressource.
- Droits : `RESOURCE:READ` (lectures, y compris `POST /resources/search` et `/search` des types, groupes, politiques et équipements, et la galerie de photos), `RESOURCE:CREATE` (créations, liaison d'équipement, créneaux), `RESOURCE:UPDATE` (modifications, bascules, activation de fermeture, libération de créneaux, horaires d'ouverture, y compris leur retour aux horaires globaux), `RESOURCE:DELETE` (suppressions), `RESOURCE:PRICE` (toutes les routes de tarifs, lecture comprise), `RESOURCE:GALLERY` (ajout, modification et retrait de photos). Lot R0 : les horaires d'ouverture n'exigent plus le rôle ADMIN.
- Les créations renvoient `201` sans corps (ressource, type, groupe, politique, équipement, règle, fermeture, créneaux) : recharger la liste pour obtenir le code ou l'id.
- Les routes d'écriture acceptent `Idempotency-Key` (voir chaque route) : en envoyer une par action.
- `GET /sni/api/v1/resources` : avec `summary=false` (défaut), page de `ResourceResponse` et les filtres `typeCode`, `groupCode`, `policyCode` sont **ignorés** ; avec `summary=true`, page de `ResourceSummaryResponse` filtrée par **un seul** critère (type prioritaire sur groupe, groupe sur politique). Pour filtrer, utiliser `summary=true`.
- Ne pas utiliser `POST /resource-availabilities/reserve` ni `PATCH /resource-availabilities/release` depuis un écran ordinaire : ces routes retirent ou rendent de la capacité **sans réservation associée** et désynchronisent les créneaux des réservations. Toute réservation passe par `booking`.
- Le statut d'une ressource s'envoie en paramètre de requête (`?status=`), celui d'une politique et d'une règle aussi (`?active=`, `?allowed=`), alors que les autres bascules prennent un corps JSON.
- Afficher un avertissement lors d'une fermeture : les réservations existantes sur la période restent valides.
- Les créneaux du mois suivant n'existent qu'à partir du 1er à 03:00 : un calendrier ou une recherche au-delà de la fin du mois en cours est vide tant que personne ne les a ouverts.

## Effets automatiques

- **Ouverture mensuelle des créneaux** (`MonthlyAvailabilityWorker`) : une fois environ 2 minutes après le démarrage du serveur, puis le 1er de chaque mois à 03:00 (fuseau `app.time-zone`). Pour chaque ressource réservable **sans aucun créneau dans le reste du mois**, crée les créneaux de maintenant à la fin du mois selon ses horaires (propres ou globaux). Une journée refusée (fermeture, chevauchement) est sautée. Une ressource qui a déjà au moins un créneau dans le mois n'est pas complétée.
- **Réalignement des capacités** (`SharedSlotCapacityWorker`) : environ 1 minute après le démarrage puis toutes les heures, remet la capacité des créneaux à venir à celle de leur ressource (après un changement de `capacity` ou de `bookableSlots`), en conservant ce qui est déjà réservé.
- **Expiration des créneaux passés** : déclenchée par chaque lecture de créneaux ou de fenêtres (`GET /resource-availabilities`, `/grouped`, `/remaining-slots`) et par chaque réservation ou libération, par lots de 200.
- Journal d'audit sur toutes les écritures annotées `@Audited` (création, modification, suppression, bascules, liaisons, fermetures, règles).
- Pas de temps réel ni de courriel dans ce module.

## Écarts relevés

- **Réglé (lot R1, 18/11/2026)** : un seul moteur (`ResourcePriceEngine`) pour la réservation, la disponibilité, le devis, le portail et le catalogue ; durées 5 h et 10 h partout ; conditions, ajustements et priorité appliqués partout ; trace enregistrée avec la réservation. Reste au lot R1 : recalcul quand une réservation change (D1). Constat d'origine : Deux moteurs de prix divergents : la réservation prend la règle active la plus récente de l'unité et ignore jour, heures, validité, ajustement et priorité (`src/main/java/com/sni/bokaticowork/features/booking/service/support/BookingPricingCalculator.java:56-68` et `src/main/java/com/sni/bokaticowork/features/ressource/repository/repo/ResourcePricingRuleRepository.java:36-43`), alors que le devis et l'aperçu groupé les appliquent et comptent demi-journée = 12 h, journée = 24 h (`src/main/java/com/sni/bokaticowork/features/ressource/service/implementation/ResourcePricingRuleServiceImpl.java:197-239` et `:366-376`). Le devis peut donc annoncer un autre montant que celui facturé.
- **En partie réglé (lot R0, 18/11/2026)** : la recherche demande `RESOURCE:READ` ; les filtres par codes viennent au lot R4. Constat d'origine : `POST /resources/search` exige `RESOURCE:CREATE` (règle d'URL sur le verbe POST) et filtre par `typeId`, `groupId`, `policyId` numériques que l'API n'expose jamais (`src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourceController.java:89` ; `src/main/java/com/sni/bokaticowork/features/ressource/repository/specification/criteria/ResourceSearchCriteria.java`).
- `GET /resources` ignore `typeCode`, `groupCode`, `policyCode` quand `summary=false` (`ResourceController.java:61-84`).
- Une politique désactivée (`active = false`) continue de s'appliquer aux réservations . `staffOverrideAllowed` n'était ni lisible ni modifiable par l'API : **Réglé** le 14/11/2026, il est dans la création, la modification et la réponse des politiques.
- `allowCancellation` vaut `false` par défaut à la création d'une politique : toute réservation confirmée d'une ressource rattachée devient non annulable, sauf réglage explicite (`ResourcePolicy.java:46-48`).
- Suppression d'un groupe sans vérifier les ressources rattachées, contrairement aux types, politiques et équipements (`src/main/java/com/sni/bokaticowork/features/ressource/service/implementation/ResourceGroupServiceImpl.java:79-86`). Non vérifié : réponse exacte si la base refuse la suppression.
- Suppression d'une ressource sans contrôle des réservations à venir ni libération de ses créneaux (`src/main/java/com/sni/bokaticowork/features/ressource/service/implementation/ResourceServiceImpl.java:108-114`).
- **Réglé (lot R0, 18/11/2026)** : photo rattachée à la ressource du chemin, document de la GED vérifié (image), une seule couverture, ajouts et modifications journalisés. Constat d'origine : Photo : `PATCH` et `DELETE /resources/{resourceCode}/photos/{id}` ne vérifient pas que la photo appartient à la ressource du chemin ; `documentCode` n'est pas contrôlé ; plusieurs couvertures possibles (`src/main/java/com/sni/bokaticowork/features/ressource/controller/ResourcePhotoController.java:37-46`, `src/main/java/com/sni/bokaticowork/features/ressource/service/implementation/ResourcePhotoServiceImpl.java:51-68`).
- `POST /resource-availabilities/reserve` et `PATCH /release` manipulent la capacité sans lien avec une réservation : une libération manuelle peut rendre disponible un créneau pourtant réservé (`src/main/java/com/sni/bokaticowork/features/ressource/service/implementation/ResourceAvailabilityServiceImpl.java:247-317`).
- Aucune route pour supprimer ou réactiver un créneau, ce qui bloque le changement de durée de créneau d'une ressource qui a des créneaux à venir (`ResourceAvailabilityServiceImpl.java:561-580`).
- Le champ `capacity`/`totalCapacity` de création de créneaux est accepté puis ignoré (`src/main/java/com/sni/bokaticowork/features/ressource/dto/request/CreateResourceAvailabilityRequest.java`, `ResourceAvailabilityServiceImpl.java:595-603`).
- La réponse de fermeture n'inclut pas `reason` (`src/main/java/com/sni/bokaticowork/features/ressource/service/implementation/ResourceClosureServiceImpl.java:168-176`).
- **Réglé (lot R0, 18/11/2026)** : ressource visible, active, réservable et `ACTIVE` exigée, plage de 92 jours au plus, dates inversées refusées, jour sans créneau `CLOSED`. Constat d'origine : Calendrier public : aucun contrôle de `portalVisible`, `active` ou `bookingEnabled`, et aucune borne sur l'écart `fromDate`-`toDate` (`src/main/java/com/sni/bokaticowork/features/ressource/service/implementation/PublicResourceCalendarServiceImpl.java:33-50`). Un jour sans créneau est marqué `FULL` (`:92-95`).
- Les lectures de créneaux écrivent (expiration des créneaux passés dans un `GET`) (`ResourceAvailabilityServiceImpl.java:131-136`).
- `UpdateResourceRequest.floorLabel` est accepté mais n'est lu nulle part dans le code : la valeur envoyée est perdue (`src/main/java/com/sni/bokaticowork/features/ressource/dto/request/UpdateResourceRequest.java:22`).
- Messages d'erreur majoritairement en anglais, certains en français sans accents.
