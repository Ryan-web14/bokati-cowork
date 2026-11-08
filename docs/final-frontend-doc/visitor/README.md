# visitor : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module gère les visiteurs extérieurs attendus à l'accueil : l'équipe crée un laissez-passer (pass visiteur), le visiteur reçoit une invitation par courriel avec un QR code, l'accueil enregistre son arrivée puis son départ, et peut imprimer un badge PDF. Écrans du back-office uniquement : aucune route publique ni portail membre. Le module ne dépend d'aucun autre module métier (l'hôte est un simple texte, voir [Écarts relevés](#écarts-relevés)).

## Notions

| Objet | Identifiant exposé | Rôle |
| --- | --- | --- |
| Pass visiteur (`VisitorPass`) | `passNumber`, de la forme `VIS-<millisecondes>` (ex. `VIS-1731060000123`), généré par le serveur | Autorisation de visite sur une plage `validFrom` / `validUntil`, avec un hôte et un motif. Porte le statut. |
| Visiteur (`Visitor`) | aucun, jamais exposé | Nom, courriel, téléphone, société. Un nouveau visiteur est créé à **chaque** pass, même pour une personne déjà venue. |
| Passage (`VisitorCheckIn`) | aucun | Une ligne par arrivée : heure d'arrivée, heure de départ, agent d'accueil, note. C'est ce que liste le journal. |
| `qrValue` | texte `bokati:visitor:<passNumber>` | Contenu du QR code de l'invitation et du badge. Un lecteur peut en extraire le `passNumber`. |

Toutes les routes désignent le pass par son `passNumber` : aucun identifiant numérique à manipuler.

## Statuts et cycle de vie

