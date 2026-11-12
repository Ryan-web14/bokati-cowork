# SupportTagController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [support](../README.md) · 1 route(s).

## Routes

<a id="r-get-sni-api-v1-support-tags"></a>
### GET /sni/api/v1/support/tags

- Accès : back-office : rôle d'administration et droit `SUPPORT:READ` ; contrôle supplémentaire sur la méthode : `hasAnyAuthority('SUPPORT:READ','SUPPORT_READ')`
- Réponse : `List<SupportDtos.TagResponse>`
- Modèles : [SupportDtos.TagResponse](#m-SupportDtos-TagResponse)
- Code : `src/main/java/com/sni/bokaticowork/features/support/controller/SupportTagController.java`, méthode `list`

## Modèles

Les champs JSON de chaque type cité plus haut, et des types qu'ils contiennent.

<a id="m-SupportDtos-TagResponse"></a>
#### SupportDtos.TagResponse

| Champ | Type | Requis | Contraintes |
| --- | --- | --- | --- |
| `id` | entier int64 ⚠ |  |  |
| `name` | texte |  |  |
