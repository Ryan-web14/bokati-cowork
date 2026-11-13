# Livraison du plan facturation et devis

Au 13/11/2026, branche `feature/facturation-devis`. Les 16 lots du [plan de réalisation](realisation.md) sont livrés, chacun avec ses migrations, ses tests et son commit ; la suite complète passe après chaque lot. Documentation frontend : `docs/final-frontend-doc/billing/` (README et un fichier par domaine), `docs/final-frontend-doc/approval/`, `docs/final-frontend-doc/securite/fiche-collaborateur.md`, `docs/final-frontend-doc/fund/demandes-avances.md`.

## 1. Lots livrés

| Lot | Contenu | Écarts | Migrations (dev / prod) |
| --- | --- | --- | --- |
| B0 | Signature publique réduite et refusée sur un devis sorti du circuit ou révisé ; avoir sur rectificative et note de débit ; exonération au recalcul | E2, E4, E5 | V324 / V318 |
| B1 | `send` sans changement de statut, `validate` après émission, avoir `APPLIED` ou `REFUNDED`, `VOIDED` retiré, devis `EXPIRED` chaque heure, acompte abandonné, auteurs tirés du jeton | E6, E7, E8, E10, E14 | V325 / V319 |
| B12 | Fiche collaborateur (fonction, signature, N+1), auteur de la pièce et de chaque version, « Établi par » et visa au PDF | | V326 / V320 |
| B15 | Centres d'approbation sur un modèle commun (circuits, demandes, niveaux, frise, délégations), hiérarchie, validation en avance, rappels et escalade ; demandes de fonds de la caisse branchées | | V327 / V321 |
| B13 | Simulation ligne par ligne (`simulate-line`), même calcul que la création, alertes ligne par ligne, contrôle de stock, descriptions de 4 000 caractères | E17 | |
| B2 | Révision des devis par versions (motif, origine), frise de négociation, devis accepté revu (D1), validité prolongée, réactivation (D4) | E9 | V328 / V322 |
| B14 | Facture non payée modifiable après approbation ; nouvelle version, ou rectificative si certifiée ou comptabilisée (D8) | | V329 / V323 |
| B3 | Plan d'acomptes, factures d'acompte, déduction sur la facture finale, remboursement ou avoir (D4), `4191` | | V330 / V324 |
| B8 | Saisie d'un total ventilé au franc près, remise implicite contrôlée | | |
| B11 | Affaires : fiche, rattachement automatique, tableau, marges, liste et export, création depuis le CRM | | V331 / V325 |
| B5 | Proforma sans valeur fiscale, modifiable à tout statut (D9), transformée en devis accepté, paiement en acompte (D2) | | V332 / V326 |
| B4 | Envois multi-destinataires (copie, copie cachée) et multi-canaux (courriel, WhatsApp), journal, préférence par client, duplicata | E15 | V333 / V327 |
| B6 | Changement de client : sur place, ou avoirs et nouvelle facture, paiements gardés ou transférés (D6) ; correctif de la double contre-passation | | V334 / V328 |
| B7 | Vérification des disponibilités (stock, ressources), réservation à l'acceptation, sortie à la facturation | | V335 / V329 |
| B9 | Relevé paginé et trié, export complet, PDF et envoi, solde `411` ; balance âgée par devise ; échéanciers suivant tous les paiements | E11, E12, E13 | V336 / V330 |
| B10 | Prix par segment et datés avec historique, lien inventaire, compte de produit, CSV, marges ; routes `/quotations` dépréciées ; messages français et code `reason` | E18, E19 | V337 / V331 |

## 2. Nouvelles routes

