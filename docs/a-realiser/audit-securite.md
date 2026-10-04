# Audit de sécurité · Bokati Cowork

Audit du code au 2 octobre 2026, branche `feature/payment-facturation`. Portée : intrusion
externe, CSRF, vol et rejeu de jeton JWT, accès administrateur par un tiers, injection de commande.

Second passage : la surface publique a été énumérée route par route et chaque élément laissé en
suspens au premier passage a été ouvert et lu. Les constats infirmés par la vérification sont
signalés comme tels · un audit qui ne retire rien n'a pas vérifié.

Chaque constat porte une **gravité**, ce qu'un attaquant obtient, et ce qui le rend possible. Le
plan de correction est en §7, dans l'ordre où il faut le livrer.

---

## État d'avancement

Mis à jour à chaque correctif livré.

| # | Constat | Gravité | État | Commit |
|---|---|---|---|---|
| §0 | `bootstrap-admin` crée un admin sans authentification | critique | **corrigé** | lot 1 |
| §0 | Défauts dangereux dans le code, sûreté portée par le profil | critique | **corrigé** | lot 1 |
| A1 | Auto-admin · authentification contournée sans jeton | critique | **corrigé** | lot 1 |
| A2 | `/actuator/metrics` déclaré exposé | faible | **sans objet** · dépendait d'A1 | lot 1 |
| §2.5 | Deux listes de chemins publics divergentes | moyen | **corrigé** | lot 1 |
| J1 | Secret JWT en clair dans la source | critique | **corrigé** | `1374858` |
| J2 | Jeton d'accès de 25 h par défaut | élevé | **corrigé** | lot 1 |
| J3 | Type de jeton non exigé sur l'API | moyen | **corrigé** | lot 1 |
| §2.8 | Clé du jeton d'aperçu partagée avec le JWT | faible | **partiel** · plus de valeur en dur, clé toujours partagée | `1374858` |
| W2 | WebSocket · type de jeton et révocation non vérifiés | moyen | **corrigé** | lot 2 |
| §3 | WebSocket · aucune autorisation par destination | élevé | **corrigé** | lot 2 |
| §2.6 | Plafond de requêtes absent hors `ApiPath.V1` | moyen | à faire | |
| §2.1 | Pas de plafond propre sur `unlock-account` et `verify/resend` | moyen | à faire | |
| §2.3 | Écritures anonymes sous `/public` sans plafond propre | moyen | à faire | |
| §2.4 | Cookie scanner · clé en clair, `path=/`, un an, check-in en GET | moyen | à faire | |
| C3 | Traversée de chemin par l'extension de fichier | élevé | **corrigé** | lot 3 |
| J4 | Jeton non lié à son porteur | moyen | à faire | |
| §6 | CORS trop large | faible | à faire | |
| §6 | Plafond de requêtes en mémoire du processus | moyen | à faire | |
| C4 | SSRF par les webhooks | faible | à faire | |
| C1 | Injection de commande | n/a | **aucun vecteur** · vérifié | |
| C2 | Injection SQL | n/a | **aucun vecteur** · vérifié | |

### Ce que le lot 1 a changé

- **`bootstrap-admin`** · trois verrous, et il faut les trois : un secret d'amorçage
  (`app.security.bootstrap.secret`, **vide par défaut donc amorçage fermé**) comparé en temps
  constant, l'absence de tout administrateur, et un journal explicite. La route reste publique ·
  par définition il n'y a encore personne · mais elle ne crée plus rien.
- **Auto-admin supprimé** · la méthode, les deux réglages, et les sections `auto-admin` des deux
  profils. Sans jeton, le filtre ne pose plus aucune identité. Le garde-fou de démarrage **refuse
  de démarrer** si la propriété réapparaît dans la configuration.
- **Une seule liste de chemins publics** · `PublicPaths`, consommée par `SecurityConfig` et par
  `JWTFilter`. Livrée dans le même commit que la suppression de l'auto-admin, parce que trois
  chemins ne fonctionnaient que grâce à lui.
- **Type de jeton exigé** · `JWTFilter` refuse un jeton de rafraîchissement ou de vérification.
- **Défauts inversés** · jeton d'accès à 15 min (contre 25 h), aperçu public de document fermé.
- **`SecurityConfigurationGuard`** · en profil `prod`, l'application **s'arrête** si le secret est
  vide ou trop court, si le jeton d'accès dépasse une heure, si la clé fiscale est vide, ou si
  l'auto-admin est encore configuré. Hors production, les mêmes constats sont des avertissements.

