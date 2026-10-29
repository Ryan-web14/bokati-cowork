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
