# Lot R6 : messages en français

> Partie du [plan ressources](README.md). Fait le 18/11/2026 sur `feature/ressources`.

Environ 170 messages d'erreur du module des ressources passés en français : validations (« Demande de créneau invalide : la quantité doit être supérieure à zéro »), introuvables (« Ressource introuvable : RES-... »), conflits, règles de la politique (durée, préavis), champs requis.

- Les conflits ne portent plus le préfixe anglais « Could not process ... : » (forme à deux arguments de `ConflictException` remplacée par le message seul).
- Les codes d'erreur (`code` de la réponse) ne changent pas : le frontend qui s'appuie sur eux n'est pas touché ; celui qui comparait le texte anglais doit passer aux codes.
- Les journaux (logs) restent tels quels.

Tests : `ResourceAvailabilityServiceImplTest` adapté au message français.
