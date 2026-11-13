# billing : relevé client, balance âgée, échéanciers (lot B9)

> Retour au [README du module](README.md). Toutes les routes sont sous `/sni/api/v1/billing`.

## Relevé client

`GET /customers/{customerType}/{customerCode}/statement?page=0&size=50&sort=issueDate,desc`

- Tri respecté sur `createdAt` (défaut, décroissant), `issueDate`, `dueDate`, `totalAmount`, `balanceDue`, `paidAmount`, `documentNumber`, `status`, `documentType` ; tout autre champ est ignoré.
- Réponse `CustomerStatementResponse` : totaux (`totalInvoiced`, `totalPaid`, `totalBalanceDue`, `totalCreditAvailable`, `netBalanceDue`, `totalDraft`), `documents` de la page, **pagination** (`page`, `size`, `totalElements`, `totalPages`) et `accountingBalance` (solde du compte client `411` en comptabilité, vide tant qu'aucune pièce ne le porte ; utile pour vérifier l'accord entre facturation et comptabilité).
- Sommer `customerImpact` sur les documents, jamais `balanceDue`.

Exports :
- `GET /customers/{t}/{c}/statement.csv` : **tout** le relevé (toutes les pages), avec le tri demandé.
- `GET /customers/{t}/{c}/statement.pdf?from=2026-01-01&to=2026-11-30` : relevé de compte PDF d'une organisation (`CUSTOMER`) ou d'un membre (`MEMBER`) sur une période.
- `POST /customers/{t}/{c}/statement/send` avec `{ "to": [...], "cc": [...], "bcc": [...], "from": "2026-01-01", "until": "2026-11-30", "message": "..." }` : envoi par courriel, PDF joint.

## Balance âgée

`GET /aging-report?customerType=&customerCode=&currency=XAF` : **une seule devise** par balance (paramètre `currency`, XAF par défaut) ; les documents d'une autre devise n'y figurent pas, jamais de cumul entre devises. Tranches `CURRENT`, `DAYS_1_TO_30`, `DAYS_31_TO_60`, `DAYS_61_TO_90`, `DAYS_OVER_90` sur `dueDate`, avec un total général (`summary.grandTotal`). Une proforma n'y figure plus (lot B5). Écran : un sélecteur de devise.

## Échéanciers

- `POST /invoices/{n}/schedule` : seulement une facture, une rectificative ou une note de débit **émise** (ni brouillon, ni annulée), avec un solde ; un seul échéancier actif par facture ; total des échéances au plus le solde.
- **Tout paiement** sur la facture (direct, par échéance, avoir imputé, acompte déduit) se répartit sur les échéances **dans l'ordre** ; une annulation de paiement les rouvre. Ce qui était déjà payé à la création de l'échéancier n'y compte pas.
- `POST /schedules/installments/{installmentNumber}/pay` encaisse sur la facture puis recalcule l'échéancier : payer l'échéance 2 avant la 1 remplit d'abord la 1.
- Statuts d'échéance : `PENDING`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`, `CANCELLED` ; échéancier `ACTIVE` puis `COMPLETED`.
