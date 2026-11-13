# FixedAssetController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [accounting](../README.md) · 49 route(s).

## Routes

<a id="r-get-sni-api-v1-accounting-fixed-assets"></a>
### GET /sni/api/v1/accounting/fixed-assets

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<FixedAsset>`
- Modèles : [FixedAsset](#m-FixedAsset)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `String` |  |  |
| `category` | requête | `String` |  |  |
| `site` | requête | `String` |  |  |
| `resource` | requête | `String` |  |  |
| `holder` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets"></a>
### POST /sni/api/v1/accounting/fixed-assets

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.AssetRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.AssetRequest](#m-FixedAssetService-AssetRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `create`

<a id="r-get-sni-api-v1-accounting-fixed-assets-categories"></a>
### GET /sni/api/v1/accounting/fixed-assets/categories

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<FaCategory>`
- Modèles : [FaCategory](#m-FaCategory)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `categories`

<a id="r-post-sni-api-v1-accounting-fixed-assets-categories"></a>
### POST /sni/api/v1/accounting/fixed-assets/categories

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.CategoryRequest`
- Réponse : `FaCategory`
- Modèles : [FaCategory](#m-FaCategory), [FixedAssetService.CategoryRequest](#m-FixedAssetService-CategoryRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `saveCategory`

<a id="r-get-sni-api-v1-accounting-fixed-assets-closing-check"></a>
### GET /sni/api/v1/accounting/fixed-assets/closing-check

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<FixedAssetReportService.Finding>`
- Modèles : [FixedAssetReportService.Finding](#m-FixedAssetReportService-Finding)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `closingCheck`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `year` | requête | `int` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-controls"></a>
### GET /sni/api/v1/accounting/fixed-assets/controls

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<FixedAssetReportService.Finding>`
- Modèles : [FixedAssetReportService.Finding](#m-FixedAssetReportService-Finding)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `controls`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `date` | requête | `LocalDate` |  |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-depreciation-profiles"></a>
### GET /sni/api/v1/accounting/fixed-assets/depreciation-profiles

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<FaDepreciationProfile>`
- Modèles : [FaDepreciationProfile](#m-FaDepreciationProfile)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `profiles`

<a id="r-post-sni-api-v1-accounting-fixed-assets-depreciation-profiles"></a>
### POST /sni/api/v1/accounting/fixed-assets/depreciation-profiles

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `DepreciationProfileService.ProfileRequest`
- Réponse : `FaDepreciationProfile`
- Modèles : [FaDepreciationProfile](#m-FaDepreciationProfile), [DepreciationProfileService.ProfileRequest](#m-DepreciationProfileService-ProfileRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `createProfile`

<a id="r-patch-sni-api-v1-accounting-fixed-assets-depreciation-profiles-id"></a>
### PATCH /sni/api/v1/accounting/fixed-assets/depreciation-profiles/{id}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `DepreciationProfileService.ProfileRequest`
- Réponse : `FaDepreciationProfile`
- Modèles : [FaDepreciationProfile](#m-FaDepreciationProfile), [DepreciationProfileService.ProfileRequest](#m-DepreciationProfileService-ProfileRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `updateProfile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-depreciation-profiles-id-deactivate"></a>
### POST /sni/api/v1/accounting/fixed-assets/depreciation-profiles/{id}/deactivate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `FaDepreciationProfile`
- Modèles : [FaDepreciationProfile](#m-FaDepreciationProfile)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `deactivateProfile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-depreciation-runs"></a>
### POST /sni/api/v1/accounting/fixed-assets/depreciation-runs

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `DepreciationRunService.RunResult`
- Modèles : [DepreciationRunService.RunResult](#m-DepreciationRunService-RunResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `run`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-depreciation-runs-preview"></a>
### GET /sni/api/v1/accounting/fixed-assets/depreciation-runs/preview

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `DepreciationRunService.RunResult`
- Modèles : [DepreciationRunService.RunResult](#m-DepreciationRunService-RunResult)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `preview`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `period` | requête | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-disposals-id-cancel"></a>
### POST /sni/api/v1/accounting/fixed-assets/disposals/{id}/cancel

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `FixedAssetDisposal`
- Modèles : [FixedAssetDisposal](#m-FixedAssetDisposal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `cancelDisposal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-disposals-id-collections"></a>
### POST /sni/api/v1/accounting/fixed-assets/disposals/{id}/collections

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.CollectRequest`
- Réponse : `FixedAssetDisposal`
- Modèles : [FixedAssetDisposal](#m-FixedAssetDisposal), [FixedAssetService.CollectRequest](#m-FixedAssetService-CollectRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `collect`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-disposals-id-indemnities"></a>
### POST /sni/api/v1/accounting/fixed-assets/disposals/{id}/indemnities

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.IndemnityRequest`
- Réponse : `FixedAssetDisposal`
- Modèles : [FixedAssetDisposal](#m-FixedAssetDisposal), [FixedAssetService.IndemnityRequest](#m-FixedAssetService-IndemnityRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `indemnity`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-disposals-id-pdf"></a>
### GET /sni/api/v1/accounting/fixed-assets/disposals/{id}/pdf

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `disposalPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-disposals-id-validate"></a>
### POST /sni/api/v1/accounting/fixed-assets/disposals/{id}/validate

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Réponse : `FixedAssetDisposal`
- Modèles : [FixedAssetDisposal](#m-FixedAssetDisposal)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `validateDisposal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-impairments-id-decision"></a>
### POST /sni/api/v1/accounting/fixed-assets/impairments/{id}/decision

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetController.DecisionRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetController.DecisionRequest](#m-FixedAssetController-DecisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `decideImpairment`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `id` | chemin | `Long` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-inventories"></a>
### GET /sni/api/v1/accounting/fixed-assets/inventories

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `List<FixedAssetInventoryCampaign>`
- Modèles : [FixedAssetInventoryCampaign](#m-FixedAssetInventoryCampaign)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `inventories`

<a id="r-post-sni-api-v1-accounting-fixed-assets-inventories"></a>
### POST /sni/api/v1/accounting/fixed-assets/inventories

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetInventoryService.CampaignRequest`
- Réponse : `FixedAssetInventoryService.CampaignView`
- Modèles : [FixedAssetInventoryService.CampaignView](#m-FixedAssetInventoryService-CampaignView), [FixedAssetInventoryService.CampaignRequest](#m-FixedAssetInventoryService-CampaignRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `openInventory`

<a id="r-get-sni-api-v1-accounting-fixed-assets-inventories-code"></a>
### GET /sni/api/v1/accounting/fixed-assets/inventories/{code}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `FixedAssetInventoryService.CampaignView`
- Modèles : [FixedAssetInventoryService.CampaignView](#m-FixedAssetInventoryService-CampaignView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `inventory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-inventories-code-close"></a>
### POST /sni/api/v1/accounting/fixed-assets/inventories/{code}/close

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (facultatif) : `FixedAssetInventoryService.CloseRequest`
- Réponse : `FixedAssetInventoryService.CampaignView`
- Modèles : [FixedAssetInventoryService.CampaignView](#m-FixedAssetInventoryService-CampaignView), [FixedAssetInventoryService.CloseRequest](#m-FixedAssetInventoryService-CloseRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `closeInventory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-inventories-code-pdf"></a>
### GET /sni/api/v1/accounting/fixed-assets/inventories/{code}/pdf

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `inventoryPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-inventories-code-scans"></a>
### POST /sni/api/v1/accounting/fixed-assets/inventories/{code}/scans

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetInventoryService.ScanRequest`
- Réponse : `FixedAssetInventoryService.CampaignView`
- Modèles : [FixedAssetInventoryService.CampaignView](#m-FixedAssetInventoryService-CampaignView), [FixedAssetInventoryService.ScanRequest](#m-FixedAssetInventoryService-ScanRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `scan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-labels"></a>
### POST /sni/api/v1/accounting/fixed-assets/labels

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetController.CodesRequest`
- Réponse : `fichier (octets)`
- Modèles : [FixedAssetController.CodesRequest](#m-FixedAssetController-CodesRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `labels`

<a id="r-get-sni-api-v1-accounting-fixed-assets-reports-report"></a>
### GET /sni/api/v1/accounting/fixed-assets/reports/{report}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `non typé, voir le code`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `report`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `report` | chemin | `String` | oui |  |
| `year` | requête | `Integer` |  |  |
| `date` | requête | `LocalDate` |  |  |
| `category` | requête | `String` |  |  |
| `site` | requête | `String` |  |  |
| `resource` | requête | `String` |  |  |
| `holder` | requête | `String` |  |  |
| `format` | requête | `String` |  | json |

<a id="r-get-sni-api-v1-accounting-fixed-assets-code"></a>
### GET /sni/api/v1/accounting/fixed-assets/{code}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `get`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-patch-sni-api-v1-accounting-fixed-assets-code"></a>
### PATCH /sni/api/v1/accounting/fixed-assets/{code}

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.AssetRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.AssetRequest](#m-FixedAssetService-AssetRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `update`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-assignments"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/assignments

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.AssignmentRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.AssignmentRequest](#m-FixedAssetService-AssignmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `assign`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-commission"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/commission

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.CommissionRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.CommissionRequest](#m-FixedAssetService-CommissionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `commission`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-code-commission-pdf"></a>
### GET /sni/api/v1/accounting/fixed-assets/{code}/commission.pdf

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `commissionPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-components"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/components

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `List<FixedAssetService.ComponentInput>`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.ComponentInput](#m-FixedAssetService-ComponentInput)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `components`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-code-depreciation-profile"></a>
### GET /sni/api/v1/accounting/fixed-assets/{code}/depreciation-profile

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `DepreciationProfileService.Resolution`
- Modèles : [DepreciationProfileService.Resolution](#m-DepreciationProfileService-Resolution)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `resolvedProfile`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-disposals"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/disposals

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.DisposalRequest`
- Réponse : `FixedAssetDisposal`
- Modèles : [FixedAssetDisposal](#m-FixedAssetDisposal), [FixedAssetService.DisposalRequest](#m-FixedAssetService-DisposalRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `prepareDisposal`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-code-handover-pdf"></a>
### GET /sni/api/v1/accounting/fixed-assets/{code}/handover.pdf

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `handoverPdf`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-impairments"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/impairments

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.ImpairmentRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.ImpairmentRequest](#m-FixedAssetService-ImpairmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `impair`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-inventory-links"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/inventory-links

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetController.InventoryLinkRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetController.InventoryLinkRequest](#m-FixedAssetController-InventoryLinkRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `linkInventory`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-code-label-pdf"></a>
### GET /sni/api/v1/accounting/fixed-assets/{code}/label.pdf

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `label`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-lease"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/lease

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetController.LeaseRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetController.LeaseRequest](#m-FixedAssetController-LeaseRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `lease`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-maintenance"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/maintenance

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.MaintenanceRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.MaintenanceRequest](#m-FixedAssetService-MaintenanceRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `maintenance`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-meter-readings"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/meter-readings

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetController.MeterRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetController.MeterRequest](#m-FixedAssetController-MeterRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `meterReading`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-operational-status"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/operational-status

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.OperationalRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.OperationalRequest](#m-FixedAssetService-OperationalRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `operational`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-code-plan"></a>
### GET /sni/api/v1/accounting/fixed-assets/{code}/plan

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `FixedAssetService.PlanView`
- Modèles : [FixedAssetService.PlanView](#m-FixedAssetService-PlanView)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `plan`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |
| `kind` | requête | `String` |  |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-reclassifications"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/reclassifications

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.ReclassRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.ReclassRequest](#m-FixedAssetService-ReclassRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `reclassify`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-returns"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/returns

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.AssignmentRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.AssignmentRequest](#m-FixedAssetService-AssignmentRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `returnAsset`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-revisions"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/revisions

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.RevisionRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.RevisionRequest](#m-FixedAssetService-RevisionRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `revise`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-get-sni-api-v1-accounting-fixed-assets-code-sheet-pdf"></a>
### GET /sni/api/v1/accounting/fixed-assets/{code}/sheet.pdf

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_REPORT:READ`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `sheet`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-transfers"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/transfers

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.TransferRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.TransferRequest](#m-FixedAssetService-TransferRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `transfer`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-accounting-fixed-assets-code-vat-regularizations"></a>
### POST /sni/api/v1/accounting/fixed-assets/{code}/vat-regularizations

- Accès : back-office : rôle d'administration et droit `ACCOUNTING_SETTINGS:MANAGE`
- Corps JSON (obligatoire) : `FixedAssetService.VatRegularizationRequest`
- Réponse : `FixedAssetService.AssetView`
- Modèles : [FixedAssetService.AssetView](#m-FixedAssetService-AssetView), [FixedAssetService.VatRegularizationRequest](#m-FixedAssetService-VatRegularizationRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/accounting/controller/FixedAssetController.java`, méthode `regularizeVat`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `code` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-FixedAsset"></a>
#### FixedAsset

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `companyCode` | texte |  |  |
| `parentId` | entier int64 ⚠ |  |  |
| `categoryCode` | texte |  |  |
| `targetCategoryCode` | texte |  |  |
| `designation` | texte |  |  |
| `brand` | texte |  |  |
| `model` | texte |  |  |
| `serialNumber` | texte |  |  |
| `quantity` | entier |  |  |
| `entryMode` | texte |  |  |
| `supplierCode` | texte |  |  |
| `supplierName` | texte |  |  |
| `supplierInvoiceNumber` | texte |  |  |
| `supplierInvoiceLineNo` | entier |  |  |
| `orderNumber` | texte |  |  |
| `fundRequestRef` | texte |  |  |
| `justificationRef` | texte |  |  |
| `entryNumber` | texte |  |  |
| `booked` | booléen |  |  |
| `trackedOffBooks` | booléen |  |  |
| `acquisitionDate` | date AAAA-MM-JJ |  |  |
| `receivedDate` | date AAAA-MM-JJ |  |  |
| `inServiceDate` | date AAAA-MM-JJ |  |  |
| `entryValue` | nombre décimal |  |  |
| `residualValue` | nombre décimal |  |  |
| `deductedVat` | nombre décimal |  |  |
| `accountingStatus` | texte |  |  |
| `operationalStatus` | texte |  |  |
| `operationalSince` | date AAAA-MM-JJ |  |  |
| `method` | texte |  |  |
| `durationMonths` | entier |  |  |
| `coefficient` | nombre décimal |  |  |
| `totalUnits` | nombre décimal |  |  |
| `taxMethod` | texte |  |  |
| `taxDurationMonths` | entier |  |  |
| `planVersion` | entier |  |  |
| `accumulatedDepreciation` | nombre décimal |  |  |
| `accumulatedImpairment` | nombre décimal |  |  |
| `accumulatedDerogatory` | nombre décimal |  |  |
| `subsidyAmount` | nombre décimal |  |  |
| `subsidyAccount` | texte |  |  |
| `subsidyReleased` | nombre décimal |  |  |
| `organisation` | texte |  |  |
| `site` | texte |  |  |
| `resourceCode` | texte |  |  |
| `amenityLinkId` | entier int64 ⚠ |  |  |
| `locationLabel` | texte |  |  |
| `holder` | texte |  |  |
| `holderSince` | date AAAA-MM-JJ |  |  |
| `analyticSections` | texte |  |  |
| `warrantyEnd` | date AAAA-MM-JJ |  |  |
| `maintenanceContract` | texte |  |  |
| `maintenanceEnd` | date AAAA-MM-JJ |  |  |
| `insurer` | texte |  |  |
| `policyNumber` | texte |  |  |
| `insuredValue` | nombre décimal |  |  |
| `documentRefs` | texte |  |  |
| `photoRef` | texte |  |  |
| `qrActive` | booléen |  |  |
| `labelPrintCount` | entier |  |  |
| `notes` | texte |  |  |
| `disposedOn` | date AAAA-MM-JJ |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |
| `loanCode` | texte |  |  |

<a id="m-FixedAssetService-AssetView"></a>
#### FixedAssetService.AssetView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `asset` | [FixedAsset](#m-FixedAsset) |  |  |
| `category` | [FaCategory](#m-FaCategory) |  |  |
| `netValue` | nombre décimal |  |  |
| `analytic` | objet { clé : texte } |  |  |
| `values` | [FixedAssetValueLine](#m-FixedAssetValueLine)[] |  |  |
| `components` | [FixedAsset](#m-FixedAsset)[] |  |  |
| `suggestedComponents` | texte[] |  |  |
| `schedule` | [FixedAssetSchedule](#m-FixedAssetSchedule)[] |  |  |
| `events` | [FixedAssetEvent](#m-FixedAssetEvent)[] |  |  |
| `disposals` | [FixedAssetDisposal](#m-FixedAssetDisposal)[] |  |  |
| `impairments` | [FixedAssetImpairment](#m-FixedAssetImpairment)[] |  |  |
| `maintenance` | [FixedAssetMaintenance](#m-FixedAssetMaintenance)[] |  |  |
| `inventoryAssets` | texte[] |  |  |
| `warnings` | texte[] |  |  |

<a id="m-FixedAssetService-AssetRequest"></a>
#### FixedAssetService.AssetRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `categoryCode` | texte |  |  |
| `designation` | texte |  |  |
| `brand` | texte |  |  |
| `model` | texte |  |  |
| `serialNumber` | texte |  |  |
| `quantity` | entier |  |  |
| `entryMode` | texte |  |  |
| `supplierCode` | texte |  |  |
| `supplierName` | texte |  |  |
| `supplierInvoiceNumber` | texte |  |  |
| `orderNumber` | texte |  |  |
| `fundRequestRef` | texte |  |  |
| `justificationRef` | texte |  |  |
| `acquisitionDate` | date AAAA-MM-JJ |  |  |
| `receivedDate` | date AAAA-MM-JJ |  |  |
| `entryValue` | nombre décimal |  |  |
| `residualValue` | nombre décimal |  |  |
| `deductedVat` | nombre décimal |  |  |
| `trackedOffBooks` | booléen |  |  |
| `counterAccount` | texte |  |  |
| `entryNumber` | texte |  |  |
| `organisation` | texte |  |  |
| `site` | texte |  |  |
| `resourceCode` | texte |  |  |
| `amenityLinkId` | entier int64 ⚠ |  |  |
| `locationLabel` | texte |  |  |
| `analytic` | objet { clé : texte } |  |  |
| `warrantyEnd` | date AAAA-MM-JJ |  |  |
| `maintenanceContract` | texte |  |  |
| `maintenanceEnd` | date AAAA-MM-JJ |  |  |
| `insurer` | texte |  |  |
| `policyNumber` | texte |  |  |
| `insuredValue` | nombre décimal |  |  |
| `documentRefs` | texte |  |  |
| `photoRef` | texte |  |  |
| `notes` | texte |  |  |
| `method` | texte |  |  |
| `durationMonths` | entier |  |  |
| `coefficient` | nombre décimal |  |  |
| `totalUnits` | nombre décimal |  |  |
| `taxMethod` | texte |  |  |
| `taxDurationMonths` | entier |  |  |
| `targetCategoryCode` | texte |  |  |
| `inventoryAssetCodes` | texte[] |  |  |

<a id="m-FaCategory"></a>
#### FaCategory

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `assetAccount` | texte |  |  |
| `depreciationAccount` | texte |  |  |
| `expenseAccount` | texte |  |  |
| `impairmentAccount` | texte |  |  |
| `impairmentExpenseAccount` | texte |  |  |
| `impairmentReversalAccount` | texte |  |  |
| `disposalValueAccount` | texte |  |  |
| `disposalIncomeAccount` | texte |  |  |
| `method` | texte |  |  |
| `durationMonths` | entier |  |  |
| `coefficient` | nombre décimal |  |  |
| `annualRate` | nombre décimal |  |  |
| `threshold` | nombre décimal |  |  |
| `tracking` | texte |  |  |
| `trackOffBooks` | booléen |  |  |
| `taxMethod` | texte |  |  |
| `taxDurationMonths` | entier |  |  |
| `componentTypes` | texte |  |  |
| `vatRecoverable` | booléen |  |  |
| `inProgress` | booléen |  |  |
| `active` | booléen |  |  |

<a id="m-FixedAssetService-CategoryRequest"></a>
#### FixedAssetService.CategoryRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `assetAccount` | texte |  |  |
| `depreciationAccount` | texte |  |  |
| `expenseAccount` | texte |  |  |
| `impairmentAccount` | texte |  |  |
| `impairmentExpenseAccount` | texte |  |  |
| `impairmentReversalAccount` | texte |  |  |
| `disposalValueAccount` | texte |  |  |
| `disposalIncomeAccount` | texte |  |  |
| `method` | texte |  |  |
| `durationMonths` | entier |  |  |
| `coefficient` | nombre décimal |  |  |
| `threshold` | nombre décimal |  |  |
| `tracking` | texte |  |  |
| `trackOffBooks` | booléen |  |  |
| `taxMethod` | texte |  |  |
| `taxDurationMonths` | entier |  |  |
| `componentTypes` | texte |  |  |
| `vatRecoverable` | booléen |  |  |
| `inProgress` | booléen |  |  |
| `active` | booléen |  |  |
| `annualRate` | nombre décimal |  |  |

<a id="m-FixedAssetReportService-Finding"></a>
#### FixedAssetReportService.Finding

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `severity` | texte |  |  |
| `subject` | texte |  |  |
| `message` | texte |  |  |

<a id="m-FaDepreciationProfile"></a>
#### FaDepreciationProfile

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `label` | texte |  |  |
| `categoryCode` | texte |  |  |
| `brand` | texte |  |  |
| `modelPattern` | texte |  |  |
| `method` | texte |  |  |
| `annualRate` | nombre décimal |  |  |
| `durationMonths` | entier |  |  |
| `coefficient` | nombre décimal |  |  |
| `residualRate` | nombre décimal |  |  |
| `priority` | entier |  |  |
| `active` | booléen |  |  |
| `notes` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `updatedBy` | texte |  |  |
| `updatedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DepreciationProfileService-ProfileRequest"></a>
#### DepreciationProfileService.ProfileRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `categoryCode` | texte |  |  |
| `brand` | texte |  |  |
| `modelPattern` | texte |  |  |
| `method` | texte |  |  |
| `annualRate` | nombre décimal |  |  |
| `durationMonths` | entier |  |  |
| `coefficient` | nombre décimal |  |  |
| `residualRate` | nombre décimal |  |  |
| `priority` | entier |  |  |
| `active` | booléen |  |  |
| `notes` | texte |  |  |

<a id="m-DepreciationRunService-RunResult"></a>
#### DepreciationRunService.RunResult

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `period` | texte |  |  |
| `simulated` | booléen |  |  |
| `assets` | entier |  |  |
| `total` | nombre décimal |  |  |
| `derogatory` | nombre décimal |  |  |
| `subsidyRelease` | nombre décimal |  |  |
| `entries` | texte[] |  |  |
| `lines` | [DepreciationRunService.RunLine](#m-DepreciationRunService-RunLine)[] |  |  |
| `byCategory` | objet { clé : nombre décimal } |  |  |
| `bySection` | objet { clé : nombre décimal } |  |  |
| `warnings` | texte[] |  |  |

<a id="m-FixedAssetDisposal"></a>
#### FixedAssetDisposal

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `assetId` | entier int64 ⚠ |  |  |
| `voucherNumber` | texte |  |  |
| `reason` | texte |  |  |
| `disposalDate` | date AAAA-MM-JJ |  |  |
| `grossValue` | nombre décimal |  |  |
| `accumulatedDepreciation` | nombre décimal |  |  |
| `accumulatedImpairment` | nombre décimal |  |  |
| `netValue` | nombre décimal |  |  |
| `price` | nombre décimal |  |  |
| `vatRate` | nombre décimal |  |  |
| `vatAmount` | nombre décimal |  |  |
| `gainLoss` | nombre décimal |  |  |
| `vatRegularization` | nombre décimal |  |  |
| `buyerName` | texte |  |  |
| `justificationRef` | texte |  |  |
| `removeAmenity` | booléen |  |  |
| `status` | texte |  |  |
| `complementEntry` | texte |  |  |
| `exitEntry` | texte |  |  |
| `saleEntry` | texte |  |  |
| `collectedAmount` | nombre décimal |  |  |
| `indemnityAmount` | nombre décimal |  |  |
| `indemnityEntry` | texte |  |  |
| `billingDocumentId` | entier int64 ⚠ |  |  |
| `preparedBy` | texte |  |  |
| `validatedBy` | texte |  |  |
| `validatedAt` | date-heure ISO 8601 UTC |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FixedAssetService-CollectRequest"></a>
#### FixedAssetService.CollectRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `treasuryAccount` | texte |  |  |
| `journalCode` | texte |  |  |

<a id="m-FixedAssetService-IndemnityRequest"></a>
#### FixedAssetService.IndemnityRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `amount` | nombre décimal |  |  |
| `insurerAccount` | texte |  |  |

<a id="m-FixedAssetController-DecisionRequest"></a>
#### FixedAssetController.DecisionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `approve` | booléen |  |  |

<a id="m-FixedAssetInventoryCampaign"></a>
#### FixedAssetInventoryCampaign

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `code` | texte |  |  |
| `label` | texte |  |  |
| `site` | texte |  |  |
| `resourceCodes` | texte |  |  |
| `categoryCodes` | texte |  |  |
| `team` | texte |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `endDate` | date AAAA-MM-JJ |  |  |
| `status` | texte |  |  |
| `expectedCount` | entier |  |  |
| `seenCount` | entier |  |  |
| `missingCount` | entier |  |  |
| `foundCount` | entier |  |  |
| `mislocatedCount` | entier |  |  |
| `missingCodes` | texte |  |  |
| `mislocatedCodes` | texte |  |  |
| `decisions` | texte |  |  |
| `witness` | texte |  |  |
| `voucherNumber` | texte |  |  |
| `openedBy` | texte |  |  |
| `closedBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
| `closedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FixedAssetInventoryService-CampaignView"></a>
#### FixedAssetInventoryService.CampaignView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `campaign` | [FixedAssetInventoryCampaign](#m-FixedAssetInventoryCampaign) |  |  |
| `scans` | [FixedAssetInventoryScan](#m-FixedAssetInventoryScan)[] |  |  |
| `expected` | texte[] |  |  |
| `seen` | texte[] |  |  |
| `missing` | texte[] |  |  |
| `mislocated` | texte[] |  |  |
| `warnings` | texte[] |  |  |

<a id="m-FixedAssetInventoryService-CampaignRequest"></a>
#### FixedAssetInventoryService.CampaignRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `label` | texte |  |  |
| `site` | texte |  |  |
| `resourceCodes` | texte[] |  |  |
| `categoryCodes` | texte[] |  |  |
| `team` | texte[] |  |  |
| `startDate` | date AAAA-MM-JJ |  |  |
| `witness` | texte |  |  |

<a id="m-FixedAssetInventoryService-CloseRequest"></a>
#### FixedAssetInventoryService.CloseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `decisions` | texte |  |  |
| `witness` | texte |  |  |

<a id="m-FixedAssetInventoryService-ScanRequest"></a>
#### FixedAssetInventoryService.ScanRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assetCode` | texte |  |  |
| `unregisteredLabel` | texte |  |  |
| `site` | texte |  |  |
| `resourceCode` | texte |  |  |
| `locationLabel` | texte |  |  |
| `photoRef` | texte |  |  |

<a id="m-FixedAssetController-CodesRequest"></a>
#### FixedAssetController.CodesRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `codes` | texte[] |  |  |

<a id="m-FixedAssetService-AssignmentRequest"></a>
#### FixedAssetService.AssignmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `holder` | texte |  |  |
| `condition` | texte |  |  |
| `accessories` | texte |  |  |

<a id="m-FixedAssetService-CommissionRequest"></a>
#### FixedAssetService.CommissionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `site` | texte |  |  |
| `resourceCode` | texte |  |  |
| `locationLabel` | texte |  |  |
| `holder` | texte |  |  |
| `organisation` | texte |  |  |
| `targetCategoryCode` | texte |  |  |
| `analytic` | objet { clé : texte } |  |  |
| `notes` | texte |  |  |

<a id="m-FixedAssetService-ComponentInput"></a>
#### FixedAssetService.ComponentInput

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `designation` | texte |  |  |
| `value` | nombre décimal |  |  |
| `durationMonths` | entier |  |  |
| `categoryCode` | texte |  |  |
| `method` | texte |  |  |

<a id="m-DepreciationProfileService-Resolution"></a>
#### DepreciationProfileService.Resolution

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assetCode` | texte |  |  |
| `method` | texte |  |  |
| `durationMonths` | entier |  |  |
| `residualRate` | nombre décimal |  |  |
| `profile` | [FaDepreciationProfile](#m-FaDepreciationProfile) |  |  |

<a id="m-FixedAssetService-DisposalRequest"></a>
#### FixedAssetService.DisposalRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `price` | nombre décimal |  |  |
| `vatRate` | nombre décimal |  |  |
| `buyerName` | texte |  |  |
| `justificationRef` | texte |  |  |
| `removeAmenity` | booléen |  |  |

<a id="m-FixedAssetService-ImpairmentRequest"></a>
#### FixedAssetService.ImpairmentRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `kind` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `currentValue` | nombre décimal |  |  |
| `indicator` | texte |  |  |
| `justificationRef` | texte |  |  |

<a id="m-FixedAssetController-InventoryLinkRequest"></a>
#### FixedAssetController.InventoryLinkRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `inventoryCodes` | texte[] |  |  |
| `unlink` | booléen |  |  |

<a id="m-FixedAssetController-LeaseRequest"></a>
#### FixedAssetController.LeaseRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `loanCode` | texte |  |  |

<a id="m-FixedAssetService-MaintenanceRequest"></a>
#### FixedAssetService.MaintenanceRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `kind` | texte |  |  |
| `provider` | texte |  |  |
| `contractRef` | texte |  |  |
| `cost` | nombre décimal |  |  |
| `supplierInvoiceNumber` | texte |  |  |
| `description` | texte |  |  |

<a id="m-FixedAssetController-MeterRequest"></a>
#### FixedAssetController.MeterRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `period` | texte |  |  |
| `units` | nombre décimal |  |  |

<a id="m-FixedAssetService-OperationalRequest"></a>
#### FixedAssetService.OperationalRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `status` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |

<a id="m-FixedAssetService-PlanView"></a>
#### FixedAssetService.PlanView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `code` | texte |  |  |
| `kind` | texte |  |  |
| `versions` | [FixedAssetPlan](#m-FixedAssetPlan)[] |  |  |
| `current` | [FixedAssetSchedule](#m-FixedAssetSchedule)[] |  |  |
| `history` | [FixedAssetSchedule](#m-FixedAssetSchedule)[] |  |  |
| `byYear` | objet { clé : nombre décimal } |  |  |

<a id="m-FixedAssetService-ReclassRequest"></a>
#### FixedAssetService.ReclassRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `categoryCode` | texte |  |  |
| `date` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |

<a id="m-FixedAssetService-RevisionRequest"></a>
#### FixedAssetService.RevisionRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `reason` | texte |  |  |
| `comment` | texte |  |  |
| `durationMonths` | entier |  |  |
| `residualValue` | nombre décimal |  |  |
| `method` | texte |  |  |
| `coefficient` | nombre décimal |  |  |
| `totalUnits` | nombre décimal |  |  |
| `taxMethod` | texte |  |  |
| `taxDurationMonths` | entier |  |  |

<a id="m-FixedAssetService-TransferRequest"></a>
#### FixedAssetService.TransferRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `site` | texte |  |  |
| `resourceCode` | texte |  |  |
| `locationLabel` | texte |  |  |
| `organisation` | texte |  |  |
| `analytic` | objet { clé : texte } |  |  |
| `reason` | texte |  |  |

<a id="m-FixedAssetService-VatRegularizationRequest"></a>
#### FixedAssetService.VatRegularizationRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `date` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |
| `amount` | nombre décimal |  |  |

<a id="m-FixedAssetValueLine"></a>
#### FixedAssetValueLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `assetId` | entier int64 ⚠ |  |  |
| `lineType` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `valueDate` | date AAAA-MM-JJ |  |  |
| `origin` | texte |  |  |
| `entryNumber` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FixedAssetSchedule"></a>
#### FixedAssetSchedule

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `planId` | entier int64 ⚠ |  |  |
| `assetId` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `version` | entier |  |  |
| `periodCode` | texte |  |  |
| `planned` | nombre décimal |  |  |
| `posted` | nombre décimal |  |  |
| `cumulative` | nombre décimal |  |  |
| `netValue` | nombre décimal |  |  |
| `status` | texte |  |  |
| `entryNumber` | texte |  |  |
| `postedInPeriod` | texte |  |  |
| `note` | texte |  |  |
| `postedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FixedAssetEvent"></a>
#### FixedAssetEvent

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `assetId` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `eventDate` | date AAAA-MM-JJ |  |  |
| `beforeValue` | texte |  |  |
| `afterValue` | texte |  |  |
| `reason` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `voucherNumber` | texte |  |  |
| `entryNumber` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FixedAssetImpairment"></a>
#### FixedAssetImpairment

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `assetId` | entier int64 ⚠ |  |  |
| `kind` | texte |  |  |
| `impairmentDate` | date AAAA-MM-JJ |  |  |
| `indicator` | texte |  |  |
| `currentValue` | nombre décimal |  |  |
| `amount` | nombre décimal |  |  |
| `justificationRef` | texte |  |  |
| `status` | texte |  |  |
| `requestedBy` | texte |  |  |
| `approvedBy` | texte |  |  |
| `entryNumber` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FixedAssetMaintenance"></a>
#### FixedAssetMaintenance

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `assetId` | entier int64 ⚠ |  |  |
| `maintenanceDate` | date AAAA-MM-JJ |  |  |
| `kind` | texte |  |  |
| `provider` | texte |  |  |
| `contractRef` | texte |  |  |
| `cost` | nombre décimal |  |  |
| `underWarranty` | booléen |  |  |
| `supplierInvoiceNumber` | texte |  |  |
| `description` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-DepreciationRunService-RunLine"></a>
#### DepreciationRunService.RunLine

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `assetCode` | texte |  |  |
| `designation` | texte |  |  |
| `categoryCode` | texte |  |  |
| `sections` | texte |  |  |
| `period` | texte |  |  |
| `amount` | nombre décimal |  |  |
| `catchUp` | booléen |  |  |
| `estimated` | booléen |  |  |

<a id="m-FixedAssetInventoryScan"></a>
#### FixedAssetInventoryScan

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `campaignId` | entier int64 ⚠ |  |  |
| `assetId` | entier int64 ⚠ |  |  |
| `assetCode` | texte |  |  |
| `unregisteredLabel` | texte |  |  |
| `site` | texte |  |  |
| `resourceCode` | texte |  |  |
| `locationLabel` | texte |  |  |
| `photoRef` | texte |  |  |
| `mislocated` | booléen |  |  |
| `holderAlert` | booléen |  |  |
| `scannedBy` | texte |  |  |
| `scannedAt` | date-heure ISO 8601 UTC |  |  |

<a id="m-FixedAssetPlan"></a>
#### FixedAssetPlan

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `assetId` | entier int64 ⚠ |  |  |
| `version` | entier |  |  |
| `kind` | texte |  |  |
| `method` | texte |  |  |
| `durationMonths` | entier |  |  |
| `rate` | nombre décimal |  |  |
| `coefficient` | nombre décimal |  |  |
| `base` | nombre décimal |  |  |
| `validFrom` | date AAAA-MM-JJ |  |  |
| `reason` | texte |  |  |
| `createdBy` | texte |  |  |
| `createdAt` | date-heure ISO 8601 UTC |  |  |
