# Audit de sécurité · Bokati Cowork

Audit du code au 2 octobre 2026, branche `feature/payment-facturation`. Portée : intrusion
externe, CSRF, vol et rejeu de jeton JWT, accès administrateur par un tiers, injection de commande,
et ce que la lecture a fait apparaître d'autre.

Chaque constat porte une **gravité**, ce qu'un attaquant obtient, et ce qui le rend possible. Le
plan de correction est en §6, dans l'ordre où il faut le livrer.

---

## 0. Le constat qui gouverne tous les autres

**La sécurité de la production ne tient qu'à `application-prod.yml`.** Les valeurs écrites en dur
dans le code Java sont systématiquement les valeurs **dangereuses** ; ce sont les surcharges du
fichier de profil qui les rendent sûres.

| Réglage | Défaut dans le code Java | Surcharge en prod |
|---|---|---|
| `app.security.auto-admin.enabled` | **`true`** · authentifie en admin sans jeton | `false` |
| `app.security.jwt.secret` | **secret réel écrit en clair dans la source** | `${APP_JWT_SECRET}` |
| `app.security.jwt.access-token-expiration-ms` | **90 000 000 ms (25 heures)** | `900000` (15 min) |
| `app.documents.public-preview-enabled` | `true` | non surchargé |
| `bokati.payment.pawaypay.test-endpoint-enabled` | `false` | `false` |

Conséquence directe : **un profil Spring absent ou mal nommé au démarrage transforme la production
en administration ouverte à tous**, avec un secret JWT public et des jetons valables 25 heures. Il
n'existe aucun garde-fou qui empêche l'application de démarrer dans cet état.

C'est ce renversement qu'il faut corriger d'abord : le défaut du code doit être le réglage sûr, et
c'est l'ouverture qui doit être explicite.

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

**Ce qu'un attaquant obtient** : avec `auto-admin.enabled = true`, une requête **sans aucun
en-tête d'authentification** est traitée comme venant de l'administrateur. Toute l'API
d'administration, tous les clients, tous les paiements, tous les documents KYC. Aucune
authentification, aucune trace nominative.

**Pourquoi c'est encore un constat critique alors que la prod met `false`** :

- le défaut du code est `true`, et le défaut du profil `dev` est `true` ;
- un seul réglage sépare la production d'une ouverture totale ;
- un `SPRING_PROFILES_ACTIVE` absent, un fichier de profil non chargé, une erreur de nom de
  variable, et l'ouverture est silencieuse · rien dans les journaux ne la signale comme anormale ;
- le journal d'audit attribue alors **toutes** les actions à `admin@bokati.com`, ce qui rend
  l'incident irretraçable après coup.

Un mécanisme de confort de développement ne doit pas pouvoir devenir une porte de production.

### A2 · Un rôle d'administration suffit, la permission n'est pas toujours exigée · **moyen**

`AdminApiAuthorizationManager` refuse par défaut ce qu'aucune règle de chemin ne couvre · c'est le
bon sens par défaut, et il est bien là. Mais `SecurityConfig` place avant lui une liste de chemins
`permitAll` qui court-circuite entièrement ce contrôle (§2).

### A3 · `/actuator/metrics` est déclaré exposé · **faible**

`management.endpoints.web.exposure.include: health,info,metrics`. Seuls `/actuator/health` et
`/actuator/info` sont en `permitAll`, donc `metrics` retombe sur `anyRequest().authenticated()`.
`health.show-details: never` est correct. Le constat n'est sérieux **que combiné à A1** : avec
l'auto-admin, « authentifié » ne veut plus rien dire.

---

## 2. Intrusion externe · la surface publique

Tout ce qui est joignable sans jeton, d'après `SecurityConfig` et `JWTFilter.isPublicApiRequest` ·
les deux listes doivent rester identiques et **ne le sont pas exactement**.

