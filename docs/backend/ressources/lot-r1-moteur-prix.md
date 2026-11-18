# Lot R1 : un seul moteur de prix

> Partie du [plan ressources](README.md). Première partie faite le 18/11/2026 sur `feature/ressources`.

## Avant

Trois calculs : la réservation (`BookingPricingCalculator`, dernière règle de l'unité, demi-journée 5 h, journée 10 h, conditions ignorées), le devis et l'aperçu (`ResourcePricingRuleServiceImpl.quote`, conditions appliquées, demi-journée 12 h, journée 24 h, quantité ignorée), le catalogue (`ServiceCatalogServiceImpl`, dernière règle toutes unités). Le portail affichait le devis, la facture venait de la réservation.

## Moteur

`ressource/service/pricing/ResourcePriceEngine`, seul point de calcul :

- **Règle de base** d'une unité (sans ajustement, avec un prix) : priorité, puis précision (nombre de conditions), puis la plus récente, parmi celles qui s'appliquent.
- **Ajustements** : `FIXED_PRICE` remplace la base (le meilleur) ; puis le meilleur écart non cumulable et tous les écarts `stackable`, par priorité ; pourcentage en décimal (avant : division entière) ; jamais sous zéro.
- **Conditions** : validité, jours (`days_of_week`, plusieurs), plage horaire (même passant minuit), segment de client, plan, participants, durée, délai avant le début (`min_lead_minutes`, `last_minute_minutes`), canal.
- **Tranches** à l'heure : bornes horaires des règles et minuits, au prorata.
- **Unité** : imposée par l'équipe, sinon la moins chère pour le client ; à égalité, celle de la durée.
- **Forfaits** : demi-journée 5 h et journée 10 h en paramètres (D2) ; sur plusieurs jours, un forfait par jour touché.
- **Quantité** : salle entière une fois, poste partagé par place, règle `per_participant` par participant.
- **Résultat** (`PriceResult`) : unité, unités, quantité, montant, tranches avec règles et ajustements, coût de chaque unité possible.

`PriceContextResolver` donne le contexte du titulaire, le même pour le devis et la réservation : segments (membre, abonné et code de plan par l'abonnement en cours, entreprise, client hors membre) et canal (`PORTAL` sous `/client/` ou `/portal/`, `TEAM` sinon).

## Appels

- Réservation : `BookingPricingCalculator` délègue au moteur avec le contexte du titulaire ; trace JSON enregistrée (`booking.price_trace`).
- Vérification de disponibilité : même contexte (membre, client ou entreprise de la demande).
- Suggestions de créneaux : prix public indicatif.
- Devis (`GET /resource-pricing-rules/quote`, nouveaux paramètres `quantity`, `ownerType`, `ownerCode`, unité facultative), simulateur (`POST /resource-pricing-rules/simulate`), portail (membre connecté, canal `PORTAL`, unité jamais imposée, comme la réservation du portail).
- Catalogue : prix de base de la plus petite unité, client public, règles valides et non supprimées (`listPriceRule`).

## Changement de comportement

L'unité la moins chère est retenue : 4 heures à 5000 l'heure (20000) passent à la demi-journée à 18000 ; 8 heures à la journée à 30000 au lieu de 40000. Les réservations existantes gardent leur prix.

## Migration

Dev `V373__ressources_r1_moteur_prix.sql`, prod `V367` : conditions nouvelles des règles, `booking.price_trace`.

## Reste au lot R1

Recalcul quand l'horaire, la durée ou la ressource d'une réservation change, avec facture complémentaire ou avoir (D1) ; versions des règles ; signalement des règles en conflit à l'enregistrement.

## Tests

`ResourcePriceEngineTest` (heures pleines au prorata, base la plus précise un samedi, remise membre, cumul des ajustements, prix fixe d'un plan, salle entière ou par participant, réservation anticipée, règle supprimée ignorée, plusieurs jours) ; `BookingPricingCalculatorTest` adapté (unité la moins chère).
