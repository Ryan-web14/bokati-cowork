# Factures d'un membre pour une organisation

Préfixe des routes : `/sni/api/v1`.

Une facture reste au nom du membre ; elle peut être émise **pour le compte** d'une organisation à
laquelle il est rattaché. Elle compte alors dans l'encours de l'organisation, ses relances partent
aussi au courriel de facturation de l'organisation, et le PDF imprime l'organisation en tête du
destinataire (raison sociale, identifiants saisis), puis « À l'attention de » le membre.

## Quelle organisation

| Champ `companyCustomerCode` de la facture | Effet |
| --- | --- |
| absent | selon le mode du rattachement : l'organisation en `AUTOMATIC` s'il y en a une, sinon le membre seul |
| `"NONE"` | le membre seul, même si son rattachement est en `AUTOMATIC` |
| un code client | cette organisation ; le membre doit y être rattaché et actif, sinon 400 |

Côté **réservation du portail**, le choix se fait par `billingCompanyCode` dans
`POST /client/bookings` (même valeurs). Il est vérifié dès la réservation et suit jusqu'à la
facture. `GET /client/companies` donne les organisations du membre et le mode qui s'applique : en
`ON_CHOICE`, proposer « Pour moi » / « Pour <organisation> ».

Une organisation bloquée, ou dont le plafond d'encours serait dépassé, refuse aussi les factures de
ses membres émises pour son compte.

La réponse d'une facture porte `companyCustomerCode`, `companyName` et `companyLinkSource`
(`AT_ISSUE` : émise ainsi ; `ATTACHED` : ancienne facture reliée).

## Relier d'anciennes factures (lien seulement)

Le document émis ne change pas : ni nom, ni PDF. La facture rejoint l'encours et le compte de
l'organisation.

| Méthode et chemin | Effet |
| --- | --- |
| `POST /billing/documents/company-links/preview` | ce qui serait relié, et pourquoi le reste ne le serait pas ; rien n'est écrit |
| `POST /billing/documents/company-links` | relie les factures admissibles |
| `DELETE /billing/documents/{documentNumber}/company-link` | retire un lien `ATTACHED` ; un lien `AT_ISSUE` est imprimé et ne se retire pas |

```json
{ "companyCustomerCode": "CUS-COM-202610-00000012", "documentNumbers": ["INV-...", "INV-..."] }
```
ou
```json
{ "companyCustomerCode": "CUS-COM-202610-00000012", "memberId": "MBR-...", "from": "2026-01-01", "to": "2026-10-31" }
```

Admissible : facture (pas devis ni avoir) au nom d'un membre, émise (pas brouillon), membre
rattaché à l'organisation à un moment (lien actif ou terminé), pas déjà reliée ailleurs.
