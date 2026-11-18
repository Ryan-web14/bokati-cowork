# Sites, plans, trouver une salle, iCalendar

> Fiche du module [ressource](../README.md), d'après le lot R5b du [plan ressources](../../../backend/ressources/README.md) (18/11/2026). Routes exactes et champs JSON : [routes.md](../routes.md).

## Sites et étages

- `GET|POST /resource-sites`, `GET|PATCH|DELETE /resource-sites/{code}` : nom, adresse, ville, téléphone, fuseau (`Africa/Brazzaville` par défaut), horaires affichés (texte), actif. Code `SIT-AAAA-…`. Suppression refusée s'il reste des ressources ou des étages.
- `GET|POST /resource-sites/{code}/floors`, `PATCH|DELETE /resource-floors/{code}` : nom, niveau, `planDocumentCode` (image du plan dans la GED), actif. Code `FLR-…`.
- `PUT /resources/{code}/placement` `{ siteCode, floorCode, planX, planY }` : rattachement et position sur le plan, en pourcentage depuis le coin haut gauche (0 à 100). L'étage doit appartenir au site ; sans site, celui de l'étage. Rend la ressource.
- Ressource : `siteCode`, `floorCode`, `planX`, `planY` dans la réponse.
- Filtre par site : `GET /resources?siteCode=` (résumé et détail, combinable avec type, groupe, politique) ; `POST /resources/search` accepte `siteCode` et `floorCode`.

## Plan en direct

`GET /resource-floors/{code}/plan?at=` (maintenant par défaut, dans le fuseau du site) : chaque ressource placée avec son état, `FREE`, `PARTIAL` (poste partagé entamé), `OCCUPIED`, `CLOSED`, `NO_SLOT` (pas de créneau ouvert) ou `OUT_OF_SERVICE`, plus la capacité restante et `freeAt` (prochain créneau libre du jour).

## Trouver une salle

`POST /resources/find` `{ startedAt, endedAt, participants, amenityCodes, siteCode, typeCode, maxBudget, ownerType, ownerCode }` (droit `RESOURCE:READ`) :

- `matches` : réservables, du bon type et du bon site, assez grandes, avec tous les équipements, libres sur le créneau et dans le budget ; triées par prix (celui du titulaire, moteur unique).
- `alternatives` (10 au plus), avec `notes` : « Autre horaire le même jour » (créneau libre le plus proche), « Autre site : … », « Sans : … » (équipements manquants), « Au-dessus du budget ».

## Calendriers iCalendar

- `GET /resources/{code}/calendar.ics` (équipe, `RESOURCE:READ`) : réservations actives avec le contact, et fermetures, de -7 à +90 jours.
- Flux d'abonnement : `POST /resources/{code}/ical-token` crée un lien `/public/resource-calendars/{jeton}.ics` (l'ancien cesse). Il ne montre que « Occupé » et « Fermé », sans donnée de client. **Coupé par défaut** (`app.resources.ical.public-feed-enabled` = faux), à activer par l'exploitant s'il le souhaite.
- Invitation de la réservation : `GET /public/bookings/{numéro}/invitation.ics?token=` (même jeton que la confirmation PDF). Le courriel de confirmation porte un bouton « Ajouter à mon agenda ».