Facturation (`/sni/api/v1/billing`) :
- devis : `PUT /quotes/{n}`, `GET /quotes/{n}/negotiation`, `PATCH /quotes/{n}/revalidate`, `POST /quotes/{n}/deposits`, `POST /quotes/{n}/proforma` ;
- factures : `POST /invoices/{n}/deposits`, `POST`, `GET /invoices/{n}/change-requests`, `PATCH /invoices/change-requests/{id}/cancel`, `POST /invoices/{n}/change-customer` ;
- proformas : `POST /proformas`, `POST /proformas/{n}/to-quote` ;
- documents : `POST /documents/simulate-line`, `POST /documents/allocate-amount`, `POST /documents/check-availability`, `POST /documents/{n}/send`, `GET /documents/{n}/sendings`, `GET /documents/{n}/deposits` ;
- acomptes : `PATCH /deposits/{id}/invoice`, `/cancel`, `/settle` ;
- clients : `GET`, `PUT /customers/{t}/{c}/billing-recipients`, `GET /customers/{t}/{c}/statement.pdf`, `POST /customers/{t}/{c}/statement/send` ;
- affaires : `/deals` (liste, export, création, depuis une opportunité, tableau, modification, étape, rattachement) ;
- catalogue : `/catalog/{itemCode}/prices`, `/price`, `/links`, `/catalog/margins`, `/catalog/export`, `/catalog/import`.

Approbations (`/sni/api/v1/approvals`) : `hierarchy`, `circuits`, `delegations`, `{center}/mine`, `{center}/to-approve`, `{center}/history`, `requests/{number}`, `{number}/approve`, `/reject`, `/send-back`.

Utilisateurs : `/admin/users/{userCode}/staff-profile`, `/staff/me/profile`, `/staff/me/signature`.

## 3. Paramètres ajoutés

| Clé | Défaut | Rôle |
| --- | --- | --- |
| `bokati.billing.workers.quote-expiry-delay-ms` | 3 600 000 | Cadence de l'expiration des devis et proformas |
| `billing.quote.revision-validity-days` | 30 | Validité garantie après une révision de devis |
| `billing.deal.auto-create` | true | Affaire ouverte au premier devis d'un client |
| `billing.stock.issue-on` | INVOICE | Sortie de stock à la facturation (ou NONE) |
| `accounting.sales.deposit-account` | 4191 | Compte des acomptes reçus |
| `approval.reminder-hours` | 24 | Rappel aux décideurs |
| `approval.escalation-hours` | 72 | Escalade au supérieur du décideur attendu |
| `approval.worker-cron` | 0 0 8-18 * * * | Planification des rappels et escalades |

Retirés : `fund.approval.reminder-hours`, `fund.approval.escalation-days` (remplacés par les clés `approval.*`).

## 4. Droits ajoutés

`APPROVAL_MANAGE` (SUPER_ADMIN, ADMIN), `BILLING_APPROVE` (SUPER_ADMIN, ADMIN, FINANCE, MANAGER), `BILLING_APPROVE_L2` (SUPER_ADMIN, ADMIN, FINANCE).

## 5. Schémas comptables ajoutés

`SALES_DEPOSIT_INVOICE` (facture d'acompte : D client, C 4191), `SALES_DEPOSIT_APPLIED` (acompte déduit : D 4191, C client, TVA en attente libérée au prorata), `SALES_RECEIVABLE_TRANSFER` (règlement transféré d'un client à l'autre). Correctif : une facture annulée par avoir n'est plus contre-passée une seconde fois.

## 6. Reprise des données (migrations)

- `VOIDED` devient `CANCELLED` ; les avoirs consommés `ISSUED` deviennent `APPLIED` ou `REFUNDED` (portefeuille).
- Les délégations de la caisse passent dans `approval_delegation` ; les demandes de fonds en cours de validation sont reprises en demandes d'approbation, au même niveau.
- Les échéanciers actifs reçoivent `paid_before` (déjà payé hors échéances).
- Les proformas ne comptent plus dans l'encours : une proforma ancienne émise et non payée disparaît des soldes clients (comportement voulu, décision D2).

## 7. Points ouverts

- **D3, TVA des acomptes** : la TVA est portée par la facture finale ; en régime des encaissements elle devient exigible au prorata de chaque acompte déduit. À valider avec l'expert-comptable (exigibilité à l'encaissement de l'acompte).
- **Abonnement aux événements WhatsApp** (remise et lecture) : à créer avec une clé Infobip portant `subscriptions:manage` (voir `docs/backend/whatsapp-infobip`) ; en attendant, la tâche de suivi relit les statuts.
- **Courriel en file** : le journal des envois note `QUEUED` pour un courriel ; l'état de remise réel se lit dans le journal des courriels du socle.
