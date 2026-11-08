# client : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Le module tient les deux fiches de base de toute la plateforme : le **client** (`Customer`, celui qui est facturé : une personne ou une organisation) et le **membre** (`Member`, la personne qui utilise les espaces et se connecte au portail). Toutes ses routes sont **back-office** (`CLIENT:READ`, `CLIENT:CREATE`, `CLIENT:UPDATE`, `CLIENT:DELETE`) : il sert aux écrans de gestion des clients et des membres, à la création d'un membre par l'équipe et à l'ouverture ou la fermeture de son accès au portail.
Il travaille avec `securite` (chaque membre a un compte utilisateur et le rôle `ROLE_MEMBER`), `document` (dossier KYC créé et synchronisé à chaque changement de statut), `company` (rattachement d'un membre à une organisation, voir [../company/README.md](../company/README.md)) et `portal` (le portail membre lit `portalAccess` et le délai KYC fixés ici).

## Notions

| Objet | Identifiant exposé | Forme | Liens |
| --- | --- | --- | --- |
| Client (`Customer`) | `customerId` (code lisible) | `CUS-<type>-<AAAAMM>-<n>` composé par `CodeComposer.withMonth("CUS", abrégé du type, …)` (`src/main/java/com/sni/bokaticowork/features/client/customer/service/implementation/CustomerServiceImpl.java:104`) | 0 à n membres ; profil légal, conditions de paiement, blocage : gérés par `company` |
| Membre (`Member`) | `memberId` (code lisible) | `MBR-<AAAAMM>-<n>` (`src/main/java/com/sni/bokaticowork/features/client/member/service/implementation/MemberServiceImpl.java:139`) | exactement un compte utilisateur (`userId`, code lisible du compte) ; un client « payeur par défaut » (`customerId`) |
| Profil de membre (`MemberProfile`) | aucun, lu par `memberId` | date de naissance, fonction, rôle dans l'entreprise, adresse, ville, pays | un par membre, créé à la création du membre, recréé au besoin à la première mise à jour |

Points clés :

- `type` d'un client : `PERSON` ou `COMPANY`. Pour une personne, le serveur remplit `companyName` avec `"No company"` ; pour une organisation, `firstname` et `lastname` valent `"No firstname"` et `"No lastname"` quand ils ne sont pas fournis (`src/main/java/com/sni/bokaticowork/features/client/customer/mapper/decorator/CustomerMapperDecorator.java:53-60`). Ne pas afficher ces valeurs telles quelles.
- Le `customerId` d'un membre est son **payeur par défaut** : ses factures vont à ce client. Il peut s'agir de sa fiche personnelle (`PERSON`) ou d'une organisation (`COMPANY`). Les rattachements à une ou plusieurs organisations (liens avec statut, mode de facturation, fin de lien) sont un autre objet, géré par le module `company` : [../company/README.md](../company/README.md).
- `CustomerSummaryresponse` contient un champ `id` numérique int64 : ne pas l'utiliser, travailler avec `customerId` (voir [identifiants](../README.md#identifiants--attention-aux-grands-nombres)).
- Les fiches supprimées restent en base (`deleted = true`) et disparaissent de toutes les lectures. Un membre dont le client ou le compte utilisateur est supprimé disparaît aussi des listes (`findAllVisible`, `src/main/java/com/sni/bokaticowork/features/client/member/repository/repo/MemberRepository.java:92-112`).

## Statuts et cycle de vie

### Client (`CustomerStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `ACTIVE` | client utilisable | création (`POST /sni/api/v1/customers`, création d'un membre) ; `PATCH .../status` ; dossier KYC du client approuvé (automatique) ; membre passé `ACTIVE` | `SUSPENDED`, `INACTIVE`, `ARCHIVED` |
| `PENDING` | dossier KYC du client en cours | automatiquement juste après `POST /sni/api/v1/customers` si des pièces KYC sont exigées pour ce type de client ; dossier KYC en correction, expiré ou à renouveler (worker KYC) | `ACTIVE`, `INACTIVE`, `ARCHIVED` |
| `SUSPENDED` | client bloqué | `PATCH .../status` ; blocage d'une organisation par le module `company` (`CompanyAccountService.java:222`) | `ACTIVE`, `INACTIVE`, `ARCHIVED` |
| `INACTIVE` | client désactivé | `PATCH .../status` ; un de ses membres passé `INACTIVE` ou `SUSPENDED` | `ACTIVE`, `ARCHIVED` |
| `ARCHIVED` | état final | `PATCH .../status` | aucun |

La table des transitions permises est dans `src/main/java/com/sni/bokaticowork/features/client/customer/service/implementation/CustomerServiceImpl.java:146-152`. Hors table : `400` `Status transition from X to Y is not allowed`. Les transitions automatiques (KYC, membre) ne passent pas par cette table.

Effet de bord : après chaque `PATCH .../status`, le dossier KYC du client est resynchronisé. Passer un client à `ACTIVE` met son dossier KYC à `APPROVED` (`src/main/java/com/sni/bokaticowork/features/document/kyc/service/implementation/KycAutomationServiceImpl.java:110-116`), même si les pièces n'ont pas été vérifiées. L'écran doit le dire avant de confirmer.

`DELETE /sni/api/v1/customers/{customerId}` ne change pas le statut : la fiche est marquée supprimée et **tous ses membres** sont archivés (`deleted = true`, `ARCHIVED`, `portalAccess = false`, `MemberRepository.java:169-179`).

### Membre (`MemberStatus`)

| Statut | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| `PENDING` | créé, dossier KYC en cours | toute création ; dossier KYC revenu en cours (automatique) | tout statut |
| `UNDER_REVIEW` | pièces KYC déposées, en vérification | dépôt de pièces KYC (module `document`) ; `PATCH .../status` | tout statut |
| `PENDING_CORRECTION` | pièce refusée, expirée ou à renouveler | décision KYC ou worker KYC (automatique) ; `PATCH .../status` | tout statut |
| `REJECTED` | refusé | `PATCH .../status` seulement (aucun automatisme ne l'écrit) | tout statut |
| `ACTIVE` | membre actif | `PATCH .../status` avec `ACTIVE` ; `POST /sni/api/v1/members/portal/complete` ; vérification de l'adresse courriel au portail ; dossier KYC approuvé (automatique) ; `PATCH .../portal-access/enable` depuis `INACTIVE` ou `SUSPENDED` | tout statut |
| `INACTIVE` | désactivé | `PATCH .../status` ; `PATCH .../portal-access/disable` | tout statut |
| `SUSPENDED` | suspendu | `PATCH .../status` | tout statut |
| `ARCHIVED` | retiré (fiche marquée supprimée) | `DELETE /sni/api/v1/members/{id}` ; suppression de son client | aucun (le membre n'est plus lisible) |

Il n'y a **aucune table de transitions** pour le membre : `PATCH /sni/api/v1/members/{id}/status` accepte n'importe quelle valeur de l'énumération (`MemberServiceImpl.java:246`). Effets selon la cible (`MemberServiceImpl.java:241-266`) :

- `ACTIVE` : `portalAccess = true` ; au premier passage, `portalActivatedAt = maintenant` et fin du délai KYC = maintenant + `PORTAL_KYC_GRACE_PERIOD_DAYS` (7 jours par défaut, `application.yml:61`) ; le **client payeur** passe `ACTIVE` ; le compte utilisateur reçoit `ROLE_MEMBER`, est activé et son adresse courriel est marquée vérifiée.
- `INACTIVE` ou `SUSPENDED` : `portalAccess = false` et le **client payeur** passe `INACTIVE`.
- Toute cible : le dossier KYC du membre est resynchronisé (`PENDING` donne `IN_PROGRESS`, `UNDER_REVIEW` donne `UNDER_REVIEW`, `PENDING_CORRECTION` ou `REJECTED` donnent `PENDING_CORRECTION`, `ARCHIVED` donne `EXPIRED`).

### Accès au portail

L'accès au portail (`/sni/api/v1/client/**`) exige trois choses (`src/main/java/com/sni/bokaticowork/security/authorization/ClientPortalAuthorizationManager.java:27-50`) : le rôle `ROLE_MEMBER` sur le compte, un membre non supprimé lié au compte, et `portalAccess = true`. Ensuite, sur le portail, un garde KYC refuse en `403` (`KYC_REQUIRED`) tant que le dossier KYC n'est pas `APPROVED` **et** que le délai de grâce (`kycGracePeriodEndAt`) est dépassé ou absent (`src/main/java/com/sni/bokaticowork/features/portal/guard/ClientOnboardingGuard.java:58-78`). Un membre créé n'a pas encore de délai : il n'en reçoit un qu'au premier passage en `ACTIVE` ou à `portal-access/enable`.

## Parcours

### Liste et recherche des clients

1. `GET /sni/api/v1/customers?summary=true&page=0&size=20` renvoie une page de `CustomerSummaryresponse` (`customerId`, `displayName`, `email`, `phone`, `status`). Sans `summary`, la page contient des `CustomerResponse` complets (adresse comprise). `GET /sni/api/v1/customers/summary` équivaut à `summary=true`. Tri par défaut `createdAt,DESC`, le paramètre `sort` est respecté.
2. Recherche instantanée : `GET /sni/api/v1/customers/search/basic?query=...&type=COMPANY` (type facultatif, `PERSON` ou `COMPANY`, insensible à la casse). Cherche dans le code, la raison sociale, le prénom, le nom, le courriel et le téléphone. Liste non paginée.
3. Liste déroulante complète : `GET /sni/api/v1/customers/summary/all` (toutes les fiches non supprimées, sans limite).
4. Fiche : `GET /sni/api/v1/customers/{customerId}` ou `GET /sni/api/v1/customers/by-email?email=...`.

### Créer un client sans membre

1. `POST /sni/api/v1/customers` ([détail](routes/CustomerController.md#r-post-sni-api-v1-customers)) avec `type`, `email`, `phone`, et selon le type `firstname` + `lastname` (`PERSON`) ou `companyName` (`COMPANY`). Facultatifs : `billingEmail`, `whatsappPhone`, `phoneCountry`, `address`. En-tête `Idempotency-Key` conseillé.
2. Réponse `201` avec le `CustomerResponse`. Le statut est `ACTIVE`, ou déjà `PENDING` si des pièces KYC sont configurées pour ce type de client (le statut renvoyé est relu après l'initialisation KYC).
3. Un courriel « Bienvenue chez Bokati » part vers `email` après validation.
4. Pour une organisation, la suite (profil légal, facturation, blocage, membres rattachés) se fait dans le module `company`.

### Modifier un client

1. `PATCH /sni/api/v1/customers/{customerId}` avec un `CustomerRequest`. Malgré le verbe, le corps est **validé comme une création** : `type`, `email` et `phone` sont obligatoires, ainsi que `firstname` + `lastname` (`PERSON`) ou `companyName` (`COMPANY`). Renvoyer donc toujours ces champs avec leur valeur actuelle.
2. Seuls les champs non vides changent. Une adresse partielle complète l'adresse existante.
3. Réponse `204` : relire la fiche pour l'afficher.
4. Changer le courriel ou le téléphone du client ne change ni le membre ni le compte utilisateur.

### Changer le statut d'un client, le supprimer

1. `PATCH /sni/api/v1/customers/{customerId}/status` avec `{ "status": "SUSPENDED" }`. Réponse `204`. Ne proposer que les cibles permises par la table ci-dessus.
2. `DELETE /sni/api/v1/customers/{customerId}` : réponse `204`. Prévenir l'utilisateur que tous les membres du client sont archivés et perdent l'accès au portail.

### Liste et recherche des membres

1. `GET /sni/api/v1/members?summary=true` : page de `MemberSummaryResponse` (`memberId`, `customerId`, `fullName`, `email`, `phone`, `status`, `portalAccess`). Sans `summary` : page de `MemberResponse`. Avec `customerId=CUS-...` : page résumée des membres dont c'est le payeur par défaut (le paramètre `summary` est alors ignoré). Ces trois listes sont toujours triées par date de création décroissante : le paramètre `sort` est ignoré (`MemberServiceImpl.java:826-828`).
2. Recherche instantanée : `GET /sni/api/v1/members/search/basic?query=...` (alias `/search/by-name`), recherche approchée en base, liste non paginée triée par pertinence.
3. Recherche avancée : `POST /sni/api/v1/members/search?page=0&size=20` avec un corps facultatif `{ "query", "customerId", "memberId", "code", "firstname", "lastname", "email", "phone", "status", "portalAccess" }`. Les champs texte sont combinés en **OU** et la correspondance est approchée (accents et casse ignorés, similarité) ; `customerId`, `status` et `portalAccess` filtrent en **ET**. Seule cette liste respecte `sort`. Elle exige le droit `CLIENT:CREATE` (route en `POST`).
4. Liste déroulante : `GET /sni/api/v1/members/summary/all` (1 000 membres au plus, `MemberServiceImpl.java:268-282`).
5. Fiche : `GET /sni/api/v1/members/{memberId}` ou `GET /sni/api/v1/members/by-email?email=...`.

### Créer un membre depuis le back-office

Route à utiliser : `POST /sni/api/v1/members/admin/create` ([détail](routes/MemberController.md#r-post-sni-api-v1-members-admin-create)), avec `Idempotency-Key`.

1. Choisir le payeur :
   - **nouvelle fiche personnelle** : ne pas envoyer `existingCustomerId` ; `customerType` absent ou `PERSON`. Le serveur crée un client `PERSON` avec les nom, courriel, téléphone et adresse du membre (sans courriel de bienvenue client ni dossier KYC client).
   - **client existant** : envoyer `existingCustomerId` (code `CUS-...`, recherché via `GET /sni/api/v1/customers/search/basic`).
   - **organisation** : `customerType: "COMPANY"` **et** `existingCustomerId` d'un client de type `COMPANY` sont obligatoires ensemble. Une organisation ne se crée pas ici.
2. Champs obligatoires : `firstname`, `lastname`, `email`, `phone`. Mot de passe : soit `generatePassword: true` (le serveur tire un mot de passe de 8 caractères), soit `password`. Facultatifs : `phoneCountry`, `whatsappPhone` (par défaut le téléphone), `billingEmail`, `address`, `birthDate`, `gender`, `preferredCommunicationChannel`, `companyName`.
3. Réponse `201` avec le `MemberResponse` : `status = PENDING`, `portalAccess = true`, le compte a déjà `ROLE_MEMBER`.
4. Après validation, deux courriels partent : bienvenue membre, et un lien de définition du mot de passe (le mot de passe généré n'est jamais renvoyé).
5. Le membre ne passe pas encore le garde KYC du portail (aucun délai de grâce) : pour lui ouvrir le portail tout de suite, enchaîner avec `PATCH /sni/api/v1/members/{memberId}/status` `{ "status": "ACTIVE" }` ou `PATCH .../portal-access/enable`.

`POST /sni/api/v1/members` (même corps) crée le membre comme une inscription portail : `password` obligatoire, `generatePassword` ignoré, **pas de `ROLE_MEMBER`**, pas de lien de mot de passe. Le compte ne peut pas entrer au portail tant qu'il n'est pas activé (étape suivante). Le portail public, lui, passe par `POST /sni/api/v1/auth/register` (module `securite`).

### Activer un membre inscrit

1. `POST /sni/api/v1/members/portal/complete` avec `{ "email": "..." }`. Le serveur donne `ROLE_MEMBER`, active le compte, marque le courriel vérifié au nom de l'utilisateur connecté, puis passe le membre en `ACTIVE` (avec les effets décrits plus haut).
2. Réponse `200` avec le `MemberResponse` à jour.

Équivalent : `PATCH /sni/api/v1/members/{memberId}/status` avec `ACTIVE`.

### Ouvrir ou fermer l'accès au portail, délai KYC

1. Ouvrir : `PATCH /sni/api/v1/members/{memberId}/portal-access/enable` (sans corps, `204`). Donne `ROLE_MEMBER`, active le compte, fixe le délai KYC au premier passage, et repasse en `ACTIVE` un membre `INACTIVE` ou `SUSPENDED` (les autres statuts ne bougent pas).
2. Fermer : `PATCH /sni/api/v1/members/{memberId}/portal-access/disable` (`204`). `portalAccess = false` et statut `INACTIVE`. Le rôle n'est pas retiré, le client payeur n'est pas touché.
3. Délai KYC : `PATCH /sni/api/v1/members/{memberId}/kyc-grace-period` avec `{ "gracePeriodDays": 14 }` (1 à 365). La fin du délai est calculée **depuis la date d'ouverture du portail** (`portalActivatedAt`), pas depuis aujourd'hui (`MemberServiceImpl.java:413-420`). Pour prolonger un membre ouvert il y a 20 jours, il faut donc envoyer plus de 20. Si le portail n'a jamais été ouvert, le calcul part de maintenant. Aucune route de ce module ne renvoie `portalActivatedAt` ni `kycGracePeriodEndAt` : l'écran ne peut pas afficher l'échéance actuelle depuis ce module (voir `GET /sni/api/v1/client/me/onboarding-status`, côté portail).

### Modifier un membre

1. `PATCH /sni/api/v1/members/{memberId}` avec seulement les champs à changer parmi `firstname`, `lastname`, `email`, `phone`, `phoneCountry`, `whatsappPhone`. Réponse `204`.
2. Le nouveau courriel et les nouveaux prénom et nom sont recopiés sur le compte utilisateur (le courriel sert d'identifiant de connexion : prévenir l'utilisateur). La fiche client n'est pas modifiée.
3. Un changement de téléphone est signalé au module de conformité.

### Profil du membre

1. `GET /sni/api/v1/members/{memberId}/profile` : `MemberProfileResponse` (identité recopiée du membre, plus `birthDate`, `jobTitle`, `companyRole`, `address`, `city`, `country`).
2. `PUT /sni/api/v1/members/{memberId}/profile` avec les champs à changer. Malgré le `PUT`, un champ absent ou vide **n'efface pas** la valeur existante. Réponse `200` avec le profil.
3. `GET` et `PUT /sni/api/v1/members/me/profile` font la même chose pour le membre dont le courriel est celui du compte connecté. Ce sont des routes back-office (rôle d'administration et `CLIENT:READ` ou `CLIENT:UPDATE`) : un membre du portail doit utiliser `GET` / `PUT /sni/api/v1/client/profile` (module `portal`). Un compte d'équipe sans fiche membre reçoit `404 Member not found`.

### Changer le payeur par défaut

1. `PATCH /sni/api/v1/members/{memberId}/transfer-customer` avec `{ "customerId": "CUS-..." }`. Réponse `200` avec le `MemberResponse`.
2. Seul le payeur change : aucun lien de rattachement n'est créé ni terminé. Pour rattacher un membre à une organisation, utiliser le module `company` ([../company/README.md](../company/README.md)), qui gère aussi le retour du payeur sur la fiche personnelle quand le lien prend fin.

### Retirer un membre

1. `DELETE /sni/api/v1/members/{memberId}` : archivage (statut `ARCHIVED`, fiche marquée supprimée, dossier KYC `EXPIRED`), `204`. Idempotent : un second appel répond aussi `204`. Si c'était le dernier membre de son client, le client n'est plus marqué « membre ». Les factures, contrats et mouvements restent.
2. `DELETE /sni/api/v1/members/{memberId}?purge=true&confirm=true&reason=...` (rôle `ROLE_ADMIN` et droit `SYSTEM:SETTINGS` en plus de `CLIENT:DELETE` ; sans `confirm=true` : `400`) : **suppression définitive** du membre et de toutes ses données (compte, factures, portefeuille, contrats, abonnements, réservations), réponse `200` avec le rapport de purge. Refusée en `400` tant que la purge n'est pas activée côté serveur (`MEMBER_PURGE_ENABLED`). Ne pas exposer cette option dans l'écran ordinaire ; la prévisualisation est `GET /sni/api/v1/admin/maintenance/members/{memberCode}/purge-preview` (module `socle`).

## Règles et erreurs à refléter dans l'écran

Client (`POST` et `PATCH /sni/api/v1/customers...`) :

- `type` obligatoire, `PERSON` ou `COMPANY`. Sinon `400` `Invalid customer request, the type is not valid`. Les autres erreurs sont réunies dans un seul `message`, séparées par des virgules (`... the firstname is not valid`, `... the lastname is not valid`, `... the company name is required`, `... the email is not valid`, `... the billing email is not valid`, `... the phone number is not valid`, `... the whatsapp phone number is not valid`). Le champ `errors` est vide : découper `message` pour placer les erreurs sous les champs.
- Courriel unique parmi les clients non supprimés : `409` `A customer with this email already exists`. Téléphone unique : `409` `A customer with this phone number already exists`.
- Téléphone : normalisé au format international. Sans indicatif, le pays vient de `phoneCountry` (code ISO `CG` ou indicatif `+242`), sinon du pays de l'établissement.
- Fiche introuvable sur `GET /customers/{customerId}` ou `GET /customers/by-email`, courriel mal formé sur `by-email`, `query` vide sur `search/basic` : le serveur répond **`500`** (`INTERNAL_SERVER_ERROR`), pas `404` ni `400` (voir Écarts). Traiter un `500` sur ces lectures comme « introuvable ». `type` invalide sur `search/basic` : `400` `Invalid customer type: ...`.
- Statut : `404` si le client n'existe pas, `400` si la transition est interdite.

Membre (création, `POST /members` et `/members/admin/create`) :

- Message unique, erreurs séparées par des virgules : `Firstname is required`, `Lastname is required`, `A valid email is required`, `A valid phone number is required`, `Customer type is invalid`, `Company name is required when customer type is COMPANY`, `Billing email is invalid`, `Password is required for portal registration`, `Password is required when generatePassword is false`, `Existing customer id is required when customer type is COMPANY`, `Existing customer must be of type COMPANY when customer type is COMPANY`. Tous en `400`.
- Doublons, tous en **`400`** (pas `409`) : `A member with this email already exists`, `A member with this phone number already exists`, `A user with this email already exists`, et, sans `existingCustomerId`, `A customer with this email already exists. Use existingCustomerId to attach this member to the existing customer`. Ce dernier message invite à proposer à l'utilisateur de choisir le client existant. Un téléphone déjà pris par un **client** (`A customer with this phone number already exists`) arrive aussi en `400`.
- `existingCustomerId` inconnu : `404` `Customer with id ... not found` ; client supprimé : `400` `Le client ... est supprimé et ne peut plus être utilisé pour de nouvelles opérations`.
- Tailles maximales (validation, `400 VALIDATION_FAILED`) : `firstname`, `lastname` 350 ; `email`, `billingEmail` 250 ; `phone`, `whatsappPhone`, `customerType`, `preferredCommunicationChannel` 30 ; `password` 45 ; `phoneCountry` 10 ; `gender` 20.

Membre (autres routes) :

- Membre introuvable ou archivé : `404` `Member not found`.
- `PATCH .../status` : envoyer une valeur exacte de l'énumération, en majuscules. Une valeur inconnue, absente ou `null` produit un **`500`** (voir Écarts). Passer en `ACTIVE`, `INACTIVE` ou `SUSPENDED` un membre sans client payeur produit aussi un `500`.
- `PATCH /members/{id}` : `400` `A valid email is required`, `A member with this email already exists` (le courriel est aussi refusé s'il appartient déjà à un compte utilisateur), `A valid phone number is required`, `A member with this phone number already exists`, `A valid whatsapp phone number is required`.
- `kyc-grace-period` : `gracePeriodDays` entre 1 et 365, sinon `400` (`Grace period must be at least 1 day` ou `Grace period cannot exceed 365 days`).
- `transfer-customer` : `customerId` vide, `400` ; client inconnu, `404` ; client supprimé, `400`.
- Recherche : `query` vide sur `members/search/basic`, `400` `Search query is required`.

## À prévoir côté frontend

Écrans :

- **Liste des clients** (filtre par type via la recherche, statut), **fiche client** (identité, adresse, statut, membres via `GET /sni/api/v1/members?customerId=...`), formulaire de création et de modification. Pour une organisation, la fiche complète est celle du module `company`.
- **Liste des membres** avec recherche avancée, **fiche membre** (identité, statut, `portalAccess`, payeur, profil), formulaire de création back-office, actions de statut et de portail.

Droits pour masquer les boutons :

| Action | Droit |
| --- | --- |
| Lire listes, fiches, profils, recherche simple | `CLIENT:READ` |
| Créer un client ou un membre, activer un inscrit (`portal/complete`), **recherche avancée** `POST /members/search` | `CLIENT:CREATE` |
| Modifier, changer un statut, ouvrir ou fermer le portail, délai KYC, changer de payeur, profil | `CLIENT:UPDATE` |
| Supprimer un client, archiver ou purger un membre | `CLIENT:DELETE` |

Pièges :

- Deux formes de réponse sur `GET /sni/api/v1/customers` et `GET /sni/api/v1/members` selon `summary` (et `customerId` pour les membres) : typer la réponse selon les paramètres envoyés.
- Les champs ne portent pas les mêmes noms selon le modèle : `fullname` (`MemberResponse`), `fullName` (`MemberSummaryResponse`), `firstName` / `lastName` (`MemberProfileResponse`) contre `firstname` / `lastname` ailleurs.
- `MemberSummaryResponse.whatsapp_phone` est toujours `null` : lire le numéro WhatsApp sur la fiche complète.
- `CustomerResponse.createdAt` est une **chaîne** ; `MemberResponse.createdAt` est une date-heure ISO.
- `status` arrive en texte ; listes à coder en dur : `CustomerStatus` (`ACTIVE`, `PENDING`, `INACTIVE`, `SUSPENDED`, `ARCHIVED`), `MemberStatus` (`ACTIVE`, `PENDING`, `UNDER_REVIEW`, `PENDING_CORRECTION`, `REJECTED`, `INACTIVE`, `SUSPENDED`, `ARCHIVED`), `CustomerType` (`PERSON`, `COMPANY`). Aucune route ne les fournit.
- Adresse : `AddressRequest` avec `countryCode` ; la liste des pays est dans le module `socle`.
- Les `PATCH` de modification renvoient `204` sans corps : relire la fiche.
- Ne pas envoyer de champ d'affichage (JSON strict, voir [README racine](../README.md#corps-des-requêtes)) : par exemple pas de `customerId` ni de `status` dans un `CustomerRequest`.
- Aucun fichier, aucun temps réel dans ce module.

## Effets automatiques

- **Courriels** (envoyés après validation de la transaction, un échec d'envoi n'annule rien) : « Bienvenue chez Bokati » à la création d'un client par `POST /sni/api/v1/customers` ; « Bienvenue dans votre espace membre » à toute création de membre ; lien de définition du mot de passe à la création par `/members/admin/create`.
- **KYC** (module `document`) : un dossier est ouvert à la création d'un membre et d'un client créé seul, s'il existe des exigences KYC actives ; chaque changement de statut le resynchronise. En retour, le dépôt et la vérification des pièces changent seuls le statut du membre (`PENDING`, `UNDER_REVIEW`, `PENDING_CORRECTION`, `ACTIVE`) et celui du client (`PENDING`, `ACTIVE`). Le worker KYC (`src/main/java/com/sni/bokaticowork/features/document/kyc/worker/KycAutomationWorker.java`) passe chaque lundi à 8 h les membres aux pièces expirées en `PENDING_CORRECTION` et leurs clients en `PENDING`, et approuve chaque jour à 8 h les pièces éligibles à l'approbation automatique (ce qui peut rendre le membre `ACTIVE`).
- **Conformité** : la création d'un membre et le changement de son téléphone sont signalés au module de conformité des paiements, après validation.
- **Organisation** : bloquer une organisation (module `company`) la passe en `SUSPENDED` ; mettre fin au lien d'un membre avec l'organisation qui était son payeur remet le payeur sur sa fiche personnelle.
- **Journal d'audit** : toutes les routes d'écriture sont journalisées (modules d'audit `CUSTOMER` et `MEMBER`).

## Écarts relevés

1. **Corrigé le 2026-11-08** : la purge par cette route exige maintenant `ROLE_ADMIN`, `SYSTEM:SETTINGS` (ou `ROLE_SUPER_ADMIN`) et `confirm=true`, comme la route dédiée. Constat d'origine : `DELETE /sni/api/v1/members/{id}?purge=true` déclenchait la purge définitive avec le seul droit `CLIENT:DELETE`, sans la confirmation `confirm=true` ni la règle `hasAnyAuthority('ROLE_ADMIN', 'ADMIN')` qu'exige la route dédiée `MemberPurgeController` (`src/main/java/com/sni/bokaticowork/features/client/member/controller/MemberController.java:239`, à comparer avec `src/main/java/com/sni/bokaticowork/core/maintenance/MemberPurgeController.java:37`). Seul le réglage `MEMBER_PURGE_ENABLED` la retient.
2. Passer un membre en `INACTIVE` ou `SUSPENDED` passe son client payeur en `INACTIVE`, même quand ce client est une organisation qui a d'autres membres ; passer en `ACTIVE` réactive le client sans respecter la table des transitions du client (un client `ARCHIVED` redevient `ACTIVE`) (`src/main/java/com/sni/bokaticowork/features/client/member/service/implementation/MemberServiceImpl.java:255-261`). Si le membre n'a pas de client, `NullPointerException`, donc `500`.
3. `PATCH /sni/api/v1/members/{id}/status` : aucune table de transitions, et `MemberStatus.valueOf` sans contrôle : valeur inconnue ou absente, `500` au lieu de `400` (`MemberServiceImpl.java:246`). Le corps `UpdateStatusRequest` n'a aucune annotation de validation (`src/main/java/com/sni/bokaticowork/features/client/member/dto/request/UpdateStatusRequest.java:3`).
4. `CustomerServiceImpl` lève `IllegalArgumentException` pour « introuvable » et « requête invalide », que le gestionnaire global traduit en `500` (`CustomerServiceImpl.java:173`, `:183`, `:187`, `:247`).
5. `PATCH /sni/api/v1/customers/{customerId}` valide le corps comme une création (type, courriel, téléphone et noms obligatoires) alors que la mise à jour est partielle (`CustomerServiceImpl.java:122`).
6. Passer un client à `ACTIVE` par `PATCH .../status` approuve son dossier KYC sans vérification des pièces (`src/main/java/com/sni/bokaticowork/features/document/kyc/service/implementation/KycAutomationServiceImpl.java:112`).
7. `MemberSummaryResponse.whatsapp_phone` n'est jamais rempli, et son nom est en `snake_case` contrairement aux autres champs (`src/main/java/com/sni/bokaticowork/features/client/member/dto/response/MemberSummaryResponse.java:20`, mapping dans `src/main/java/com/sni/bokaticowork/features/client/member/mapper/decorator/MemberMapperDecorator.java:139-149`).
8. Les doublons à la création d'un membre sont convertis en `400 BAD_REQUEST` au lieu de `409` (`MemberServiceImpl.java:157-159`), alors que la création d'un client renvoie `409` pour les mêmes cas.
9. `POST /sni/api/v1/members/search` est une lecture mais exige `CLIENT:CREATE` (verbe `POST`, `MemberController.java:188`). `GET` / `PUT /sni/api/v1/members/me/profile` sont des routes back-office qui cherchent un membre au nom du compte d'équipe connecté : inutilisables en pratique (`MemberController.java:125`, `:139`).
10. `transfer-customer` change le payeur sans toucher l'indicateur « client membre » de l'ancien et du nouveau client, et sans créer de lien d'organisation (`MemberServiceImpl.java:361-373`).
11. `GET /sni/api/v1/members` ignore le paramètre `sort` annoncé (tri fixe par date de création, `MemberServiceImpl.java:826`), et `GET /sni/api/v1/customers/summary/all` renvoie toutes les fiches sans limite (`CustomerServiceImpl.java:236-241`).
12. Messages d'erreur en anglais pour presque toutes les règles du module (seul le refus d'un client supprimé est en français, `CustomerServiceImpl.java:230`).
13. La création d'un client compare le courriel en minuscules à des courriels enregistrés tels que saisis (`existsByEmailAndDeletedFalse` fait une égalité stricte, `CustomerServiceImpl.java:89` et `src/main/java/com/sni/bokaticowork/features/client/customer/repository/CustomerRepository.java:88-92`) : un doublon qui ne diffère que par la casse n'est pas détecté.
