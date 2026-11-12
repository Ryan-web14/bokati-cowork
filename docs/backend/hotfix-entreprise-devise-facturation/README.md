# Correctif : devise de facturation d'une organisation

## Constat

`PATCH /sni/api/v1/companies/{customerId}/billing` refusait `currencyCode` (`400 VALIDATION_FAILED`, « Propriete inconnue dans le corps de la requete : currencyCode »). La devise ne pouvait être choisie qu'à la création de l'organisation, et la fiche ne la renvoyait pas.

## Correction

- `CompanyDtos.UpdateBillingRequest` accepte `currencyCode` (3 lettres, facultatif).
- `CompanyAccountService.updateBilling` : si `currencyCode` est présent, la devise du profil légal (`business_entity.base_currency_id`) est remplacée. Le profil légal est créé au besoin, comme pour `PATCH /legal`. Une devise inconnue répond `400` « Devise inconnue : XXX ». Sans `currencyCode`, la devise ne change pas.
- `CompanyDtos.CompanyResponse` renvoie `currencyCode`, sur toutes les routes qui rendent la fiche.
- L'auteur du changement est noté sur le profil légal (`updated_by`).

Aucune migration.

## Portée

La devise de l'organisation est une donnée de fiche. Elle ne convertit pas les factures déjà émises. Une facture prend la devise de ce qu'elle facture (abonnement, réservation, devis), pas celle de l'organisation.

## Tests

`CompanyBillingCurrencyTest` : la devise change et revient dans la réponse ; sans `currencyCode`, rien ne change ; une devise inconnue est refusée.

## Docs mises à jour

`docs/final-frontend-doc/company/routes/CompanyAccountController.md` et `routes.json` : `currencyCode` dans `UpdateBillingRequest` et `CompanyResponse`.
