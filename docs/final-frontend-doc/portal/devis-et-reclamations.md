# Devis, proformas et réclamations dans l'espace client (lot C16)

> Rédigé d'après le code (compléments de facturation, 13/11/2026). Espace client authentifié : le membre connecté ne voit que ses pièces, jamais un brouillon.

| Route (`/sni/api/v1/client/billing`) | Rôle |
| --- | --- |
| `GET /quotes?type=QUOTE` ou `?type=PROFORMA_INVOICE` | Mes devis, mes proformas (pagination) |
| `GET /quotes/{n}`, `GET /quotes/{n}/pdf` | Détail, PDF |
| `POST /quotes/{n}/accept`, `POST /quotes/{n}/reject` | Répondre à un devis proposé ; un devis déjà répondu renvoie 400 `DEVIS_DEJA_REPONDU` |
| `POST /proformas/{n}/pay/mobile-money` | Payer une proforma (`phoneNumber`, `correspondent`) ; le paiement devient un acompte du devis |
| `GET /claim-reasons` | Motifs de réclamation |
| `POST /invoices/{n}/claims` | Réclamer sur une facture : `{"reasonCode", "description", "lineOrders", "requestedAmount"}` |
| `GET /claims`, `POST /claims/{n}/withdraw` | Mes réclamations ; retirer la sienne |

Factures, paiements et relevés : routes existantes de [l'espace client](README.md) (`/client/billing/invoices...`, `/statement`).
