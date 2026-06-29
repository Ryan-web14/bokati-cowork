# Portail Client — Phase 6 : Guide Frontend — Facturation & Paiements

Tous les endpoints requièrent `Authorization: Bearer <accessToken>`.

---

## Factures — `/client/billing/invoices`

### GET `/client/billing/invoices`

Liste les factures du membre, les plus récentes en premier.

**Paramètres de requête :**
| Paramètre | Type | Description |
|---|---|---|
| `status` | String | Filtrer par statut (voir tableau ci-dessous) |
| `from` | Date | Date d'émission ≥ (ISO : `2026-01-01`) |
| `to` | Date | Date d'émission ≤ (ISO : `2026-06-30`) |
| `page` / `size` | Integer | Pagination |

**Valeurs de `status` :**
| Statut | Signification |
|---|---|
| `DRAFT` | Pas encore émise |
| `ISSUED` | Émise, en attente de paiement |
| `SENT` | Envoyée au membre |
| `PARTIALLY_PAID` | Partiellement payée |
| `PAID` | Entièrement payée |
| `OVERDUE` | Dépassée, impayée |
| `CANCELLED` | Annulée |
| `VOIDED` | Invalidée |

**Réponse — `200 OK` :**
```json
{
  "data": [
    {
      "documentNumber": "INV-2026-0042",
      "documentType": "INVOICE",
      "status": "ISSUED",
      "title": "Facture Abonnement Juin 2026",
      "currency": "XAF",
      "totalAmount": 75000,
      "paidAmount": 0,
      "balanceDue": 75000,
      "issueDate": "2026-06-01",
      "dueDate": "2026-06-15",
      "issuedAt": "2026-06-01T08:00:00Z",
      "paidAt": null,
      "sourceType": "SUBSCRIPTION",
      "sourceCode": "SUB-2026-0001",
      "resolvedSourceLabel": "Abonnement Flex - Juin 2026"
    }
  ],
  "pageable": { "page": 0, "size": 20, "totalElements": 3, "totalPages": 1, "first": true, "last": true }
}
```

**Couleurs des badges par statut :**
| Statut | Couleur |
|---|---|
| `PAID` | Vert |
| `PARTIALLY_PAID` | Jaune |
| `ISSUED` / `SENT` | Bleu |
| `OVERDUE` | Rouge |
| `DRAFT` | Gris |
| `CANCELLED` / `VOIDED` | Gris barré |

---

### GET `/client/billing/invoices/payable`

Retourne uniquement les factures avec un solde restant dû (`balanceDue > 0`). Utiliser pour la section "Payer maintenant".

**Réponse — `200 OK` :** Même structure que la liste `/invoices`.

---

### GET `/client/billing/invoices/{documentNumber}`

Détail complet d'une facture.

**Réponse — `200 OK` :**
```json
{
  "documentNumber": "INV-2026-0042",
  "documentType": "INVOICE",
  "status": "ISSUED",
  "title": "Facture Abonnement Juin 2026",
  "description": null,
  "customerName": "Jean Dupont",
  "customerEmail": "jean@example.com",
  "currency": "XAF",
  "subtotalAmount": 75000,
  "discountAmount": 0,
  "vatAmount": 0,
  "totalAmount": 75000,
  "paidAmount": 0,
  "balanceDue": 75000,
  "issueDate": "2026-06-01",
  "dueDate": "2026-06-15",
  "issuedAt": "2026-06-01T08:00:00Z",
  "paidAt": null,
  "paymentReference": null,
  "paymentInstructions": "Paiement par Mobile Money ou portefeuille",
  "bankDetailsJson": null,
  "sourceType": "SUBSCRIPTION",
  "sourceCode": "SUB-2026-0001",
  "resolvedSourceLabel": "Abonnement Flex",
  "lines": [
    { "lineType": "SUBSCRIPTION", "description": "Abonnement Flex — Juin 2026", "quantity": 1, "unitPrice": 75000, "totalAmount": 75000, "unit": "mois" }
  ]
}
```