Pass visiteur (`status`, énumération `VisitorPassStatus`) :

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `SCHEDULED` | Pass créé, visiteur attendu | `POST /sni/api/v1/visitors/passes` | `CHECKED_IN` (arrivée), `EXPIRED` (automatique) |
| `CHECKED_IN` | Visiteur présent dans les locaux | `POST /sni/api/v1/visitors/{passNumber}/check-in` | `CHECKED_OUT` (départ) |
| `CHECKED_OUT` | Visiteur reparti | `POST /sni/api/v1/visitors/{passNumber}/check-out` | `CHECKED_IN` (une nouvelle arrivée est acceptée) |
| `EXPIRED` | Pass jamais utilisé, plage dépassée | Worker horaire : tout pass `SCHEDULED` dont `validUntil` est passé | `CHECKED_IN` (l'arrivée reste acceptée, voir règles) |
| `CANCELLED` | Prévu par l'énumération | Jamais atteint : aucune route ni automatisme ne le pose | aucun |

## Parcours

### Créer un pass et inviter le visiteur

1. `POST /sni/api/v1/visitors/passes` ([détail](routes/VisitorManagementController.md#r-post-sni-api-v1-visitors-passes)) avec au minimum `fullName`, `validFrom`, `validUntil`. Ajouter `email` pour que l'invitation parte, et `hostName` (affiché sur l'invitation, le badge et le journal). `hostMemberCode`, `phone`, `company`, `purpose`, `createdBy` sont facultatifs.
2. `validFrom` et `validUntil` sont des **textes** (pas des `Instant`) : le serveur accepte un ISO 8601 avec fuseau (`2026-11-08T09:00:00Z`, `2026-11-08T10:00:00+01:00`) ou sans fuseau (`2026-11-08T10:00`), ce dernier étant lu dans le fuseau de l'application (`app.time-zone`, défaut `Africa/Lagos`). `validUntil` doit être strictement après `validFrom`.
3. Réponse `201` : le pass complet, statut `SCHEDULED`, avec `passNumber` et `qrValue`. L'écran affiche le `passNumber` et propose « Imprimer le badge ».
4. Si un `email` est fourni, l'invitation part en arrière-plan avec le QR code en image intégrée (objet « Votre invitation visiteur · Elle A Osé Bokati Cowork »). Aucun retour de l'envoi dans la réponse.

### Accueil du jour

1. `GET /sni/api/v1/visitors/passes/today` : les pass dont la plage de validité recoupe la journée en cours. Liste non paginée.
2. Pour chaque ligne, bouton « Arrivée » si `status` vaut `SCHEDULED`, `EXPIRED` ou `CHECKED_OUT`, bouton « Départ » si `CHECKED_IN`.
3. Recherche par QR code : décoder `qrValue`, retirer le préfixe `bokati:visitor:` pour obtenir le `passNumber`, puis appeler l'arrivée.

### Enregistrer l'arrivée puis le départ

1. `POST /sni/api/v1/visitors/{passNumber}/check-in` avec un corps JSON, par exemple `{ "agent": "Accueil RDC", "notes": "Badge n°12" }`. Le corps est **obligatoire** : envoyer au moins `{}`.
2. Réponse `200` : le pass, statut `CHECKED_IN`. Une ligne de passage est ouverte avec l'heure serveur.
3. `POST /sni/api/v1/visitors/{passNumber}/check-out` avec `{ "agent": "...", "notes": "..." }` (au moins `{}`). Ferme le dernier passage ouvert ; une note non vide remplace la note de l'arrivée.
4. Réponse `200` : le pass, statut `CHECKED_OUT`.

### Imprimer le badge

1. `GET /sni/api/v1/visitors/passes/{passNumber}/badge` : PDF (`application/pdf`), nom proposé `badge-<passNumber>.pdf` dans `Content-Disposition`. Le télécharger avec le jeton (pas de lien direct sans en-tête `Authorization`).
2. Le badge reprend le nom, l'hôte, la plage de validité (format `dd/MM/yyyy HH:mm`, fuseau du serveur) et le QR code.

### Liste et journal

1. `GET /sni/api/v1/visitors/passes?status=SCHEDULED` : tous les pass, filtrables par statut. Liste complète, **non paginée**, sans tri garanti : trier et paginer côté écran.
2. `GET /sni/api/v1/visitors/log` : tous les passages, du plus récent au plus ancien (`checkedInAt` décroissant), avec agents et notes. Non paginé lui aussi.

## Règles et erreurs à refléter dans l'écran

| Situation | Statut | Message (`message`) |
| --- | --- | --- |
| `fullName`, `validFrom` ou `validUntil` vide | `400` | `VALIDATION_FAILED` (Bean Validation) ou `Visitor name and validity dates are required` |
| Date illisible | `400` | `validFrom must be a valid date-time` (ou `validUntil ...`) |
| `validUntil` pas après `validFrom` | `400` | `validUntil must be after validFrom` |
| Arrivée d'un visiteur déjà `CHECKED_IN` | `400` | `Visitor is already checked in` |
| Départ sans arrivée ouverte | `400` | `Visitor is not checked in` |
| `passNumber` inconnu | `404` | `Visitor pass not found: <passNumber>` |
| Corps absent sur check-in ou check-out | `400` | corps illisible (envoyer `{}`) |

Les messages sont en anglais : prévoir une traduction côté écran à partir du statut et du texte.

Ce que le serveur **ne vérifie pas**, à contrôler dans l'écran si besoin : l'arrivée hors de la plage de validité (avant `validFrom` ou après `validUntil`) est acceptée, de même que l'arrivée sur un pass `EXPIRED`.

## À prévoir côté frontend

- Écrans : création de pass (formulaire), accueil du jour (liste + actions arrivée/départ + lecture QR), liste des pass filtrable par statut, journal des passages, impression du badge.
- Droits (comparer à la liste `roles` reçue à la connexion) : `VISITOR:READ` pour les listes, le journal et le badge ; `VISITOR:WRITE` pour créer un pass ; `VISITOR:CHECKIN` pour les boutons Arrivée et Départ. La règle `@PreAuthorize` s'applique aussi à `ROLE_SUPER_ADMIN`, qui doit porter le droit.
- Aucune modification ni annulation de pass n'existe : une erreur de saisie se corrige en créant un nouveau pass.
- `createdBy` est un texte libre recopié tel quel : y mettre le nom ou l'adresse du compte connecté si on veut une trace (le serveur ne le déduit pas du jeton).
- `hostMemberCode` n'est pas contrôlé : le choisir dans la liste des membres (module `client`) pour éviter une valeur fausse.
- Listes non paginées : prévoir une pagination locale si le volume grandit.

## Effets automatiques

- **Invitation par courriel** à la création si `email` est renseigné (asynchrone, gabarit `visitor/visitor-invitation`, QR code intégré). Un échec d'envoi est seulement journalisé.
- **Expiration** : `VisitorPassExpiryWorker`, à chaque heure pile (`0 0 * * * *`), passe en `EXPIRED` tout pass `SCHEDULED` dont `validUntil` est dépassé. Un écran ouvert peut donc voir un statut changer à l'heure suivante.
- Aucune notification en temps réel, aucun événement publié.

## Écarts relevés

- Statut `CANCELLED` jamais atteint : aucune route ni automatisme ne le pose (`src/main/java/com/sni/bokaticowork/features/visitor/enums/VisitorPassStatus.java:7`).
- L'arrivée n'est refusée que si le pass est déjà `CHECKED_IN` : un pass `EXPIRED`, `CANCELLED` ou hors plage de validité passe sans contrôle (`src/main/java/com/sni/bokaticowork/features/visitor/service/implementation/VisitorManagementServiceImpl.java:103-116`).
- « Aujourd'hui » est calculé en UTC et non dans le fuseau de l'application : entre minuit et 1 h (heure de Lagos) la liste du jour reflète la veille (`VisitorManagementServiceImpl.java:94`).
- `passNumber` dérivé de l'horloge (`"VIS-" + epochMilli`) : deux créations dans la même milliseconde violent l'unicité et la seconde échoue (`VisitorManagementServiceImpl.java:61`).
- Corps JSON obligatoire sur check-in et check-out alors que les deux champs sont facultatifs (`src/main/java/com/sni/bokaticowork/features/visitor/controller/VisitorManagementController.java:46` et `:53`).
- `hostMemberCode` n'est relié à aucun membre et n'est pas vérifié ; un visiteur est recréé à chaque pass, sans historique par personne (`VisitorManagementServiceImpl.java:55-60`).
- Listes `GET /visitors/passes` et `GET /visitors/log` non paginées (`findAll`) (`VisitorManagementServiceImpl.java:87` et `:136`).
- Messages d'erreur en anglais.
