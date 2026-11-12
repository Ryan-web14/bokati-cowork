# Réservation multi-ressources

Réserver en une fois plusieurs ressources pour un ou plusieurs membres, avec une seule facture par membre. Back-office seulement. Doc frontend : [docs/final-frontend-doc/booking/reservation-multi-ressources.md](../../final-frontend-doc/booking/reservation-multi-ressources.md).

## Décisions

- Forme : des lignes (ressource et créneau) et des membres par défaut ; une ligne peut porter ses propres membres. Chaque couple ligne et membre est une réservation.
- Tout ou rien : une seule réservation impossible refuse le lot entier (`ValidationException`, liste dans `errors`).
- Back-office seulement, pas de portail.
- `WALLET` accepté : chaque réservation bloque son montant ; au règlement du blocage, `BookingWalletSettlementSupport` retrouve la facture commune (par `billable_item.invoice_id`) et ne crée l'intention de paiement que pour `min(montant du blocage, reste dû)`. Une facture seule garde le règlement du solde entier. Le portefeuille d'organisation limitait déjà le débit au montant de la retenue.
- Approbation : une réservation `PENDING_APPROVAL` du lot reçoit son élément facturable à la création du lot et entre dans la facture commune. `approve` le reprend (pas de second élément) et confirme aussitôt si `BookingBillableBridge.invoiceSettled` ; `reject` appelle `BookingCancellationRefundSupport.processRejection` : avoir de la part, remboursement de ce qui dépasse le nouveau total, sans politique d'annulation.

## Schéma

Migration dev `V287__booking_lot_multi_ressources.sql`, prod `V283` (contenu identique) :

- table `booking_batch` (numéro `BKL-AAAAMM-NNNNNN`, clé d'idempotence, mode, organisation, compteurs, notes, auteur) ;
- colonne `booking.batch_number` et son index ;
- séquence `booking_batch` (id 2870001).

## Code

| Élément | Rôle |
|---|---|
| `booking/model/BookingBatch`, `repository/BookingBatchRepository` | Le lot. |
| `BookingRepository.findAllByBatchNumber` | Réservations d'un lot, ressource chargée (pas d'open-in-view). |
| `service/implementation/BookingBatchServiceImpl` | `plan` (couples ligne et membre, erreurs de forme), `create`, `get`, `list`. |
| `BookingBillableBridge.beginDeferredInvoicing / endDeferredInvoicing / invoiceGroup` | Pendant le lot, l'élément facturable de chaque réservation est créé mais mis de côté (ThreadLocal) au lieu d'être facturé seul ; le lot les regroupe ensuite par titulaire, organisation payeuse et devise, et appelle `BillableItemInvoiceSupport.ensureInvoicedGroup` (déjà utilisé par la marketplace). |
| `BookingController` | `POST /bookings/batches`, `GET /bookings/batches`, `GET /bookings/batches/{batchNumber}`. |
| `BookingResponse.batchNumber` | Le lot d'une réservation, `null` hors lot. |

Création : `BookingBatchServiceImpl.create` appelle `BookingService.create` (proxy, même transaction) pour chaque couple. Une `BaseException` est notée et la boucle continue, pour rendre toutes les erreurs d'un coup ; la transaction est déjà marquée pour annulation, et le `ValidationException` final annule tout. Toute autre exception remonte telle quelle (annulation aussi).

## Facture commune : ce qui a dû suivre

Une facture issue de plusieurs éléments a pour source `BILLABLE_ITEM` et pour code `BI-1,BI-2,...`. Les chemins qui remontaient d'une facture à **sa** réservation par recherche exacte ne la trouvaient pas :

| Chemin | Avant | Maintenant |
|---|---|---|
| Paiement complet (`PaymentTransactionWorkflowProcessor`) | `TransactionContextResolver` rendait `MULTIPLE`, rien n'était confirmé | Toutes les lignes de la facture sont des réservations : source `BOOKINGS` (code : numéros séparés par des virgules), chacune est confirmée |
| Remboursement total | rien | chaque réservation est annulée (`systemCancel`) |
| Annulation de la facture (`BillingDocumentCancellationOutboxEventProcessor`) | une seule réservation cherchée par le code entier, donc aucune | chaque élément du code est suivi jusqu'à sa réservation |
| Annulation d'une réservation (`BookingCancellationRefundSupport`) | facture non trouvée, restait due en entier | facture retrouvée par `billable_item.invoice_id` ; rien d'encaissé : avoir de la part de la réservation, ou annulation de la facture si c'était la dernière ; encaissé : politique d'annulation sur la part de la réservation seulement |

Les réservations seules ne changent pas de chemin (la recherche exacte les trouve toujours en premier).

## Limites connues

- Le reste de la facture commune n'est pas recalculé si une réservation change de prix après coup (déplacement, changement de ressource) ; c'était déjà vrai d'une facture seule.
- Une annonce d'espèces (`CashDeclarationExpiryWorker`) vise une réservation précise ; pas étendue au lot.

## Tests

- `BookingBatchServiceImplTest` : une facture par membre, membres de ligne, ligne sans membre, tout ou rien, portefeuille accepté, lot trop grand, réservation soumise à approbation facturée avec le lot, rejeu idempotent.
- `BookingWalletSettlementSupportTest.debitsOnlyTheBookingShareOfABatchInvoice` : le débit au portefeuille se limite à la part de la réservation.
- `BookingBillableBridgeDeferredTest` : mise de côté en lot, facture immédiate hors lot.
- `BookingBatchCancellationTest` : avoir de la part, annulation de la dernière, politique sur la part, refus remboursé en entier, refus avec règlement partiel.
- `PaymentTransactionWorkflowProcessorTest` : confirmation et annulation de toutes les réservations d'une facture de lot.
- `BookingBatchRepositoryIT` (Postgres, migrations prod) : table, colonne, séquence, requêtes.
