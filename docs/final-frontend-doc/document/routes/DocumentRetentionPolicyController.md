# DocumentRetentionPolicyController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 5 route(s).

## Routes

<a id="r-get-sni-api-v1-document-retention-policies"></a>
### GET /sni/api/v1/document-retention-policies

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Réponse : `List<DocumentRetentionPolicyResponse>`
- Modèles : [DocumentRetentionPolicyResponse](#m-DocumentRetentionPolicyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/retention/controller/DocumentRetentionPolicyController.java`, méthode `list`

<a id="r-post-sni-api-v1-document-retention-policies"></a>
### POST /sni/api/v1/document-retention-policies

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Corps JSON (obligatoire) : `DocumentRetentionPolicyRequest`
- Réponse : `DocumentRetentionPolicyResponse`
- Modèles : [DocumentRetentionPolicyResponse](#m-DocumentRetentionPolicyResponse), [DocumentRetentionPolicyRequest](#m-DocumentRetentionPolicyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/retention/controller/DocumentRetentionPolicyController.java`, méthode `create`

<a id="r-delete-sni-api-v1-document-retention-policies-code"></a>
### DELETE /sni/api/v1/document-retention-policies/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/retention/controller/DocumentRetentionPolicyController.java`, méthode `deactivate`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-retention-policies-code"></a>
### GET /sni/api/v1/document-retention-policies/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Réponse : `DocumentRetentionPolicyResponse`
- Modèles : [DocumentRetentionPolicyResponse](#m-DocumentRetentionPolicyResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/document/retention/controller/DocumentRetentionPolicyController.java`, méthode `getByCode`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-document-retention-policies-code"></a>
### PUT /sni/api/v1/document-retention-policies/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:SETTINGS` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('DOCUMENT:REVIEW','DOCUMENT_REVIEW')`
- Corps JSON (obligatoire) : `DocumentRetentionPolicyRequest`
- Réponse : `DocumentRetentionPolicyResponse`
- Modèles : [DocumentRetentionPolicyResponse](#m-DocumentRetentionPolicyResponse), [DocumentRetentionPolicyRequest](#m-DocumentRetentionPolicyRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/document/retention/controller/DocumentRetentionPolicyController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DocumentRetentionPolicyResponse"></a>
#### DocumentRetentionPolicyResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `name` | texte |  |  |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `documentTypeCode` | texte |  |  |
| `retentionDays` | entier |  |  |
| `retentionReference` | enum : `UPLOAD_DATE`, `EXPIRY_DATE` |  |  |
| `action` | enum : `ARCHIVE`, `PURGE_FLAG` |  |  |
| `active` | booléen |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `createdBy` | entier int64 ⚠ |  |  |

<a id="m-DocumentRetentionPolicyRequest"></a>
#### DocumentRetentionPolicyRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte | oui | longueur min `1` |
| `name` | texte | oui | longueur min `1` |
| `space` | enum : `KYC_SPACE`, `CONTRACT_SPACE`, `FINANCIAL_SPACE`, `ASSET_SPACE`, `ADMINISTRATIVE`, `GENERIC` |  |  |
| `documentTypeCode` | texte |  |  |
| `retentionDays` | entier | oui | min `1` |
| `retentionReference` | enum : `UPLOAD_DATE`, `EXPIRY_DATE` |  |  |
| `action` | enum : `ARCHIVE`, `PURGE_FLAG` |  |  |
