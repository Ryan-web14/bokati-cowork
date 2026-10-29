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
