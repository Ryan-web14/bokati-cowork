# ClientDomiciliationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [domiciliation](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-client-companies-customerid-domiciliation"></a>
### GET /sni/api/v1/client/companies/{customerId}/domiciliation

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<DomiciliationDtos.ContractView>`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/ClientDomiciliationController.java`, méthode `companyDomiciliation`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-domiciliation-contracts"></a>
### GET /sni/api/v1/client/domiciliation/contracts

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<DomiciliationDtos.ContractView>`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/ClientDomiciliationController.java`, méthode `contracts`

<a id="r-get-sni-api-v1-client-domiciliation-contracts-contractnumber"></a>
### GET /sni/api/v1/client/domiciliation/contracts/{contractNumber}

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `DomiciliationDtos.ContractView`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/ClientDomiciliationController.java`, méthode `contract`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-domiciliation-contracts-contractnumber-certificate"></a>
### GET /sni/api/v1/client/domiciliation/contracts/{contractNumber}/certificate

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/ClientDomiciliationController.java`, méthode `certificate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-domiciliation-contracts-contractnumber-document"></a>
### GET /sni/api/v1/client/domiciliation/contracts/{contractNumber}/document

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/ClientDomiciliationController.java`, méthode `contractDocument`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-domiciliation-contracts-contractnumber-mail"></a>
### GET /sni/api/v1/client/domiciliation/contracts/{contractNumber}/mail

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `PaginatedResponse<DomiciliationDtos.MailItemView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/ClientDomiciliationController.java`, méthode `mail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-client-domiciliation-contracts-contractnumber-mail-itemnumber-trail"></a>
### GET /sni/api/v1/client/domiciliation/contracts/{contractNumber}/mail/{itemNumber}/trail

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `List<DomiciliationDtos.MailEventView>`
- Modèles : [DomiciliationDtos.MailEventView](#m-DomiciliationDtos-MailEventView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/ClientDomiciliationController.java`, méthode `mailTrail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |
| `itemNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DomiciliationDtos-ContractView"></a>
#### DomiciliationDtos.ContractView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `contractNumber` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `serviceNumber` | texte |  |  |
| `legalName` | texte |  |  |
| `legalForm` | texte |  |  |
| `registrationNumber` | texte |  |  |
| `taxNumber` | texte |  |  |
| `legalRepresentativeName` | texte |  |  |
| `addressCode` | texte |  |  |
| `addressLabel` | texte |  |  |
| `suiteNumber` | texte |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `commitmentMonths` | entier |  |  |
| `fiscalAddressEligible` | booléen |  |  |
| `fiscalAddressGrantedAt` | date-heure ISO 8601 UTC |  |  |
| `fiscalAddressRevokedAt` | date-heure ISO 8601 UTC |  |  |
| `fiscalAddressRevocationReason` | texte |  |  |
| `status` | enum : `DRAFT`, `PENDING_DOCUMENTS`, `PENDING_SIGNATURE`, `PENDING_REGISTRATION`, `ACTIVE`, `SUSPENDED`, `TERMINATED`, `EXPIRED` |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `noticePeriodDays` | entier |  |  |
| `contractDocumentCode` | texte |  |  |
| `signedDocumentCode` | texte |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `certificateDocumentCode` | texte |  |  |
| `certificateScope` | enum : `COMMERCIAL`, `FISCAL` |  |  |
| `certificateIssuedAt` | date-heure ISO 8601 UTC |  |  |
| `certificateValidUntil` | date AAAA-MM-JJ |  |  |
| `certificateInForce` | booléen |  |  |
| `certificateRevokedAt` | date-heure ISO 8601 UTC |  |  |
| `certificateRevocationReason` | texte |  |  |
| `mailForwardingMode` | enum : `HOLD`, `FORWARD`, `SCAN_AND_FORWARD`, `SCAN_ONLY` |  |  |
| `terminatedAt` | date-heure ISO 8601 UTC |  |  |
| `terminationReason` | texte |  |  |
| `retainDocumentsUntil` | date AAAA-MM-JJ |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DomiciliationDtos-MailItemView"></a>
#### DomiciliationDtos.MailItemView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `itemNumber` | texte |  |  |
| `contractNumber` | texte |  |  |
| `mailType` | enum : `LETTER`, `REGISTERED_LETTER`, `PARCEL`, `ADMINISTRATIVE`, `LEGAL_NOTICE`, `OTHER` |  |  |
| `senderName` | texte |  |  |
| `senderReference` | texte |  |  |
| `receivedAt` | date-heure ISO 8601 UTC |  |  |
| `receivedBy` | texte |  |  |
| `weightGrams` | entier |  |  |
| `dimensions` | texte |  |  |
| `status` | enum : `RECEIVED`, `NOTIFIED`, `SCANNED`, `COLLECTED`, `FORWARDED`, `RETURNED`, `DESTROYED` |  |  |
| `scanDocumentCode` | texte |  |  |
| `notifiedAt` | date-heure ISO 8601 UTC |  |  |
| `collectedAt` | date-heure ISO 8601 UTC |  |  |
| `collectedBy` | texte |  |  |
| `collectorIdDocument` | texte |  |  |
| `handedOverBy` | texte |  |  |
| `forwardedAt` | date-heure ISO 8601 UTC |  |  |
| `forwardingTrackingNumber` | texte |  |  |
| `forwardingCost` | nombre décimal |  |  |
| `storageDeadline` | date AAAA-MM-JJ |  |  |
| `billable` | booléen |  |  |
| `billedAmount` | nombre décimal |  |  |
| `notes` | texte |  |  |
| `collectedByMember` | texte |  |  |

<a id="m-DomiciliationDtos-MailEventView"></a>
#### DomiciliationDtos.MailEventView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `eventType` | texte |  |  |
| `actor` | texte |  |  |
| `details` | texte |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |
