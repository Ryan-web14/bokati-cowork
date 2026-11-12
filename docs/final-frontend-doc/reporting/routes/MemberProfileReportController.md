# MemberProfileReportController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [reporting](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-reporting-member-profile-memberid"></a>
### GET /sni/api/v1/reporting/member-profile/{memberId}

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `MemberProfileReportResponse`
- Modèles : [MemberProfileReportResponse](#m-MemberProfileReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/MemberProfileReportController.java`, méthode `memberProfile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-reporting-member-profile-memberid-pdf"></a>
### GET /sni/api/v1/reporting/member-profile/{memberId}/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/MemberProfileReportController.java`, méthode `memberProfilePdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-reports-member-profile-memberid"></a>
### GET /sni/api/v1/reports/member-profile/{memberId}

- Accès : back-office : rôle d'administration et droit `REPORT:VIEW`
- Réponse : `MemberProfileReportResponse`
- Modèles : [MemberProfileReportResponse](#m-MemberProfileReportResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/MemberProfileReportController.java`, méthode `memberProfile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-reports-member-profile-memberid-pdf"></a>
### GET /sni/api/v1/reports/member-profile/{memberId}/pdf

- Accès : back-office : rôle d'administration et droit `REPORT:EXPORT`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/reporting/controller/MemberProfileReportController.java`, méthode `memberProfilePdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `memberId` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-MemberProfileReportResponse"></a>
#### MemberProfileReportResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `generatedAt` | date-heure ISO 8601 UTC |  |  |
| `member` | [MemberProfileReportResponse.MemberInfo](#m-MemberProfileReportResponse-MemberInfo) |  |  |
| `subscriptions` | [MemberProfileReportResponse.SubscriptionSummary](#m-MemberProfileReportResponse-SubscriptionSummary)[] |  |  |
| `bookings` | [MemberProfileReportResponse.BookingSummary](#m-MemberProfileReportResponse-BookingSummary)[] |  |  |
| `invoices` | [MemberProfileReportResponse.InvoiceSummary](#m-MemberProfileReportResponse-InvoiceSummary)[] |  |  |
| `payments` | [MemberProfileReportResponse.PaymentSummary](#m-MemberProfileReportResponse-PaymentSummary)[] |  |  |
| `wallet` | [MemberProfileReportResponse.WalletInfo](#m-MemberProfileReportResponse-WalletInfo) |  |  |
| `contracts` | [MemberProfileReportResponse.ContractSummary](#m-MemberProfileReportResponse-ContractSummary)[] |  |  |
| `tickets` | [MemberProfileReportResponse.TicketSummary](#m-MemberProfileReportResponse-TicketSummary)[] |  |  |

<a id="m-MemberProfileReportResponse-MemberInfo"></a>
#### MemberProfileReportResponse.MemberInfo

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `memberCode` | texte |  |  |
| `firstname` | texte |  |  |
| `lastname` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `whatsappPhone` | texte |  |  |
| `status` | texte |  |  |
| `customerCode` | texte |  |  |
| `customerName` | texte |  |  |
| `jobTitle` | texte |  |  |
| `companyRole` | texte |  |  |
| `birthDate` | date AAAA-MM-JJ |  |  |
| `city` | texte |  |  |
| `country` | texte |  |  |
| `photoUrl` | texte |  |  |
| `portalAccess` | booléen |  |  |
| `createdAt` | date AAAA-MM-JJ |  |  |

<a id="m-MemberProfileReportResponse-SubscriptionSummary"></a>
#### MemberProfileReportResponse.SubscriptionSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte |  |  |
| `planName` | texte |  |  |
| `status` | texte |  |  |
| `billingCycle` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `currentPeriodEnd` | date AAAA-MM-JJ |  |  |
| `autoRenew` | booléen |  |  |

<a id="m-MemberProfileReportResponse-BookingSummary"></a>
#### MemberProfileReportResponse.BookingSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `bookingNumber` | texte |  |  |
| `resourceName` | texte |  |  |
| `status` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `durationMinutes` | entier |  |  |
| `totalAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |

<a id="m-MemberProfileReportResponse-InvoiceSummary"></a>
#### MemberProfileReportResponse.InvoiceSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `documentNumber` | texte |  |  |
| `status` | texte |  |  |
| `totalAmount` | nombre décimal |  |  |
| `paidAmount` | nombre décimal |  |  |
| `balanceDue` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `issueDate` | date AAAA-MM-JJ |  |  |
| `dueDate` | date AAAA-MM-JJ |  |  |

<a id="m-MemberProfileReportResponse-PaymentSummary"></a>
#### MemberProfileReportResponse.PaymentSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `transactionNumber` | texte |  |  |
| `paymentMethod` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | texte |  |  |
| `paidAt` | date AAAA-MM-JJ |  |  |

<a id="m-MemberProfileReportResponse-WalletInfo"></a>
#### MemberProfileReportResponse.WalletInfo

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `walletNumber` | texte |  |  |
| `availableBalance` | nombre décimal |  |  |
| `ledgerBalance` | nombre décimal |  |  |
| `heldBalance` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `status` | texte |  |  |

<a id="m-MemberProfileReportResponse-ContractSummary"></a>
#### MemberProfileReportResponse.ContractSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `contractCode` | texte |  |  |
| `title` | texte |  |  |
| `status` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `renewalType` | texte |  |  |
| `signedAt` | date AAAA-MM-JJ |  |  |

<a id="m-MemberProfileReportResponse-TicketSummary"></a>
#### MemberProfileReportResponse.TicketSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `ticketNumber` | texte |  |  |
| `title` | texte |  |  |
| `status` | texte |  |  |
| `priority` | texte |  |  |
| `category` | texte |  |  |
| `createdAt` | date AAAA-MM-JJ |  |  |
| `resolvedAt` | date AAAA-MM-JJ |  |  |
