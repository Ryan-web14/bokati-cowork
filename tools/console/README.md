# Console de test

Interface locale pour tester l'API à l'œil. Elle ne fait pas partie de l'application et ne part jamais
en production. Aucune installation : seulement Node.

## Lancer

1. Démarre l'application sur le port 8080, profil `dev`.
2. Depuis la racine du projet : `node tools/console/server.js`
3. Ouvre <http://localhost:5199> et connecte-toi avec un compte de ta base de développement.

Les actions agissent sur cette base.

| Variable | Rôle | Valeur par défaut |
| --- | --- | --- |
| `CONSOLE_PORT` | port de la console | `5199` |
| `CONSOLE_API` | adresse de l'API | `http://localhost:8080` |

## Ce qu'elle contient

- **Écrans dédiés** : paramètres, horaires des ressources, marketplace (administration et portail),
  contrats et versions, renouvellement d'abonnement, lots de coupons (générer, suivre, exporter en CSV,
  révoquer), parrainage côté administration (programmes, liens des parrains, parrainages, page publique
  d'un lien) et côté portail (son lien, ses parrainages : se connecter avec un membre), explorateur de la
  GED (parcourir, déposer, numériser, chercher dans le contenu, accès restreint d'un dossier, verrou,
  corbeille et destruction, gels juridiques, fichiers à détruire, espace et quotas).
- **Tous les modules** : le menu se construit à la connexion depuis `/v3/api-docs`. Chaque module liste
  ses routes, avec un formulaire généré et sa réponse en tableau, en fiche ou en JSON. Les listes se
  lisent à l'ouverture.

Les routes du portail client demandent un compte membre : déconnecte-toi et reconnecte-toi avec un
membre pour les essayer.

## Tester la console

```bash
node --test tools/console/test/spec.test.mjs   # la lecture des routes et des schémas
node tools/console/test/mock-api.js            # la console sur un faux serveur, port 5198
```

## Limites

- Les écrans générés sont fonctionnels, pas soignés, et ne suivent pas les parcours métier.
- Un champ complexe d'un formulaire se saisit en JSON.
- Le menu dépend de `/v3/api-docs` : si cette route échoue, seuls les écrans dédiés restent disponibles.
