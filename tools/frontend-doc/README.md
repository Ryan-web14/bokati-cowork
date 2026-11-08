# Génération de la doc frontend

Régénère les fichiers de routes de [docs/final-frontend-doc](../../docs/final-frontend-doc/README.md) à partir du code. Cet outil ne fait pas partie de l'application.

| Fichier | Rôle |
| --- | --- |
| `RouteCatalog.java` | Lit les classes compilées et produit l'inventaire en JSON : chaque route, son accès calculé par les vraies règles de sécurité (`PublicPaths`, `AdminApiAuthorizationManager`, `@PreAuthorize`), ses paramètres, son corps, sa réponse, l'en-tête d'idempotence, et le schéma de chaque modèle. |
| `render.py` | Transforme cet inventaire en `MODULES.md`, `<module>/routes.md`, `<module>/routes/<Contrôleur>.md` et `<module>/routes.json`. |
| `generate.py` | Enchaîne les deux. |

Les `README.md` de la doc sont rédigés à la main : la génération ne les touche pas.

## Lancer

Il faut Python 3, le JDK utilisé par le projet (`JAVA_HOME`) et le code compilé.

**Ne pas compiler dans le dossier où l'IDE fait tourner l'application** : la compilation réécrit `target/classes` pendant que le serveur s'en sert. Utiliser un second dossier de travail :

```bash
git worktree add --detach ../bkw-doc HEAD
cd ../bkw-doc
./mvnw -o -q compile
python tools/frontend-doc/generate.py <chemin du dépôt principal>/docs/final-frontend-doc
```

Sans argument, `generate.py` écrit dans le `docs/final-frontend-doc` du dossier où il est lancé.

## Limites connues

- Les schémas JSON sont lus avec la bibliothèque de Swagger, qui s'appuie sur Jackson 2. L'application sérialise avec Jackson 3 : les annotations Jackson 2 de `com.fasterxml.jackson.annotation` (`@JsonProperty`, `@JsonIgnore`…) sont communes aux deux et bien prises en compte. Parmi les annotations propres à Jackson 3, seule `@JsonSerialize(using = ToStringSerializer.class)` est reconnue, et son champ est décrit en texte.
- Une route `Object` ou `ResponseEntity<?>` est décrite « non typé, voir le code ».
- Les routes qui renvoient une page HTML (pages servies par le serveur) sont listées sans modèle.