### Ce que le lot 2 a changé · WebSocket

- **Autorisation par destination** · `WebSocketDestinations` réserve `/topic/admin/**`,
  `/topic/inventory/**` et `/topic/support/**` aux rôles de la maison. Un membre du portail qui
  tente l'un des trois reçoit un refus nominatif dans les journaux.
- **Toute destination demande une identité** · y compris les files `/queue/**`, qui n'en
  exigeaient aucune. Seul `/topic/` était contrôlé.
- **Le type de jeton est exigé à la connexion** · un jeton de rafraîchissement, valable sept
  jours, ouvrait une session temps réel.
- **La révocation de session est consultée** · une déconnexion ferme désormais le temps réel
  aussi. `/ws/**` est exclu du filtre HTTP, donc personne ne la vérifiait.
- **Un jeton inacceptable refuse la connexion** au lieu de la laisser sans identité · ce qui,
  avant le durcissement des abonnements, ouvrait les files.
- **La liste des rôles de la maison vit une seule fois** (`SecurityRoles`). Elle existait en
  trois exemplaires · gestionnaire d'autorisation de l'API, résolveur d'origine de paiement, et
  l'autorisation WebSocket allait en ajouter un quatrième. C'est la copie oubliée qui devient la
  faille.

Note · `WebSocketTopics.SUPPORT_TICKETS` est déclaré mais **personne ne publie dessus**. La règle
le couvre quand même : le jour où un service s'en sert, il n'aura pas à y penser.

### Ce que le lot 3 a changé · écriture de fichiers

- **L'extension se restreint à un alphabet** · lettres et chiffres, huit au plus, et la lecture
  s'arrête au premier caractère qui n'en est pas un. Un nom comme
  `photo.../../../etc/passwd` ne produit plus d'extension du tout.
- **Chaque segment de chemin est assaini** · dossier du propriétaire et code du document. Ils
  viennent de données et sont sûrs aujourd'hui ; le calcul du chemin ne dépend plus de cette
  hypothèse.
- **L'écriture est confinée** · `normalize()` résout les `..`, il ne dit pas si le résultat est
  encore chez nous. C'est cette vérification qui manquait, et c'est la seule qui compte.
- **La lecture aussi** · un enregistrement écrit avant ce correctif pourrait pointer hors de la
  racine ; il ne sortira rien.

Un test tient l'invariant qui ferme la faille, indépendamment du détail : quelle que soit
l'entrée, l'extension retenue correspond à `[a-z0-9]{0,8}`.

**Une régression introduite et corrigée dans le même lot** · vider les défauts `minioadmin` du
lot « secrets » rendait le constructeur de `MinioClient` fatal au démarrage (il refuse des
identifiants vides). Le client objet ne se construit désormais que si le fournisseur est
`MINIO`/`S3` **et** que les clés sont présentes ; dans ce cas leur absence lève une erreur
explicite, et un stockage sur disque n'exige plus de clés S3. En production
`application-prod.yml` laisse ces clés vides quand `BUCKETEER_*`/`AWS_*` ne sont pas
renseignées · le démarrage aurait échoué.

Le garde-fou a immédiatement trouvé deux erreurs dans le `.env` local, corrigées :
`APP_JWT_ACCESS_EXPIRATION_MS` valait **604800000**, soit sept jours · la durée du
rafraîchissement recopiée sur l'accès ; et `APP_JWT_SECRET` faisait 31 caractères.
**À vérifier sur Heroku** : si `APP_JWT_ACCESS_EXPIRATION_MS` y porte la même valeur, la
production délivre des jetons d'accès de sept jours malgré le défaut du `yml` · et l'application
refusera désormais de démarrer, ce qui est le comportement voulu.

---

## 0. Les deux constats qui gouvernent tout le reste

### Un administrateur se crée sans authentification · **critique, exploitable en production**

`UserProvisioningServiceImpl.initializeGlobalAdmin` :

```java
public Users initializeGlobalAdmin(UserRequest request) {
//        if (roleUserService.hasAnyUserAssignedToRole(ADMIN_ROLE)) {
//            throw new BadRequestException("Global admin has already been initialized");
//        }
    Users admin = userService.createUser(request);
    roleUserService.addRoleToUser(admin.getId(), ADMIN_ROLE, SYSTEM_ASSIGNER);
    userService.activateUser(admin.getEmail());
    return admin;
}
```

