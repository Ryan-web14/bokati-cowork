# Bokati Cowork — Changelog Avril 2026

> Document de référence pour les changements d'API, nouvelles fonctionnalités et corrections de bugs  
> implémentés sur la branche `feature/payment-facturation`.  
> Base URL : `https://{host}/sni/api/v1`

---

## Table des matières

1. [Module KYC & Documents](#1-module-kyc--documents)
2. [Module Inventaire](#2-module-inventaire)
3. [Module Réservation & Abonnement](#3-module-réservation--abonnement)

---

## 1. Module KYC & Documents

### 1.1 Validation de la taille du fichier à l'upload

**Fichier modifié :** `DocumentServiceImpl.java`  
**Avant :** `maxFileSizeBytes` était stocké dans `DocumentType` mais jamais vérifié.  
**Après :** Validation stricte avant l'inspection de sécurité.

#### Comportement

Si le fichier dépasse `DocumentType.maxFileSizeBytes`, l'upload est rejeté avec HTTP 400 :

```json
{
  "message": "File size (2048 KB) exceeds the maximum allowed size of 1024 KB for this document type"
}
```

#### Endpoints concernés

| Méthode | Endpoint | Changement |
|---------|----------|------------|
| `POST` | `/documents/upload` | Validation taille avant inspection |
| `POST` | `/documents/{code}/replace` | Idem |

---

### 1.2 Vue d'ensemble des requirements dans la réponse KYC

**Fichier modifié :** `KycCaseResponse.java`, `KycServiceImpl.java`  
**Nouveau DTO :** `KycRequirementStatus`

Chaque réponse `GET /kyc/cases/{code}` inclut désormais un champ `requirements` listant **tous** les documents attendus pour ce type de propriétaire, avec leur statut de soumission.

#### Nouveau champ dans `KycCaseResponse`

```json
{
  "code": "KYCCASE-2026-000001",
  "status": "IN_PROGRESS",
  "complete": false,
  "approved": false,
  "missingDocumentTypeCodes": ["MEMBER_PASSPORT"],
  "requirements": [
    {
      "documentTypeCode": "MEMBER_ID_CARD",
      "documentTypeName": "Carte d'identité nationale",
      "required": true,
      "status": "VERIFIED",
      "documentCode": "DOC-202604-000012"
    },
    {
      "documentTypeCode": "MEMBER_PASSPORT",
      "documentTypeName": "Passeport",
      "required": true,
      "status": null,
      "documentCode": null
    }
  ],
  "documents": [ ... ]
}
```

#### Objet `KycRequirementStatus`

| Champ | Type | Description |
|-------|------|-------------|
| `documentTypeCode` | `String` | Code du type de document |
| `documentTypeName` | `String` | Libellé lisible |
| `required` | `boolean` | Obligatoire ou optionnel |
| `status` | `KycDocumentVerificationStatus` \| `null` | `PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED` ou `null` si pas encore soumis |
| `documentCode` | `String` \| `null` | Code du document soumis, ou `null` si aucun |

---

### 1.3 Endpoint — Documents manquants d'un dossier KYC

**Nouveau endpoint :** `GET /kyc/cases/{code}/missing-requirements`

Retourne uniquement les requirements **obligatoires** qui n'ont pas encore été soumis ou validés.

#### Requête

```
GET /sni/api/v1/kyc/cases/KYCCASE-2026-000001/missing-requirements
```

#### Réponse `200 OK`

```json
[
  {
    "documentTypeCode": "MEMBER_PASSPORT",
    "documentTypeName": "Passeport",
    "required": true,
    "status": null,
    "documentCode": null
  }
]
```

Si le dossier est complet, retourne `[]`.

---

### 1.4 Soumission KYC — Message d'erreur enrichi

**Fichier modifié :** `KycServiceImpl.submit()`  
**Endpoint :** `POST /kyc/cases/{code}/submit`

**Avant :**
```json
{ "message": "KYC case is incomplete" }
```

**Après :**
```json
{ "message": "KYC case is incomplete. Missing required documents: MEMBER_PASSPORT, MEMBER_ID_CARD" }
```

Le message liste explicitement les codes de documents manquants.

---

### 1.5 Optimisation Preview & Download

**Fichiers modifiés :** `DocumentService.java`, `DocumentServiceImpl.java`, `DocumentController.java`

Les deux endpoints faisaient deux appels DB séparés (`getByCode` + `downloadFile`). Ils utilisent maintenant une seule méthode `getFileWithMeta()`.

#### Comportement inchangé

| Endpoint | `Content-Disposition` | `Content-Type` |
|----------|-----------------------|----------------|
| `GET /documents/{code}/download` | `attachment; filename=...` | MIME détecté à l'upload |
| `GET /documents/{code}/preview` | `inline; filename=...` | MIME détecté à l'upload |

> Le `Content-Type` est maintenant toujours basé sur le MIME **détecté** (magic bytes) et non déclaré par le client. Si le MIME n'est pas stocké (vieux documents), fallback sur `application/octet-stream`.

---

## 2. Module Inventaire

### 2.1 Conversion automatique Demande d'achat → Bon d'achat

**Endpoint modifié :** `PATCH /inventory/procurement/purchase-requests/{requestCode}/approve`

Nouveau paramètre optionnel `supplierCode`. Si fourni, le bon d'achat est créé automatiquement à l'approbation.

#### Requête

```
PATCH /sni/api/v1/inventory/procurement/purchase-requests/DA-2026-000001/approve
     ?approvedBy=USR-001
     &supplierCode=SUP-2026-000003
```

#### Réponse `200 OK` — avec auto-création

```json
{
  "requestCode": "DA-2026-000001",
  "status": "CONVERTED",
  "approvedBy": "USR-001",
  "autoCreatedOrderCode": "PO-2026-000005",
  "lines": [ ... ]
}
```

#### Réponse `200 OK` — sans fournisseur (comportement antérieur)

```json
{
  "requestCode": "DA-2026-000001",
  "status": "APPROVED",
  "approvedBy": "USR-001",
  "autoCreatedOrderCode": null,
  "lines": [ ... ]
}
```

#### Champ ajouté dans `PurchaseRequestResponse`

| Champ | Type | Description |
|-------|------|-------------|
| `autoCreatedOrderCode` | `String` \| `null` | Code du bon créé automatiquement, ou `null` |

---

### 2.2 Workers d'alertes — Fréquence augmentée

**Fichier modifié :** `InventoryDailyWorker.java`

L'ancien worker unique (2h du matin, 1×/jour) est remplacé par **4 workers distincts** configurables via `application.yml`.

| Worker | Cron par défaut | Rôle |
|--------|-----------------|------|
| `runReservationExpiryChecks` | `0 */30 * * * *` | Expire les réservations périmées toutes les **30 min** |
| `runExpirySoonChecks` | `0 0 */2 * * *` | Détecte lots expirant < 30j et < 7j (EXPIRY_IMMINENT) toutes les **2h** |
| `runSlowMovingChecks` | `0 0 */6 * * *` | Détecte articles sans sortie depuis 30j toutes les **6h** |
| `runAssetAlertChecks` | `0 0 */3 * * *` | Garanties / maintenances / retards d'asset toutes les **3h** |

#### Configuration `application.yml`

```yaml
inventory:
  worker:
    reservation-cron: "0 */30 * * * *"
    expiry-cron: "0 0 */2 * * *"
    slow-moving-cron: "0 0 */6 * * *"
    asset-cron: "0 0 */3 * * *"
```

---

### 2.3 Suggestions de réapprovisionnement — Champs enrichis

**DTO modifié :** `ReorderSuggestionResponse`  
**Endpoint :** `GET /inventory/intelligence/reorder-rules/suggestions`

Deux nouveaux champs calculés à partir des mouvements de stock des 30 derniers jours :

| Champ | Type | Description |
|-------|------|-------------|
| `consumptionRateLast30Days` | `BigDecimal` \| `null` | Taux journalier moyen de consommation (sorties / 30j) |
| `daysOfStockRemaining` | `Integer` \| `null` | Jours de stock restants au rythme actuel. `null` si aucune consommation |

#### Exemple de réponse enrichie

```json
{
  "itemCode": "ITEM-PAP-001",
  "itemName": "Ramette papier A4",
  "currentQuantity": 5,
  "minQuantity": 10,
  "reorderQuantity": 25,
  "severity": "CRITICAL",
  "consumptionRateLast30Days": 2.5,
  "daysOfStockRemaining": 2,
  "reason": "Stock disponible sous le seuil minimum 10. Objectif proposé: 30."
}
```

#### Ajustement automatique de sévérité par vélocité

| `daysOfStockRemaining` | Sévérité minimale forcée |
|------------------------|--------------------------|
| ≤ 2 jours | `CRITICAL` |
| ≤ 7 jours | `HIGH` |
| > 7 jours | Calculée par ratio stock/minimum |

---

### 2.4 Nouveaux types d'alertes

**Fichier modifié :** `InventoryAlertType.java`

#### Récapitulatif de tous les types (anciens + nouveaux)

| Type | Déclenché par | Quand |
|------|---------------|-------|
| `LOW_STOCK` | Mouvement de stock | `quantityAvailable <= minQuantity` (1re/2e occurrence) |
| **`RECURRING_LOW_STOCK`** *(nouveau)* | Mouvement de stock | `quantityAvailable <= minQuantity` **3e occurrence ou plus** |
| `OUT_OF_STOCK` | Mouvement de stock | `quantityAvailable <= 0` |
| `NEGATIVE_STOCK` | Mouvement de stock | `quantityAvailable < 0` |
| **`OVERSTOCK`** *(nouveau)* | Mouvement de stock | `quantityAvailable > maxQuantity` de la règle |
| **`SLOW_MOVING`** *(nouveau)* | Worker 6h | Aucune sortie depuis 30j avec stock > 0 |
| `EXPIRY_SOON` | Worker 2h | Lot expire dans ≤ 30j |
| **`EXPIRY_IMMINENT`** *(nouveau)* | Worker 2h | Lot expire dans ≤ 7j (priorité maximale) |
| `WARRANTY_SOON` | Worker 3h | Garantie asset expire dans ≤ 30j |
| `MAINTENANCE_DUE` | Worker 3h | Maintenance planifiée dans < 24h |
| `ASSET_RETURN_OVERDUE` | Worker 3h | Retour asset en retard |
| `SUSPICIOUS_ADJUSTMENT` | (disponible) | Ajustement de stock anormalement élevé |

#### Détail `RECURRING_LOW_STOCK`

Message d'alerte :
```
Stock bas récurrent (4e occurrence) pour ITEM-PAP-001 : 3 <= seuil 10
```

- Se déclenche à partir de la **3e occurrence** de stock bas pour le même article/emplacement
- Résolu automatiquement lorsque le stock remonte au-dessus du seuil (comme `LOW_STOCK`)
- Compté dans `lowStockAlerts` du dashboard

#### Détail `OVERSTOCK`

- Résolu automatiquement dès que `quantityAvailable <= maxQuantity`
- Nécessite qu'une règle `InventoryReorderRule` avec `maxQuantity` défini existe pour l'article

---

### 2.5 Rapport de mouvements — Reformatage

**Fichiers modifiés :** `InventoryMovementReportResponse.java`, `InventoryAdminServiceImpl.java`

#### Nouveau champ `reportTitle`

```json
{
  "reportTitle": "Rapport de mouvements de stock — Article ITEM-PAP-001 | Du 01/04/2026 au 28/04/2026",
  "generatedAt": "28/04/2026 14:35",
  "fromDate": "01/04/2026 00:00",
  "toDate": "28/04/2026 23:59",
  ...
}
```

#### Dates formatées

**Avant :** `"2026-04-28T14:35:00Z"` (ISO-8601)  
**Après :** `"28/04/2026 14:35"` (dd/MM/yyyy HH:mm, UTC)

Champs concernés : `fromDate`, `toDate`, `generatedAt`, `firstMovementAt`, `lastMovementAt`, `performedAt` dans les détails.

#### Export CSV — Nouvelle structure

Le CSV (`GET /inventory/admin/reports/movements.csv`) est maintenant structuré en **5 sections** :

```
Rapport de mouvements de stock — Article ITEM-PAP-001
Généré le:,28/04/2026 14:35

RÉSUMÉ
Total mouvements,Entrées (qte),Sorties (qte),Net (qte),Entrées (valeur),...

PAR TYPE DE MOUVEMENT
Type,Nb mouvements,Quantité totale,Valeur totale,...
IN,12,150,75000,...
OUT,8,80,40000,...

PAR ARTICLE (TOP 20)
Code article,Nom,Nb mouvements,Entrées,Sorties,Net,Valeur totale
ITEM-PAP-001,Ramette papier A4,20,150,80,70,35000

PAR EMPLACEMENT (TOP 20)
Code emplacement,Nom,Nb mouvements,...

DÉTAIL DES MOUVEMENTS RÉCENTS
Code,Type,Article,Nom article,De,Vers,Quantité,...
```

---

## 3. Module Réservation & Abonnement

### 3.1 Fix — Vérification de disponibilité avec mode de paiement SUBSCRIPTION/PASS

**Fichier modifié :** `BookingAvailabilityServiceImpl.java`  
**Endpoint :** `POST /bookings/availability` (ou `GET`)

#### Problème corrigé

**Avant :** Une seule vérification globale mélangeait les erreurs de créneau et les erreurs de paiement → `available: false` dans les deux cas, comportement identique.

**Après :** 4 étapes séparées avec comportements distincts.

#### Nouvelle logique de vérification

```
Étape 1 — Créneau physique
├── validateBookable(resource, start, end, qty)
└── validateNoSingleCapacityConflict(resource, start, end)
    → Si échec : available: false, message = erreur créneau

Étape 2 — Fenêtres disponibles
├── findRemainingWindows(resource, start, end, duration, qty)
└── slotFits = une fenêtre CONTIENT le créneau demandé (pas égalité stricte)
    → Si aucune fenêtre : available: false, "Créneau non disponible"

Étape 3 — Contexte de paiement (si non DIRECT)
├── identityResolver.resolve(identityLookup)
└── paymentContextResolver.resolve(paymentMode, identity, resource)
    → Si échec : available: false, message = erreur paiement spécifique

Étape 4 — Calcul du prix
└── pricingCalculator.calculate(resource, start, end, qty)
    → Si succès : available: true
```

#### Fix comparaison fenêtre (bug critique)

**Avant** (égalité stricte) :
```java
// Ne matchait JAMAIS si la fenêtre dispo est 08:00-18:00 et qu'on demande 13:00-14:00
window.getStartedAt().equals(request.startedAt()) && window.getEndedAt().equals(request.endedAt())
```

**Après** (contenu dans la fenêtre) :
```java
!window.getStartedAt().isAfter(request.startedAt()) && !window.getEndedAt().isBefore(request.endedAt())
```

#### Réponse — Erreur de paiement (créneau libre)

```json
{
  "resourceCode": "RES-BUR-202604-00000002",
  "resourceName": "Bureau privé Lembissi",
  "startedAt": "2026-04-29T13:00:00",
  "endedAt": "2026-04-29T14:00:00",
  "available": false,
  "message": "No active subscription found for booking owner"
}
```

> Le frontend peut distinguer une erreur de paiement d'un conflit de créneau en lisant `message`. Un conflit de créneau aura un message différent (ex : "Resource not available for the requested time period").

#### Réponse — Créneau disponible avec abonnement

```json
{
  "resourceCode": "RES-BUR-202604-00000002",
  "resourceName": "Bureau privé Lembissi",
  "startedAt": "2026-04-29T13:00:00",
  "endedAt": "2026-04-29T14:00:00",
  "durationMinutes": 60,
  "quantity": 1,
  "available": true,
  "remainingCapacity": 3,
  "bookingUnit": "HOUR",
  "unitPrice": 5000,
  "estimatedAmount": 5000,
  "currency": "XAF",
  "message": "Disponible — couvert par abonnement"
}
```

---

### 3.2 Abonnements — Activation et entitlement grants automatiques

#### Problème corrigé

Les abonnements payants étaient créés en `PENDING_ACTIVATION` sans grants d'entitlement, rendant toute réservation avec mode `SUBSCRIPTION` impossible.

#### Flux d'activation (inchangé, documenté pour clarté)

```
POST /subscriptions          → status: PENDING_ACTIVATION (payant)
                              → status: ACTIVE + grants créés (gratuit ou autoActivate: true)

PATCH /subscriptions/{n}/activate  → status: ACTIVE + grants créés automatiquement
```

> **Note :** Le `SubscriptionIntegrityWorker` tourne toutes les **30 minutes** et répare automatiquement les abonnements `ACTIVE` sans grants.

#### Nouveau champ `autoActivate` dans la création

**Endpoint :** `POST /subscriptions`

```json
{
  "planCode": "PLAN-FLEX-01",
  "subscriberType": "MEMBER",
  "subscriberCode": "MBR-000002",
  "startDate": "2026-04-29",
  "billingCycle": "MONTHLY",
  "autoRenew": true,
  "autoActivate": true
}
```

| Champ | Type | Défaut | Description |
|-------|------|--------|-------------|
| `autoActivate` | `Boolean` | `null` | Si `true`, active l'abonnement **immédiatement** après création et génère les entitlement grants. Utile pour les flux admin où le paiement est géré séparément. |

#### Règles d'auto-activation à la création

| Condition | Comportement |
|-----------|--------------|
| `totalAmount = 0` (plan gratuit) | Auto-activé automatiquement |
| `autoActivate: true` dans la requête | Auto-activé immédiatement |
| Aucune des deux | `PENDING_ACTIVATION`, activation manuelle requise |

#### Entitlement grants — Ce qui est créé à l'activation

Pour chaque `PlanEntitlement` associé à la version du plan :

```json
{
  "grantNumber": "EG-2026-000012",
  "entitlementDefinitionCode": "HOURS_PRIVATE_OFFICE",
  "quantityGranted": 40,
  "quantityRemaining": 40,
  "unlimited": false,
  "validFrom": "2026-04-29T00:00:00Z",
  "validUntil": "2026-05-28T23:59:59Z",
  "status": "ACTIVE",
  "sourceType": "SUBSCRIPTION",
  "sourceId": "SUB-MBR-2026-000001"
}
```

#### Activer un abonnement existant

```
PATCH /sni/api/v1/subscriptions/SUB-MBR-2026-000001/activate
```

Body (optionnel) :
```json
{
  "reason": "Paiement confirmé",
  "changedBy": "ADMIN-001"
}
```

Réponse : `SubscriptionResponse` avec `status: "ACTIVE"` et grants créés.

---

## Récapitulatif des nouveaux endpoints

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/kyc/cases/{code}/missing-requirements` | Documents obligatoires manquants d'un dossier KYC |
| `PATCH` | `/inventory/procurement/purchase-requests/{code}/approve?supplierCode=X` | Approbation + création automatique du bon de commande |

## Récapitulatif des champs ajoutés

| DTO | Champ ajouté | Type |
|-----|-------------|------|
| `KycCaseResponse` | `requirements` | `List<KycRequirementStatus>` |
| `PurchaseRequestResponse` | `autoCreatedOrderCode` | `String \| null` |
| `ReorderSuggestionResponse` | `consumptionRateLast30Days` | `BigDecimal \| null` |
| `ReorderSuggestionResponse` | `daysOfStockRemaining` | `Integer \| null` |
| `InventoryMovementReportResponse` | `reportTitle` | `String` |
| `CreateSubscriptionRequest` | `autoActivate` | `Boolean \| null` |

## Récapitulatif des nouveaux types d'alerte inventaire

| Type | Déclencheur | Résolution auto |
|------|-------------|-----------------|
| `RECURRING_LOW_STOCK` | 3e+ occurrence stock bas | Oui — stock > seuil |
| `OVERSTOCK` | Stock > maxQuantity | Oui — stock <= maxQuantity |
| `SLOW_MOVING` | Aucune sortie 30j | Non — worker seulement |
| `EXPIRY_IMMINENT` | Lot expire ≤ 7j | Non — worker seulement |
