# portal : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module `portal` est l'espace du **membre** (le client final du coworking), servi sous `/sni/api/v1/client/**` et, pour le KYC seulement, sous l'alias `/sni/api/v1/portal/kyc/**`. Il ne porte presque aucune règle métier propre : chaque route retrouve le membre connecté, vérifie que l'objet demandé est bien à lui, puis appelle le service du module métier (`subscription`, `booking`, `billing`, `payment`, `contract`, `document`, `company`, `ressource`, `notification`). Ce README décrit ce qui est propre au portail : l'accès, la barrière KYC, le périmètre des données, les écarts avec les routes du back-office. Le fonctionnement de fond de chaque domaine est dans le README du module concerné, auquel les fichiers de parcours renvoient.

Code : `src/main/java/com/sni/bokaticowork/features/portal/`.

## Table des matières

| Sujet | Fichier |
| --- | --- |
| Accès au portail, barrière KYC, périmètre des données | ce fichier, sections [Accès au portail](#accès-au-portail) et [Périmètre des données](#périmètre-des-données) |
| Accueil, profil, sessions, notifications, dossier KYC | [parcours-profil-kyc.md](parcours-profil-kyc.md) |
| Catalogue, souscription, paiement, renouvellement, changement de plan, gel, résiliation, prélèvement, devis, historique, pass et droits | [parcours-abonnements-pass.md](parcours-abonnements-pass.md) |
| Ressources, disponibilités, réservations | [parcours-reservations.md](parcours-reservations.md) |
| Factures, paiements (mobile money, portefeuille, espèces), reçus, état de compte | [parcours-facturation-paiements.md](parcours-facturation-paiements.md) |
| Portefeuille personnel : code secret, recharge, transferts, bénéficiaires, demandes de paiement, verrou, appareils, relevés | [parcours-portefeuille.md](parcours-portefeuille.md) |
| Contrats à lire et signer | [parcours-contrats.md](parcours-contrats.md) |
| Organisations : gérant (membres, factures, passes, abonnement partagé, portefeuille) et collaborateur | [parcours-organisation.md](parcours-organisation.md) |

### Routes membre servies par d'autres modules

Ces routes sont aussi sous `/sni/api/v1/client/**` (même contrôle d'accès, même barrière KYC), mais leur code et leur documentation sont ailleurs :

| Domaine | Chemins | Documentation |
| --- | --- | --- |
| Support (tickets, base de connaissances) | `/sni/api/v1/client/support/**` | [../support/README.md](../support/README.md) |
| Domiciliation | `/sni/api/v1/client/domiciliation/**`, `/sni/api/v1/client/companies/{customerId}/domiciliation` | [../domiciliation/README.md](../domiciliation/README.md) |
| Marketplace | `/sni/api/v1/client/marketplace/**`, `/sni/api/v1/client/companies/{customerId}/marketplace/orders` | [../marketplace/README.md](../marketplace/README.md) |
| Panier, coupons, prix du catalogue | `/sni/api/v1/client/cart/**`, `/sni/api/v1/client/catalogue/prices`, `/sni/api/v1/client/promotions/available` | [../subscription/README.md](../subscription/README.md) |
| Contrats d'une organisation (gérant) | `/sni/api/v1/client/companies/{customerId}/contracts/**` | [../company/README.md](../company/README.md) |

## Notions

| Objet | Identifiant exposé | Remarques |
| --- | --- | --- |
| Membre connecté | `memberId` (code `MBR...`) | Jamais passé dans un chemin : le serveur le déduit du jeton (`ClientContextService.getAuthenticatedMember`, `src/main/java/com/sni/bokaticowork/features/portal/context/ClientContextService.java:29`). |
| Dossier KYC | `caseCode` | Un seul dossier courant par membre (le plus récent). Créé à la création du membre. |
| Pièce KYC | `id` (numéro court, `short_id`) | Petit entier, sans risque d'arrondi en pratique, mais typé `int64 ⚠` dans les modèles. |
| Abonnement | `subscriptionNumber` | Seuls ceux dont l'abonné est le membre (`subscriberType = MEMBER`). Ceux d'une organisation passent par `/client/companies/{customerId}/...`. |
| Pass | `passNumber` | Seuls ceux dont le titulaire est le membre. |
| Devis d'abonnement | `quoteNumber` | |
| Résiliation | `terminationCode` | |
| Mandat de prélèvement | `mandateCode` | |
| Réservation | `bookingNumber` | |
| Ressource | `code` | Seules les ressources visibles au portail, actives et réservables. |
| Facture | `documentNumber` | Jamais les brouillons (`DRAFT`). |
| Paiement | `intentNumber`, `transactionNumber` (reçu) | |
| Dépôt mobile money | `depositId` | Rendu par toute route `pay/mobile-money` ou `top-ups`. |
| Annonce d'espèces | `declarationNumber` | |
| Portefeuille | `walletNumber` | |
| Transfert, demande de paiement | `transferNumber`, `requestNumber` | |
| Bénéficiaire, appareil du portefeuille | `id` | **Grand entier généré** (`@IdGeneration`) : arrondi par JavaScript, voir [../README.md#identifiants--attention-aux-grands-nombres](../README.md#identifiants--attention-aux-grands-nombres). |
| Session (jeton de rafraîchissement) | `id` | **Grand entier généré** : même précaution. |
| Contrat, avenant | `contractCode`, `amendmentCode` | |
| Notification | `notificationNumber` | |
| Organisation | `customerId` (code client de type `COMPANY`) | Le membre y a un rôle `OWNER` (gérant) ou `EMPLOYEE`. |
| Rattachement membre et organisation | `id` / `linkId` (numéro court) | |
| Place d'abonnement partagé, accès à un pass, autorisation de dépense, dépense | `seatId`, `accessId`, `authorizationId`, `spendId` (numéros courts) | |

## Accès au portail

Trois contrôles s'enchaînent sur chaque appel `/sni/api/v1/client/**`.

1. **Jeton** : un jeton d'accès valide (voir [../README.md#connexion-et-jetons](../README.md#connexion-et-jetons)). Exception : `GET /sni/api/v1/client/catalog/plans` et `GET /sni/api/v1/client/catalog/plans/{planCode}` sont publics (`src/main/java/com/sni/bokaticowork/security/config/PublicPaths.java:70`) ; le filtre JWT les ignore, un jeton envoyé n'y change rien.
2. **Accès portail** (`ClientPortalAuthorizationManager`, `src/main/java/com/sni/bokaticowork/security/authorization/ClientPortalAuthorizationManager.java:28`) : le compte doit porter `ROLE_MEMBER`, être rattaché à un membre non supprimé, et ce membre doit avoir `portalAccess = true`. Sinon `403` au format de la couche de sécurité (`{ "status": 403, "error": "Forbidden", ... }`). Le même contrôle protège `/sni/api/v1/portal/kyc/**`.
3. **Barrière d'accueil** (`ClientOnboardingGuard`, `src/main/java/com/sni/bokaticowork/features/portal/guard/ClientOnboardingGuard.java:39`) : intercepteur posé sur `/sni/api/v1/client/**`, **sauf** `/sni/api/v1/client/documents/kyc/**` et toute adresse finissant par `/me/onboarding-status`. L'alias `/sni/api/v1/portal/kyc/**` n'est pas sous `/client`, il n'est donc pas intercepté non plus. Règles :
   - compte utilisateur verrouillé (`isAccountLocked`) : `403`, `errorCode = "EMAIL_NOT_VERIFIED"` ;
   - dossier KYC `APPROVED` : passe ;
   - sinon, période de grâce KYC en cours (`kycGracePeriodEndAt` dans le futur) : passe ;
   - sinon : `403`, `errorCode = "KYC_REQUIRED"`, `message` en français qui nomme l'étape (dossier non commencé, en cours d'examen, pièces refusées, échu) et `errors` = noms des pièces refusées.

La barrière bloque **tout** le reste du portail, profil, sessions, notifications et support compris, pas seulement les quatre actions listées dans `restrictedActions`. Le corps du `403 KYC_REQUIRED` a le format des erreurs métier (`errorCode`, `status`, `message`, `errors`), pas celui de la couche de sécurité.

**Qui ouvre l'accès** (vérifié dans `src/main/java/com/sni/bokaticowork/features/client/member/service/implementation/MemberServiceImpl.java`) : la création d'un membre met `portalAccess = true`, le statut `PENDING` et crée son dossier KYC (lignes 140 à 148). L'activation par le back-office (statut `ACTIVE` ou activation de l'accès portail) donne le rôle `MEMBER` au compte, l'active, marque l'adresse vérifiée, pose `portalActivatedAt` et ouvre la période de grâce KYC de `app.portal.kyc-grace-period-days` jours (7 par défaut) (lignes 241 à 256 et 393 à 409). Le back-office peut changer cette durée (`setKycGracePeriodDays`, ligne 413). Un membre dont la période de grâce n'a jamais été posée (`kycGracePeriodEndAt` nul) est bloqué jusqu'à l'approbation de son KYC. Désactivation (statut `INACTIVE` ou `SUSPENDED`, ou désactivation de l'accès) : `portalAccess = false`, donc `403` sur tout le portail. Détail côté back-office : [../client/README.md](../client/README.md).

