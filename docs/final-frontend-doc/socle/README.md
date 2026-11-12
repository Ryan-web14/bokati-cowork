# socle : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le socle regroupe les outils transverses de `core/` qui ne sont ni la sécurité ni les paramètres : le journal d'audit, le journal des changements de paramètres, les référentiels (pays, devises, adresses), le suivi de la file d'événements interne (outbox), le suivi des courriels envoyés, le registre d'idempotence et la maintenance (purge d'un membre, purge totale).

Public visé : le back-office technique (écrans « Système » ou « Exploitation »), réservés aux comptes qui portent `SYSTEM:SETTINGS` ou `SYSTEM:AUDIT`. Seule la liste des pays est publique, utile aux formulaires d'inscription et de saisie d'adresse de tous les portails.

Il travaille avec tous les modules : chaque module publie ses événements dans l'outbox (notifications, contrats, documents, KYC, réservations, paiements), envoie ses courriels par le service de courriel suivi ici, et ses routes annotées `@Audited` alimentent le journal d'audit. Le module [parametres](../parametres/README.md) écrit le journal des changements de paramètres lu ici.

## Notions

| Objet | Identifiant exposé | Ce que c'est |
| --- | --- | --- |
| Entrée d'audit (`AuditLog`) | `id` numérique (int64, lecture seule) | Une action annotée `@Audited` sur une route : qui, quoi, quel module, succès ou échec, IP, navigateur. |
| Changement de paramètre (`SettingsAuditLogs`) | `id` numérique | Ancienne et nouvelle valeur d'une clé de paramètre, auteur (`changedBy`, id utilisateur int64), motif. |
| Pays (`Country`) | `countryCode` (2 ou 3 lettres, mis en majuscules) | Nom, indicatif téléphonique, devise par défaut, appartenance OHADA. |
| Devise (`Currency`) | `currencyCode` (3 lettres, ISO 4217) | Code et libellé. |
| Adresse (`Address`) | `id` numérique (int64) | Numéro, rue, quartier, ville, pays. Utilisée surtout à l'intérieur d'autres modules (client, entreprise, domiciliation). |
| Événement outbox (`OutboxEvent`) | `id` numérique (int64) | Travail différé publié par un module (`eventType`, `aggregateType`, `aggregateId` = code métier de l'objet concerné), traité en arrière-plan. |
| Courriel (`EmailDelivery`) | `emailNumber`, code lisible de la forme `EML-AAAAMMJJ-00000001` | Un courriel sortant, son destinataire, son sujet, son statut d'envoi, l'objet métier lié (`relatedType`, `relatedCode`). |
| Enregistrement d'idempotence (`IdempotencyRecord`) | `id` numérique | Une clé `Idempotency-Key` reçue, par opération, avec la réponse mémorisée. |
| Rapport de purge (`PurgeReport`) | `memberCode` | Nombre de lignes supprimées (ou qui le seraient) par table pour un membre. |

Les identifiants numériques de ce module ne servent qu'à l'affichage, sauf deux : `POST /sni/api/v1/admin/outbox-events/{id}/requeue` et les routes `/sni/api/v1/addresses/{id}`. Pour ces deux cas, garder l'`id` en texte (voir [Identifiants](../README.md#identifiants--attention-aux-grands-nombres)).

## Statuts et cycle de vie

### Événement outbox (`OutboxEventStatus`)

Source : `src/main/java/com/sni/bokaticowork/core/outbox/service/implementation/OutboxServiceImpl.java`.

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING` | En attente de traitement. | Publication par un module ; `POST .../outbox-events/{id}/requeue` ; `POST .../outbox-events/requeue-failed`. | `PROCESSING` |
| `PROCESSING` | Pris par un traitement, avec un bail de 300 s (`availableAt` = maintenant + 300 s). | Worker ou `POST .../outbox-events/process`. | `PUBLISHED`, `FAILED`, `DEAD`. Si le serveur s'arrête pendant le traitement, l'événement est repris automatiquement à l'expiration du bail. |
| `PUBLISHED` | Traité avec succès (`publishedAt` rempli, `lastError` vidé). | Traitement réussi. | Aucun : la remise en file est refusée (`400`). |
| `FAILED` | Échec, nouvel essai prévu 30 s plus tard (`availableAt`). `attemptCount` augmente, `lastError` contient le message. | Exception pendant le traitement, tant que `attemptCount + 1 < 10`. | `PROCESSING` (automatiquement), `PENDING` (remise en file manuelle). |
| `DEAD` | Abandonné après 10 tentatives. Plus jamais repris seul. | 10e échec. | `PENDING` uniquement par `POST .../outbox-events/{id}/requeue` (la remise en file groupée ne prend pas les `DEAD`). |

Un événement dont aucun processeur ne reconnaît le type échoue à chaque passage (« No outbox processor found for event type ... ») et finit `DEAD` après environ 5 minutes.

### Courriel (`EmailDeliveryStatus`)

Sources : `src/main/java/com/sni/bokaticowork/core/communication/mailService/service/EmailDeliveryTracker.java`, `.../consumer/EmailConsumer.java`, `.../consumer/EmailDlqConsumer.java`.

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `QUEUED` | Enregistré, déposé (ou à déposer) dans la file RabbitMQ. `attempts` n'augmente pas encore. | Envoi par un module ; `PATCH .../email-deliveries/{emailNumber}/retry`. | `SENDING` ; redépôt automatique s'il reste `QUEUED` plus de 120 s. |
| `SENDING` | Le consommateur l'envoie au fournisseur (Microsoft Graph). `attempts` + 1. | Consommateur de file. | `SENT`, `FAILED` |
| `SENT` | Remis au fournisseur (`sentAt`). | Envoi réussi. | Aucun : le renvoi est refusé (`400`). |
| `FAILED` | Échec (`lastError`, `failedAt`). RabbitMQ peut encore réessayer ; après échec définitif le message passe en file morte et `lastError` vaut « Message transféré en DLQ après échec définitif de la livraison RabbitMQ ». | Exception à l'envoi ; file morte. | `SENDING` (nouvel essai de RabbitMQ), `QUEUED` (renvoi manuel). |

### Enregistrement d'idempotence (`IdempotencyStatus`)

Source : `src/main/java/com/sni/bokaticowork/core/idempotency/service/implementation/IdempotencyServiceImpl.java`.

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PROCESSING` | Première requête en cours avec cette clé. | Première réception d'une clé pour une opération. | `COMPLETED`, `FAILED` |
| `COMPLETED` | Réponse mémorisée (`responseBody`) : une même clé rejouée renvoie cette réponse. | Fin réussie. | Aucun. |
| `FAILED` | La requête a levé une erreur (`errorBody`). | Exception. | `PROCESSING` : la même clé, avec le même contenu, relance réellement l'opération. |

### Entrée d'audit (`AuditStatus`)

`SUCCESS` si la méthode annotée s'est terminée normalement, `FAILURE` si elle a levé une exception (y compris une erreur métier `400` ou `404`) ; `errorCode` contient alors le nom de la classe d'exception et `errorMessage` son message (tronqué à 1000 caractères). Pas de transition : une entrée n'est jamais modifiée.

### Pays

Suppression logique : `DELETE /sni/api/v1/countries/{countryCode}` doit passer la colonne `deleted` à vrai (voir les écarts : la route échoue probablement). Les listes filtrent les pays supprimés ; les recherches par code ne les filtrent pas.

## Parcours

### Journal d'audit

1. `GET /sni/api/v1/admin/audit-logs` ([détail](routes/AdminAuditController.md#r-get-sni-api-v1-admin-audit-logs)) avec les filtres :
   - `module` et `action` : égalité exacte, insensible à la casse. Valeurs de `module` présentes dans le code : `INVENTORY`, `COMPANY`, `DOCUMENT`, `RESOURCE`, `KYC`, `CONTRACT`, `MEMBER`, `TASK`, `CONTRACT_AMENDMENT`, `CONTRACT_DRAFT`, `USER`, `CONTRACT_TEMPLATE`, `BUSINESS`, `ROLE`, `PERMISSION`, `CUSTOMER`, `ROLE_PERMISSION`, `MAINTENANCE`, `CURRENCY`, `COUNTRY`, `ADDRESS`, `SETTINGS`, `ROLE_USER`, `EVENT_REGISTRATION`, `MOBILE_MONEY`. Aucune route ne fournit cette liste : la coder en dur ou laisser un champ libre.
   - `actorEmail` : recherche « contient », insensible à la casse.
   - `status` : `SUCCESS` ou `FAILURE`.
   - `createdFrom`, `createdTo` : date-heure ISO (bornes incluses).
   - Tri par défaut `createdAt,DESC`, 20 par page.
2. La réponse est une page `PaginatedResponse<AuditLog>`. Afficher `createdAt`, `actorEmail`, `module`, `action`, `ressource`, `auditStatus`, et en détail `ipAddress`, `userAgent`, `errorMessage`, `metadata`.
3. `metadata` contient toujours `method` (classe et méthode Java), `requestUri`, `httpMethod`, et `queryString` quand il y en a un ; en cas d'échec, `exception`. `diff` est toujours `null` (aucun code ne le remplit).
4. Une action faite sans compte (route publique) apparaît avec `actorEmail = "SYSTEM"` et `actorId = 0`.

L'écriture du journal est asynchrone (`AuditServiceImpl.save` en `@Async`) : une action qui vient d'être faite peut n'apparaître qu'après un court délai. Prévoir un bouton « Actualiser » plutôt que d'attendre l'entrée juste après l'action.

### Journal des changements de paramètres

1. `GET /sni/api/v1/admin/settings-audit-logs` ([détail](routes/AdminSettingsAuditController.md#r-get-sni-api-v1-admin-settings-audit-logs)).
   - `settingKey` : recherche « contient », insensible à la casse.
   - `changedBy` : id utilisateur exact (int64, à envoyer en texte).
   - `changedFrom`, `changedTo` : bornes sur `changedAt`. Tri par défaut `changedAt,DESC`.
2. Afficher `settingKey`, `oldValue` → `newValue`, `changedAt`, `reason`. `changedBy` est un id numérique : aucune route de ce module ne le traduit en nom ; utiliser la liste des utilisateurs du module [securite](../securite/README.md) si un nom est voulu.

Les lignes sont écrites par les paramètres de plateforme (`core/settings/platform/PlatformSettingsAdminService.java:198`) et les préférences des membres (`core/settings/userSettings/service/implementation/MemberSettingsServiceImpl.java:227`). Le module [parametres](../parametres/README.md) décrit ce qui provoque ces écritures.

### Pays : liste publique et sélecteurs

1. `GET /sni/api/v1/countries` (public, sans jeton) renvoie tous les pays non supprimés, triés par nom, avec `countryCode`, `name` (en majuscules), `phoneCode`, `defaultCurrency` (`{ currencyCode, currencyName }` ou `null`) et `ohadaMember`.
2. C'est cette route qu'il faut utiliser pour les sélecteurs de pays et d'indicatif dans les formulaires publics et le portail membre : `GET /sni/api/v1/countries/phone-codes` et `GET /sni/api/v1/countries/by-phone-code` exigent `SYSTEM:SETTINGS`.
3. La charger une fois et la garder en cache côté écran.

### Pays : gestion (back-office)

1. Créer : `POST /sni/api/v1/countries` avec `name`, `countryCode`, `phoneCode` (tous trois obligatoires, non vides), `currencyCode` (facultatif, devise existante), `isOhadaMember` (à envoyer, voir les règles). Réponse `201` **sans corps** : recharger la liste.
2. Lire : `GET /sni/api/v1/countries/{countryCode}` (code mis en majuscules par le serveur).
3. Modifier : `PUT /sni/api/v1/countries/{countryCode}` avec le même corps. Réponse `200` avec le `CountryResponse` à jour. Envoyer le code du chemin **en majuscules** (la recherche n'y est pas convertie, voir les écarts).
4. Supprimer : `DELETE /sni/api/v1/countries/{countryCode}`, réponse `204` attendue.

Le champ envoyé s'appelle `isOhadaMember`, le champ reçu `ohadaMember`.

### Devises (back-office)

Deux préfixes équivalents, `/sni/api/v1/currencies` et `/sni/api/v1/currency` (même contrôleur). Utiliser `/sni/api/v1/currencies`.

1. Lister : `GET /sni/api/v1/currencies` (triées par libellé). `GET /sni/api/v1/currencies/all` renvoie exactement la même chose.
2. Créer : `POST /sni/api/v1/currencies` avec `currencyCode` (3 lettres) et `currencyName`. Réponse `201` avec la devise.
3. Lire : `GET /sni/api/v1/currencies/{currencyCode}`.
4. Modifier : `PUT /sni/api/v1/currencies/{currencyCode}` avec les deux champs. Le libellé est enregistré **en majuscules**, le code aussi : le code peut donc être changé par cette route.
5. Supprimer : `DELETE /sni/api/v1/currencies/{currencyCode}` (suppression physique ; voir les écarts).

### Adresses (back-office)

Routes génériques rarement utiles à un écran : les adresses des clients, entreprises et domiciliations se saisissent par les routes de ces modules.

1. Créer : `POST /sni/api/v1/addresses` avec `city` (obligatoire), `countryCode` (obligatoire en pratique : pays existant, code exact), `streetNumber`, `streetName`, `district`. Réponse `201` **sans corps et sans id** : l'adresse créée ne peut pas être relue par cette API.
2. Lire : `GET /sni/api/v1/addresses/{id}` renvoie les champs et `fullAddress` composé (« numéro rue, quartier ville, PAYS »).
3. Modifier : `PUT /sni/api/v1/addresses/{id}`. Réponse `200` sans corps (voir les écarts : la modification n'est probablement pas enregistrée).
4. Supprimer : `DELETE /sni/api/v1/addresses/{id}`, `204`.

### Suivi de la file d'événements (outbox)

1. `GET /sni/api/v1/admin/outbox-events` ([détail](routes/AdminOutboxController.md#r-get-sni-api-v1-admin-outbox-events)) avec les filtres `eventType` et `aggregateType` (égalité, insensible à la casse), `aggregateId` (égalité exacte), `status`, `createdFrom`, `createdTo`.
2. Afficher `eventType`, `aggregateType`, `aggregateId`, `status`, `attemptCount`, `availableAt` (prochain essai), `lastError`, `publishedAt`. `payload` est le JSON de l'événement **sous forme de texte** : le montrer dans un bloc repliable, après `JSON.parse` si besoin.
3. Vue conseillée par défaut : `status=DEAD`, puis `status=FAILED`.
4. Remettre un événement en file : `POST /sni/api/v1/admin/outbox-events/{id}/requeue` ([détail](routes/AdminOutboxController.md#r-post-sni-api-v1-admin-outbox-events-id-requeue)), `204`. Refusé en `400` si l'événement est `PUBLISHED` (son traitement envoie souvent un courriel, il partirait deux fois). Proposer le bouton sur `FAILED`, `DEAD`, et `PROCESSING` bloqué.
5. Remettre en file tous les échecs : `POST /sni/api/v1/admin/outbox-events/requeue-failed?aggregateType=...` (filtre facultatif). Réponse `{ "requeued": n }`. Ne prend que les `FAILED` (pas les `DEAD`), 500 au maximum par appel.
6. Forcer un passage du traitement : `POST /sni/api/v1/admin/outbox-events/process?batchSize=25`. Réponse `{ "processed": n }` (nombre d'événements pris, réussis ou non). Le traitement est synchrone : un gros lot allonge la réponse. Utile surtout si le worker est désactivé.
7. Après chaque action, recharger la liste.

### Suivi des courriels

1. `GET /sni/api/v1/admin/email-deliveries` ([détail](routes/EmailDeliveryController.md#r-get-sni-api-v1-admin-email-deliveries)) avec `status`, `provider` (égalité, insensible à la casse), `recipientEmail` (« contient »), `relatedType` (égalité, insensible à la casse), `relatedCode` (égalité exacte), `createdFrom`, `createdTo`.
2. Le contenu du courriel n'est pas exposé : seuls l'expéditeur, le destinataire, le sujet et le type de corps (`bodyType`) le sont.
3. Détail : `GET /sni/api/v1/admin/email-deliveries/{emailNumber}`.
4. Renvoyer : `PATCH /sni/api/v1/admin/email-deliveries/{emailNumber}/retry` ([détail](routes/EmailDeliveryController.md#r-patch-sni-api-v1-admin-email-deliveries-emailnumber-retry)). Réponse **`202`** avec le courriel repassé `QUEUED`. Refusé en `400` si le courriel est `SENT`. Le renvoi part **sans pièce jointe ni image intégrée** (le journal ne les garde pas) et en priorité normale : le signaler à l'utilisateur avant de confirmer, surtout pour une facture.
5. Pour suivre l'envoi, relire le détail quelques secondes plus tard (pas de temps réel).

Lien avec les autres écrans : depuis une facture, un contrat ou un dossier KYC, filtrer par `relatedCode` pour afficher l'historique des courriels de l'objet.

### Registre d'idempotence

1. `GET /sni/api/v1/admin/idempotency-records` ([détail](routes/AdminIdempotencyController.md#r-get-sni-api-v1-admin-idempotency-records)) avec `operation` (égalité, insensible à la casse, par exemple `CURRENCY_CREATE`), `idempotencyKey` (« contient »), `status`, `createdFrom`, `createdTo`.
2. Écran de diagnostic en lecture seule : aucune route ne permet de supprimer ou débloquer un enregistrement. `responseBody` et `errorBody` sont du texte (JSON sérialisé pour la réponse).

### Purge d'un membre (maintenance)

Sert à retirer définitivement des données de test (membre, client non partagé, compte, factures, paiements, contrats, réservations...). Irréversible.

1. Trouver les candidats : `GET /sni/api/v1/admin/maintenance/members/purge-candidates?createdBefore=2026-01-01T00:00:00Z&limit=200` ([détail](routes/MemberPurgeController.md#r-get-sni-api-v1-admin-maintenance-members-purge-candidates)). Renvoie les membres créés avant la date, les plus anciens d'abord, en excluant ceux rattachés à un compte `ADMIN` ou `SUPER_ADMIN`. `createdBefore` est obligatoire.
2. Prévisualiser : `GET /sni/api/v1/admin/maintenance/members/{memberCode}/purge-preview`. Renvoie un `PurgeReport` avec `dryRun: true`, `rowsAffected` et `rowsByTable` (tables triées par nombre de lignes décroissant). Lire aussi `skippedTables` : tables ignorées et pourquoi. Rien n'est supprimé.
3. Confirmer : afficher le rapport, demander une confirmation explicite (saisie du code du membre par exemple) et un motif.
4. Supprimer un membre : `DELETE /sni/api/v1/admin/maintenance/members/{memberCode}?confirm=true&reason=...`. Réponse `200` avec un `PurgeReport` (`dryRun: false`). Sans `confirm=true` : `400` avec un corps **non standard** `{ "message": "...", "preview": "GET /sni/api/v1/admin/maintenance/members/.../purge-preview" }`.
5. Supprimer un lot : `POST /sni/api/v1/admin/maintenance/members/purge-batch?confirm=true` avec `{ "memberCodes": ["MBR-...", ...], "reason": "..." }`. Réponse `BatchPurgeReport` (`membersPurged`, `rowsAffected`, `reports`). 25 membres au maximum par appel (`MEMBER_PURGE_MAX_BATCH_SIZE`). Le lot est une seule transaction : un membre en erreur (introuvable, administrateur) annule tout le lot.

Le code membre est comparé sans tenir compte des espaces autour ni de la casse.

### Purge totale des données (maintenance)

`DELETE /sni/api/v1/admin/maintenance/purge-data?confirm=true` vide **toutes** les tables sauf les référentiels listés dans `src/main/java/com/sni/bokaticowork/core/maintenance/DataPurgeService.java:25`, puis supprime tous les comptes qui ne sont ni `ADMIN` ni `SUPER_ADMIN`. Réponse `{ message, tablesTruncated, nonAdminUsersDeleted, nonAdminRolesDeleted, adminUsersPreserved, durationMs }`. Sans `confirm=true` : `400` avec `{ "message": "This will permanently delete ALL non-admin data. Pass ?confirm=true to proceed.", "warning": "This action is irreversible." }`.

Ne pas l'exposer dans une interface de production. Si un écran de développement la propose, exiger une double confirmation.

## Règles et erreurs à refléter dans l'écran

Erreurs au format standard (voir [Erreurs](../README.md#erreurs)), sauf mention contraire.

| Situation | Statut | Message (`message`) |
| --- | --- | --- |
| Pays : `name`, `countryCode` ou `phoneCode` vide | `400` | « Invalid country request » |
| Pays : code, nom ou indicatif déjà pris à la création | `400` (pas `409`) | « Error with creation of the country » (la cause précise n'est pas renvoyée) |
| Pays : code, nom ou indicatif déjà pris à la modification | `400` | « Country with code X already exist », « Country with name X already exist », « Phone code X already exist » |
| Pays : `currencyCode` inconnu | `400` | « Invalid currency provided » |
| Pays : code inconnu | `404` | « Country with code X not found » |
| Pays : `countryCode` de plus de 3 caractères | `400` | `VALIDATION_FAILED` |
| Devise : code ou libellé vide | `400` | « Invalid currency request » / « Invalid currency update request » |
| Devise : code ou libellé déjà pris | `400` | « Currency with code X already exist » (même si c'est le libellé qui est en double, à la création) |
| Devise : code inconnu en lecture | `404` | « Currency with code X not found » |
| Devise : code inconnu en modification | `400` (pas `404`) | « Currency with code X not found » (X est le code du corps, pas celui du chemin) |
| Adresse : `countryCode` absent ou inconnu | `400` | « No country found for country code X » |
| Adresse : `city` vide | `500` | message générique (l'erreur n'est pas traduite en `400`) |
| Adresse : id inconnu | `404` | « Address with id X not found » |
| Outbox : remise en file d'un événement `PUBLISHED` | `400` | « Outbox event N was already published on ... and cannot be requeued » |
| Outbox : `batchSize` inférieur à 1 | `400` | « Outbox batch size must be greater than zero » |
| Outbox : id inconnu | `404` | « Outbox event with id N not found » |
| Courriel : renvoi d'un courriel `SENT` | `400` | « Email X was already delivered on ... and cannot be retried » |
| Courriel : numéro inconnu | `404` | « Email delivery log not found: X » |
| Purge : `createdBefore` absent | `400` | `MISSING_PARAMETER` |
| Purge : code membre inconnu | `404` | « Aucun membre pour le code X » |
| Purge : membre rattaché à un compte administrateur | `400` | « Le membre X est rattache a un compte administrateur · purge refusee » |
| Purge : interrupteur `MEMBER_PURGE_ENABLED` à `false` | `400` | « La purge par membre est desactivee. ... » (la prévisualisation reste possible) |
| Purge : lot vide | `400` | « Aucun code membre fourni » |
| Purge : lot de plus de 25 | `400` | « Lot de N membres · maximum autorise : 25 » |
| Purge : `confirm` absent | `400` | corps non standard (voir les parcours) |
| Idempotence : même clé, contenu différent | `409` | « Could not process idempotency request: the same key was reused with a different payload » |
| Idempotence : même clé encore en cours | `409` | « Could not process idempotency request: another request with the same key is still processing » |
| Idempotence : clé exigée par configuration et absente | `400` | « Missing required header: Idempotency-Key » |

Règles de saisie :

- Codes pays et devise : 3 caractères au plus (2 ou 3 pour un pays, 3 pour une devise selon l'entité). Le serveur met le code en majuscules à la création ; mettre en majuscules dans l'écran aussi, pour les chemins.
- Nom de pays : enregistré en majuscules.
- Le contrôle d'unicité du nom (pays) et du libellé (devise) à la création est une recherche par expression régulière sensible à la casse sur les noms existants (`CountryRepository.existsByCountryName`, `CurrencyRepository.existsByCurrencyName`) : un nom contenu dans un nom existant est refusé, et un caractère spécial (`(`, `.`, `+`) peut provoquer une erreur `500`.

## À prévoir côté frontend

Écrans :

| Écran | Routes | Droit pour afficher |
| --- | --- | --- |
| Journal d'audit | `GET /admin/audit-logs` | `SYSTEM:AUDIT` |
| Historique des paramètres | `GET /admin/settings-audit-logs` | `SYSTEM:AUDIT` |
| Pays | `/countries/**` | lecture de la liste : public ; le reste : `SYSTEM:SETTINGS` |
| Devises | `/currencies/**` | `SYSTEM:SETTINGS` (lecture comprise) |
| File d'événements | `/admin/outbox-events/**` | `SYSTEM:SETTINGS` |
| Courriels envoyés | `/admin/email-deliveries/**` | `SYSTEM:SETTINGS` |
| Idempotence | `/admin/idempotency-records` | `SYSTEM:SETTINGS` |
| Purge de membres | `/admin/maintenance/members/**` | lecture : `ADMIN:ACCESS` ; suppression : `SYSTEM:SETTINGS` ; dans les deux cas le compte doit **aussi** avoir l'autorité `ROLE_ADMIN` ou `ADMIN` |

Pièges :

- **Purge et `SUPER_ADMIN`** : les quatre routes de purge de membre et la purge totale portent `@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")`. Un compte qui n'a que `ROLE_SUPER_ADMIN` reçoit `403`. Masquer les boutons si `roles` ne contient ni `ROLE_ADMIN` ni `ADMIN`.
- **Devises en lecture** : un écran métier (facturation, tarifs) qui voudrait une liste de devises ne l'obtiendra qu'avec `SYSTEM:SETTINGS`. Utiliser `defaultCurrency` des pays (route publique) si un sélecteur est nécessaire sans ce droit.
- **Réponses sans corps** : `POST /countries` (`201`), `POST /addresses` (`201`), `PUT /addresses/{id}` (`200`) ne renvoient rien. Recharger après coup.
- **Idempotence sur pays et devises** : envoyer `Idempotency-Key` sur `POST` et `PUT` (voir le [README racine](../README.md#idempotence)). Sur les `PUT`, voir l'écart ci-dessous : générer une clé neuve à chaque enregistrement.
- **`payload`**, `responseBody`, `errorBody` sont des textes JSON, pas des objets.
- **`metadata`** et `diff` des entrées d'audit sont des objets libres.
- **Pas de temps réel** dans ce module : actualiser manuellement ou par minuterie les écrans de suivi.
- Les identifiants `id`, `actorId`, `changedBy`, `rowsAffected` et `durationMs` sont des int64 : les lire avec un analyseur qui préserve les grands entiers.

## Effets automatiques

| Automatisme | Fréquence | Effet visible |
| --- | --- | --- |
| `OutboxWorker` (`core/outbox/worker/OutboxWorker.java:23`) | Toutes les 500 ms en dev et en prod (`OUTBOX_WORKER_FIXED_DELAY_MS`, défaut du code 2000 ms), lots de 100 (`OUTBOX_WORKER_BATCH_SIZE`). Désactivable par `OUTBOX_WORKER_ENABLED=false`. | Les événements passent de `PENDING` à `PUBLISHED` en moins d'une seconde ; nouvel essai 30 s après un échec ; `DEAD` au 10e échec. Les traitements créent des notifications, envoient des courriels, génèrent des contrats, relancent des affectations de paiement. |
| `EmailRequeueWorker` (`core/communication/mailService/service/EmailRequeueWorker.java:39`) | Toutes les 60 s (`bokati.mail.requeue.delay-ms`). | Redépose dans RabbitMQ, 50 au plus par passage, les courriels restés `QUEUED` depuis plus de 120 s (broker indisponible au moment de l'envoi). Ils repartent sans pièce jointe. |
| Consommateurs RabbitMQ `email.critical`, `email.high`, `email.normal`, `email.bulk` | En continu, avec un limiteur (25 courriels par minute et par instance par défaut, `GRAPH_RATE_LIMIT`). | `QUEUED` → `SENDING` → `SENT` ou `FAILED`. Un message déjà `SENT` n'est jamais renvoyé. |
| Consommateur `email.dlq` | En continu. | Passe le courriel `FAILED` avec le message de file morte. |
| Déduplication des courriels (`bokati.email.dedup`) | À chaque envoi. | Un même courriel lié à un objet métier n'est pas renvoyé dans les 24 h (1 h pour un courriel sans référence) : il n'apparaît pas une deuxième fois dans la liste. |
| Aspect d'audit | À chaque appel d'une méthode `@Audited`. | Une ligne de journal, écrite de façon asynchrone. |

Aucun worker ne purge le journal d'audit, l'outbox, les courriels ou les enregistrements d'idempotence : ces listes ne font que grandir. Utiliser les filtres de date.

## Écarts relevés

1. **Corrigé le 2026-11-08** : seule la lecture `GET /sni/api/v1/countries` reste publique ; créer, modifier et supprimer exigent un jeton et `SYSTEM:SETTINGS`. Constat d'origine : **Création de pays sans jeton.** `PublicPaths.CATALOGUE` ouvre `/sni/api/v1/countries` pour toutes les méthodes (`src/main/java/com/sni/bokaticowork/security/config/PublicPaths.java:72`, appliqué sans restriction de méthode dans `security/config/SecurityConfig.java:90`). `POST /sni/api/v1/countries` est donc appelable par n'importe qui, sans compte ; l'audit l'enregistre avec l'acteur `SYSTEM`. Seule la lecture semble voulue publique.
2. **Purge interdite au seul `SUPER_ADMIN`.** `@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")` sur `src/main/java/com/sni/bokaticowork/core/maintenance/DataPurgeController.java:18` et `MemberPurgeController.java:80` : un compte `ROLE_SUPER_ADMIN` sans `ROLE_ADMIN` est refusé, contrairement à la règle générale.
3. **Purge totale sans garde d'environnement.** `DataPurgeService.purgeAllNonAdminData` lit les profils actifs (`src/main/java/com/sni/bokaticowork/core/maintenance/DataPurgeService.java:85`) sans s'en servir pour refuser en production ; la route reste joignable avec `SYSTEM:SETTINGS` et `ROLE_ADMIN`. Les tables non listées (journal d'audit, paramètres de plateforme, outbox, courriels) sont vidées aussi.
4. **Interrupteur de purge allumé par défaut.** `MemberPurgeProperties.enabled = true` (`src/main/java/com/sni/bokaticowork/core/maintenance/MemberPurgeProperties.java:21`) et `MEMBER_PURGE_ENABLED:true` dans `src/main/resources/application.yml:84`, alors que la Javadoc de la classe dit « desactivee par defaut ».
5. **Lot de purge sans validation.** `purgeBatch` reçoit `@RequestBody BatchPurgeRequest` sans `@Valid` (`src/main/java/com/sni/bokaticowork/core/maintenance/MemberPurgeController.java:117`) : `@NotEmpty` n'est pas appliqué, et un corps sans `memberCodes` appelé sans `confirm=true` provoque une `NullPointerException` (`500`) à la ligne 120.
6. **Suppression de pays et de devise probablement en échec.** `softDeleteByCountryCode` et `deleteByCurrencyCode` sont des requêtes `@Modifying` (`src/main/java/com/sni/bokaticowork/core/baseClasses/repository/CountryRepository.java`, `CurrencyRepository.java`) appelées depuis des méthodes sans `@Transactional` (`CountryServiceImpl.java:137`, `CurrencyServiceImpl.java:71`). Spring Data refuse en principe une requête de modification hors transaction : réponse `500` attendue. Non vérifié à l'exécution.
7. **Pays supprimés encore visibles par code.** `findByCountryCode` et les contrôles `exists*` sont des requêtes natives (`CountryRepository.java`) qui ignorent `@SQLRestriction("deleted = false")` : un pays supprimé reste lisible par `GET /countries/{code}`, utilisable dans une adresse, et son code ne peut pas être recréé.
8. **Modification de pays incomplète.** Le décorateur `CountryMapperDecorator.mapCountryUpdate` (`src/main/java/com/sni/bokaticowork/core/baseClasses/mapper/decorator/CountryMapperDecorator.java`) n'applique pas `phoneCode` : l'indicatif ne change jamais par `PUT`. Le nom n'est remplacé que s'il diffère exactement de l'ancien, sinon reste tel quel. `updateCountry` cherche le code du chemin sans le mettre en majuscules (`CountryServiceImpl.java:81`), contrairement à la lecture.
9. **Création de pays sans `isOhadaMember`.** La colonne est `NOT NULL` (`Country.java:44`) et le service ne donne pas de valeur par défaut : un corps sans ce champ finit en erreur base de données (`500`). Non vérifié à l'exécution.
10. **Modification d'adresse non enregistrée.** `AddressServiceImpl.updateAddress` (`src/main/java/com/sni/bokaticowork/core/baseClasses/service/implementation/AddressServiceImpl.java:39`) modifie l'entité sans `save` et sans transaction : la modification est probablement perdue alors que la route répond `200`. La création ne renvoie ni corps ni id (`AddressController.java:25`), et une ville vide lève `IllegalArgumentException`, rendue en `500`.
11. **Idempotence des `PUT` calculée sur le chemin, pas sur le corps.** `@Idempotent` prend par défaut l'argument d'indice 0 (`src/main/java/com/sni/bokaticowork/core/idempotency/aop/Idempotent.java`), qui est la variable de chemin pour `updateCountry` (`CountryController.java:40`) et `updateCurrency` (`CurrencyController.java:39`). Une même clé réutilisée avec un autre corps rejoue l'ancienne réponse au lieu de répondre `409`.
12. **Clés d'idempotence globales et bloquables.** L'enregistrement est indexé par opération et clé seulement, sans l'utilisateur (`IdempotencyServiceImpl.java:176`). Un enregistrement resté `PROCESSING` (arrêt du serveur en cours de requête) bloque définitivement sa clé en `409`, et aucune route ne permet de le débloquer.
13. **Événements outbox sans processeur.** Les événements d'inventaire publiés avec `aggregateType` `STOCK_MOVEMENT` et `INVENTORY_ASSET` (`src/main/java/com/sni/bokaticowork/features/inventory/intelligence/service/implementation/InventoryAutomationServiceImpl.java:70` et `:224`) ne sont reconnus par aucun processeur de `core/outbox/service/implementation/` : ils finissent `DEAD` et encombrent l'écran de suivi.
14. **Remise en file groupée sans les `DEAD`.** `requeueFailed` ne prend que `FAILED` (`OutboxServiceImpl.java:199`), alors que `FAILED` est repris automatiquement : le bouton n'a d'effet utile que pour avancer un essai, pas pour les événements abandonnés.
15. **Renvoi d'un courriel en cours.** `DefaultEmailSender.retry` (`src/main/java/com/sni/bokaticowork/core/communication/mailService/baseService/DefaultEmailSender.java:159`) n'écarte que `SENT` : un courriel `SENDING` peut être redéposé et partir deux fois. Le renvoi perd la pièce jointe.
16. **Audit sans différence.** `AuditContext.setDiff` et `putMeta` ne sont appelés nulle part : `diff` est toujours `null` et `metadata` ne contient que les informations de requête.
17. **Routes en double.** `/sni/api/v1/currency/**` double `/sni/api/v1/currencies/**`, et `GET /currencies/all` double `GET /currencies` (`CurrencyController.java:21` et `:54`).
18. **Messages en anglais** dans les pays, devises, adresses, outbox, courriels, idempotence et purge totale ; messages de purge de membre en français sans accents.
19. **Corps d'erreur non standard** pour `confirm` absent sur `DELETE /admin/maintenance/purge-data` et `DELETE /admin/maintenance/members/{memberCode}` (`DataPurgeController.java:28`, `MemberPurgeController.java:105`) : ni `errorCode` ni `traceId`.
