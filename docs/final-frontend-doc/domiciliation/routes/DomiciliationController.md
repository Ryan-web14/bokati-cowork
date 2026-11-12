# DomiciliationController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [domiciliation](../README.md) · 33 route(s).

## Routes

<a id="r-get-sni-api-v1-services-catalogue"></a>
### GET /sni/api/v1/services/catalogue

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<DomiciliationDtos.ServiceDefinitionView>`
- Modèles : [DomiciliationDtos.ServiceDefinitionView](#m-DomiciliationDtos-ServiceDefinitionView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `catalogue`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `includeInactive` | requête | `boolean` |  | false |

<a id="r-put-sni-api-v1-services-catalogue"></a>
### PUT /sni/api/v1/services/catalogue

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.ServiceDefinitionRequest`
- Réponse : `DomiciliationDtos.ServiceDefinitionView`
- Modèles : [DomiciliationDtos.ServiceDefinitionView](#m-DomiciliationDtos-ServiceDefinitionView), [DomiciliationDtos.ServiceDefinitionRequest](#m-DomiciliationDtos-ServiceDefinitionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `saveDefinition`

<a id="r-get-sni-api-v1-services-domiciliation-addresses"></a>
### GET /sni/api/v1/services/domiciliation/addresses

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<DomiciliationDtos.AddressView>`
- Modèles : [DomiciliationDtos.AddressView](#m-DomiciliationDtos-AddressView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `addresses`

<a id="r-post-sni-api-v1-services-domiciliation-addresses"></a>
### POST /sni/api/v1/services/domiciliation/addresses

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.RegisterAddressRequest`
- Réponse : `DomiciliationDtos.AddressView`
- Modèles : [DomiciliationDtos.AddressView](#m-DomiciliationDtos-AddressView), [DomiciliationDtos.RegisterAddressRequest](#m-DomiciliationDtos-RegisterAddressRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `registerAddress`

<a id="r-patch-sni-api-v1-services-domiciliation-addresses-code"></a>
### PATCH /sni/api/v1/services/domiciliation/addresses/{code}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.UpdateAddressRequest`
- Réponse : `DomiciliationDtos.AddressView`
- Modèles : [DomiciliationDtos.AddressView](#m-DomiciliationDtos-AddressView), [DomiciliationDtos.UpdateAddressRequest](#m-DomiciliationDtos-UpdateAddressRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `updateAddress`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-services-domiciliation-contracts"></a>
### GET /sni/api/v1/services/domiciliation/contracts

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PaginatedResponse<DomiciliationDtos.ContractView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView), [DomiciliationContract.Status](#m-DomiciliationContract-Status)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `contracts`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `List<DomiciliationContract.Status>` |  |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-services-domiciliation-contracts"></a>
### POST /sni/api/v1/services/domiciliation/contracts

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.OpenContractRequest`
- Réponse : `DomiciliationDtos.ContractView`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView), [DomiciliationDtos.OpenContractRequest](#m-DomiciliationDtos-OpenContractRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `open`

<a id="r-get-sni-api-v1-services-domiciliation-contracts-contractnumber"></a>
### GET /sni/api/v1/services/domiciliation/contracts/{contractNumber}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `DomiciliationDtos.ContractView`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `contract`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-certificate"></a>
### POST /sni/api/v1/services/domiciliation/contracts/{contractNumber}/certificate

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.CertificateRequest`
- Réponse : `DomiciliationDtos.ContractView`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView), [DomiciliationDtos.CertificateRequest](#m-DomiciliationDtos-CertificateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `certificate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-commitment"></a>
### POST /sni/api/v1/services/domiciliation/contracts/{contractNumber}/commitment

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.CommitmentRequest`
- Réponse : `DomiciliationDtos.ContractView`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView), [DomiciliationDtos.CommitmentRequest](#m-DomiciliationDtos-CommitmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `commitment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-services-domiciliation-contracts-contractnumber-mail"></a>
### GET /sni/api/v1/services/domiciliation/contracts/{contractNumber}/mail

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PaginatedResponse<DomiciliationDtos.MailItemView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `contractMail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-mail"></a>
### POST /sni/api/v1/services/domiciliation/contracts/{contractNumber}/mail

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.ReceiveMailRequest`
- Réponse : `DomiciliationDtos.MailItemView`
- Modèles : [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView), [DomiciliationDtos.ReceiveMailRequest](#m-DomiciliationDtos-ReceiveMailRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `receiveMail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-prepare"></a>
### POST /sni/api/v1/services/domiciliation/contracts/{contractNumber}/prepare

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `DomiciliationDtos.ContractView`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `prepare`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-registration-confirm"></a>
### POST /sni/api/v1/services/domiciliation/contracts/{contractNumber}/registration/confirm

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.ConfirmRegistrationRequest`
- Réponse : `DomiciliationDtos.ContractView`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView), [DomiciliationDtos.ConfirmRegistrationRequest](#m-DomiciliationDtos-ConfirmRegistrationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `confirmRegistration`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-registration-reject"></a>
### POST /sni/api/v1/services/domiciliation/contracts/{contractNumber}/registration/reject

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.RejectRegistrationRequest`
- Réponse : `DomiciliationDtos.RegistrationView`
- Modèles : [DomiciliationDtos.RegistrationView](#m-DomiciliationDtos-RegistrationView), [DomiciliationDtos.RejectRegistrationRequest](#m-DomiciliationDtos-RejectRegistrationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `rejectRegistration`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-registration-submit"></a>
### POST /sni/api/v1/services/domiciliation/contracts/{contractNumber}/registration/submit

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.SubmitRegistrationRequest`
- Réponse : `DomiciliationDtos.RegistrationView`
- Modèles : [DomiciliationDtos.RegistrationView](#m-DomiciliationDtos-RegistrationView), [DomiciliationDtos.SubmitRegistrationRequest](#m-DomiciliationDtos-SubmitRegistrationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `submitRegistration`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-services-domiciliation-contracts-contractnumber-registrations"></a>
### GET /sni/api/v1/services/domiciliation/contracts/{contractNumber}/registrations

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<DomiciliationDtos.RegistrationView>`
- Modèles : [DomiciliationDtos.RegistrationView](#m-DomiciliationDtos-RegistrationView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `registrations`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-signed"></a>
### POST /sni/api/v1/services/domiciliation/contracts/{contractNumber}/signed

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.SignedRequest`
- Réponse : `DomiciliationDtos.ContractView`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView), [DomiciliationDtos.SignedRequest](#m-DomiciliationDtos-SignedRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `signed`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-contracts-contractnumber-terminate"></a>
### POST /sni/api/v1/services/domiciliation/contracts/{contractNumber}/terminate

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.TerminateRequest`
- Réponse : `DomiciliationDtos.ContractView`
- Modèles : [DomiciliationDtos.ContractView](#m-DomiciliationDtos-ContractView), [DomiciliationDtos.TerminateRequest](#m-DomiciliationDtos-TerminateRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `terminate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-services-domiciliation-mail-pending"></a>
### GET /sni/api/v1/services/domiciliation/mail/pending

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PaginatedResponse<DomiciliationDtos.MailItemView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `pendingMail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-services-domiciliation-mail-itemnumber"></a>
### GET /sni/api/v1/services/domiciliation/mail/{itemNumber}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `DomiciliationDtos.MailItemView`
- Modèles : [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `mailItem`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-mail-itemnumber-collect"></a>
### POST /sni/api/v1/services/domiciliation/mail/{itemNumber}/collect

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.CollectRequest`
- Réponse : `DomiciliationDtos.MailItemView`
- Modèles : [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView), [DomiciliationDtos.CollectRequest](#m-DomiciliationDtos-CollectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `collect`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-mail-itemnumber-destroy"></a>
### POST /sni/api/v1/services/domiciliation/mail/{itemNumber}/destroy

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.ReasonRequest`
- Réponse : `DomiciliationDtos.MailItemView`
- Modèles : [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView), [DomiciliationDtos.ReasonRequest](#m-DomiciliationDtos-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `destroy`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-mail-itemnumber-forward"></a>
### POST /sni/api/v1/services/domiciliation/mail/{itemNumber}/forward

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.ForwardRequest`
- Réponse : `DomiciliationDtos.MailItemView`
- Modèles : [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView), [DomiciliationDtos.ForwardRequest](#m-DomiciliationDtos-ForwardRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `forward`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-mail-itemnumber-return"></a>
### POST /sni/api/v1/services/domiciliation/mail/{itemNumber}/return

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (facultatif) : `DomiciliationDtos.ReasonRequest`
- Réponse : `DomiciliationDtos.MailItemView`
- Modèles : [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView), [DomiciliationDtos.ReasonRequest](#m-DomiciliationDtos-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `returnMail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-domiciliation-mail-itemnumber-scan"></a>
### POST /sni/api/v1/services/domiciliation/mail/{itemNumber}/scan

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.ScanRequest`
- Réponse : `DomiciliationDtos.MailItemView`
- Modèles : [DomiciliationDtos.MailItemView](#m-DomiciliationDtos-MailItemView), [DomiciliationDtos.ScanRequest](#m-DomiciliationDtos-ScanRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `scan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-services-domiciliation-mail-itemnumber-trail"></a>
### GET /sni/api/v1/services/domiciliation/mail/{itemNumber}/trail

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<DomiciliationDtos.MailEventView>`
- Modèles : [DomiciliationDtos.MailEventView](#m-DomiciliationDtos-MailEventView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `mailTrail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `itemNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-services-domiciliation-registrations-in-progress"></a>
### GET /sni/api/v1/services/domiciliation/registrations/in-progress

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `PaginatedResponse<DomiciliationDtos.RegistrationView>`
- Modèles : [PaginatedResponse](#m-PaginatedResponse), [DomiciliationDtos.RegistrationView](#m-DomiciliationDtos-RegistrationView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `registrationsInProgress`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `page, size, sort` | requête (pagination) | `Pageable` |  | size=20 |

<a id="r-get-sni-api-v1-services-domiciliation-registry"></a>
### GET /sni/api/v1/services/domiciliation/registry

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Renvoie : `text/csv`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `registry`

<a id="r-post-sni-api-v1-services-subscribed-servicenumber-suspend"></a>
### POST /sni/api/v1/services/subscribed/{serviceNumber}/suspend

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (facultatif) : `DomiciliationDtos.ReasonRequest`
- Réponse : `DomiciliationDtos.SubscriptionServiceView`
- Modèles : [DomiciliationDtos.SubscriptionServiceView](#m-DomiciliationDtos-SubscriptionServiceView), [DomiciliationDtos.ReasonRequest](#m-DomiciliationDtos-ReasonRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `suspend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `serviceNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-subscribed-servicenumber-terminate"></a>
### POST /sni/api/v1/services/subscribed/{serviceNumber}/terminate

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `DomiciliationDtos.SubscriptionServiceView`
- Modèles : [DomiciliationDtos.SubscriptionServiceView](#m-DomiciliationDtos-SubscriptionServiceView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `terminateService`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `serviceNumber` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-services-subscriptions-subscriptionnumber"></a>
### GET /sni/api/v1/services/subscriptions/{subscriptionNumber}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Réponse : `List<DomiciliationDtos.SubscriptionServiceView>`
- Modèles : [DomiciliationDtos.SubscriptionServiceView](#m-DomiciliationDtos-SubscriptionServiceView)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `ofSubscription`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-services-subscriptions-subscriptionnumber"></a>
### POST /sni/api/v1/services/subscriptions/{subscriptionNumber}

- Accès : back-office : rôle d'administration et droit `ADMIN:ACCESS` ; contrôle supplémentaire sur la méthode : `hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')`
- Corps JSON (obligatoire) : `DomiciliationDtos.SubscribeServiceRequest`
- Réponse : `DomiciliationDtos.SubscriptionServiceView`
- Modèles : [DomiciliationDtos.SubscriptionServiceView](#m-DomiciliationDtos-SubscriptionServiceView), [DomiciliationDtos.SubscribeServiceRequest](#m-DomiciliationDtos-SubscribeServiceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/domiciliation/controller/DomiciliationController.java`, méthode `subscribe`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `subscriptionNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DomiciliationDtos-ServiceDefinitionView"></a>
#### DomiciliationDtos.ServiceDefinitionView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `description` | texte |  |  |
| `serviceCategory` | enum : `DOMICILIATION`, `MAIL_HANDLING`, `PHONE_ANSWERING`, `STORAGE`, `MEETING_ROOM`, `COWORKING_ACCESS`, `PRIVATE_OFFICE`, `ADMIN_SUPPORT`, `LEGAL_SUPPORT`, `ACCOUNTING_SUPPORT`, `IT_SUPPORT`, `EVENT_SPACE`, `PARKING`, `LOCKER`, `OTHER` |  |  |
| `deliveryMode` | enum : `CONTINUOUS`, `ON_DEMAND`, `SCHEDULED`, `METERED` |  |  |
| `requiresContract` | booléen |  |  |
| `requiresKycLevel` | entier |  |  |
| `requiresPhysicalResource` | booléen |  |  |
| `hasRegulatoryObligations` | booléen |  |  |
| `defaultBillingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `defaultNoticePeriodDays` | entier |  |  |
| `defaultCommitmentMonths` | entier |  |  |
| `unitPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `usageEntitlementCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-DomiciliationDtos-ServiceDefinitionRequest"></a>
#### DomiciliationDtos.ServiceDefinitionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `1` |
| `name` | texte | oui | longueur min `1` |
| `description` | texte |  |  |
| `serviceCategory` | enum : `DOMICILIATION`, `MAIL_HANDLING`, `PHONE_ANSWERING`, `STORAGE`, `MEETING_ROOM`, `COWORKING_ACCESS`, `PRIVATE_OFFICE`, `ADMIN_SUPPORT`, `LEGAL_SUPPORT`, `ACCOUNTING_SUPPORT`, `IT_SUPPORT`, `EVENT_SPACE`, `PARKING`, `LOCKER`, `OTHER` | oui |  |
| `deliveryMode` | enum : `CONTINUOUS`, `ON_DEMAND`, `SCHEDULED`, `METERED` |  |  |
| `requiresContract` | booléen |  |  |
| `requiresKycLevel` | entier |  |  |
| `requiresPhysicalResource` | booléen |  |  |
| `hasRegulatoryObligations` | booléen |  |  |
| `defaultBillingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `defaultNoticePeriodDays` | entier |  |  |
| `defaultCommitmentMonths` | entier |  |  |
| `unitPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `usageEntitlementCode` | texte |  |  |
| `active` | booléen |  |  |

<a id="m-DomiciliationDtos-AddressView"></a>
#### DomiciliationDtos.AddressView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `countryCode` | texte |  |  |
| `fiscalCapable` | booléen |  |  |
| `maxOccupants` | entier |  |  |
| `occupants` | entier int64 ⚠ |  |  |
| `active` | booléen |  |  |
| `notes` | texte |  |  |

<a id="m-DomiciliationDtos-RegisterAddressRequest"></a>
#### DomiciliationDtos.RegisterAddressRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte | oui | longueur min `1` |
| `address` | [AddressRequest](#m-AddressRequest) | oui |  |
| `fiscalCapable` | booléen |  |  |
| `maxOccupants` | entier |  |  |
| `notes` | texte |  |  |

<a id="m-DomiciliationDtos-UpdateAddressRequest"></a>
#### DomiciliationDtos.UpdateAddressRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `fiscalCapable` | booléen |  |  |
| `maxOccupants` | entier |  |  |
| `active` | booléen |  |  |
| `notes` | texte |  |  |

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

<a id="m-DomiciliationContract-Status"></a>
#### DomiciliationContract.Status

Énumération : `DRAFT`, `PENDING_DOCUMENTS`, `PENDING_SIGNATURE`, `PENDING_REGISTRATION`, `ACTIVE`, `SUSPENDED`, `TERMINATED`, `EXPIRED`

<a id="m-DomiciliationDtos-OpenContractRequest"></a>
#### DomiciliationDtos.OpenContractRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `subscriptionNumber` | texte | oui | longueur min `1` |
| `legalName` | texte |  |  |
| `legalForm` | texte |  |  |
| `registrationNumber` | texte |  |  |
| `taxNumber` | texte |  |  |
| `legalRepresentativeName` | texte |  |  |
| `addressCode` | texte | oui | longueur min `1` |
| `suiteNumber` | texte |  |  |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` | oui |  |
| `commitmentMonths` | entier |  |  |
| `noticePeriodDays` | entier |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `mailForwardingMode` | enum : `HOLD`, `FORWARD`, `SCAN_AND_FORWARD`, `SCAN_ONLY` |  |  |

<a id="m-DomiciliationDtos-CertificateRequest"></a>
#### DomiciliationDtos.CertificateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `scope` | enum : `COMMERCIAL`, `FISCAL` | oui |  |

<a id="m-DomiciliationDtos-CommitmentRequest"></a>
#### DomiciliationDtos.CommitmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `billingCycle` | enum : `ONE_TIME`, `DAILY`, `WEEKLY`, `MONTHLY`, `QUARTERLY`, `YEARLY` |  |  |
| `commitmentMonths` | entier |  |  |

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

<a id="m-DomiciliationDtos-ReceiveMailRequest"></a>
#### DomiciliationDtos.ReceiveMailRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `mailType` | enum : `LETTER`, `REGISTERED_LETTER`, `PARCEL`, `ADMINISTRATIVE`, `LEGAL_NOTICE`, `OTHER` | oui |  |
| `senderName` | texte |  |  |
| `senderReference` | texte |  |  |
| `weightGrams` | entier |  |  |
| `dimensions` | texte |  |  |
| `notes` | texte |  |  |

<a id="m-DomiciliationDtos-ConfirmRegistrationRequest"></a>
#### DomiciliationDtos.ConfirmRegistrationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `administrationReference` | texte | oui | longueur min `1` |
| `registrationDate` | date AAAA-MM-JJ |  |  |
| `expiresAt` | date AAAA-MM-JJ |  |  |
| `receiptDocumentCode` | texte |  |  |
| `registeredDocumentCode` | texte | oui | longueur min `1` |

<a id="m-DomiciliationDtos-RegistrationView"></a>
#### DomiciliationDtos.RegistrationView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `registrationNumber` | texte |  |  |
| `contractNumber` | texte |  |  |
| `status` | enum : `PENDING`, `SUBMITTED`, `REGISTERED`, `REJECTED` |  |  |
| `submittedAt` | date-heure ISO 8601 UTC |  |  |
| `submittedBy` | texte |  |  |
| `administrationOffice` | texte |  |  |
| `administrationReference` | texte |  |  |
| `registrationDate` | date AAAA-MM-JJ |  |  |
| `stampDutyAmount` | nombre décimal |  |  |
| `registrationFeeAmount` | nombre décimal |  |  |
| `totalDutyAmount` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `paidBy` | enum : `COMPANY`, `CLIENT` |  |  |
| `rebilled` | booléen |  |  |
| `receiptDocumentCode` | texte |  |  |
| `registeredDocumentCode` | texte |  |  |
| `expiresAt` | date AAAA-MM-JJ |  |  |
| `rejectionReason` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DomiciliationDtos-RejectRegistrationRequest"></a>
#### DomiciliationDtos.RejectRegistrationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |

<a id="m-DomiciliationDtos-SubmitRegistrationRequest"></a>
#### DomiciliationDtos.SubmitRegistrationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `administrationOffice` | texte |  |  |
| `stampDutyAmount` | nombre décimal |  |  |
| `registrationFeeAmount` | nombre décimal |  |  |
| `paidBy` | enum : `COMPANY`, `CLIENT` |  |  |
| `rebilled` | booléen |  |  |

<a id="m-DomiciliationDtos-SignedRequest"></a>
#### DomiciliationDtos.SignedRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signedDocumentCode` | texte | oui | longueur min `1` |

<a id="m-DomiciliationDtos-TerminateRequest"></a>
#### DomiciliationDtos.TerminateRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |
| `effectiveDate` | date AAAA-MM-JJ |  |  |

<a id="m-DomiciliationDtos-CollectRequest"></a>
#### DomiciliationDtos.CollectRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `collectedBy` | texte |  |  |
| `collectorIdDocument` | texte |  |  |
| `collectorSignatureUrl` | texte |  |  |
| `collectorMemberId` | texte |  |  |

<a id="m-DomiciliationDtos-ReasonRequest"></a>
#### DomiciliationDtos.ReasonRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte | oui | longueur min `1` |

<a id="m-DomiciliationDtos-ForwardRequest"></a>
#### DomiciliationDtos.ForwardRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `trackingNumber` | texte |  |  |
| `transportCost` | nombre décimal |  |  |

<a id="m-DomiciliationDtos-ScanRequest"></a>
#### DomiciliationDtos.ScanRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `scanDocumentCode` | texte | oui | longueur min `1` |

<a id="m-DomiciliationDtos-MailEventView"></a>
#### DomiciliationDtos.MailEventView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `eventType` | texte |  |  |
| `actor` | texte |  |  |
| `details` | texte |  |  |
| `occurredAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DomiciliationDtos-SubscriptionServiceView"></a>
#### DomiciliationDtos.SubscriptionServiceView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `serviceNumber` | texte |  |  |
| `subscriptionNumber` | texte |  |  |
| `serviceCode` | texte |  |  |
| `serviceName` | texte |  |  |
| `status` | enum : `PENDING`, `ACTIVE`, `SUSPENDED`, `TERMINATED` |  |  |
| `quantity` | entier |  |  |
| `unitPrice` | nombre décimal |  |  |
| `currency` | texte |  |  |
| `activatedAt` | date-heure ISO 8601 UTC |  |  |
| `suspendedAt` | date-heure ISO 8601 UTC |  |  |
| `terminatedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DomiciliationDtos-SubscribeServiceRequest"></a>
#### DomiciliationDtos.SubscribeServiceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `serviceCode` | texte | oui | longueur min `1` |
| `quantity` | entier |  |  |
| `unitPrice` | nombre décimal |  |  |
| `metadataJson` | texte |  |  |

<a id="m-AddressRequest"></a>
#### AddressRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `streetNumber` | texte |  |  |
| `streetName` | texte |  |  |
| `district` | texte |  |  |
| `city` | texte |  |  |
| `countryCode` | texte |  | longueur min `0`, longueur max `3` |