Le garde-fou qui referme l'amorçage après le premier administrateur est **commenté**. Et la route
est en `permitAll` dans `SecurityConfig` :

```java
ApiPath.V1 + "/admin/provisioning/bootstrap-admin",
```

**Ce qu'un attaquant obtient** : une seule requête HTTP non authentifiée

```http
POST /sni/api/v1/admin/provisioning/bootstrap-admin
{ "email": "lui@ailleurs.com", "password": "…", "firstname": "…", "lastname": "…" }
```

lui rend un compte **`ADMIN`, activé, déverrouillé**, avec lequel il se connecte normalement. Il
dispose alors de toute l'API d'administration : clients, factures, paiements, portefeuilles,
documents KYC, création d'autres comptes.

`createUser` ne vérifie que la forme de la requête et l'unicité de l'adresse. Aucune invitation,
aucun jeton, aucune permission.

Contrairement au constat suivant, **celui-ci ne dépend d'aucun réglage** : il est actif en
production, maintenant. C'est le premier à corriger, avant tout déploiement.

### La sécurité de la production ne tient qu'à `application-prod.yml` · **critique**

Les valeurs écrites en dur dans le code Java sont systématiquement les valeurs **dangereuses** ;
ce sont les surcharges du fichier de profil qui les rendent sûres.

| Réglage | Défaut dans le code Java | Prod | Dev |
|---|---|---|---|
| `app.security.auto-admin.enabled` | **`true`** · authentifie en admin sans jeton | `false` | `true` |
| `app.security.jwt.secret` | **secret réel écrit en clair dans la source** | `${APP_JWT_SECRET}` | hérité |
| `jwt.access-token-expiration-ms` | **90 000 000 ms (25 h)** | `900000` (15 min) | hérité |
| `app.documents.public-preview-enabled` | `true` | **non surchargé** | non surchargé |
| `app.security.rate-limit.enabled` | `true` | `true` | `true` |

Un profil Spring absent ou mal nommé au démarrage transforme la production en **administration
ouverte à tous**, avec un secret JWT public et des jetons valables 25 heures. Rien n'empêche
l'application de démarrer dans cet état, et rien dans les journaux ne le signale comme anormal.

---

## 1. Accès administrateur par un tiers

### A1 · Contournement complet de l'authentification · **critique**

`JWTFilter.doFilterInternal` :

```java
String token = extractToken(request);
if (token == null) {
    authenticateAutoAdmin();      // aucun jeton → on authentifie en administrateur
    filterChain.doFilter(request, response);
    return;
}
```

`authenticateAutoAdmin()` charge le compte `app.security.auto-admin.email`
(défaut `admin@bokati.com`) et pose son `UserPrincipal` **avec toutes ses autorités** dans le
contexte de sécurité.

Avec `auto-admin.enabled = true`, une requête **sans aucun en-tête d'authentification** est traitée
comme venant de l'administrateur. La production met `false`, mais :

- le défaut du code est `true`, et le défaut du profil `dev` est `true` ;
- un seul réglage sépare la production d'une ouverture totale, sans aucun garde-fou au démarrage ;
- le journal d'audit attribue alors **toutes** les actions à `admin@bokati.com`, ce qui rend
  l'incident irretraçable après coup.

Un mécanisme de confort de développement ne doit pas pouvoir devenir une porte de production.

### A2 · `/actuator/metrics` déclaré exposé · **faible**

`management.endpoints.web.exposure.include: health,info,metrics`. Seuls `health` et `info` sont en
`permitAll` ; `metrics` retombe sur `anyRequest().authenticated()`, et `health.show-details: never`
est correct. Sérieux **uniquement combiné à A1** : avec l'auto-admin, « authentifié » ne veut plus
rien dire.

---

## 2. Intrusion externe · la surface publique, route par route

Deux listes indépendantes décident ce qui est joignable sans jeton : les `permitAll` de
`SecurityConfig` (autorisation) et `JWTFilter.isPublicApiRequest` (traitement du jeton). **Elles
divergent déjà** (§2.5).

### 2.1 Authentification · attendu, et correctement plafonné

