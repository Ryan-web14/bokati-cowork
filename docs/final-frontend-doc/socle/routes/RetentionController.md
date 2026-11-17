# RetentionController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 3 route(s).

## Routes

<a id="r-get-sni-api-v1-admin-audit-integrity"></a>
### GET /sni/api/v1/admin/audit/integrity

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `List<AuditIntegrity.Report>`
- Modèles : [AuditIntegrity.Report](#m-AuditIntegrity-Report)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/retention/RetentionController.java`, méthode `integrity`

<a id="r-get-sni-api-v1-admin-audit-retention"></a>
### GET /sni/api/v1/admin/audit/retention

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `List<AuditRetention.TargetView>`
- Modèles : [AuditRetention.TargetView](#m-AuditRetention-TargetView)
- Code : `src/main/java/com/sni/bokaticowork/core/audit/retention/RetentionController.java`, méthode `retention`

<a id="r-post-sni-api-v1-admin-audit-retention-run"></a>
### POST /sni/api/v1/admin/audit/retention/run

- Accès : back-office : rôle d'administration et droit `SYSTEM:AUDIT`
- Réponse : `Map<String, Long>`
- Code : `src/main/java/com/sni/bokaticowork/core/audit/retention/RetentionController.java`, méthode `run`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-AuditIntegrity-Report"></a>
#### AuditIntegrity.Report

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `journal` | texte |  |  |
| `checked` | entier int64 (texte au-delà de 2^53) |  |  |
| `firstSeq` | entier int64 (texte au-delà de 2^53) |  |  |
| `lastSeq` | entier int64 (texte au-delà de 2^53) |  |  |
| `unchained` | entier int64 (texte au-delà de 2^53) |  |  |
| `intact` | booléen |  |  |
| `breaks` | [AuditIntegrity.Break](#m-AuditIntegrity-Break)[] |  |  |

<a id="m-AuditRetention-TargetView"></a>
#### AuditRetention.TargetView

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `name` | texte |  |  |
| `table` | texte |  |  |
| `settingKey` | texte |  |  |
| `days` | entier int64 (texte au-delà de 2^53) |  |  |
| `rows` | entier int64 (texte au-delà de 2^53) |  |  |
| `oldest` | date-heure ISO 8601 UTC |  |  |

<a id="m-AuditIntegrity-Break"></a>
#### AuditIntegrity.Break

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `chainSeq` | entier int64 (texte au-delà de 2^53) |  |  |
| `kind` | texte |  |  |
