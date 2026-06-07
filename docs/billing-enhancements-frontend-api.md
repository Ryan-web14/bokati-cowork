# Guide Frontend — Améliorations Facturation (Sprint Billing v2)

Base API : `/sni/api/v1`

Ce document couvre les nouvelles fonctionnalités de facturation : édition de document après création, facturation d'une réservation hors-système ou existante, catalogue de services unifié, suivi de récupération d'articles, duplication de document et rapport d'ancienneté des créances.

---

## 1. Édition d'un document après création

### Règles de modification selon le statut

| Statut du document | Autorisé |
|---|---|
| `DRAFT` | Édition complète : en-tête, lignes, remises, clauses |
| `ISSUED`, `SENT` | Édition restreinte : date d'échéance, conditions, instructions de paiement, notes internes |
| `PARTIALLY_PAID`, `PAID`, `CANCELLED`, `VOIDED`, `REFUNDED` | Lecture seule — aucune modification |

### 1.1 Mettre à jour un document

```
PUT /sni/api/v1/billing/documents/{documentNumber}
```

**Corps de la requête — édition complète (DRAFT)**

```json
{
  "title": "Facture prestation coworking — Juin 2026",
  "description": "Mise à jour suite validation client",
  "terms": "Paiement sous 30 jours.",
  "issueDate": "2026-06-01",
  "dueDate": "2026-07-01",
  "customerReference": "REF-CLIENT-2026",
  "poNumber": "PO-0099",
  "projectCode": "PROJ-BETA",
  "salespersonCode": "USR-RYAN",
  "paymentInstructions": "Virement Eco Bank — réf. à rappeler",
  "bankDetailsJson": "{\"bank\":\"Eco Bank\",\"rib\":\"123 456 789\"}",
  "language": "fr",
  "internalNotes": "Client a demandé une révision de la remise le 2026-06-05",
  "lines": [
    {
      "action": "UPSERT",
      "lineOrder": 1,
      "lineType": "SERVICE",
      "itemCode": "BUREAU-001",
      "description": "Bureau privatif — juillet 2026",
      "quantity": 1,
      "unit": "mois",
      "unitPrice": 150000,
      "discountRate": 5,
      "taxable": true
    },
    {
      "action": "REMOVE",
      "lineOrder": 3
    }
  ],
  "discounts": [
    {
      "discountCode": "DISC-FIDELITE",
      "description": "Remise fidélité",
      "discountType": "PERCENTAGE",
      "value": 10
    }
  ],
  "clauses": []
}
```

**Corps de la requête — édition restreinte (ISSUED / SENT)**

```json
{
  "dueDate": "2026-07-15",
  "terms": "Règlement accepté par mobile money.",
  "paymentInstructions": "MTN Mobile Money au +242 06 XXX XXXX",
  "internalNotes": "Relance client effectuée le 2026-06-07"
}
```

Champs ignorés si le document n'est pas en statut `DRAFT` (retournés sans erreur) : `lines`, `discounts`, `clauses`, `title`, `issueDate`, `customerReference`, `poNumber`, `projectCode`.

**Action sur les lignes**

| Valeur `action` | Comportement |
|---|---|
| `UPSERT` | Crée la ligne si `lineOrder` est nouveau, la met à jour sinon |
| `REMOVE` | Supprime la ligne avec ce `lineOrder` |

**Réponse** : `BillingDocumentResponse` standard (cf. `billing-frontend-api.md`), recalculée.

---

### 1.2 Champ `internalNotes`

Disponible sur tous les documents. Visible uniquement dans l'interface admin et manager — jamais affiché sur le PDF client ni dans les emails.

Ce champ est aussi accessible en lecture dans la réponse standard :

```json
{
  "documentNumber": "INV-2026-000042",
  "internalNotes": "Relance client effectuée le 2026-06-07",
  ...
}
```

---

## 2. Facturer une réservation

Endpoint unifié qui gère deux cas : une réservation existante dans le système, ou une réservation externe (hors-système).

```
POST /sni/api/v1/billing/invoices/from-reservation
```

### 2.1 Depuis une réservation existante (`mode: BOOKING`)

Le système récupère la réservation (`bookingNumber`), extrait les données du client, de la ressource, des dates et des montants, puis crée la facture. Le champ `billable_number` de la réservation est mis à jour pour pointer vers ce nouveau document.

