# Rattachement des paiements et des factures aux éléments

Branche `feature/rattachement-paiements`, partie de new-main 66256786 le 14/11/2026. Migrations à partir de dev `V357`, prod `V351` (dev V355, V356 et prod V349, V350 sont prises par `feature/modeles-documents`).

## Besoin

L'équipe doit pouvoir rattacher un paiement ou une facture à n'importe quel élément facturé : abonnement classique, domiciliation, réservation, pass. Le rattachement doit produire les mêmes effets qu'un paiement fait directement sur l'élément.

## État avant ce chantier

- L'activation d'un abonnement ou d'un pass et la confirmation d'une réservation ne partent que de l'événement de fin de paiement (`PAYMENT_TRANSACTION_WORKFLOW`, `PaymentTransactionWorkflowProcessor`), selon la source de l'intention de paiement.
- L'affectation tardive (`POST /billing/unallocated-payments/{tx}/allocate`, `UnallocatedPaymentService`) solde la facture et passe D 4719 / C 411 (`SalesAccountingService.feedLateAllocation`). Elle ne déclenche rien d'autre.
- Aucun événement n'est émis quand une facture passe à « payée » (`BillingDocumentServiceImpl.applyPayment`).
- Liens facture-élément :
  - abonnement : `billable_item.source_id` (types `SUBSCRIPTION_RECURRING`, `SUBSCRIPTION_SETUP_FEE`, `DEPOSIT`, `SUBSCRIPTION_ADDON`), plus une facture rattachée à la main (`subscription.metadata_json->>'linkedInvoiceNumber'`, `SubscriptionBillingLinkService`) ;
  - réservation : `booking.billableNumber`, puis `billable_item` de source `BOOKING` ;
  - domiciliation : pas de facture propre, facturée par son abonnement parent (`DomiciliationContract.subscription_id`). Son contrat devient actif par la confirmation d'immatriculation de l'équipe (`DomiciliationService.confirmRegistration`).

## Décisions de l'exploitant (14/11/2026)

| Sujet | Décision |
| --- | --- |
| Domiciliation | Le paiement active l'abonnement parent. Le contrat reste en attente de la confirmation d'immatriculation par l'équipe. |
| Facture propre de l'élément quand on lui rattache une autre facture | Annulée automatiquement : supprimée si brouillon, annulée par avoir si émise et certifiée (comme le rattachement d'abonnement actuel). |
| Activation après un paiement partiel | Au choix de l'administrateur à chaque rattachement, selon ce qui a été convenu avec le client : au solde de toutes les factures de l'élément, ou dès le premier paiement. |
| Annulation d'une affectation | Toujours possible. Le paiement revient en somme en attente (contrepassation 411 vers 4719). |
| Facture certifiée après annulation d'une affectation | Au choix de l'administrateur : rouverte et toujours due, ou neutralisée par une facture rectificative. |
| Élément déjà activé quand on annule l'affectation | Retour arrière : l'élément repasse en attente (ou suspendu), ses droits sont retirés, le contrat généré est annulé ; historique et pointages restent. Refusé si l'élément a déjà été consommé (réservation commencée, droits utilisés), avec le motif. |
| Recherche des éléments | Par le client, l'entreprise, le membre rattaché ou le client externe (sans compte, saisi au comptoir), par nom, courriel, téléphone ou code. |

## Gestes

- **A. Depuis un paiement, vers une facture** (par son numéro) : la route existante déclenche désormais les effets sur les éléments que la facture règle.
- **B. Depuis un paiement, vers un élément** (type et code) : le serveur trouve les factures ouvertes de l'élément et y affecte le paiement, échéance la plus ancienne d'abord. Le reliquat reste en attente. Les effets suivent.
- **C. Depuis une facture, vers un élément** : le lien est enregistré. Si la facture est déjà payée, les effets partent tout de suite, sinon au paiement, quel que soit le moyen. La facture propre impayée de l'élément est annulée.
- **D. Annuler une affectation** : voir les décisions.
- **Recherche** : un tiers (membre, client, client externe, entreprise, contact de réservation) donne ses éléments rattachables et leurs factures ouvertes.

## Moteur d'effets

Chaque facture soldée, et chaque premier paiement quand l'administrateur a choisi l'activation dès le premier paiement, publie par la boîte de sortie un événement de règlement des éléments. Son traitement applique les effets d'aujourd'hui aux éléments réglés par la facture :
- abonnement : renouvellement payé, sortie de période de grâce, réalignement, ou activation s'il attendait ;
- pass : activation ;
- réservation : confirmation si elle attendait son paiement ;
- domiciliation : effets de l'abonnement parent seulement.

Ensuite, il envoie la facture payée par courriel et WhatsApp. Un rejeu ne refait rien : un élément déjà actif ou confirmé est laissé tel quel. Une erreur des effets n'annule pas le paiement.

## Contrôles

- Le paiement est réussi et non remboursé. On n'affecte jamais plus que le reste du paiement, ni au-delà du reste dû d'une facture.
- Le payeur est le client de la facture, ou l'entreprise qui paie pour lui (facturation sur compte).
- L'élément n'est ni annulé ni terminé. Une réservation passée reste rattachable.
- Chaque geste est audité.

## Routes prévues

| Route | Rôle |
| --- | --- |
| `GET /billing/elements/search?query=` | Tiers trouvés (membre, client, client externe, entreprise, contact de réservation) avec leurs éléments rattachables |
| `GET /billing/elements/{type}/{code}/invoices` | Factures d'un élément et leur reste dû |
| `POST /billing/unallocated-payments/{tx}/allocate` | Existante, le corps accepte aussi une cible `{ "type", "code" }` et le mode d'activation |
| `GET /payments/transactions/{tx}/allocations` | Affectations d'un paiement |
| `POST /billing/allocations/{id}/cancel` | Annulation d'une affectation, avec le sort de la facture |
| `POST /billing/documents/{n}/elements`, `DELETE .../elements/{type}/{code}`, `GET .../elements` | Rattacher une facture à un élément, défaire le lien, les lire |

## Lots

| Lot | Contenu | État |
| --- | --- | --- |
| R1 | Table des liens facture-élément, résolution des éléments d'une facture et des factures d'un élément, recherche par tiers | À faire |
| R2 | Moteur d'effets par la boîte de sortie, branché sur l'affectation tardive (geste A) | À faire |
| R3 | Gestes B et C, mode d'activation choisi par l'administrateur | À faire |
| R4 | Annulation d'une affectation et retour arrière (geste D) | À faire |
| R5 | Doc frontend finale et routes régénérées | À faire |