| Route | Constat |
|---|---|
| `POST /auth/login` | 5 requêtes / 60 s par IP, et verrouillage de compte en base après 5 échecs · les deux protections existent |
| `POST /auth/refresh` | **Vérifié : la rotation est correcte.** `revokeToken(ancien)` puis nouveau couple accès + rafraîchissement, et `refreshSession`. Un jeton de rafraîchissement volé puis utilisé par le légitime se révoque de lui-même |
| `POST /auth/register` | Inscription portail · `registerMemberFromPortal` crée le membre **sans rôle**. Le rôle `MEMBER` n'est accordé qu'à l'activation par le personnel. Pas d'élévation possible |
| `POST /auth/ott/*`, `/password-reset/*` | 3 requêtes / 600 s par IP et par chemin |
| `POST /auth/unlock-account`, `/unlock-account/confirm` | Plafond générique seulement (600/min) · un déverrouillage de compte non plafonné spécifiquement annule en partie le verrouillage après 5 échecs |
| `POST /auth/email/verify/resend` | Plafond générique seulement · vecteur d'envoi massif de courriels vers une adresse tierce |

### 2.2 Rappels d'opérateur de paiement · durcis au lot précédent

`/payments/mobile-money/pawa(y)pay/callback`, `/refund-callback`, `/return`. Signature vérifiée ou,
à défaut, statut relu chez l'opérateur avant toute écriture ; 120 requêtes / 60 s. Voir
`docs/backend/mobile-money.md`.

### 2.3 Les sept contrôleurs sous `/public/**`

| Route | Méthode | Constat |
|---|---|---|
| `/public/bookings/{n}` | GET | **Vérifié : jeton obligatoire** (`@RequestParam String token`). Non énumérable par numéro |
| `/public/bookings/{n}/confirmation.pdf` | GET | Idem · jeton obligatoire |
| `/public/bookings/check-in/scan/{token}` | GET | Jeton de check-in + **cookie scanner** · voir §2.4 |
| `/public/bookings/check-in/scanner-verify` | POST | Valide la clé admin, pose le cookie · voir §2.4 |
| `/public/bookings/check-in/scanner-setup` | GET/POST | Idem |
| `/public/bookings/check-in/self` | POST | Auto-check-in client · fenêtre horaire et géolocalisation vérifiées |
| `/public/crm/leads` | POST | **Écriture non authentifiée** · création d'opportunités. Plafond générique seulement : inondation de la base CRM |
| `/public/events/{code}/registrations` | POST | **Écriture non authentifiée** · inscriptions à un événement. Même constat |
| `/public/events/{code}`, `/public/opening`, `/public/resources` | GET | Lecture de catalogue · attendu |
| `/public/support/csat` | POST | Enquête de satisfaction · à confirmer qu'un jeton lie la réponse au ticket |

**Gravité** : moyenne. Rien ne fuit, mais deux écritures anonymes ne sont protégées que par le
plafond générique de 600 requêtes/minute par IP · largement de quoi polluer le CRM et les
inscriptions, et de quoi déclencher des envois de courriels en masse.

### 2.4 Le cookie scanner · **un identifiant par cookie existe bien** · moyen

C'est le constat qui **corrige** la conclusion du premier passage sur le CSRF (§6).

```java
Cookie cookie = new Cookie(SCANNER_COOKIE_NAME, adminCode.trim());  // la cle admin, en clair
cookie.setMaxAge(365 * 24 * 3600);                                   // un an
cookie.setPath("/");                                                 // tout le domaine
cookie.setHttpOnly(true);
```

et la consommation :

```java
@GetMapping("/check-in/scan/{checkInToken}")   // GET, qui modifie l'etat
public ModelAndView scanCheckIn(...) {
    if (!checkInProperties.hasScannerCookie(request)) { ... }
    return performScanCheckIn(checkInToken);
}
```

Cinq constats, du plus gênant au moins :

1. **La clé admin partagée est stockée en clair dans le cookie**, un an, `path=/` · elle est donc
   envoyée à **toutes** les requêtes du domaine, y compris l'API.
2. Ni `Secure`, ni `SameSite` · le cookie part en clair si le domaine est jamais joignable en HTTP,
   et accompagne les requêtes déclenchées par un site tiers.
3. **Un check-in est un GET** avec autorité ambiante : une page visitée par le terminal scanner
   peut déclencher des check-ins par `<img src>`. CSRF est désactivé globalement.
4. Une **clé statique unique** pour tous les terminaux, jamais rotée.
5. `isScannerKeyValid` compare avec `String.equals`, donc en temps non constant. Peu exploitable
   via HTTP, mais gratuit à corriger.

L'impact métier d'un check-in forcé est faible. Ce qui compte, c'est que le motif existe : une
autorité ambiante par cookie, dans une application dont tout le modèle de sécurité suppose qu'il
n'y en a pas.

