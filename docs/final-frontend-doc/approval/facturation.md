# Demandes d'approbation de la facturation : `subjectRef` et `detailsJson`

> Rédigé d'après le code (`InvoiceChangeService`, `DiscountApprovalService`), 19/11/2026. Règles communes du moteur : [README](README.md).

`GET /sni/api/v1/approvals/requests/{number}` (et les listes du centre `BILLING`) rend une `RequestView`. Pour la facturation, trois champs portent l'objet de la demande :

| Champ | Valeur pour la facturation |
| --- | --- |
| `subjectType` | Toujours `BILLING_DOCUMENT` |
| `subjectRef` | Numéro de la pièce concernée (`documentNumber`) : facture (`INV-...`) pour une modification ; facture ou devis (`QUO-...`) pour une dérogation de remise |
| `detailsJson` | **Texte** contenant un objet JSON (à passer à `JSON.parse`), ou `null`. Sa forme dépend de `requestType`, ci-dessous |

## Bouton « Ouvrir le document »

- L'afficher quand `subjectType === "BILLING_DOCUMENT"` et `subjectRef` est renseigné, quel que soit `requestType`.
- Il mène à la fiche de la pièce du module facturation : `GET /sni/api/v1/billing/documents/{subjectRef}` ; le PDF : `GET /sni/api/v1/billing/documents/{subjectRef}/pdf`.
- Ne pas chercher le numéro dans `title` ni dans `detailsJson` : `subjectRef` est la référence. `detailsJson` sert à afficher le contenu de la demande sans autre appel.

## `requestType: INVOICE_CHANGE` (modification d'une facture émise)

```json
{
  "invoiceNumber": "INV-CUS-20261118-00000039",
  "documentType": "INVOICE",
  "customer": "Société Kintele SARL",
  "currency": "XAF",
  "totalBefore": 118000,
  "totalAfter": 94400,
  "difference": -23600,
  "reason": "Remise accordée",
  "sealed": true,
  "changeRequestId": "42",
  "requestedBy": "agent@exemple.cg",
  "requestedByName": "Awa Mabiala",
  "discountOverrideReason": null,
  "changes": { "lines": [ ... ], "discountRate": 20, "notes": "..." }
}
```

| Clé | Sens |
| --- | --- |
| `invoiceNumber` | Facture à modifier (égale à `subjectRef`) |
| `documentType` | `INVOICE`, `QUOTE`, `PROFORMA_INVOICE`, `DEBIT_NOTE`, `CORRECTIVE_INVOICE`... |
| `customer`, `currency` | Client et devise de la facture |
| `totalBefore`, `totalAfter`, `difference` | Total TTC actuel, total TTC après modification, écart (négatif : baisse). `totalAfter` est aussi le `amount` de la demande |
| `reason` | Motif de la modification |
| `sealed` | Facture certifiée (scellée) : l'accord produira une **facture rectificative** au lieu d'une nouvelle version |
| `changeRequestId` | Identifiant de la demande de modification côté facturation (texte), pour `PATCH /billing/invoices/change-requests/{id}/cancel` |
| `requestedBy`, `requestedByName` | Demandeur |
| `discountOverrideReason` | Motif de dérogation si la nouvelle version passe sous un plancher de prix, sinon `null` |
| `changes` | Les modifications demandées, même forme que le corps de la mise à jour d'une pièce (`UpdateBillingDocumentRequest`, voir [billing](../billing/README.md)) : afficher les lignes et remises proposées |

Après la décision : `GET /billing/invoices/{invoiceNumber}/change-requests` rend la demande avec `status` (`APPLIED`, `REJECTED`, `CANCELLED`), `result` (`NEW_VERSION` ou `CORRECTIVE_INVOICE`) et `resultDocumentNumber` (la facture modifiée, ou la rectificative qui la remplace : à ouvrir après l'accord).

## `requestType: DISCOUNT_OVERRIDE` (remise ou prix hors limites)

```json
{
  "documentNumber": "QUO-CUS-20261118-00000012",
  "documentType": "QUOTE",
  "customer": "Société Kintele SARL",
  "currency": "XAF",
  "totalAmount": 450000,
  "reason": "Client stratégique",
  "violations": [
    { "type": "MAX_DISCOUNT_RATE", "severity": "BLOCK", "itemCode": "BUREAU-PRIVE", "lineDescription": "Bureau privatif, novembre",
      "observed": 35, "limit": 20, "message": "Remise de 35 % sur Bureau privatif au-delà du plafond de 20 %" }
  ]
}
```

| Clé | Sens |
| --- | --- |
| `documentNumber` | Pièce en cause (égale à `subjectRef`) |
| `documentType`, `customer`, `currency`, `totalAmount` | Pièce, client, devise, total TTC de l'offre soumise (aussi le `amount` de la demande) |
| `reason` | Motif donné par le demandeur |
| `violations[]` | Chaque limite dépassée. `type` ci-dessous ; `severity` (`BLOCK` : bloque l'émission) ; `itemCode` et `lineDescription` : la ligne (nuls pour une remise sur toute la pièce) ; `observed` et `limit` : valeur constatée et limite, **en % pour un taux, en devise pour un montant ou un prix** ; `message` : phrase prête à afficher |

| `type` | Limite dépassée | Unité de `observed` / `limit` |
| --- | --- | --- |
| `MAX_DISCOUNT_RATE` | Remise d'une ligne au-delà du plafond de l'article | % |
| `FLOOR_PRICE` | Prix unitaire net sous le prix plancher de l'article | devise |
| `LINE_MIN_NET_RATE` | Ligne nette sous le pourcentage minimal du prix catalogue | % |
| `LINE_MIN_NET_AMOUNT` | Ligne nette sous le montant minimal | devise |
| `DOCUMENT_DISCOUNT_RATE` | Remise globale de la pièce au-delà du taux permis | % |
| `DOCUMENT_DISCOUNT_AMOUNT` | Remise globale au-delà du montant permis | devise |

Après l'accord, le demandeur émet la pièce normalement ; une offre modifiée relance une demande (voir [README](README.md#dérogation-de-remise-lot-c3-de-la-facturation)).

## Demandes plus anciennes

Les clés `documentType`, `customer`, `currency`, `difference`, `changeRequestId`, `requestedBy`, `requestedByName`, `discountOverrideReason`, `changes` (modification) et `currency`, `totalAmount`, `severity`, `lineDescription`, `observed`, `limit` (dérogation) sont écrites depuis le 19/11/2026. Une demande antérieure ne les a pas : traiter chaque clé comme facultative.