```json
{
  "mode": "BOOKING",
  "bookingNumber": "BKG-202606-000123",
  "documentType": "INVOICE",
  "issueDate": "2026-06-07",
  "dueDate": "2026-07-07",
  "terms": "Paiement dû à réception.",
  "paymentInstructions": "Virement Eco Bank",
  "internalNotes": "Facture émise manuellement suite à blocage du module auto-facturation",
  "extraLines": [
    {
      "lineOrder": 2,
      "lineType": "SERVICE",
      "description": "Frais de ménage",
      "quantity": 1,
      "unit": "forfait",
      "unitPrice": 5000,
      "taxable": true
    }
  ]
}
```

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| `mode` | `BOOKING` | oui | Mode réservation système |
| `bookingNumber` | string | oui | Numéro de la réservation dans le système |
| `documentType` | `INVOICE` \| `PROFORMA_INVOICE` | non | Défaut : `INVOICE` |
| `issueDate` | date | non | Défaut : aujourd'hui |
| `dueDate` | date | non | |
| `terms` | string | non | Conditions de paiement |
| `paymentInstructions` | string | non | |
| `internalNotes` | string | non | Note interne admin |
| `extraLines` | array | non | Lignes supplémentaires ajoutées aux lignes de la réservation |

**Données auto-remplies depuis la réservation**

- Client (`customerType`, `customerCode`, nom, email, téléphone)
- Ressource : nom, description, unité de réservation
- Dates de début et fin
- `unitPrice`, `totalAmount` issus de la réservation
- `sourceType = BOOKING`, `sourceCode = bookingNumber`

**Erreurs**

| Code | Cause |
|---|---|
| `BOOKING_NOT_FOUND` | `bookingNumber` inexistant |
| `BOOKING_ALREADY_BILLED` | La réservation a déjà un `billableNumber` |
| `BOOKING_NOT_COMPLETED` | Statut incompatible (ex. `CANCELLED`) |

---

### 2.2 Depuis une réservation externe (`mode: EXTERNAL`)

Aucune réservation dans le système. Le staff saisit manuellement les détails.

```json
{
  "mode": "EXTERNAL",
  "documentType": "INVOICE",
  "customerType": "CUSTOMER",
  "customerCode": "CST-000088",
  "issueDate": "2026-06-07",
  "dueDate": "2026-07-07",
  "externalBooking": {
    "resourceName": "Salle Etoile",
    "resourceDescription": "Salle de réunion 8 places, projecteur inclus",
    "checkInAt": "2026-06-03T09:00:00",
    "checkOutAt": "2026-06-03T17:00:00",
    "bookingUnit": "HOUR",
    "quantity": 8,
    "unitPrice": 5000,
    "externalReference": "REF-EXT-2026-042",
    "notes": "Réservation effectuée par téléphone"
  },
  "extraLines": [],
  "terms": "Paiement sous 15 jours.",
  "internalNotes": "Réservation non saisie dans le système — client walk-in"
}
```

| Champ `externalBooking` | Type | Obligatoire | Description |
|---|---|---|---|
| `resourceName` | string | oui | Nom de la ressource/espace |
| `resourceDescription` | string | non | Description affichée sur la facture |
| `checkInAt` | datetime | oui | Début de la réservation |
| `checkOutAt` | datetime | oui | Fin de la réservation |
| `bookingUnit` | `HOUR` \| `HALF_DAY` \| `DAY` \| `WEEK` \| `MONTH` | oui | Unité de facturation |
| `quantity` | number | oui | Nombre d'unités |
| `unitPrice` | number | oui | Prix unitaire en centimes XAF |
| `externalReference` | string | non | Référence externe (email, note, numéro appel) |
| `notes` | string | non | Notes affichées sur la ligne |

`sourceType = EXTERNAL_BOOKING` sera automatiquement positionné.

**Réponse (modes BOOKING et EXTERNAL)** : `BillingDocumentResponse` standard.

---

## 3. Catalogue de services — Préremplissage des lignes

### 3.1 Recherche unifiée dans le catalogue

Permet de rechercher parmi trois sources pour préremplir une ligne de facture ou de devis.

