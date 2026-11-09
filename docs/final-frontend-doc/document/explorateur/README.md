# Explorateur de la GED

> Chantier GED terminé : lots 1 à 5 (récapitulatif plus bas). Routes exactes et champs JSON : [routes/GedExplorerController.md](../routes/GedExplorerController.md). Règles communes : [README racine](../../README.md). Fonctionnement général des documents : [README du module](../README.md).

## En bref

La GED se consulte comme un explorateur de fichiers : une arborescence à gauche, le contenu du dossier ouvert à droite (sous-dossiers puis fichiers), un fil d'Ariane, et les gestes habituels (créer un dossier, déposer des fichiers ou un dossier entier, renommer, déplacer, copier, mettre à la corbeille, restaurer).

Elle n'est plus réservée au KYC : on y conserve tout document de l'organisation (courrier numérisé, statuts, PDF à archiver, tableur, document Word). Les documents produits par les autres modules (pièces KYC, contrats, domiciliation) s'y rangent tout seuls dans un plan de classement.

On y numérise aussi le papier (photos ou images de scanner assemblées en un PDF), et l'on cherche dans le **contenu** des fichiers : le texte des PDF, des scans (par OCR), des images et des documents bureautiques est lu par le serveur.

Elle tient enfin le cycle de vie des documents : dossiers à accès restreint, verrou des fichiers, règles de conservation, gel juridique, destruction définitive et quotas d'espace.

Back-office uniquement.

### Ce que chaque lot a apporté