---

### GET `/client/billing/invoices/{documentNumber}/pdf`

Télécharge la facture en PDF.

**Réponse :** PDF binaire avec les en-têtes :
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="invoice-INV-2026-0042.pdf"
```

---

## Paiement des factures

### POST `/client/billing/invoices/{documentNumber}/pay/mobile-money`

Initie un paiement Mobile Money pour une facture.

**Corps de la requête :**
```json
{
  "phoneNumber": "242065000000",
  "correspondent": "MTN_MOMO_COG"
}
```

**Valeurs de `correspondent` (focus Congo) :**
| Valeur | Opérateur |
|---|---|
| `MTN_MOMO_COG` | MTN Mobile Money (Congo) |
| `AIRTEL_COG` | Airtel Money (Congo) |
| `ORANGE_COD` | Orange Money (RDC) |
| `AIRTEL_COD` | Airtel Money (RDC) |
| `VODACOM_MPESA_COD` | Vodacom M-Pesa (RDC) |
| `MTN_MOMO_CIV` | MTN Mobile Money (Côte d'Ivoire) |
| `ORANGE_CIV` | Orange Money (Côte d'Ivoire) |
| `WAVE_CIV` | Wave (Côte d'Ivoire) |
| `ORANGE_SEN` | Orange Money (Sénégal) |
| `WAVE_SEN` | Wave (Sénégal) |

**Réponse — `200 OK` :**
```json
{
  "depositId": "uuid-deposit-id",
  "clientReferenceId": "our-ref-123",
  "status": "PENDING",
  "provider": "MTN_MOMO_COG",
  "amount": 75000,
  "currency": "XAF",
  "phoneNumber": "242065000000",
  "customerMessage": "Paiement Bokati",
  "intentNumber": "PI-2026-0010",
  "transactionNumber": null,
  "createdAt": "2026-06-15T09:00:00Z"
}
```

**Flux UX :**
1. Afficher le formulaire : numéro de téléphone + sélecteur d'opérateur
2. POST → reçoit le dépôt avec `status: "PENDING"`
3. Afficher l'état de chargement "Attente de confirmation..."
4. Interroger `GET /payments/mobile-money/deposits/{depositId}` toutes les 5 à 10 secondes
5. Quand `status = "COMPLETED"` → afficher succès + lien vers le reçu
6. Quand `status = "FAILED"` → afficher erreur + option de rééssai

**Note :** La confirmation de paiement est asynchrone. Le membre reçoit une invite USSD sur son téléphone.

---

### POST `/client/billing/invoices/{documentNumber}/pay/wallet`

Paye une facture avec le solde du portefeuille du membre. Aucun corps requis — le portefeuille est auto-résolu depuis la devise de la facture.

**Réponse — `200 OK` :** `PaymentTransactionResponse`
```json
{
  "transactionNumber": "TXN-2026-0055",
  "intentNumber": "PI-2026-0011",
  "paymentMethod": "WALLET",
  "provider": null,
  "providerReference": null,
  "receiptNumber": "RCP-2026-0055",
  "amount": 75000,
  "currency": "XAF",
  "status": "SUCCEEDED",
  "paidAt": "2026-06-15T09:05:00Z",
  "receivedBy": null,
  "failureReason": null
}
```

**Erreurs :**
| Statut | Signification |
|---|---|
| `400` | Solde du portefeuille insuffisant |
| `400` | Facture déjà payée |
| `404` | Facture introuvable |

---

## Historique des paiements — `/client/billing/payments`

### GET `/client/billing/payments`

Liste les intentions de paiement du membre (toutes les tentatives de paiement).

**Réponse — `200 OK` :**
```json
{
  "data": [
    {
      "intentNumber": "PI-2026-0010",
      "amount": 75000,
      "paidAmount": 75000,
      "remainingAmount": 0,
      "currency": "XAF",
      "status": "SUCCEEDED",
      "purpose": "Paiement Facture INV-2026-0042",
      "sourceType": "INVOICE",
      "sourceCode": "INV-2026-0042",
      "resolvedSourceLabel": "Facture Abonnement Juin 2026"
    }
  ],
  "pageable": { ... }
}
```

**Valeurs de `status` d'une intention :**
| Statut | Signification |
|---|---|
| `PENDING` | En attente de paiement |
| `PROCESSING` | En cours de traitement |
| `SUCCEEDED` | Paiement validé |
| `FAILED` | Paiement échoué |
| `CANCELLED` | Annulé |
| `EXPIRED` | Intention expirée |
| `REFUNDED` | Remboursé intégralement |
| `PARTIALLY_REFUNDED` | Remboursé partiellement |

---

### GET `/client/billing/payments/{transactionNumber}/receipt`

Retourne un reçu de paiement.

**Réponse — `200 OK` :**
```json
{
  "receiptNumber": "RCP-2026-0055",
  "transactionNumber": "TXN-2026-0055",
  "intentNumber": "PI-2026-0010",
  "receiptIssuedAt": "2026-06-15T09:05:00Z",
  "paymentMethod": "MOBILE_MONEY",
  "transactionStatus": "SUCCEEDED",
  "provider": "MTN_MOMO_COG",
  "providerReference": "provider-txn-ref",
  "payerPhone": "242065000000",
  "purpose": "Paiement Facture INV-2026-0042",
  "customerName": "Jean Dupont",
  "customerEmail": "jean@example.com",
  "paidAmount": 75000,
  "currency": "XAF",
  "paidAt": "2026-06-15T09:05:00Z",
  "allocations": [
    { "billingDocumentNumber": "INV-2026-0042", "allocatedAmount": 75000 }
  ]
}
```

---

### GET `/client/billing/payments/{transactionNumber}/receipt/pdf`

Télécharge le reçu en PDF.

**Réponse :** PDF binaire avec les en-têtes :
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="receipt-TXN-2026-0055.pdf"
```

