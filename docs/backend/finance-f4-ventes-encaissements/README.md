# Lot F4 : ventes, encaissements, portefeuille

Lot du [plan finance](../plan-finance-comptabilite/README.md), écritures du [catalogue, sections 1, 3, 4 et 5](../plan-finance-comptabilite/catalogue-ecritures.md#1-ventes), lettrage du [README, section 4.7](../plan-finance-comptabilite/README.md#47-lettrage). S'appuie sur F1 (socle), F2 (moteur) et F3 (caisse comptable).

Branche `feature/finance-comptabilite`. Migrations : dev `V299__comptabilite_ventes_encaissements_f4.sql`, prod `V293__comptabilite_ventes_encaissements_f4.sql` (contenu identique). Code : `SalesAccountingService`, `SalesAccountingController`, lettrage automatique dans `AccountingLetteringService`.

## Principe

Les tables de facturation, de paiement et de portefeuille **ne sont pas modifiées** (le grand livre du portefeuille est en ajout seul). Le registre des événements comptables (`acc_posting_event`, clé type + référence) dit ce qui est déjà transmis : à chaque passage (30 s, comptabilité active), le service transmet ce qui manque.

| Fait | Événement | Référence | Date |
| --- | --- | --- | --- |
| Facture, facture rectificative, note de débit émise (`issuedAt` renseigné) | `INVOICE_ISSUED` | numéro du document | date d'émission |
| Avoir émis | `CREDIT_NOTE_ISSUED` | numéro de l'avoir | date d'émission |
| Document émis puis annulé (`CANCELLED`, `VOIDED`) | `INVOICE_CANCELLED` (contre-passation) | numéro du document | date d'annulation |
| Transaction `SUCCEEDED` (ou passée depuis en `REFUNDED`, `REVERSED`) | `PAYMENT_RECEIVED` | numéro de transaction | date de paiement |
| Transaction de remboursement ou d'annulation (liée à l'originale) | `PAYMENT_REFUNDED` | numéro de transaction | date |
| Écriture du portefeuille (hors retenues) | `WALLET_ENTRY` | numéro d'écriture | date |

Une transaction n'est transmise que `accounting.sales.payment-delay-minutes` (5) après son paiement, le temps de son imputation sur les factures.

L'état comptable d'un fait se lit par `GET /accounting/sales/status/{référence}` (événements, statut, pièce, motif).

## Écritures (schémas initiaux)

| Schéma | Écriture |
| --- | --- |
| `SALES_INVOICE` | Journal `VT`. D `411` client TTC (référence : numéro de facture, échéance) ; D `709` remises (brut des lignes moins base taxable) ; C produit HT par ligne ; C `4431` TVA ; C centime additionnel |
| `SALES_CREDIT_NOTE` | Inverse ; le crédit `411` porte le numéro de la facture d'origine pour se lettrer avec elle |
| `SALES_PAYMENT` | Journal du moyen. D trésorerie du moyen ; C `411` par facture imputée (référence : numéro de facture) ; C `4719` pour le reste (rechargement, trop-perçu, paiement à affecter) |
| `SALES_REFUND` | Inverse : D `411` par facture de la transaction d'origine, D `4719` pour le reste ; C trésorerie |
| `WALLET_ENTRY` | Journal `PF`. Crédit du portefeuille : C `4191` du client ; débit : D `4191` ; contrepartie selon le type |

Comptes de produit par type de ligne (`accounting.sales.account.<TYPE>`) : `PRODUCT` 701, `SUBSCRIPTION` et `PASS` 7062, `BOOKING` 7063, `WALLET` 4719, les autres 706.

Trésorerie par moyen (`accounting.payment.<MOYEN>.account` et `.journal`) :

| Moyen | Compte | Journal |
| --- | --- | --- |
| `CASH` | compte `571x` de la caisse de la session | `CA-571x` |
| `MOBILE_MONEY` (PawaPay) | `5212` | `MM-5212` |
| `MOBILE_MONEY_OFFLINE` | `5213` | `MM-5213` |
| `BANK_TRANSFER` | `5211` | `BQ-5211` |
| `CARD` | `5851` | `ENC` |
| `CHEQUE` | `513` | `ENC` |
| `WALLET` | `4719` (liaison) | `PF` |
| `CREDIT_NOTE` | `411` du client | `ENC` |
| `MANUAL_ADJUSTMENT` | `471` | `ENC` |

