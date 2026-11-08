# contract : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module gère les contrats passés entre l'espace (l'entité exploitante) et un client : membre, client (particulier ou organisation) ou entreprise. Il couvre la fiche du contrat et ses parties, la génération du PDF depuis un gabarit, la relecture, la signature électronique par lien envoyé au signataire, les avenants, l'historique des versions, et les transitions automatiques (activation, expiration, alertes d'échéance).

Toutes les routes de ce module sont du **back-office**, sauf les deux routes publiques de signature par lien. Le membre consulte et signe ses propres contrats depuis le portail (`/sni/api/v1/client/documents/contracts/**`, module [portal](../portal/README.md)).

Modules voisins : `subscription` (crée, signe, prolonge et clôt seul les contrats d'abonnement, de pass et d'add-on), `document` (stocke chaque PDF produit, téléchargé par `GET /sni/api/v1/documents/{code}/download`), `client` et `company` (propriétaires et parties), `payment` (un remboursement clôt le contrat lié), `notification` (courriels et notifications du portail via la file d'événements).

## Sommaire

- [Notions](#notions)
- [Statuts et cycle de vie](#statuts-et-cycle-de-vie)
- [Parcours](#parcours)
- [Règles et erreurs à refléter dans l'écran](#règles-et-erreurs-à-refléter-dans-lécran)
- [À prévoir côté frontend](#à-prévoir-côté-frontend)
- [Effets automatiques](#effets-automatiques)
- [Écarts relevés](#écarts-relevés)

## Notions

| Objet | Identifiant exposé | Ce que c'est | Liens |
| --- | --- | --- | --- |
| Contrat (`Contract`) | `contractCode`, lisible, séquence `CTR-AAAA-NNNNNN` | La fiche contractuelle : titre, gabarit, propriétaire, dates, statut, PDF brouillon et PDF signé. | Propriétaire (`ownerType` + `ownerCode`), entité exploitante (`businessCode`), parties, avenants, journal. |
| Partie (`ContractParty`) | aucun (liste dans `parties` du contrat) | Une personne ou organisation citée au contrat, avec son rôle (`BENEFICIARY`, `OPERATOR`, `SIGNATORY`, `BILLING_CONTACT`), son ordre de signature et `mustSign`. | Remplacée en bloc à chaque envoi de `parties`. |
| Gabarit « figé » | code en minuscules (`membership-agreement`, `business-service-agreement`, `contrat-domiciliation`, `subscription-pass-non-refundable`) | Fichier Thymeleaf `src/main/resources/templates/contracts/<code>.html`. Les quatre codes sont aussi inscrits en base (migration `V163`) sans contenu HTML : le rendu se fait par le fichier. | Utilisé par `POST /contracts/generate`, `/preview`, `/{contractCode}/generate-draft`. |
| Modèle de contrat (`ContractTemplate`) | `code`, séquence `CTM-NNNN` | Gabarit éditable en base : HTML, CSS, en-tête, pied, sections, définitions de variables. | Sert de point de départ aux brouillons libres. Voir l'écart 2 : un modèle `CTM-…` ne passe pas par la génération de contrat. |
| Brouillon libre (`ContractDraft`) | `code`, séquence `CTD-AAAA-NNNNNN` | Document de rédaction section par section, avec variables `{{cle}}`. Produit un PDF, mais **ne crée ni ne met à jour aucun contrat**. | Optionnellement rattaché à un modèle (`templateCode`) et à un propriétaire. |
| Avenant (`ContractAmendment`) | `code`, séquence `AVT-AAAA-NNNNNN` | Modification d'un contrat signé : sections ajoutées, modifiées, supprimées, et changements de valeurs (`variableChanges`). | `originalContractCode`. Sections d'avenant identifiées par `id` numérique. |
| Lien de signature (`ContractSigningToken`) | `token` (UUID) | Lien à usage unique, valable 72 h (`app.contract.signing-token-hours`), pour un signataire nommé. | Un seul lien ouvert par contrat : une nouvelle demande révoque le précédent. |
| Journal du contrat (`ContractAuditEvent`) | `id` numérique (int64) | Événements chaînés par empreinte SHA-256 (`previousHash`, `eventHash`). Alimenté par les avenants, les prolongations et les réouvertures. | `GET /contracts/{contractCode}/audit-events`. |
| Version (`ContractVersionHistory.Version`) | `version` (1, 2, 3…) | Vue calculée : l'original, puis chaque avenant `ACTIVE` et chaque prolongation de date, triés par date. La dernière est en vigueur. | `GET /contracts/{contractCode}/versions`. Rien n'est stocké : relu sur les avenants et le journal. |

Propriétaire et parties : `ownerType` / `partyType` acceptent seulement `MEMBER` (code membre), `CUSTOMER` (code client) et `BUSINESS` (code entreprise). Les autres valeurs de l'énumération `DocumentOwnerType` sont refusées en `400`.

Identifiants d'acteur : plusieurs routes exigent un entier `createdBy`, `uploadedBy`, `proposedBy`, `reviewedBy` ou `actorId`. Le serveur l'enregistre tel quel, sans le comparer au compte connecté. Voir [À prévoir](#à-prévoir-côté-frontend) et l'écart 6.

## Statuts et cycle de vie

### Contrat (`ContractStatus`)

Les transitions autorisées sont codées dans `src/main/java/com/sni/bokaticowork/features/contract/service/implementation/ContractServiceImpl.java:387` (`validateTransition`). Demander le statut courant est toujours accepté (sans effet).

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Fiche créée, pas de PDF. | `POST /sni/api/v1/contracts`, ou le renouvellement automatique (voir l'écart 1). | `GENERATED` (en pratique `UNDER_REVIEW` ou `AWAITING_SIGNATURE`, voir ligne suivante), `UNDER_REVIEW`, `CANCELLED` |
| `GENERATED` | PDF brouillon produit. Statut transitoire : la génération ne s'y arrête pas. On y revient par un rejet de relecture. | `POST /contracts/{contractCode}/reject-review` (depuis `UNDER_REVIEW`), `PATCH .../status` | `UNDER_REVIEW`, `AWAITING_SIGNATURE`, `SIGNED`, `CANCELLED` |
| `UNDER_REVIEW` | En relecture interne. | `POST .../generate-draft` quand aucune partie n'est signataire (aucune partie `SIGNATORY` ni `mustSign: true`), ou `PATCH .../status` | `AWAITING_SIGNATURE` (`/approve`), `GENERATED` (`/reject-review`), `SIGNED`, `CANCELLED` |
| `AWAITING_SIGNATURE` | Prêt à signer. Seul statut où l'on peut envoyer un lien de signature. | `POST .../generate-draft` quand au moins une partie est signataire, ou `POST .../approve` | `SIGNED` (signature), `GENERATED` (nouvelle génération du PDF), `CANCELLED` |
| `SIGNED` | Signé, date d'effet future. | Signature (lien public, portail, `PATCH .../signed-document`) quand `effectiveDate` (à défaut `startDate`) est dans le futur | `ACTIVE` (worker ou `/activate`), `CANCELLED` |
| `ACTIVE` | En vigueur. | Signature quand la date d'effet est atteinte ou absente (activation immédiate), worker horaire, `/activate`, reprise d'un `SUSPENDED`, prolongation d'un `EXPIRED` | `SUSPENDED`, `TERMINATED`, `EXPIRED`, `AMENDED` |
| `SUSPENDED` | Suspendu, motif conservé côté serveur (non renvoyé, voir l'écart 5). | `PATCH .../suspend` | `ACTIVE`, `TERMINATED`, `CANCELLED`, `AMENDED` |
| `AMENDED` | En vigueur, modifié par au moins un avenant appliqué. Se comporte comme `ACTIVE` pour les avenants et l'expiration. | Signature d'un avenant (`POST /amendments/{code}/sign`), avenant de renouvellement d'abonnement | `TERMINATED`, `EXPIRED`. Ni `ACTIVE`, ni `SUSPENDED`, ni `CANCELLED`. |
| `EXPIRED` | Date de fin dépassée. Final pour les routes. | Worker horaire, `PATCH .../status` depuis `ACTIVE` ou `AMENDED` | Aucune route. Le module `subscription` peut le rouvrir en `ACTIVE` (prolongation de date ou réouverture), ou en `AMENDED` (avenant de renouvellement). |
| `TERMINATED` | Résilié après entrée en vigueur. Final. | `PATCH .../terminate`, clôture d'abonnement, remboursement | Aucune |
| `CANCELLED` | Annulé avant ou pendant la vie du contrat. Final. | `PATCH .../cancel`, clôture d'abonnement ou remboursement d'un contrat non actif | Aucune |

Effets de bord des transitions (`ContractServiceImpl.java:314`) :

- vers `ACTIVE` : `activatedAt` = maintenant, motif de suspension effacé ;
- vers `TERMINATED` ou `CANCELLED` : `terminatedAt` = maintenant, `terminationReason` = motif ;
- vers `SUSPENDED` : motif de suspension enregistré ;
- vers tout autre statut : `terminatedAt` et `terminationReason` remis à vide.

### Avenant (`AmendmentStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | En rédaction : sections et variables modifiables. | `POST /sni/api/v1/contracts/{contractCode}/amendments` | `UNDER_REVIEW` (`/submit`), `CANCELLED` |
| `UNDER_REVIEW` | Soumis pour examen, figé. | `POST /amendments/{code}/submit` (au moins une section ou une variable) | `PENDING_SIGNATURE` (`/approve`), `REJECTED` (`/reject`), `CANCELLED` |
| `PENDING_SIGNATURE` | Approuvé, attend la signature. | `POST /amendments/{code}/approve` | `ACTIVE` (`/sign`), `CANCELLED` |
| `ACTIVE` | Signé et appliqué : le contrat passe en `AMENDED`. Final. | `POST /amendments/{code}/sign`, ou automatiquement pour un renouvellement d'abonnement payé | Aucune |
| `REJECTED` | Refusé à l'examen. Final : proposer un nouvel avenant. | `POST /amendments/{code}/reject` | Aucune |
| `CANCELLED` | Abandonné. Final. | `POST /amendments/{code}/cancel`, ou remplacé par l'avenant d'un renouvellement payé (seulement les anciens avenants de renouvellement restés ouverts) | Aucune |

Un seul avenant ouvert (`DRAFT`, `UNDER_REVIEW` ou `PENDING_SIGNATURE`) à la fois par contrat.

### Brouillon libre (`ContractDraftStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFTING` | Valeur à la création. | `POST /contract-drafts`, `POST /contract-drafts/from-template/{templateCode}` | N'importe quel statut par `PATCH .../status` |
| `READY` | Marqueur libre « prêt ». Aucun effet côté serveur. | `PATCH /contract-drafts/{code}/status?status=READY` | Idem |
| `GENERATED` | Un PDF a été produit. | `POST /contract-drafts/{code}/generate`, ou `PATCH .../status` | Idem |

Le statut d'un brouillon n'empêche rien : un brouillon `GENERATED` reste modifiable et régénérable.

### Lien de signature

Pas de champ statut : l'état se lit sur `revoked`, `signedAt` et `expiresAt` de `ContractSigningResponse`. Un lien est utilisable s'il n'est ni révoqué, ni signé, ni expiré.

## Parcours

### 1. Liste des contrats

1. `GET /sni/api/v1/contracts` avec les filtres facultatifs `ownerType`, `ownerCode`, `status` (texte, casse indifférente), `businessCode`, `templateCode`, et `page`, `size`, `sort` ([détail](routes/ContractController.md#r-get-sni-api-v1-contracts)).
2. Réponse paginée `PaginatedResponse<ContractResponse>`. Afficher `contractCode`, `title`, `status`, `ownerType`/`ownerCode`, `startDate`/`endDate`, `renewalType`.
3. Les contrats supprimés n'apparaissent jamais. Un statut inconnu renvoie `400` « Statut de contrat invalide: X ».

Pour la fiche d'un client, filtrer par `ownerType` + `ownerCode`.

### 2. Créer un contrat puis produire son PDF

1. Charger les listes de référence : `GET /sni/api/v1/contracts/templates-drafting` renvoie en un appel `templates`, `sectionTypes`, `renewalTypes`, `partyRoles`, `statuses`, `languages` (`fr`, `en`), `variableTypes` et `variableSources` ([détail](routes/ContractController.md#r-get-sni-api-v1-contracts-templates-drafting)). `GET /sni/api/v1/contracts/templates` renvoie seulement les gabarits.
2. `POST /sni/api/v1/contracts` ([détail](routes/ContractController.md#r-post-sni-api-v1-contracts)) avec `title`, `templateCode`, `ownerType`, `ownerCode`, `createdBy` (obligatoires), et au besoin `businessCode`, `renewalType` (défaut `NONE`), `effectiveDate`, `startDate`, `endDate`, `parties`. Réponse `201` avec le contrat en `DRAFT` et son `contractCode`.
3. Facultatif, aperçu : `POST /sni/api/v1/contracts/preview` avec un `GenerateContractRequest` (mêmes champs que la génération, voir parcours 3). Réponse `{ templateCode, html }` : afficher `html` dans un cadre isolé (`iframe` avec `srcdoc`).
4. `POST /sni/api/v1/contracts/{contractCode}/generate-draft` avec `{ "uploadedBy": <id> }` ([détail](routes/ContractController.md#r-post-sni-api-v1-contracts-contractcode-generate-draft)). Le serveur reprend le gabarit, le propriétaire, l'entité, le titre et les dates de la fiche, produit le PDF, le range dans les documents (`CONTRACT_DRAFT`) et **envoie un courriel « Votre contrat est prêt » avec le PDF au propriétaire**. Réponse : le contrat, avec `draftDocumentCode` rempli et le statut `AWAITING_SIGNATURE` (au moins une partie signataire) ou `UNDER_REVIEW` (aucune).
5. Proposer le téléchargement du PDF : `GET /sni/api/v1/documents/{draftDocumentCode}/download` (droit `DOCUMENT:READ`, module `document`).

La génération par la fiche n'utilise ni les clauses, ni les variables libres, ni le nom du signataire : pour les fournir, passer par la génération directe (parcours 3) puis rattacher le document.

### 3. Génération directe (sans fiche)

1. `POST /sni/api/v1/contracts/generate` avec `templateCode`, `ownerType`, `ownerCode`, `title` (obligatoires), et `businessCode`, `effectiveDate`, `startDate`, `endDate`, `signatoryName`, `signatoryRole`, `clauses` (liste de textes), `variables` (objet clé/texte), `uploadedBy` ([détail](routes/ContractController.md#r-post-sni-api-v1-contracts-generate)).
2. Réponse `201` : un `DocumentResponse` (le PDF rangé dans les documents). **Aucun contrat n'est créé.** Le courriel « Votre contrat est prêt » part au propriétaire comme au parcours 2.
3. Variables injectées automatiquement (les `variables` envoyées les écrasent, sauf `placeOfSigning` et `signingDate`, toujours recalculées) : `clientName`, `clientEmail`, `clientPhone`, `clientCode`, `clientRccm`, `clientNiu`, `clientLegalForm`, `clientAddress`, `clientRepresentative`, `clientRepresentativeTitle`, `startDate`, `endDate`, `effectiveDate` (écrites en toutes lettres en français), `signatoryName`, `signatoryRole`, `contractTitle`, `operatorName`, `businessName`, `businessEmail`, `businessPhone`, `operatorRccm`, `operatorAddress`. Dans les clauses, `{{cle}}` est remplacé par la valeur ; une clé inconnue reste telle quelle.
4. Sans `businessCode`, l'entité retenue est celle marquée exploitante. S'il n'y en a pas : `400` « Aucune entite exploitante n'est designee… ».

### 4. Relecture interne

Pour un contrat en `UNDER_REVIEW` (ou `GENERATED`) :

1. Approuver : `POST /sni/api/v1/contracts/{contractCode}/approve?reviewedBy=<id>&comment=<texte>` ([détail](routes/ContractController.md#r-post-sni-api-v1-contracts-contractcode-approve)). Paramètres **de requête**, pas de corps. Le contrat passe en `AWAITING_SIGNATURE`.
2. Rejeter : `POST /sni/api/v1/contracts/{contractCode}/reject-review?reviewedBy=<id>&comment=<texte>`. Exige `UNDER_REVIEW`, sinon `400` « Le contrat doit être en statut UNDER_REVIEW pour être rejeté ». Le contrat revient en `GENERATED` : corriger la fiche (`PUT`), puis relancer `generate-draft`.

Le commentaire de relecture est enregistré mais n'est pas renvoyé dans `ContractResponse` (écart 5).

### 5. Signature électronique par lien (signataire externe)

Côté back-office :

1. Le contrat doit être `AWAITING_SIGNATURE`.
2. `POST /sni/api/v1/contracts/{contractCode}/request-signing` avec `{ "signerEmail": "...", "signerName": "..." }` ([détail](routes/ContractSigningController.md#r-post-sni-api-v1-contracts-contractcode-request-signing)).
   - Propriétaire particulier, membre ou entreprise (`BUSINESS`) : les deux champs sont obligatoires.
   - Propriétaire client de type organisation (`CUSTOMER` de type `COMPANY`) : les deux champs sont facultatifs. Vides, le serveur retient le premier signataire désigné (représentant légal, ou membre lié autorisé à signer) qui a une adresse. Renseignés, ils doivent désigner l'une de ces personnes, sinon `400` « X n'est pas désigné pour signer au nom de Y… ».
3. Réponse `201` `ContractSigningResponse` : `token`, `signerEmail`, `signerName`, `expiresAt` (72 h). Tout lien précédent encore ouvert est révoqué. Le serveur envoie un courriel « Signature requise · <titre> » au signataire et une notification au portail du membre concerné.

Côté page publique de signature (sans jeton de connexion) :

1. La page reçoit le `token` (UUID) dans son adresse et appelle `GET /sni/api/v1/public/contracts/sign/{token}` ([détail](routes/ContractSigningController.md#r-get-sni-api-v1-public-contracts-sign-token)). Afficher `contractTitle`, `contractCode`, `signerName`, `expiresAt`.
2. La réponse ne contient pas le PDF ni son code : la page publique ne peut pas afficher le contrat à signer (écart 4).
3. Case à cocher d'acceptation, puis `POST /sni/api/v1/public/contracts/sign/{token}` avec `{ "accepted": true, "consentText": "<texte affiché à côté de la case>" }`. Le serveur enregistre l'heure, l'adresse IP, le navigateur et le texte de consentement, puis signe le contrat : `ACTIVE` si la date d'effet est atteinte, sinon `SIGNED`. `signedDocumentCode` reçoit le code du PDF brouillon (pas de nouveau PDF).
4. Réponse : le lien avec `signedAt` rempli. Afficher une confirmation.

Erreurs de la page publique, à afficher telles quelles : `404` « Lien de signature introuvable », `400` « Ce lien de signature a été révoqué », « Ce contrat a déjà été signé », « Ce lien de signature a expiré », « Le contrat n'est plus en attente de signature », « Vous devez accepter les termes du contrat pour signer ».

### 6. Signature hors ligne (copie signée téléversée)

1. Téléverser la copie signée dans le module `document` (type `CONTRACT_SIGNED_COPY` conseillé) pour obtenir son `code`.
2. `PATCH /sni/api/v1/contracts/{contractCode}/signed-document` avec `{ "documentCode": "<code>" }` ([détail](routes/ContractController.md#r-patch-sni-api-v1-contracts-contractcode-signed-document)).
3. Accepté depuis `GENERATED`, `UNDER_REVIEW` ou `AWAITING_SIGNATURE` (et `SIGNED` qui reste `SIGNED`). Même règle d'activation qu'au parcours 5. Le code du document n'est pas vérifié par le serveur.

### 7. Modifier, suspendre, résilier, annuler

1. Modifier la fiche : `PUT /sni/api/v1/contracts/{contractCode}` avec seulement les champs à changer. Accepté seulement avant signature (`DRAFT`, `GENERATED`, `UNDER_REVIEW`, `AWAITING_SIGNATURE`). Envoyer `parties` remplace toute la liste ; ne pas l'envoyer la conserve. Après une modification, régénérer le PDF (`generate-draft`) : l'ancien PDF reste attaché sinon.
2. Activer à la main : `PATCH .../activate` (depuis `SIGNED` ou `SUSPENDED`).
3. Suspendre : `PATCH .../suspend?reason=<texte>` (depuis `ACTIVE`).
4. Résilier : `PATCH .../terminate?reason=<texte>` (depuis `ACTIVE`, `SUSPENDED`, `AMENDED`).
5. Annuler : `PATCH .../cancel?reason=<texte>` (depuis tout statut avant `ACTIVE`, et depuis `SUSPENDED`).
6. Changement générique : `PATCH .../status` avec `{ "status": "...", "reason": "..." }`, mêmes règles de transition. Préférer les routes dédiées.

Le motif passe en **paramètre de requête** pour `suspend`, `terminate` et `cancel`. Chaque action renvoie le contrat à jour.

Supprimer : `DELETE /sni/api/v1/contracts/{contractCode}` (`204`). Suppression logique sans contrôle de statut (écart 7) : réserver le bouton aux contrats `DRAFT` ou `CANCELLED`.

### 8. Avenant à un contrat en vigueur

Le contrat doit être `ACTIVE`, `SUSPENDED` ou `AMENDED`, sans autre avenant ouvert.

1. Proposer : `POST /sni/api/v1/contracts/{contractCode}/amendments?proposedBy=<id>` avec `{ "description": "...", "effectiveDate": "AAAA-MM-JJ" }` ([détail](routes/ContractAmendmentController.md#r-post-sni-api-v1-contracts-contractcode-amendments)). Réponse `201`, avenant `DRAFT` avec son `code`.
2. Rédiger les changements de clauses : `POST /sni/api/v1/amendments/{amendmentCode}/sections` avec `action` (`ADD`, `MODIFY`, `REMOVE`), `targetSectionRef` (titre ou référence de l'article visé, obligatoire pour `MODIFY` et `REMOVE`), `title`, `content` (HTML, obligatoire pour `ADD` et `MODIFY`), `sectionType`, `sectionOrder`. Modifier : `PUT .../sections/{sectionId}` (seuls les champs envoyés changent). Retirer : `DELETE .../sections/{sectionId}`.
3. Rédiger les changements de valeurs : `PUT /sni/api/v1/amendments/{amendmentCode}/variables` avec un **tableau** `[{ "variableKey": "...", "previousValue": "...", "newValue": "..." }]`. Le tableau remplace toutes les variables de l'avenant. Pour qu'un changement de date de fin apparaisse dans l'historique des versions, utiliser la clé `periode_fin` avec des dates `AAAA-MM-JJ`.
4. Aperçu : `GET /sni/api/v1/amendments/{amendmentCode}/preview` renvoie du **HTML brut** (`text/html`), pas du JSON. PDF : `POST .../generate-pdf?uploadedBy=<id>` renvoie un `DocumentResponse` et remplit `draftDocumentCode` de l'avenant. Ces deux routes marchent à tout statut.
5. Soumettre : `POST /amendments/{amendmentCode}/submit` (exige au moins une section ou une variable).
6. Examiner : `POST .../approve?reviewedBy=<id>` ou `POST .../reject?reviewedBy=<id>`, corps facultatif `{ "comment": "..." }`. Le commentaire devient `reviewComment` (approbation) ou `rejectionReason` (rejet).
7. Signer et appliquer : `POST .../sign?actorId=<id>`, corps facultatif `{ "signedDocumentCode": "...", "justification": "..." }`. L'avenant devient `ACTIVE`, le contrat passe en `AMENDED`.
8. Abandonner à tout moment avant application : `POST .../cancel?actorId=<id>`, corps facultatif `{ "reason": "..." }`.

Signer un avenant **ne modifie aucun champ du contrat** (dates, titre, gabarit) : seul son statut change. Les valeurs de `variableChanges` servent au document et à l'historique, pas à la fiche.

### 9. Historique : versions, avenants, journal

1. Versions : `GET /sni/api/v1/contracts/{contractCode}/versions` ([détail](routes/ContractAmendmentController.md#r-get-sni-api-v1-contracts-contractcode-versions)). Liste ordonnée, la dernière est en vigueur. Pour chaque version : `version`, `kind` (`ORIGINAL`, `AMENDMENT`, `EXTENSION`), `amendmentCode` (avenant seulement), `description`, `effectiveDate`, `previousEndDate` et `endDate` (vides si la version n'a pas changé la fin), `at`, `documentCode` (PDF signé sinon brouillon ; vide pour une prolongation). Afficher une frise : « v1 Original », « v2 Avenant AVT-… », « v3 Prolongation jusqu'au … ».
2. Avenants : `GET /sni/api/v1/contracts/{contractCode}/amendments`, tous statuts, du plus récent au plus ancien, avec sections et variables.
3. Journal : `GET /sni/api/v1/contracts/{contractCode}/audit-events`, du plus ancien au plus récent. `eventType` vaut par exemple `AMENDMENT_PROPOSED`, `AMENDMENT_SUBMITTED_FOR_REVIEW`, `AMENDMENT_REVIEW_APPROVED`, `AMENDMENT_REVIEW_REJECTED`, `AMENDMENT_SIGNED`, `CONTRACT_AMENDED`, `AMENDMENT_CANCELLED`, `AMENDMENT_RENEWAL_APPLIED`, `CONTRACT_TERM_EXTENDED`, `CONTRACT_REOPENED`. `payload` est un texte JSON à décoder.

Le journal ne trace ni la création, ni la génération, ni la signature, ni la suspension, ni la résiliation du contrat lui-même : ces actions passent par l'audit général (`@Audited`, module `socle`).

Ces trois routes renvoient une liste vide (et non `404`) pour un code inconnu, sauf `versions` qui renvoie `404` « Contrat introuvable ».

### 10. Contrats issus d'un abonnement (lecture seule en pratique)

Le module `subscription` crée seul un contrat pour chaque abonnement, pass et add-on (gabarit `subscription-pass-non-refundable`). Le contrat naît, est généré et signé par le système dans la même opération : il apparaît directement en `ACTIVE` (ou `SIGNED` si l'abonnement démarre plus tard). Ensuite :

- **Renouvellement payé** : un avenant `ACTIVE` est créé et appliqué sans examen ni signature (description « Renouvellement de l'abonnement <numéro> · nouvelle période du … au … », variables `periode_debut`, `periode_fin`, `prochaine_facturation`). Le contrat prend la nouvelle `endDate` et passe en `AMENDED`. Le PDF de l'avenant est généré dans la foulée ; s'il échoue, le relancer par `POST /amendments/{code}/generate-pdf`.
- **Alignement de période** : la date de fin est prolongée sans avenant (événement `CONTRACT_TERM_EXTENDED`, version `EXTENSION`). Un contrat `EXPIRED` revient alors en `ACTIVE`.
- **Contrat pas encore signé** au moment d'un renouvellement : seule la date de fin est prolongée.
- **Clôture de l'abonnement** : le contrat est résilié (`ACTIVE`, `SUSPENDED`, `AMENDED`) ou annulé (autres statuts non finaux).
- Tant que l'abonnement est ouvert, le contrat n'expire pas automatiquement.

L'écran contrat doit donc rafraîchir la fiche plutôt que supposer un statut, et ne pas proposer d'avenant manuel concurrent quand un renouvellement est attendu.

### 11. Modèles de contrat (rédaction)

1. Liste : `GET /sni/api/v1/contract-templates` (paginé, 20 par défaut, plus récents d'abord), recherche `GET .../search?query=` (nom, description, catégorie), par catégorie `GET .../category/{category}`.
2. Créer : `POST /sni/api/v1/contract-templates` avec `name` (obligatoire), `description`, `language` (défaut `fr`), `category`, `htmlContent`, `cssContent`, `headerHtml`, `footerHtml`, `variableDefinitions` (liste de `{ key, label, type, required, defaultValue, source, description }`, types et sources listés par `templates-drafting`). Réponse `201` avec `code` `CTM-NNNN`.
3. Sections : `POST .../{code}/sections`, `PUT .../{code}/sections/{sectionId}`, `DELETE .../{code}/sections/{sectionId}`, `GET .../{code}/sections`. Le corps exige toujours `content` et `sectionType`, **même pour une modification**.
4. Aperçu : `POST .../{code}/preview` avec un objet `{ "cle": "valeur" }` d'exemple (facultatif). Les sections actives remplacent `htmlContent` quand il y en a. Réponse `{ templateCode, html }`.
5. Dupliquer : `POST .../{code}/duplicate` (nom suffixé « (copie) », sections copiées). Désactiver : `PUT .../{code}` avec `"active": false`. Supprimer : `DELETE .../{code}` (définitif).

### 12. Brouillons libres

1. Créer vide : `POST /sni/api/v1/contract-drafts` avec `title`, `createdBy` (obligatoires), `ownerType`, `ownerCode`, `businessCode`, `sections`. Ou depuis un modèle : `POST /sni/api/v1/contract-drafts/from-template/{templateCode}` avec le même corps : les sections actives du modèle sont copiées (ou son `htmlContent` en une section), puis les sections envoyées sont ajoutées à la suite.
2. Liste : `GET /sni/api/v1/contract-drafts`, filtres exclusifs : `status` l'emporte, sinon `ownerType` **et** `ownerCode` ensemble, sinon tout. Recherche : `GET .../search?query=`.
3. Éditer : `PUT .../{code}` (envoyer `sections` remplace toutes les sections), ou section par section (`POST`, `PUT`, `DELETE` sur `.../sections`).
4. Aperçu : `POST .../{code}/preview` avec `{ "variables": { ... } }` facultatif. Variables automatiques : `contractTitle`, `generatedDate`, `clientName`, `clientEmail`, `clientPhone`, `clientCode`, `clientRccm` (entreprise seulement), `businessName`, `businessEmail`, `businessPhone`, `operatorName`, `operatorRccm`.
5. Générer : `POST .../{code}/generate?uploadedBy=<id>` avec le même corps facultatif. Réponse `201` `DocumentResponse` ; le brouillon passe en `GENERATED`. Aucun contrat n'est créé ni mis à jour : pour en faire un contrat signé, créer la fiche (parcours 2) puis rattacher ce PDF par `PATCH /contracts/{contractCode}/signed-document` une fois signé.

## Règles et erreurs à refléter dans l'écran

Toutes ces erreurs sont des `400` (`BAD_REQUEST`) sauf mention contraire. Les messages sont ceux renvoyés dans `message`.

Contrat :

| Règle | Message |
| --- | --- |
| `endDate` ≥ `startDate` | « La date de fin doit etre posterieure ou egale a la date de debut » |
| `effectiveDate` ≤ `startDate` | « La date d'effet ne peut pas etre apres la date de debut » |
| Si `parties` est envoyé non vide : au moins une partie `SIGNATORY` ou `mustSign: true` | « Le contrat doit contenir au moins un signataire » |
| Chaque partie a un `displayName` | « Le nom d'affichage d'une partie est requis » |
| Propriétaire ou partie de type `MEMBER`, `CUSTOMER` ou `BUSINESS` existant | « Type de proprietaire non supporte pour un contrat: X », ou l'erreur `404` du module client ou company |
| Modification après signature | « Ce contrat a été signé et ne peut plus être modifié en place. Toute modification doit passer par un avenant. » |
| Modification d'un contrat clos (`TERMINATED`, `CANCELLED`, `EXPIRED`) | « Ce contrat est clôturé et ne peut plus être modifié. » |
| Transition interdite | « Transition de statut invalide: ACTIVE -> CANCELLED » (par exemple) |
| Statut inconnu dans `status` | « Statut de contrat invalide: X » |
| Code inconnu | `404` « Contrat introuvable » |
| Gabarit inconnu (génération, aperçu) | « Unknown contract template: X » (en anglais) |
| Pas d'entité exploitante désignée | « Aucune entite exploitante n'est designee. Creez la fiche de l'espace puis designez-la comme exploitante… » |
| Échec du rendu PDF | « Unable to generate contract PDF: … » |
| Demande de signature hors `AWAITING_SIGNATURE` | « Le contrat doit être en attente de signature » |
| Signataire manquant (particulier, membre, entreprise) | « Le nom et l'adresse du signataire sont requis » |
| Organisation sans signataire désigné ayant une adresse | « Aucun signataire désigné avec une adresse pour X : renseignez le représentant légal ou autorisez un membre à signer. » |

Le statut proposé dans l'écran doit suivre la table de transitions ([Statuts](#contrat-contractstatus)) : n'afficher que les actions permises par le statut courant.

Avenant :

| Règle | Message |
| --- | --- |
| Contrat `ACTIVE`, `SUSPENDED` ou `AMENDED` | « Un avenant ne peut être proposé que sur un contrat ACTIVE, SUSPENDED ou AMENDED. Statut actuel : X » |
| Un seul avenant ouvert | « Un avenant est déjà en cours de traitement pour ce contrat (statut : X) » |
| `description` obligatoire | `400` `VALIDATION_FAILED`, « La description de l'avenant est requise » |
| Sections et variables modifiables seulement en `DRAFT` | « Impossible de modifier les sections : l'avenant n'est plus en DRAFT (statut : X) » |
| `targetSectionRef` pour `MODIFY` / `REMOVE`, `content` pour `ADD` / `MODIFY` | « targetSectionRef est requis pour l'action REMOVE », « content est requis pour les actions ADD et MODIFY » |
| Au moins un changement avant soumission | « L'avenant doit contenir au moins une modification de section ou de variable avant soumission. » |
| Mauvais statut pour l'étape | « Transition invalide : l'avenant est en statut X, statut attendu : Y » |
| Annulation d'un avenant `ACTIVE`, `REJECTED` ou `CANCELLED` | « Cet avenant ne peut plus être annulé (statut : X) » |
| Code inconnu | `404` « Avenant introuvable : X », « Section introuvable pour cet avenant » |

Modèles et brouillons : `404` « Template de contrat non trouvé: X », « Brouillon de contrat non trouvé: X », « Section non trouvée: X » ; `400` « Ce template n'a pas de contenu défini (ni sections ni html_content) » à l'aperçu d'un modèle vide ; « Impossible de générer le PDF du brouillon: … ».

## À prévoir côté frontend

Écrans back-office :

| Écran | Routes principales | Droit exigé |
| --- | --- | --- |
| Liste et fiche des contrats | `GET /contracts`, `GET /contracts/{contractCode}`, `/versions`, `/amendments`, `/audit-events` | `BILLING:READ` |
| Création et génération | `POST /contracts`, `/preview`, `/generate`, `/{contractCode}/generate-draft`, `/approve`, `/reject-review`, `/request-signing` | `BILLING:CREATE` |
| Modification et cycle de vie | `PUT /contracts/{contractCode}`, `PATCH .../activate`, `/suspend`, `/terminate`, `/cancel`, `/status`, `/signed-document`, `DELETE` | `BILLING:UPDATE` |
| Proposition d'avenant | `POST /contracts/{contractCode}/amendments` | `BILLING:CREATE` |
| Rédaction, examen, signature d'avenant | toutes les routes `/sni/api/v1/amendments/**` | `ADMIN:ACCESS` |
| Modèles de contrat | `/sni/api/v1/contract-templates/**` | `ADMIN:ACCESS` |
| Brouillons libres | `/sni/api/v1/contract-drafts/**` | `ADMIN:ACCESS` |

Page publique : une page sans connexion qui lit le `token` dans son adresse et appelle les deux routes `/sni/api/v1/public/contracts/sign/{token}`. Attention à l'adresse envoyée par courriel (écart 3).

Masquer les boutons selon le statut :

- « Modifier » : `DRAFT`, `GENERATED`, `UNDER_REVIEW`, `AWAITING_SIGNATURE`.
- « Générer le PDF » : `DRAFT`, `UNDER_REVIEW`, `AWAITING_SIGNATURE` (régénération), `GENERATED`.
- « Approuver » : `GENERATED`, `UNDER_REVIEW`. « Renvoyer en correction » : `UNDER_REVIEW`.
- « Envoyer pour signature » : `AWAITING_SIGNATURE` seulement.
- « Joindre la copie signée » : `GENERATED`, `UNDER_REVIEW`, `AWAITING_SIGNATURE`.
- « Activer » : `SIGNED`, `SUSPENDED`. « Suspendre » : `ACTIVE`. « Résilier » : `ACTIVE`, `SUSPENDED`, `AMENDED`. « Annuler » : `DRAFT`, `GENERATED`, `UNDER_REVIEW`, `AWAITING_SIGNATURE`, `SIGNED`, `SUSPENDED`.
- « Proposer un avenant » : `ACTIVE`, `SUSPENDED`, `AMENDED`, et aucun avenant en `DRAFT`, `UNDER_REVIEW` ou `PENDING_SIGNATURE` dans la liste des avenants.
- Aucune action sur `EXPIRED`, `TERMINATED`, `CANCELLED`.

Pièges propres au module :

- **Identifiants d'acteur.** `createdBy`, `uploadedBy` (corps), `proposedBy`, `reviewedBy`, `actorId`, `uploadedBy` (requête) sont des entiers obligatoires selon la route. `GET /sni/api/v1/auth/me` renvoie `userId` en **texte**, pas l'identifiant numérique du compte. Non vérifié : aucune route de ce module ne fournit cet identifiant numérique ; le serveur ne le contrôle pas. Décider d'une valeur avec l'équipe backend avant de coder ces formulaires (écart 6).
- **Paramètres de requête et non de corps** : `reviewedBy`, `comment` (relecture de contrat), `reason` (suspend, terminate, cancel), `proposedBy`, `actorId`, `reviewedBy`, `uploadedBy` (avenants), `status` (brouillon).
- **`PUT /amendments/{code}/variables`** attend un tableau JSON brut, pas un objet.
- **`GET /amendments/{code}/preview`** renvoie du HTML (`text/html`), à lire comme texte. Les autres aperçus renvoient du JSON `{ templateCode, html }`.
- Afficher tout HTML d'aperçu dans une `iframe` isolée (`sandbox`, `srcdoc`) : le contenu des sections est inséré sans échappement.
- **Codes de gabarit** : le serveur met `templateCode` en minuscules pour les contrats. Utiliser les codes renvoyés par `GET /contracts/templates`. Un modèle `CTM-…` créé en base n'est pas utilisable pour un contrat (écart 2).
- `GET /contracts/templates` renvoie les modèles actifs en base s'il en existe, sinon les quatre gabarits figés. Les champs de contenu (`htmlContent`, `sections`…) sont vides dans cette liste.
- **Génération = courriel au client.** `POST /contracts/generate` et `generate-draft` envoient le PDF au propriétaire à chaque appel. Pour un essai, utiliser `POST /contracts/preview`. Le courriel part même si `generate-draft` échoue ensuite sur une transition interdite (écart 8).
- Le PDF se télécharge par le module `document` : `draftDocumentCode` ou `signedDocumentCode` de la fiche, `documentCode` d'une version, `draftDocumentCode`/`signedDocumentCode` d'un avenant.
- `ContractResponse` ne contient ni le motif de suspension, ni le commentaire de relecture, ni le contrat d'origine d'un renouvellement (écart 5) : ne pas prévoir de champ d'affichage pour eux.
- `createdBy`, `reviewedBy`, `proposedBy`, `actorId`, `id` (sections, journal) sont des int64 : appliquer la règle des grands nombres du [README racine](../README.md#identifiants--attention-aux-grands-nombres). Les `sectionId` des avenants, modèles et brouillons partent dans le chemin : les garder en texte.
- `Idempotency-Key` conseillé sur la création, la modification, les changements de statut, la génération et `signed-document` des contrats, et sur la création, modification et génération des brouillons et modèles. Les routes d'avenant et de signature n'en ont pas : désactiver le bouton pendant l'appel.
- Temps réel : aucun sujet WebSocket propre aux contrats. Rafraîchir la fiche après une action, et à l'ouverture (le worker et l'abonnement changent le statut sans action de l'utilisateur).

## Effets automatiques

Worker `ContractLifecycleWorker` (`src/main/java/com/sni/bokaticowork/features/contract/worker/ContractLifecycleWorker.java`) : toutes les heures après la fin du passage précédent (`CONTRACT_WORKER_FIXED_DELAY_MS`, défaut `3600000`), désactivable par `CONTRACT_WORKER_ENABLED`. Chaque passage, dans cet ordre (`ContractLifecycleAutomationServiceImpl.java:38`) :

1. **Activation** : chaque contrat `SIGNED` dont `effectiveDate` (à défaut `startDate`) est atteinte ou absente passe en `ACTIVE`. Événement `CONTRACT_AUTO_ACTIVATED`, courriel « Votre contrat est actif ».
2. **Expiration** : chaque contrat `ACTIVE`, `SUSPENDED` ou `AMENDED` dont `endDate` est passée (strictement avant aujourd'hui) passe en `EXPIRED`, sauf s'il porte un abonnement encore ouvert. Événement `CONTRACT_AUTO_EXPIRED`, courriel « Votre contrat a expiré ».
3. **Reconduction** (`renewalType: AUTO_RENEW`) : prévue pour créer un nouveau contrat `DRAFT` (`renewedFromCode` = l'ancien) de même durée, événement `CONTRACT_AUTO_RENEWED`. Le worker reconduit avant d'expirer : le successeur est créé, puis l'ancien contrat expire dans le même passage (corrigé le 2026-11-08, écart 1).
4. **Alerte d'échéance** : à J-30 et J-7 exactement, pour chaque contrat `ACTIVE` (pas `AMENDED`, pas `AUTO_RENEW`), une seule fois par seuil. Événement `CONTRACT_EXPIRY_ALERT`, courriel « Votre contrat expire bientôt ». Prolonger la date de fin réarme l'alerte.

Courriels (envoyés par la file d'événements, traitée toutes les 10 s, au propriétaire du contrat) :

| Déclencheur | Destinataire | Objet |
| --- | --- | --- |
| `POST /contracts/generate`, `generate-draft`, contrat d'abonnement | Propriétaire, PDF joint | « Votre contrat est prêt · Elle A Osé » (envoi direct, après validation de la transaction) |
| `request-signing` | Signataire | « Signature requise · <titre> », avec le lien et l'échéance |
| Activation, expiration, reconduction, alerte (worker) | Propriétaire | voir ci-dessus |

Notifications du portail (membre) : `CONTRACT_SIGNING_REQUESTED` (« Nouveau contrat a signer ») et `CONTRACT_SIGNED_VIA_ESIGN` (« Contrat signe ») sont créées sur le canal `IN_APP`, destinataire de type membre, avec le `ownerCode` du contrat et l'adresse du signataire (`src/main/java/com/sni/bokaticowork/features/portal/notification/service/MemberInAppNotifier.java:21`). Non vérifié : leur affichage quand le propriétaire n'est pas un membre.

Venant d'autres modules :

- `subscription` : création et signature système des contrats d'abonnement, de pass et d'add-on ; avenant de renouvellement (événement `CONTRACT_RENEWAL_AMENDMENT_REQUESTED`, traité par `src/main/java/com/sni/bokaticowork/core/outbox/service/implementation/ContractRenewalAmendmentOutboxEventProcessor.java`) ; prolongation de date et réouverture ; résiliation ou annulation à la clôture.
- `payment` : un remboursement qui clôt la vente résilie (`ACTIVE`, `SUSPENDED`) ou annule (autres statuts) le contrat lié.
- Portail membre : la signature par `POST /sni/api/v1/client/documents/contracts/{contractCode}/sign` (depuis `GENERATED` ou `AWAITING_SIGNATURE`, ordre de signature des parties respecté) fait passer le contrat en `SIGNED` ou `ACTIVE` comme ci-dessus.

## Écarts relevés

1. **Corrigé le 2026-11-08** : la reconduction passe désormais avant l'expiration. Constat d'origine : **La reconduction automatique ne se produit jamais.** Dans `processScheduledTransitions`, l'expiration (`src/main/java/com/sni/bokaticowork/features/contract/service/implementation/ContractLifecycleAutomationServiceImpl.java:68`) passe en `EXPIRED` tous les contrats `ACTIVE` échus avant que la reconduction (`:89`) ne cherche les contrats `ACTIVE` échus. Les contrats `AUTO_RENEW` (hors abonnement) expirent donc sans successeur, et `CONTRACT_AUTO_RENEWED` n'est jamais publié. Le choix `AUTO_RENEW` proposé à l'écran n'a pas d'effet.
2. **Les modèles créés en base ne servent pas à générer un contrat.** Leur code vient de la séquence `CTM-NNNN` (`src/main/resources/db/migration/V259__contract_missing_sequences.sql`), mais la génération le met en minuscules (`ContractGenerationServiceImpl.java:376`) puis cherche `ctm-nnnn` : « Unknown contract template ». Ils sont pourtant listés par `GET /contracts/templates` quand ils sont actifs. De plus, la génération depuis un modèle en base n'utilise que `htmlContent` et ignore ses sections (`ContractGenerationServiceImpl.java:186`), alors que l'aperçu du modèle les utilise (`ContractTemplateServiceImpl.java:224`).
3. **Le lien de signature envoyé par courriel pointe sur l'API, pas sur une page.** `ContractSigningServiceImpl.java:198` construit `<APP_BASE_URL>/sni/api/v1/public/contracts/sign/<token>` : le destinataire ouvre une réponse JSON, sans formulaire pour signer (la signature exige un `POST`). Sans `APP_BASE_URL`, le lien est relatif (`/contracts/sign/<token>`, ligne 196). Il faut une page frontend et que le serveur en connaisse l'adresse.
4. **La page publique de signature ne peut pas montrer le contrat.** `ContractSigningResponse` ne porte ni le PDF ni son code, et le téléchargement des documents exige `DOCUMENT:READ`. Le signataire signe sans voir le texte.
5. **Champs enregistrés mais jamais renvoyés.** `suspensionReason`, `reviewedBy`, `reviewComment`, `renewedFromCode` et `expiryAlertSentDays` existent sur l'entité (`src/main/java/com/sni/bokaticowork/features/contract/model/Contract.java`) mais pas dans `ContractResponse` (`src/main/java/com/sni/bokaticowork/features/contract/dto/response/ContractResponse.java`).
6. **Identifiants d'acteur fournis par le client.** `createdBy`, `uploadedBy`, `proposedBy`, `reviewedBy`, `actorId` sont lus dans la requête (`ContractController.java`, méthodes `create`, `approveReview`, `rejectReview`, `generateFromContract` ; `ContractAmendmentController.java:39` et suivantes) et jamais rapprochés du compte connecté : le journal chaîné des avenants enregistre l'acteur déclaré. `GET /auth/me` ne donne pas d'identifiant numérique. `uploadedBy` est reçu puis ignoré par la génération d'avenant et de brouillon.
7. **Suppression sans contrôle.** `DELETE /sni/api/v1/contracts/{contractCode}` (`ContractServiceImpl.java:275`) supprime logiquement un contrat quel que soit son statut, y compris `ACTIVE` ou `AMENDED` lié à un abonnement, avec le seul droit `BILLING:UPDATE`.
8. **`generate-draft` produit et envoie le PDF avant de vérifier la transition.** `ContractController.java:182` appelle la génération (transaction propre, courriel au client après validation) puis `markGenerated` (`:183`), qui refuse par exemple un contrat `SIGNED` : le document orphelin reste et le client a reçu « Votre contrat est prêt ».
9. **Droits incohérents sur les avenants.** Proposer et lister un avenant demandent `BILLING:CREATE` / `BILLING:READ` (`/contracts/{contractCode}/amendments`), mais toutes les étapes suivantes (`/amendments/**`) demandent `ADMIN:ACCESS`. Un compte facturation peut ouvrir un avenant qu'il ne peut ni rédiger ni annuler, et qui bloque tout autre avenant.
10. **Un remboursement ne clôt pas un contrat `AMENDED`.** `src/main/java/com/sni/bokaticowork/features/payment/service/support/PaymentTransactionWorkflowProcessor.java:371` annule tout contrat autre que `ACTIVE`/`SUSPENDED`, or `AMENDED -> CANCELLED` est interdit : l'erreur est journalisée et le contrat reste en vigueur.
11. **Signer un avenant n'applique rien à la fiche.** `ContractAmendmentServiceImpl.java:174` change seulement le statut du contrat. Une nouvelle date de fin saisie en variable `periode_fin` apparaît dans l'historique des versions mais pas dans `endDate`, qui pilote l'expiration. Seuls les avenants de renouvellement d'abonnement mettent `endDate` à jour (`:237`). Le journal `CONTRACT_AMENDED` indique toujours `previousStatus: ACTIVE` (`:184`), même depuis `SUSPENDED` ou `AMENDED`.
12. **Signature électronique sans document signé distinct.** Après signature par lien, `signedDocumentCode` reçoit le code du PDF brouillon (`ContractSigningServiceImpl.java:156`), ou à défaut le code du contrat lui-même, qui n'est pas un document.
13. **Messages en anglais** : « Unknown contract template: X », « Unable to generate contract PDF: … », « This contract generator supports MEMBER, CUSTOMER or BUSINESS owners only », « Template code is required » (`ContractGenerationServiceImpl.java:299`, `:361`, `:368`, `:374`). Messages français sans accents dans `ContractServiceImpl.java:345`, `:348`, `:448`.
14. **Modèle et brouillon** : la version d'un modèle (`version`) reste toujours à 1. Le statut d'un brouillon se change librement, sans règle. Lister les avenants, le journal ou les sections d'un code inconnu renvoie une liste vide au lieu d'un `404`. La mise à jour d'une section de modèle ou de brouillon exige `content` et `sectionType` même pour changer seulement l'ordre.
