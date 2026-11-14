# DocumentTemplateController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [document](../README.md) · 11 route(s).

## Routes

<a id="r-get-sni-api-v1-document-templates"></a>
### GET /sni/api/v1/document-templates

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Réponse : `List<DocumentTemplateDtos.Summary>`
- Modèles : [DocumentTemplateDtos.Summary](#m-DocumentTemplateDtos-Summary)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `list`

<a id="r-get-sni-api-v1-document-templates-code"></a>
### GET /sni/api/v1/document-templates/{code}

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Réponse : `DocumentTemplateDtos.Detail`
- Modèles : [DocumentTemplateDtos.Detail](#m-DocumentTemplateDtos-Detail)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-delete-sni-api-v1-document-templates-code-draft"></a>
### DELETE /sni/api/v1/document-templates/{code}/draft

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `deleteDraft`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-put-sni-api-v1-document-templates-code-draft"></a>
### PUT /sni/api/v1/document-templates/{code}/draft

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Corps JSON (obligatoire) : `DocumentTemplateDtos.Content`
- Réponse : `DocumentTemplateDtos.Version`
- Modèles : [DocumentTemplateDtos.Version](#m-DocumentTemplateDtos-Version), [DocumentTemplateDtos.Content](#m-DocumentTemplateDtos-Content)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `saveDraft`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-document-templates-code-fields-catalog"></a>
### GET /sni/api/v1/document-templates/{code}/fields/catalog

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Réponse : `DocumentTemplateDtos.Catalog`
- Modèles : [DocumentTemplateDtos.Catalog](#m-DocumentTemplateDtos-Catalog)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `catalog`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-document-templates-code-preview"></a>
### POST /sni/api/v1/document-templates/{code}/preview

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Renvoie : `application/pdf`
- Corps JSON (facultatif) : `DocumentTemplateDtos.Content`
- Réponse : `fichier (octets)`
- Modèles : [DocumentTemplateDtos.Content](#m-DocumentTemplateDtos-Content)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `preview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `number` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-document-templates-code-preview-email"></a>
### POST /sni/api/v1/document-templates/{code}/preview-email

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Renvoie : `text/html`
- Corps JSON (facultatif) : `DocumentTemplateDtos.Content`
- Réponse : `String`
- Modèles : [DocumentTemplateDtos.Content](#m-DocumentTemplateDtos-Content)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `previewEmail`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-document-templates-code-publish"></a>
### POST /sni/api/v1/document-templates/{code}/publish

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Réponse : `DocumentTemplateDtos.Version`
- Modèles : [DocumentTemplateDtos.Version](#m-DocumentTemplateDtos-Version)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `publish`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-document-templates-code-reset"></a>
### POST /sni/api/v1/document-templates/{code}/reset

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Réponse : `DocumentTemplateDtos.Detail`
- Modèles : [DocumentTemplateDtos.Detail](#m-DocumentTemplateDtos-Detail)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `reset`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-document-templates-code-restore-version"></a>
### POST /sni/api/v1/document-templates/{code}/restore/{version}

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Réponse : `DocumentTemplateDtos.Version`
- Modèles : [DocumentTemplateDtos.Version](#m-DocumentTemplateDtos-Version)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `restore`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `version` | chemin | `int` | oui |  |

<a id="r-post-sni-api-v1-document-templates-code-test-send"></a>
### POST /sni/api/v1/document-templates/{code}/test-send

- Accès : back-office : rôle d'administration et droit `SYSTEM:DOCUMENT_TEMPLATES`
- Corps JSON (facultatif) : `DocumentTemplateDtos.Content`
- Réponse : `Map<String, String>`
- Modèles : [DocumentTemplateDtos.Content](#m-DocumentTemplateDtos-Content)
- Code : `src/main/java/com/sni/bokaticowork/features/document/template/controller/DocumentTemplateController.java`, méthode `testSend`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-DocumentTemplateDtos-Summary"></a>
#### DocumentTemplateDtos.Summary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `family` | texte |  |  |
| `domain` | texte |  |  |
| `active` | booléen |  |  |
| `publishedVersion` | entier |  |  |
| `publishedAt` | date-heure ISO 8601 UTC |  |  |
| `hasDraft` | booléen |  |  |

<a id="m-DocumentTemplateDtos-Detail"></a>
#### DocumentTemplateDtos.Detail

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `family` | texte |  |  |
| `domain` | texte |  |  |
| `fallbackTemplate` | texte |  |  |
| `description` | texte |  |  |
| `active` | booléen |  |  |
| `published` | [DocumentTemplateDtos.Version](#m-DocumentTemplateDtos-Version) |  |  |
| `draft` | [DocumentTemplateDtos.Version](#m-DocumentTemplateDtos-Version) |  |  |
| `versions` | [DocumentTemplateDtos.VersionSummary](#m-DocumentTemplateDtos-VersionSummary)[] |  |  |

<a id="m-DocumentTemplateDtos-Version"></a>
#### DocumentTemplateDtos.Version

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `versionNumber` | entier |  |  |
| `status` | texte |  |  |
| `pageFormat` | texte |  |  |
| `copies` | objet { clé : objet libre }[] |  |  |
| `style` | objet { clé : objet libre } |  |  |
| `blocks` | objet { clé : objet libre }[] |  |  |
| `fields` | objet { clé : objet libre }[] |  |  |
| `note` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `publishedBy` | texte |  |  |
| `publishedAt` | date-heure ISO 8601 UTC |  |  |
| `archivedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DocumentTemplateDtos-Content"></a>
#### DocumentTemplateDtos.Content

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `pageFormat` | texte |  |  |
| `copies` | objet { clé : objet libre }[] |  | éléments min `0`, éléments max `4` |
| `style` | objet { clé : objet libre } |  |  |
| `blocks` | objet { clé : objet libre }[] |  | éléments min `0`, éléments max `40` |
| `fields` | objet { clé : objet libre }[] |  | éléments min `0`, éléments max `20` |
| `note` | texte |  | longueur min `0`, longueur max `500` |

<a id="m-DocumentTemplateDtos-Catalog"></a>
#### DocumentTemplateDtos.Catalog

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `fields` | [DocumentTemplateDtos.FieldEntry](#m-DocumentTemplateDtos-FieldEntry)[] |  |  |
| `tables` | [DocumentTemplateDtos.TableEntry](#m-DocumentTemplateDtos-TableEntry)[] |  |  |
| `signers` | [DocumentTemplateDtos.SignerEntry](#m-DocumentTemplateDtos-SignerEntry)[] |  |  |
| `commonTokens` | texte[] |  |  |
| `blockTypes` | texte[] |  |  |
| `pageFormats` | texte[] |  |  |
| `realPreview` | booléen |  |  |
| `previewNumberHint` | texte |  |  |

<a id="m-DocumentTemplateDtos-VersionSummary"></a>
#### DocumentTemplateDtos.VersionSummary

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `versionNumber` | entier |  |  |
| `status` | texte |  |  |
| `note` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `publishedBy` | texte |  |  |
| `publishedAt` | date-heure ISO 8601 UTC |  |  |
| `archivedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DocumentTemplateDtos-FieldEntry"></a>
#### DocumentTemplateDtos.FieldEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `key` | texte |  |  |
| `label` | texte |  |  |
| `sample` | texte |  |  |

<a id="m-DocumentTemplateDtos-TableEntry"></a>
#### DocumentTemplateDtos.TableEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `key` | texte |  |  |
| `title` | texte |  |  |
| `columns` | [DocumentTemplateDtos.ColumnEntry](#m-DocumentTemplateDtos-ColumnEntry)[] |  |  |

<a id="m-DocumentTemplateDtos-SignerEntry"></a>
#### DocumentTemplateDtos.SignerEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `key` | texte |  |  |
| `role` | texte |  |  |

<a id="m-DocumentTemplateDtos-ColumnEntry"></a>
#### DocumentTemplateDtos.ColumnEntry

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `key` | texte |  |  |
| `label` | texte |  |  |
| `numeric` | booléen |  |  |
