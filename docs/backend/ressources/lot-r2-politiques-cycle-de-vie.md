# Lot R2 : politiques et cycle de vie

> Partie du [plan ressources](README.md). Fait le 18/11/2026 sur `feature/ressources`.

## Politiques

- Politique en vigueur (`Resource.getEffectivePolicy()`) : une politique désactivée ne s'applique plus nulle part (contrôle des réservations, des options, des créneaux, de l'annulation, du devis, du portail) ; la ressource est traitée comme sans politique.
- Une politique désactivée ne se rattache plus à une ressource (`400`).
- `allowCancellation` vrai par défaut à la création (D3). Les politiques existantes gardent leur valeur.

## Suppressions refusées proprement

Groupe, type, politique : `409` s'il reste des ressources rattachées, archivées comprises (requêtes natives qui comptent aussi les ressources supprimées en douce). Avant : `500` sur la clé étrangère pour le groupe, et un contrôle qui ignorait les ressources archivées pour le type et la politique.

## Réservations à venir (D4)

`ResourceImpactGuard`, alimenté par le module des réservations (`ResourceBookingImpact`, mis en œuvre par `BookingResourceImpactAdapter`, sans dépendance en boucle) :

- suppression d'une ressource, passage en `MAINTENANCE`, `OUT_OF_SERVICE`, `INACTIVE` ou `ARCHIVED`, fermeture (création ou réactivation) : refus `409 RESOURCE_HAS_BOOKINGS` avec le nombre et les numéros des réservations actives touchées (confirmées, en cours, en attente d'approbation ou de paiement) ;
- option explicite `cancelBookings=true` (paramètre de `DELETE /resources/{code}` et `PATCH /resources/{code}/status`, champ de la fermeture et de son activation) : annulation par l'équipe (`cancelForResourceUnavailability`, sans contrôle du délai, client prévenu par courriel, remboursement selon les règles d'annulation existantes) ;
- aperçu : `GET /resources/{code}/impacted-bookings?from=&to=`.

## Divers

- Étage (`floorLabel`) enregistré et rendu (colonne `resource.floor_label`).
- Motif (`reason`) rendu dans la réponse d'une fermeture.

## Migration

Dev `V375__ressources_r2_etage_annulation.sql`, prod `V369`.

## Tests

`ResourceR2Test` : politique désactivée sans effet, annulation permise par défaut, réservations à venir bloquantes sauf annulation, rien à annuler, groupe avec ressources non supprimé.