```
GET /sni/api/v1/billing/catalog/lookup?q=salle&source=SERVICE_CATALOG,INVENTORY_ITEM,RESOURCE&page=0&size=20
```

| Paramètre | Type | Description |
|---|---|---|
| `q` | string | Texte libre (nom, code, description) |
| `source` | enum(s) | Filtrer par source : `SERVICE_CATALOG`, `INVENTORY_ITEM`, `RESOURCE` — virgule pour plusieurs |
| `category` | string | Filtrer par catégorie (SERVICE_CATALOG uniquement) |
| `page`, `size` | int | Pagination |

**Réponse**

```json
{
  "content": [
    {
      "sourceType": "SERVICE_CATALOG",
      "sourceCode": "SVC-000001",
      "name": "Location bureau privatif",
      "description": "Bureau individuel fermé, accès 24h/24",
      "category": "ESPACE",
      "unit": "mois",
      "unitPrice": 150000,
      "currency": "XAF",
      "taxRuleCode": "TVA_CG_18"
    },
    {
      "sourceType": "INVENTORY_ITEM",
      "sourceCode": "INV-CAFE-001",
      "name": "Pack café & boissons",
      "description": "Accès boissons chaudes — forfait journée",
      "category": "CONSOMMABLE",
      "unit": "forfait",
      "unitPrice": 3000,
      "currency": "XAF",
      "taxRuleCode": null
    },
    {
      "sourceType": "RESOURCE",
      "sourceCode": "SALLE-ETOILE",
      "name": "Salle Étoile",
      "description": "Salle de réunion 8 places",
      "category": null,
      "unit": "HOUR",
      "unitPrice": 5000,
      "currency": "XAF",
      "taxRuleCode": "TVA_CG_18",
      "resourceMeta": {
        "bookingUnit": "HOUR",
        "capacity": 8,
        "zone": "Niveau 2"
      }
    }
  ],
  "totalElements": 3,
  "page": 0,
  "size": 20
}
```

### 3.2 Utiliser un article du catalogue pour préremplir une ligne

Dans `CreateBillingDocumentLineRequest` (création ou édition), passer `catalogSourceType` + `catalogSourceCode`. Le backend remplit automatiquement `description`, `unit`, `unitPrice` et `taxable`/`vatRate`. Le frontend peut ensuite laisser ou écraser ces valeurs.

```json
{
  "lineOrder": 1,
  "lineType": "SERVICE",
  "catalogSourceType": "SERVICE_CATALOG",
  "catalogSourceCode": "SVC-000001",
  "quantity": 2,
  "discountRate": 0
}
```

Résultat : la ligne sera créée avec `description = "Bureau privatif"`, `unit = "mois"`, `unitPrice = 150000`, `taxable = true`, `vatRate = 18`.

Le `sourceType` / `sourceCode` de la ligne conserve la traçabilité vers la source catalogue.

---

### 3.3 Gestion du catalogue de services

#### Créer un article

```
POST /sni/api/v1/billing/catalog
```

```json
{
  "name": "Location poste coworking",
  "description": "Accès espace coworking — poste fixe",
  "category": "ESPACE",
  "unit": "jour",
  "unitPrice": 8000,
  "currency": "XAF",
  "taxRuleCode": "TVA_CG_18",
  "displayOrder": 1
}
```

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| `name` | string | oui | Nom de la prestation |
| `description` | string | non | Description affichée sur les documents |
| `category` | string | non | Catégorie libre (ex. `ESPACE`, `CONSOMMABLE`, `SERVICE`) |
| `unit` | string | non | Unité de mesure (ex. `jour`, `heure`, `forfait`) |
| `unitPrice` | number | oui | Prix en centimes XAF |
| `currency` | string | non | Défaut `XAF` |
| `taxRuleCode` | string | non | Code de règle fiscale (ex. `TVA_CG_18`) |
| `displayOrder` | int | non | Ordre d'affichage dans les listes |

**Réponse** : `ServiceCatalogItemResponse`

```json
{
  "itemCode": "SVC-000001",
  "name": "Location poste coworking",
  "description": "Accès espace coworking — poste fixe",
  "category": "ESPACE",
  "unit": "jour",
  "unitPrice": 8000,
  "currency": "XAF",
  "taxRuleCode": "TVA_CG_18",
  "active": true,
  "displayOrder": 1,
  "createdAt": "2026-06-07T10:00:00Z"
}
```

