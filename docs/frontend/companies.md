# Centre de gestion des entreprises · fiche organisation

Une organisation cliente (société, établissement, entrepreneur individuel, profession libérale,
association, ONG, institution publique) se crée en un appel et se met à jour par bloc. Sa fiche
alimente la facture : identifiants saisis, adresse et courriel de facturation, échéance,
exonération de TVA, blocage et plafond d'encours.

Rôles : `ADMIN`, `SUPER_ADMIN`, `MANAGER`. Préfixe : `/sni/api/v1`.

## Routes

| Méthode et chemin | Effet |
| --- | --- |
| `POST /companies` | crée l'organisation (client `COMPANY` + profil légal) |
| `GET /companies?q=&status=&page=&size=` | liste ; `q` cherche dans le nom, les courriels, le code client et tous les identifiants |
| `GET /companies/{customerId}` | la fiche entière |
| `PATCH /companies/{customerId}/legal` | identité légale, identifiants, TVA |
| `PATCH /companies/{customerId}/billing` | facturation |
| `PATCH /companies/{customerId}/contact` | contact |
| `POST /companies/{customerId}/block` | bloque, `{ "reason": "…" }` obligatoire |
| `POST /companies/{customerId}/unblock` | débloque |
| `POST /companies/{customerId}/legal-profile` | relie un profil légal existant, `{ "businessCode": "…" }` |

## Types d'organisation et identifiants

`organizationType` : `SOCIETE`, `ETABLISSEMENT`, `ENTREPRENEUR_INDIVIDUEL`, `PROFESSION_LIBERALE`,
`ASSOCIATION`, `ONG`, `INSTITUTION_PUBLIQUE`. La réponse donne `suggestedIdentifiers`, la liste à
proposer dans le formulaire pour ce type.

| `type` d'identifiant | Libellé imprimé | Format vérifié |
| --- | --- | --- |
| `NIU` | NIU | une lettre (P ou E) suivie de 15 chiffres |
| `RCCM` | RCCM | OHADA `CG-BZV-01-2024-B12-00001` ; ancien format `CG/BZV/16 B 6261` accepté (réponse `format: "LEGACY"`) |
| `ORDRE_PROFESSIONNEL` | N° d'inscription à l'ordre | libre |
| `RECEPISSE` | N° de récépissé | libre |
| `AGREMENT` | N° d'agrément | libre |
| `ACTE_CREATION` | Acte de création | libre |
| `CODE_ADMINISTRATIF` | Code administratif | libre |
| `AUTRE` | `label` obligatoire | libre |

**Aucun identifiant n'est obligatoire.** Seuls ceux qui sont saisis sont imprimés sur la facture.
Un identifiant mal formé est refusé en 400, avec le format attendu dans le message.

## Créer

```json
POST /companies
{
  "organizationType": "SOCIETE",
  "name": "SARL Mboté",
  "legalForm": "SARL",
  "activity": "Conseil",
  "identifiers": [
    { "type": "NIU", "value": "P200711000047346" },
    { "type": "RCCM", "value": "CG-BZV-01-2024-B12-00001" }
  ],
  "legalRepresentativeName": "Jean Mbemba",
  "legalRepresentativeTitle": "Gérant",
  "vatRegime": "SUBJECT",
  "email": "contact@mbote.cg",
  "billingEmail": "compta@mbote.cg",
  "phone": "+242061234567",
  "website": "https://mbote.cg",
  "address": { "streetNumber": "12", "streetName": "Avenue de la Paix", "city": "Brazzaville", "countryCode": "CG" },
  "billingAddress": null,
  "paymentTermsDays": 30,
  "creditLimit": 2000000,
  "currencyCode": "XAF"
}
```

- `legalForm` n'est vérifié que pour une société (SARL, SARLU, SA, SAU, SAS, SASU, SNC, SCS, SP,
  SCOOP, SCI) ; pour les autres types il est libre ou vide.
- `vatRegime` : `SUBJECT` (défaut) ou `EXEMPT`. `EXEMPT` exige `vatExemptionNote` (texte ou
  décision), imprimée sur la facture.

## Mettre à jour par bloc

Un champ absent reste inchangé. `identifiers`, quand il est fourni, **remplace la liste entière**.

```json
PATCH /companies/{customerId}/billing
{ "billingEmail": "factures@mbote.cg", "paymentTermsDays": 45, "creditLimit": 3000000 }
```

`"removeCreditLimit": true` retire le plafond.

## Effets sur la facturation

