3# Guide Frontend — Devis & Factures : Fonctionnalités Sprint 1, 2, 3

Base API : `/sni/api/v1`

Ce document complète les guides `billing-frontend-api.md` et `billing-payment-frontend-api.md`. Il couvre uniquement les nouveautés apportées par les sprints 1, 2 et 3 : unités, acompte, référence de paiement, champs formulaire enrichis, lignes optionnelles, escompte, workflow devis avancé et signature électronique.

---

## 1. Structures mises à jour

### 1.1 Ligne de document — `CreateBillingDocumentLineRequest`

```json
{
  "lineOrder": 1,
  "lineType": "SERVICE",
  "itemCode": "ESPACE-001",
  "description": "Location salle de réunion",
  "detailedDescription": "Salle Etoile — 8 places",
  "quantity": 2,
  "unit": "jour",
  "unitPrice": 25000,
  "discountRate": 10,
  "discountAmount": 0,
  "taxable": true,
  "vatRate": null,
  "additionalCentRate": null,
  "sourceType": null,
  "sourceCode": null,
  "externalReference": "REF-CLIENT-42",
  "notes": "Note interne visible uniquement dans l'admin",
  "optional": false
}
```

Nouveaux champs :

| Champ | Type | Obligatoire | Description |
|---|---|---|---|
| `unit` | string | non | Unité de mesure affichée (ex : `jour`, `heure`, `m²`, `forfait`, `pcs`) |
| `externalReference` | string | non | Référence article côté client |
| `notes` | string | non | Note interne sur la ligne, non affichée sur le PDF client |
| `optional` | boolean | non | `true` → ligne optionnelle : calculée mais exclue du total (voir §3) |

---

### 1.2 Document — `CreateManualBillingDocumentRequest` / `CreateBillingDocumentRequest`

Tous les nouveaux champs sont **optionnels**.

```json
{
  "customerType": "MEMBER",
  "customerCode": "MBR-0042",
  "title": "Devis espaces coworking — Juin 2026",
  "description": "...",
  "terms": "...",
  "currency": "XAF",
  "issueDate": "2026-05-17",
  "dueDate": "2026-06-17",

  "customerReference": "BC-COMMANDE-2026-88",
  "poNumber": "PO-2026-0099",
  "projectCode": "PROJ-ALPHA",
  "salespersonCode": "USR-RYAN",
  "deliveryAddressJson": "{\"street\":\"12 Av. des Cocotiers\",\"city\":\"Brazzaville\"}",
  "language": "fr",
  "exchangeRate": null,

  "paymentReference": "DEV-2026-0042",
  "paymentInstructions": "Virement à l'ordre de Bokati Cowork SAS. Merci d'indiquer la référence lors du règlement.",
  "bankDetailsJson": "{\"bank\":\"Eco Bank\",\"rib\":\"123 456 789\",\"iban\":\"CG29...\"}",

  "lines": [...],
  "discounts": [...],
  "clauses": [...],

  "advance": {
    "advanceType": "PERCENTAGE",
    "advanceValue": 30,
    "includedLineOrders": null,
    "excludedLineOrders": [3],
    "paymentReference": "AV-2026-0042",
    "referenceLabel": "Acompte 30% — Devis DEV-2026-0042",
    "dueDate": "2026-05-25",
    "notes": null
  },

  "earlyPaymentDiscount": {
    "discountRate": 2,
    "ifPaidBefore": "2026-05-27",
    "label": null
  }
}
```

#### Champs formulaire (Sprint 2)

| Champ | Type | Description |
|---|---|---|
| `customerReference` | string | Numéro de commande interne du client (ex : BC-COMMANDE-…) |
| `poNumber` | string | Numéro de bon de commande client |
| `projectCode` | string | Regroupement par projet / affaire |
| `salespersonCode` | string | Code utilisateur du commercial rattaché |
| `deliveryAddressJson` | string JSON | Adresse de livraison si différente de l'adresse de facturation |
| `language` | string | `fr` (défaut) ou `en` — langue du PDF généré |
| `exchangeRate` | number | Taux de change si devise ≠ devise de base (optionnel) |

