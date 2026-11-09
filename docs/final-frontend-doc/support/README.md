# support : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module gère l'assistance : des tickets ouverts par les membres depuis le portail, par le personnel depuis le back-office, par des automatismes (quota de no-show, anomalie de caisse) ou complétés par courriel. Chaque ticket porte un fil de messages (publics ou notes internes), des pièces jointes, des étiquettes, des échéances de service (SLA) surveillées par un worker qui escalade, et une note de satisfaction (CSAT) donnée depuis le portail ou depuis un lien de courriel public.

Trois publics : le back-office (`/sni/api/v1/support/**`, droits `SUPPORT:READ`, `SUPPORT:WRITE`, `SUPPORT:ASSIGN`, `SUPPORT:METRICS`), le portail membre (`/sni/api/v1/client/support/**`) et une route publique sans jeton (`/sni/api/v1/public/support/csat`). Le module travaille avec `task` (tâche créée depuis un ticket), `notification` (notifications internes aux agents), `client` (membres et clients propriétaires des tickets), `booking` et `payment` (tickets automatiques), et une base de connaissances intégrée.

## Notions

| Objet | Identifiant exposé | Remarques |
| --- | --- | --- |
| Ticket | `ticketNumber`, par exemple `TCK-1762598130123` (préfixe `TCK-` et horodatage en millisecondes) | Toutes les routes de ticket le prennent dans le chemin. |
| Propriétaire du ticket | couple `ownerType` + `ownerCode` (texte libre) | `MEMBER` + identifiant de membre pour le portail ; `CUSTOMER`, `CASHIER` (anomalie de caisse) ou autre côté back-office. |
| Objet lié | couple `relatedType` + `relatedCode` (texte libre) | Exemples écrits par le serveur : `BOOKING_NO_SHOW_QUOTA` + code du propriétaire, `CASH_ANOMALY_FLAG` + numéro de l'anomalie. |
| Message | `id` numérique (petit, séquence de base) | `senderType` : `CLIENT`, `AGENT`, `SYSTEM`. `internal: true` = note interne, jamais montrée au membre. |
| Pièce jointe | `id` numérique (petit) | Rattachée au ticket, éventuellement à un message (côté serveur seulement : la réponse ne dit pas lequel). `internal` la cache au membre. |
| Étiquette (tag) | son nom normalisé | Minuscules, espaces remplacés par `-` (`"Facture Erronée"` devient `facture-erronée`). Créée à la volée au premier usage. |
| Événement de chronologie | `id` numérique | Journal lisible du ticket : `GET .../timeline`. |
| Agent assigné | `assignedTo` : **le courriel de l'agent** (ex. `"assignedTo": "support@sni-cg.com"`), vide si personne | À afficher tel quel, à renvoyer tel quel dans le filtre de la file et pour réassigner. Ailleurs (règles de routage, statistiques par agent, `uploadedBy`) il arrive en nombre int64 : voir [Identifiants](../README.md#identifiants--attention-aux-grands-nombres). |
| Règle de routage | `id` numérique (petit) | Assigne automatiquement un ticket neuf à un agent selon catégorie, priorité, type de propriétaire, type d'objet lié. |
| Réponse rapide | `id` numérique (petit) | Modèle de texte par catégorie, pour l'agent. |
| Article de connaissance | `articleCode` (`KB-` + horodatage) côté back-office, `slug` côté portail | `publicVisible` le rend lisible au portail. |

Énumérations :

- `TicketStatus` : `OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`, `RESOLVED`, `CLOSED`.
- `TicketPriority` : `LOW`, `MEDIUM`, `HIGH`, `URGENT` (défaut `MEDIUM`).
- `TicketCategory` : `BILLING`, `BOOKING`, `ACCESS`, `TECHNICAL`, `OTHER` (défaut `OTHER`).
- `TicketEventType` (chronologie) : `TICKET_CREATED`, `ASSIGNED`, `STATUS_CHANGED`, `PRIORITY_CHANGED`, `MESSAGE_ADDED`, `SLA_ESCALATED`, `CSAT_SUBMITTED`, `REOPENED`, `AUTO_CLOSED`, `TASK_CREATED` (plus `CATEGORY_CHANGED` et `MERGED`, jamais écrits).

### Échéances de service (SLA)

Fixées à la création selon la priorité, jamais recalculées ensuite (`src/main/java/com/sni/bokaticowork/features/support/service/implementation/SupportTicketServiceImpl.java:781`) :

| Priorité | Première réponse (`firstResponseDueAt`) | Résolution (`resolutionDueAt`) |
| --- | --- | --- |
| `URGENT` | 2 h | 8 h |
| `HIGH` | 8 h | 24 h |
| `MEDIUM` | 24 h | 72 h |
| `LOW` | 48 h | 120 h |

`firstRespondedAt` est posé au premier message d'un agent (même une note interne). `resolvedAt` au passage en `RESOLVED`. Un ticket est « en retard » s'il est `OPEN` ou `IN_PROGRESS` et que l'une des deux échéances est passée sans la date correspondante. Un ticket `WAITING_CLIENT` n'est jamais compté en retard.

## Statuts et cycle de vie

