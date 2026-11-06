# Passes d'organisation partagés

Préfixe des routes : `/sni/api/v1`.

L'organisation détient le pass (`ownerType = CUSTOMER`, son code) et ses droits. L'administration
ou le gérant ouvre l'accès à un collaborateur rattaché ; il réserve alors en mode `PASS`, le droit
est décompté sur ce pass chez l'organisation, et il reste noté comme bénéficiaire.

- La réservation essaie d'abord les passes du membre, puis ceux d'une organisation dont il est
  bénéficiaire. Le choix entre organisations suit la règle des places (`billingCompanyCode`, lien
  automatique, seule organisation, sinon 400).
- Un accès sert si le lien est actif, le pass actif dans sa validité, et l'accès ouvert : sa
  période (`validFrom`, `validUntil`) et ses passages (`maxUses`, comptés au contrôle d'entrée).
- Un accès retiré puis rouvert reprend la même ligne, compteur remis à zéro.
- La fin du rattachement retire tous ses accès aux passes de l'organisation. Ses réservations à
  venir sur ces passes sont signalées (`warning`), pas annulées.
- Un pass n'a pas de dépassement : au-delà de son solde, la réservation est refusée.

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Administration | `GET /companies/{customerId}/passes` | passes non clos de l'organisation, nombre d'accès ouverts |
| Administration | `GET /companies/{customerId}/passes/{passNumber}/beneficiaries` | droits du pass, accès et consommation par collaborateur |
| Administration | `POST /companies/{customerId}/passes/{passNumber}/beneficiaries` | ouvre l'accès `{ memberId, maxUses?, validFrom?, validUntil? }` |
| Administration | `POST /companies/{customerId}/passes/{passNumber}/beneficiaries/{id}/revoke` | retire l'accès `{ reason? }` |
| Gérant | les mêmes sous `/client/companies/{customerId}/passes...` | comme l'administration |
| Gérant | `POST /client/companies/{customerId}/passes/purchase` | achète un pass du catalogue au nom de l'organisation `{ planCode, passType, name, idempotencyKey }` |
| Collaborateur | `GET /client/me/company-passes` | les passes de ses organisations auxquels il a accès, et ce qu'il en reste |

Un pass acheté par le gérant est au nom de l'organisation et facturé à elle. Il s'active au
paiement de sa facture, réglée par les moyens de l'organisation : mobile money, espèces, ou son
portefeuille (`POST /client/companies/{customerId}/invoices/{n}/pay/...`).

`id` est le numéro court de l'accès. Extrait de `beneficiaries` :

```json
{
  "companyCustomerCode": "CUS-COM-...",
  "pass": { "passNumber": "PAS-...", "name": "Carnet 20 h", "status": "ACTIVE", "beneficiaries": 3 },
  "entitlements": [{ "entitlementCode": "SALLE_H", "unit": "HOUR", "unlimited": false, "granted": 20, "remaining": 11 }],
  "beneficiaries": [{
    "id": 4, "memberId": "MBR-...", "memberName": "Jean Mbemba", "open": true, "maxUses": 10, "usedCount": 2,
    "consumption": [{ "entitlementCode": "SALLE_H", "consumed": 6 }]
  }]
}
```
