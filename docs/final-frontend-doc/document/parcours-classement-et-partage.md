# document : recherche, classement, partage et journaux

> Partie du [README du module](README.md). Routes exactes : [routes/DocumentController.md](routes/DocumentController.md), [routes/DocumentSpaceController.md](routes/DocumentSpaceController.md), [routes/DocumentFolderController.md](routes/DocumentFolderController.md), [routes/DocumentTagController.md](routes/DocumentTagController.md), [routes/DocumentPermissionController.md](routes/DocumentPermissionController.md), [routes/DocumentShareController.md](routes/DocumentShareController.md).

## Lister et rechercher

### Liste simple

`GET /sni/api/v1/documents?ownerType=MEMBER&ownerCode=MBR…` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents)), droit `DOCUMENT:READ`, tri par défaut `uploadedAt` décroissant.

- Exclut les documents `REJECTED`.
- `ownerCode` n'est pris en compte qu'avec `ownerType` ; sans les deux, tous les documents.
- Les lignes n'ont pas `versions[]` (liste vide) ; étiquettes et métadonnées sont incluses.

### Recherche à facettes

`GET /sni/api/v1/documents/search` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-search)), droit `DOCUMENT:READ`, 20 par page, tri `uploadedAt` décroissant.

| Filtre | Effet |
| --- | --- |
| `q` | contient (sans casse) dans le titre ou la description ; ne cherche ni dans le code ni dans le nom de fichier |
| `space`, `category`, `ownerType` | égalité |
| `ownerCode` | avec `ownerType` seulement (code membre, client, entreprise ; numérique pour les autres) |
| `statuses` | répétable (`statuses=APPROVED&statuses=SIGNED`) ; inclut `REJECTED` si demandé |
| `tags` | codes d'étiquettes, répétable ; au moins une correspondance |
| `expiresInDays` | expiration entre aujourd'hui et aujourd'hui + N jours |
| `isExpired=true` | statut `EXPIRED` |
| `uploadedAfter`, `uploadedBefore` | dates `AAAA-MM-JJ`, bornes incluses, jour calculé en UTC |
| `metaKey`, `metaValue` | métadonnée présente (et égale à la valeur si fournie) |

Chaque ligne est un `DocumentResponse` complet (versions comprises) : la page coûte une requête par document, garder `size` raisonnable.

Les listes par espace `GET /sni/api/v1/spaces/<espace>/documents` ([routes](routes/DocumentSpaceController.md)), droit `ADMIN:ACCESS`, sont la même recherche limitée à l'espace avec `ownerType`, `ownerCode`, `statuses`, `q` (et `uploadedAfter`/`uploadedBefore` pour `financial`).

## Tableaux de bord et statistiques

- `GET /sni/api/v1/documents/dashboard?space=…` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-dashboard)), droit `DOCUMENT:READ` ; et `GET /sni/api/v1/spaces/<espace>/dashboard` (droit `ADMIN:ACCESS`). Contenu : `total`, `byStatus`, `byCategory` (statuts et catégories à zéro omis), `pendingReviewOlderThan48h` (selon la dernière modification), `needsCorrectionCount`, `expiringIn30Days`, `rejectionRate30d` (pourcentage, une décimale, **tous espaces confondus**), `avgReviewTimeHours` (tous espaces), `topTags` (5 étiquettes les plus utilisées).
- `GET /sni/api/v1/documents/analytics?space=…&months=12` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-analytics)), droit `DOCUMENT:REVIEW` (porté aussi par `@PreAuthorize`). `months` borné entre 1 et 24 (un mois = 30 jours). Contenu : compteurs, `byStatus`, `bySpace` (toujours tous espaces), `byCategory`, taux de correction et de refus sur 30 jours, délai moyen de revue, séries mensuelles `uploadsByMonth`, `approvalsByMonth`, `rejectionsByMonth`, `correctionsByMonth` (`period` au format `AAAA-MM`, mois sans valeur absents : compléter les trous côté graphique).

## Vue d'un propriétaire

`GET /sni/api/v1/documents/folder?ownerType=MEMBER&ownerCode=MBR…` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-folder)), droit `DOCUMENT:READ`. Les deux paramètres sont obligatoires.