#### Référence de paiement (Sprint 1)

| Champ | Type | Description |
|---|---|---|
| `paymentReference` | string | Référence à rappeler lors du virement (générée automatiquement si absente) |
| `paymentInstructions` | string | Instructions de paiement libres affichées sur le PDF |
| `bankDetailsJson` | string JSON | Coordonnées bancaires : `bank`, `rib`, `iban` |

#### Acompte (Sprint 1)

Objet `advance` — inclus à la création du document.

| Champ | Type | Requis | Description |
|---|---|---|---|
| `advanceType` | enum | oui | `PERCENTAGE` ou `FIXED_AMOUNT` |
| `advanceValue` | number | oui | Pourcentage (ex : `30`) ou montant brut (ex : `50000`) |
| `includedLineOrders` | number[] | non | Numéros de lignes inclus dans la base de calcul. `null` = toutes |
| `excludedLineOrders` | number[] | non | Numéros de lignes exclus de la base de calcul |
| `paymentReference` | string | non | Référence à mentionner lors du règlement de l'acompte |
| `referenceLabel` | string | non | Libellé affiché dans la section acompte du PDF |
| `dueDate` | date | non | Date limite de règlement de l'acompte |
| `notes` | string | non | Note libre |

Règle de calcul backend :
```
base = somme totalAmount des lignes incluses (ou toutes - exclues)
PERCENTAGE  → computedAmount = base × advanceValue / 100
FIXED_AMOUNT → computedAmount = min(advanceValue, base)
```

#### Escompte pour paiement anticipé (Sprint 2)

Objet `earlyPaymentDiscount` — inclus à la création.

| Champ | Type | Requis | Description |
|---|---|---|---|
| `discountRate` | number | oui | Taux d'escompte en % (ex : `2`) |
| `ifPaidBefore` | date | oui | Date limite pour en bénéficier |
| `label` | string | non | Libellé affiché. Si absent, le backend génère : `"Escompte 2,00% si règlement avant le 27/05/2026"` |

---

### 1.3 Réponse document — `BillingDocumentResponse`

La réponse inclut désormais tous les nouveaux champs.

```json
{
  "documentNumber": "DEV-2026-0042",
  "documentType": "QUOTE",
  "status": "DRAFT",

  "customerReference": "BC-COMMANDE-2026-88",
  "poNumber": "PO-2026-0099",
  "projectCode": "PROJ-ALPHA",
  "salespersonCode": "USR-RYAN",
  "deliveryAddressJson": "{...}",
  "language": "fr",
  "exchangeRate": null,

  "paymentReference": "DEV-2026-0042",
  "paymentInstructions": "Virement à l'ordre de ...",
  "bankDetailsJson": "{...}",

  "subtotalAmount": 50000,
  "discountAmount": 5000,
  "taxableAmount": 45000,
  "vatAmount": 8550,
  "additionalCentAmount": 855,
  "taxAmount": 9405,
  "totalAmount": 54405,
  "paidAmount": 0,
  "balanceDue": 54405,
  "optionsTotal": 12000,

  "lines": [
    {
      "lineOrder": 1,
      "lineType": "SERVICE",
      "description": "Location salle de réunion",
      "quantity": 2,
      "unit": "jour",
      "unitPrice": 25000,
      "discountRate": 10,
      "discountAmount": 5000,
      "taxable": true,
      "vatRate": 19,
      "additionalCentRate": 10,
      "subtotalAmount": 50000,
      "taxableAmount": 45000,
      "vatAmount": 8550,
      "additionalCentAmount": 855,
      "taxAmount": 9405,
      "totalAmount": 54405,
      "externalReference": "REF-CLIENT-42",
      "notes": "Note interne",
      "optional": false
    }
  ],

  "advance": {
    "advanceType": "PERCENTAGE",
    "advanceValue": 30,
    "computedAmount": 16321.50,
    "includedLineOrders": null,
    "excludedLineOrders": [3],
    "paymentReference": "AV-2026-0042",
    "referenceLabel": "Acompte 30% — Devis DEV-2026-0042",
    "dueDate": "2026-05-25",
    "status": "PENDING",
    "paidAt": null,
    "notes": null
  },

  "earlyPaymentDiscount": {
    "discountRate": 2,
    "ifPaidBefore": "2026-05-27",
    "computedAmount": 1088.10,
    "label": "Escompte 2,00% si règlement avant le 27/05/2026"
  },

  "signature": {
    "signerName": "Jean Moukala",
    "signerEmail": "jean.moukala@example.com",
    "status": "PENDING",
    "signedAt": null,
    "expiresAt": "2026-05-24T10:32:00Z"
  }
}
```

