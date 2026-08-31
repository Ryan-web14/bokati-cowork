# Facturation — nouvelles fonctionnalités, intégration frontend

Écrit le 2026-08-29. Couvre les quatre lots livrés : catalogue enrichi, garde-fous de remise,
versionnage des documents, avoir vers portefeuille.

Base des chemins : `/sni/api/v1`.

---

## 1. Simuler avant de créer

Le seul moyen de connaître un total était de créer le document. D'où les calculs à la
calculatrice et les brouillons créés pour être aussitôt supprimés.

```
POST /billing/documents/simulate
```

Le corps reprend celui de `POST /billing/documents`, en plus permissif : **`documentType` et
`currency` sont facultatifs** — simuler n'engage rien, exiger un type de document n'aurait pas de
sens. À défaut, le serveur retient `INVOICE` et la devise par défaut, et les renvoie dans la
réponse avec le `title`, pour que l'aperçu affiche son en-tête sans rien deviner. Seules les
lignes sont validées. Rien n'est écrit, aucun numéro de
séquence n'est consommé. L'interface peut donc appeler `simulate` à chaque frappe, puis envoyer
la même charge utile pour de bon.

### Réponse

```json
{
  "subtotalAmount": "1500.0000",
  "lineDiscountAmount": "100.0000",
  "documentDiscountAmount": "150.0000",
  "discountAmount": "250.0000",
  "taxableAmount": "1250.0000",
  "vatAmount": "225.0000",
  "additionalCentAmount": "11.2500",
  "taxAmount": "236.2500",
  "totalAmount": "1486.2500",
  "blocked": false,
  "violations": [],
  "lines": [
    {
      "itemCode": "SRV-001",
      "description": "Salle de réunion",
      "category": "ESPACES",
      "unit": "heure",
      "quantity": "1.0000",
      "unitPrice": "1000.0000",
      "subtotalAmount": "1000.0000",
      "discountRate": null,
      "discountAmount": "100.0000",
      "effectiveRate": "10.0000",
      "netAmount": "900.0000",
      "taxAmount": "189.0000",
      "totalAmount": "1089.0000",
      "taxable": true,
      "optional": false
    }
  ]
}
```

**Deux champs à ne pas manquer.**

`effectiveRate` est le taux de remise réellement obtenu sur la ligne, **recalculé depuis les
montants même quand la remise a été saisie en valeur**. C'est le chiffre qu'on cherchait à la
calculatrice — affichez-le à côté de la remise.

`lineDiscountAmount` et `documentDiscountAmount` séparent ce que portent les lignes de ce qui est
appliqué au document entier. Le calcul n'exposait que le total.

---

## 2. Les garde-fous de remise

### Ce qui est contrôlé

| Niveau | Origine | Ce qui est vérifié |
|---|---|---|
| Ligne | Article du catalogue | Prix net sous le plancher, taux au-delà du maximum de l'article |
| Ligne | Configuration | Part minimale du montant de base conservée (50 % par défaut), montant net minimal |
| Document | Configuration | Remise totale au-delà du taux ou du plafond admis |

Le repli de configuration s'applique **même aux lignes libres**, sans article. C'est le cas le
plus courant en saisie manuelle, et il échapperait sinon à tout contrôle par ligne.

### Avertir puis bloquer

C'est le point essentiel pour l'interface :

- **`simulate`, `create` et `update` n'échouent jamais** sur un dépassement. Ils le signalent.
- **`issue` refuse.**

Autrement dit, laissez l'utilisateur construire son offre et affichez les dépassements en
direct ; le refus n'arrive qu'au moment d'émettre, et il ne doit alors surprendre personne.

### Le tableau `violations`

```json
{
  "type": "FLOOR_PRICE",
  "severity": "BLOCK",
  "itemCode": "SRV-001",
  "lineDescription": "Salle de réunion",
  "observed": "9000.0000",
  "limit": "10000.0000",
  "message": "« Salle de réunion » descend à 9000 alors que son plancher est 10000"
}
```

| `type` | Sens |
|---|---|
| `FLOOR_PRICE` | Prix net sous le plancher de l'article |
| `MAX_DISCOUNT_RATE` | Taux au-delà du maximum de l'article |
| `LINE_MIN_NET_RATE` | Ligne sous la part minimale du montant de base |
| `LINE_MIN_NET_AMOUNT` | Ligne sous le montant net minimal absolu |
| `DOCUMENT_DISCOUNT_RATE` | Remise totale au-delà du taux admis |
| `DOCUMENT_DISCOUNT_AMOUNT` | Remise totale au-delà du plafond en valeur |

`severity` vaut `WARN` ou `BLOCK`. Seules les `BLOCK` empêchent l'émission — le drapeau `blocked`
de la simulation les résume. `message` est rédigé pour être affiché tel quel.

