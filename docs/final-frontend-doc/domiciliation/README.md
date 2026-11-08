# domiciliation : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module porte trois choses : le **catalogue des services** qu'un abonnement peut contenir en plus de son plan (domiciliation, courrier, casier, parking…), le **contrat de domiciliation** d'une entreprise ou d'un membre, de l'ouverture à l'activation puis à la fin, et le **courrier** reçu pour le domicilié (réception, numérisation, remise, réexpédition).

- **Back-office** (`/sni/api/v1/services/**`, 33 routes) : l'accueil et l'administration ouvrent le contrat, font avancer chaque étape, délivrent l'attestation, enregistrent et remettent le courrier.
- **Portail membre** (`/sni/api/v1/client/...`, 7 routes, lecture seule) : le domicilié consulte son contrat, télécharge le contrat et l'attestation, suit son courrier.
- Il travaille avec `subscription` (un contrat se rattache toujours à un abonnement existant), `document` (toutes les pièces sont des documents, désignés par leur code), `document`/KYC (niveau de vérification exigé), `company` (fiche juridique de l'organisation, droit `canCollectMail` des membres rattachés), `billing` par le module d'usage (actes de courrier facturés), `notification` (courriels via la file d'événements).

Il n'existe **aucune route de demande** côté portail ni côté public : la demande arrive hors application (accueil, prospect CRM avec l'intérêt `DOMICILIATION`, `src/main/java/com/sni/bokaticowork/features/crm/enums/LeadInterest.java:7`), et c'est le back-office qui ouvre le contrat sur un abonnement déjà créé.

## Notions

| Objet | Identifiant exposé | Format | Liens |
| --- | --- | --- | --- |
| Service du catalogue (`ServiceDefinition`) | `code` (saisi) | ex. `SVC-DOMICILIATION`, `SVC-MAIL-SCAN` | utilisé par les services souscrits et par la facturation du courrier |
| Service souscrit (`SubscriptionService`) | `serviceNumber` | `SSV-AAAAMM-000001` | un abonnement (`subscriptionNumber`) contient 0 à n services ; le contrat de domiciliation pointe sur l'un d'eux |
| Adresse du registre (`DomiciliationAddress`) | `code` | `DAD-0001` | une adresse postale attribuable ; plusieurs contrats peuvent la partager, séparés par `suiteNumber` |
| Contrat de domiciliation (`DomiciliationContract`) | `contractNumber` | `DOM-AAAA-00001` | un abonnement, un service souscrit, une adresse du registre ; ses pièces sont des codes de document |
| Démarche d'enregistrement (`DomiciliationRegistration`) | `registrationNumber` | `DRG-AAAA-00001` | n démarches par contrat (une nouvelle après chaque rejet) |
| Pli (`MailItem`) | `itemNumber` | `MAIL-AAAAMM-000001` | un contrat ; un journal d'événements (`trail`) non modifiable |

Aucun identifiant numérique n'est exposé par ce module : tout passe par ces codes lisibles. Les formats viennent des séquences de `src/main/resources/db/migration/V239__services_and_domiciliation.sql:342-360`.

**Domicilié.** C'est le titulaire de l'abonnement : un membre (`subscriberType = MEMBER`) ou une organisation (`CUSTOMER` de type `COMPANY`). Pour une organisation, la fiche juridique de l'entreprise fait foi : dénomination, forme, RCCM, NIU et représentant sont recopiés sur le contrat à l'ouverture, avant la génération du contrat, avant chaque attestation, et à chaque modification de la fiche (`DomiciliationService.applyCard`, `refreshCompany`, écouteur `DomiciliationCardListener`).

**Qualité fiscale.** Elle ne se saisit jamais. `fiscalAddressEligible` vaut `true` si et seulement si `commitmentMonths >= 12` **et** l'adresse du registre a `fiscalCapable = true` (`DomiciliationService.java:481-507`). La base le garantit aussi (`ck_domiciliation_fiscal`). Une attestation `FISCAL` n'est possible que sur un contrat éligible ; une attestation `COMMERCIAL` est toujours possible sur un contrat `ACTIVE`.

**Pièces.** Le contrat généré (`contractDocumentCode`) et l'attestation (`certificateDocumentCode`) sont produits par le serveur. L'exemplaire signé (`signedDocumentCode`), l'exemplaire enregistré et timbré (`registeredDocumentCode`), la quittance (`receiptDocumentCode`) et la numérisation d'un pli (`scanDocumentCode`) sont **téléversés d'abord** dans le module `document`, puis leur code est transmis à la route de l'étape.

**Qui voit quoi côté portail** (`DomiciliationCompanyAccess.java:330-354`) : un contrat au nom d'un membre est vu par ce membre seul. Un contrat d'organisation est vu par le gérant (lien de rôle `OWNER`) et par les membres rattachés dont le lien actif porte `canCollectMail = true`. Le lien se règle dans `company` (`POST/PATCH /sni/api/v1/customers/{customerId}/members...`). La fin du lien retire l'accès.

## Statuts et cycle de vie

### Contrat de domiciliation (`DomiciliationContract.Status`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `DRAFT` | Ouvert, pièces non encore contrôlées | `POST /sni/api/v1/services/domiciliation/contracts` | `PENDING_SIGNATURE` ou `PENDING_DOCUMENTS` (`prepare`), `TERMINATED` |
| `PENDING_DOCUMENTS` | Le niveau KYC du titulaire est insuffisant | `prepare` refusé (le statut est enregistré **puis** la réponse est `409`) | `PENDING_SIGNATURE` (`prepare` rejoué une fois le KYC validé), `TERMINATED` |
| `PENDING_SIGNATURE` | Contrat PDF généré, en attente de signature | `prepare` réussi | `PENDING_REGISTRATION` (`signed`), `TERMINATED` |
| `PENDING_REGISTRATION` | Signé ; démarche d'enregistrement auprès de l'administration en cours | `signed` | `ACTIVE` (`registration/confirm`), `TERMINATED`. Un rejet laisse le contrat ici. |
| `ACTIVE` | Enregistré, service rendu, courrier accepté, attestation possible | `registration/confirm` | `TERMINATED` (`terminate`), `EXPIRED` (automatique) |
| `SUSPENDED` | Prévu (compte comme « vivant » pour le courrier) | **jamais atteint** : aucun code ne pose ce statut | |
| `TERMINATED` | Résilié | `terminate`, depuis tout statut non terminé | fin |
| `EXPIRED` | Arrivé à terme sans reconduction | worker quotidien (07:40) si `ACTIVE`, `endDate` passée et abonnement sans `autoRenew` | fin |

Le courrier n'est accepté que sur `ACTIVE` (ou `SUSPENDED`). Un seul contrat non terminé par abonnement, et un seul contrat non terminé par couple adresse + `suiteNumber` (index unique `uk_domiciliation_suite_active`, `V239__services_and_domiciliation.sql:201`).

### Démarche d'enregistrement (`DomiciliationRegistration.Status`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING` | À déposer auprès de l'administration | créée automatiquement par `signed`, et à nouveau après chaque rejet | `SUBMITTED` (`registration/submit`), `REGISTERED` (`confirm` directement), `REJECTED` |
| `SUBMITTED` | Déposée (bureau, droits saisis) | `registration/submit` | `REGISTERED`, `REJECTED` |
| `REGISTERED` | Obtenue ; sa référence figure sur l'attestation | `registration/confirm` (le contrat passe `ACTIVE`) | fin |
| `REJECTED` | Refusée, motif conservé | `registration/reject` (une nouvelle démarche `PENDING` est ouverte aussitôt) | fin |

### Service souscrit (`SubscriptionService.Status`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING` | Souscrit mais pas encore rendu (le service exige un contrat ou une ressource physique) | `POST /sni/api/v1/services/subscriptions/{subscriptionNumber}`, ou l'ouverture d'un contrat de domiciliation | `ACTIVE` (confirmation de l'enregistrement du contrat), `TERMINATED` |
| `ACTIVE` | Rendu | souscription d'un service sans contrat ni ressource ; `registration/confirm` pour la domiciliation | `SUSPENDED`, `TERMINATED` |
| `SUSPENDED` | Suspendu | `POST /sni/api/v1/services/subscribed/{serviceNumber}/suspend` (depuis `ACTIVE` seulement) | `TERMINATED` ; **aucune route de reprise** |
| `TERMINATED` | Terminé, ne se réactive pas | `.../terminate`, ou résiliation du contrat de domiciliation | fin |

### Pli (`MailItem.Status`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `RECEIVED` | Enregistré | transitoire : `POST .../contracts/{contractNumber}/mail` le fait passer aussitôt à `NOTIFIED` dans le même appel | |
| `NOTIFIED` | Titulaire prévenu (courriel) | réception | `SCANNED`, `COLLECTED`, `FORWARDED`, `RETURNED`, `DESTROYED` |
| `SCANNED` | Numérisé, toujours au guichet | `scan` (rejouable, chaque passage facture) | mêmes sorties que `NOTIFIED` |
| `COLLECTED` | Remis en main propre | `collect` | fin |
| `FORWARDED` | Réexpédié | `forward` | fin |
| `RETURNED` | Retourné à l'expéditeur | `return` | fin |
| `DESTROYED` | Détruit | `destroy` (interdit pour les plis remis contre identité) | fin |

Sur un pli fermé (`COLLECTED`, `FORWARDED`, `RETURNED`, `DESTROYED`), toute action renvoie `409` « ce pli est déjà ... ». Le pli reçoit à la réception une date limite de garde `storageDeadline` = aujourd'hui + 30 jours (`bokati.domiciliation.mail.storage-days`).

## Parcours

### 1. Paramétrer le catalogue et le registre des adresses (administration)

1. `GET /sni/api/v1/services/catalogue?includeInactive=true` : la liste des services, triée par nom. Les services livrés par la migration : `SVC-DOMICILIATION` (exige un contrat et le KYC niveau 2), `SVC-MAIL-HANDLING`, `SVC-MAIL-SCAN` (500 XAF l'acte), `SVC-MAIL-FORWARD` (2000 XAF l'acte, transport en sus), `SVC-MAIL-STORAGE` (100 XAF par jour et par pli), `SVC-PHONE-ANSWERING`, `SVC-LOCKER`, `SVC-PARKING`.
2. Créer ou modifier : `PUT /sni/api/v1/services/catalogue` ([détail](routes/DomiciliationController.md#r-put-sni-api-v1-services-catalogue)). Le `code` sert de clé : s'il existe, **tous** les champs sont remplacés par ceux envoyés (voir les pièges plus bas). Obligatoires : `code`, `name`, `serviceCategory`.
3. `GET /sni/api/v1/services/domiciliation/addresses` : le registre, trié par libellé, avec `occupants` (contrats non terminés à cette adresse) et `maxOccupants`.
4. `POST /sni/api/v1/services/domiciliation/addresses` avec `label`, `address` (adresse postale, modèle `AddressRequest`), `fiscalCapable` (vrai par défaut), `maxOccupants` (vide = sans limite), `notes`. Retour : l'adresse avec son `code` `DAD-....`.
5. `PATCH /sni/api/v1/services/domiciliation/addresses/{code}` : `label`, `fiscalCapable`, `maxOccupants`, `active`, `notes`. Seuls les champs non nuls changent. L'adresse postale elle-même ne se modifie pas ici. Passer `active=false` retire l'adresse des nouvelles ouvertures sans toucher aux contrats en cours. Changer `fiscalCapable` **ne recalcule pas** les contrats existants (la règle n'est rejouée qu'à l'ouverture et au changement d'engagement).

### 2. Ouvrir un contrat (administration)

Prérequis : un abonnement existe (module `subscription`) pour le membre ou l'organisation, non résilié ni expiré ; le titulaire a un dossier KYC approuvé de niveau 2 au moins (voir l'écart 1).

1. Charger le registre (`GET .../domiciliation/addresses`) pour proposer les adresses `active` dont `occupants < maxOccupants`.
2. `POST /sni/api/v1/services/domiciliation/contracts` ([détail](routes/DomiciliationController.md#r-post-sni-api-v1-services-domiciliation-contracts)) :
   - obligatoires : `subscriptionNumber`, `addressCode`, `billingCycle` (`MONTHLY`, `QUARTERLY` ou `YEARLY` seulement) ;
   - `commitmentMonths` : par défaut 12 pour `YEARLY`, 3 pour `QUARTERLY`, 1 pour `MONTHLY` ; c'est lui qui décide de la qualité fiscale ;
   - `noticePeriodDays` (30 par défaut), `startDate` (sinon la date d'enregistrement), `suiteNumber`, `mailForwardingMode` (`HOLD` par défaut ; `FORWARD`, `SCAN_AND_FORWARD`, `SCAN_ONLY`) ;
   - `legalName`, `legalForm`, `registrationNumber`, `taxNumber`, `legalRepresentativeName` : **obligatoire seulement `legalName` pour un membre**. Pour une organisation, la fiche de l'entreprise fait foi ; les valeurs envoyées ne servent qu'à compléter une fiche vide (elles y sont reportées, sans jamais écraser une valeur saisie).
3. Effets : si l'abonnement n'a pas encore de service de catégorie `DOMICILIATION` non terminé, le serveur souscrit `SVC-DOMICILIATION` (statut `PENDING`). Le contrat revient en `DRAFT` avec `fiscalAddressEligible` calculé et `endDate = startDate + commitmentMonths` si `startDate` est fourni.
4. L'écran affiche le contrat, sa qualité (fiscale ou commerciale) et propose « Générer le contrat ».

### 3. Générer, faire signer, enregistrer, activer (administration)

1. **Générer** : `POST .../contracts/{contractNumber}/prepare` (sans corps). Possible depuis `DRAFT` ou `PENDING_DOCUMENTS`.
   - KYC insuffisant : `409` avec le message « les pièces d'identité du représentant légal doivent être vérifiées (niveau 2) avant de générer le contrat · le dossier est en attente de pièces », et le contrat est **resté** en `PENDING_DOCUMENTS` (recharger le contrat après l'erreur). Le contrôle porte sur le dossier KYC du titulaire de l'abonnement (membre ou organisation), pas sur une fiche du représentant.
   - Succès : PDF généré et déposé au module `document` (type `DOMICILIATION_CONTRACT`), `contractDocumentCode` rempli, statut `PENDING_SIGNATURE`. Pour l'afficher : `GET /sni/api/v1/documents/{code}/download` (droit `DOCUMENT:READ`, module `document`).
2. **Signer** : faire signer hors application, puis téléverser l'exemplaire signé par `POST /sni/api/v1/documents/upload` (multipart, `documentTypeCode = DOMICILIATION_CONTRACT_SIGNED`, PDF ou image ; `ownerType`/`ownerCode` = titulaire, `MEMBER` + `memberId` ou `CUSTOMER` + `customerId`). Récupérer le `code` du document renvoyé.
3. `POST .../contracts/{contractNumber}/signed` avec `{ "signedDocumentCode": "<code>" }`. Statut `PENDING_REGISTRATION`, `signedAt` posé, et une démarche d'enregistrement `PENDING` est créée.
4. `GET .../contracts/{contractNumber}/registrations` : les démarches du contrat, la plus récente d'abord.
5. **Déposer** (facultatif) : `POST .../contracts/{contractNumber}/registration/submit` avec `administrationOffice`, `stampDutyAmount`, `registrationFeeAmount` (le serveur calcule `totalDutyAmount`), `paidBy` (`COMPANY` ou `CLIENT`), `rebilled`. La démarche passe `SUBMITTED`. Devise fixée à `XAF`.
6. **Confirmer** : téléverser l'exemplaire enregistré et timbré (`DOMICILIATION_REGISTERED_COPY`, ce type exige une date d'expiration au téléversement d'après la migration, non vérifié côté contrôle) et éventuellement la quittance (`DOMICILIATION_REGISTRATION_RECEIPT`), puis `POST .../registration/confirm` avec `administrationReference` et `registeredDocumentCode` (obligatoires), `registrationDate` (aujourd'hui par défaut), `expiresAt`, `receiptDocumentCode`.
   - Effets : démarche `REGISTERED`, contrat `ACTIVE`, `startDate` posée à la date d'enregistrement si elle était vide (et `endDate` recalculée), `fiscalAddressGrantedAt` posé si éligible, service souscrit `ACTIVE`, courriel `DOMICILIATION_ACTIVATED` au titulaire.
7. **Rejet** : `POST .../registration/reject` avec `reason` (obligatoire). La démarche passe `REJECTED`, une nouvelle `PENDING` est ouverte, le contrat reste `PENDING_REGISTRATION`, courriel `DOMICILIATION_REGISTRATION_REJECTED` avec le motif. L'écran affiche le motif et repropose « Déposer ».
8. **Tableau de suivi** : `GET /sni/api/v1/services/domiciliation/registrations/in-progress` (paginé, 20 par défaut) liste les démarches `PENDING` et `SUBMITTED`, triées par date de dépôt (les `PENDING`, sans date, ont un ordre non garanti).

### 4. Délivrer l'attestation (administration)

1. `POST .../contracts/{contractNumber}/certificate` avec `{ "scope": "COMMERCIAL" }` ou `"FISCAL"`.
2. Conditions : contrat `ACTIVE`, sinon `409` « aucune attestation avant l'enregistrement du contrat · le contrat est ... ». `FISCAL` sur un contrat non éligible : `409` « l'adresse n'est fiscale que si l'abonnement est pris pour un an · cet engagement est de N mois, seule une attestation commerciale peut être délivrée ». Masquer l'option `FISCAL` quand `fiscalAddressEligible` est faux.
3. Effets : PDF généré (type `DOMICILIATION_CERTIFICATE`), `certificateDocumentCode`, `certificateScope`, `certificateIssuedAt`, `certificateValidUntil` = aujourd'hui + 365 jours (`bokati.domiciliation.certificate-validity-days`, 30 au minimum), **plafonnée à `endDate`** du contrat. Une révocation antérieure est effacée. Courriel `DOMICILIATION_CERTIFICATE_ISSUED`.
4. Afficher `certificateInForce` (calculé : document présent, non révoqué, date de validité non dépassée) plutôt que de déduire soi-même.

### 5. Changer l'engagement (administration)

1. `POST .../contracts/{contractNumber}/commitment` avec `billingCycle` et/ou `commitmentMonths` (au moins 1). Possible dans tout statut non terminé.
2. `endDate` est recalculée si `startDate` existe. La qualité fiscale est recalculée :
   - perte (passage sous 12 mois) : `fiscalAddressRevokedAt` et motif posés, attestation `FISCAL` en cours **révoquée** ; si le contrat est `ACTIVE`, courriel `DOMICILIATION_FISCAL_LOST` au titulaire et, si configuré, à l'administration fiscale ;
   - gain : rien n'est délivré, l'attestation fiscale se demande ensuite (parcours 4).
3. Le PDF du contrat n'est pas régénéré. Prévenir l'utilisateur par une confirmation quand le changement fait perdre la qualité fiscale.

### 6. Résilier (administration)

1. `POST .../contracts/{contractNumber}/terminate` avec `reason` (obligatoire) et `effectiveDate` (par défaut aujourd'hui + `noticePeriodDays`).
2. Effets immédiats, quelle que soit la date d'effet : statut `TERMINATED`, `endDate = effectiveDate`, attestation révoquée, `retainDocumentsUntil = effectiveDate + 10 ans`, service souscrit `TERMINATED`, courriel `DOMICILIATION_TERMINATED`, et avis à l'administration si configuré (`administrationNotifiedAt`).
3. La résiliation d'un abonnement (module `subscription`) affiche des éléments de sortie « Contrat de domiciliation résilié » et « courrier en attente », cochés automatiquement quand le contrat est clos et qu'il ne reste aucun pli `RECEIVED`/`NOTIFIED`/`SCANNED` (`SubscriptionTerminationService.java:379-414`). Ce module ne résilie pas le contrat de lui-même.

### 7. Listes et registre (administration)

1. `GET /sni/api/v1/services/domiciliation/contracts?status=PENDING_REGISTRATION&status=ACTIVE&page=0&size=20` : contrats, les plus récents d'abord ; `status` est répétable. Le paramètre `sort` est ignoré (`DomiciliationController.java:263`).
2. `GET .../contracts/{contractNumber}` : le détail (modèle `ContractView`).
3. `GET /sni/api/v1/services/domiciliation/registry` : fichier CSV `registre-domicilies.csv` (séparateur `;`, UTF-8) de tous les contrats, terminés compris, dans l'ordre des entrées. Télécharger en blob.

### 8. Courrier au guichet (administration)

1. **Réception** : `POST .../contracts/{contractNumber}/mail` avec `mailType` (obligatoire : `LETTER`, `REGISTERED_LETTER`, `PARCEL`, `ADMINISTRATIVE`, `LEGAL_NOTICE`, `OTHER`), `senderName`, `senderReference`, `weightGrams`, `dimensions`, `notes`. Contrat non `ACTIVE` : `409`. Le pli revient en `NOTIFIED` ; courriel `MAIL_RECEIVED` au titulaire (objet « Courrier important reçu · ... » pour un recommandé, un courrier administratif ou un acte).
2. **À traiter** : `GET /sni/api/v1/services/domiciliation/mail/pending` (paginé) liste les plis `RECEIVED`, `NOTIFIED`, `SCANNED`, du plus ancien au plus récent. Par contrat : `GET .../contracts/{contractNumber}/mail` (du plus récent au plus ancien). Détail : `GET .../mail/{itemNumber}` ; journal : `GET .../mail/{itemNumber}/trail` (événements `RECEIVED`, `NOTIFIED`, `SCANNED`, `COLLECTED`, `FORWARDED`, `RETURNED`, `DESTROYED`, `STORAGE_CHASED`, avec acteur et détail).
3. **Numériser** : téléverser le fichier (`documentTypeCode = MAIL_SCAN`), puis `POST .../mail/{itemNumber}/scan` avec `scanDocumentCode`. Statut `SCANNED`, acte facturé (`SVC-MAIL-SCAN`), courriel `MAIL_SCANNED`.
4. **Remettre** : `POST .../mail/{itemNumber}/collect` avec :
   - `collectedBy` (nom du porteur), ou `collectorMemberId` (identifiant membre `memberId` d'un gérant ou d'un membre autorisé ; son nom remplace alors `collectedBy` s'il est vide) ;
   - `collectorIdDocument` : **obligatoire** pour `REGISTERED_LETTER`, `ADMINISTRATIVE` et `LEGAL_NOTICE` (référence de la pièce d'identité) ;
   - `collectorSignatureUrl` : facultatif, texte libre, aucun téléversement associé.
   Pour une organisation, proposer la liste des membres autorisés (lien `OWNER` ou `canCollectMail`, module `company`) ; un membre non autorisé donne `400` « Ce membre n'est pas autorisé à retirer le courrier de ... ».
5. **Réexpédier** : `POST .../mail/{itemNumber}/forward` avec `trackingNumber` et `transportCost`. Exige un contrat en mode `FORWARD` ou `SCAN_AND_FORWARD` **et** une adresse de réexpédition enregistrée (voir l'écart 2). Acte facturé (`SVC-MAIL-FORWARD` + transport), courriel `MAIL_FORWARDED`.
6. **Retourner** : `POST .../mail/{itemNumber}/return`, corps facultatif `{ "reason": "..." }`.
7. **Détruire** : `POST .../mail/{itemNumber}/destroy` avec `reason` obligatoire. Refusé (`409`) pour les plis remis contre identité : proposer « Retourner » à la place.

### 9. Espace du domicilié (portail membre)

1. `GET /sni/api/v1/client/domiciliation/contracts` : les contrats au nom du membre, puis ceux des organisations dont il est gérant ou autorisé à retirer le courrier. Liste non paginée.
2. Pour une organisation précise : `GET /sni/api/v1/client/companies/{customerId}/domiciliation` (`404` « Organisation introuvable » si le membre n'y a pas accès).
3. Détail : `GET /sni/api/v1/client/domiciliation/contracts/{contractNumber}` (`404` « Contrat introuvable » aussi bien pour un contrat inexistant que pour un contrat d'autrui).
4. Télécharger le contrat : `GET .../contracts/{contractNumber}/document` (PDF `contrat-<numéro>.pdf` : l'exemplaire signé s'il existe, sinon le contrat généré ; `404` « Document pas encore disponible » avant `prepare`). Afficher le bouton dès que `contractDocumentCode` ou `signedDocumentCode` est rempli.
5. Télécharger l'attestation : `GET .../contracts/{contractNumber}/certificate` (PDF `attestation-<numéro>.pdf`), seulement si `certificateInForce` ; sinon `404` « Aucune attestation en vigueur ».
6. Courrier : `GET .../contracts/{contractNumber}/mail` (paginé, plus récent d'abord) et `GET .../contracts/{contractNumber}/mail/{itemNumber}/trail` (journal). Aucune action côté portail : la remise se fait au guichet. La numérisation d'un pli n'est **pas** téléchargeable depuis le portail (aucune route, voir les écarts).

### 10. Services souscrits d'un abonnement (administration)

1. `GET /sni/api/v1/services/subscriptions/{subscriptionNumber}` : les services de l'abonnement, dans l'ordre de souscription.
2. `POST /sni/api/v1/services/subscriptions/{subscriptionNumber}` avec `serviceCode` (obligatoire), `quantity` (1 par défaut, minimum 1), `unitPrice` (sinon le prix du catalogue), `metadataJson` (texte libre). Refusé si l'abonnement est `CANCELLED` ou `EXPIRED` (`409`), si le service est inactif (`400`), s'il est déjà souscrit et non terminé (`409`), ou si le KYC du titulaire est sous le niveau exigé (`400`).
3. `POST /sni/api/v1/services/subscribed/{serviceNumber}/suspend` (corps facultatif `reason`, non conservé) et `.../terminate`. Pour la domiciliation, préférer les routes du contrat : suspendre ou terminer le service ne change pas le statut du contrat.

## Règles et erreurs à refléter dans l'écran

Statuts HTTP : `BadRequestException` donne `400` (`BAD_REQUEST`), `ConflictException` donne `409` (`CONFLICT`), introuvable donne `404` (`RESOURCE_NOT_FOUND`). Les messages de `409` sont préfixés en anglais : `Could not process domiciliation: <motif en français>` (`src/main/java/com/sni/bokaticowork/core/exception/customs/ConflictException.java:15-17`). Pour afficher, retirer le préfixe jusqu'au premier `: `.

| Situation | Statut | Message (extrait) |
| --- | --- | --- |
| `billingCycle` hors `MONTHLY`/`QUARTERLY`/`YEARLY` | 400 | La domiciliation se facture au mois, au trimestre ou à l'année |
| `commitmentMonths < 1` | 400 | L'engagement est d'un mois au moins |
| Adresse inconnue / inactive | 404 / 400 | Adresse de domiciliation introuvable / Cette adresse n'est plus proposée |
| Adresse pleine (`occupants >= maxOccupants`) | 409 | cette adresse a atteint son nombre maximal de domiciliés |
| Contrat déjà en cours sur l'abonnement | 409 | un contrat est déjà en cours sur cet abonnement · DOM-... |
| Membre sans `legalName` | 400 | La dénomination du domicilié est requise |
| KYC sous le niveau 2 à l'ouverture (souscription du service) | 400 | `KYC level 2 required for this service « Domiciliation d entreprise »` (anglais) |
| Même adresse et même `suiteNumber` qu'un contrat en cours | 500 probable | non vérifié : violation de l'index unique, non traitée par `GlobalExceptionHandler`, donc message générique. Contrôler côté écran avant l'envoi. |
| Étape hors de son statut | 409 | cette étape n'est pas possible · le contrat est <STATUT> |
| KYC insuffisant au `prepare` | 409 | les pièces d'identité du représentant légal doivent être vérifiées (niveau 2)... |
| `signed` sans code / `confirm` sans référence ou exemplaire | 400 | Déposez l'exemplaire signé... / L'enregistrement se confirme avec la référence... |
| Démarche déjà déposée (`submit` rejoué) | 409 | cette démarche est déjà SUBMITTED |
| Rejet ou résiliation sans motif | 400 | Un rejet se motive... / La fin d'une domiciliation se motive |
| Attestation hors `ACTIVE` / `FISCAL` non éligible | 409 | voir parcours 4 |
| Contrat terminé (`commitment`, `terminate`) | 409 | ce contrat est terminé / ce contrat est déjà terminé |
| Courrier sur contrat non actif | 409 | ce contrat n'est pas actif · le courrier ne peut pas y être enregistré |
| Action sur un pli fermé | 409 | ce pli est déjà COLLECTED (ou autre) |
| Remise sans nom / sans pièce pour un pli sensible | 400 | Le nom de la personne qui retire le pli est requis / Un recommandé se remet contre pièce d'identité · indiquez sa référence |
| Réexpédition non prévue / sans adresse | 409 | ce contrat ne prévoit pas la réexpédition · mode HOLD / aucune adresse de réexpédition n'est enregistrée sur ce contrat |
| Destruction d'un pli sensible / sans motif | 409 / 400 | un recommandé ne se détruit pas · il se retourne à l'expéditeur / Une destruction se motive |
| Catalogue sans code, nom ou catégorie | 400 | Un service a un code, un nom et une catégorie |

Validation des corps (`400 VALIDATION_FAILED`, voir le [README racine](../README.md#erreurs)) : `@NotBlank` sur `subscriptionNumber`, `addressCode`, `signedDocumentCode`, `administrationReference`, `registeredDocumentCode`, `reason` (rejet, résiliation, destruction), `scanDocumentCode`, `serviceCode`, `label` d'adresse ; `@NotNull` sur `billingCycle`, `scope`, `mailType`, `serviceCategory`, `address`.

## À prévoir côté frontend

**Droits.** Toutes les routes `/sni/api/v1/services/**` exigent le droit `ADMIN:ACCESS` **et** un des rôles `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`, `ROLE_STAFF` (règle `@PreAuthorize` sur toute la classe, `DomiciliationController.java:35`). Il n'y a pas de droit plus fin : afficher le module entier, ou pas. Les routes du portail exigent un membre connecté avec l'accès portail ; l'accès à chaque contrat est ensuite filtré (gérant ou `canCollectMail`). Les téléversements passent par `DOCUMENT:UPLOAD` et la lecture des PDF côté back-office par `DOCUMENT:READ` (module `document`) : vérifier ces droits aussi.

**Écrans back-office.**
- Catalogue des services (liste, formulaire complet).
- Registre des adresses (liste avec taux d'occupation, création, modification).
- Liste des contrats avec filtre multi-statut ; fiche contrat avec une frise des étapes (`DRAFT` → `PENDING_SIGNATURE` → `PENDING_REGISTRATION` → `ACTIVE`), le bouton de l'étape suivante selon le statut, l'historique des démarches, l'attestation (portée, validité, `certificateInForce`, révocation et motif), la qualité fiscale (`fiscalAddressEligible`, dates d'octroi et de perte), le courrier du contrat.
- Tableau des enregistrements en cours.
- Guichet du courrier : file des plis à traiter, saisie de réception rapide (recherche de contrat par numéro ou par liste filtrée sur `ACTIVE`), fiche pli avec actions et journal.
- Services souscrits, dans la fiche de l'abonnement (module `subscription`).

**Boutons selon le statut du contrat.** `DRAFT`/`PENDING_DOCUMENTS` : Générer. `PENDING_SIGNATURE` : Télécharger le contrat, Déclarer signé. `PENDING_REGISTRATION` : Déposer (si démarche `PENDING`), Confirmer, Rejeter. `ACTIVE` : Attestation, Recevoir un pli. Tous les statuts sauf `TERMINATED`/`EXPIRED` : Changer l'engagement, Résilier.

**Boutons selon le pli.** Ouvert (`NOTIFIED`, `SCANNED`) : Numériser, Remettre, Réexpédier (si le mode du contrat le permet), Retourner, Détruire (masqué pour `REGISTERED_LETTER`, `ADMINISTRATIVE`, `LEGAL_NOTICE`). Le champ « pièce d'identité » devient obligatoire pour ces trois types. Afficher `storageDeadline` et signaler les plis dépassés (facturés chaque jour).

**Téléversements.** Enchaînement en deux temps : `POST /sni/api/v1/documents/upload` (multipart, 10 Mo au plus par requête, voir le [README racine](../README.md#corps-des-requêtes)), puis la route de l'étape avec le `code` reçu. Le serveur ne vérifie **pas** que le code existe ni son type : un code erroné est accepté et le PDF manquera plus tard. Types de document : `DOMICILIATION_CONTRACT_SIGNED`, `DOMICILIATION_REGISTERED_COPY`, `DOMICILIATION_REGISTRATION_RECEIPT`, `MAIL_SCAN`.

**Téléchargements.** Portail : routes `/document` et `/certificate` (PDF en octets). Back-office : `GET /sni/api/v1/documents/{code}/download` avec `contractDocumentCode`, `signedDocumentCode`, `certificateDocumentCode`, `scanDocumentCode`. Registre : CSV. Ces réponses sont binaires : les lire en blob et utiliser le nom de `Content-Disposition`.

**Listes de référence.** Statuts et énumérations (cycle, portée, mode de courrier, type de pli, catégorie de service) sont fixes et décrits dans les modèles de `routes/`. Les adresses viennent de `GET .../domiciliation/addresses`, les services de `GET /sni/api/v1/services/catalogue`.

**Champs à ne pas envoyer.** `fiscalAddressEligible` n'existe dans aucune requête : ne pas proposer de case à cocher. Pour une organisation, ne pas présenter `legalName` et suivants comme modifiables à l'ouverture : afficher la fiche de l'entreprise et renvoyer vers `company` pour la corriger.

**Pagination.** Les listes paginées sont des `PaginatedResponse` (`data` + `pageable`). `sort` est sans effet sur toutes les routes paginées du module : l'ordre est fixe.

**Temps réel.** Aucun sujet WebSocket : recharger la fiche après chaque action (chaque route renvoie l'objet à jour).

**Pièges.**
- `PUT /sni/api/v1/services/catalogue` remplace tous les champs : un champ omis repasse à sa valeur par défaut (`requiresContract=false`, `usageEntitlementCode=null`, `active` omis repasse à `true`). Toujours renvoyer la définition complète. Vider `usageEntitlementCode` ou `unitPrice` d'un `SVC-MAIL-*` arrête sa facturation sans message.
- Après un `409` sur `prepare`, le statut a changé (`PENDING_DOCUMENTS`) : recharger.
- Une `effectiveDate` future à la résiliation ne retarde rien : le contrat est `TERMINATED` tout de suite.
- `certificateValidUntil` est plafonnée à `endDate` : sur un contrat dont `endDate` est passée, l'attestation naît déjà échue (`certificateInForce=false`).

## Effets automatiques

| Quoi | Quand | Effet visible |
| --- | --- | --- |
| Relance des attestations | chaque jour à 07:20 (`bokati.domiciliation.reminder-cron`) | courriel `DOMICILIATION_CERTIFICATE_EXPIRING` pour chaque contrat `ACTIVE` dont l'attestation non révoquée expire dans les 30 jours (`renewal-reminder-days`). Non vérifié : un envoi par jour tant que la date est dans la fenêtre, la file d'événements ne semble pas dédoublonner. |
| Garde prolongée du courrier | chaque jour à 07:30 (`mail-cron`) | pour chaque pli ouvert au-delà de `storageDeadline` : facturation `SVC-MAIL-STORAGE` par jour de dépassement, `storageDeadline` repoussée à aujourd'hui, événement `STORAGE_CHASED`, courriel `MAIL_STORAGE_OVERDUE`. `billable` et `billedAmount` du pli augmentent. |
| Expiration des contrats | chaque jour à 07:40 (`expiry-cron`) | contrat `ACTIVE` à `endDate` passée et abonnement sans reconduction automatique : `EXPIRED`, attestation révoquée (« Contrat arrivé à terme »). Pas de courriel, service souscrit inchangé. |
| Report de la fiche d'entreprise | à chaque modification de la fiche juridique (événement `CompanyLegalCardChangedEvent`) | dénomination, forme, RCCM, NIU, représentant mis à jour sur les contrats non terminés de l'organisation. |
| Courriels | via la file d'événements (traitée toutes les 10 s) | `DOMICILIATION_ACTIVATED`, `DOMICILIATION_REGISTRATION_REJECTED`, `DOMICILIATION_CERTIFICATE_ISSUED`, `DOMICILIATION_FISCAL_LOST`, `DOMICILIATION_TERMINATED`, `MAIL_RECEIVED`, `MAIL_SCANNED`, `MAIL_FORWARDED`, `MAIL_STORAGE_OVERDUE`, adressés à l'adresse du titulaire (aucun envoi si elle est vide). `DOMICILIATION_FISCAL_LOST_ADMIN` et `DOMICILIATION_TERMINATED_ADMIN` partent vers l'administration fiscale seulement si `bokati.domiciliation.notify-administration-on-termination=true` et `administration-email` renseigné (vide par défaut). |
| Facturation des actes | numérisation, réexpédition, garde prolongée | ligne d'usage créée au module d'usage (code de droit `MAIL_SCAN`, `MAIL_FORWARD`, `MAIL_STORAGE`), `usageNumber` sur le pli. Aucune facturation si le service du catalogue n'a ni prix ni code de droit (seulement une trace dans les journaux du serveur). |

Réglages (`application*.yml`, préfixe `bokati.domiciliation`) : `kyc-level-required` (2), `certificate-validity-days` (365), `retention-years` (10), `mail.storage-days` (30), `renewal-reminder-days` (30), les trois `*-cron`, `notify-administration-on-termination`, `administration-email`.

## Écarts relevés

1. **Ouverture bloquée par le KYC avant l'étape prévue pour le gérer.** `open` souscrit `SVC-DOMICILIATION` (`DomiciliationService.java:137`), et la souscription exige déjà le KYC niveau 2 (`SubscribedServiceService.java:113`, service livré avec `requires_kyc_level = 2`, `V239__services_and_domiciliation.sql:45`). Quand le service n'est pas déjà souscrit, un titulaire non vérifié est donc refusé dès l'ouverture, en `400` avec un message anglais (`SubscriberKycLevelGuard.java:46`), et le statut `PENDING_DOCUMENTS` prévu par `prepare` n'est atteint que si le KYC baisse entre l'ouverture et la génération.
2. **Réexpédition impossible.** Aucune route ni aucun code ne renseigne `forwardingAddress` (ni `forwardingFrequency`) d'un contrat, et `mailForwardingMode` ne se change plus après l'ouverture. `forward` renvoie donc toujours `409` (`MailItemService.java:184`).
3. **Statut `SUSPENDED` du contrat jamais atteint** (`DomiciliationContract.java:36`) ; la suspension du service souscrit (`SubscribedServiceService.java:150`) n'a pas de route de reprise et ne touche pas le contrat.
4. **Résiliation immédiate malgré une date d'effet future** : statut `TERMINATED`, service terminé et attestation révoquée le jour même (`DomiciliationService.java:522-534`) ; le courrier est alors refusé pendant le préavis.
5. **Contrats reconduits jamais prolongés.** Avec `autoRenew`, le worker n'expire pas le contrat mais rien ne repousse `endDate` (seuls `DomiciliationService.java:366`, `:467`, `:523` l'écrivent). Une attestation délivrée ensuite est plafonnée à une `endDate` passée et naît échue (`DomiciliationService.java:428-431`).
6. **Codes de document non contrôlés** : `signedDocumentCode`, `registeredDocumentCode`, `receiptDocumentCode`, `scanDocumentCode` sont acceptés sans vérifier leur existence ni leur type (`DomiciliationService.java:295-298`, `:351-360`, `MailItemService.java:122-125`).
7. **Pli marqué `NOTIFIED` même sans courriel** : `notifyHolder` ne publie rien si le titulaire n'a pas d'adresse, mais le statut, `notifiedAt` et `notificationChannel = EMAIL` sont posés quand même (`MailItemService.java:108-114`, `:312-314`).
8. **Numérisation invisible côté portail** : `scanDocumentCode` est exposé dans `MailItemView` mais aucune route `/client/...` ne permet de télécharger la numérisation (`ClientDomiciliationController.java`). À l'inverse, `collectorIdDocument` (référence de pièce d'identité du porteur) est exposé au portail.
9. **Messages en anglais** : préfixe `Could not process ...:` de tous les `409` (`ConflictException.java:16`) ; message KYC (`SubscriberKycLevelGuard.java:46`) ; `404` « No customer linked to this member account » / « No authenticated member session » du contexte portail (`features/portal/context/ClientContextService.java`).
10. **Violation de l'index unique adresse + complément non traitée** : `open` ne vérifie pas `uk_domiciliation_suite_active` (`V239__services_and_domiciliation.sql:201`) ; l'erreur base remonte probablement en `500` (non vérifié en exécution).
11. **Suspension du service : motif ignoré.** `reason` est reçu mais jamais conservé (`DomiciliationController.java:72`, `SubscribedServiceService.suspend`).
12. **Refacturation des droits jamais produite** : `rebilled` est stocké mais `rebilledDocumentCode` n'est écrit nulle part (`DomiciliationRegistration.java`), aucune facture n'est créée.
13. **Expiration silencieuse** : le worker passe le contrat en `EXPIRED` sans courriel et sans terminer le service souscrit (`DomiciliationWorker.java:86-105`).
14. **Paramètre `sort` accepté mais ignoré** sur les cinq routes paginées (`DomiciliationController.java:263`, `ClientDomiciliationController.java:102`).