### 2.5 Les deux listes de chemins publics divergent · **moyen**

| Chemin | `SecurityConfig` | `JWTFilter` |
|---|---|---|
| `/admin/provisioning/bootstrap-admin` | ouvert | **absent** |
| `/public/**` | ouvert | **absent** |
| `/countries` | ouvert | **absent** |
| `/client/catalog/plans` | ouvert | ouvert |
| `/shares/**`, `/verify/**` | ouvert | ouvert |

Les chemins ouverts par `SecurityConfig` mais absents de `JWTFilter` fonctionnent quand même · le
filtre, sans jeton, tombe dans `authenticateAutoAdmin()` puis laisse passer. **C'est-à-dire que ces
routes-là ne sont « publiques » que grâce au mécanisme d'auto-admin, ou par le repli sur une absence
d'authentification.** Le jour où l'auto-admin disparaît (et il doit disparaître), leur comportement
change · la correction de A1 doit donc s'accompagner de la fusion des deux listes, pas la suivre.

Autre symptôme de la même cause : `QuoteSignaturePublicController` est monté sur
`/public/quotes/sign`, **sans le préfixe `ApiPath.V1`**. Il n'est donc couvert par aucun `permitAll`
et retombe sur `anyRequest().authenticated()` : une page de signature de devis destinée à un
signataire externe exige un compte. Fonctionnalité cassée, dans l'autre sens, par la même absence
de source unique.

### 2.6 Pages de vérification publiques · **conception saine, deux réserves**

`/verify/doc/{documentNumber}` et `/verify/receipt/{receiptNumber}`, sans jeton.

**Vérifié, et c'est bien fait** : sans le paramètre `?k=` correspondant au préfixe de la signature
fiscale, la page ne rend qu'une vue **minimale** · numéro, type, date d'émission, statut. Le nom du
client et le montant n'apparaissent **qu'avec** la signature. La décision est explicite dans le
code et elle est la bonne.

Les deux réserves :

1. La vue minimale confirme l'**existence** d'un document. Les numéros étant séquentiels
   (`INV-2026-000042`), un tiers peut énumérer le volume de facturation et son rythme.
2. **Aucun plafond de requêtes ne couvre `/verify/**`** · `RateLimitingFilter.ruleFor` ne reconnaît
   que les chemins commençant par `ApiPath.V1`, et `/verify` n'en fait pas partie : la méthode rend
   `null`, donc aucune règle. L'énumération ci-dessus est donc illimitée, et
   `POST /verify/doc/{n}/compare` accepte un **téléversement de fichier non authentifié et non
   plafonné**, dont le traitement compare des PDF · vecteur d'épuisement de ressources.

Le même angle mort couvre `/images/**` et `/public/quotes/sign`.

### 2.7 Partage de documents · **vérifié, sain**

`/shares/{token}` (`DocumentShareService`) : jeton `UUID.randomUUID()` sans tirets, soit 122 bits
d'entropie · non devinable. Expiration vérifiée, mot de passe facultatif **haché** avec le
`PasswordEncoder`, révocation par un drapeau `active`. Le jeton circule dans le chemin de l'URL,
compromis habituel et acceptable pour un lien de partage. Rien à corriger.

### 2.8 Aperçu de document signé · **vérifié, sain · une réserve sur la clé**

`/documents/*/signed-preview`, ouvert par défaut et **non surchargé en prod**. La robustesse repose
entièrement sur `DocumentAccessTokenService`, qui a été lu :

- HMAC-SHA256, format `base64url(code:exp).base64url(signature)` ;
- comparaison en **temps constant** (`MessageDigest.isEqual`) ;
- signature vérifiée **avant** de décoder la charge utile ;
- jeton **lié au code du document** et expirant en 300 s.

C'est correct. La seule réserve : le secret retombe sur `app.security.jwt.secret`, donc sur le
secret écrit en clair dans la source si rien n'est configuré, et il **réutilise la clé de signature
des JWT** pour un autre usage.

---

## 3. WebSocket · fuite de données entre comptes · **élevé**

`WebSocketSubscriptionInterceptor` :

```java
if (destination.startsWith("/topic/") && user == null) {
    throw new MessagingException("Authentication required for topic subscription");
}
```

C'est le **seul** contrôle : être authentifié. Aucun contrôle de rôle, aucun contrôle par
destination.