Champs clés à noter :

| Champ | Description |
|---|---|
| `optionsTotal` | Somme des lignes `optional = true` encore non sélectionnées. `0` si aucune ligne optionnelle. Afficher à part sur le devis interactif |
| `advance.status` | `PENDING` / `PAID` / `CANCELLED` |
| `signature` | Dernière demande de signature. `null` si aucune demande n'a été faite |
| `signature.status` | `PENDING` / `SIGNED` / `EXPIRED` |

---

## 2. Lignes optionnelles — devis interactif (Sprint 2)

Les lignes `optional: true` sont calculées individuellement mais **exclues du total du devis**. Elles représentent des options que le client peut accepter ou refuser.

### Affichage recommandé

```
Lignes confirmées          Total : 54 405 XAF
─────────────────────────────────────────────
Options disponibles        Sous-total options : 12 000 XAF
  ☐  Formation initiale      10 000 XAF
  ☐  Extension mois +1        2 000 XAF
─────────────────────────────────────────────
Total si toutes options    66 405 XAF  ← calculer côté front
```

### Endpoint — sélectionner des options

```http
PATCH /billing/quotes/{quoteNumber}/select-options
```

```json
{
  "selectedLineOrders": [3, 5]
}
```

- Les lignes optionnelles listées dans `selectedLineOrders` passent en lignes normales (incluses dans le total).
- Les lignes optionnelles **non** listées sont supprimées définitivement.
- Les lignes non-optionnelles ne sont pas affectées.
- Les totaux du document sont recalculés automatiquement.
- Appeler **avant** `accept` ou `convert-to-invoice`.

---

## 3. Workflow devis enrichi (Sprint 3)

### Statuts complets (incluant les nouveaux)

```
DRAFT → ISSUED / SENT
SENT  → VIEWED (tracking)
SENT | VIEWED | ACCEPTED → NEGOTIATION
DRAFT | SENT | VIEWED | NEGOTIATION → ACCEPTED
ACCEPTED → DEPOSIT_REQUESTED → DEPOSIT_PAID
ACCEPTED | DEPOSIT_PAID → CONVERTED (via convert-to-invoice)
Tout statut → REJECTED / CANCELLED
```

Nouveaux statuts :

| Statut | Signification |
|---|---|
| `VIEWED` | Le client a ouvert l'email du devis |
| `NEGOTIATION` | Devis en cours de discussion / renegociation |
| `DEPOSIT_REQUESTED` | Acompte demandé, en attente de règlement |
| `DEPOSIT_PAID` | Acompte reçu, prêt pour conversion en facture |

### Endpoints de transition

```http
PATCH /billing/quotes/{quoteNumber}/mark-viewed
PATCH /billing/quotes/{quoteNumber}/start-negotiation
PATCH /billing/quotes/{quoteNumber}/request-deposit
PATCH /billing/quotes/{quoteNumber}/mark-deposit-paid
```

Tous retournent le `BillingDocumentResponse` mis à jour. Aucun body requis.

### `mark-viewed`

Transition SENT → VIEWED uniquement. Idempotent : si le devis est déjà dans un statut ultérieur, retourne le document sans erreur.

**Cas d'usage** : déclencher depuis un tracking pixel dans l'email envoyé au client, ou manuellement par le commercial.

### `start-negotiation`

Statuts sources : `SENT`, `VIEWED`, `ACCEPTED`.

**Cas d'usage** : le client a demandé des modifications de prix. Le devis repasse en discussion.

### `request-deposit`

Statut source : `ACCEPTED`.

