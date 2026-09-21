# Courriels des lots portefeuille, domiciliation, abonnements et relances

Les services de ces lots publient des événements dans la boîte de sortie avec `recipientEmail`
(ou `adminEmail`), `subject`, `templateCode` et leurs variables. `NotificationOutboxEventProcessor`
les relaie au module notification, qui résout le gabarit dans `notification_template` par
`template_code` (insensible à la casse) et rend le fichier Thymeleaf `templates/email/<template_name>.html`.
Sans ligne, c'est `generic-notification` qui part · lisible, mais muet sur les faits.

Migration `V243` (dev) / `V239` (prod) : une ligne par code d'événement. Les aggrégats
`DOMICILIATION` et `SUBSCRIPTION_QUOTE` ont été ajoutés aux aggrégats relayés ; sans cela, les
courriels de domiciliation et de devis restaient dans la boîte de sortie sans jamais partir.

Tous les gabarits partagent `email/fragments/notice-layout.html` (styles, en-tête, pied, mention
légale) et ne diffèrent que par leur corps. `LotEmailTemplatesRenderTest` rend chacun avec la
charge utile réellement émise.

| Événement | Gabarit | Variables citées |
|---|---|---|
| `WALLET_TRANSFER_SENT` | `wallet-transfer-sent` | `amount`, `currency`, `counterpartyName`, `counterpartyWallet`, `walletNumber`, `availableBalance`, `aggregateId` |
| `WALLET_TRANSFER_RECEIVED` | `wallet-transfer-received` | idem |
| `WALLET_TOPUP_COMPLETED` | `wallet-topup-completed` | `amount`, `currency`, `walletNumber`, `availableBalance`, `aggregateId` |
| `WALLET_PAYMENT_REQUEST_RECEIVED` | `wallet-payment-request` | `amount`, `reason`, `counterpartyName`, `aggregateId` |
| `WALLET_LOW_BALANCE` | `wallet-low-balance` | `amount` (solde), `threshold` |
| `WALLET_FROZEN`, `WALLET_SUSPENDED`, `WALLET_UNSUSPENDED`, `WALLET_CLOSED` | `wallet-status-changed` (bascule sur `eventType`) | `reason`, `actionNumber`, `freezeNumber` |
| `WALLET_NEW_DEVICE`, `WALLET_REVOKED_DEVICE_USED`, `WALLET_LOCKED_BY_OWNER`, `WALLET_UNLOCKED_BY_OWNER` | `wallet-security-event` (bascule) | `deviceId`, `ipAddress` |
| `WALLET_KYC_DOCUMENTS_REQUESTED` | `wallet-kyc-documents-requested` | `reason`, `missingDocuments`, `kycCaseCode` |
| `WALLET_TREASURY_COVERAGE_ALERT` (admin) | `wallet-treasury-alert` | `reconciliationNumber`, `totalWalletBalance`, `availableCash`, `coverageRatio` |
| `DOMICILIATION_ACTIVATED` | `domiciliation-activated` | `legalName`, `contractNumber`, `administrationReference` |
| `DOMICILIATION_REGISTRATION_REJECTED` | `domiciliation-registration-rejected` | `reason` |
| `DOMICILIATION_CERTIFICATE_ISSUED` | `domiciliation-certificate-issued` | `scope`, `validUntil`, `documentCode` |
| `DOMICILIATION_CERTIFICATE_EXPIRING` | `domiciliation-certificate-expiring` | `scope`, `validUntil` |
| `DOMICILIATION_FISCAL_LOST` | `domiciliation-fiscal-lost` | `commitmentMonths` |
| `DOMICILIATION_TERMINATED` | `domiciliation-terminated` | `effectiveDate`, `reason` |
| `DOMICILIATION_FISCAL_LOST_ADMIN`, `DOMICILIATION_TERMINATED_ADMIN` (admin) | `domiciliation-administration-notice` (bascule) | `legalName`, `contractNumber`, `administrationReference` |
| `MAIL_RECEIVED`, `MAIL_SCANNED`, `MAIL_FORWARDED`, `MAIL_STORAGE_OVERDUE` | `mail-item-event` (bascule) | `itemNumber`, `mailType`, `senderName`, `contractNumber` |
| `SUBSCRIPTION_GRACE_PERIOD` | `subscription-grace-period` | `subscriptionNumber`, `daysBeforeSuspension` |
| `SUBSCRIPTION_TERMINATION_REQUESTED`, `_ACCEPTED`, `_COMPLETED` | `subscription-termination` (bascule) | `terminationCode`, `effectiveDate`, `feeAmount` |
| `SUBSCRIPTION_DIRECT_DEBIT_FAILED` | `subscription-direct-debit-failed` | `invoiceNumber`, `amount`, `currency`, `status`, `mandateSuspended` |
| `SUBSCRIPTION_QUOTE_SENT` | `subscription-quote-sent` | `quoteNumber`, `planName`, `quotedPrice`, `currency`, `billingCycle`, `commitmentMonths`, `validUntil` |
| `BILLING_DUNNING_REMINDER`, `_FORMAL_NOTICE`, `_GRACE_PERIOD`, `_SUSPEND` | `billing-dunning` (bascule) | `message` (rendu depuis le palier), `documentNumber`, `balanceDue`, `dueDate`, `daysOverdue` |
| `BILLING_DUNNING_HANDOVER` (admin) | `billing-dunning-handover` | `message`, `customerName`, `customerCode`, `balanceDue`, `daysOverdue` |

Le sujet porté par la charge utile prime sur celui de la table ; celui de la table sert quand
le service n'en met pas. `recipientName`, `eventType`, `subject`, `aggregateId` sont toujours
disponibles au gabarit. Les alertes de pass (`EXPIRING`, `LOW_BALANCE`, `UNUSED`) passent par le
module de notifications d'abonnement, pas par ce chemin.
