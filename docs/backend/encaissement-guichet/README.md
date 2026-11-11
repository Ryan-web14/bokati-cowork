# Encaissement au guichet

Monnaie rendue en espèces, paiements reçus hors de la plateforme (chèque, virement, mobile money hors plateforme), mobile money de la plateforme rangé dans sa caisse automatique, annulation et relance d'une intention de paiement. Back-office. Doc frontend : [docs/final-frontend-doc/payment/encaissement-guichet.md](../../final-frontend-doc/payment/encaissement-guichet.md).

## Décisions

- **Monnaie rendue.** La règle est dans `CashChange.compute(dû, amount, tenderedAmount)`. Elle est partagée par l'aperçu et l'encaissement :
  - montant encaissé = `amount`, sinon `min(tendered, dû)` ;
  - monnaie = `tendered - encaissé` ;
  - refus si `tendered < amount`.
- **Ce que la caisse enregistre.** Le mouvement de caisse porte le montant encaissé, pas le montant remis : la monnaie sort du tiroir au même moment. Le montant remis et la monnaie sont gardés sur la transaction (`tendered_amount`, `change_amount`) pour le reçu.
- **Nouveau moyen `MOBILE_MONEY_OFFLINE`.** Il est distinct de `MOBILE_MONEY` pour trois raisons :
  - il ne doit pas entrer dans `CSR-AUTO-MOBILE-MONEY`, rapprochée avec PawaPay ;
  - son remboursement ne doit pas partir chez PawaPay (les chemins de remboursement testent `== MOBILE_MONEY`) ;
  - l'opérateur (`MTN_MOMO`, `AIRTEL_MONEY`) est rangé dans `provider`.
- **Paiements hors plateforme.** Pour le chèque, le virement et le mobile money hors plateforme, la référence est obligatoire et le doublon est refusé. La clé est le moyen, le fournisseur et la référence (sans tenir compte de la casse), plus la banque pour un chèque ; seules les transactions `SUCCEEDED` comptent.
- **Hors du compte des espèces.** Ces paiements ne créent pas de mouvement de caisse : un chèque n'est pas une espèce, et le compter fausserait l'écart à la clôture. Ils sont rattachés à la session par `payment_transaction.cash_session_number` et totalisés à part dans le résumé (`nonCashPayments`).
- **Règlement direct d'une facture (`POST /billing/invoices/{n}/pay`).**
  - `MOBILE_MONEY` est refusé : il créait un succès sans PawaPay, rangé dans la caisse automatique et impossible à rembourser.
  - `cashSessionNumber` n'est plus `@NotNull` sur le record ; il reste obligatoire en espèces, contrôle fait dans le code.
- **Caisse automatique mobile money.** Ouvrir une session au besoin et y ranger le paiement existait déjà (`recordAutomaticPayment`). Deux corrections :
  - `REQUIRES_NEW` : l'appelant (`PaymentTransactionWorkflowProcessor`) attrape les erreurs, mais dans la même transaction une erreur la marquait pour annulation, et toute la confirmation (facture, réservation) échouait au commit puis était rejouée sans fin par l'outbox ;
  - `CashRegisterRepository.lockById` (`FOR UPDATE`) avant de chercher la session ouverte, pour que deux confirmations simultanées n'ouvrent pas deux sessions.
- **Relance.** Elle rouvre **la même** intention plutôt que d'en créer une : réservation, lien envoyé et écran de caisse gardent leur référence.
  - Pour une intention qui règle des factures, le montant est ramené à « déjà payé + reste dû des factures ». La relance est refusée si les factures sont soldées ou closes, ou si une autre intention est en cours pour la même source.
  - Nouvelle échéance : celle demandée, sinon la durée d'origine (bornée entre 15 min et 30 jours).
- **Annulation.** Elle est refusée pendant un dépôt mobile money en cours, car un rappel de succès arriverait sur une intention close. Elle ferme le lien de paiement (`paymentLinkExpiresAt = now`) et ne touche pas aux factures.
- **Règle d'état unique.** `PaymentIntentStates` sert à la fois au service et aux booléens `cancellable` et `relaunchable` de `PaymentIntentResponse`.