### Émettre malgré tout

```
PATCH /billing/documents/{documentNumber}/issue?discountOverrideReason=Geste%20commercial
```

Sans motif, ou sans la permission `BILLING:DISCOUNT_OVERRIDE`, l'appel renvoie **400** avec le
détail du dépassement — pas 403, car ce n'est pas l'accès à la route qui est refusé mais
l'opération demandée.

Le motif est obligatoire : un dépassement silencieux n'aurait aucune valeur de contrôle. Il est
conservé sur le document avec son auteur et sa date, et tracé au journal fiscal.

**Parcours conseillé.** Si `blocked` est vrai à la simulation, affichez les violations et
proposez un champ « motif de dérogation » — mais seulement aux comptes qui détiennent la
permission. Sinon, l'utilisateur remplit un champ pour se voir refuser ensuite.

---

## 3. Catalogue enrichi

Les endpoints n'ont pas changé (`/billing/catalog`). Les corps se sont étoffés.

### Champs ajoutés

**Économie** — `costPrice`, `floorPrice`, `minMarginRate`, `maxDiscountRate`,
`discountPolicy` (`NONE` par défaut, `WARN`, `BLOCK`).

**Contenu** — `detailedDescription`, `includedItems` (liste de libellés), `imageUrl`.

**Saisie** — `billingMode` (`UNIT`, `HOURLY`, `DAILY`, `MONTHLY`, `FIXED`), `defaultQuantity`,
`minQuantity`, `maxQuantity`, `taxableByDefault`.

**Classement** — `subcategory`, `tags`, `externalReference`, `validFrom`, `validUntil`.

Tous facultatifs. Laissés vides, l'article se comporte comme avant.

### Désigner un article du catalogue

Le champ est **`itemCode`**. `catalogSourceCode` est accepté comme alias, de même que
`catalogSourceType` pour `sourceType` — les deux orthographes fonctionnent, sur la création
comme sur la modification.

> **Pourquoi cet alias existe.** Une clé mal nommée sur ce champ n'échoue pas : la propriété
> inconnue est ignorée, la ligne n'est rattachée à aucun article, et le plancher de prix, le taux
> de remise maximal, la catégorie et l'unité cessent tous de s'appliquer — sans le moindre
> message. Une remise de 40 % passait ainsi sans déclencher un seul garde-fou.
>
> Si vous écrivez une nouvelle intégration, employez `itemCode` : c'est le nom du champ, l'autre
> n'est qu'un alias de compatibilité.

### Deux champs en lecture seule qui comptent

`effectiveFloorPrice` est le plancher **réellement appliqué** — il peut exister alors que
`floorPrice` est vide, parce qu'il se déduit du coût. **Affichez celui-ci, pas `floorPrice`.**

`floorPriceOrigin` dit d'où il vient : `EXPLICIT`, `DERIVED_FROM_MARGIN`, `COST_PRICE` ou `NONE`.
Il sert à expliquer un refus plutôt qu'à opposer un nombre nu.

La dérivation emploie le **taux de marque** : `coût / (1 − taux)`. À 8 000 de coût et 20 % de
marge minimale, le plancher vaut 10 000 — la marge réalisée à ce prix est bien de 20 % du prix de
vente.

`currentlyValid` indique si l'article est dans sa fenêtre de validité aujourd'hui.

### Effacer un champ

`PUT /billing/catalog/{itemCode}` n'applique que les champs présents. Un champ absent est
conservé — il ne peut donc pas être vidé en l'omettant. Pour retirer un plancher :

```json
{ "clearFields": ["floorPrice"] }
```

Un nom de champ inconnu renvoie 400 plutôt que d'être ignoré silencieusement. Champs effaçables :
`costPrice`, `floorPrice`, `minMarginRate`, `maxDiscountRate`, `detailedDescription`,
`includedItems`, `imageUrl`, `billingMode`, `defaultQuantity`, `minQuantity`, `maxQuantity`,
`taxableByDefault`, `subcategory`, `tags`, `externalReference`, `validFrom`, `validUntil`.

### Refus à connaître

- Plancher supérieur au prix de vente → 400, l'article serait invendable.
- Quantité minimale supérieure à la maximale → 400.
- `discountPolicy` ou `billingMode` hors des valeurs admises → 400 avec la liste attendue.

---

## 4. Versionnage des documents

Un document ne s'écrase plus : chaque modification archive l'état précédent.

```
GET /billing/documents/{documentNumber}/versions
GET /billing/documents/{documentNumber}/versions/{versionNumber}
GET /billing/documents/{documentNumber}/versions/{from}/diff/{to}
```

La liste ne porte pas les instantanés — ils sont volumineux. Le détail les inclut.

