# Tarifs : un seul moteur de prix

> Fiche du module [ressource](../README.md), d'après les lots R1, R5a, R5e et R7 du [plan ressources](../../../backend/ressources/README.md) (18/11/2026). Routes exactes et champs JSON : [routes.md](../routes.md).

Un seul moteur de prix (lot R1, 18/11/2026) sert la réservation, la vérification de disponibilité, le devis, le portail et le catalogue : **le devis annonce ce qui sera facturé**.

1. Créer : `POST /sni/api/v1/resource-pricing-rules` avec `resourceCode`, `bookingUnit` (`HOUR`, `HALF_DAY`, `DAY`, `WEEK`, `MONTH`), `label`, et selon le rôle de la règle :
   - **prix de base** : `price` (entier ≥ 0, en XAF), sans `adjustmentType` ;
   - **ajustement** : `adjustmentType` (`FIXED_PRICE` remplace la base, `AMOUNT_DELTA` ajoute un montant, `PERCENT_DELTA` un pourcentage) et `adjustmentValue` (positif : majoration ; négatif : remise). **Pas de `price`** : depuis le 23/11/2026 il n'est plus demandé, et s'il est envoyé il est ignoré (la règle est enregistrée avec `price = null`). Dans le formulaire, masquer le champ prix dès qu'un type d'ajustement est choisi. `stackable = true` pour qu'il se cumule (voir « Superposition des règles »).
   - **Conditions**, toutes facultatives et combinables : `daysOfWeek` (« 1,2,3,4,5 », 1 = lundi … 7 = dimanche ; remplace `dayOfWeek`), `startsAt`/`endsAt` (`HH:mm`, une plage qui passe minuit est admise), `validFrom`/`validUntil` (`AAAA-MM-JJ`), `customerSegment` (`PUBLIC` client hors membre, `MEMBER`, `SUBSCRIBER` abonnement en cours, `COMPANY`, `PARTNER`), `planCode` (plan d'abonnement), `minParticipants`/`maxParticipants`, `minDurationMinutes`/`maxDurationMinutes`, `minLeadMinutes` (réservation anticipée : au moins ce délai avant le début), `lastMinuteMinutes` (dernier moment : au plus ce délai), `channel` (`PORTAL` ou `TEAM`).
   - `perParticipant = true` : prix par participant, même pour une salle louée entière. `priority` (défaut 0) départage, puis la règle la plus précise (le plus de conditions), puis la plus récente.
2. Liste : `GET /resource-pricing-rules?resourceCode=...`, mêmes champs en réponse. L'`id` (petit numéro) sert dans `GET`, `PUT`/`PATCH`, `DELETE /resource-pricing-rules/{id}` et `PATCH /{id}/active?active=...`. Mise à jour partielle ; une chaîne vide efface une condition texte (`daysOfWeek`, `customerSegment`, `planCode`, `channel`).
3. Plusieurs ressources : `POST /resource-pricing-rules/bulk/preview` (aucune écriture), puis `POST /resource-pricing-rules/bulk`.
4. Devis : `GET /resource-pricing-rules/quote?resourceCode=...&startedAt=...&endedAt=...` avec, facultatifs, `quantity` (postes ou participants), `ownerType` (`MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`) et `ownerCode` pour le prix de ce client, `bookingUnit` pour imposer une unité (équipe). Réponse : `bookingUnit` retenue, `units`, `quantity`, `amount` (montant exact), `finalPrice` (arrondi), `currency`, `lines` (tranches : début, fin, unités, prix d'une unité, règle de base, ajustements, montant), `alternatives` (coût de chaque unité possible, `chosen` sur la retenue) ; `basePrice`, `appliedRuleId`, `appliedRuleLabel`, `adjustmentType`, `adjustmentValue` restent pour la première tranche.
5. Simulateur : `POST /sni/api/v1/resource-pricing-rules/simulate` avec `{ resourceCode, startedAt, endedAt, quantity, bookingUnit, ownerType, ownerCode, channel }` : même réponse, pour vérifier une grille avant de la publier (droit `RESOURCE:PRICE`).
6. Versions : `GET /sni/api/v1/resource-pricing-rules/{id}/history` : chaque création, modification, activation, désactivation et suppression d'une règle, avec la règle complète (`snapshot`, JSON), l'auteur et la date, la plus récente d'abord. La suppression est douce : la règle disparaît des listes et du calcul, son historique reste.
7. Cohérence : une règle identique à une règle active (même unité, même rôle, mêmes conditions, même priorité) est refusée en `409` ; `GET /sni/api/v1/resource-pricing-rules/conflicts?resourceCode=...` liste les paires ambiguës (deux prix de base, ou deux ajustements non cumulables, de même priorité et de même précision sur les mêmes créneaux : la plus récente l'emporte). Afficher ces conflits sur l'écran des tarifs.

Règles de calcul :

- **Unité** : imposée par l'équipe (`bookingUnit`, un forfait imposé vaut un forfait), sinon **la moins chère pour le client** parmi les unités qui portent des règles (prix manquant déduit, voir plus bas) ; à égalité, celle de la durée. Exemple : 8 heures à 5000 l'heure (40000) coûtent plus qu'une journée à 30000 : la journée est retenue.
- **Durées** : demi-journée 5 h, journée 10 h (paramètres `app.resources.pricing.half-day-hours`, `app.resources.pricing.day-hours`) ; sur plusieurs jours, un forfait journée par jour touché (deux demi-journées par jour) ; semaine par tranche de 7 jours ; mois calendaire.
- **À l'heure**, la réservation est découpée aux bornes horaires des règles et à minuit : heures creuses et heures pleines au prorata (`lines`). Un forfait est tarifé à l'heure de début.
- **Quantité** : une salle louée entière se facture une fois ; un poste partagé, par place ; une règle `perParticipant`, par participant.
- **Client** : le portail et la réservation appliquent le segment du titulaire (membre, abonné et son plan, entreprise, client hors membre) et le canal (`PORTAL` pour l'espace client, `TEAM` pour le back-office).
- Le détail du calcul est enregistré avec la réservation (`priceTrace`, même forme que le devis) : une facture reste explicable après un changement de grille.

## Prix manquant et superposition des règles (23/11/2026)

Correctif du moteur, d'après les règles réelles d'une salle : un prix horaire posé pour le lundi de 8 h à 18 h, un forfait journée, une majoration de +20 % le samedi de 11 h à 23 h. Avant, un samedi (ou un lundi après 18 h), le moteur abandonnait le calcul à l'heure : la journée entière était facturée, ou l'heure imposée était refusée, et la majoration ne s'appliquait jamais.

**Prix de base manquant à un instant.** Une unité qui porte au moins une règle (prix ou ajustement) est toujours calculée. Quand aucun de ses prix de base ne couvre un moment, le prix est trouvé dans cet ordre :
1. **déduit d'une unité voisine** qui couvre ce moment, au prorata : l'heure depuis la demi-journée (5 h) puis la journée (10 h) ; la demi-journée depuis la journée puis l'heure ; la journée depuis la demi-journée puis l'heure. La ligne du devis l'explique : « Déduit du prix à la journée … (105014 XAF pour 10 h, ramené à l'heure) » ;
2. sinon, **prix de référence** de l'unité : son prix de base le plus général qui vise ce client, appliqué hors de ses jours ou heures (ligne « Prix de référence … ») ;
3. les ajustements de l'unité (majoration du samedi, remise membre…) s'appliquent ensuite normalement.

Une unité sans aucune règle n'est jamais inventée, même imposée par l'équipe (`400`, comme avant).

Exemple, la salle ci-dessus, samedi 12 h-14 h : 105014 / 10 = 10501,40 l'heure, +20 % = 12601,68, deux heures = **25203,36 XAF**, à l'heure (moins cher que la journée). Lundi 17 h-19 h : 15000 (heure ouvrable) + 10501,40 (heure suivante) = 25501,40.

**Plus de réservation gratuite par erreur.** Une ressource sans aucun prix de base reste gratuite. Une ressource qui a des prix, mais aucun pour ce client (prix réservés aux membres, à un plan, à des clients désignés), répond `400` « Aucun prix de la ressource … ne s'applique à ce client pour ce créneau » au lieu d'un prix nul. Les suggestions de créneaux montrent alors le créneau sans montant (`amount = null`).

**Superposition des règles d'une même unité** (dans l'ordre) :
1. prix de base (ou déduit) ;
2. `FIXED_PRICE` : le plus prioritaire remplace la base ;
3. **majorations** (valeur positive) : la plus prioritaire des non cumulables, plus toutes les cumulables ;
4. **remises** (valeur négative) : parmi les non cumulables, la plus prioritaire et, à priorité égale, **la plus avantageuse pour le client** ; plus toutes les cumulables.

Une majoration et une remise ne s'excluent jamais : un membre qui réserve un samedi paie la majoration puis sa remise (10000 + 20 % = 12000, - 10 % = 10800). Avant, un seul ajustement non cumulable s'appliquait, toutes natures confondues. « Non cumulable » veut dire désormais : ne se cumule pas avec une autre majoration (ou une autre remise) non cumulable.

**Plage horaire qui passe minuit** : `startsAt = 22:00`, `endsAt = 06:00` est accepté (avant : refusé « endsAt doit suivre startsAt ») ; seule une plage vide (début = fin) est refusée.

**Suggestions de créneaux** : calculées au prix du client de la demande (membre, client, entreprise), comme la vérification de disponibilité.

## Jours fériés (lot R5a)

Condition `holidayMode` d'une règle : `ONLY` (jours fériés seulement, par exemple une majoration), `EXCLUDED` (hors jours fériés), vide pour tous les jours. Jours fériés : nationaux du Congo calculés, plus ceux ajoutés par l'équipe ([calendrier.md](calendrier.md)).

## Modifier une règle (lot R7)

`PUT /resource-pricing-rules/{id}` : mise à jour partielle ; `clear` vide des conditions, par exemple `{ "clear": ["startsAt", "endsAt", "validUntil"] }`. Noms admis : `startsAt`, `endsAt`, `validFrom`, `validUntil`, `dayOfWeek`, `daysOfWeek`, `lastMinuteMinutes`, `minLeadMinutes`, `minParticipants`, `maxParticipants`, `minDurationMinutes`, `maxDurationMinutes`, `customerSegment`, `planCode`, `channel`, `holidayMode`, `adjustment` (type et valeur). La règle modifiée est revalidée comme à la création (`400` `VALIDATION_FAILED` sinon : plage horaire vide, min au-dessus du max, prix de base sans prix, ajustement sans valeur…), puis comparée aux règles existantes (doublon `409`).

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

