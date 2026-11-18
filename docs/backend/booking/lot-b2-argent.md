# Lot B2 : argent

> Partie du [plan booking](README.md). Fait le 18/11/2026 sur `feature/booking`. Migration V384 (prod V378).

## Recalcul (écart Bn-1)

Le recalcul d'une réservation déplacée ou changée de ressource :

- garde les remises (lignes `DISCOUNT` : code promo, remise de lot) ;
- ne compare que le prix de la ressource. Avant, un déplacement au même prix refacturait la remise en complément ;
- garde l'unité imposée par l'équipe (nouvelle colonne `unit_forced`) ;
- met à jour la ligne du droit d'abonnement.

## Droit d'abonnement (écart Bn-2)

- La quantité retenue sur le droit à la confirmation est gardée (`entitlement_quantity`) et rendue telle quelle. Avant, elle était recalculée sur la nouvelle durée : des heures perdues ou rendues en trop.
- Déplacement : le droit est rendu sur l'ancienne durée, puis repris sur la nouvelle.
- Changement de ressource : le droit est rendu, le contexte de paiement est résolu pour la nouvelle ressource (le droit doit la couvrir), puis repris.

## Couverture partielle par les heures incluses (reste du chantier ressources)

- Quand le solde du droit ne couvre qu'une partie de la réservation et que le plan n'autorise pas le dépassement, les minutes couvertes sont prises sur le droit (`covered_minutes`). Le reste est facturé au prorata du prix.
- Exemple : 4 h à 20 000, solde 1 h 30, d'où 12 500 facturés.
- Avec dépassement autorisé, rien ne change : le module des dépassements facture.
- La ligne ressource porte le montant facturé ; la facture de la réservation est émise pour ce reste.

## Annulation (écarts Bn-3, Bn-4, Bn-6)

- **Reste dû** : une facture payée en partie voit son reste annulé par un avoir. Avant, il restait dû après l'annulation.
- **Rien d'engagé** (réservation en attente) : tout le payé est rendu.
- **Suppléments et compléments** facturés à part : même traitement que la facture de la réservation (annulés s'ils sont impayés, sinon reste annulé et remboursement selon le pourcentage). La caution suit le registre des cautions.
- **Une seule source de règles** : la politique de la ressource porte sa grille de remboursement (`refundRules`, « heures:pourcentage » : `72:100,24:50,0:0`) ; sans grille, la grille générale (`/bookings/cancellation-policy`) s'applique. Le préavis et l'autorisation d'annuler viennent aussi de la politique.

## Échec du remboursement (écart Bn-5)

Un avoir ou un remboursement automatique en échec crée une tâche « Remboursement à traiter » (priorité haute, échéance 24 h) et un événement sur la réservation. Avant, il n'était que journalisé.

## Reporté

Écart Bn-7 (total sans les suppléments dans la réponse) : traité avec la réponse complète au lot B7 (`extrasAmount` et `grandTotal`), sans changer le sens de `totalAmount` (montant de la ressource, remises déduites), qui sert la facture.

## Tests

`BookingB2Test` (part non couverte au prorata, grille de la politique) ; `BookingCancellationRefundSupportTest` adapté.