**Cas d'usage** : après acceptation, demander le versement d'un acompte avant de lancer les travaux.

### `mark-deposit-paid`

Statut source : `DEPOSIT_REQUESTED`.

**Cas d'usage** : l'acompte a été reçu (enregistré dans le module payment). Le devis peut être converti en facture.

---

## 4. Signature électronique (Sprint 3)

### Flux complet

```
1. POST /billing/quotes/{quoteNumber}/request-signature
   → génère un token (64 chars), envoie un email au client avec un lien
   → retourne { status: "PENDING", expiresAt: "..." }

2. Le client ouvre le lien : GET /public/quotes/sign/{token}
   → page HTML autonome (pas d'authentification requise)
   → affiche le résumé du devis + un canvas pour dessiner la signature

3. Le client valide : POST /public/quotes/sign/{token}/confirm
   → le backend sauvegarde la signature (image base64, IP, date)
   → le devis passe en statut ACCEPTED
```

### `POST /billing/quotes/{quoteNumber}/request-signature`

Body optionnel :

```json
{
  "signerName": "Jean Moukala",
  "signerEmail": "jean.moukala@client.com",
  "customMessage": "Merci de signer ce devis pour valider votre réservation."
}
```

Si `signerName` / `signerEmail` sont absents, le backend utilise les données du document.

Réponse :

```json
{
  "signerName": "Jean Moukala",
  "signerEmail": "jean.moukala@client.com",
  "status": "PENDING",
  "signedAt": null,
  "expiresAt": "2026-05-24T10:32:00Z"
}
```

Le champ `signature` de `BillingDocumentResponse` reflète cette demande dès qu'on relit le document.

### `GET /public/quotes/sign/{token}` (page HTML)

Pas d'authentification. Le backend retourne une page HTML complète avec :
- Le résumé du devis (numéro, client, total, échéance)
- Un champ texte pour le nom du signataire
- Un canvas pour dessiner la signature à la souris ou au doigt
- Un bouton "Signer et accepter le devis"

Le token expire après 7 jours.

### `POST /public/quotes/sign/{token}/confirm`

Appelé automatiquement par le JavaScript de la page de signature. Si le frontend veut implémenter sa propre page de signature :

```json
{
  "signerName": "Jean Moukala",
  "signerEmail": "jean.moukala@client.com",
  "signatureImageBase64": "data:image/png;base64,iVBOR..."
}
```

`ipAddress` et `userAgent` sont extraits par le backend depuis la requête HTTP.

Réponse : `BillingDocumentResponse` complet avec `status: "ACCEPTED"` et `signature.status: "SIGNED"`.

### États de la signature

| `signature.status` | Signification |
|---|---|
| `PENDING` | Email envoyé, le client n'a pas encore signé |
| `SIGNED` | Devis signé — le document est en `ACCEPTED` |
| `EXPIRED` | Le lien de 7 jours a expiré |

Pour renvoyer un nouveau lien : rappeler `POST /billing/quotes/{n}/request-signature` — un nouveau token est créé.

---

## 5. Récapitulatif des endpoints

### Nouveaux endpoints backend → frontend

