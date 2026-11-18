# Tarifs : un seul moteur de prix

> Fiche du module [ressource](../README.md), d'après les lots R1, R5a, R5e et R7 du [plan ressources](../../../backend/ressources/README.md) (18/11/2026). Routes exactes et champs JSON : [routes.md](../routes.md).

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
6. Versions : `GET /sni/api/v1/resource-pricing-rules/{id}/history` : chaque création, modification, activation, désactivation et suppression d'une règle, avec la règle complète (`snapshot`, JSON), l'auteur et la date, la plus récente d'abord. La suppression est douce : la règle disparaît des listes et du calcul, son historique reste.
7. Cohérence : une règle identique à une règle active (même unité, même rôle, mêmes conditions, même priorité) est refusée en `409` ; `GET /sni/api/v1/resource-pricing-rules/conflicts?resourceCode=...` liste les paires ambiguës (deux prix de base, ou deux ajustements non cumulables, de même priorité et de même précision sur les mêmes créneaux : la plus récente l'emporte). Afficher ces conflits sur l'écran des tarifs.

Règles de calcul :

- **Unité** : imposée par l'équipe (`bookingUnit`, un forfait imposé vaut un forfait), sinon **la moins chère pour le client** parmi les unités qui ont un prix de base applicable ; à égalité, celle de la durée. Exemple : 8 heures à 5000 l'heure (40000) coûtent plus qu'une journée à 30000 : la journée est retenue.
- **Durées** : demi-journée 5 h, journée 10 h (paramètres `app.resources.pricing.half-day-hours`, `app.resources.pricing.day-hours`) ; sur plusieurs jours, un forfait journée par jour touché (deux demi-journées par jour) ; semaine par tranche de 7 jours ; mois calendaire.
- **À l'heure**, la réservation est découpée aux bornes horaires des règles et à minuit : heures creuses et heures pleines au prorata (`lines`). Un forfait est tarifé à l'heure de début.
- **Quantité** : une salle louée entière se facture une fois ; un poste partagé, par place ; une règle `perParticipant`, par participant.
- **Client** : le portail et la réservation appliquent le segment du titulaire (membre, abonné et son plan, entreprise, client hors membre) et le canal (`PORTAL` pour l'espace client, `TEAM` pour le back-office).
- Le détail du calcul est enregistré avec la réservation (`priceTrace`, même forme que le devis) : une facture reste explicable après un changement de grille.

## Jours fériés (lot R5a)

Condition `holidayMode` d'une règle : `ONLY` (jours fériés seulement, par exemple une majoration), `EXCLUDED` (hors jours fériés), vide pour tous les jours. Jours fériés : nationaux du Congo calculés, plus ceux ajoutés par l'équipe ([calendrier.md](calendrier.md)).

## Modifier une règle (lot R7)

`PUT /resource-pricing-rules/{id}` : mise à jour partielle ; `clear` vide des conditions, par exemple `{ "clear": ["startsAt", "endsAt", "validUntil"] }`. Noms admis : `startsAt`, `endsAt`, `validFrom`, `validUntil`, `dayOfWeek`, `daysOfWeek`, `lastMinuteMinutes`, `minLeadMinutes`, `minParticipants`, `maxParticipants`, `minDurationMinutes`, `maxDurationMinutes`, `customerSegment`, `planCode`, `channel`, `holidayMode`, `adjustment` (type et valeur). La règle modifiée est revalidée comme à la création (`400` `VALIDATION_FAILED` sinon : fin avant début, min au-dessus du max, prix de base sans prix, ajustement sans valeur…), puis comparée aux règles existantes (doublon `409`).

## Historique des prix appliqués (lot R5e)

`GET /resources/{code}/price-history?from=&to=` (droit `RESOURCE:PRICE`) : pour chaque réservation, unité, prix unitaire, sous-total, total et règles appliquées (lues dans la trace du prix). Avec `GET /resource-pricing-rules/{id}/history` (versions de chaque règle), on retrouve le prix d'hier et pourquoi.

## Tarifs réservés à des clients : prix au cas par cas (lot B10)

Une règle tarifaire peut viser un ou plusieurs clients, avec le champ `audience` : une liste de `{ type, code }`, avec les mêmes types que la [visibilité](visibilite.md) (`MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`, `SEGMENT`, `PLAN`).

- **Prix au cas par cas** : un prix de base (`price`) ou un prix imposé (`adjustmentType: FIXED_PRICE`) avec `audience: [{ "type": "CUSTOMER", "code": "CLI-ACME" }]`. Il ne s'applique qu'à ce client, et aux membres rattachés à cette organisation. Plusieurs clients dans la même liste partagent le même prix.
- **Tarif membre** : `audience: [{ "type": "SEGMENT", "code": "MEMBER" }]`, ou le champ `customerSegment`, équivalent pour un seul segment. Tarif des abonnés d'un plan : `{ "type": "PLAN", "code": "PREMIUM" }`.
- **Remise négociée** pour un client : ajustement `PERCENT_DELTA` ou `AMOUNT_DELTA` avec son `audience`.
- **Précédence** : une règle avec audience compte trois conditions de plus. À priorité égale, le prix d'un client l'emporte donc sur la grille générale, sans toucher à `priority`.
- **Confidentialité** : la fiche du portail ne montre jamais une règle réservée à d'autres clients ; le devis et la réservation appliquent le prix du client connecté.
- **Modifier** : `PUT /resource-pricing-rules/{id}` avec `audience` (remplace la liste), ou `clear: ["audience"]` pour une règle valable pour tous. Une règle identique (mêmes conditions et même audience) est refusée en `409`.
- **Écran** : une colonne « Clients » dans la grille tarifaire, un filtre « prix au cas par cas ». Le simulateur (`POST /resource-pricing-rules/simulate` avec `ownerType` et `ownerCode`) montre le prix d'un client précis.