#### Lister les articles

```
GET /sni/api/v1/billing/catalog?category=ESPACE&active=true&q=coworking&page=0&size=20
```

#### Mettre à jour un article

```
PUT /sni/api/v1/billing/catalog/{itemCode}
```

Mêmes champs que la création, tous optionnels.

#### Activer / désactiver

```
PATCH /sni/api/v1/billing/catalog/{itemCode}/activate
PATCH /sni/api/v1/billing/catalog/{itemCode}/deactivate
```

#### Supprimer

```
DELETE /sni/api/v1/billing/catalog/{itemCode}
```

Refusé si l'article est référencé dans des lignes de documents actifs.

---

## 4. Suivi de récupération d'articles (recouvrables)

Permet de tracer les articles remis au client (clé, badge, équipement) et de confirmer leur retour.

### 4.1 Ajouter des articles à récupérer

```
POST /sni/api/v1/billing/documents/{documentNumber}/recoverables
```

```json
{
  "items": [
    {
      "itemDescription": "Badge d'accès RFID",
      "quantity": 1,
      "unit": "pcs",
      "sourceType": "INVENTORY_ITEM",
      "sourceCode": "ASSET-BADGE-042",
      "notes": "Badge numéro 042, remis à la signature du contrat"
    },
    {
      "itemDescription": "Clé bureau 2B",
      "quantity": 1,
      "unit": "pcs",
      "notes": "Clé à récupérer en fin de contrat"
    }
  ]
}
```

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| `itemDescription` | string | oui | Description de l'article |
| `quantity` | number | non | Défaut : 1 |
| `unit` | string | non | Unité (ex. `pcs`, `kit`) |
| `sourceType` | `INVENTORY_ITEM` \| `ASSET` \| `MANUAL` | non | Source dans le système |
| `sourceCode` | string | non | Code de l'article ou de l'asset |
| `notes` | string | non | Note de remise |

**Réponse** : liste de `BillingRecoverableResponse`

```json
[
  {
    "recoverableNumber": "REC-202606-000001",
    "documentNumber": "INV-2026-000042",
    "itemDescription": "Badge d'accès RFID",
    "quantity": 1,
    "unit": "pcs",
    "sourceType": "INVENTORY_ITEM",
    "sourceCode": "ASSET-BADGE-042",
    "status": "PENDING",
    "notes": "Badge numéro 042",
    "recoveredAt": null,
    "recoveredBy": null,
    "createdAt": "2026-06-07T10:30:00Z"
  }
]
```

### 4.2 Lister les articles à récupérer

```
GET /sni/api/v1/billing/documents/{documentNumber}/recoverables
```

Filtres optionnels : `?status=PENDING`

### 4.3 Marquer un article comme récupéré

```
PATCH /sni/api/v1/billing/documents/{documentNumber}/recoverables/{recoverableNumber}/recover
```

```json
{
  "notes": "Badge restitué en main propre le 2026-06-07",
  "partialQuantity": null
}
```

| Champ | Description |
|---|---|
| `notes` | Note de retour |
| `partialQuantity` | Si fourni → statut `PARTIALLY_RECOVERED`. Sinon → `RECOVERED` |

### 4.4 Passer en statut irrécouvrable

```
PATCH /sni/api/v1/billing/documents/{documentNumber}/recoverables/{recoverableNumber}/write-off
```

```json
{
  "notes": "Badge perdu par le client — déduction appliquée sur caution"
}
```

**Statuts possibles**

| Statut | Description |
|---|---|
| `PENDING` | Article remis, retour non confirmé |
| `RECOVERED` | Retour confirmé intégralement |
| `PARTIALLY_RECOVERED` | Retour partiel enregistré |
| `WRITTEN_OFF` | Article non récupéré (perdu, détruit) |

---

## 5. Dupliquer un document

Crée une copie du document en statut `DRAFT` avec un nouveau numéro et la date du jour. Toutes les lignes, remises et clauses sont reprises.

```
POST /sni/api/v1/billing/documents/{documentNumber}/duplicate
```

Aucun corps requis.

**Réponse** : `BillingDocumentResponse` du nouveau document (statut `DRAFT`).