Non vérifié : comment un compte créé par une inscription publique reçoit `ROLE_MEMBER` avant l'activation par le back-office.

## Périmètre des données

- Le membre n'envoie jamais son propre code : chaque service lit le membre depuis le jeton.
- Chaque lecture ou action sur un objet désigné par son code vérifie qu'il appartient au membre (`subscriberType`/`ownerType`/`customerType = MEMBER` et code égal au `memberId`). Un objet qui n'est pas le sien répond **`404`** (`Subscription not found`, `Invoice not found`, `Portefeuille introuvable`...), jamais `403` : le serveur ne confirme pas l'existence des objets des autres. L'écran traite donc un `404` comme « introuvable ou pas à vous ».
- Les listes sont filtrées côté serveur par le membre : aucun paramètre ne permet de lister les objets d'un autre.
- Ce qui appartient à une **organisation** n'apparaît pas dans les routes personnelles. Le gérant (`OWNER`) le voit sous `/sni/api/v1/client/companies/{customerId}/...` ; pour un membre qui n'est pas gérant actif de cette organisation, elle répond `404` (`CompanyMembershipService.ownedCompany`, `src/main/java/com/sni/bokaticowork/features/company/membership/service/CompanyMembershipService.java:295`). Le collaborateur (`EMPLOYEE`) n'a que les routes `/sni/api/v1/client/me/company-*` et `/sni/api/v1/client/me/entitlements`.
- Les factures en brouillon (`DRAFT`) n'existent pas pour le membre : absentes des listes, `404` en lecture.
- Les ressources non visibles au portail (ou inactives, ou non réservables) répondent `404`, y compris quand on tente de les réserver par leur code.