| Lot | Apport | Section |
| --- | --- | --- |
| 1 | Explorateur : contenu d'un dossier, arborescence, dépôt de fichiers et de dossiers, renommer, déplacer, copier, corbeille | [Parcours](#parcours) |
| 2 | Classement automatique en vrais dossiers (Membres, Clients, Entreprises...) | [Le classement automatique](#le-classement-automatique) |
| 3 | Numérisation, lecture du texte (PDF, OCR, bureautique), recherche plein texte | [Numériser](#numériser-un-document-papier), [Chercher](#chercher-dans-les-fichiers) |
| 4 | Dossiers restreints (droits par personne et par rôle), verrou des fichiers | [Dossiers restreints et verrou](#dossiers-restreints-et-verrou-lot-4) |
| 5 | Conservation, gel juridique, destruction définitive, quotas | [Conservation, gel et destruction](#conservation-gel-juridique-destruction-et-quotas-lot-5) |

## Notions

| Notion | Ce que c'est |
| --- | --- |
| Élément | Une ligne du contenu d'un dossier : `kind` vaut `FOLDER` ou `FILE`. Les deux sortes arrivent dans une seule liste paginée, les dossiers d'abord. |
| Document interne | Un document qui n'appartient à aucun membre, client ou entreprise : `ownerType = INTERNAL`. C'est le cas par défaut d'un dépôt depuis l'explorateur. |
| Type « Document général » | `documentTypeCode = GENERAL` : sans revue (approuvé dès le dépôt), plusieurs par dossier, formats larges (voir plus bas). Type par défaut d'un dépôt depuis l'explorateur. |
| Document protégé | Un document produit par un autre module (pièce KYC, contrat, document de domiciliation) : `protectedItem = true` sur un fichier. Il se range et se renomme, mais ne part pas à la corbeille d'ici : son module en a encore besoin. |
| Dossier du plan | Un dossier créé par le serveur pour le classement automatique (`Membres`, `Awa Ndoye · MBR-000012`, `KYC`…) : `protectedItem = true` sur un dossier. Il ne se renomme, ne se déplace ni ne se supprime. On peut créer ses propres sous-dossiers dedans. |
| Lot de corbeille | Ce qui a été supprimé ensemble (un dossier et tout son contenu) : il se restaure ensemble, à son emplacement d'origine. |

Les éléments se désignent par leur `code` (texte) : jamais d'identifiant numérique.

## L'écran

```
┌───────────────────────┬────────────────────────────────────────────────────────────┐
│ Arborescence          │ Accueil › Administratif › Courriers  [🔍 Rechercher partout]│
│ (GET /ged/tree)       │ [Nouveau dossier] [Déposer] [Déposer un dossier] [Numériser] [Corbeille]
│                       │ ┌────┬──────────────────────┬────────────┬───────┬────────┐ │
│ ▸ Administratif       │ │    │ Nom                  │ Modifié    │ Taille│ Type   │ │
│   ▸ Courriers         │ │ 📁 │ 2025                 │ 03/11/2026 │       │ dossier│ │
│ ▸ Membres  🔒         │ │ 📄 │ convocation AG.pdf   │ 05/11/2026 │ 220 Ko│ pdf    │ │
│ ▸ Entreprises 🔒      │ └────┴──────────────────────┴────────────┴───────┴────────┘ │
│                       │                             Aperçu du fichier sélectionné → │
└───────────────────────┴────────────────────────────────────────────────────────────┘
```

- Double-clic sur un dossier : l'ouvrir (`GET /ged/items?folderCode=...`). Sur un fichier : l'aperçu.
- Le cadenas signale un élément `protectedItem` : masquer « Renommer », « Déplacer » et « Supprimer » pour un dossier du plan, et seulement « Supprimer » pour un fichier protégé.
- Sélection multiple (Ctrl, Maj), glisser-déposer vers un dossier de l'arbre ou de la liste pour déplacer, menu clic droit avec les mêmes actions que la barre.

## Parcours

### Ouvrir un dossier

1. `GET /sni/api/v1/ged/items?folderCode=FLD...&sort=name&direction=asc&page=0&size=50`. Sans `folderCode` : la racine.
2. La réponse donne `folder` (le dossier ouvert, vide à la racine), `breadcrumb` (de la racine au dossier ouvert), `items` et `page` (`totalElements`, `totalPages`, `folderCount`, `fileCount`).
3. `sort` : `name`, `updatedAt`, `size` ou `type`. Les dossiers suivent le nom, sauf avec `updatedAt` où ils suivent aussi la date. `search` filtre le dossier ouvert sur le nom, sans tenir compte de la casse.
4. Un fichier donne `previewUrl` et `downloadUrl` : ces adresses demandent le jeton, à appeler par `fetch` avec l'en-tête `Authorization` puis afficher un `blob:`. Pour une image dans une balise `<img>`, passer par l'aperçu signé décrit dans [parcours-televersement-et-apercus.md](../parcours-televersement-et-apercus.md).

### L'arborescence

`GET /sni/api/v1/ged/tree` : tous les dossiers, imbriqués, triés par nom, avec le nombre de fichiers de chacun (`documentsCount`). À recharger après une création, un déplacement ou une restauration.

### Créer et renommer un dossier

- Créer : `POST /sni/api/v1/ged/folders`, `{ "name": "Courriers", "parentCode": "FLD..." }` (sans `parentCode` : à la racine). Nom unique dans son dossier parent (`400`), 10 niveaux au plus.
- Renommer : `PATCH /sni/api/v1/ged/folders/{code}`, `{ "name": "..." }`. Refusé (`400`) pour un dossier du plan.

### Déposer des fichiers

`POST /sni/api/v1/ged/files`, en `multipart/form-data` :

| Partie | Rôle |
| --- | --- |
| `files` | Un ou plusieurs fichiers. |
| `folderCode` | Le dossier de destination ; absent : la racine. |
| `relativePaths` | Facultatif, une valeur par fichier dans le même ordre : le chemin du fichier dans le dossier déposé (`Factures/2025/janvier.pdf`). Les sous-dossiers manquants sont créés, l'arborescence est conservée. |
| `documentTypeCode`, `ownerType`, `ownerCode` | Facultatifs. Sans eux : document général interne. Avec un membre (`ownerType=MEMBER&ownerCode=MBR...`), le document lui appartient. |

- Réponse `201` : `uploaded` (les éléments créés) et `failed` (`fileName`, `message` pour chaque refus). Un fichier refusé n'annule pas les autres. Si tous sont refusés : `400` avec le même corps.
- Un nom déjà pris dans le dossier reçoit un suffixe : `rapport.pdf` devient `rapport (2).pdf`.
- **Taille** : 10 Mo au plus par requête, tous fichiers confondus. Pour déposer beaucoup de fichiers, en envoyer **un par requête**, l'un après l'autre, avec une barre de progression ; pour un dossier, garder le même `folderCode` de départ et passer à chaque fois `relativePaths` avec le chemin du fichier.
- **Déposer un dossier** : `<input type="file" webkitdirectory>` ou le glisser-déposer d'un dossier ; le chemin de chaque fichier est dans `file.webkitRelativePath`.
- **Formats acceptés** (type général) : PDF, JPEG, PNG, WebP, TIFF, GIF, TXT, CSV, Word (`.docx`, `.doc`), Excel (`.xlsx`, `.xls`), PowerPoint (`.pptx`, `.ppt`), OpenDocument (`.odt`, `.ods`). Le format est reconnu au contenu du fichier : une archive `.zip`, un exécutable ou un binaire renommé en `.txt` est refusé. Les types KYC restent limités au PDF et aux images.

### Renommer un fichier

`PATCH /sni/api/v1/ged/files/{code}`, `{ "name": "Statuts signés.pdf" }`. Le nom affiché change, le fichier stocké non. `409` si le nom est déjà pris dans le dossier. Permis aussi pour un fichier protégé.

### Déplacer

`POST /sni/api/v1/ged/move`, `{ "documentCodes": [...], "folderCodes": [...], "targetFolderCode": "FLD..." }` (`targetFolderCode` vide : à la racine). Tout ou rien.

- Un dossier ne va ni dans lui-même ni dans un de ses descendants (`400`), et pas là où un dossier du même nom existe déjà (`409`).
- Un dossier du plan ne se déplace pas (`400`).
- Un fichier dont le nom est pris dans la destination reçoit un suffixe.
- Un fichier rangé par le plan puis déplacé à la main reste où on l'a mis : le classement ne le reprend jamais.

### Copier

`POST /sni/api/v1/ged/copy`, `{ "documentCodes": [...], "targetFolderCode": "FLD..." }`. Chaque copie est un **document général interne**, indépendant de l'original (une copie de pièce KYC n'est pas une pièce KYC). Réponse comme le dépôt (`uploaded`, `failed`). Les dossiers ne se copient pas.

### Corbeille

1. Supprimer : `POST /sni/api/v1/ged/trash`, `{ "documentCodes": [...], "folderCodes": [...] }`. Un dossier part avec tout son contenu. Réponse : `batch` (le lot), `folders`, `documents` (nombres).
   - Refusé entièrement (`409`) si la sélection contient un fichier protégé, en citant jusqu'à cinq titres ; refusé (`400`) si elle contient un dossier du plan.
2. Voir la corbeille : `GET /sni/api/v1/ged/trash` : les 100 derniers lots, du plus récent au plus ancien. Chaque lot donne `trashedAt`, `trashedBy`, les nombres, et `items` : les éléments de tête (le dossier supprimé, pas son contenu).
3. Restaurer : `POST /sni/api/v1/ged/trash/{batch}/restore`. Le lot revient à son emplacement d'origine. `409` si cet emplacement est lui-même à la corbeille : le restaurer d'abord. Un dossier dont le nom a été repris entre-temps revient en « Nom (restauré) ».

La corbeille se vide seule chaque nuit des lots qui y sont depuis plus de 30 jours, et un administrateur peut détruire un lot ou la vider tout de suite : voir [Destruction définitive](#destruction-définitive).

### Numériser un document papier

`POST /sni/api/v1/ged/scans`, en `multipart/form-data` :

| Partie | Rôle |
| --- | --- |
| `pages` | Les images des pages, **dans l'ordre** : photos (JPEG), PNG, TIFF (un TIFF de plusieurs pages en donne plusieurs), GIF, BMP. |
| `folderCode` | Le dossier de destination ; absent : la racine. |
| `title` | Facultatif : le nom du PDF (`.pdf` ajouté s'il manque). Sans titre : « Numérisation du 08-11-2026 14h05.pdf ». |
| `documentTypeCode`, `ownerType`, `ownerCode` | Comme le dépôt : sans eux, document général interne. |

- Réponse `201` : l'élément créé (un seul PDF, une page A4 par image, en paysage si l'image est plus large que haute). `400` si une page n'est pas une image lisible, en citant son numéro.
- L'écran : une zone « Numériser » qui ouvre l'appareil photo sur mobile (`<input type="file" accept="image/*" capture="environment" multiple>`) ou accepte les images d'un scanner, une vignette par page, réordonnables par glisser-déposer, une corbeille par page, puis « Créer le PDF ».
- **Taille** : 10 Mo par requête, pages comprises. Réduire chaque photo avant l'envoi (canvas, 1600 px de large, JPEG qualité 0,8 : environ 300 Ko la page) ; une vingtaine de pages tient alors en une requête.
- Le PDF est ensuite lu par OCR, comme tout dépôt (voir plus bas) : il devient cherchable une à deux minutes après.

### Chercher dans les fichiers

`GET /sni/api/v1/ged/search?q=echeance bail&folderCode=FLD...&page=0&size=20`

- Cherche dans le **contenu** et le **nom** des fichiers, sans tenir compte des accents ni des majuscules, avec les variantes du français (« facture » trouve « factures »). Au moins deux caractères (`400` sinon).
- Syntaxe d'un moteur de recherche : `contrat bail` (les deux mots), `"bail commercial"` (expression exacte), `-brouillon` (exclure), `devis or facture` (l'un ou l'autre).
- Avec `folderCode` : le dossier et tous ses sous-dossiers ; sans : toute la GED. Les fichiers à la corbeille n'apparaissent pas.
- Réponse : `hits`, `totalElements`, `totalPages`. Chaque résultat donne `item` (le fichier, comme dans le contenu d'un dossier), `location` (« Membres / Awa Ndoye · MBR-000012 / KYC »), `rank` et `snippet`.
- `snippet` est un extrait du contenu, les mots trouvés entre `⟦` et `⟧`. **L'afficher en texte, jamais en HTML** : découper la chaîne sur ces deux caractères et mettre les morceaux trouvés dans des `<mark>` créés par le code (le texte vient de fichiers déposés). `snippet` est vide quand seul le nom correspond.
- Un nom de fichier qui contient la saisie passe devant. Clic sur un résultat : ouvrir son dossier (`item.folderCode`) en sélectionnant le fichier, ou l'aperçu.
- Le champ de recherche du dossier ouvert (`GET /ged/items?search=`) reste un filtre sur le **nom** dans ce seul dossier ; la recherche plein texte est un écran de résultats à part.

### Le texte d'un fichier

Le serveur lit chaque fichier après son dépôt, en tâche de fond (toutes les minutes, 20 fichiers par passage ; les fichiers existants sont repris au démarrage) :

| Fichier | Lecture (`method`) |
| --- | --- |
| PDF numérique | sa couche texte (`TEXT_LAYER`) |
| PDF scanné, image, TIFF | OCR (`OCR`), 30 pages au plus par fichier |
| PDF mêlant les deux | `MIXED` |
| TXT, CSV | le contenu (`PLAIN`) |
| Word, Excel, PowerPoint, OpenDocument récents | le texte du document (`OFFICE`) |
| `.doc`, `.xls`, `.ppt` anciens | non lu (`UNSUPPORTED`) : seul leur nom se cherche |

- `GET /sni/api/v1/ged/files/{code}/text` : `status` (`PENDING` pas encore lu, `DONE`, `EMPTY` rien à lire, `UNSUPPORTED`, `FAILED`), `method`, `pageCount`, `ocrPages`, `length`, `truncated` (texte coupé à 300 000 caractères), `extractedAt`, `error` (cause d'un échec, ou pages scannées non lues), `upToDate` (faux quand une nouvelle version attend sa lecture) et `content`. À montrer dans un onglet « Texte » de l'aperçu, avec un bouton « Copier le texte ».
- `POST /sni/api/v1/ged/files/{code}/text/refresh` : relit le fichier tout de suite et rend le même objet. L'OCR prend 2 à 3 secondes par page : prévoir un indicateur d'attente.
- `GET /sni/api/v1/ged/text/status` : `pending` (fichiers en attente) et `byStatus` (`{ "DONE": 120, "FAILED": 2, ... }`), pour un bandeau « Indexation : 37 fichiers en attente ».
- `POST /sni/api/v1/ged/text/run?limit=20` : lit un lot tout de suite (`processed`, `done`, `empty`, `unsupported`, `failed`, `remaining`).
- Un échec est retenté seul après six heures, trois fois au plus ; « Relire » le retente aussitôt.

## Dossiers restreints et verrou (lot 4)

### Le principe

- Un dossier **ouvert** (le cas par défaut) suit les droits généraux : tout compte qui a `DOCUMENT:READ` le voit, comme avant.
- Un dossier **restreint**, et tout ce qu'il contient, n'est accessible qu'aux personnes et aux rôles qui y ont un droit. Pour les autres, il **n'existe pas** :
  - il disparaît du contenu de son dossier parent, de l'arborescence, de la recherche et de la corbeille ;
  - l'ouvrir par son code rend `404`, pas `403`, pour ne pas révéler son existence.
- Les niveaux sont cumulatifs, chacun comprend ceux qui le précèdent :

| Niveau | Permet |
| --- | --- |
| `READ` | voir le dossier et la liste de ses fichiers |
| `DOWNLOAD` | ouvrir, télécharger, prévisualiser un fichier, lire son texte |
| `EDIT` | déposer, numériser, créer un sous-dossier, renommer, déplacer, copier vers ce dossier, verrouiller |
| `DELETE` | mettre à la corbeille et restaurer |
| `ADMIN` | gérer l'accès : restreindre, rouvrir, donner et retirer des droits |

- Les niveaux s'ajoutent aux droits généraux, ils ne les remplacent pas : déposer demande toujours `DOCUMENT:UPLOAD`, modifier `DOCUMENT:WRITE`.
- Quand plusieurs dossiers restreints se suivent sur le chemin, il faut un droit sur **chacun**, et le plus faible l'emporte. Exemple : le rôle RH a `READ` sur « Direction » et `DOWNLOAD` sur « Direction / Paie » ; dans « Paie », il ne peut que voir.
- Un droit se donne à un **utilisateur** (désigné par son e-mail) ou à un **rôle** (désigné par son nom, `STAFF`, `ADMIN`...).
- Un super administrateur voit tout.
- Les dossiers du plan de classement (`Membres`, `Clients`...) ne se restreignent pas : les modules KYC et contrats y rangent et y lisent. On peut restreindre un sous-dossier créé à la main dedans.
- Les mêmes règles s'appliquent aux routes `/documents/**` (consultation, téléchargement, aperçu, remplacement, versions, archivage) et `/document-folders/**` : elles ne contournent pas un dossier restreint.

### L'écran « Accès » d'un dossier

1. Lire : `GET /sni/api/v1/ged/folders/{code}/access`. La réponse donne :
   - `restricted` ;
   - `restrictedAt`, `restrictedBy` ;
   - `systemFolder` : vrai pour un dossier du plan, qui ne se restreint pas, masquer « Restreindre » ;
   - `inheritedFrom` : les dossiers restreints au-dessus, à afficher (« Aussi restreint par Direction ») ;
   - `grants` : la liste des droits, chacun avec `granteeType` (`USER` ou `ROLE`), `grantee` (e-mail ou nom du rôle), `granteeLabel` (libellé du rôle), `level`, `grantedAt`, `grantedBy` ;
   - `myLevel` ;
   - `canManage` : afficher les boutons de gestion seulement s'il est vrai.
2. Restreindre : `POST .../folders/{code}/restrict`, corps facultatif `{ "grants": [{ "granteeType": "ROLE", "grantee": "STAFF", "level": "DOWNLOAD" }] }`.
   - La personne qui restreint reçoit `ADMIN`, pour ne pas s'enfermer dehors.
   - `400` sur un dossier du plan.
3. Donner ou changer un droit : `PUT .../folders/{code}/grants` avec `{ "granteeType", "grantee", "level" }`.
   - Un bénéficiaire n'a qu'un niveau par dossier : le nouveau remplace l'ancien.
   - `404` si l'e-mail ou le rôle est inconnu.
   - `400` si le dossier n'est pas restreint.
4. Retirer : `DELETE .../folders/{code}/grants?granteeType=USER&grantee=alice@exemple.cg`. Le dernier droit `ADMIN` ne se retire pas (`409`) : le donner d'abord à quelqu'un d'autre.
5. Rouvrir à tous : `POST .../folders/{code}/unrestrict`. Tous les droits du dossier sont effacés.
6. Les quatre écritures demandent `ADMIN` sur le dossier (`403` sinon) et `DOCUMENT:WRITE`. Elles rendent toutes l'accès à jour, sous la même forme que la lecture.

Dans les listes :
- un dossier restreint porte `restricted: true` : afficher un cadenas ;
- `GET /ged/items` rend `access` (`level`, `canDownload`, `canEdit`, `canDelete`, `canManage`) pour le dossier ouvert : griser les actions non permises ;
- un refus d'action rend `403` avec un message qui dit le niveau manquant : « il faut le droit "modifier" (vous avez "voir") ».

### Verrouiller un fichier

- Verrouiller : `POST /sni/api/v1/ged/files/{code}/lock?hours=1`.
  - 1 heure par défaut, 24 au plus.
  - Le reposer prolonge le verrou.
  - Il faut le niveau `EDIT`.
- Déverrouiller : `POST /sni/api/v1/ged/files/{code}/unlock`. C'est possible pour celui qui l'a posé, pour un administrateur, et pour le gestionnaire (`ADMIN`) du dossier restreint qui contient le fichier.
- Tant que le verrou court, personne d'autre ne peut remplacer le fichier, restaurer une version, l'archiver, le renommer, le déplacer ou le mettre à la corbeille. Ces actions rendent `409` « « statuts.pdf » est verrouillé par alice@exemple.cg jusqu'au 09/11/2026 18h30 ».
- Le fichier reste lisible et téléchargeable par tous ceux qui y ont droit.
- Le verrou expire seul.
- Chaque fichier des listes porte `lock` (`lockedBy`, `lockedAt`, `expiresAt`, `mine`) ou `null` : afficher un cadenas avec le nom et l'échéance, et « Déverrouiller » si `mine`.

## Conservation, gel juridique, destruction et quotas (lot 5)

Toutes les écritures de cette partie sont réservées aux administrateurs (rôle `ADMIN` ou `SUPER_ADMIN`, `403` sinon) : elles figent ou détruisent des documents. Les lectures suivent les droits habituels.

### Gel juridique

Un contentieux, un contrôle fiscal ou une enquête imposent de garder des documents tels quels.

- Un gel porte sur un **fichier**, ou sur un **dossier et tout ce qu'il contient**, sous-dossiers compris, y compris ce qui y sera déposé ensuite.
- Tant que le gel court, ce qu'il couvre :
  - ne part pas à la corbeille ;
  - ne se remplace pas par une nouvelle version, et ne se restaure pas dans une ancienne ;
  - ne s'archive pas, ne se détruit pas ;
  - échappe aux règles de conservation.

  Chacune de ces actions rend `409` « « constat.pdf » est sous gel juridique : il ne se remplace pas tant que le gel n'est pas levé ».
- Lire, télécharger, renommer et déplacer restent permis.
- Un gel se lève, il ne s'efface pas : qui l'a posé et qui l'a levé, quand et pourquoi, restent consultables.

Routes :

1. Poser : `POST /sni/api/v1/ged/holds` avec `{ "targetType": "FOLDER" | "DOCUMENT", "targetCode": "FLD...", "reason": "Contentieux Dupont", "reference": "RG 26/0142" }` (`reference` facultatif). Réponse `201` `GedHold`.
2. Lister : `GET /sni/api/v1/ged/holds` : les gels en cours ; `?active=false` : tous, levés compris, du plus récent au plus ancien.
3. Lever : `POST /sni/api/v1/ged/holds/{code}/release` avec `{ "reason": "Affaire classée" }` (`409` si déjà levé).
4. `GedHold` : `code` (`GEL-XXXXXXXX`), `targetType`, `targetCode`, `targetName`, `reason`, `reference`, `placedBy` (e-mail), `placedAt`, `releasedBy`, `releasedAt`, `releaseReason`, `active`.
5. Dans les listes, chaque élément porte `held` (vrai si un gel le couvre, directement ou par un dossier au-dessus) : afficher un sceau et masquer « Supprimer ».

### Destruction définitive

Détruire efface toutes les versions du fichier du stockage, ainsi que son texte lu. La ligne du document reste comme trace (qui, quand, pourquoi) : le document n'apparaît plus nulle part, même pas dans la corbeille. Un fichier gelé n'est jamais détruit : il est épargné et compté.

- Détruire un lot de la corbeille : `POST /sni/api/v1/ged/trash/{batch}/purge` avec `{ "reason": "..." }`. Il faut aussi le niveau « supprimer » à l'emplacement d'origine (dossiers restreints). Les dossiers du lot ne sont détruits que si rien de gelé n'y reste.
- Vider la corbeille : `POST /sni/api/v1/ged/trash/empty?olderThanDays=0` avec `{ "reason": "..." }`. `olderThanDays` : seulement les lots supprimés depuis plus de N jours ; `0` : tout.
- Chaque nuit à 3 h 15, la corbeille est vidée des lots de plus de 30 jours (`app.ged.trash.retention-days`, coupure par `app.ged.trash.purge-enabled`). Afficher dans l'écran de la corbeille : « Les éléments sont détruits définitivement 30 jours après leur suppression ».
- Réponse `GedPurgeResult` : `batches`, `folders`, `documents` (détruits), `filesErased` (fichiers effacés du stockage), `heldSkipped` (épargnés car gelés), `failed` (erreur de stockage : le fichier reste entier, à rejouer). Afficher par exemple « 12 fichiers détruits, 1 épargné (gel juridique) ».
- Avant d'envoyer, toujours demander une confirmation qui dit que l'action est irréversible, avec un motif obligatoire.

### Règles de conservation

Une règle dit combien de temps garder des documents, et ce qu'il advient ensuite. Les règles se gèrent par `/sni/api/v1/document-retention-policies` (liste, création, modification, désactivation) :

- `code`, `name` et `retentionDays` (au moins 1) sont obligatoires.
- `retentionReference` : `UPLOAD_DATE` (date de dépôt) ou `EXPIRY_DATE` (date d'expiration du document).
- `action` :
  - `ARCHIVE` : archiver ;
  - `PURGE_FLAG` : marquer « à détruire », la destruction attendant la confirmation d'un administrateur.
- Champs facultatifs de portée : `space`, `documentTypeCode`, et **`folderCode`** (nouveau), qui fait viser un dossier de la GED et tout son contenu. La réponse donne `folderCode` et `folderName`.

Les règles s'appliquent chaque nuit à 2 h 30. Pour les appliquer tout de suite : `POST /sni/api/v1/ged/retention/run`, réponse `{ "applied": n }`. Les fichiers gelés et ceux qu'un administrateur a décidé de garder sont ignorés.

L'écran « À détruire » :

1. Liste : `GET /sni/api/v1/ged/retention/due?limit=200`. Chaque ligne donne :
   - `item` : le fichier, avec `held` et `destructionDueAt` ;
   - `policyCode` : la règle qui l'a marqué ;
   - `dueAt` ;
   - `location` : son dossier.

   Les fichiers des dossiers restreints auxquels la personne n'a pas droit n'y figurent pas.
2. Confirmer la destruction : `POST /sni/api/v1/ged/retention/destroy` avec `{ "documentCodes": [...], "reason": "Durée de conservation échue" }`. Réponse `GedPurgeResult`. Un fichier non marqué renvoie `400` : pour le supprimer, passer par la corbeille.
3. Garder : `POST /sni/api/v1/ged/retention/keep` avec `{ "documentCodes": [...], "reason": "Valeur historique" }`. Réponse `{ "kept": n }`. Le fichier sort du marquage et des règles de conservation ; le motif est ajouté à sa description.

### Quotas d'espace

- **Calcul** : l'espace d'un dossier, c'est la place de toutes les versions de ses fichiers, sous-dossiers compris et corbeille comprise, car un fichier supprimé occupe le stockage tant qu'il n'est pas détruit.
- **Lire** : `GET /sni/api/v1/ged/usage?folderCode=...` (sans `folderCode` : toute la GED). Réponse `GedUsage` :
  - `usedBytes`, `files` ;
  - `quotaBytes` : le quota du dossier lui-même ;
  - `remainingBytes` et `limitingFolderCode` : ce qui reste sous le quota le plus serré du chemin, et le dossier qui le porte ;
  - `totalUsedBytes` et `totalQuotaBytes` : pour toute la GED.

  Afficher une jauge « 1,2 Go sur 5 Go ».
- **Fixer ou retirer** : `PUT /sni/api/v1/ged/folders/{code}/quota` avec `{ "quotaBytes": 5368709120 }`. `null` ou `0` retire le quota. Il faut aussi le niveau « gérer l'accès » sur un dossier restreint.
- **Quota global** : `app.ged.quota.total-bytes` (0 : aucun).
- **Refus** : un dépôt, une numérisation, une copie ou une nouvelle version qui dépasserait un quota rend `409` « le dossier « Service juridique » a atteint son quota (4,8 Go sur 5 Go) : il manque 12,3 Mo ». Pour un dépôt de plusieurs fichiers, le fichier refusé apparaît dans `failed` avec ce message.

## Le classement automatique

Tout document créé avec un propriétaire est rangé dès sa création, s'il n'a pas été déposé dans un dossier choisi :

```
Membres / Awa Ndoye · MBR-000012 / KYC | Contrats | Domiciliation | Finances | Juridique | Documents
Clients / <nom> · <code client>   / (même découpage)
Entreprises / <nom> · <code>      / (même découpage)
Contrats | Facturation | Commercial | Équipements / <année>     (documents rattachés à un objet)
```

- Le sous-dossier se choisit ainsi : catégorie KYC, sinon type de contrat (`CONTRACT_...`), sinon type de domiciliation (`DOMICILIATION_...`), sinon catégorie financière, juridique, et à défaut « Documents ».
- Un document interne n'est jamais rangé d'office.
- Les documents existants sont repris par une tâche horaire (premier passage deux minutes après le démarrage, 1 000 documents par passage). Pour lancer la reprise tout de suite : `POST /sni/api/v1/ged/filing/run?limit=1000`, réponse `{ "filed", "skipped", "remaining" }`. Rejouer tant que `remaining` baisse ; `skipped` compte les documents sans place possible (propriétaire introuvable).
- Un dossier créé à la main avec le même nom au même endroit (par exemple « Membres » à la racine) est repris par le plan : il devient protégé.
- Le nom du dossier d'un propriétaire est fixé à sa création : il ne suit pas un changement de nom ultérieur.

## Droits

| Action | Droit |
| --- | --- |
| Parcourir, arborescence, corbeille (lecture) | `DOCUMENT:READ` |
| Chercher, lire le texte d'un fichier, état de la lecture | `DOCUMENT:READ` |
| Déposer (`POST /ged/files`), numériser (`POST /ged/scans`) | `DOCUMENT:UPLOAD` |
| Créer, renommer, déplacer, copier, corbeille, restaurer, lancer le classement, relire un fichier, lancer la lecture | `DOCUMENT:WRITE` |
| Gérer l'accès d'un dossier, verrouiller, déverrouiller | `DOCUMENT:WRITE` |
| Lire l'espace occupé, la liste « à détruire » | `DOCUMENT:READ` |
| Gels, destruction, vidage de la corbeille, garder ou détruire un fichier marqué, appliquer les règles, quotas | `DOCUMENT:WRITE` et rôle `ADMIN` ou `SUPER_ADMIN` |

Dans un dossier restreint, le niveau du dossier s'ajoute à ces droits : voir le tableau des niveaux plus haut.

## Ce que l'écran ne doit pas envoyer

Les corps sont stricts : n'envoyer que les champs listés ci-dessus. Pas de `kind`, ni d'élément complet renvoyé tel quel.

## Limites connues

- Une destruction est irréversible : le fichier est effacé du stockage. Seule la trace (titre, dates, auteur et motif de la destruction) reste en base, sans route pour la lire ; elle sert au contrôle et à l'audit.
- Les sauvegardes de l'hébergement ne sont pas touchées par la destruction : leur durée de conservation se règle chez l'hébergeur.
- Le quota compte la corbeille : vider la corbeille libère de la place.

- Les droits se posent sur des dossiers, pas sur un fichier isolé : pour un fichier confidentiel, le ranger dans un dossier restreint. Les anciens droits sur un document (`/document-permissions`, cible `DOCUMENT`) ne sont pas appliqués.
- Dans `/documents` (liste et recherche), les documents cachés sont retirés de la page après coup : une page peut compter moins de lignes que `size`, et `totalElements` les compte encore.
- L'OCR exige Tesseract sur le serveur. En production il est coupé par défaut (`GED_OCR_ENABLED=false`) : les scans y sont alors en `FAILED` (« OCR désactivé »), les PDF numériques et les fichiers bureautiques restent lus et cherchables.
- Le PDF d'une numérisation contient les images des pages, pas de couche texte : son texte est dans la GED (recherche, onglet « Texte »), pas dans le fichier téléchargé.
- Les PDF des factures et avoirs sont conservés par le module de facturation, pas dans la GED : ils n'apparaissent pas dans « Facturation ».
- Les anciens écrans des dossiers (`/document-folders/**`) restent en place et exigent `ADMIN:ACCESS` ; l'explorateur passe par `/ged/**`. Les dossiers restreints y sont aussi appliqués.
