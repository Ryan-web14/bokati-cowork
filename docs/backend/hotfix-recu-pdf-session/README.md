# Correctif : impression du reçu de paiement (erreur 500)

Symptôme : `GET /sni/api/v1/payments/transactions/{transactionNumber}/receipt/pdf` (et `GET /payments/receipts/{receiptNumber}/pdf`) répondait 500 avec `LazyInitializationException: Could not initialize proxy [PaymentIntent#...] - no session`.

## Cause

L'impression de l'administration (original puis duplicata dans un même PDF) était une méthode par défaut de l'interface `PaymentReceiptService`. Le `@Transactional` posé sur la classe `PaymentReceiptServiceImpl` ne couvre que les méthodes déclarées dans la classe : la méthode par défaut s'exécutait sans transaction, appelait le rendu du reçu en interne (sans repasser par le proxy Spring), et la lecture de l'intention de paiement, chargée à la demande, échouait faute de session.

## Changement

- `PaymentReceiptService` : `generateAdminPdfByTransactionNumber` et `generateAdminPdfByReceiptNumber` n'ont plus de corps.
- `PaymentReceiptServiceImpl` : les deux méthodes y sont déclarées, donc exécutées dans la transaction de la classe. Le contenu du PDF ne change pas.
- La facture (`BillingDocumentPdfServiceImpl.generateAdminPdf`) était déjà déclarée dans sa classe, avec `@Transactional` : rien à corriger.
- Aucune migration, aucun réglage.

## Tests

- `PaymentReceiptTransactionTest` : les deux méthodes sont déclarées dans la classe transactionnelle et l'interface n'a plus de méthode par défaut.

## Pour le frontend

Rien à changer : les mêmes adresses renvoient de nouveau le PDF.