### Ce qui diffère des routes du back-office

| Sujet | Portail | Back-office |
| --- | --- | --- |
| Droits | Aucun droit `MODULE:ACTION` : seul l'accès portail compte. Pas de bouton à masquer selon `roles`. | Rôle d'administration et droit par route. |
| Identité du payeur | Déduite du jeton, jamais dans le corps. | Envoyée dans le corps (`subscriberType`, `subscriberCode`...). |
| Réservation | Créneaux du soir réservés aux abonnés ; ressource visible au portail exigée ; tarif déduit de la durée (pas d'unité imposée). | L'accueil réserve pour n'importe qui, unité au choix. |
| Gel d'abonnement | Immédiat seulement, refusé si la politique impose un préavis. | Gel planifié possible. |
| Changement de plan | Plans du catalogue seulement ; la confirmation du membre vaut approbation. | Plans privés, dérogations, visa. |
| Résiliation | Canal `PORTAL`, sans dispense de frais. | Dispenses et décisions humaines. |
| Paiement espèces | Une **annonce** (`AWAITING_CONFIRMATION`), la facture reste due jusqu'à l'encaissement en caisse. | Encaissement réel. |
| Paiement portefeuille | Le portefeuille du membre est trouvé ou créé d'office dans la devise de la facture ; code secret exigé seulement si la politique le demande. | Portefeuille choisi. |
| Messages | Souvent en français pour les règles récentes, en anglais pour les plus anciennes (voir [Écarts relevés](#écarts-relevés)). | |

## Statuts et cycle de vie

Les statuts des objets métier (abonnement, pass, réservation, facture, contrat, transfert...) sont ceux des modules métier : voir les README cités dans chaque fichier de parcours. Deux cycles sont propres au portail.

### Statut d'accueil

Route : `GET /sni/api/v1/client/me/onboarding-status`.

Calculé à la volée par `ClientOnboardingService` (`src/main/java/com/sni/bokaticowork/features/portal/profile/service/ClientOnboardingService.java`), à partir du statut du dossier KYC le plus récent :

| `kycStatus` (portail) | Statuts KYC d'origine | `kycCompletionPercent` | `nextStep` (si l'adresse est vérifiée) |
| --- | --- | --- | --- |
| `NOT_STARTED` | aucun dossier, `NOT_STARTED` | 0 | `SUBMIT_KYC` |
| `DRAFT` | `IN_PROGRESS` | 30 | `COMPLETE_KYC` |
| `SUBMITTED` | `SUBMITTED`, `UNDER_REVIEW` | 70 | `AWAIT_KYC_REVIEW` |
| `APPROVED` | `APPROVED` | 100 | `COMPLETED` |
| `REJECTED` | `REJECTED`, `PENDING_CORRECTION`, `RENEWAL_REQUIRED`, `EXPIRED` | 40 | `CORRECT_KYC` |

- `nextStep = VERIFY_EMAIL` passe avant tout si `emailVerified` est faux.
- `restrictedActions` vaut `["BOOKING","PAYMENT","CONTRACT","SUBSCRIPTION"]` si l'adresse n'est pas vérifiée, ou si le KYC n'est pas approuvé et la période de grâce terminée ; sinon `[]`. C'est une indication : la barrière réelle est celle décrite plus haut.
- `portalFullyActive` = adresse vérifiée et KYC approuvé.
- `gracePeriodEndsAt`, `gracePeriodActive`, `gracePeriodDaysRemaining` décrivent la période de grâce.

### Dossier KYC vu du membre

| Statut (`status` de `my-case`) | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `NOT_STARTED` | Dossier créé, rien de déposé | Création du membre | `IN_PROGRESS` au premier dépôt |
| `IN_PROGRESS` | Pièces en cours de dépôt | Dépôt d'une pièce, membre au statut `PENDING` | `UNDER_REVIEW` automatiquement quand toutes les pièces exigées sont présentes, ou `SUBMITTED` par `POST .../submit` |
| `SUBMITTED`, `UNDER_REVIEW` | Examen par l'équipe ; dépôts fermés | `POST .../submit`, ou recalcul automatique au dépôt de la dernière pièce exigée | `APPROVED`, `PENDING_CORRECTION` (décision du back-office) |
| `PENDING_CORRECTION`, `REJECTED` | Pièce(s) refusée(s), à redéposer | Revue du back-office | `IN_PROGRESS`/`UNDER_REVIEW` après redépôt, `SUBMITTED` par `submit` |
| `APPROVED` | Vérifié : barrière levée, portefeuille XAF créé | Toutes les pièces exigées vérifiées, ou aucune pièce exigée | `RENEWAL_REQUIRED`, `EXPIRED` |
| `RENEWAL_REQUIRED`, `EXPIRED` | Pièces échues | Échéance (module `document`) | Nouveau dépôt |

Le recalcul automatique est dans `KycAutomationServiceImpl.recomputeCaseState` (`src/main/java/com/sni/bokaticowork/features/document/kyc/service/implementation/KycAutomationServiceImpl.java:263`). Détail du KYC côté back-office : [../document/README.md](../document/README.md).

## Parcours

Les parcours détaillés sont dans les fichiers listés dans la [table des matières](#table-des-matières). Enchaînement type d'une première connexion :

1. `POST /sni/api/v1/auth/login`, puis `GET /sni/api/v1/client/me/onboarding-status` (toujours accessible).
2. Si `kycStatus` n'est pas `APPROVED` et `gracePeriodActive` est faux : n'afficher que l'écran KYC ([parcours-profil-kyc.md](parcours-profil-kyc.md#déposer-son-dossier-kyc)) ; tout autre appel `/client/**` renverra `403 KYC_REQUIRED`.
3. Sinon : tableau de bord (`GET /sni/api/v1/client/me`, `GET /sni/api/v1/client/notifications/unread-count`, `GET /sni/api/v1/client/subscriptions/current`, `GET /sni/api/v1/client/billing/summary`, `GET /sni/api/v1/client/companies`), avec un bandeau tant que `portalFullyActive` est faux.

## Règles et erreurs à refléter dans l'écran

| Cas | Statut | `errorCode` / message | À faire |
| --- | --- | --- | --- |
| Compte sans accès portail, sans `ROLE_MEMBER`, ou membre supprimé | `403` | format sécurité, `Forbidden` | Écran « accès au portail non ouvert, contactez l'accueil ». |
| KYC non approuvé, grâce terminée | `403` | `KYC_REQUIRED`, message en français, `errors` = pièces refusées | Rediriger vers l'écran KYC et afficher `message` tel quel. |
| Compte verrouillé | `403` | `EMAIL_NOT_VERIFIED`, `Please verify your email before accessing this resource.` | Le code ne correspond pas à la cause réelle (voir écarts) : afficher « compte verrouillé, contactez l'accueil ». |
| Objet d'un autre membre ou inexistant | `404` | `RESOURCE_NOT_FOUND` | « Introuvable ». |
| Organisation dont le membre n'est pas gérant actif | `404` | `Organisation introuvable` | Ne pas proposer les écrans de gérant aux `EMPLOYEE` (voir `role` dans `GET /sni/api/v1/client/companies`). |
| Membre sans dossier KYC | `404` | `No KYC case found. Please contact support to initialize your KYC dossier.` | Message d'aide, contact accueil. |
| Paiement par portefeuille verrouillé | `409` | `votre portefeuille est verrouillé · déverrouillez-le pour payer` | Proposer le déverrouillage. |
| Paiement par portefeuille, code exigé et absent | `400` | `Ce paiement exige votre code secret` | Redemander avec `{ "pin": "..." }`. |

Règles propres à chaque domaine : dans les fichiers de parcours.

## À prévoir côté frontend

- **Écrans** : accueil et barrière KYC, profil et sécurité (mot de passe, sessions), notifications, KYC, catalogue et souscription, mes abonnements (détail, droits, échéancier, historique, gel, changement de plan, résiliation, prélèvement), devis, pass, ressources et réservation, factures et paiements, annonces d'espèces, portefeuille, contrats, organisations (vue gérant et vue collaborateur), plus les écrans des modules support, domiciliation, marketplace.
- **Droits** : aucun droit à tester. Les seules conditions d'affichage sont l'état d'accueil (`onboarding-status`), le rôle dans chaque organisation (`OWNER`/`EMPLOYEE`) et les indicateurs des objets (`canSign`, `canDownloadPdf`, `canFreezeNow`, `hasMandate`...).
- **Listes de référence** : types de ressources (`GET /sni/api/v1/client/resource-types`), opérateurs mobile money (valeurs de `CongoCorrespondent` : `AIRTEL_COG`, `MTN_MOMO_COG`, `ORANGE_COD`, `AIRTEL_COD`, `VODACOM_MPESA_COD`, etc. ; les alias `AIRTEL_OAPI_COG`, `AIRTEL_OAPI_COD`, `MPESA_COD` sont gardés pour compatibilité et ne doivent pas être proposés), configuration de dépôt par type de pièce (`GET /sni/api/v1/document-types/{code}/upload-config`, ouverte à tout compte connecté).
- **Fichiers téléchargés** (octets, nom dans `Content-Disposition`) : PDF de facture, de reçu, d'état de compte, de contrat et d'avenant, relevé et reçu de portefeuille, pièce KYC. Les télécharger avec le jeton (pas de lien direct `<a href>` sans en-tête).
- **Fichiers envoyés** : pièces KYC en `multipart/form-data` (partie `file`, 10 Mo au plus).
- **Grands identifiants** : sessions (`id`), bénéficiaires et appareils du portefeuille (`id`) sont des entiers générés qui dépassent `2^53` : lecture JSON sans perte obligatoire avant d'appeler `DELETE /sni/api/v1/client/sessions/{sessionId}`, `DELETE .../beneficiaries/{id}`, `PATCH .../devices/{id}`.
- **Idempotence** : aucune route du portail ne lit l'en-tête `Idempotency-Key`. La création de réservation et l'achat de pass acceptent un champ `idempotencyKey` **dans le corps** ; les routes `pay/*`, `top-ups`, `transfers` n'ont aucune protection contre le double envoi : désactiver le bouton pendant l'appel.
- **Temps réel** : les notifications du membre sont poussées sur `/user/queue/notifications` et le compteur sur `/user/queue/notifications/unread-count` (voir [parcours-profil-kyc.md](parcours-profil-kyc.md#notifications)).
- **Dates** : les corps de réservation (`startedAt`, `endedAt`) sont des dates-heures **locales sans fuseau** (`LocalDateTime`), lues dans le fuseau de l'application.

## Effets automatiques

- **Recalcul du dossier KYC** à chaque dépôt et à chaque revue : le dossier passe seul en `UNDER_REVIEW` quand toutes les pièces exigées sont présentes, en `PENDING_CORRECTION` dès qu'une pièce est refusée, en `APPROVED` quand toutes sont vérifiées ; le statut du membre suit (`PENDING`, `UNDER_REVIEW`, `PENDING_CORRECTION`, `ACTIVE`). L'approbation crée le portefeuille XAF du membre (`KycServiceImpl.provisionDefaultWallet`, `src/main/java/com/sni/bokaticowork/features/document/kyc/service/implementation/KycServiceImpl.java:925`).
- **Notifications internes** (canal `IN_APP`) créées par les modules métier : réservation, abonnement, contrat, facture émise, relances, dépenses du portefeuille d'organisation, marketplace. Voir [parcours-profil-kyc.md](parcours-profil-kyc.md#notifications).
- **Workers du module `payment`** qui changent ce que voit le membre : suivi des dépôts mobile money (toutes les 60 s par défaut, `MobileMoneyStatusPollingWorker`), expiration des annonces d'espèces (toutes les 15 min, durée de validité 4 h par défaut, `CashDeclarationExpiryWorker`, libère le créneau d'une réservation), expiration des confirmations de portefeuille (toutes les 5 min). Détail : [../payment/README.md](../payment/README.md).
- **Workers du module `subscription`** : avis de fin et renouvellements, application des changements de plan planifiés, fin de gel. Détail : [../subscription/README.md](../subscription/README.md).
- Le module `portal` lui-même n'a aucun `@Scheduled`.

## Écarts relevés

1. **Code d'erreur trompeur** : un compte verrouillé reçoit `403 EMAIL_NOT_VERIFIED` « Please verify your email... » alors que la condition testée est `isAccountLocked` (`src/main/java/com/sni/bokaticowork/features/portal/guard/ClientOnboardingGuard.java:57-58`).
2. **Barrière plus large qu'annoncée** : `restrictedActions` liste quatre actions, mais la barrière bloque tout `/client/**` hors KYC, y compris profil, sessions, notifications et **support** (`/client/support/**`) : un membre bloqué ne peut pas ouvrir de ticket alors que les messages l'y invitent (`ClientOnboardingGuard.java`, `src/main/java/com/sni/bokaticowork/features/portal/config/PortalWebMvcConfigurer.java:19-21`). Inversement, `restrictedActions` cite l'adresse non vérifiée, que la barrière ne teste pas.
3. **Étapes jamais atteintes** : `nextStep = SUBMIT_KYC_BEFORE_GRACE_EXPIRES` et `CONTACT_SUPPORT` ne sont jamais rendus, tous les statuts étant traités avant (`src/main/java/com/sni/bokaticowork/features/portal/profile/service/ClientOnboardingService.java:98-99`). `profileComplete` vaut toujours `true` (ligne 49).
4. **Champ jamais rempli** : `documentTypeName` des pièces KYC (`ClientKycDocumentResponse`) n'est jamais renseigné par `toDocumentResponse` (`src/main/java/com/sni/bokaticowork/features/portal/document/kyc/service/ClientKycService.java:308`).
5. **Soumission KYC refusée après dépôt complet** : le dépôt de la dernière pièce exigée fait passer le dossier en `UNDER_REVIEW` automatiquement ; `POST .../submit` répond alors `400` « Your KYC case is not in a submittable state » (`ClientKycService.java:249-253`).
6. **Signature de contrat** : la signature du membre marque tout le contrat `SIGNED` (ou `ACTIVE`) même si des signataires d'ordre supérieur n'ont pas signé (`src/main/java/com/sni/bokaticowork/features/portal/document/contract/service/ClientContractService.java:110`) ; l'adresse IP enregistrée est `getRemoteAddr()`, celle du proxy derrière un relais (`ClientContractController.java:77`).
7. **Pagination faussée des factures** : les brouillons sont retirés après la pagination ; une page peut contenir moins d'éléments que `size` et `totalElements` les compte (`src/main/java/com/sni/bokaticowork/features/portal/billing/service/ClientBillingService.java:70`).
8. **Résiliation sans règles** : `DELETE /sni/api/v1/client/subscriptions/{subscriptionNumber}` pose seulement `cancelAtPeriodEnd = true`, sans préavis, frais d'engagement ni dossier de résiliation, alors que `POST .../termination` applique la politique (`src/main/java/com/sni/bokaticowork/features/portal/subscription/service/ClientSubscriptionService.java:119-127`).
9. **Données techniques exposées** : les routes de suivi mobile money renvoient au membre `requestPayloadJson`, `providerResponseJson` et `callbackUrl` du dépôt (`MobileMoneyDepositResponse`, `src/main/java/com/sni/bokaticowork/features/payment/dto/response/MobileMoneyDepositResponse.java`).
10. **Suivi impossible de la recharge d'organisation** : `GET /client/companies/{customerId}/mobile-money/deposits/{depositId}` n'accepte que les dépôts dont l'intention a la source `BILLING_DOCUMENT` ; une recharge du portefeuille d'organisation (source `WALLET_TOPUP`) y répond `404` (`src/main/java/com/sni/bokaticowork/features/portal/company/service/ClientCompanyBillingService.java:101`).
11. **Code secret selon la route** : payer une facture par portefeuille évalue la politique pour `MERCHANT_PAYMENT` (`ClientBillingService.java:176`), payer un abonnement ou un pass pour `BILL_PAYMENT` (`ClientSubscriptionService.java:185`, `:276`) : la même facture peut exiger le code par une route et pas par l'autre.
12. **Notifications** : messages sans accents (« Votre reservation ... a ete confirmee ») et `message` vide pour tout type non prévu (relances, dépenses d'organisation, marketplace) (`src/main/java/com/sni/bokaticowork/features/portal/notification/service/ClientNotificationFormatter.java:37-89`). La liste ne rend que les non lues.
13. **Filtres ignorés** : un `type` ou `group` inconnu dans `GET /client/resources` est ignoré en silence et la liste complète revient (`src/main/java/com/sni/bokaticowork/features/portal/resource/service/ClientResourceService.java:66`, `:73`).
14. **Champ toujours nul** : `requiredKycLevel` du détail de plan (`src/main/java/com/sni/bokaticowork/features/portal/subscription/service/ClientCatalogService.java:181`).
15. **DTO sans route** : `ChangeEmailRequest`, `ChangeEmailConfirmRequest`, `UpdateAvatarRequest`, `ChangePasswordRequest`, `UpdateClientProfileRequest`, `ClientCustomerResponse` existent dans `features/portal/profile/dto/` sans aucune route : le membre ne peut changer ni son adresse ni son avatar depuis le portail.
16. **Messages en anglais** : nombreux refus anciens (« Only CONFIRMED, PENDING_APPROVAL or PENDING_PAYMENT bookings can be cancelled », « Front side must be uploaded before the back side », « Only PENDING documents can be deleted », « Subscription not found »...) à traduire côté écran si besoin (`ClientPortalBookingService.java:130`, `ClientKycService.java:177`, `:229`, `:242`).
17. **Pas d'idempotence** sur les paiements du portail (`pay/mobile-money`, `pay/wallet`, `pay/cash`, `top-ups`) : un double clic crée deux intentions ou deux dépôts.
18. **Devis en brouillon visibles et acceptables** : `GET /sni/api/v1/client/quotes` rend aussi les devis `DRAFT` pas encore envoyés, et `accept` les accepte (`open()` inclut `DRAFT`) (`src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/model/SubscriptionQuote.java:35`, `ClientSubscriptionSelfService.quotes`).
19. **Défi `OTP` jamais reçu** : le lancement d'un transfert annonce `challenge` `OTP` ou `PIN_AND_OTP`, mais la confirmation ne reçoit qu'un `pin` et ne vérifie que le code secret (`src/main/java/com/sni/bokaticowork/features/payment/security/service/WalletConfirmationService.java:145`).
20. **Niveau KYC du plan masqué** : la souscription applique le niveau KYC exigé par le plan (`SubscriptionCreationOperator`, `SubscriberKycLevelGuard`) alors que le détail de plan du portail le rend toujours `null` (écart 14) : l'écran ne peut pas prévenir le refus.
