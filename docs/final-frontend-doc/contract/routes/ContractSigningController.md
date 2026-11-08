# ContractSigningController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [contract](../README.md) · 3 route(s).

## Routes

<a id="r-post-sni-api-v1-contracts-contractcode-request-signing"></a>
### POST /sni/api/v1/contracts/{contractCode}/request-signing

- Accès : back-office : rôle d'administration et droit `BILLING:CREATE`
- Corps JSON (obligatoire) : `RequestContractSigningRequest`
- Réponse : `ContractSigningResponse`
- Modèles : [ContractSigningResponse](#m-ContractSigningResponse), [RequestContractSigningRequest](#m-RequestContractSigningRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractSigningController.java`, méthode `requestSigning`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `contractCode` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-public-contracts-sign-token"></a>
### GET /sni/api/v1/public/contracts/sign/{token}

- Accès : public, sans jeton
- Réponse : `ContractSigningResponse`
- Modèles : [ContractSigningResponse](#m-ContractSigningResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractSigningController.java`, méthode `getSigningDetails`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `UUID` | oui |  |

<a id="r-post-sni-api-v1-public-contracts-sign-token"></a>
### POST /sni/api/v1/public/contracts/sign/{token}

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `SignContractRequest`
- Réponse : `ContractSigningResponse`
- Modèles : [ContractSigningResponse](#m-ContractSigningResponse), [SignContractRequest](#m-SignContractRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/contract/controller/ContractSigningController.java`, méthode `sign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `token` | chemin | `UUID` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-ContractSigningResponse"></a>
#### ContractSigningResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `token` | uuid |  |  |
| `contractCode` | texte |  |  |
| `contractTitle` | texte |  |  |
| `signerEmail` | texte |  |  |
| `signerName` | texte |  |  |
| `expiresAt` | date-heure ISO 8601 UTC |  |  |
| `signedAt` | date-heure ISO 8601 UTC |  |  |
| `revoked` | booléen |  |  |

<a id="m-RequestContractSigningRequest"></a>
#### RequestContractSigningRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `signerEmail` | string (email) |  |  |
| `signerName` | texte |  |  |

<a id="m-SignContractRequest"></a>
#### SignContractRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `accepted` | booléen |  |  |
| `consentText` | texte |  |  |