| Méthode | Endpoint | Sprint | Description |
|---|---|---|---|
| `PATCH` | `/billing/quotes/{n}/select-options` | 2 | Sélectionner les lignes optionnelles |
| `PATCH` | `/billing/quotes/{n}/mark-viewed` | 3 | Marquer le devis comme vu |
| `PATCH` | `/billing/quotes/{n}/start-negotiation` | 3 | Passer en négociation |
| `PATCH` | `/billing/quotes/{n}/request-deposit` | 3 | Demander un acompte |
| `PATCH` | `/billing/quotes/{n}/mark-deposit-paid` | 3 | Marquer l'acompte comme reçu |
| `POST` | `/billing/quotes/{n}/request-signature` | 3 | Envoyer le lien de signature par email |
| `GET` | `/public/quotes/sign/{token}` | 3 | Page HTML de signature (pas d'auth) |
| `POST` | `/public/quotes/sign/{token}/confirm` | 3 | Valider la signature (pas d'auth) |

### Endpoints inchangés (compatibilité ascendante)

Tous les endpoints existants restent compatibles. Les nouveaux champs des requêtes sont optionnels — les anciens payloads sans les nouveaux champs continuent de fonctionner.

---

## 6. Affichage recommandé dans les formulaires

### Formulaire de création devis / facture (onglets suggérés)

**Onglet 1 — Informations générales**
- Client (type + code ou snapshot)
- Titre, description
- Date d'émission, date d'échéance
- Référence client (`customerReference`), numéro de bon de commande (`poNumber`)
- Commercial (`salespersonCode`)
- Projet (`projectCode`)
- Langue (`language`)

**Onglet 2 — Lignes**
- Tableau de lignes avec : description, `unit`, quantité, prix unitaire, remise ligne, taxable
- `externalReference` et `notes` en champs secondaires (accordéon ou tooltip)
- Checkbox "Ligne optionnelle" → `optional: true`
- Bouton "Ajouter une ligne"

**Onglet 3 — Remises & Taxes**
- Liste des remises globales (`discounts`) : type + valeur
- Escompte pour paiement anticipé (`earlyPaymentDiscount`) : taux + date limite

**Onglet 4 — Acompte**
- Choix : Pourcentage ou Montant fixe
- Valeur de l'acompte
- Lignes incluses / exclues (sélecteur multi-choix sur les numéros de ligne)
- Référence de paiement de l'acompte
- Date d'échéance

**Onglet 5 — Paiement & Coordonnées bancaires**
- Référence de paiement principale (`paymentReference`)
- Instructions de paiement (`paymentInstructions`)
- Coordonnées bancaires (`bankDetailsJson`)
- Adresse de livraison si différente (`deliveryAddressJson`)
- Taux de change (`exchangeRate`) — si multi-devise activé

### Carte de statut du devis (timeline)

```
● Créé (DRAFT)
● Émis (ISSUED / SENT)
○ Vu (VIEWED)           ← nouveau
○ Négociation (NEGOTIATION) ← nouveau
● Accepté (ACCEPTED)
○ Acompte demandé (DEPOSIT_REQUESTED) ← nouveau
○ Acompte reçu (DEPOSIT_PAID)         ← nouveau
● Converti en facture (CONVERTED)
```

### Actions selon le statut du devis

| Statut | Actions disponibles |
|---|---|
| `DRAFT` | Émettre, Modifier, Annuler |
| `SENT` | Marquer vu, Accepter, Rejeter, Négociation, Demander signature |
| `VIEWED` | Accepter, Rejeter, Négociation, Demander signature |
| `NEGOTIATION` | Accepter, Rejeter |
| `ACCEPTED` | Demander acompte, Convertir en facture, Demander signature |
| `DEPOSIT_REQUESTED` | Marquer acompte reçu |
| `DEPOSIT_PAID` | Convertir en facture |
| `CONVERTED` | Voir facture liée |
| `REJECTED` | Recréer un devis |

---

## 7. Exemples complets

### Créer un devis avec toutes les nouvelles fonctionnalités

```http
POST /sni/api/v1/billing/quotes/manual
Content-Type: application/json
Authorization: Bearer {token}
```

```json
{
  "customerType": "MEMBER",
  "customerCode": "MBR-0042",
  "title": "Devis location espaces — Juin 2026",
  "currency": "XAF",
  "issueDate": "2026-05-17",
  "dueDate": "2026-06-17",
  "customerReference": "BC-2026-88",
  "poNumber": "PO-2026-0099",
  "projectCode": "PROJ-BRAZZA",
  "salespersonCode": "USR-RYAN",
  "language": "fr",
  "paymentReference": "DEV-2026-0042",
  "paymentInstructions": "Virement à l'ordre de Bokati Cowork SAS. Référence obligatoire.",
  "bankDetailsJson": "{\"bank\":\"Eco Bank\",\"rib\":\"001 200 123456789 01\"}",
  "lines": [
    {
      "lineOrder": 1,
      "lineType": "SERVICE",
      "description": "Bureau partagé — open space",
      "quantity": 20,
      "unit": "jour",
      "unitPrice": 5000,
      "discountRate": 10,
      "discountAmount": 0,
      "taxable": true
    },
    {
      "lineOrder": 2,
      "lineType": "SERVICE",
      "description": "Salle de réunion Etoile",
      "quantity": 4,
      "unit": "heure",
      "unitPrice": 15000,
      "taxable": true
    },
    {
      "lineOrder": 3,
      "lineType": "SERVICE",
      "description": "Formation espace numérique",
      "quantity": 1,
      "unit": "forfait",
      "unitPrice": 20000,
      "taxable": true,
      "optional": true
    }
  ],
  "advance": {
    "advanceType": "PERCENTAGE",
    "advanceValue": 30,
    "excludedLineOrders": [3],
    "paymentReference": "AV-2026-0042",
    "referenceLabel": "Acompte 30% — Devis DEV-2026-0042",
    "dueDate": "2026-05-25"
  },
  "earlyPaymentDiscount": {
    "discountRate": 2,
    "ifPaidBefore": "2026-05-27"
  },
  "clauses": [
    {
      "clauseCode": "PAYMENT_TERMS",
      "title": "Conditions de paiement",
      "body": "Acompte de 30% à la commande. Solde à la livraison.",
      "displayOrder": 1
    }
  ]
}
```

**Ce que le backend calcule automatiquement :**
- Montants ligne 1 : 20 × 5000 = 100 000, remise 10% = 10 000, net = 90 000
- Montants ligne 2 : 4 × 15 000 = 60 000 (pas de remise)
- Ligne 3 (`optional: true`) : 20 000 — calculée mais **non** incluse dans le total
- `totalAmount` = 150 000 + taxes (lignes 1 + 2 uniquement)
- `optionsTotal` = 20 000 + taxes ligne 3
- `advance.computedAmount` = base (ligne 1 + 2) × 30%
- `earlyPaymentDiscount.computedAmount` = totalAmount × 2%
- `earlyPaymentDiscount.label` = `"Escompte 2,00% si règlement avant le 27/05/2026"`

---

### Sélectionner les options et convertir

```http
PATCH /sni/api/v1/billing/quotes/DEV-2026-0042/select-options

{
  "selectedLineOrders": [3]
}
```

→ La ligne 3 (formation) est intégrée au total. `optionsTotal` revient à `0`.

```http
PATCH /sni/api/v1/billing/quotes/DEV-2026-0042/accept
```

```http
POST /sni/api/v1/billing/quotes/DEV-2026-0042/convert-to-invoice
```

---

### Demander et confirmer une signature

```http
POST /sni/api/v1/billing/quotes/DEV-2026-0042/request-signature

{
  "customMessage": "Merci de signer ce devis pour valider votre réservation de juin."
}
```

→ Email envoyé à l'adresse du membre avec lien vers :
`{BASE_URL}/public/quotes/sign/{token}`

Le client signe sur la page publique → le devis passe automatiquement en `ACCEPTED`.

---

## 8. Points d'attention

- **`advance`** et **`earlyPaymentDiscount`** ne sont passés qu'à la **création**. Pour les modifier, il faut recréer le document (DRAFT uniquement).
- Les lignes `optional: true` n'entrent pas dans le calcul de `advance.computedAmount` si elles font partie d'`excludedLineOrders` (comportement recommandé).
- `optionsTotal` est `0` une fois que `select-options` a été appelé, même si aucune option n'a été sélectionnée.
- La conversion d'un devis en facture (`convert-to-invoice`) copie `paymentReference`, `paymentInstructions` et `bankDetailsJson`. L'acompte (`advance`) **n'est pas** automatiquement reporté sur la facture — le frontend doit l'indiquer dans les clauses ou dans les instructions de paiement.
- Un devis `DEPOSIT_PAID` peut être directement converti en facture sans repasser par `ACCEPTED`.
- Le token de signature expire au bout de **7 jours**. Pour renouveler : rappeler `POST /request-signature` — un nouveau token est créé, l'ancien est invalidé (statut `EXPIRED` à la prochaine tentative d'accès).
- La page publique `/public/quotes/sign/{token}` ne nécessite aucun header d'authentification.