| Chemin | Gravité | Remarque |
|---|---|---|
| `/auth/login`, `/refresh`, `/register`, `/ott/**`, `/password-reset/**`, `/unlock-account*`, `/email/verify/resend` | attendu | Plafonnés par `RateLimitingFilter` (voir R1) |
| `/payments/mobile-money/pawa(y)pay/callback`, `/refund-callback`, `/return` | attendu | Durcis dans le lot mobile money · signature ou relecture du statut |
| `/client/catalog/plans/**` | attendu | Catalogue public |
| `/shares/**` | **à vérifier** | Non examiné dans cet audit · partage par jeton, à confirmer |
| `/verify/**` | **à vérifier** | Idem |
| `/documents/*/signed-preview` | **moyen** | Ouvert par défaut (`public-preview-enabled: true`), **non surchargé en prod** · la robustesse repose entièrement sur la signature du `?token=`, qui n'a pas été auditée ici |
| `/images/**` | faible | Ressources de marque |
| `/ws/**` | **élevé** | Voir §3 |
| `/admin/provisioning/bootstrap-admin` | **élevé** | Création d'un administrateur · à confirmer qu'elle se referme après le premier usage |
| `/countries` | faible | Référentiel |

**Divergence des deux listes** : `SecurityConfig` ouvre `/admin/provisioning/bootstrap-admin`,
`/public/**`, `/countries` et `/client/catalog/plans` ; `JWTFilter.isPublicApiRequest` ne connaît
ni `bootstrap-admin`, ni `/public/**`, ni `/countries`. Deux listes qui expriment la même intention
et divergent déjà : la prochaine ouverture sera oubliée dans l'une des deux.

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

### W2 · Le type de jeton n'est pas vérifié à la connexion · **moyen**

`WebSocketAuthInterceptor` appelle `jwtService.extractEmail(token)`, qui **vérifie bien la
signature et l'expiration** (`Jwts.parser().verifyWith(...)`) · ce point est correct. Mais il ne
vérifie **ni le type de jeton, ni la révocation de session** :

- un **jeton de rafraîchissement** (7 jours) ouvre une session WebSocket authentifiée ;
- un **jeton de vérification** d'e-mail aussi ;
- une session révoquée par déconnexion reste acceptée · `UserSessionService.isSessionRevoked`
  n'est consulté que par `JWTFilter`, pas ici.

`/ws/**` est d'ailleurs explicitement exclu de `JWTFilter` (`shouldNotFilter`), donc rien ne
rattrape ces contrôles ailleurs.

---

## 4. Vol et rejeu de jeton JWT

### J1 · Secret de signature écrit en clair dans la source · **critique**

```java
@Value("${app.security.jwt.secret:Q7mP2xL9vB4nH6sT1yK8dF5wR3cZ0aEQ7mP2xL9vB4nH6sT1yK8dF5wR3cZ0aE}")
```

Un secret HMAC de 62 caractères, valide, dans le dépôt. La production le surcharge
(`${APP_JWT_SECRET}`, sans défaut, donc obligatoire) · mais :

