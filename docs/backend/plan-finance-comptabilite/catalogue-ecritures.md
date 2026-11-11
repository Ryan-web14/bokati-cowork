# Catalogue des écritures automatiques

Pour chaque fait de la plateforme : l'événement comptable publié, le journal, les lignes produites et l'analytique. Les comptes sont ceux proposés au [README, section 4.1](README.md#41-plan-de-comptes), à valider avec l'expert-comptable. Ce sont les **valeurs initiales** des schémas d'imputation (`acc_posting_rule`), modifiables sans code.

Conventions :

- `D` débit, `C` crédit.
- `{TRESO}` est le compte de trésorerie du moyen. Caisse de la session : `571x`. Caisse automatique du portefeuille : sans objet, voir la section 4. Mobile money plateforme : compte PawaPay. Mobile money marchand : compte du numéro. Chèque : `513`. Virement : compte bancaire désigné.
- `{CLIENT}` : compte auxiliaire `411` du client de la facture (membre, client ou organisation payeuse).
- `{NATURE}` : compte de la nature d'opération choisie.
- `{PRODUIT}` : compte de produit de la ligne de facture (par type de ligne ou de service du catalogue).
- Clé d'idempotence : `eventType` + référence. Un même événement ne produit qu'une pièce.
- Analytique : « héritée » signifie copiée des lignes de la facture concernée.

## Sommaire

1. [Ventes](#1-ventes)
2. [Caisse](#2-caisse)
3. [Encaissements](#3-encaissements)
4. [Portefeuille électronique](#4-portefeuille-électronique)
5. [Remboursements](#5-remboursements)
6. [Abonnements, pass, domiciliation](#6-abonnements-pass-domiciliation)
7. [Réservations](#7-réservations)
8. [Marketplace et stock](#8-marketplace-et-stock)
9. [Demandes de fonds et avances](#9-demandes-de-fonds-et-avances)
10. [Banque, chèques, mobile money](#10-banque-chèques-mobile-money)
11. [Clôture et inventaire](#11-clôture-et-inventaire)
12. [Ce qui ne produit pas d'écriture](#12-ce-qui-ne-produit-pas-décriture)

---

## 1. Ventes

| Événement | Fait déclencheur | Journal | Écriture | Analytique |
| --- | --- | --- | --- | --- |
| `INVOICE_ISSUED` | Facture émise ou scellée (numéro définitif) | `VT` | D `{CLIENT}` TTC ; C `{PRODUIT}` HT par ligne ; C `4431` TVA par taux | Par ligne : site et activité de la ressource ou du service |
| `INVOICE_DISCOUNT` | Remise de document, coupon, réduction de parrainage portés sur la facture | `VT` (même pièce) | D compte de réductions sur ventes HT ; la TVA est déjà calculée sur le net | Héritée |
| `CREDIT_NOTE_ISSUED` | Avoir (annulation partielle d'une réservation de lot, geste commercial, correction) | `VT` | Inverse de la facture au prorata : D `{PRODUIT}`, D `4431` ; C `{CLIENT}` | Héritée de la facture d'origine |
| `INVOICE_CANCELLED` | Facture annulée avant tout paiement | `VT` | Contre-passation de la pièce `INVOICE_ISSUED` | Héritée |
| `EARLY_PAYMENT_DISCOUNT` | Escompte de règlement appliqué au paiement | `VT` | D `673` ; D `4431` (TVA sur l'escompte, selon le régime) ; C `{CLIENT}` | Héritée |
| `RECOVERABLE_BILLED` | Débours refacturés au client | `VT` | D `{CLIENT}` ; C compte de débours (sans TVA si débours au sens fiscal) | Héritée |
| `INVOICE_WRITTEN_OFF` | Facture passée en perte (`WRITTEN_OFF`) | `OD` | D `651` HT ; D `4431` (si récupération de TVA admise) ; C `{CLIENT}` ; reprise de provision s'il y en a une | Héritée |

## 2. Caisse

Tous dans le journal choisi par la caissière (par défaut `CA-<caisse>`).

| Type de mouvement | Cas | Écriture |
| --- | --- | --- |
| `OPENING_FLOAT` | Fonds de caisse de la première session | D `571x` ; C `585` (venant de la caisse principale ou de la banque) |
| `OPENING_FLOAT` | Report de fin de mois (clôture automatique) | **Aucune** écriture : l'argent ne bouge pas |
| `PAYMENT` | Encaissement client | **Aucune** écriture du mouvement : c'est `PAYMENT_RECEIVED` qui écrit (section 3), pour ne pas doubler |
| `REFUND` | Remboursement client en espèces | **Aucune** écriture du mouvement : c'est `PAYMENT_REFUNDED` qui écrit (section 5) |
| `CASH_OUT` / bon de sortie | Dépense directe sur pièce | D `{NATURE}` HT ; D `4452`/`4454` TVA récupérable ; C `571x` |
| `CASH_OUT` | Paiement d'une facture fournisseur | D `401` fournisseur (lettré) ; C `571x` |
| `CASH_OUT` | Décaissement d'une avance à justifier | D `421` bénéficiaire ; C `571x` |
| `CASH_OUT` | Restitution d'un dépôt de garantie | D `165` ; C `571x` |
| `CASH_IN` / bon d'entrée | Retour de reliquat d'avance | D `571x` ; C `421` bénéficiaire |
| `CASH_IN` | Dépôt de garantie reçu | D `571x` ; C `165` |
| `CASH_IN` | Produit divers (remboursement d'assurance, cession) | D `571x` ; C `{NATURE}` |
| `SAFE_DEPOSIT` | Remise en banque ou au coffre | D `585` ; C `571x` ; la banque solde le `585` au relevé |
| `TRANSFER_OUT` / `TRANSFER_IN` | Transfert entre caisses | Émettrice : D `585` ; C `571x`. Réceptrice : D `571y` ; C `585`. Le `585` se solde quand les deux côtés sont passés |
| `ADJUSTMENT` | Correction manuelle approuvée | Selon la nature choisie, sinon D/C `571x` contre `471` |
| Écart de clôture (session en `CLOSING_REVIEW`) | Manquant | D `471` ; C `571x` |
| Écart de clôture | Excédent | D `571x` ; C `471` |
| Approbation de l'écart | Manquant passé en charge | D `658` ; C `471` (ou D `421` du caissier si imputé) |
| Approbation de l'écart | Excédent passé en produit | D `471` ; C `758` |
| Contre-mouvement (`relatedMovementId`) | Annulation d'un mouvement | Contre-passation de la pièce du mouvement d'origine |

Analytique des sorties : section de la nature, sinon site de la caisse ; modifiable à la saisie.

## 3. Encaissements

Événement `PAYMENT_RECEIVED` : transaction `SUCCEEDED`, publié en même temps que le `PAYMENT_TRANSACTION_WORKFLOW` existant.

| Moyen | Journal | Écriture |
| --- | --- | --- |
| `CASH` | `CA-<caisse de la session>` | D `571x` ; C `{CLIENT}` lettré avec la ou les factures imputées |
| `MOBILE_MONEY` (PawaPay) | `MM-PAWAPAY` | D compte PawaPay ; C `{CLIENT}` |
| `MOBILE_MONEY_OFFLINE` | `MM-<opérateur>` | D compte du numéro marchand ; C `{CLIENT}` |
| `CHEQUE` | `BQ-CHQ` ou journal de la caisse | D `513` ; C `{CLIENT}` |
| `BANK_TRANSFER` | `BQ-<banque>` | D `521x` ; C `{CLIENT}` (à pointer sur le relevé) |
| `CARD` | `BQ-<acquéreur>` | D compte d'attente carte `5851` ou `521x` ; C `{CLIENT}` |
| `WALLET` | `PF` | D `4191` (sous-compte du client) ; C `{CLIENT}` |
| `CREDIT_NOTE` | `OD` | Imputation d'un avoir sur une facture : D `{CLIENT}` (avoir) ; C `{CLIENT}` (facture), lettrage |

Cas particuliers :

| Cas | Écriture |
| --- | --- |
| Paiement sans facture (acompte, intention sur une source non facturée) | C `4191` au lieu de `{CLIENT}` ; à l'émission de la facture, D `4191` ; C `{CLIENT}` |
| Trop-perçu crédité au portefeuille (`creditOverpayment`) | Dans la même pièce : C `4191` pour l'excédent |
| Facture commune d'un lot multi-ressources | Une seule écriture par paiement ; le lettrage porte la facture commune |
| Monnaie rendue | Rien de plus : seul le montant encaissé est écrit |
| Frais opérateur PawaPay | D `631` ; C compte PawaPay (au taux contractuel, ou au relevé) |

## 4. Portefeuille électronique

Le portefeuille est une dette envers le client : son solde total doit égaler le solde de `4191` (contrôle quotidien, et extension de `WalletTreasuryReconciliation`).

| Événement | Écriture |
| --- | --- |
| Rechargement en espèces | D `571x` ; C `4191` client |
| Rechargement par mobile money | D compte PawaPay ; C `4191` client |
| Paiement par portefeuille | voir section 3 (`WALLET`) |
| Retenue posée (`WalletHold`) | Aucune : l'argent reste au client |
| Retenue capturée | Écriture du paiement qu'elle règle |
| Transfert entre portefeuilles | D `4191` émetteur ; C `4191` bénéficiaire |
| Expiration ou péremption d'un solde (si les conditions le prévoient) | D `4191` ; C `758` (à valider juridiquement) |
| Ajustement administratif à double visa | D/C `4191` contre `471` puis compte décidé (geste commercial `6xx`, correction) |
| Remboursement du solde au client | D `4191` ; C `{TRESO}` |

## 5. Remboursements

Événement `PAYMENT_REFUNDED` (et `PAYMENT_REVERSED`) : transaction de remboursement `SUCCEEDED`.

| Cas | Écriture |
| --- | --- |
| Remboursement en espèces | D `{CLIENT}` ; C `571x` |
| Remboursement mobile money PawaPay | D `{CLIENT}` ; C compte PawaPay |
| Remboursement d'un moyen hors plateforme (chèque, virement, MoMo marchand) | D `{CLIENT}` ; C `{TRESO}` du moyen de restitution |
| Remboursement vers le portefeuille | D `{CLIENT}` ; C `4191` |
| Remboursement mobile money refusé par l'opérateur | Aucune écriture (la transaction est `FAILED`) |
| Contre-passation (chèque rejeté, erreur de saisie) | Contre-passation de l'écriture d'encaissement ; la facture redevient due |

L'avoir qui accompagne le remboursement (annulation de réservation) est écrit par `CREDIT_NOTE_ISSUED`. Le remboursement solde le crédit du client.

## 6. Abonnements, pass, domiciliation

Chiffre d'affaires étalé, mode retenu par défaut ([décision 5](README.md#19-décisions-prises), paramétrable) :

| Événement | Écriture |
| --- | --- |
| Facture d'abonnement ou de pass couvrant une période future | Écriture de vente normale |
| Fin de mois (worker comptable) | Pour la part de service non encore rendue : D `{PRODUIT}` ; C `477` |
| Mois suivant | Reprise de la part rendue : D `477` ; C `{PRODUIT}` |
| Résiliation avec avoir | Avoir sur la part non rendue, reprise du `477` correspondant |

Sans étalement, seule l'écriture de vente s'applique. Les crédits d'heures ou de réservations consommés ne produisent pas d'écriture.

## 7. Réservations

| Événement | Écriture |
| --- | --- |
| Réservation confirmée | Aucune : l'écriture est celle de sa facture (ou de sa ligne dans la facture commune du lot) |
| Annulation avant paiement | `CREDIT_NOTE_ISSUED` (part de la réservation) ou `INVOICE_CANCELLED` |
| Annulation après paiement avec pénalité | Avoir de la part remboursée ; la pénalité reste en produit ; `PAYMENT_REFUNDED` pour le versement |
| Réservation en `WALLET` | Paiement `WALLET` (section 3) à la capture de la retenue |
| Réservation en `SUBSCRIPTION` ou `PASS` | Ligne à montant nul : aucune écriture |

Analytique : site, activité et ressource de la réservation.

## 8. Marketplace et stock

| Événement | Écriture |
| --- | --- |
| Commande facturée | `INVOICE_ISSUED` avec ventes de marchandises `701` |
| Sortie de stock à la livraison | Reprise de `stock_journal_entry` dans le journal `ST` : D `603` ; C `31` (comptes déjà portés par l'inventaire) |
| Entrée en stock, ajustement d'inventaire | Reprise de `stock_journal_entry` (compte de contrepartie du motif) |
| Vente pour compte d'un vendeur tiers (si la marketplace en a) | C compte du vendeur `401`/`467` pour sa part ; C `706` pour la commission |

## 9. Demandes de fonds et avances

| Événement | Écriture |
| --- | --- |
| Demande approuvée | Aucune écriture générale ; **engagement budgétaire** si le budget est activé |
| Décaissement d'une dépense directe | voir section 2 (`CASH_OUT`, dépense directe) |
| Décaissement d'une avance | D `421` bénéficiaire ; C `571x` |
| Ligne de justification validée | D `{NATURE}` HT ; D `4452`/`4454` TVA ; C `421` bénéficiaire |
| Reliquat rendu | D `571x` ; C `421` |
| Complément versé (dépenses supérieures à l'avance) | D `421` ; C `571x`, puis la ligne justifiée solde le `421` |
| Avance non justifiée au-delà du délai, retenue sur salaire décidée | D `421` reste ouvert ; transmis à la paie (`PA`) |
| Note de frais remboursée | D `{NATURE}` ; D TVA ; C `571x` (ou `421` puis caisse) |
| Réalimentation de petite caisse | D petite caisse `571z` ; C caisse principale `571x` ou banque |

## 10. Banque, chèques, mobile money

| Événement | Écriture |
| --- | --- |
| Bordereau de remise d'espèces | D `585` ; C `571x` (déjà via `SAFE_DEPOSIT`) |
| Crédit en banque de la remise (relevé) | D `521x` ; C `585` |
| Remise de chèques | Pas d'écriture au bordereau ; le chèque passe `DEPOSITED` |
| Chèque encaissé (relevé) | D `521x` ; C `513` |
| Chèque rejeté | Contre-passation : D `{CLIENT}` ; C `513` ; frais de rejet D `631` ; C `521x` ; refacturation éventuelle |
| Reversement PawaPay vers la banque | D `521x` ; C compte PawaPay |
| Frais bancaires, agios (relevé) | D `631` ; C `521x` |
| Retrait pour alimenter une caisse | D `585` ; C `521x`, puis caisse D `571x` ; C `585` |

## 11. Clôture et inventaire

| Événement | Écriture |
| --- | --- |
| Provision pour créance douteuse | D `6594` ; C `491` |
| Reprise de provision | D `491` ; C `7594` |
| Produits constatés d'avance | voir section 6 |
| Charges constatées d'avance (abonnement logiciel payé d'avance) | D `476` ; C `{NATURE}`, extourne à l'ouverture |
| Dotation aux amortissements | D `681` ; C `28x` |
| Liquidation de TVA | D `4431` ; C `4452`/`4454` ; solde vers `4441` (dette) ou `4449` (crédit) |
| Solde des comptes d'attente | `471` vers comptes définitifs, à la main |
| Résultat et à-nouveaux | Écritures de clôture et journal `AN` |

## 12. Ce qui ne produit pas d'écriture

- Création, modification, expiration ou annulation d'une **intention de paiement** (pas de mouvement d'argent).
- Dépôt mobile money **en cours** (`PROCESSING`) ou en échec.
- Retenue de portefeuille posée ou libérée.
- Réservation confirmée, annulée sans facture, réservation à montant nul.
- Consommation d'heures, de crédits ou de pass.
- Devis, bon de commande, état de besoin, demande approuvée (hors engagement budgétaire).
- Report de fonds de caisse en fin de mois.
- Mouvements `PAYMENT` et `REFUND` de caisse (écrits par l'événement de paiement).
