# CompanyContractsController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [company](../README.md) · 4 route(s).

## Routes

<a id="r-get-sni-api-v1-client-companies-customerid-contracts"></a>
### GET /sni/api/v1/client/companies/{customerId}/contracts

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `CompanyContractsService.CompanyContracts`
- Modèles : [CompanyContractsService.CompanyContracts](#m-CompanyContractsService-CompanyContracts)
- Code : `src/main/java/com/sni/bokaticowork/features/company/contracts/CompanyContractsController.java`, méthode `ownerList`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-client-companies-customerid-contracts-contractcode-pdf"></a>
### GET /sni/api/v1/client/companies/{customerId}/contracts/{contractCode}/pdf

- Accès : membre connecté au portail (`ROLE_MEMBER` et accès portail activé)
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/company/contracts/CompanyContractsController.java`, méthode `ownerPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-companies-customerid-contracts"></a>
### GET /sni/api/v1/companies/{customerId}/contracts

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `CompanyContractsService.CompanyContracts`
- Modèles : [CompanyContractsService.CompanyContracts](#m-CompanyContractsService-CompanyContracts)
- Code : `src/main/java/com/sni/bokaticowork/features/company/contracts/CompanyContractsController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-companies-customerid-contracts-contractcode-pdf"></a>
### GET /sni/api/v1/companies/{customerId}/contracts/{contractCode}/pdf

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/company/contracts/CompanyContractsController.java`, méthode `pdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `customerId` | chemin | `String` | oui |  |
| `contractCode` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CompanyContractsService-CompanyContracts"></a>
#### CompanyContractsService.CompanyContracts

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `companyCustomerCode` | texte |  |  |
| `signatories` | [CompanyContractsService.SignatoryView](#m-CompanyContractsService-SignatoryView)[] |  |  |
| `contracts` | [CompanyContractsService.ContractView](#m-CompanyContractsService-ContractView)[] |  |  |
| `domiciliations` | [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView)[] |  |  |

<a id="m-CompanyContractsService-SignatoryView"></a>
#### CompanyContractsService.SignatoryView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `title` | texte |  |  |
| `email` | texte |  |  |
| `memberId` | texte |  |  |
| `legalRepresentative` | booléen |  |  |

<a id="m-CompanyContractsService-ContractView"></a>
#### CompanyContractsService.ContractView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `contractCode` | texte |  |  |
| `title` | texte |  |  |
| `templateCode` | texte |  |  |
| `status` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `hasDocument` | booléen |  |  |
| `parties` | [CompanyContractsService.PartyView](#m-CompanyContractsService-PartyView)[] |  |  |
| `amendments` | [CompanyContractsService.AmendmentView](#m-CompanyContractsService-AmendmentView)[] |  |  |
| `versions` | [ContractVersionHistory.Version](#m-ContractVersionHistory-Version)[] |  |  |

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

<a id="m-CompanyContractsService-PartyView"></a>
#### CompanyContractsService.PartyView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `role` | texte |  |  |
| `partyType` | texte |  |  |
| `partyCode` | texte |  |  |
| `displayName` | texte |  |  |
| `mustSign` | booléen |  |  |

<a id="m-CompanyContractsService-AmendmentView"></a>
#### CompanyContractsService.AmendmentView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `status` | texte |  |  |
| `description` | texte |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `hasDocument` | booléen |  |  |

<a id="m-ContractVersionHistory-Version"></a>
#### ContractVersionHistory.Version

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `version` | entier |  |  |
| `kind` | enum : `ORIGINAL`, `AMENDMENT`, `EXTENSION` |  |  |
| `amendmentCode` | texte |  |  |
| `description` | texte |  |  |
| `effectiveDate` | date AAAA-MM-JJ |  |  |
| `previousEndDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `at` | date-heure ISO 8601 UTC |  |  |
| `documentCode` | texte |  |  |