| Sur la fiche | Sur la facture |
| --- | --- |
| identifiants saisis | figés à l'émission et imprimés avec leur libellé, NIU et RCCM en tête |
| `billingEmail`, `billingAddress` (sinon `address`) | destinataire de la facture |
| `paymentTermsDays` | échéance = date d'émission + délai, si la facture n'en fixe pas |
| `vatRegime: EXEMPT` | lignes sans TVA ni centime ; un prix saisi TTC est ramené à son HT ; mention imprimée |
| organisation bloquée | toute facture refusée en 400, avec le motif ; devis et avoirs restent possibles |
| `creditLimit` | facture refusée si l'encours la dépasserait ; un `SUPER_ADMIN` passe outre |

## Réponse (extrait)

```json
{
  "customerId": "CUS-COM-202610-00000012",
  "name": "SARL Mboté",
  "organizationType": "SOCIETE",
  "organizationTypeLabel": "Société",
  "identifiers": [
    { "type": "NIU", "label": "NIU", "value": "P200711000047346", "format": "STANDARD" },
    { "type": "RCCM", "label": "RCCM", "value": "CG-BZV-01-2024-B12-00001", "format": "STANDARD" }
  ],
  "suggestedIdentifiers": ["NIU", "RCCM"],
  "vatRegime": "SUBJECT",
  "paymentTermsDays": 30,
  "creditLimit": 2000000,
  "outstanding": 450000,
  "status": "ACTIVE",
  "blockedReason": null,
  "membersCount": 4,
  "activeSubscriptions": 1
}
```

## Rattachement des membres

Un membre peut être rattaché à plusieurs organisations, une fois chacune, comme `OWNER`
(gérant ou propriétaire) ou `EMPLOYEE`. Ce lien ne change pas le payeur par défaut du membre.
L'`id` d'un rattachement est un numéro court, lisible par JavaScript.

### Administration (`ADMIN`, `SUPER_ADMIN`, `MANAGER`)

| Méthode et chemin | Effet |
| --- | --- |
| `GET /customers/{customerId}/members?status=` | actifs par défaut ; `ENDED` ou `ALL` pour l'historique |
| `POST /customers/{customerId}/members` | `{ "memberId", "role", "startedAt"?, "invoiceLinkMode"?, "canSign"?, "canCollectMail"? }` |
| `PATCH /customers/{customerId}/members/{id}` | `role`, `invoiceLinkMode`, `useCompanyDefaultMode`, `canSign`, `canCollectMail` |
| `POST /customers/{customerId}/members/{id}/end` | `{ "reason" }` · le lien passe `ENDED` et reste en historique |
| `GET /members/{memberId}/companies` | les organisations d'un membre, avec rôle et dates |
| `PATCH /companies/{customerId}/invoice-link-mode` | `{ "mode": "AUTOMATIC" \| "ON_CHOICE" }` · mode par défaut de l'organisation |

### Portail du membre

| Méthode et chemin | Pour qui |
| --- | --- |
| `GET /client/companies` | tout membre : ses organisations, son rôle, le mode qui s'applique ; `companyDefaultMode` pour le gérant |
| `GET /client/companies/{customerId}/members` | le gérant ; un employé reçoit 404 |
| `PATCH /client/companies/{customerId}/invoice-link-mode` | le gérant : `{ "mode" }` |
| `PATCH /client/companies/{customerId}/members/{id}/invoice-link-mode` | le gérant : `{ "mode" }` ou `{ "useCompanyDefaultMode": true }` |

### Règles

