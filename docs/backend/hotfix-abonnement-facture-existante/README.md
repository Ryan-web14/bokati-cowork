# Correctif : abonnement à prix nul par un administrateur, facture existante rattachée

Demande : « permettre la réduction à 0 par l'admin avec avertissement mais non bloquant, c'est dans le cas où la facturation a été faite mais pas d'abo ; rajouter aussi une fonctionnalité qui peut lier un paiement ou une facture à la création d'un abo et sa validation ».

Symptôme : `POST /sni/api/v1/subscriptions/quotes` avec `quotedPrice: 0` répondait `409` « Rabais de 100 % au-delà du maximum admis (50 %) ».

Branche `hotfix/abonnement-facture-existante` (depuis `hotfix/emails-facturation-niu`). Aucune migration : la dérogation est gardée dans les notes du devis et dans le motif de la dérivation, le rattachement dans les métadonnées de l'abonnement.

## 1. Dérogation administrateur sur le prix d'un devis

- `PlanDerivationService.overridable(motif)` : seuls les blocages de prix sont levables, « Prix plancher ... » et « Rabais de ... au-delà du maximum admis ». « Rien ne diffère du catalogue » et les autres restent bloquants.
- `SubscriptionQuoteService.create(request, preparedBy, adminOverride)` : pour un administrateur (`ROLE_ADMIN` ou `ROLE_SUPER_ADMIN`, lu par `SubscriptionQuoteController`), un blocage de prix n'empêche plus la création ; la ligne `[Dérogation administrateur par <auteur>] <motif>` est ajoutée aux notes du devis et journalisée. Un membre du personnel (`STAFF`) reste bloqué.
- Acceptation : la dérogation suit le devis (repérée dans ses notes), même si un membre du personnel l'accepte ; un administrateur qui accepte peut aussi l'accorder. La dérivation est créée avec `PlanDerivationService.create(..., adminOverride = true)`, son motif détaillé porte « Dérogation administrateur : <motif> ».
- Avertissements non bloquants (`QuoteView.warnings`, sur la création, la lecture et l'acceptation) : la dérogation accordée ; pour un prix nul, le rappel que les renouvellements resteront à zéro tant que la dérivation est active.

## 2. Facture ou paiement existants rattachés à l'abonnement

`SubscriptionBillingLinkService` :

- **Contrôles** : la facture existe, est une facture (`INVOICE`, `CORRECTIVE_INVOICE`, `DEBIT_NOTE`), n'est ni brouillon ni annulée, est au nom du souscripteur (`customerCode` ou `companyCustomerCode`), n'est pas déjà rattachée à un autre abonnement ; un paiement est `SUCCEEDED`, au nom du souscripteur, et imputé sur la facture (seul, il désigne la facture qu'il règle, s'il n'en règle qu'une).
- **Avertissements** : facture non soldée, facture plus faible que la première période.
- **À la création** (`CreateSubscriptionRequest.existingInvoiceNumber`, `existingPaymentNumber`, aussi passés par l'acceptation d'un devis) : la première facture n'est pas émise ; le rattachement est inscrit dans `metadataJson` (`linkedInvoiceNumber`, `linkedPaymentNumber`, `linkedBy`) et dans l'historique des statuts ; l'abonnement est activé si la facture est soldée.
- **Après coup** : `POST /sni/api/v1/subscriptions/{n}/activate-with-billing` (`SubscriptionBillingLinkController`, droit `SUBSCRIPTION:ACTIVATE` puisque la route est sous `/activate`). La facture initiale de l'abonnement, ni réglée ni scellée, est annulée (`cancelAndArchive`) ; l'abonnement est validé si la facture rattachée est soldée, ou par un administrateur avec `force`.
- **Validation au paiement** : `PaymentTransactionWorkflowProcessor` active l'abonnement en attente dont la facture rattachée vient d'être soldée par un paiement (`activateIfLinkedInvoicePaid`), quel que soit le type de l'intention de paiement.
- `SubscriptionRepository.findNumbersLinkedToInvoice` lit `metadata_json ->> 'linkedInvoiceNumber'` ; `BillableItemRepository.findAllBySourceId`.

## Tests

- `SubscriptionQuoteServiceTest` : un administrateur crée un devis à prix nul (dérogation dans les notes, avertissements), un membre du personnel reste bloqué ; un blocage autre que de prix n'est pas levable ; un devis dérogé est accepté par un membre du personnel avec la facture existante passée à la création.
- `SubscriptionBillingLinkServiceTest` : facture soldée rattachée ; refus d'une facture d'un autre client, déjà rattachée ou annulée ; avertissements d'une facture non soldée ou plus faible ; un paiement seul désigne sa facture et doit être réussi ; sur un abonnement en attente, la facture initiale impayée est annulée et l'abonnement validé ; facture non soldée : attente, sauf validation forcée par un administrateur.

## Pour le frontend

Voir [parcours-souscription.md](../../final-frontend-doc/subscription/parcours-souscription.md#facturation-déjà-faite-hors-de-labonnement) : bandeau des avertissements du devis, champs « Facture déjà émise » et « Paiement déjà reçu » à la création et à l'acceptation, bouton « Rattacher une facture existante » sur un abonnement en attente.