Contreparties du portefeuille : rechargement, trop-perçu, paiement, transferts entre portefeuilles, remboursement d'une transaction : `4719` ; remboursement d'un avoir sur le portefeuille : `411` du client (lettré avec la facture d'origine) ; ajustements administratifs : `471` ; crédits promotionnels et remises en argent : `658` ; frais de transfert : `706`.

**Le `4191` égale la somme des portefeuilles** : chaque écriture du portefeuille passe le `4191` du client, et les transactions passent par le compte de liaison `4719`, qui se solde (par exemple : rechargement par mobile money D `5212` C `4719`, puis écriture du portefeuille D `4719` C `4191`).

## Lettrage automatique

Toutes les 10 minutes (`accounting.lettering.auto-delay-ms`) et sur demande (`POST /accounting/sales/auto-letter`) : sur `411` et `4191`, les lignes ouvertes d'un même tiers qui portent la même référence se lettrent (type `AUTO_REFERENCE`) ; complet si elles se soldent, partiel sinon (paiement partiel : la facture garde son reste). Une contre-passation est lettrée avec l'original (lot F1). `GET /accounting/sales/lettering-proposals?accountNumber=411` propose les paires restantes (même tiers, même montant, références différentes), à valider par le lettrage manuel.

## Contrôles

`GET /accounting/sales/checks` :

- total des portefeuilles, solde de `4191`, différence ;
- nombre de factures, transactions et écritures de portefeuille pas encore comptabilisées ;
- factures dont le reste dû (facturation) diffère du reste ouvert en `411` (comptabilité), avec la différence.

## Routes

| Route | Droit |
| --- | --- |
| `GET /accounting/sales/checks`, `/status/{référence}`, `/lettering-proposals` | `ACCOUNTING_REPORT:READ` |
| `POST /accounting/sales/auto-letter` | `ACCOUNTING_LETTERING:MANUAL` |
| `POST /accounting/sales/feed` | `ACCOUNTING_POSTING:REPLAY` |

## Paramètres (domaine Comptabilité)

`accounting.sales.journal` (VT), `client-account` (411), `discount-account` (709), `vat-account` (4431), `extra-tax-account` (4431), `unallocated-account` (4719), `payment-delay-minutes` (5), `accounting.sales.account.<TYPE>`, `accounting.payment.<MOYEN>.account` / `.journal`, `accounting.wallet.journal` (PF), `admin-account` (471), `promotion-account` (658), `fee-account` (706), `accounting.lettering.auto-delay-ms` (600000, redémarrage).

## Pour le frontend

- **Facture, transaction, écriture de portefeuille** : pastille comptable via `GET /accounting/sales/status/{référence}` (statut de l'événement, numéro de pièce, motif d'échec).
- **Contrôles ventes** : tableau de bord `GET /accounting/sales/checks` (écart portefeuilles / `4191` en rouge s'il n'est pas nul, compteurs de faits non comptabilisés, liste des factures en écart avec lien vers la facture et vers le compte client).
- **Lettrage** : écran des propositions (`/lettering-proposals`), bouton « Lettrer » qui appelle `POST /accounting/lettering` avec les deux lignes ; bouton « Lettrage automatique maintenant ».

## Tests

`SalesAccountingF4IT` (schéma de production) : facture payée en espèces, compte client soldé et lettré, caisse débitée, contrôle d'écart facture ; rechargement mobile money crédité au `4191` du client ; facture annulée contre-passée ; avoir lettré avec sa facture.

## Évolutions apportées par les lots suivants

- **Lots F5 et F6** : le lettrage automatique par référence couvre aussi le `421` (avances du personnel, lettrées par numéro de demande de fonds).
- **Lot F9** : les produits (classe 7) sont ventilés sur l'axe `ACTIVITE` (obligatoire, section par défaut `STRUCTURE`) quand l'analytique est active.
- **Lot F10** : les comptes `5211`, `5212` et `5213` sont suivis en rapprochement bancaire ; un virement client lu sur un relevé devient un paiement imputé à sa facture ; un chèque rejeté annule son paiement ([finance-f10-banque-rapprochement](../finance-f10-banque-rapprochement/README.md)).
- **Complément multidevise** : les factures, avoirs, paiements et écritures de portefeuille en devise sont convertis en XAF (taux du document, sinon taux du jour) ; les schémas `SALES_INVOICE`, `SALES_CREDIT_NOTE`, `SALES_PAYMENT` et `SALES_REFUND` ont une nouvelle version où la ligne du client porte la devise et le montant en devise ; au lettrage automatique, si les montants en devise se soldent, l'écart en XAF est passé en perte ou gain de change (`676` / `776`) au lieu d'un lettrage partiel. Un paiement en devise sans taux attend que le taux soit connu.
