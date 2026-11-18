# Lot R4 : listes, recherche, performances

> Partie du [plan ressources](README.md). Fait le 18/11/2026 sur `feature/ressources`.

## Filtres

- `GET /resources?typeCode=&groupCode=&policyCode=` : les filtres valent aussi pour la liste détaillée (`summary=false`). Avant, ils n'étaient lus qu'en résumé. Les trois codes se combinent en détail ; en résumé, le premier présent s'applique, comme avant.
- `POST /resources/search` : nouveaux critères `typeCode`, `groupCode`, `policyCode`, sans tenir compte de la casse. `typeId`, `groupId` et `policyId` restent acceptés (codes réversibles depuis le 17/11).

## Recherche simple paginée

`GET /resources/search/basic?query=&page=&size=` rend désormais une page (`PaginatedResponse`, 20 par défaut), comme les recherches des types, groupes, politiques et équipements. **Changement de forme** : avant, une liste nue.

## Performances

Les listes de ressources (`findAll` paginé, recherche par critères, listes par type, groupe et politique) chargent le type, le groupe et la politique avec la page (graphe d'entités) : une requête au lieu d'une par ressource.

## Nouvel essai

Les créations et modifications des ressources, types, politiques et équipements n'enveloppent plus l'enregistrement dans un nouvel essai : il tournait dans la transaction de la méthode, où `save` ne touche pas la base avant la validation et où une erreur de base rend la transaction irrécupérable. Les erreurs remontent telles quelles.

## Créations en 201 avec l'objet créé

`POST` des ressources, types, groupes, politiques, équipements, fermetures et règles tarifaires : `201` avec l'objet créé (code compris), au lieu d'un corps vide. Le frontend n'a plus à relire la liste pour connaître le code.

## Identifiants des fermetures et créneaux

Réglé par les codes réversibles du 17/11 : les identifiants sortent en codes texte et sont acceptés tels quels dans les chemins (`/resource-closures/{id}`, `/resource-availabilities/{id}`). Pas de route supplémentaire.

## Tests

`ResourceR4Test` (filtre par code de type, graphe d'entités) ; `ResourceAmenitiesServiceImplTest` adapté (enregistrement direct).
