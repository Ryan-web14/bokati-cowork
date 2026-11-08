# crm : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module suit les prospects commerciaux (leads) du premier contact jusqu'à leur conversion en client ou membre, avec un tableau Kanban, des activités (appels, rendez-vous, notes), des opportunités rattachées à un lead et des propositions commerciales chiffrées ligne par ligne. Il sert le back-office (droits `CRM:READ`, `CRM:WRITE`, `CRM:CONVERT`) et expose une seule route publique : le formulaire de contact du site web. Il s'appuie sur `billing` (génération d'un devis depuis un lead), sur `securite` (les agents assignés sont des comptes utilisateurs, identifiés par leur id numérique) et sur la file de courriels (alertes aux agents). La conversion ne crée rien dans `client` : elle enregistre seulement le code d'un client ou membre existant.

## Notions

| Objet | Identifiant exposé | Lien | Remarques |
| --- | --- | --- | --- |
| Lead (prospect) | `id` numérique (toutes les routes d'écriture), `leadNumber` lisible (`LDN-<horodatage en ms>`, lecture seule par `GET /sni/api/v1/crm/leads/number/{leadNumber}`) | porte ses activités, ses opportunités, ses propositions | `stage` = étape du pipeline. `scoredProbability` (dans `LeadResponse`) et `score` (carte Kanban) sont calculés par le serveur, `probability` est la valeur saisie. |
| Activité de lead | `id` numérique, jamais utilisé en entrée | appartient à un lead | Aucune route de modification ni de suppression. Renvoyées dans `LeadResponse.activities`, de la plus récente à la plus ancienne (`performedAt`). |
| Opportunité | `id` numérique (route d'étape), `opportunityNumber` (`OPP-<horodatage en ms>`, affichage seul) | un lead obligatoire | Pas de route de lecture unitaire ni de modification : seulement création, liste, changement d'étape. |
| Proposition commerciale | `id` numérique (toutes les routes d'écriture), `proposalNumber` (`PROP-AAAA-NNNNN`, séquence `crm_proposal`, lecture par `GET /sni/api/v1/crm/proposals/number/{proposalNumber}`) | facultativement une opportunité (`opportunityId`) et un lead (`leadId`) | Totaux (`subtotalAmount`, `taxAmount`, `totalAmount`) recalculés par le serveur à chaque modification de ligne. |
| Ligne de proposition | `id` numérique (`lineId` dans le chemin) | appartient à une proposition | Renvoyée dans `ProposalResponse.lines`. |
| Devis | `quoteNumber` (numéro de document du module `billing`) | créé depuis un lead | Vit ensuite dans `billing` avec `sourceType = CRM_LEAD` et `sourceCode = <leadNumber>`. |
| Agent assigné (`assignedTo`) | id numérique du compte utilisateur | lead, opportunité, proposition | C'est l'`id` de `AdminUserResponse` ([securite](../securite/routes/AdminUserController.md#r-get-sni-api-v1-admin-users)), **pas** le `userId` texte de `/auth/me`. |

Tous les identifiants numériques sont des int64 : appliquer la règle [Identifiants](../README.md#identifiants--attention-aux-grands-nombres) (les garder en texte).

Énumérations utiles aux listes déroulantes (valeurs exactes dans les fichiers `routes/`) :

- `LeadSource` : `WEBSITE`, `REFERRAL`, `COLD_CALL`, `TRADE_SHOW`, `PARTNER`, `SOCIAL_MEDIA`, `EVENT`, `OTHER`.
- `LeadInterest` : `WORKSPACE`, `MEETING_ROOM`, `VIRTUAL_OFFICE`, `DOMICILIATION`, `DAY_PASS`, `DEDICATED_DESK`, `PRIVATE_OFFICE`, `OTHER`.
- `LeadActivityType` : `CALL`, `EMAIL`, `MEETING`, `VISIT`, `NOTE`.

## Statuts et cycle de vie

### Lead (`stage`)

Aucune transition n'est imposée par le serveur : `PATCH .../stage` accepte n'importe quelle étape depuis n'importe quelle autre, y compris un retour de `WON` ou `LOST` vers une étape active.

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `NEW` | Prospect enregistré, pas encore traité | création (`POST /sni/api/v1/crm/leads` ou `POST /sni/api/v1/public/crm/leads`), ou `PATCH .../stage` | toute étape |
| `CONTACTED` | Premier contact établi | `PATCH .../stage` uniquement | toute étape |
| `QUALIFIED` | Besoin et budget confirmés | `PATCH /sni/api/v1/crm/leads/{id}/qualify` (raccourci) ou `PATCH .../stage` | toute étape |
| `PROPOSAL_SENT` | Offre envoyée | `PATCH .../stage` uniquement. Envoyer une proposition (`PATCH .../proposals/{id}/send`) ou générer un devis **ne change pas** l'étape du lead. | toute étape |
| `WON` | Gagné | `PATCH /sni/api/v1/crm/leads/{id}/convert` (renseigne aussi `convertedOwnerType` et `convertedOwnerCode`) ou `PATCH .../stage` (sans conversion enregistrée) | toute étape |
| `LOST` | Perdu | `PATCH .../stage` avec `stage = LOST` et `lostReason` facultatif | toute étape. `lostReason` est effacé dès que l'étape n'est plus `LOST`. |

Score calculé (`scoredProbability`, `score`) : `WON` = 100, `LOST` = 0 ; sinon base par étape (`NEW` 5, `CONTACTED` 20, `QUALIFIED` 40, `PROPOSAL_SENT` 65), plus 5 par activité (20 au plus), plus 5 si la dernière activité date de 7 jours ou moins, moins 5 si elle est plus ancienne, borné entre 1 et 95 (`src/main/java/com/sni/bokaticowork/features/crm/service/implementation/CrmServiceImpl.java:424`).

### Opportunité (`stage`)

Libre aussi : toute étape vers toute autre par `PATCH /sni/api/v1/crm/opportunities/{id}/stage`.

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `OPEN` | Ouverte (valeur à la création) | création `POST /sni/api/v1/crm/leads/{id}/opportunities` | toute étape |
| `NEGOTIATION` | En négociation | `PATCH .../stage` | toute étape |
| `PROPOSAL_SENT` | Offre envoyée | `PATCH .../stage` uniquement (jamais posé automatiquement) | toute étape |
| `WON` | Gagnée, `wonAt` horodaté | `PATCH .../stage` | toute étape (`wonAt` reste rempli) |
| `LOST` | Perdue, `lostAt` horodaté, `lostReason` facultatif | `PATCH .../stage` | toute étape (`lostAt` reste rempli, `lostReason` est effacé) |

### Proposition commerciale (`status`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Brouillon (valeur à la création) | `POST /sni/api/v1/crm/proposals` | `SENT` |
| `SENT` | Envoyée, `sentAt` horodaté | `PATCH /sni/api/v1/crm/proposals/{id}/send` (seulement depuis `DRAFT`) | `ACCEPTED`, `REJECTED` |
| `VIEWED` | Vue par le destinataire | **jamais atteint** (aucun code ne le pose) | `ACCEPTED`, `REJECTED` |
| `ACCEPTED` | Acceptée, `acceptedAt` horodaté | `PATCH .../accept` depuis `SENT` ou `VIEWED` | aucune sortie |
| `REJECTED` | Refusée, `rejectedAt` horodaté, `rejectionReason` facultatif | `PATCH .../reject` depuis `SENT` ou `VIEWED` | aucune sortie |
| `EXPIRED` | Échue | **jamais atteint** : aucun worker ne compare `validUntil` à la date du jour | aucune |
| `CONVERTED` | Transformée en facture, contrat, abonnement ou pass | **jamais atteint** ; les champs `converted*` ne sont jamais remplis | aucune |

Pas de retour en `DRAFT`, pas de suppression de proposition.

## Parcours

### Formulaire de contact public (site web)

1. `POST /sni/api/v1/public/crm/leads` sans jeton ([détail](routes/PublicCrmController.md#r-post-sni-api-v1-public-crm-leads)), avec au minimum `fullName`, et en pratique `email`, `phone`, `company`, `note`, `interest`, `source: "WEBSITE"`.
2. Réponse `201` : `{ "leadNumber": "LDN-...", "message": "Votre demande a bien été reçue. Notre équipe vous contactera sous 24h." }`. Afficher `message` tel quel.
3. Le lead apparaît dans la colonne `NEW` du Kanban back-office, sans agent assigné. Aucun courriel n'est envoyé au prospect ni à l'équipe (le courriel « lead assigné » ne part que si `assignedTo` est fourni).

### Tableau Kanban des leads

1. `GET /sni/api/v1/crm/board` avec filtres facultatifs `assignedTo`, `source`, `interest`, `searchText` (cherche dans nom, courriel, société, `leadNumber`).
2. La réponse contient toujours les six colonnes, dans l'ordre `NEW`, `CONTACTED`, `QUALIFIED`, `PROPOSAL_SENT`, `WON`, `LOST`, chacune avec `count`, `totalAmount` (somme des `estimatedAmount`) et ses `cards` triées par dernière activité (à défaut date de création) décroissante. `totalPipelineValue` exclut la colonne `LOST` mais inclut `WON`. Pas de pagination : tous les leads sont renvoyés.
3. Glisser une carte : `PATCH /sni/api/v1/crm/leads/{id}/stage` avec `{ "stage": "CONTACTED" }`. Pour `LOST`, ouvrir une boîte demandant le motif et l'envoyer dans `lostReason`. Pour `WON`, préférer le parcours de conversion ci-dessous.
4. Recharger la colonne ou remplacer la carte avec le `LeadResponse` renvoyé (le format diffère de la carte : `scoredProbability` au lieu de `score`, pas d'`activityCount`, mais la liste `activities`).

### Liste et recherche des leads

1. `GET /sni/api/v1/crm/leads` paginé ([pagination](../README.md#paramètres-de-pagination)), filtres `stage`, `assignedTo`, `source`, `searchText`. Le tri est toujours « création décroissante » : le paramètre `sort` est ignoré.
2. Chaque élément est un `LeadResponse` complet, activités comprises.

Vue « pipeline » alternative : `GET /sni/api/v1/crm/pipeline` renvoie `stages`, un objet dont les clés sont les six étapes et les valeurs des listes complètes de `LeadResponse` (sans filtre ni pagination). Pour un écran, préférer `/board`, plus léger.

### Créer ou modifier un lead (back-office)

1. Charger la liste des agents (`GET /sni/api/v1/admin/users`, droit `SYSTEM:USERS`) pour proposer `assignedTo`.
2. `POST /sni/api/v1/crm/leads` avec `fullName` (obligatoire), coordonnées, `source`, `interest`, `estimatedAmount`, `probability`, `expectedCloseDate` (`AAAA-MM-JJ`), `assignedTo`. Réponse `201` avec le `LeadResponse` en `NEW`.
3. Modification : `PUT /sni/api/v1/crm/leads/{id}` avec les seuls champs à changer. Un champ absent ou `null` est laissé tel quel. Un texte vide (`""`) pour `email`, `phone`, `company` ou `note` efface la valeur. `fullName` vide est ignoré. `assignedTo` ne peut pas être retiré, seulement remplacé. L'étape ne se change pas ici.

### Fiche d'un lead : activités

1. `GET /sni/api/v1/crm/leads/{id}` (ou par `leadNumber`).
2. Ajouter une activité : `POST /sni/api/v1/crm/leads/{id}/activities` avec `activityType` (obligatoire), `subject`, `notes`, `performedBy` (texte libre, le serveur ne le déduit pas du jeton : y mettre le nom de l'utilisateur connecté), `performedAt` (date-heure avec fuseau, par défaut maintenant). Réponse `201` avec le lead complet mis à jour.
3. L'ajout remet `lastActivityAt` à maintenant et réarme l'alerte « lead dormant ».

### Qualifier, puis convertir un lead

1. Qualifier : `PATCH /sni/api/v1/crm/leads/{id}/qualify`, sans corps. Équivaut à `stage = QUALIFIED`.
2. Convertir : le serveur **ne crée pas** le client ni le membre. L'écran doit d'abord le créer ou le sélectionner dans le module `client` (voir [client](../client/routes.md)), récupérer son code, puis appeler `PATCH /sni/api/v1/crm/leads/{id}/convert` avec `{ "ownerType": "CUSTOMER", "ownerCode": "<code>" }` (droit `CRM:CONVERT`).
3. Le lead passe en `WON`, `convertedOwnerType` et `convertedOwnerCode` sont renseignés. Afficher sur la fiche un lien vers le client ou membre à partir de ces deux champs.

`ownerType` et `ownerCode` sont du texte libre : le serveur ne vérifie ni la valeur de `ownerType` ni l'existence du code. Utiliser `CUSTOMER` ou `MEMBER` (convention des anciens documents, non contrôlée par le code).

### Générer un devis depuis un lead

1. `POST /sni/api/v1/crm/leads/{id}/generate-quote` avec `{ "currency": "XAF", "performedBy": "<nom>" }`.
2. Le serveur crée dans `billing` un devis manuel d'une ligne : libellé = intérêt du lead (ou « Prestation Bokati Cowork »), quantité 1, prix unitaire = `estimatedAmount` du lead (**0** s'il est vide), client = nom, courriel et téléphone du lead, date du jour, échéance à 30 jours.
3. Réponse `201` : `{ "quoteNumber": "<numéro du document>" }`. Le lead reçoit une activité `NOTE` « Devis généré ». L'étape du lead ne change pas.
4. Proposer d'ouvrir le devis dans l'écran de facturation (voir [billing](../billing/routes.md)) à partir de `quoteNumber`.

Prévenir l'utilisateur avant l'envoi si `estimatedAmount` est vide.

### Opportunités

1. Créer : `POST /sni/api/v1/crm/leads/{id}/opportunities` avec `title` (obligatoire), `estimatedAmount`, `probability`, `expectedCloseDate`, `assignedTo`, `notes`. Réponse `201`, étape `OPEN`.
2. Lister : `GET /sni/api/v1/crm/opportunities` paginé, filtre `stage` seul (pas de filtre par lead : pour la fiche d'un lead, filtrer côté écran sur `leadId` ou `leadNumber`).
3. Changer d'étape : `PATCH /sni/api/v1/crm/opportunities/{id}/stage` avec `stage` (obligatoire) et `lostReason` pour `LOST`.

### Propositions commerciales

1. Créer le brouillon : `POST /sni/api/v1/crm/proposals` avec `title` (obligatoire), `leadId` et/ou `opportunityId`, `recipientType`, `recipientCode`, `recipientName`, `recipientEmail`, `validUntil`, `currency`, `notes` (visibles par le client), `internalNotes`, `assignedTo`. Réponse `201`, statut `DRAFT`, totaux à 0.
2. Ajouter des lignes : `POST /sni/api/v1/crm/proposals/{id}/lines` avec `label` et `unitPrice` (obligatoires), `quantity` (défaut 1), `discountPercent` (défaut 0, en pourcentage), `taxRate` (défaut 0, en pourcentage, voir [Règles](#règles-et-erreurs-à-refléter-dans-lécran)), `taxIncluded` (défaut `false`), `sortOrder`, `serviceType`, `serviceRefCode`, `currency`. Chaque appel renvoie la proposition complète avec ses totaux recalculés : remplacer l'état de l'écran par cette réponse.
3. Modifier ou retirer une ligne : `PUT /sni/api/v1/crm/proposals/{id}/lines/{lineId}` (champs à changer seulement ; `serviceType`, `serviceRefCode` et `currency` ne sont pas modifiables) ou `DELETE .../lines/{lineId}` (renvoie aussi la proposition, statut `200`).
4. Modifier l'en-tête : `PUT /sni/api/v1/crm/proposals/{id}` (`title`, `recipientName`, `recipientEmail`, `validUntil`, `notes`, `internalNotes`, `assignedTo`). Le lead, l'opportunité, `recipientType`, `recipientCode` et `currency` ne sont plus modifiables après création.
5. Envoyer : `PATCH /sni/api/v1/crm/proposals/{id}/send`. Le statut passe à `SENT`. **Aucun courriel n'est envoyé** et aucun PDF n'est produit : l'envoi au client se fait hors application.
6. Enregistrer la réponse du client : `PATCH .../accept` (sans corps) ou `PATCH .../reject` avec `{ "reason": "..." }`.
7. Liste : `GET /sni/api/v1/crm/proposals` paginé, tri par défaut `createdAt` décroissant (le paramètre `sort` est respecté), filtres `status`, `opportunityId`, `searchText` (numéro, titre, nom du destinataire). Pas de filtre par lead.

Calcul d'une ligne (`src/main/java/com/sni/bokaticowork/features/crm/service/implementation/ProposalServiceImpl.java:220`) : brut = quantité × prix ; remise = brut × `discountPercent` / 100 ; si `taxIncluded`, HT = net / (1 + `taxRate`/100) et TTC = net ; sinon taxe = net × `taxRate` / 100 et TTC = HT + taxe. Les totaux de la proposition sont les sommes des lignes, sans conversion de devise.

### Tableaux de bord

- `GET /sni/api/v1/crm/metrics` : compteurs globaux (tous temps) `totalLeads`, `newLeads`, `wonLeads`, `lostLeads`, `activeLeads`, `pipelineValue` (somme des `estimatedAmount` hors `WON` et `LOST`), `conversionRate` en pourcentage à une décimale.
- `GET /sni/api/v1/crm/analytics?from=...&to=...` : période par défaut les 30 derniers jours. `totalCreated`, `bySource` et `byStage` portent sur les leads **créés** dans la période ; `totalWon`, `totalLost` et `avgDaysToWin` sur les leads dont la **dernière modification** tombe dans la période (voir [Écarts](#écarts-relevés)). `pipelineValue` ignore la période. `bySource` et `byStage` sont des objets `{ "WEBSITE": 4, ... }` : une clé absente vaut 0, et les leads sans source ne sont pas comptés.

## Règles et erreurs à refléter dans l'écran

Les messages du module sont en anglais : prévoir un libellé français côté écran à partir du statut et du contexte.

| Situation | Statut | `message` |
| --- | --- | --- |
| Lead créé sans `fullName` | `400` `VALIDATION_FAILED` | détail dans `errors` (`fullName : must not be blank`) |
| Lead, opportunité ou proposition introuvable | `404` | `Lead not found: <id>`, `Opportunity not found: <id>`, `Proposal not found: <id>`, `Proposal line not found: <lineId>` |
| `PATCH .../stage` sans `stage` | `400` | `Lead stage is required` |
| `PATCH .../convert` sans `ownerType` ou `ownerCode` | `400` | `Converted owner type and code are required` |
| Activité sans `activityType` | `400` `VALIDATION_FAILED` | `activityType : must not be null` |
| Devis sans `currency` | `400` `VALIDATION_FAILED` | `currency : must not be blank` |
| Devis pour un lead `LOST` | `400` | `Cannot generate a quote for a lost lead` (masquer le bouton sur un lead perdu) |
| Opportunité sans `title` | `400` `VALIDATION_FAILED` | `title : must not be blank` |
| Changement d'étape d'opportunité sans `stage` | `400` | `Opportunity stage is required` |
| Envoi d'une proposition hors `DRAFT` | `400` | `Only DRAFT proposals can be sent` |
| Acceptation ou refus hors `SENT` / `VIEWED` | `400` | `Only SENT or VIEWED proposals can be accepted` / `... rejected` |
| Ligne sans `label` ou `unitPrice` | `400` `VALIDATION_FAILED` | message de validation sur le champ |
| Formulaire public envoyé trop souvent | `429` | plafond par adresse IP : 10 écritures publiques par 10 minutes par défaut (`RATE_LIMIT_PUBLIC_WRITE_MAX`, `RATE_LIMIT_PUBLIC_WRITE_WINDOW`) |

Règles à anticiper (non contrôlées par le serveur, donc à valider dans le formulaire) :

- `probability` : entier attendu entre 0 et 100, aucune borne côté serveur.
- `estimatedAmount`, `quantity`, `unitPrice`, `discountPercent` : aucune borne ; des valeurs négatives sont acceptées et faussent les totaux. Contraintes de stockage : `quantity` 2 décimales et moins de 100 000 000, `discountPercent` inférieur à 1 000.
- `taxRate` d'une ligne de proposition : la colonne est `NUMERIC(5,4)`, donc une valeur supérieure ou égale à 10 (par exemple `18` pour 18 %) fait échouer l'enregistrement (erreur de base de données, statut non vérifié, probablement `500`), alors que le calcul l'interprète comme un pourcentage. Voir [Écarts](#écarts-relevés).
- `opportunityId` ou `leadId` inexistant à la création d'une proposition : pas de vérification préalable, l'enregistrement échoue sur la clé étrangère (statut non vérifié, probablement `500`). Ne proposer que des valeurs choisies dans une liste.
- Lignes et en-tête d'une proposition restent modifiables quel que soit son statut, y compris `ACCEPTED` ou `REJECTED` : verrouiller l'édition dans l'écran hors `DRAFT`.

## À prévoir côté frontend

Écrans à construire :

1. Kanban des leads, avec filtres (agent, source, intérêt, texte) et indicateurs de colonne.
2. Liste paginée des leads.
3. Fiche lead : coordonnées, étape, score, historique d'activités, formulaire d'activité, opportunités, propositions liées, actions Qualifier, Changer d'étape, Marquer perdu, Convertir, Générer un devis.
4. Assistant de conversion en deux temps (création ou choix d'un client ou membre dans `client`, puis `/convert`).
5. Liste des opportunités et changement d'étape.
6. Liste et éditeur de propositions (en-tête, lignes, totaux, Envoyer, Accepter, Refuser).
7. Tableau de bord (`/metrics` et `/analytics` avec sélecteur de période).
8. Formulaire de contact du site public.

Droits par action :

| Action | Droit |
| --- | --- |
| Toute lecture (board, listes, fiches, pipeline, métriques, analytics, propositions) | `CRM:READ` |
| Créer ou modifier un lead, une activité, une opportunité, une proposition et ses lignes ; changer une étape ; qualifier ; générer un devis ; envoyer, accepter, refuser une proposition | `CRM:WRITE` |
| Convertir un lead | `CRM:CONVERT` |
| Choisir un agent (`GET /sni/api/v1/admin/users`) | `SYSTEM:USERS` |
| Ouvrir le devis généré | droits du module `billing` |

Pièges :

- Pas de route de suppression (lead, activité, opportunité, proposition) : ne pas afficher de bouton Supprimer, sauf pour une ligne de proposition.
- `assignedTo` est l'id numérique d'un compte. Un utilisateur sans `SYSTEM:USERS` ne peut pas charger la liste des agents : prévoir de masquer le sélecteur ou de n'afficher que l'id. Le filtre « mes leads » demande l'id numérique de l'utilisateur connecté, que `/auth/me` ne fournit pas (il renvoie `userId`, un code texte).
- `notes` des activités et `note` des leads sont stockés tels quels (seulement tronqués des espaces) : s'ils contiennent du HTML, l'assainir avant affichage.
- Le formulaire public accepte tous les champs de `CreateLeadRequest`, y compris `assignedTo`, `estimatedAmount` et `probability` : le site public ne doit envoyer que les champs de contact.
- Les cartes Kanban et `LeadResponse` n'ont pas la même forme : ne pas réutiliser le même composant sans adaptation.
- Pas de temps réel : recharger le Kanban après chaque action, ou périodiquement si plusieurs agents travaillent en même temps.

## Effets automatiques

| Déclencheur | Effet | Code |
| --- | --- | --- |
| Création d'un lead avec `assignedTo`, ou modification qui change l'agent | Courriel « Lead assigné · <leadNumber> » à l'agent (gabarit `email/crm-event`, envoi asynchrone par la file de courriels) | `CrmServiceImpl.java:76`, `:99` |
| Changement réel d'étape (`/stage`, `/qualify`) et toute conversion | Courriel « [CRM] Avancement · <leadNumber> → <étape> » à l'agent assigné | `CrmServiceImpl.java:214`, `:232` |
| Worker `LeadDormantAlertWorker`, du lundi au vendredi à 8 h (heure du serveur, `bokati.crm.dormant-cron`) | Pour chaque lead non `WON`/`LOST`, assigné, sans activité depuis 7 jours (`bokati.crm.dormant-threshold-days`) ou jamais actif, et pas encore alerté : courriel « [CRM] Lead dormant » à l'agent. Une seule alerte par lead jusqu'à la prochaine activité. | `src/main/java/com/sni/bokaticowork/features/crm/worker/LeadDormantAlertWorker.java:28` |
| Génération d'un devis | Création d'un devis dans `billing` et d'une activité `NOTE` sur le lead | `CrmServiceImpl.java:261` |

Aucun courriel n'est envoyé au prospect par ce module. Aucune notification en temps réel.

## Écarts relevés

1. `taxRate` d'une ligne de proposition stocké en `NUMERIC(5,4)` (`src/main/resources/db/migration/V181__crm_proposal_line_and_sequence.sql:12`, même valeur en `migration-prod`) alors que `ProposalServiceImpl.java:238` et `:244` le divisent par 100 : un taux réel (18 %) ne peut pas être enregistré, et un taux saisi en fraction (0,18) donne une taxe de 0,18 %.
2. La route publique `POST /sni/api/v1/public/crm/leads` réutilise `CreateLeadRequest` sans filtrage (`src/main/java/com/sni/bokaticowork/features/crm/controller/PublicCrmController.java:32`) : un anonyme peut fixer `assignedTo` (et déclencher un courriel vers n'importe quel compte), `estimatedAmount` et `probability`.
3. Statuts de proposition `VIEWED`, `EXPIRED`, `CONVERTED` jamais atteints, et champs `convertedInvoiceNumber`, `convertedContractCode`, `convertedSubscriptionNumber`, `convertedPassNumber`, `viewedAt`, `createdBy`, `documentCode` jamais remplis (`src/main/java/com/sni/bokaticowork/features/crm/model/CommercialProposal.java:108`, `:111` ; aucun appel de setter dans le module).
4. Aucune garde de statut sur `update`, `addLine`, `updateLine`, `removeLine` d'une proposition (`ProposalServiceImpl.java:74`, `:115`, `:142`, `:163`) : une proposition acceptée peut encore changer de montant.
5. Création d'une proposition avec un `opportunityId` ou `leadId` inexistant non vérifiée (`ProposalServiceImpl.java:58` à `:67`) : erreur de clé étrangère au lieu d'un `404`. Même chose pour `assignedTo` partout : aucune vérification que le compte existe.
6. Analytics : `countWonBetween`, `countLostBetween` et `avgDaysToWinBetween` filtrent sur `updated_at` (`src/main/java/com/sni/bokaticowork/features/crm/repository/LeadRepository.java:106`, `:109`, `:118`) : toute modification ultérieure d'un lead gagné le recompte dans une autre période et allonge sa « durée de gain ». `conversionRate` d'analytics divise des leads gagnés (par date de modification) par des leads créés (par date de création).
7. Conversion sans contrôle (`CrmServiceImpl.java:221`) : `ownerType` libre, existence du code non vérifiée, conversion possible depuis `LOST` ou d'un lead déjà converti (le courriel « Avancement » part même si l'étape était déjà `WON`).
8. Le paramètre `sort` est ignoré sur `GET /sni/api/v1/crm/leads` (`CrmServiceImpl.java:129`).
9. `GET /sni/api/v1/crm/pipeline` et `GET /sni/api/v1/crm/board` renvoient tous les leads sans pagination ; `/pipeline` charge en plus les activités de chaque lead une par une (`CrmServiceImpl.java:191`, `:420`).
10. `leadNumber` et `opportunityNumber` sont fondés sur l'horodatage en millisecondes (`src/main/java/com/sni/bokaticowork/features/crm/model/Lead.java:89`, `Opportunity.java:75`) et non sur le moteur de séquences : deux créations dans la même milliseconde entrent en conflit sur la contrainte d'unicité.
11. Messages d'erreur en anglais dans tout le module (`CrmServiceImpl.java`, `ProposalServiceImpl.java`).
12. `PUT /sni/api/v1/crm/leads/{id}`, `PATCH .../leads/{id}/stage`, `PATCH .../convert`, `PATCH .../opportunities/{id}/stage` et `PATCH .../proposals/{id}/reject` n'ont pas `@Valid` sur leur corps (`src/main/java/com/sni/bokaticowork/features/crm/controller/CrmController.java:65`, `:78`, `:85`, `:128` ; `ProposalController.java:96`) : les annotations `@NotNull` des modèles ne jouent pas, les contrôles sont faits dans le service (messages différents de `VALIDATION_FAILED`).