### Ticket

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `OPEN` | Nouveau, sans agent | Création (back-office, portail, automatisme) ; réponse d'un membre ou courriel sur un ticket `CLOSED` non assigné ; `PATCH .../status` | `IN_PROGRESS` (assignation, règle de routage, message du membre), `WAITING_CLIENT` (réponse publique d'agent), tout statut par `PATCH .../status`, `CLOSED` (fermeture par le membre) |
| `IN_PROGRESS` | Pris en charge | Assignation d'un agent sur un ticket `OPEN` ; règle de routage à la création ; message du membre ou courriel entrant (depuis `OPEN`, `WAITING_CLIENT` ou `RESOLVED`) ; réouverture d'un `CLOSED` assigné | `WAITING_CLIENT`, `RESOLVED`, `CLOSED` |
| `WAITING_CLIENT` | Un agent a répondu publiquement, on attend le membre | `POST /sni/api/v1/support/tickets/{ticketNumber}/messages` avec `senderType` `AGENT` (défaut) et `internal` faux, depuis `OPEN` ou `IN_PROGRESS` | `IN_PROGRESS` (réponse du membre ou courriel), tout statut par `PATCH .../status` |
| `RESOLVED` | Traité, en attente de clôture | `PATCH .../status` avec `RESOLVED` (pose `resolvedAt`, envoie le courriel « Ticket résolu » avec les liens de note) | `CLOSED` (automatique après N jours sans réponse du membre, ou manuel), `IN_PROGRESS` (réponse du membre : `resolvedAt` n'est **pas** effacé) |
| `CLOSED` | Fermé | `PATCH .../status` avec `CLOSED`, `POST /sni/api/v1/client/support/tickets/{ticketNumber}/close`, worker de fermeture automatique (événement `AUTO_CLOSED`) | `OPEN` ou `IN_PROGRESS` si le membre écrit (portail ou courriel) : événement `REOPENED`, `closedAt` et `resolvedAt` effacés |

Le back-office peut passer de n'importe quel statut à n'importe quel autre par `PATCH .../status` : le serveur n'applique aucune table de transitions. Il pose `resolvedAt` et `closedAt` au premier passage seulement, et ne les efface pas si l'agent rouvre lui-même le ticket.

### Escalade (champ `escalationLevel`)

| Niveau | Signification | On y arrive par | Destinataire du courriel |
| --- | --- | --- | --- |
| `0` | Pas d'escalade | Création | |
| `1` | Première escalade | Worker SLA, dès qu'une échéance est dépassée | Agent assigné (sinon `bokati.support.manager-email`) |
| `2` | Deuxième | Worker SLA, 4 h après la précédente si le dépassement continue | Responsable support (`manager-email`) |
| `3` | Dernière | Worker SLA, 8 h après la précédente | Direction (`bokati.support.admin-email`) |

Le niveau ne redescend jamais, même après résolution. `escalatedAt` et `escalationReason` décrivent la dernière escalade.

### Article de connaissance

Pas de statut, trois drapeaux : `active` (faux après `DELETE`), `publicVisible` (lisible au portail), `internalOnly` (informatif, voir [Écarts](#écarts-relevés)).

## Parcours

### Back-office : file des tickets

1. `GET /sni/api/v1/support/tickets` ([détail](routes/SupportTicketController.md#r-get-sni-api-v1-support-tickets)) avec les filtres utiles : `status`, `priority`, `category`, `assignedTo` (courriel de l'agent tel que reçu dans un ticket, ou son identifiant ; un agent inconnu rend une file vide), `ownerType` + `ownerCode`, `relatedType` + `relatedCode`, `overdueOnly=true` (tickets en retard), `searchText` (titre, description, numéro), `page`, `size` (défaut 20).
2. La réponse est une page `{ data, pageable }` triée par date de création décroissante. Le paramètre `sort` est ignoré.
3. Chaque ligne est un résumé : `messages` et `attachments` sont des listes **vides**, `messageCount` et `attachmentCount` donnent les nombres, `description` est coupée à 2 000 caractères (en texte brut si elle était longue).
4. Afficher en tête les compteurs `GET /sni/api/v1/support/tickets/metrics` (droit `SUPPORT:METRICS`) : `openTickets` (`OPEN` + `IN_PROGRESS`), `overdueFirstResponse`, `overdueResolution`, `resolvedTickets`.

### Back-office : créer un ticket

1. `POST /sni/api/v1/support/tickets` ([détail](routes/SupportTicketController.md#r-post-sni-api-v1-support-tickets)) avec `title` (obligatoire), `description`, `priority`, `category`, `ownerType` + `ownerCode`, `contactName`, `contactEmail`, `contactPhone`, `relatedType` + `relatedCode`.
2. Si `ownerType` vaut `MEMBER` ou `CUSTOMER` (casse indifférente), le membre ou le client doit exister et ne pas être supprimé, sinon `400`.
3. Le serveur calcule les échéances, applique la première règle de routage qui correspond (assignation et statut `IN_PROGRESS`), copie la `description` comme premier message `CLIENT`, et envoie le courriel « Ticket ouvert » à `contactEmail` s'il est renseigné.
4. Réponse `201` : le ticket complet. Ouvrir sa fiche.

Pour qu'un membre voie le ticket dans son portail, envoyer exactement `ownerType: "MEMBER"` (majuscules) et son identifiant de membre en `ownerCode`. Le serveur ne déduit pas `contactEmail` du propriétaire : sans lui, aucun courriel ne part vers le demandeur.

### Back-office : fiche d'un ticket

1. `GET /sni/api/v1/support/tickets/{ticketNumber}` : ticket complet, avec tous les messages (notes internes comprises), les pièces jointes actives et les étiquettes.
2. `GET /sni/api/v1/support/tickets/{ticketNumber}/timeline` : chronologie (`eventType`, `actorType`, `actorId`, `description` en français, `createdAt`), dans l'ordre chronologique.
3. Échéances à afficher : `firstResponseDueAt` (si `firstRespondedAt` est vide), `resolutionDueAt` (si `resolvedAt` est vide), `escalationLevel`.
4. S'abonner au temps réel du ticket (voir [À prévoir](#à-prévoir-côté-frontend)).
5. Si `ownerType` + `ownerCode` sont renseignés, `GET /sni/api/v1/support/tickets/by-owner?ownerType=...&ownerCode=...` donne la vue du demandeur : `openTickets` (`OPEN`, `IN_PROGRESS`, `WAITING_CLIENT`), `slaBreaches`, `avgCsatScore`, `atRisk` (vrai si 3 tickets ouverts ou plus, un retard, ou une note moyenne sous 3) et les 5 derniers tickets (résumés).

### Back-office : répondre ou ajouter une note interne

1. Charger les modèles : `GET /sni/api/v1/support/quick-replies?category=<catégorie du ticket>`.
2. Proposer des articles : `GET /sni/api/v1/support/knowledge-base/suggestions?category=...&searchText=...&limit=5`.
3. `POST /sni/api/v1/support/tickets/{ticketNumber}/messages` ([détail](routes/SupportTicketController.md#r-post-sni-api-v1-support-tickets-ticketnumber-messages)) avec `content` (obligatoire) et `internal` (vrai pour une note interne). Laisser `senderType` vide (défaut `AGENT`). `senderId` et `senderName` sont facultatifs : vides, le serveur signe le message avec le courriel et le nom de l'agent connecté (le nom apparaît dans le courriel au client). Le corps est strict : `authorId`, `authorName` ou tout autre champ renvoie `400`.
4. Effets : `firstRespondedAt` posé au premier message d'agent ; un message public depuis `OPEN` ou `IN_PROGRESS` passe le ticket en `WAITING_CLIENT` et envoie le courriel « Réponse sur votre ticket » à `contactEmail` ; une note interne ne change pas le statut et n'envoie rien.
5. Réponse `201` : le ticket complet à jour. Remplacer la fiche affichée.

`content` accepte du texte, du HTML ou un document JSON TipTap/ProseMirror (`{"type":"doc",...}`) ; il est stocké tel quel (seulement rogné). Limite : 200 000 caractères.

### Back-office : assigner

1. `PATCH /sni/api/v1/support/tickets/{ticketNumber}/assign` (droit `SUPPORT:ASSIGN`) avec `{ "assignedTo": "<courriel ou id>" }`. **Préférer le courriel** de l'agent : un identifiant lu en nombre par JavaScript est arrondi et ne désigne plus personne. Un identifiant ou un courriel inconnu renvoie `404`. `null` ou vide désassigne.
2. Un ticket `OPEN` assigné passe en `IN_PROGRESS`. Un changement d'agent écrit un événement `ASSIGNED` et envoie un courriel au nouvel agent.
3. Réponse : le ticket complet, `assignedTo` étant le courriel de l'agent.

Préférer le courriel de l'agent : il évite la question des grands entiers, et un identifiant numérique n'est pas vérifié (un identifiant inexistant est accepté).

### Back-office : changer le statut

1. `PATCH /sni/api/v1/support/tickets/{ticketNumber}/status` avec `{ "status": "RESOLVED" }` (obligatoire).
2. Passage en `RESOLVED` : courriel au demandeur avec cinq liens de note (1 à 5 étoiles). Passage en `CLOSED` : `closedAt`, puis demande de note par courriel 24 h plus tard.
3. Proposer `RESOLVED` et `CLOSED` en priorité ; le serveur accepte aussi un retour en arrière.

### Back-office : pièces jointes

1. Envoyer : `POST /sni/api/v1/support/tickets/{ticketNumber}/attachments` en `multipart/form-data`, partie `file`, paramètres de requête facultatifs `internal=true` (cachée au membre) et `messageId` (rattache le fichier à un message du même ticket). Réponse `201` : la pièce jointe.
2. Lister : `GET .../attachments` (internes comprises).
3. Télécharger : `GET .../attachments/{id}/download` (octets, nom dans `Content-Disposition`).
4. Retirer : `DELETE .../attachments/{id}` (`204`, retrait logique : la pièce disparaît des listes).

### Back-office : étiquettes

1. Suggestions : `GET /sni/api/v1/support/tags` (toutes les étiquettes actives, par nom).
2. Ajouter : `POST /sni/api/v1/support/tickets/{ticketNumber}/tags` avec `{ "tag": "..." }`. L'étiquette est normalisée et créée si besoin ; ajouter deux fois ne fait rien. Réponse `201` : le ticket.
3. Retirer : `DELETE /sni/api/v1/support/tickets/{ticketNumber}/tags/{tag}` (nom tel qu'affiché). Réponse : le ticket.

### Back-office : créer une tâche depuis un ticket

1. `POST /sni/api/v1/support/tickets/{ticketNumber}/task` : exige `SUPPORT:WRITE` **et** `TASK:WRITE`. Corps : `title` (obligatoire), `description`, `assignedTo` (identifiant d'utilisateur, nombre int64), `priority` (priorité du module `task`), `dueAt`, `checklist`.
2. La tâche est créée dans le module `task` avec `relatedType` `SUPPORT_TICKET` et `relatedCode` le numéro du ticket. Un événement `TASK_CREATED` est écrit.
3. Réponse `201` : la tâche (`TaskResponse`, voir [task](../task/README.md)).

### Back-office : transformer une réponse en article

1. Sur un message du fil : `POST /sni/api/v1/support/tickets/{ticketNumber}/messages/{messageId}/convert-to-article` avec `title` (obligatoire), `slug`, `category` (défaut : celle du ticket), `tags`, `publicVisible`.
2. Le corps de l'article est le texte du message. Réponse `201` : l'article.

### Back-office : base de connaissances

1. Liste : `GET /sni/api/v1/support/knowledge-base?category=&searchText=&page=&size=` : articles **actifs** seulement, triés par nombre de vues puis date.
2. Lire : `GET /sni/api/v1/support/knowledge-base/{articleCode}` (actif ou non).
3. Créer : `POST /sni/api/v1/support/knowledge-base` avec `title`, `body` (obligatoires), `slug` (sinon tiré du titre ; rendu unique par suffixe `-2`, `-3`...), `category`, `tags` (liste), `publicVisible` (défaut faux), `internalOnly` (défaut vrai), `active` (défaut vrai).
4. Modifier : `PUT .../{articleCode}` avec le même corps ; `publicVisible`, `internalOnly`, `active` absents ne changent pas.
5. Retirer : `DELETE .../{articleCode}` (`204`, `active` passe à faux). Pour le remettre : `PUT` avec `active: true`.
6. Statistiques : `GET /sni/api/v1/support/knowledge-base/analytics` (droit `SUPPORT:METRICS`).

### Back-office : règles de routage

1. `GET /sni/api/v1/support/routing-rules` : toutes les règles, par `sortOrder` puis `id`.
2. `POST` ou `PUT .../{id}` avec `name` (obligatoire), critères facultatifs `category`, `priority`, `ownerType`, `relatedType` (un critère vide accepte tout), `assignedTo` (identifiant d'agent, nombre int64) ou `teamCode` (l'un des deux obligatoire), `active` (défaut vrai), `sortOrder` (défaut 0).
3. À la création d'un ticket, la première règle active qui correspond, dans l'ordre de `sortOrder`, assigne l'agent. Une règle sans `assignedTo` (seulement `teamCode`) s'arrête là : rien n'est assigné.
4. `DELETE .../{id}` supprime définitivement (`204`).

### Back-office : réponses rapides

`GET /sni/api/v1/support/quick-replies?category=` (actives seulement), `POST` avec `category`, `title`, `body` (tous obligatoires), `DELETE .../{id}` (désactivation). Pas de modification : supprimer et recréer.

### Back-office : tableaux de bord (droit `SUPPORT:METRICS`)

1. `GET /sni/api/v1/support/tickets/analytics?from=&to=` : période par défaut, les 30 derniers jours. Volumes, taux de dépassement SLA (en %), délais moyens en heures (arrondis au dixième), répartitions par catégorie, priorité, étiquette (`topTags`), type d'objet lié, charge par agent (`backlogByAgent`, clés = identifiant d'agent en texte), CSAT moyen global, par catégorie et par agent.
2. `GET /sni/api/v1/support/tickets/analytics.csv?from=&to=` : les mêmes chiffres en CSV (`support-analytics.csv`), à télécharger avec le jeton.
3. `GET /sni/api/v1/support/tickets/agents/performance?from=&to=` : une ligne par agent (`agentId` en nombre int64, attention à l'arrondi).

Pour afficher un nom d'agent, il faut faire la correspondance avec la liste des utilisateurs (module `securite`, droit `SYSTEM:USERS`) : le module support ne renvoie aucun nom d'agent.

### Portail membre : mes tickets

1. Résumé d'accueil : `GET /sni/api/v1/client/support/tickets/summary` (`openTickets`, `slaBreaches`, `avgCsatScore`, `atRisk`, 5 derniers tickets).
2. Liste : `GET /sni/api/v1/client/support/tickets?status=&category=&searchText=&page=&size=` : seulement les tickets dont le propriétaire est le membre connecté (`MEMBER` + son identifiant). Mêmes résumés que le back-office, sans notes internes dans les compteurs.
3. Fiche : `GET /sni/api/v1/client/support/tickets/{ticketNumber}`. Le ticket d'un autre propriétaire répond `404` (« Ticket not found »). La vue portail retire les notes internes, les pièces jointes internes, `assignedTo` (toujours `null`) et `escalationReason` (toujours `null`).

### Portail membre : ouvrir un ticket

1. Avant l'envoi, proposer des articles : `GET /sni/api/v1/client/support/knowledge-base/suggestions?category=...&searchText=<titre saisi>`.
2. `POST /sni/api/v1/client/support/tickets` ([détail](routes/ClientSupportController.md#r-post-sni-api-v1-client-support-tickets)) avec `title` (obligatoire, 255 caractères au plus), `description` (5 000 au plus), `priority`, `category`, `relatedType` + `relatedCode` (par exemple la réservation ou la facture concernée). Nom, courriel et téléphone sont pris du membre.
3. Réponse `201` : le ticket. Le membre reçoit le courriel « Ticket ouvert ».
4. Joindre des fichiers ensuite : `POST /sni/api/v1/client/support/tickets/{ticketNumber}/attachments` (`multipart/form-data`, partie `file`).

### Portail membre : échanger, fermer, noter

1. Répondre : `POST /sni/api/v1/client/support/tickets/{ticketNumber}/messages` avec `content` (obligatoire, 10 000 caractères au plus). Le ticket passe en `IN_PROGRESS` ; s'il était `CLOSED`, il est rouvert. Réponse `201` : le ticket.
2. Pièces jointes : `GET .../attachments`, `GET .../attachments/{id}/download` (les internes répondent `404`). Pas de suppression côté portail.
3. Fermer : `POST /sni/api/v1/client/support/tickets/{ticketNumber}/close` (quel que soit le statut). Réponse : le ticket `CLOSED`.
4. Noter : sur un ticket `RESOLVED` ou `CLOSED` sans note (`csatSubmittedAt` vide), `POST .../csat` avec `score` (1 à 5, obligatoire) et `comment`. Une seule note par ticket.

### Portail membre : base de connaissances

1. `GET /sni/api/v1/client/support/knowledge-base?category=&searchText=` : articles actifs et `publicVisible`.
2. `GET /sni/api/v1/client/support/knowledge-base/{slug}` : l'article ; chaque lecture ajoute une vue.

### Public : note depuis le courriel

Les courriels « Ticket résolu » et « Donnez votre avis » contiennent cinq liens `GET /sni/api/v1/public/support/csat?ticket=<numéro>&score=<1..5>&token=<signature>` ([détail](routes/SupportCsatPublicController.md#r-get-sni-api-v1-public-support-csat)), construits sur `app.api-base-url`. Le serveur répond toujours `200` avec une **page HTML complète** (« Merci pour votre avis », « Déjà noté », « Ticket non éligible » ou « Lien invalide »). Le frontend n'a rien à construire : ces liens pointent sur l'API, pas sur le frontend. La signature ne vaut que pour le couple ticket et note, et n'expire pas.

## Règles et erreurs à refléter dans l'écran

| Situation | Réponse | Message |
| --- | --- | --- |
| Titre vide (back-office) | `400` | `Ticket title is required` (ou `VALIDATION_FAILED` sur `title`) |
| Propriétaire `MEMBER` ou `CUSTOMER` supprimé ou inconnu | `400` | `Le membre X est supprimé ou introuvable et ne peut pas ouvrir de ticket` (ou `Le client X ...`) |
| Texte de plus de 200 000 caractères (description, message) | `400` | `Ce texte est trop long (N caractères, 200000 au plus). Joignez les images et les documents en pièce jointe.` |
| Portail : titre de plus de 255, description de plus de 5 000, message de plus de 10 000 caractères | `400` `VALIDATION_FAILED` | détail dans `errors` |
| Message vide | `400` | `Message is required` |
| Numéro de ticket inconnu | `404` | `Support ticket not found: TCK-...` (portail : `Ticket not found`) |
| Statut absent dans `PATCH .../status` | `400` | `Ticket status is required` |
| Assignation : valeur ni numérique ni courriel | `400` | `Assigned agent must be a user id or email` |
| Assignation : courriel inconnu | erreur du module `securite` (non vérifié : statut exact) | |
| Note hors de 1 à 5 | `400` | `VALIDATION_FAILED` ou `CSAT score must be between 1 and 5` |
| Note sur un ticket ni `RESOLVED` ni `CLOSED` | `400` | `CSAT can only be submitted for resolved or closed tickets` |
| Note déjà donnée | `400` | `CSAT already submitted for this ticket` |
| Fichier absent ou vide | `400` | `Le fichier joint est requis` |
| Type de fichier refusé | `400` | `Type de fichier non autorisé : <type>` |
| Fichier de plus de 10 Mo | refusé par la limite globale d'envoi (voir [Corps des requêtes](../README.md#corps-des-requêtes)) | Le contrôle du module (15 Mo) n'est jamais atteint. |
| `messageId` d'un autre ticket | `400` | `Le message ne correspond pas à ce ticket` |
| Pièce jointe inconnue, retirée, ou interne vue du portail | `404` | `Pièce jointe introuvable : <id>` |
| Étiquette vide | `400` | `Le tag est requis` |
| Vue demandeur sans `ownerType` ou `ownerCode` | `400` | `ownerType et ownerCode sont requis` |
| Règle de routage sans nom | `400` | `Le nom de la règle est requis` |
| Règle sans agent ni équipe | `400` | `La règle doit définir un agent ou une équipe` |
| Article sans titre ou sans contenu | `400` | `Le titre de l'article est requis`, `Le contenu de l'article est requis` |
| Article inconnu, ou non public vu du portail | `404` | `Knowledge article not found: ...` |
| Tâche depuis un ticket sans `TASK:WRITE` | `403` | |

Types de fichiers acceptés en pièce jointe : PNG, JPEG, WebP, GIF, PDF, Word (`.doc`, `.docx`), Excel (`.xls`, `.xlsx`), texte et CSV. Le type est lu dans l'en-tête de la partie : un navigateur qui envoie `application/octet-stream` sera refusé.

Plusieurs messages d'erreur sont en anglais (`Ticket title is required`, `Support ticket not found`, `CSAT ...`) : prévoir une traduction côté écran à partir du statut et du contexte.

## À prévoir côté frontend

### Écrans

- Back-office : file des tickets (filtres, compteurs), fiche ticket (fil, notes internes, pièces jointes, étiquettes, chronologie, échéances, escalade, assignation, statut, création de tâche, conversion en article), création de ticket, base de connaissances, règles de routage, réponses rapides, tableau de bord.
- Portail : liste et résumé de mes tickets, ouverture d'un ticket (avec suggestions d'articles), fiche avec fil et pièces jointes, fermeture, note, base de connaissances.

### Droits qui conditionnent les boutons

| Action | Droit |
| --- | --- |
| Voir la file, une fiche, la chronologie, les pièces jointes, les étiquettes, les articles, les règles, les réponses rapides | `SUPPORT:READ` |
| Créer, répondre, changer le statut, joindre, retirer une pièce, étiqueter, convertir en article, gérer articles, règles et réponses rapides | `SUPPORT:WRITE` |
| Assigner | `SUPPORT:ASSIGN` |
| Compteurs, tableaux de bord, CSV, performance des agents, statistiques des articles | `SUPPORT:METRICS` |
| Créer une tâche depuis un ticket | `SUPPORT:WRITE` et `TASK:WRITE` |

### Listes de référence

Les énumérations ci-dessus (statut, priorité, catégorie) sont fixes : les coder en dur avec leurs libellés. Les étiquettes viennent de `GET /sni/api/v1/support/tags`. Il n'existe pas de liste « agents du support » dans ce module : le choix d'un agent passe par la liste des utilisateurs du module `securite` (droit `SYSTEM:USERS`) ou par la saisie d'un courriel.

### Identifiants d'agent

- `SupportTicketResponse.assignedTo` est le courriel de l'agent : le garder tel quel, et le renvoyer tel quel dans le filtre `assignedTo` de la file et pour réassigner.
- Réponses par courriel : un client (ou un agent) qui répond à une notification en gardant `TCK-...` dans l'objet voit sa réponse ajoutée au ticket. Les notifications automatiques que la plateforme reçoit dans sa propre boîte sont écartées.
- `RoutingRuleResponse.assignedTo`, `RoutingRuleRequest.assignedTo`, `CreateTicketTaskRequest.assignedTo`, `AgentWorkloadEntry.agentId`, `AgentPerformanceEntry.agentId`, `AttachmentResponse.uploadedBy`, `KnowledgeArticleResponse.createdBy` et `updatedBy` restent des nombres int64 : lire avec un analyseur qui préserve les grands entiers, et écrire le nombre sans passer par un `number` JavaScript (voir [Identifiants](../README.md#identifiants--attention-aux-grands-nombres)).
- Les autres `id` du module (messages, pièces jointes, règles, réponses rapides, étiquettes, articles) sont de petites séquences, sans risque d'arrondi.

### Contenus riches

Descriptions, messages, réponses rapides et articles sont stockés sans nettoyage : texte, HTML ou JSON TipTap. Le frontend doit **assainir** le HTML avant de l'afficher (risque d'injection de script), et savoir afficher un document TipTap. Un message trop volumineux revient remplacé par `[Message trop volumineux pour être affiché : N caractères]`.

### Fichiers

Téléchargements de pièces jointes et du CSV : appeler avec l'en-tête `Authorization`, récupérer le contenu en blob et proposer l'enregistrement avec le nom de `Content-Disposition`. Un lien `<a href>` direct échoue en `401`.

### Temps réel

- Back-office uniquement : `/topic/support/` est réservé aux rôles d'administration ; un membre du portail est refusé à l'abonnement. Le portail doit donc relire la fiche (après chaque envoi, et périodiquement s'il veut voir les réponses).
- Sujet : `/topic/support/tickets/{ticketNumber}`. Chaque nouveau message (agent, note interne, membre, courriel entrant) y est publié après validation en base, sous la forme d'un `TicketMessageResponse` (`id`, `senderType`, `senderId`, `senderName`, `content`, `internal`, `createdAt`).
- Rien n'est publié pour un changement de statut, une assignation, une pièce jointe, une étiquette ou une escalade. Sur réception d'un message, ajouter le message au fil puis relire la fiche (`GET .../{ticketNumber}`), car le statut a pu changer (`IN_PROGRESS`, `WAITING_CLIENT`, réouverture). Ignorer un message dont l'`id` est déjà affiché (l'auteur le reçoit aussi dans sa réponse HTTP).
- Connexion : voir [Temps réel](../README.md#temps-réel).

### Champs à ne pas envoyer

- Portail : seulement les champs de `ClientCreateTicketRequest` (`title`, `description`, `priority`, `category`, `relatedType`, `relatedCode`) et `ClientAddMessageRequest` (`content`). Pas de `ownerCode`, `contactEmail`, `senderType`.
- Back-office, message : ne pas envoyer `senderType: "CLIENT"` depuis l'écran agent (le serveur l'accepte et traiterait la réponse comme venant du demandeur).
- Ne pas renvoyer un ticket reçu tel quel : JSON strict, propriété inconnue refusée.

### Pièges

- Le tri demandé à la file des tickets (`sort`) est sans effet : ordre fixe par création décroissante.
- `openTickets` n'a pas le même sens partout : `metrics` compte `OPEN` + `IN_PROGRESS`, la vue demandeur et le résumé portail ajoutent `WAITING_CLIENT`.
- La priorité et la catégorie d'un ticket ne se modifient par aucune route ; seule l'escalade SLA monte la priorité d'un cran.
- La liste des articles au back-office cache les articles retirés : garder leur `articleCode` pour pouvoir les réactiver.

## Effets automatiques

| Automatisme | Fréquence | Effet visible |
| --- | --- | --- |
| Échéances et escalade (`src/main/java/com/sni/bokaticowork/features/support/worker/SupportSlaEscalationWorker.java:30`) | Toutes les heures (`bokati.support.sla-check-delay-ms`, défaut 3 600 000 ms) | Sur les tickets `OPEN` ou `IN_PROGRESS` en retard : un courriel d'alerte unique au responsable par type de dépassement (première réponse, résolution) ; au premier dépassement de résolution, priorité montée d'un cran (événement `PRIORITY_CHANGED`) ; escalade de niveau 1 à 3 (`escalationLevel`, `escalatedAt`, `escalationReason`, événement `SLA_ESCALATED`, courriel au destinataire du niveau). |
| Fermeture automatique (`.../worker/SupportAutoCloseWorker.java:33`) | Toutes les heures | Un ticket `RESOLVED` depuis plus de N jours sans message du membre depuis la résolution passe `CLOSED` (événement `AUTO_CLOSED`). N = réglage `bokati.support.auto-close-delay-days` (module `parametres`, modifiable à chaud, défaut 5). |
| Demande de note (`.../worker/SupportCsatRequestWorker.java:24`) | Toutes les heures | Pour un ticket `CLOSED` depuis plus de 24 h, avec `contactEmail`, une seule fois : courriel « Donnez votre avis » avec les liens de note publics. |
| Réponses par courriel (`.../worker/SupportInboundEmailWorker.java:56`) | Toutes les minutes (`bokati.support.inbound-poll-delay-ms`) | Lit les courriels non lus de la boîte support (Microsoft Graph). Si l'objet contient `TCK-<chiffres>` d'un ticket existant, le texte (citations et signatures retirées) devient un message `CLIENT` : ticket `IN_PROGRESS`, ou réouvert s'il était `CLOSED`. Les pièces jointes du courriel sont ajoutées au ticket (non internes). |
| Tickets automatiques | À l'événement | `booking` : dépassement du quota mensuel de no-show (priorité `HIGH`, catégorie `BOOKING`, propriétaire `MEMBER`/`CUSTOMER`/`BUSINESS_ENTITY`, `relatedType` `BOOKING_NO_SHOW_QUOTA`). `payment` : anomalie de caisse à risque élevé (`HIGH`, `BILLING`, propriétaire `CASHIER`, `relatedType` `CASH_ANOMALY_FLAG`). Pas de doublon tant qu'un ticket du même objet lié est `OPEN`, `IN_PROGRESS` ou `WAITING_CLIENT`. |

Courriels envoyés par le module (tous depuis `bokati.support.sender-email`, mis en file d'envoi) :

| Moment | Destinataire |
| --- | --- |
| Création | `contactEmail` (« Ticket ouvert ») |
| Réponse publique d'agent | `contactEmail` |
| Message du membre, courriel entrant | Agent assigné, sinon responsable |
| Réouverture par le membre | `contactEmail` et agent assigné (sinon responsable) |
| Passage en `RESOLVED` | `contactEmail`, avec les liens de note |
| Assignation à un nouvel agent | Le nouvel agent (gabarit « nouveau message client », sans message) |
| Alerte SLA, escalade | Responsable, agent ou direction selon le niveau |

Notifications internes (module `notification`, canal `IN_APP`, destinataire l'agent assigné) : `SUPPORT_TICKET_ROUTED` quand une règle de routage assigne un ticket neuf, `SUPPORT_CLIENT_REPLY` quand le demandeur répond (portail ou courriel). Le membre ne reçoit aucune notification interne du module, seulement des courriels.

## Écarts relevés

1. **Ticket automatique de no-show visible par le membre.** `src/main/java/com/sni/bokaticowork/features/booking/service/support/BookingPolicyEnforcer.java:105` crée un ticket avec le propriétaire `MEMBER` et son code : il apparaît dans le portail du membre, et la description interne (« a atteint N absence(s) non justifiée(s)... ») y figure comme premier message signé `CLIENT` (`SupportTicketServiceImpl.java:123`).
2. **Identifiants d'agent encore en int64.** Seul `SupportTicketResponse.assignedTo` est en texte. `RoutingRuleRequest.assignedTo` et `RoutingRuleResponse.assignedTo` (`src/main/java/com/sni/bokaticowork/features/support/dto/SupportDtos.java:63` et `:174`), `CreateTicketTaskRequest.assignedTo` (`:71`), `AgentWorkloadEntry.agentId` (`:214`), `AgentPerformanceEntry.agentId` (`:220`), `AttachmentResponse.uploadedBy` (`:91`) sont arrondis par JavaScript ; une règle relue puis réenregistrée peut pointer sur un autre identifiant. Le filtre `assignedTo` de `GET /sni/api/v1/support/tickets` est aussi un `Long` (`SupportTicketController.java:53`).
3. **Assignation : courriel trompeur à l'agent.** `SupportTicketServiceImpl.java:195` envoie `sendClientMessage(saved, null)` : l'agent reçoit « [Support] Nouveau message client » avec un message vide au lieu d'un avis d'assignation. Un identifiant numérique inexistant est accepté sans contrôle (`:726`).
4. **Réponses par courriel sans contrôle d'expéditeur.** `src/main/java/com/sni/bokaticowork/features/support/worker/SupportInboundEmailWorker.java:114` ajoute comme message `CLIENT` tout courriel dont l'objet contient un numéro de ticket, quel que soit l'expéditeur ; ses pièces jointes sont stockées sans contrôle de type ni de taille.
5. **Note publique par simple `GET`.** `src/main/java/com/sni/bokaticowork/features/support/controller/SupportCsatPublicController.java:24` enregistre la note à l'ouverture du lien, sans expiration du jeton : un antivirus de messagerie qui visite les liens peut noter à la place du destinataire (le premier lien visité gagne).
6. **Demande de note renvoyée même après une note.** `src/main/java/com/sni/bokaticowork/features/support/repository/SupportTicketRepository.java:119` ne vérifie pas `csatSubmittedAt` : un membre qui a déjà noté reçoit quand même « Donnez votre avis ».
7. **Règle de routage par équipe sans effet.** `teamCode` est accepté seul (`SupportRoutingRuleServiceImpl.java`, méthode `validate`), mais `SupportTicketServiceImpl.java:794` ne fait rien si la règle n'a pas d'agent, et rien n'exploite `teamCode`.
8. **Limite de taille incohérente.** `SupportAttachmentService.java:32` annonce 15 Mo, la limite globale (`src/main/resources/application.yml:20`) coupe à 10 Mo.
9. **Statuts sans garde.** `PATCH .../status` accepte toute transition (`SupportTicketServiceImpl.java:203`) ; une réouverture par l'agent ne remet pas `resolvedAt` ni `closedAt` à vide et n'écrit pas `REOPENED`. Un message du membre sur un ticket `RESOLVED` le repasse en `IN_PROGRESS` sans effacer `resolvedAt` (`:271`), il sort alors de toute surveillance SLA.
10. **Fermeture par le membre journalisée comme un agent.** `POST /sni/api/v1/client/support/tickets/{ticketNumber}/close` passe par `updateStatus`, qui écrit l'événement avec `actorType` `AGENT` (`SupportTicketServiceImpl.java:218`).
11. **Articles : `internalOnly` sans effet.** Un article est créé `internalOnly: true` par défaut (`KnowledgeArticleServiceImpl.java:61`) mais le portail ne filtre que sur `publicVisible` (`:114`) : un article marqué à la fois public et interne est publié. La lecture au portail (`GET`) incrémente un compteur (`:116`). La réponse portail expose aussi `createdBy`, `updatedBy`, `internalOnly`.
12. **Numéros par horodatage.** `TCK-` + millisecondes (`SupportTicketServiceImpl.java:101`) et `KB-` + millisecondes (`KnowledgeArticleServiceImpl.java:54`, `:143`) : deux créations dans la même milliseconde violent la contrainte d'unicité (erreur serveur).
13. **Résumé portail trop bavard.** `src/main/java/com/sni/bokaticowork/features/portal/support/service/ClientSupportService.java:134` renvoie au membre `atRisk` et `slaBreaches`, indicateurs internes ; `escalationLevel` et `escalatedAt` restent aussi visibles dans la vue portail (`:149`), seul `escalationReason` est masqué.
14. **Événements jamais écrits.** `CATEGORY_CHANGED` et `MERGED` existent dans `src/main/java/com/sni/bokaticowork/features/support/enums/TicketEventType.java` sans aucun code qui les produise ; aucune route ne change la catégorie ni ne fusionne des tickets.
15. **Messages d'erreur en anglais** dans `SupportTicketServiceImpl.java` (`Ticket title is required`, `Support ticket not found`, `CSAT ...`, `Assigned agent ...`), `ClientSupportService.java:179` (`Ticket not found`) et `KnowledgeArticleServiceImpl.java` (`Knowledge article not found`).
