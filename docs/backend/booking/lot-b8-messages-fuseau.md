# Lot B8 : messages en français et fuseau horaire

> 18/11/2026. Écarts 18, Bn-26 et Bn-27. Aucune migration.

## Messages (écart 18)

- Les messages d'erreur du module réservation, de la pré-réservation, de la liste d'attente, des politiques et du portail sont en français (environ 60). Le préfixe « Could not process booking: » a disparu : le message est rendu tel quel.
- Un statut est nommé en français (`BookingStatus.label()` : « confirmée », « en attente de paiement », « marquée absente »…). Exemple : « Action impossible : la réservation est annulée, il faut qu'elle soit confirmée ».
- L'historique et le journal d'événements d'une réservation, visibles par l'équipe, sont en français (« Réservation créée », « Créneau retenu après le report », « Arrivée pointée », « Droit consommé »…). Les lignes déjà écrites gardent leur texte d'origine.
- Les codes d'erreur ne changent pas : le frontend qui teste un code continue de fonctionner. Celui qui testait un texte anglais doit passer au code.

## Fuseau (écart Bn-26)

- Nouveau `AppTime` (`core/utils/time`) : « maintenant » dans le fuseau `app.time-zone`, Africa/Brazzaville par défaut. Un fuseau mal écrit est signalé dans les journaux et remplacé par le défaut, sans bloquer le démarrage.
- Le module réservation et les ressources prennent l'heure et la date du jour par `AppTime`, et convertissent les créneaux en instants avec ce fuseau (rappels, tâches de préparation, fin de droit, dépassement au départ). Sur un serveur en UTC, plus d'heure de décalage : un départ à l'heure n'est plus facturé une heure de dépassement, un rappel part à l'heure.
- Le fuseau de la JVM n'est pas modifié : le pilote PostgreSQL l'utiliserait pour la session et décalerait les colonnes horodatées sans fuseau.

## Annonces d'espèces et portail (écart Bn-27)

- Annonce d'espèces échue : la réservation encore en attente est annulée, et sa facture avec, comme toute réservation impayée échue (lot B1). La documentation du traitement disait la facture maintenue ; elle décrit maintenant ce qui se passe.
- Portail : le créneau d'une réservation en attente de paiement est réellement retenu depuis le lot B1. Le commentaire qui le promettait est donc exact.

## Tests

`AppTimeTest` (fuseau par défaut, fuseau mal écrit, heure de l'application). Les tests qui attendaient les anciens messages en anglais sont alignés.