---

## Résumé des dépenses — `/client/billing/summary`

### GET `/client/billing/summary`

Retourne le récapitulatif de facturation du membre.

**Réponse — `200 OK` :**
```json
{
  "totalInvoiced": 225000,
  "totalPaid": 150000,
  "totalBalanceDue": 75000,
  "invoiceCount": 3,
  "unpaidCount": 1,
  "overdueCount": 0
}
```

Utiliser pour construire un widget tableau de bord :
- "Dépenses totales : XAF 225 000"
- "Payé : XAF 150 000"
- "Solde dû : XAF 75 000"
- Badge d'avertissement si `overdueCount > 0`

---

## Flux UX recommandés

### Payer une facture (Mobile Money)
```
1. GET /billing/invoices/payable         → badge "Payer" sur la liste de factures
2. GET /billing/invoices/{num}           → détail de facture + options de paiement
3. POST /billing/invoices/{num}/pay/mobile-money  → initier le dépôt
4. Polling GET /payments/mobile-money/deposits/{depositId}  → attendre la validation
5. À COMPLETED → GET /billing/payments/{txn}/receipt  → afficher le reçu
```

### Page liste des factures
```
GET /billing/summary              → widget tableau de bord (totaux)
GET /billing/invoices?page=0      → tableau des factures
GET /billing/invoices/payable     → onglet "Impayées" (optionnel)
```

---

## Référence des codes d'erreur

| Code | HTTP | Déclencheur |
|---|---|---|
| `INVOICE_NOT_FOUND` | `404` | Document introuvable ou appartient à un autre membre |
| `RECEIPT_NOT_FOUND` | `404` | Reçu introuvable ou appartient à un autre membre |
| `INSUFFICIENT_BALANCE` | `400` | Solde du portefeuille insuffisant pour couvrir la facture |
| `INVOICE_ALREADY_PAID` | `400` | La facture n'a plus de solde restant |
| `PAYMENT_INTENT_FAILED` | `400` | Demande rejetée par le fournisseur de paiement |
