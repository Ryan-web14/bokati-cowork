# billing : saisie, simulation et montant ventilé (lots B13, B8, B12)

> Retour au [README du module](README.md). Toutes les routes sont sous `/sni/api/v1/billing`. Rien n'est créé par ces routes.

## Simulation à chaque saisie (B13)

`POST /documents/simulate-line` : à chaque frappe sur une ligne (quantité, prix, remise, taxe).

```json
{
  "lineIndex": 1,
  "line": { "description": "Salle", "quantity": 3, "unitPrice": 40000, "itemCode": "SAL-2" },
  "documentType": "QUOTE", "currency": "XAF",
  "lines": [ { "description": "Bureau", "quantity": 1, "unitPrice": 150000 }, { "description": "Salle", "quantity": 2, "unitPrice": 40000 } ],
  "discounts": [],
  "customerType": "CUSTOMER", "customerCode": "CUS-..."
}
```

- `lineIndex` : rang de la ligne saisie dans `lines` (0 pour la première) ; vide ou hors des lignes : ajoutée à la fin.
- Réponse `SimulateBillingLineResponse` : `line` (HT, remise, taux effectif, net, taxes, TTC de la ligne), `alerts` de la ligne, `document` (totaux et toutes les alertes).

`POST /documents/simulate` : tout le document ; accepte aussi `customerType`, `customerCode`, `companyCustomerCode`.

**Même calcul que la création** : l'exonération de TVA de l'organisation s'applique (alerte `VAT_EXEMPT`) ; le blocage (`ORGANIZATION_BLOCKED`) et le plafond d'encours (`CREDIT_LIMIT`) sont annoncés sans refuser.

`alerts[]` : `lineIndex` (vide pour le document), `code`, `severity`, `message`. Codes : `FLOOR_PRICE`, `MAX_DISCOUNT_RATE`, `LINE_MIN_NET_RATE`, `LINE_MIN_NET_AMOUNT`, `DOCUMENT_DISCOUNT_RATE`, `DOCUMENT_DISCOUNT_AMOUNT` (limites de prix et de remise), `STOCK`, `VAT_EXEMPT`, `ORGANIZATION_BLOCKED`, `CREDIT_LIMIT`, `INACTIVE_ITEM`, `RESOURCE_UNAVAILABLE`. `severity` : `INFO`, `WARN`, `BLOCK` (l'émission sera refusée sans dérogation). `blocked` : au moins une alerte `BLOCK`.

## Descriptions longues (B13)

Description, description détaillée et notes d'une ligne, description et conditions du document : 4 000 caractères au plus. Les retours à la ligne sont gardés à l'impression ; une description de 2 000 caractères s'imprime entière.

## Saisir un total, le système ventile (B8)

`POST /documents/allocate-amount` :

```json
{ "lines": [ ... ], "targetAmount": 500000, "taxIncluded": true, "lineIndexes": [0, 1, 2], "customerType": "CUSTOMER", "customerCode": "CUS-..." }
```

- `taxIncluded` : vrai par défaut (total TTC), faux pour un total HT.
- `lineIndexes` : lignes qui se partagent le total ; vide : toutes les lignes non optionnelles. Les autres gardent leur prix.
- Répartition au prorata des prix de référence (à défaut des quantités), au franc près, le reste sur la dernière ligne.
- Le prix de référence est gardé et l'écart devient une **remise par ligne** (remise implicite), contrôlée par le plancher et la remise maximale ; au-dessus de la référence, le prix unitaire monte.

Réponse `AllocateAmountResponse` : `lines` (à reprendre telles quelles dans la création), `referenceAmount`, `implicitDiscount`, `simulation` (calcul complet et alertes). Exemple : 500 000 TTC sur trois lignes à 18 % de TVA donnent des lignes dont la somme fait exactement 500 000.

## Auteur et visa (B12)

Le serveur reprend l'utilisateur connecté, sans saisie : bloc `author` de chaque document (`createdBy`, `createdByName`, `createdByTitle` pour la création ; `versionAuthor`, `versionAuthorName`, `versionAuthorTitle` pour la version courante). Le PDF imprime « Établi par : nom, fonction » et le visa (nom, fonction, date, signature enregistrée). Fonction et signature se gèrent dans la fiche collaborateur ([securite](../securite/fiche-collaborateur.md)).
