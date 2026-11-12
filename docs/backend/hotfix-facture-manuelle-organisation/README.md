# Correctif : facture manuelle d'un membre pour le compte d'une organisation

## Constat

`POST /sni/api/v1/billing/invoices/manual` (ainsi que les devis manuels et la facture rectificative) refusait `companyCustomerCode` : `400 VALIDATION_FAILED`, « Propriete inconnue dans le corps de la requete : companyCustomerCode ». La création standard d'un document l'acceptait déjà, mais la requête manuelle n'avait pas le champ.

## Correction

- `CreateManualBillingDocumentRequest` accepte `companyCustomerCode`, facultatif, avec le synonyme `customerCompanyCode`. Il est transmis tel quel à la création du document. La règle est celle de la création standard :
  - vide : la facture d'un membre suit le mode de son rattachement (automatique ou membre seul) ;
  - `NONE` : le membre seul ;
  - un code client : cette organisation, à laquelle le membre doit être rattaché.
- Facture rectificative sans destinataire saisi : elle garde le payeur de la facture d'origine, c'est-à-dire son organisation, ou le membre seul (`NONE`) si la facture d'origine n'en avait pas. Elle ne repasse pas par le mode automatique.
- Les appels internes qui construisent la requête sans ce champ continuent de compiler, grâce à un constructeur sans organisation.

Aucune migration.

## Tests

`ManualInvoiceCompanyJsonTest` vérifie trois cas, avec le rejet des propriétés inconnues activé comme en production :
- le champ `NONE` est accepté ;
- le synonyme est accepté ;
- un champ absent reste vide.

## Docs mises à jour

`docs/final-frontend-doc/billing/routes/BillingDocumentController.md` et `routes.json` : `companyCustomerCode` dans `CreateManualBillingDocumentRequest`.