- Seule une organisation (`COMPANY`) reçoit des membres ; un seul lien actif par couple.
- Mode de rattachement des factures : `AUTOMATIC` (toutes les factures du membre reliées
  d'office) ou `ON_CHOICE` (choix à chaque fois). Défaut de l'organisation, remplaçable par lien.
- **Un membre n'a qu'une organisation en mode automatique**, mode hérité compris : sinon 400
  avec les noms en conflit.
- Fin de lien : si l'organisation était aussi le payeur par défaut du membre, il repasse sur sa
  fiche personnelle (`payerChanged: true`). Si elle ne peut pas être créée (courriel ou
  téléphone déjà pris), le lien est quand même terminé et `warning` le dit.
- L'abonnement d'une organisation couvre ses membres rattachés actifs pour les créneaux
  réservés aux abonnés.
- Reprise : les membres dont le payeur était une organisation y sont rattachés comme
  `EMPLOYEE` ; les gérants se désignent ensuite.

## Factures d'un membre pour une organisation

Une facture reste au nom du membre ; elle peut être émise **pour le compte** d'une organisation à
laquelle il est rattaché. Elle compte alors dans l'encours de l'organisation, ses relances partent
aussi au courriel de facturation de l'organisation, et le PDF imprime l'organisation en tête du
destinataire (raison sociale, identifiants saisis), puis « À l'attention de » le membre.

### Quelle organisation

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

### Relier d'anciennes factures (lien seulement)

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

## États de compte

Un état de compte dit ce qui a été facturé sur une période, ce qui a été réglé, ce qui reste dû et
depuis combien de temps.

- **Période** : `from` et `to` (dates ISO), douze mois jusqu'à aujourd'hui par défaut, trois ans
  au plus.
- **Ce qui reste dû se lit à la date de l'état**, période ou non : une facture de mars encore
  impayée figure dans l'état de septembre, avec `inPeriod: false` (en italique dans le PDF). Elle
  compte dans `balanceDue` et l'ancienneté, pas dans `invoiced`.
- Les brouillons n'y figurent jamais.
- **Organisation** : ses propres documents, plus les factures de membres qui lui sont reliées
  (`AT_ISSUE` ou `ATTACHED`) et les avoirs émis sur ces factures.
- **Membre** : toutes ses factures, réparties entre son compte personnel (`PERSONAL`) et chaque
  organisation à laquelle une facture est reliée (`COMPANY`).

### Routes

| Qui | Méthode et chemin |
| --- | --- |
| Administration | `GET /companies/{customerId}/statement` (et `.pdf`, `.csv`) |
| Administration | `GET /members/{memberId}/statement` (et `.pdf`, `.csv`) |
| Gérant (portail) | `GET /client/companies/{customerId}/statement` (et `.pdf`) |
| Membre (portail) | `GET /client/billing/statement` (et `.pdf`) |

### Réponse (extrait)

```json
{
  "subject": "COMPANY",
  "code": "CUS-COM-202610-00000012",
  "name": "SARL Mboté",
  "from": "2025-10-31", "to": "2026-10-30", "asOf": "2026-10-30",
  "creditLimit": 500000, "paymentTermsDays": 30, "blocked": false,
  "totals": [{
    "currency": "XAF",
    "invoiced": 350000, "paid": 240000,
    "balanceDue": 110000, "overdue": 60000,
    "creditAvailable": 0, "netBalanceDue": 110000,
    "openInvoices": 2, "overdueInvoices": 1,
    "aging": { "notDue": 50000, "days1To30": 0, "days31To60": 60000, "days61To90": 0, "over90": 0 }
  }],
  "accounts": [
    { "accountType": "COMPANY", "accountCode": "CUS-COM-...", "accountName": "SARL Mboté", "balanceDue": 60000, "...": "..." },
    { "accountType": "MEMBER", "accountCode": "MBR-...", "accountName": "Jean Mbemba", "balanceDue": 50000, "...": "..." }
  ],
  "lines": [{
    "documentNumber": "INV-...", "documentType": "INVOICE", "status": "OVERDUE",
    "issueDate": "2026-09-01", "dueDate": "2026-09-15", "currency": "XAF",
    "accountType": "COMPANY", "accountCode": "CUS-COM-...", "accountName": "SARL Mboté",
    "companyLinkSource": null, "totalAmount": 100000, "paidAmount": 40000, "balanceDue": 60000,
    "impact": 60000, "daysOverdue": 45, "inPeriod": true
  }]
}
```

`totals` contient une entrée par devise : deux devises ne s'additionnent pas. `impact` est l'effet
signé sur le solde (un avoir disponible compte en négatif, un document soldé ou annulé à zéro).

## Compte de l'organisation dans le portail du gérant

Réservé au gérant actif (`OWNER`). Pour tout autre membre, l'organisation répond 404. Une facture
qui n'est ni à l'organisation ni reliée à elle, ou qui est en brouillon, répond aussi 404.

| Méthode et chemin | Effet |
| --- | --- |
| `GET /client/companies/{customerId}/statement` | état de compte ; ses `lines` servent de liste des factures (celles avec `balanceDue > 0` sont à régler) |
| `GET /client/companies/{customerId}/invoices/{documentNumber}` | détail d'une facture (même forme que `/client/billing/invoices/{n}`) |
| `GET /client/companies/{customerId}/invoices/{documentNumber}/pdf` | PDF de la facture |
| `POST /client/companies/{customerId}/invoices/{documentNumber}/pay/mobile-money` | paiement mobile money, même corps que le paiement d'un membre (`phoneNumber`, `correspondent`) |
| `GET /client/companies/{customerId}/mobile-money/deposits/{depositId}` | suivi de ce paiement |
| `POST /client/companies/{customerId}/invoices/{documentNumber}/pay/cash` | « je passerai payer en espèces » ; la facture reste due jusqu'à la caisse |
| `GET /client/companies/{customerId}/payments/{transactionNumber}/receipt` (et `/pdf`) | reçu d'un paiement sur une facture de l'organisation |

Le gérant peut régler les factures de ses employés qui sont reliées à l'organisation : le
paiement porte l'identifiant du gérant comme initiateur, la facture reste celle du membre.

Pas de paiement par portefeuille ici : le portefeuille personnel du gérant ne paie pas pour
l'organisation, et le portefeuille de l'organisation arrive avec la tranche 5.

## Abonnement d'organisation partagé

L'organisation détient l'abonnement (souscrit à son nom : `subscriberType = CUSTOMER`, son code
`CUS-COM-...`). Ses collaborateurs le consomment par une **place**. Le droit est décompté chez
l'organisation ; la réservation garde le collaborateur comme bénéficiaire. Seule l'organisation
est facturée, dépassement compris (la ligne de dépassement nomme le collaborateur).

### Répartition, réglable par droit

| Mode | Fonctionnement |
| --- | --- |
| `POOL` (par défaut) | pot commun : chaque place y puise jusqu'à épuisement du solde de l'organisation |
| `QUOTA` | part nominative fixe par place, rien au-delà |
| `QUOTA_AND_POOL` | sa part d'abord, puis le pot commun (ce que l'organisation a reçu moins les parts distribuées) |