- toute autre instance (dev, recette, poste d'un prestataire) signe avec ce secret ;
- **quiconque a lu le dépôt peut forger un jeton valide** pour ces instances, avec les rôles qu'il
  choisit · le claim `roles` est dans le jeton ;
- il est dans l'historique git, donc le retirer ne suffit pas à l'invalider.

### J2 · Durée de vie de 25 heures par défaut · **élevé**

`access-token-expiration-ms` vaut **90 000 000 ms** dans le code, soit 25 heures · le commentaire
juste au-dessus dit « *Remettre 900000 millisecond* ». La production surcharge à 15 minutes, et
`CLAUDE.md` annonce 15 minutes. Partout ailleurs, un jeton volé reste exploitable une journée.

### J3 · Rien ne lie un jeton à son porteur · **moyen**

Le jeton ne porte ni empreinte de client, ni adresse, ni `jti` consultable. Un jeton intercepté est
rejouable depuis n'importe où jusqu'à son expiration. La révocation existe (`sessionId` +
`isSessionRevoked`) et c'est bien, mais elle suppose qu'on sache qu'il y a eu vol.

### J4 · Le type de jeton n'est pas exigé sur l'API · **moyen**

`JWTFilter.processToken` appelle `extractEmail` sans vérifier `isAccessToken(token)`. Les méthodes
existent (`isAccessToken`, `isRefreshToken`, `isVerificationToken`) et ne sont pas appelées là où il
faut. Un jeton de rafraîchissement, valable **7 jours**, authentifie donc n'importe quel appel
d'API · ce qui annule l'intérêt d'un jeton d'accès court.

### J5 · Ce qui est correct, et qu'il faut garder

- La signature **est** vérifiée (`verifyWith` + `parseSignedClaims`) · pas de `none`, pas de
  confusion d'algorithme, pas de décodage sans vérification.
- L'expiration est contrôlée à chaque extraction de claim.
- Un secret trop court est dérivé en SHA-256 au lieu d'être accepté tel quel.
- La révocation de session est consultée sur chaque requête d'API.
- **Aucun jeton n'apparaît dans les journaux** · vérifié sur l'ensemble des appels de journalisation.

---

## 5. CSRF, injections, et le reste

### C1 · CSRF · **pas un vecteur aujourd'hui, mais rien ne le garantit demain**

`csrf(AbstractHttpConfigurer::disable)`, session `STATELESS`, et le jeton est lu **uniquement**
dans l'en-tête `Authorization`. CORS : `allowCredentials(false)`. Il n'existe donc **aucune
autorité ambiante** qu'un site tiers pourrait faire jouer · le CSRF ne s'applique
structurellement pas.

Cette sûreté repose sur un invariant **non écrit et non vérifié** : aucun cookie d'authentification.
Le jour où un jeton est posé en cookie « pour simplifier le frontend », toute l'API devient
vulnérable d'un coup, sans qu'aucun test ne change de couleur.

Le formulaire de réinitialisation de mot de passe (`POST /auth/password-reset/form`, rendu côté
serveur) est le seul formulaire HTML. Il n'est pas vulnérable : le secret est le jeton **dans le
corps** de la requête, pas une autorité ambiante. Un attaquant qui connaît ce jeton n'a pas besoin
de CSRF.

### C2 · CORS trop large · **faible**

`allowedOriginPatterns("*")`, `allowedHeaders("*")`, `exposedHeaders("*")`. Sans identifiants c'est
peu exploitable, mais cela autorise n'importe quelle page à lire les réponses de l'API avec un jeton
qu'elle aurait obtenu autrement, et cela entretient C1.

### C3 · Injection de commande · **aucun vecteur trouvé**

Aucun `ProcessBuilder`, `Runtime.getRuntime().exec`, `/bin/sh` ni `bash -c` dans
`src/main/java`. Aucun `ObjectInputStream`/`readObject`, aucun `SpelExpressionParser`, aucun
`ScriptEngine`. Rien n'exécute de commande système ni d'expression fournie par l'utilisateur.

### C4 · Injection SQL · **aucun vecteur trouvé**

Toutes les requêtes natives examinées utilisent des blocs de texte avec paramètres nommés
(`:param`) ou `?`. Aucune concaténation de valeur d'utilisateur dans une requête.

### C5 · Traversée de chemin par l'extension du fichier · **élevé**

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

### C6 · Le plafond de requêtes est en mémoire du processus · **moyen**

`RateLimitingFilter` garde ses compteurs dans une `ConcurrentHashMap` locale. Conséquences :

- avec plusieurs instances, le plafond est multiplié par leur nombre ;
- un redémarrage remet tous les compteurs à zéro ;
- la clé est l'adresse issue de `X-Forwarded-For`, **non validée** · un en-tête forgé donne un
  compteur neuf à chaque requête, ce qui annule le plafond pour qui le sait.

Cela concerne directement le brute-force de `/auth/login` et des codes à usage unique. Le
verrouillage de compte (`failed-login.max-attempts: 5`) reste, lui, en base · c'est la vraie
protection.

### C7 · SSRF par les webhooks · **faible**

`WebhookService` appelle des URL enregistrées par un administrateur. Pas de validation d'hôte, donc
une URL interne (`169.254.169.254`, `localhost`, réseau privé) est appelable. Réservé aux
administrateurs, donc faible · mais à garder en tête.

---

## 6. Plan de correction, dans l'ordre de livraison

### Lot 1 · Ce qui ouvre la porte en grand (à livrer seul, en premier)

1. **Supprimer l'auto-admin** (A1). Pas le passer à `false` par défaut · le supprimer. Un confort
   de développement qui authentifie en administrateur ne doit pas exister dans le binaire de
   production. Si un raccourci de développement est voulu, il doit être derrière un profil
   `@Profile("dev")` et refuser de se charger avec un profil `prod`.
2. **Retirer le secret JWT du code** (J1) et faire **échouer le démarrage** si
   `app.security.jwt.secret` est absent. Puis **rotationner le secret** : il est dans l'historique
   git, donc il est public. La rotation invalide tous les jetons en circulation · à faire à une
   heure creuse.
3. **Inverser tous les défauts dangereux** (§0) : `auto-admin` supprimé, jeton d'accès à 15 min
   dans le code, `public-preview-enabled` à `false` par défaut.
4. **Un contrôle au démarrage** qui refuse de démarrer si une configuration manifestement non sûre
   est active en profil `prod` · secret par défaut, auto-admin, jeton de plus d'une heure.

### Lot 2 · Les jetons

5. **Exiger le type de jeton** (J4, W2) : `isAccessToken` sur `JWTFilter`, et sur le WebSocket.
6. **Vérifier la révocation de session au WebSocket** (W2).
7. Rapprocher la durée du jeton d'accès de celle annoncée, et documenter l'écart s'il en reste.

### Lot 3 · WebSocket et surface publique

8. **Autoriser par destination** (§3) : `/topic/admin/**` et `/topic/inventory/**` réservés aux
   rôles du realm d'administration, `/topic/support/tickets/{id}` au personnel et au demandeur,
   `/queue/**` au destinataire. C'est le constat le plus directement exploitable aujourd'hui par un
   client légitime.
9. **Une seule liste de chemins publics** (§2), partagée par `SecurityConfig` et `JWTFilter`, avec
   un test qui échoue si les deux divergent.
10. Confirmer `/shares/**`, `/verify/**`, `signed-preview` et `bootstrap-admin` · non audités ici,
    chacun mérite sa lecture.

### Lot 4 · Le reste

11. **Confiner les écritures de fichier** (C5) : alphabet restreint pour l'extension
    (`[a-z0-9]{1,8}`), et refus si la cible normalisée ne commence pas par `rootPath`.
12. **Plafond de requêtes partagé** (C6) · Redis est déjà dans l'infrastructure. Et ne faire
    confiance à `X-Forwarded-For` que pour le nombre de sauts de proxy connus.
13. Resserrer CORS (C2) sur les origines réelles du frontend.
14. **Écrire l'invariant « aucun cookie d'authentification »** (C1) et le tenir par un test · c'est
    ce qui garde le CSRF hors sujet.
15. Allowlist d'hôtes pour les webhooks (C7).

### Hors code

16. Révoquer la clé API PawaPay exposée (déjà signalé, toujours en attente).
17. Rotationner `APP_JWT_SECRET`, `TOKEN_HASH_SECRET` et les identifiants qui ont pu transiter par
    le dépôt.

---

## 7. Ce que cet audit n'a pas couvert

À traiter dans un second passage, pour ne pas donner une fausse impression de complétude :

- la signature du `?token=` de `signed-preview`, et les flux `/shares/**` et `/verify/**` ;
- `bootstrap-admin` · se referme-t-il après le premier administrateur ?
- le cycle de vie des jetons de réinitialisation de mot de passe (usage unique, stockage haché,
  invalidation des autres sessions après changement) ;
- l'autorisation au niveau objet (IDOR) sur l'ensemble des contrôleurs · plusieurs cas ont été
  trouvés et corrigés cette session (lecture d'un dépôt mobile money, vues de portefeuille, dossier
  KYC), ce qui suggère qu'il en reste ;
- les dépendances et leurs vulnérabilités connues · aucun scan n'a été lancé ;
- la configuration de l'hébergement, du SGBD et du stockage objet ;
- les en-têtes de sécurité pour les pages rendues côté serveur (CSP) au-delà de celles déjà posées
  sur le formulaire de réinitialisation.