**Ce qu'un attaquant obtient** : n'importe quel membre du portail client, avec son propre jeton
valide, s'abonne à `/topic/admin/alerts` et reçoit en temps réel les alertes d'administration ·
notamment, depuis les lots de cette session :

- `SELF_SERVICE_PAYMENT_RECEIVED` · « *Joël Bikindou a réglé 25000 XAF depuis son espace client* »
- `CASH_PAYMENT_DECLARED` · nom du client, montant, facture, réservation
- les alertes de caisse (anomalies, écarts) et les événements de sécurité des portefeuilles

C'est-à-dire le flux de paiement de toute la maison, nominatif, en direct, pour tout client
connecté. `/topic/inventory/alerts` et `/topic/support/tickets/` sont dans le même cas.

### W2 · Le type de jeton et la révocation ne sont pas vérifiés à la connexion · **moyen**

`WebSocketAuthInterceptor` appelle `jwtService.extractEmail(token)`, qui **vérifie bien la
signature et l'expiration** · ce point est correct. Mais il ne vérifie ni le type de jeton, ni la
révocation de session :

- un **jeton de rafraîchissement** (7 jours) ouvre une session WebSocket authentifiée ;
- un **jeton de vérification** d'e-mail aussi ;
- une session révoquée par déconnexion reste acceptée · `isSessionRevoked` n'est consulté que par
  `JWTFilter`, et `/ws/**` en est explicitement exclu (`shouldNotFilter`).

`registerStompEndpoints` accepte par ailleurs toutes les origines (`setAllowedOriginPatterns("*")`).

---

## 4. Vol et rejeu de jeton JWT

### J1 · Secret de signature écrit en clair dans la source · **critique**

```java
@Value("${app.security.jwt.secret:<ancien secret JWT, 62 car.>}")
```

Un secret HMAC de 62 caractères, valide, dans le dépôt. La production le surcharge, mais :

- toute autre instance (dev, recette, poste d'un prestataire) signe avec ce secret ;
- **quiconque a lu le dépôt peut forger un jeton valide** pour ces instances, avec les rôles qu'il
  choisit · le claim `roles` est dans le jeton ;
- il est dans l'historique git, donc le retirer ne l'invalide pas ;
- il sert aussi, par repli, de clé aux jetons d'aperçu de document (§2.8).

### J2 · Durée de vie de 25 heures par défaut · **élevé**

`access-token-expiration-ms` vaut **90 000 000 ms** dans le code, soit 25 heures · le commentaire
juste au-dessus dit « *Remettre 900000 millisecond* ». La production surcharge à 15 minutes, et
`CLAUDE.md` annonce 15 minutes. Partout ailleurs, un jeton volé reste exploitable une journée.

### J3 · Le type de jeton n'est pas exigé sur l'API · **moyen**

`JWTFilter.processToken` appelle `extractEmail` sans vérifier `isAccessToken(token)`. Les méthodes
existent (`isAccessToken`, `isRefreshToken`, `isVerificationToken`) et ne sont appelées nulle part
dans le filtre. Un jeton de rafraîchissement, valable **7 jours**, authentifie donc n'importe quel
appel d'API · ce qui annule l'intérêt d'un jeton d'accès court.

### J4 · Rien ne lie un jeton à son porteur · **moyen**

Pas d'empreinte de client, pas d'adresse, pas de `jti` consultable. Un jeton intercepté est
rejouable depuis n'importe où jusqu'à son expiration.

### J5 · Ce qui est correct, et qu'il faut garder

- La signature **est** vérifiée (`verifyWith` + `parseSignedClaims`) · pas de `none`, pas de
  confusion d'algorithme, pas de décodage sans vérification.
- L'expiration est contrôlée à chaque extraction de claim.
- Un secret trop court est dérivé en SHA-256 au lieu d'être accepté tel quel.
- La révocation de session est consultée sur chaque requête d'API.
- La rotation du jeton de rafraîchissement est correcte (§2.1).
- **Aucun jeton ni secret n'apparaît dans les journaux** · vérifié sur l'ensemble des appels de
  journalisation.

---

## 5. Injections

### C1 · Injection de commande · **aucun vecteur · vérifié**

Aucun `ProcessBuilder`, `Runtime.getRuntime().exec`, `/bin/sh` ni `bash -c` dans
`src/main/java`. Aucun `ObjectInputStream`/`readObject`, aucun `SpelExpressionParser`, aucun
`ScriptEngine`. Rien n'exécute de commande système ni d'expression fournie par l'utilisateur.

### C2 · Injection SQL · **aucun vecteur · vérifié**

Toutes les requêtes natives examinées utilisent des blocs de texte avec paramètres nommés
(`:param`). Aucune concaténation de valeur d'utilisateur dans une requête.

### C3 · Traversée de chemin par l'extension du fichier · **élevé**

`DocumentStorageService` remplace bien le nom du fichier par un UUID · mais garde l'**extension**
telle quelle :

```java
private String extractExtension(String filename) {
    int lastDot = filename.lastIndexOf('.');
    return lastDot < 0 ? "" : filename.substring(lastDot + 1).toLowerCase();
}
...
String storedFileName = UUID.randomUUID() + "." + extension;
Path target = rootPath.resolve(objectKey).normalize();   // normalisé, mais jamais confiné
```

L'extension n'est ni filtrée, ni restreinte à un alphabet. Un nom de fichier contenant des points et
des séparateurs produit une « extension » porteuse de `../`, et `normalize()` la résout **sans
vérifier que la cible reste sous `rootPath`**.

**Ce qu'un attaquant obtient** : écriture d'un fichier dont il contrôle le contenu à un chemin
arbitraire accessible au processus, sur le stockage `FILESYSTEM`. Sur `MINIO`/`S3`, écriture hors du
préfixe prévu.

Les validations d'upload existantes (`DocumentUploadValidator`, `DocumentSpaceService`,
`DocumentSecurityService`) portent sur le **type MIME**, pas sur le nom · un `image/jpeg`
parfaitement valide peut porter un nom hostile.