## Schéma

Migration dev `V288__paiement_monnaie_hors_plateforme_intentions.sql`, prod `V284` (contenu identique) :

- `payment_transaction` : `tendered_amount`, `change_amount`, `cash_session_number` (index partiel), `bank_name`, `payer_name`, `payer_phone` ; index `(payment_method, provider_reference)` ;
- `payment_intent` : `cancelled_at`, `cancelled_by`, `cancellation_reason`, `relaunch_count` (défaut 0), `last_relaunched_at`, `last_relaunched_by`.

## Code

| Élément | Rôle |
|---|---|
| `service/support/CashChange` | Règle de la monnaie rendue. |
| `service/support/ManualPaymentDetailsSupport` | Contrôle des champs par moyen, doublons, normalisation (`provider` = opérateur pour le mobile money hors plateforme). |
| `service/support/PaymentMethodLabels` | Libellé français du moyen, opérateur compris ; utilisé par la transaction, le reçu, la facture PDF et l'alerte de paiement libre-service. |
| `service/support/PaymentIntentStates` | États annulables et relançables. |
| `enums/OfflineMobileMoneyOperator` | `MTN_MOMO` (« MTN MoMo Pay »), `AIRTEL_MONEY` (« Airtel Money »). |
| `PaymentServiceImpl.registerCashPayment / payInvoice` | Montant remis et monnaie ; détails hors plateforme sur le règlement direct. |
| `PaymentServiceImpl.registerOfflinePayment` | `PATCH /payments/intents/{n}/offline`. |
| `PaymentServiceImpl.previewIntentCashChange / previewDocumentCashChange` | `GET .../cash-change`, lecture seule. |
| `PaymentIntentLifecycleServiceImpl` | `POST /payments/intents/{n}/cancel` et `/relaunch`. |
| `CashRegisterServiceImpl.assertCounterSessionOpen` | Session ouverte et non automatique pour rattacher un paiement hors espèces. |
| `CashSessionSummarySupport` | `nonCashPayments` via `PaymentTransactionRepository.nonCashTotalsBySession`. |
| `templates/payment/receipt.html` | Libellé, chèque, virement, banque, payeur, montant remis, monnaie rendue. |
| `BillingDocumentPdfServiceImpl.fetchPaymentInfos` | Libellé du moyen et numéro du payeur sur la facture. |

Les records `RegisterCashPaymentRequest` et `PayInvoiceRequest` gardent leur ancien constructeur (appels internes : annonce d'espèces, échéancier, prélèvement d'abonnement).

## Limites connues

- Un chèque ou un virement est tenu pour encaissé dès la saisie. Il n'y a pas d'état « en attente d'encaissement bancaire » ni de rejet de chèque ; un chèque rejeté se traite par un remboursement ou une contre-passation manuelle (`/transactions/{n}/reverse`).
- Le remboursement d'un `MOBILE_MONEY_OFFLINE`, d'un chèque ou d'un virement est enregistré sans versement réel : l'argent se rend hors plateforme.
- La monnaie rendue n'est pas proposée en crédit portefeuille ; un trop-perçu par `amount` explicite reste, comme avant, crédité au portefeuille par l'imputation sur facture.

## Tests

- `CashChangeTest` : monnaie, compte juste, paiement partiel, montant explicite, refus.
- `ManualPaymentDetailsSupportTest` : champs obligatoires par moyen, opérateur, doublons, même numéro de chèque d'une autre banque.
- `PaymentIntentLifecycleServiceImplTest` : annulation (lien fermé, dépôt en cours, états), relance (échéance, montant ramené, factures soldées, autre intention, intention échue avant le worker, déjà payée).
- `CounterAndAutomaticRegisterTest` : ouverture de la session automatique au besoin et verrou, session déjà ouverte, mobile money hors plateforme exclu, session de guichet.
- `CounterPaymentsRepositoryIT` (Postgres, migrations prod) : colonnes, recherche de doublon, totaux hors espèces d'une session.
