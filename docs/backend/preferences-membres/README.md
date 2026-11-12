# Préférences des membres appliquées par tous les modules

Branche `feature/parametres-a-chaud`. Décisions de l'exploitant :
- notifications activées par défaut ;
- un membre peut refuser les factures et reçus par courriel, qui restent dans le portail ;
- relances d'impayés et messages de sécurité jamais coupés.

## Constat de départ

- Le `PATCH` des préférences remettait à `false` ou `null` les champs absents.
- `notificationPref` était typé avec l'entité JPA et ignoré.
- Un couple sans ligne était renvoyé désactivé.
- Le fuseau par défaut était `Africa/Lagos`.
- `walletReceiptPreference` était stocké mais ni renvoyé ni modifiable.
- Surtout, aucun module ne lisait `user_settings` ni `notification_preferences`.

## Socle

| Élément | Rôle |
| --- | --- |
| `core/settings/userSettings/service/MemberPreferences` | Point d'entrée unique. `allows`, `allowsEmail`, `billingEmail`, `billingRecipient`, `zone`, `locale`, `walletReceipt`, `dailySpendingCap`, `weeklySpendingCap`. Sans préférence enregistrée, le comportement d'avant. Une lecture en échec ne coupe aucun envoi. |
| `MemberSettingsServiceImpl` | `PATCH` partiel : un champ absent garde sa valeur, une chaîne vide retire un texte, `0` retire un plafond. Les contrôles sont décrits plus bas. |
| `ClientSettingsController` | Portail : `GET/PATCH /client/settings` et `/client/settings/notifications` pour le membre connecté, journal avec le motif « Portail membre ». |
| `features/billing/service/support/MemberBillingContact` | Retrouve le membre derrière un destinataire (`MEMBER`, ou client personne avec un seul membre) pour la facturation et le portefeuille. |
| `features/booking/service/support/BookingMemberPreferences` | Retrouve le membre d'une réservation (titulaire, sinon l'adresse de contact) ; préférence et fuseau. |
| `features/notification/service/support/MemberNotificationEvents` | Traduit un nom d'événement en `EventType` ; tout autre nom ne se coupe jamais. |

Contrôles du `PATCH` des préférences :
- fuseau connu ;
- langue non vide ;
- plafond journalier au plus égal au plafond hebdomadaire ;
- une préférence de notification exige ses trois champs.

## Notifications

| Événement | Se coupe |
| --- | --- |
| Réservation créée, confirmée, approuvée | `BOOKING_CONFIRMED` |
| Réservation annulée | `BOOKING_CANCELLED` |
| Rappel de réservation (envoyé à l'échéance, passé à `CANCELLED` si coupé, sans échec compté) | `BOOKING_REMINDER` |
| Réservation rejetée, option expirée, échec | `BOOKING_FAILED` |
| Envoi automatique d'une facture (pas d'un devis) | `INVOICE_ISSUED` |
| Confirmation de paiement, renvoi de facture après paiement, encaissement espèces confirmé, reçu de commande marketplace | `PAYMENT_RECEIVED` |
| Dépôt mobile money échoué | `PAYMENT_FAILED` |

Le canal `IN_APP` coupe la notification dans l'application (`MemberInAppNotifier`).

Ne se coupent jamais :
- relances d'impayés (`DunningRunner`, `BillingOverdueWorker`, `BillingPaymentReminderWorker`) et rappels d'échéance ;
- dépôt en vérification, expiration d'une annonce d'espèces ;
- signature de devis, contrats, KYC, conformité ;
- sécurité (codes, mots de passe, connexions, alertes du portefeuille), solde bas ;
- messages à l'administration ou à une organisation ;
- courriels d'abonnement et de pass (aucun `EventType` ne les couvre).

Un envoi demandé par un administrateur (`BillingEmailService.sendDocument`, bouton « renvoyer ») part toujours. L'envoi automatique passe par `sendDocumentIfAllowed`. Un envoi coupé est journalisé en info (« preference du membre »), sans erreur.

## Adresse de facturation

L'adresse `billingEmail` des préférences du membre reçoit ce qui suit :
- les factures (adresse figée par `BillingCustomerSnapshotResolver` et adresse d'envoi) ;
- les reçus (`PaymentReceiptServiceImpl`) et les remboursements ;
- les messages d'encaissement espèces ;
- les courriels d'abonnement et de pass ;
- les reçus du portefeuille ;
- dans l'outbox, les messages `OVERAGE_BILLED`, `PAYMENT_DUE` et `WALLET_CREDIT_APPLIED` (`OutboxRecipientResolver.resolveBillingByOwnerCode` et `resolveBillingByOwnerId`).

Règles :
- Elle remplace l'adresse du compte (celle du membre, ou l'`email` / `billingEmail` du client personne).
- Une adresse différente saisie à la main sur un document est gardée.
- Une organisation garde son propre courriel de facturation.
- Les listes d'adresses séparées par des virgules sont gardées telles quelles.
- `resolveParty` et `resolveByOwnerCode` sont inchangés : ils servent aux contrats, au KYC, à la conformité et à la sécurité.

## Fuseau

Les dates et heures sont données dans le fuseau du membre :
- courriels de réservation, et heure du corps du rappel ;
- reçus de paiement (`PaymentReceiptTemplateFormatter` reçoit un `ZoneId`) ;
- remboursements ;
- dates de validité des pass.

Les heures de réservation sont lues dans le fuseau de l'espace (`Africa/Brazzaville`) puis converties. Sans préférence, le rendu ne change pas.

## Portefeuille

- `WalletLimitService.assertWithinPersonalCaps` contrôle les plafonds personnels :
  - il cumule `PAYMENT`, `TRANSFER_OUT` et `TRANSFER_FEE` ;
  - le jour se compte dans le fuseau du membre, la semaine sur 7 jours glissants ;
  - un dépassement répond `400`, « Plafond personnel journalier atteint » ou « hebdomadaire » ;
  - il s'ajoute aux limites du palier.
- Il est appelé :
  - par `WalletServiceImpl.debit` pour un paiement ;
  - par `WalletTransferService` : simulation (affichée comme blocage), initiation, exécution.
- Ne sont pas concernés : les opérations d'administration, les crédits, la capture d'un blocage déjà engagé.
- `receiptPreference = INVOICE_ONLY` : pas de courriel pour les transferts envoyés, transferts reçus et rechargements. Le solde bas, la sécurité et les demandes de paiement partent toujours.
- Les plafonds sont dans la devise du portefeuille, sans conversion.

## Non appliqué

- Rechargement automatique (`autoTopup*`) et options d'affichage `show*` : enregistrés, sans module qui les utilise.
- Langue : formatage seulement, les messages restent en français.

## Tests

- `MemberSettingsPatchTest`, `MemberPreferencesTest`.
- `MemberNotificationEventsTest`, `MemberInAppNotifierPreferenceTest`, `BookingMemberPreferenceNotifyTest`, `BookingReminderPreferenceTest`, `MobileMoneyClientNotifierPreferenceTest`, `OutboxRecipientResolverBillingTest`.
- `MemberBillingContactTest`, `BillingSnapshotBillingEmailTest`, `BillingEmailPreferencesTest`, `WalletPersonalCapsTest`, `WalletNotifierPreferencesTest`.

Suite unitaire complète : 2119 tests, aucun échec.

## Doc frontend

`docs/final-frontend-doc/parametres/` : règles du `PATCH`, routes du portail, effet des préférences, écarts réglés.