- `owner` : type, code, nom.
- `spaces[]` : par espace non vide, `totalDocuments`, `approvedDocuments`, `completionRate` (pourcentage d'`APPROVED`), `documents[]` (tous statuts, refusés compris).
- `missingRequirements[]` : exigences globales obligatoires sans document valide (ni refusé, ni expiré, ni archivé) ; sans filtre par type de client ni forme juridique.
- `expiringSoon[]` : documents qui expirent sous 30 jours.

C'est l'écran « Documents » à intégrer dans la fiche d'un membre, d'un client ou d'une entreprise.

## Export CSV

`GET /sni/api/v1/documents/export/csv` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-export-csv)), droit `DOCUMENT:REVIEW`. Mêmes filtres que la recherche sauf `q`, `expiresInDays`, `isExpired`, métadonnées. Fichier `documents-export.csv` (UTF-8, séparateur virgule, sans BOM), 10 000 lignes au plus, colonnes `code,title,ownerType,ownerId,space,category,documentTypeCode,status,uploadedAt,expiryDate`. Les cellules commençant par `=`, `+`, `-`, `@` sont préfixées d'une apostrophe. Télécharger par `fetch` puis `blob`.

## Journal d'accès

- Par document : `GET /sni/api/v1/documents/{code}/access-logs` ([route](routes/DocumentController.md#r-get-sni-api-v1-documents-code-access-logs)), droit `DOCUMENT:REVIEW`, plus récent d'abord.
- Par utilisateur : `GET /sni/api/v1/documents/access-logs/by-user?userId=…` (int64).
- Chaque entrée : `documentCode`, `userId` (0 pour un accès sans compte, par aperçu signé), `action` (`PREVIEW` ou `DOWNLOAD`), `accessedAt`, `ipAddress`, `userAgent`. Seuls les aperçus et téléchargements sont journalisés, pas les consultations de fiche.

## Dossiers de classement

Toutes les routes `/sni/api/v1/document-folders/**` exigent `ADMIN:ACCESS` ([routes](routes/DocumentFolderController.md)).

1. Racines : `GET /document-folders/roots?space=…` (toutes les racines si `space` absent), triées par `sortOrder` puis nom.
2. Naviguer : `GET /document-folders/{code}/children`, `GET /document-folders/{code}/breadcrumb` (de la racine au dossier), `GET /document-folders/{code}/tree` (arbre complet avec `documentsCount` par nœud).
3. Contenu : `GET /document-folders/{code}/documents` (paginé, 20, `uploadedAt` décroissant). Ces lignes sont **allégées** : ni `documentTypeCode`, ni `previewUrl`, ni étiquettes. Ouvrir la fiche pour le reste.
4. Créer : `POST /document-folders`, corps `{ "name": "…", "description": "…", "parentCode": "FLD…", "space": "CONTRACT_SPACE", "ownerType": "MEMBER", "ownerCode": "MBR…", "color": "…", "icon": "…" }`. Seul `name` est obligatoire. Avec `parentCode`, l'espace du parent s'impose. 10 niveaux au plus (`400` « Maximum folder depth (10) exceeded »). Nom unique sous un même parent (`400` « A folder with this name already exists in the same parent »). Un `ownerCode` inconnu est ignoré sans erreur.
5. Modifier : `PATCH /document-folders/{code}`, champs facultatifs `name`, `description`, `color`, `icon`, `sortOrder`.
6. Déplacer : `POST /document-folders/{code}/move`, corps `{ "targetParentCode": "FLD…" }` (vide ou absent : à la racine). Interdit vers un descendant (`400` « Cannot move a folder into one of its descendants »). L'espace du nouveau parent s'impose au dossier déplacé (pas à ses sous-dossiers).
7. Supprimer : `DELETE /document-folders/{code}`, refusé s'il contient des sous-dossiers ou des documents (`400`). Suppression définitive.
8. Ranger un document : `POST /sni/api/v1/document-folders/{documentCode}/move-document` (le chemin porte le **code du document**), corps `{ "folderCode": "FLD…" }` ou `{ "folderCode": null }` pour le sortir de tout dossier.

`DocumentResponse.folderCode` n'étant jamais rempli, l'écran ne peut pas savoir dans quel dossier se trouve un document depuis sa fiche : passer par la navigation dans l'arborescence.

## Étiquettes

Catalogue (`/sni/api/v1/document-tags/**`, droit `ADMIN:ACCESS`, [routes](routes/DocumentTagController.md)) :

1. Lister : `GET /document-tags?space=…` : étiquettes de l'espace **et** étiquettes globales (`space` nul), triées par libellé.
2. Créer : `POST /document-tags`, corps `{ "code": "contrat-signe", "label": "Contrat signé", "color": "#3B82F6", "space": "CONTRACT_SPACE" }`. `code` : minuscules, chiffres et tirets, 80 caractères au plus ; `color` : `#RRGGBB`. Code déjà pris : `400` « Tag with code 'x' already exists ». L'espace n'est plus modifiable ensuite.
3. Modifier : `PUT /document-tags/{code}`, corps `{ "label": "…", "color": "…" }` (`color` vide efface la couleur).
4. Supprimer : `DELETE /document-tags/{code}` ; refusé si l'étiquette est assignée (`400` « Tag 'x' is in use and cannot be deleted »).

Sur un document :

1. Lire : `GET /sni/api/v1/documents/{code}/tags` (ou `tags[]` de la fiche).
2. Assigner : `POST /sni/api/v1/documents/{code}/tags`, corps `{ "tagCodes": ["contrat-signe"] }`, droit `DOCUMENT:UPLOAD`. Ajoute sans retirer les existantes. Envoyer les codes en minuscules et sans doublon : sinon `400` « One or more tag codes not found ». Réponse : toutes les étiquettes du document.
3. Retirer : `DELETE /sni/api/v1/documents/{code}/tags/{tagCode}`, `204`.

## Métadonnées

1. Lire : `GET /sni/api/v1/documents/{code}/metadata` : objet `{ "cle": "valeur" }` (aussi dans `metadata` de la fiche).
2. Écrire : `PATCH /sni/api/v1/documents/{code}/metadata`, corps objet `{ "numeroBail": "B-2026-14", "etage": "3" }` : chaque clé est créée ou remplacée, les autres sont conservées. Clés vides ignorées. Valeurs en texte. Réponse : toutes les métadonnées.
3. Supprimer une clé : `DELETE /sni/api/v1/documents/{code}/metadata/{key}`, `204` même si la clé n'existait pas.
4. La recherche filtre par `metaKey`/`metaValue`.

## Droits d'accès

Les droits se posent sur les dossiers (dossiers restreints de l'explorateur, voir [explorateur](explorateur/README.md)). Les anciennes permissions par document (`/document-permissions`) sont **retirées** (lot KG5) : pour un fichier confidentiel, le ranger dans un dossier restreint.

## Liens de partage (internes)

Un lien de partage sert à désigner un document ou un dossier à un collègue de l'équipe : il exige la connexion et les droits du compte sur le document ou le dossier (dossiers restreints compris). Il n'y a **aucun accès public** (lot KG5).

1. Créer pour un document : `POST /sni/api/v1/documents/{documentCode}/share` ([route](routes/DocumentShareController.md#r-post-sni-api-v1-documents-documentcode-share)), droit `DOCUMENT:UPLOAD` et téléchargement permis sur le document ; pour un dossier : `POST /sni/api/v1/document-folders/{folderCode}/share`. Corps : `{ "expiresInHours": 48, "password": "…", "allowDownload": true, "maxAccessCount": 10 }` (`expiresInHours` obligatoire). Réponse `201` `ShareLinkResponse` : `token`, `shareUrl`, `documentCode` ou `folderCode`, `allowDownload`, `passwordProtected`, `maxAccessCount`, `accessCount`, `expiresAt`, `createdAt`, `createdBy`, `createdByName` (le courriel de l'auteur n'est plus renvoyé), `active`.
2. Lister les liens actifs d'un document : `GET /sni/api/v1/documents/{documentCode}/shares` (droit `DOCUMENT:READ`).
3. Ouvrir un lien : `GET /sni/api/v1/shares/{token}` ([route](routes/DocumentShareController.md#r-get-sni-api-v1-shares-token)), connecté, droit `DOCUMENT:READ` et lecture du document ou du dossier visé. Mot de passe éventuel dans l'en-tête `X-Share-Password` (plus dans l'URL). Renvoie les informations du lien ; pour un dossier, ouvrir ensuite `folderCode` dans l'explorateur. Chaque ouverture compte un accès.
4. Télécharger le fichier d'un lien de document : `GET /sni/api/v1/shares/{token}/file`, mêmes conditions plus le téléchargement permis sur le document ; `403` « Ce lien ne permet pas le téléchargement » si `allowDownload = false`, `400` pour un lien de dossier. Compte un accès.
5. Révoquer : `DELETE /sni/api/v1/shares/{token}`, droit `DOCUMENT:WRITE`, **par l'auteur du lien ou un administrateur** seulement (`403` sinon), `204` ; `404` si déjà révoqué.

Erreurs d'un lien : `404` « Lien de partage introuvable ou expiré » (inconnu ou révoqué), `400` « Ce lien de partage a expiré », `400` « … nombre maximal d'accès », `403` « Ce lien de partage demande un mot de passe valide » (ce n'est pas une fin de session : ne pas rafraîchir le jeton), `403` sans droit sur le document ou le dossier.