### C4 · SSRF par les webhooks · **faible**

`WebhookService` appelle des URL enregistrées par un administrateur, sans validation d'hôte · une
URL interne (`169.254.169.254`, `localhost`, réseau privé) est appelable. Réservé aux
administrateurs, donc faible.

---

## 6. CSRF · correction du premier passage

**Le premier passage concluait « pas un vecteur ». C'est faux dans un cas**, mis au jour par la
vérification de §2.4.

Pour l'**API** la conclusion tient : `csrf(disable)`, session `STATELESS`, jeton lu **uniquement**
dans l'en-tête `Authorization`, CORS en `allowCredentials(false)`. Aucune autorité ambiante · le
CSRF ne s'y applique pas.

Mais il existe **un identifiant par cookie** : `bokati_scanner`, `path=/`, un an, et
`GET /public/bookings/check-in/scan/{token}` l'honore pour effectuer un check-in. Là, le CSRF
s'applique pleinement, et il est désactivé. Impact métier faible (un check-in forcé), mais
l'invariant sur lequel reposait toute la conclusion est déjà rompu dans le code.

Le formulaire de réinitialisation de mot de passe (`POST /auth/password-reset/form`, rendu côté
serveur) n'est, lui, pas vulnérable : le secret est le jeton **dans le corps** de la requête, pas
une autorité ambiante.

### CORS trop large · **faible**

`allowedOriginPatterns("*")`, `allowedHeaders("*")`, `exposedHeaders("*")`. Sans identifiants c'est
peu exploitable, mais cela entretient l'ambiguïté ci-dessus.

### Plafond de requêtes en mémoire du processus · **moyen**

`RateLimitingFilter` garde ses compteurs dans une `ConcurrentHashMap` locale :

- avec plusieurs instances, le plafond est multiplié par leur nombre ;
- un redémarrage remet tous les compteurs à zéro ;
- la clé est l'adresse issue de `X-Forwarded-For`, **non validée** · un en-tête forgé donne un
  compteur neuf à chaque requête, ce qui annule le plafond pour qui le sait ;
- et il ne couvre **rien** hors de `ApiPath.V1` (§2.6).

Le verrouillage de compte après 5 échecs, lui, est en base · c'est la vraie protection du login.

---

## 7. Plan de correction, dans l'ordre de livraison

### Lot 1 · Ce qui est exploitable maintenant (à livrer seul, en premier)

1. **Refermer `bootstrap-admin`** (§0). Rétablir le garde-fou commenté, et ne pas s'en contenter :
   l'amorçage ne doit pas être une route HTTP publique. Soit il exige un secret d'amorçage à usage
   unique venu de l'environnement, soit il devient une commande d'administration hors HTTP.
   **Et vérifier en production quels comptes `ADMIN` existent** · la route est ouverte depuis
   longtemps.