- `defaultQuota` : la part de chaque place (modes `QUOTA` et `QUOTA_AND_POOL`). Une place peut
  avoir sa propre part.
- `seatCap` : plafond facultatif par place sur la période, dans tous les modes. Une place peut avoir
  son propre plafond.
- Les parts distribuées ne peuvent pas dépasser ce que l'organisation a reçu pour la période.
- Parts, plafonds et pot repartent à chaque période de l'abonnement.
- La politique de dépassement du plan s'applique au solde de l'organisation. Une réservation qui
  dépasserait la part, le plafond ou le pot d'une place est refusée, avec la raison.

### Réservation par un collaborateur

En mode abonnement, la réservation cherche d'abord les droits du membre lui-même, puis ceux des
organisations où il a une place active (lien actif, abonnement actif sur sa période). Quand
plusieurs organisations sont possibles :

1. celle passée dans `billingCompanyCode` (`NONE` : aucune) ;
2. sinon celle dont le lien est en mode automatique ;
3. sinon la seule où il a une place ;
4. sinon refus (400) : le membre doit choisir (`billingCompanyCode`).

### Départ d'un collaborateur

La fin de son lien ferme ses places. Ce qui reste de sa part est perdu ; ce qu'il n'a pas pris
dans le pot y reste. Ses réservations à venir sur ces places sont signalées (`warning` de la
réponse), pas annulées.

### Routes

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Administration | `GET /companies/{customerId}/subscriptions` | abonnements ouverts de l'organisation, places occupées |
| Administration | `GET /companies/{customerId}/subscriptions/{n}/sharing` | règles, droits reçus et restants, places et consommation |
| Administration | `PUT /companies/{customerId}/subscriptions/{n}/sharing/{entitlementCode}` | règle d'un droit `{ mode, defaultQuota, seatCap }` |
| Administration | `PATCH /companies/{customerId}/subscriptions/{n}/max-seats` | nombre de places au plus `{ maxSeats }` (vide = sans limite) |
| Administration et gérant | `POST .../subscriptions/{n}/seats` | attribue une place `{ memberId, allowances? }` à un membre rattaché |
| Administration et gérant | `PATCH .../subscriptions/{n}/seats/{seatId}` | change part et plafond `{ allowances: [{ entitlementCode, quota, cap }] }` |
| Administration et gérant | `POST .../subscriptions/{n}/seats/{seatId}/release` | libère la place `{ reason? }` |
| Gérant | `GET /client/companies/{customerId}/subscriptions` (et `.../{n}/sharing`) | comme l'administration |
| Gérant | `GET /client/companies/{customerId}/usage` | consommation par collaborateur, tous abonnements ouverts |
| Collaborateur | `GET /client/me/entitlements` | ses places : sa part, ce qui en reste, son plafond, le pot et le solde de l'organisation |

Côté gérant, les chemins commencent par `/client/companies/{customerId}` ; les règles et le
nombre de places restent à l'administration. `seatId` est le numéro court de la place.

Extrait de `sharing` :

```json
{
  "companyCustomerCode": "CUS-COM-...",
  "subscription": { "subscriptionNumber": "SUB-...", "planName": "Équipe", "maxSeats": 10, "activeSeats": 4 },
  "entitlements": [{
    "entitlementCode": "SALLE_H", "unit": "HOUR", "mode": "QUOTA_AND_POOL",
    "defaultQuota": 5, "seatCap": 12, "granted": 50, "remaining": 31,
    "distributedQuotas": 20, "poolSize": 30, "poolUsed": 7
  }],
  "seats": [{
    "id": 12, "memberId": "MBR-...", "memberName": "Jean Mbemba",
    "entitlements": [{ "entitlementCode": "SALLE_H", "quota": 5, "quotaUsed": 5, "quotaRemaining": 0, "cap": 12, "used": 9 }]
  }]
}
```