```json
{
  "versionNumber": 2,
  "editType": "FULL_EDIT",
  "changedBy": "commercial@sni-cg.com",
  "changedAt": "2026-08-29T09:14:22Z",
  "sentToCustomerAt": "2026-08-29T09:20:00Z",
  "changeSummary": "totalAmount, discountAmount, lignes",
  "snapshot": null
}
```

**`sentToCustomerAt` est la distinction qui compte.** Une version sans horodatage de transmission
est du travail en cours ; une version transmise est ce que le client a réellement eu sous les
yeux. Distinguez-les visuellement.

`changeSummary` liste les champs modifiés depuis la version précédente, en clair.

Le diff renvoie une entrée par champ ayant changé :

```json
{
  "fromVersion": 1,
  "toVersion": 2,
  "changes": [
    { "field": "totalAmount", "before": "10000", "after": "9000" }
  ]
}
```

Les listes de lignes sont comparées dans leur ensemble, sous le champ `lines`, et non ligne à
ligne : un réordonnancement ou une suppression déplace les indices, et une comparaison
positionnelle rapporterait des changements qui n'en sont pas.

**Un document scellé n'est pas versionné** — il est immuable, sa correction passe par un avoir ou
une facture rectificative.

---

## 5. Avoir vers portefeuille

```
POST /billing/credit-notes/{creditNoteNumber}/refund-to-wallet?reason=...
```

À utiliser quand il n'y a plus rien à imputer : le client n'a pas de facture ouverte, mais la
créance existe et doit lui rester acquise pour ses prochaines commandes.

L'opération est **idempotente** : un second appel retrouve l'écriture d'origine au lieu de
recréditer. Un double clic est donc sans conséquence — mais n'en faites pas une raison de ne pas
désactiver le bouton.

Refus possibles, tous en 400 :

| Cause | Message |
|---|---|
| Le document n'est pas un avoir | `n'est pas un avoir` |
| L'avoir n'est pas validé (SEFC) | `doit etre valide` |
| L'avoir a déjà été consommé | `deja ete consomme` |
| Montant nul | `montant nul` |

Après reversement, l'avoir passe au statut `ISSUED`, qui signifie ici « consommé ».

---

## 5 bis. Propriétés inconnues refusées

L'API **refuse désormais toute propriété qu'elle ne connaît pas**, au lieu de l'ignorer
silencieusement, et nomme le champ fautif :

```json
{
  "errorCode": "VALIDATION_FAILED",
  "status": 400,
  "message": "Propriete inconnue dans le corps de la requete : itemCod.",
  "errors": ["itemCod : propriete inconnue · attendu parmi : ..., itemCode, lineOrder, ..."]
}
```

**Pourquoi ce durcissement.** Une clé mal orthographiée ne produisait aucune erreur : le champ
était jeté, et ce qui en dépendait cessait de s'appliquer sans le moindre message. C'est ainsi que
`catalogSourceCode` a désarmé le plancher de prix et le taux de remise maximal.

**Une exception, délibérée** : `POST /billing/documents/simulate` reste tolérant, pour que
l'interface puisse lui envoyer la charge utile complète de la création sans avoir à en retirer les
champs du document.

Si une intégration tierce envoyait des champs superflus, `API_STRICT_PAYLOAD=false` désactive le
durcissement sans redéploiement.

---

## 6. Ce qui change sur l'existant

- **`ServiceCatalogItemResponse`** gagne vingt champs. Aucun n'est retiré.
- **`SimulateBillingDocumentResponse`** gagne `violations` et `blocked`.
- **`issue`** accepte `discountOverrideReason` en paramètre de requête, facultatif, et peut
  désormais répondre 400 sur un dépassement de remise — ce n'était pas le cas auparavant.
- **`update`** archive une version à chaque appel. Sans effet sur la réponse.
- **`send`** marque la dernière version comme transmise. Sans effet sur la réponse.
- **Relevé client** — `statementTotals` se limite désormais aux factures et proformas. Les devis
  n'entrent plus dans le total facturé, les chiffres affichés vont donc baisser sur les clients
  ayant des devis ouverts. C'est une correction, pas une régression.
- **Facture annulée** — son statut passe bien à `CANCELLED` après annulation par avoir, là où il
  restait à `PAID`. Une facture annulée ne compte plus dans le chiffre d'affaires.

---

## 7. Configuration côté serveur

Rien à faire côté interface, mais utile pour comprendre les refus :

```yaml
bokati:
  billing:
    discount-guard:
      enabled: true
      default-min-net-rate: 50          # part minimale conservée par ligne
      min-line-net-amount:              # montant net minimal absolu par ligne
      max-document-discount-rate: 50    # remise totale maximale du document
      max-document-discount-amount:     # plafond de remise en valeur
```

`enabled: false` coupe **les limites de configuration seulement**. Celles portées par les
articles du catalogue relèvent de leur propre `discountPolicy` et restent actives.