2. **Supprimer l'auto-admin** (A1). Pas le passer à `false` par défaut · le supprimer. Si un
   raccourci de développement est voulu, il doit être derrière `@Profile("dev")` et refuser de se
   charger avec un profil `prod`.
3. **Fusionner les deux listes de chemins publics** (§2.5) **dans le même lot que 2** · certaines
   routes ne sont publiques aujourd'hui que grâce à l'auto-admin, leur comportement change quand il
   disparaît.
4. **Retirer le secret JWT du code** (J1) et faire **échouer le démarrage** si
   `app.security.jwt.secret` est absent. Puis **rotationner le secret** : il est dans l'historique
   git, donc il est public. La rotation invalide tous les jetons en circulation · heure creuse.
5. **Inverser tous les défauts dangereux** (§0) et ajouter un **contrôle au démarrage** qui refuse
   de démarrer en profil `prod` avec une configuration manifestement non sûre : secret par défaut,
   auto-admin actif, jeton de plus d'une heure.

### Lot 2 · Les jetons

6. **Exiger le type de jeton** (J3, W2) : `isAccessToken` sur `JWTFilter` et sur le WebSocket.
7. **Vérifier la révocation de session au WebSocket** (W2).
8. Donner au jeton d'aperçu de document **sa propre clé** (§2.8).

### Lot 3 · WebSocket et surface publique

9. **Autoriser par destination** (§3) : `/topic/admin/**` et `/topic/inventory/**` réservés aux
   rôles du realm d'administration, `/topic/support/tickets/{id}` au personnel et au demandeur,
   `/queue/**` au destinataire. C'est le constat le plus directement exploitable par un client
   légitime aujourd'hui.
10. **Étendre le plafond de requêtes hors `ApiPath.V1`** (§2.6) · `/verify/**`, `/images/**`, et un
    plafond propre à `POST /verify/doc/{n}/compare` qui accepte un fichier sans authentification.
11. **Plafonds propres** à `/auth/unlock-account` et `/auth/email/verify/resend` (§2.1), et aux deux
    écritures anonymes `/public/crm/leads` et `/public/events/*/registrations` (§2.3).
12. **Durcir le cookie scanner** (§2.4) : `Secure`, `SameSite=Lax`, `path` restreint au chemin de
    check-in, durée courte, valeur **dérivée** et non la clé en clair, comparaison en temps
    constant, et le check-in en `POST` et non en `GET`.
13. Corriger le montage de `/public/quotes/sign`, hors `ApiPath.V1` et donc inaccessible (§2.5).

### Lot 4 · Le reste

14. **Confiner les écritures de fichier** (C3) : alphabet restreint pour l'extension
    (`[a-z0-9]{1,8}`), et refus si la cible normalisée ne commence pas par `rootPath`.
15. **Plafond de requêtes partagé** · Redis est déjà dans l'infrastructure. Et ne faire confiance à
    `X-Forwarded-For` que pour le nombre de sauts de proxy connus.
16. Resserrer CORS sur les origines réelles du frontend.
17. **Écrire l'invariant sur les cookies** : aucun cookie porteur d'autorité hors du flux de
    check-in, et un test qui le tient · c'est ce qui garde le CSRF hors sujet pour l'API.
18. Allowlist d'hôtes pour les webhooks (C4).

### Hors code

19. **Auditer les comptes `ADMIN` en production** (conséquence de §0).
20. Révoquer la clé API PawaPay exposée (déjà signalé, toujours en attente).
21. Rotationner `APP_JWT_SECRET`, `TOKEN_HASH_SECRET`, la clé scanner, et les identifiants qui ont
    pu transiter par le dépôt.

---

## 8. Ce que cet audit n'a toujours pas couvert

- le cycle de vie des jetons de réinitialisation de mot de passe · usage unique, stockage haché,
  invalidation des autres sessions après changement ;
- `/public/support/csat` · un jeton lie-t-il la réponse au ticket ?
- l'autorisation au niveau objet (IDOR) sur l'ensemble des contrôleurs · trois cas ont été trouvés
  et corrigés cette session (lecture d'un dépôt mobile money, vues de portefeuille, dossier KYC) en
  travaillant sur autre chose, ce qui suggère qu'il en reste ;
- les dépendances et leurs vulnérabilités connues · aucun scan n'a été lancé ;
- la configuration de l'hébergement, du SGBD et du stockage objet ;
- les en-têtes de sécurité (CSP) des pages rendues côté serveur, au-delà du formulaire de
  réinitialisation qui en a déjà.