Cas d'usage : facturation récurrente pour un même client — dupliquer le mois précédent, ajuster les dates, émettre.

---

## 6. Rapport d'ancienneté des créances (Aging Report)

```
GET /sni/api/v1/billing/aging-report
```

**Paramètres optionnels**

| Paramètre | Type | Description |
|---|---|---|
| `customerType` | string | Filtrer par type de client (`MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY`) |
| `customerCode` | string | Filtrer sur un client précis |
| `currency` | string | Défaut : `XAF` |

**Réponse**

```json
{
  "generatedAt": "2026-06-07T14:00:00Z",
  "currency": "XAF",
  "summary": {
    "current": { "count": 12, "totalDue": 1850000 },
    "days1To30": { "count": 4, "totalDue": 620000 },
    "days31To60": { "count": 2, "totalDue": 280000 },
    "days61To90": { "count": 1, "totalDue": 95000 },
    "daysOver90": { "count": 1, "totalDue": 150000 },
    "grandTotal": { "count": 20, "totalDue": 2995000 }
  },
  "documents": [
    {
      "documentNumber": "INV-2026-000010",
      "customerType": "MEMBER",
      "customerCode": "MBR-0042",
      "customerName": "Jean-Pierre Moukala",
      "issueDate": "2026-04-01",
      "dueDate": "2026-05-01",
      "totalAmount": 95000,
      "balanceDue": 95000,
      "daysOverdue": 37,
      "agingBucket": "DAYS_31_TO_60",
      "status": "OVERDUE"
    }
  ]
}
```

**Buckets**

| Valeur `agingBucket` | Critère |
|---|---|
| `CURRENT` | Échéance non dépassée ou pas encore émis |
| `DAYS_1_TO_30` | 1 à 30 jours de retard |
| `DAYS_31_TO_60` | 31 à 60 jours |
| `DAYS_61_TO_90` | 61 à 90 jours |
| `DAYS_OVER_90` | Plus de 90 jours |

Seuls les documents avec `balanceDue > 0` et statut dans (`ISSUED`, `SENT`, `VIEWED`, `PARTIALLY_PAID`, `OVERDUE`) sont inclus.

---

## 7. Résumé des nouveaux endpoints

| Méthode | Endpoint | Description |
|---|---|---|
| `PUT` | `/billing/documents/{documentNumber}` | Éditer un document après création |
| `POST` | `/billing/invoices/from-reservation` | Facturer une réservation (existante ou externe) |
| `GET` | `/billing/catalog/lookup` | Recherche unifiée catalogue (services, inventaire, ressources) |
| `GET` | `/billing/catalog` | Lister les articles du catalogue de services |
| `POST` | `/billing/catalog` | Créer un article catalogue |
| `PUT` | `/billing/catalog/{itemCode}` | Mettre à jour un article |
| `PATCH` | `/billing/catalog/{itemCode}/activate` | Activer un article |
| `PATCH` | `/billing/catalog/{itemCode}/deactivate` | Désactiver un article |
| `DELETE` | `/billing/catalog/{itemCode}` | Supprimer un article |
| `POST` | `/billing/documents/{documentNumber}/recoverables` | Ajouter des articles à récupérer |
| `GET` | `/billing/documents/{documentNumber}/recoverables` | Lister les articles à récupérer |
| `PATCH` | `/billing/documents/{documentNumber}/recoverables/{recoverableNumber}/recover` | Confirmer retour |
| `PATCH` | `/billing/documents/{documentNumber}/recoverables/{recoverableNumber}/write-off` | Passer en irrécouvrable |
| `POST` | `/billing/documents/{documentNumber}/duplicate` | Dupliquer un document |
| `GET` | `/billing/aging-report` | Rapport d'ancienneté des créances |

---

## 8. Nouveaux champs dans `BillingDocumentResponse`

```json
{
  "documentNumber": "INV-2026-000042",
  "internalNotes": "Note interne non visible client",
  "recoverables": [
    {
      "recoverableNumber": "REC-202606-000001",
      "itemDescription": "Badge RFID",
      "status": "PENDING"
    }
  ],
  ...
}
```

Le champ `recoverables` est inclus dans la réponse document uniquement si des articles sont enregistrés.