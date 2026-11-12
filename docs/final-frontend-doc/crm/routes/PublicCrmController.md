# PublicCrmController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [crm](../README.md) · 1 route(s).

## Routes

<a id="r-post-sni-api-v1-public-crm-leads"></a>
### POST /sni/api/v1/public/crm/leads

- Accès : public, sans jeton
- Corps JSON (obligatoire) : `CrmDtos.CreateLeadRequest`
- Réponse : `Map<String, String>`
- Modèles : [CrmDtos.CreateLeadRequest](#m-CrmDtos-CreateLeadRequest)
- Code : `src/main/java/com/sni/bokaticowork/features/crm/controller/PublicCrmController.java`, méthode `submitContactForm`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-CrmDtos-CreateLeadRequest"></a>
#### CrmDtos.CreateLeadRequest

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `fullName` | texte | oui | longueur min `1` |
| `email` | texte |  |  |
| `phone` | texte |  |  |
| `company` | texte |  |  |
| `note` | texte |  |  |
| `source` | enum : `WEBSITE`, `REFERRAL`, `COLD_CALL`, `TRADE_SHOW`, `PARTNER`, `SOCIAL_MEDIA`, `EVENT`, `OTHER` |  |  |
| `interest` | enum : `WORKSPACE`, `MEETING_ROOM`, `VIRTUAL_OFFICE`, `DOMICILIATION`, `DAY_PASS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `OTHER` |  |  |
| `estimatedAmount` | nombre décimal |  |  |
| `probability` | entier |  |  |
| `expectedCloseDate` | date AAAA-MM-JJ |  |  |
| `assignedTo` | entier int64 ⚠ |  |  |
