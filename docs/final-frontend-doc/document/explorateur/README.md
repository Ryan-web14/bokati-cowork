# Explorateur de la GED

> Chantier GED, lots 1, 2 et 3. Routes exactes et champs JSON : [routes/GedExplorerController.md](../routes/GedExplorerController.md). Règles communes : [README racine](../../README.md). Fonctionnement général des documents : [README du module](../README.md).

## En bref

La GED se consulte comme un explorateur de fichiers : une arborescence à gauche, le contenu du dossier ouvert à droite (sous-dossiers puis fichiers), un fil d'Ariane, et les gestes habituels (créer un dossier, déposer des fichiers ou un dossier entier, renommer, déplacer, copier, mettre à la corbeille, restaurer).

Elle n'est plus réservée au KYC : on y conserve tout document de l'organisation (courrier numérisé, statuts, PDF à archiver, tableur, document Word). Les documents produits par les autres modules (pièces KYC, contrats, domiciliation) s'y rangent tout seuls dans un plan de classement.

On y numérise aussi le papier (photos ou images de scanner assemblées en un PDF), et l'on cherche dans le **contenu** des fichiers : le texte des PDF, des scans (par OCR), des images et des documents bureautiques est lu par le serveur.

Back-office uniquement.

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

Il n'y a pas encore de suppression définitive ni de vidage de la corbeille : ils viendront avec les règles de conservation (lot 5).

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

## Ce que l'écran ne doit pas envoyer

Les corps sont stricts : n'envoyer que les champs listés ci-dessus. Pas de `kind`, ni d'élément complet renvoyé tel quel.

## Limites connues

- Les droits par dossier (`/document-permissions`) sont enregistrés mais pas encore appliqués : tout compte qui a `DOCUMENT:READ` voit toute l'arborescence (lot 4).
- La recherche ne tient pas encore compte des droits par dossier (lot 4) : elle voit ce que voit l'explorateur.
- L'OCR exige Tesseract sur le serveur. En production il est coupé par défaut (`GED_OCR_ENABLED=false`) : les scans y sont alors en `FAILED` (« OCR désactivé »), les PDF numériques et les fichiers bureautiques restent lus et cherchables.
- Le PDF d'une numérisation contient les images des pages, pas de couche texte : son texte est dans la GED (recherche, onglet « Texte »), pas dans le fichier téléchargé.
- Les PDF des factures et avoirs sont conservés par le module de facturation, pas dans la GED : ils n'apparaissent pas dans « Facturation ».
- Les anciens écrans des dossiers (`/document-folders/**`) restent en place et exigent `ADMIN:ACCESS` ; l'explorateur passe par `/ged/**`.
