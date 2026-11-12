# VisitorManagementController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [visitor](../README.md) · 7 route(s).

## Routes

<a id="r-get-sni-api-v1-visitors-log"></a>
### GET /sni/api/v1/visitors/log

- Accès : back-office : rôle d'administration et droit `VISITOR:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('VISITOR:READ','VISITOR_READ')`
- Réponse : `List<VisitorDtos.VisitorLogResponse>`
- Modèles : [VisitorDtos.VisitorLogResponse](#m-VisitorDtos-VisitorLogResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/visitor/controller/VisitorManagementController.java`, méthode `log`

<a id="r-get-sni-api-v1-visitors-passes"></a>
### GET /sni/api/v1/visitors/passes

- Accès : back-office : rôle d'administration et droit `VISITOR:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('VISITOR:READ','VISITOR_READ')`
- Réponse : `List<VisitorDtos.VisitorPassResponse>`
- Modèles : [VisitorDtos.VisitorPassResponse](#m-VisitorDtos-VisitorPassResponse), [VisitorPassStatus](#m-VisitorPassStatus)
- Code : `src/main/java/com/sni/bokaticowork/features/visitor/controller/VisitorManagementController.java`, méthode `list`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `status` | requête | `VisitorPassStatus` |  |  |

<a id="r-post-sni-api-v1-visitors-passes"></a>
### POST /sni/api/v1/visitors/passes

- Accès : back-office : rôle d'administration et droit `VISITOR:WRITE` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('VISITOR:WRITE','VISITOR_WRITE')`
- Corps JSON (obligatoire) : `VisitorDtos.CreateVisitorPassRequest`
- Réponse : `VisitorDtos.VisitorPassResponse`
- Modèles : [VisitorDtos.VisitorPassResponse](#m-VisitorDtos-VisitorPassResponse), [VisitorDtos.CreateVisitorPassRequest](#m-VisitorDtos-CreateVisitorPassRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/visitor/controller/VisitorManagementController.java`, méthode `create`

<a id="r-get-sni-api-v1-visitors-passes-today"></a>
### GET /sni/api/v1/visitors/passes/today

- Accès : back-office : rôle d'administration et droit `VISITOR:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('VISITOR:READ','VISITOR_READ')`
- Réponse : `List<VisitorDtos.VisitorPassResponse>`
- Modèles : [VisitorDtos.VisitorPassResponse](#m-VisitorDtos-VisitorPassResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/visitor/controller/VisitorManagementController.java`, méthode `today`

<a id="r-get-sni-api-v1-visitors-passes-passnumber-badge"></a>
### GET /sni/api/v1/visitors/passes/{passNumber}/badge

- Accès : back-office : rôle d'administration et droit `VISITOR:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('VISITOR:READ','VISITOR_READ')`
- Réponse : `fichier (octets)`
- Code : `src/main/java/com/sni/bokaticowork/features/visitor/controller/VisitorManagementController.java`, méthode `badge`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-visitors-passnumber-check-in"></a>
### POST /sni/api/v1/visitors/{passNumber}/check-in

- Accès : back-office : rôle d'administration et droit `VISITOR:CHECKIN` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('VISITOR:CHECKIN','VISITOR_CHECKIN')`
- Corps JSON (obligatoire) : `VisitorDtos.CheckInRequest`
- Réponse : `VisitorDtos.VisitorPassResponse`
- Modèles : [VisitorDtos.VisitorPassResponse](#m-VisitorDtos-VisitorPassResponse), [VisitorDtos.CheckInRequest](#m-VisitorDtos-CheckInRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/visitor/controller/VisitorManagementController.java`, méthode `checkIn`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

<a id="r-post-sni-api-v1-visitors-passnumber-check-out"></a>
### POST /sni/api/v1/visitors/{passNumber}/check-out

- Accès : back-office : rôle d'administration et droit `VISITOR:CHECKIN` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('VISITOR:CHECKIN','VISITOR_CHECKIN')`
- Corps JSON (obligatoire) : `VisitorDtos.CheckInRequest`
- Réponse : `VisitorDtos.VisitorPassResponse`
- Modèles : [VisitorDtos.VisitorPassResponse](#m-VisitorDtos-VisitorPassResponse), [VisitorDtos.CheckInRequest](#m-VisitorDtos-CheckInRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/visitor/controller/VisitorManagementController.java`, méthode `checkOut`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `passNumber` | chemin | `String` | oui |  |

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-VisitorDtos-VisitorLogResponse"></a>
#### VisitorDtos.VisitorLogResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `visitorName` | texte |  |  |
| `hostName` | texte |  |  |
| `checkedInAt` | date-heure ISO 8601 UTC |  |  |
| `checkedOutAt` | date-heure ISO 8601 UTC |  |  |
| `checkInAgent` | texte |  |  |
| `checkOutAgent` | texte |  |  |
| `notes` | texte |  |  |

<a id="m-VisitorDtos-VisitorPassResponse"></a>
#### VisitorDtos.VisitorPassResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `passNumber` | texte |  |  |
| `fullName` | texte |  |  |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `company` | texte |  |  |
| `hostMemberCode` | texte |  |  |
| `hostName` | texte |  |  |
| `validFrom` | date-heure ISO 8601 UTC |  |  |
| `validUntil` | date-heure ISO 8601 UTC |  |  |
| `purpose` | texte |  |  |
| `status` | enum : `SCHEDULED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`, `EXPIRED` |  |  |
| `qrValue` | texte |  |  |

<a id="m-VisitorPassStatus"></a>
#### VisitorPassStatus

Énumération : `SCHEDULED`, `CHECKED_IN`, `CHECKED_OUT`, `CANCELLED`, `EXPIRED`

<a id="m-VisitorDtos-CreateVisitorPassRequest"></a>
#### VisitorDtos.CreateVisitorPassRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fullName` | texte | oui | longueur min `1` |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `company` | texte |  |  |
| `hostMemberCode` | texte |  |  |
| `hostName` | texte |  |  |
| `validFrom` | texte | oui | longueur min `1` |
| `validUntil` | texte | oui | longueur min `1` |
| `purpose` | texte |  |  |
| `createdBy` | texte |  |  |

<a id="m-VisitorDtos-CheckInRequest"></a>
#### VisitorDtos.CheckInRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `agent` | texte |  |  |
| `notes` | texte |  |  |
