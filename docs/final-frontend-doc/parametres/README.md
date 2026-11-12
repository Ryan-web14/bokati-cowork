# parametres : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Deux ensembles sans lien entre eux :

- **Paramètres de la plateforme** (`/sni/api/v1/admin/settings`, 5 routes) : l'écran de back-office qui modifie, sans toucher au fichier `application.yml`, les 237 réglages du catalogue `src/main/resources/settings/platform-settings.tsv`, plus les 13 réglages de la politique d'abonnement. Réservé aux rôles `SUPER_ADMIN` et `ADMIN`. Chaque changement est journalisé.
- **Préférences d'un membre** (`/sni/api/v1/members/{memberId}/settings`, 4 routes) : langue, fuseau, visibilité, préférences de facturation et de portefeuille, préférences de notification d'un membre, consultées et modifiées depuis le back-office (fiche membre). Aucune route du portail membre n'y donne accès.

Modules liés : `subscription` (fournit la politique d'abonnement), `socle` (journal `GET /sni/api/v1/admin/settings-audit-logs`), `client` (le membre visé), `ressource` (horaires globaux), `marketplace`, `support`, `company`, `booking` (consommateurs des réglages à effet immédiat).

## Notions

| Objet | Identifiant exposé | Stockage | Remarques |
| --- | --- | --- | --- |
| Réglage de plateforme (`SettingView`) | `key`, texte avec des points, par exemple `bokati.marketplace.payment-window-minutes` | ligne de `platform_setting` seulement si la valeur a été modifiée ; sinon la valeur vient du fichier de configuration | Défini par le catalogue TSV (colonnes : clé, domaine, type, effet, défaut, libellé), lu par `src/main/java/com/sni/bokaticowork/core/settings/platform/PlatformSettingCatalog.java` |
| Réglage fourni par un module (`ProvidedSetting`) | `key` préfixée `subscription.policy.` | table de la politique d'abonnement (module `subscription`) | 13 clés, déclarées dans `src/main/java/com/sni/bokaticowork/features/subscription/lifecycle/service/SubscriptionPolicySettings.java:49`. Seul fournisseur existant. |
| Domaine (`SettingDomain`) | nom d'énumération (`BOOKING`, `SECURITY`…) | énumération `src/main/java/com/sni/bokaticowork/core/settings/platform/SettingDomain.java` | 27 domaines, chacun avec un libellé français et un indicateur `adminEditable` |
| Changement (`ChangeView`) | aucun | `setting_audit_logs` | `oldValue`, `newValue`, `reason`, `changedAt`. Pas de champ auteur : l'auteur est en tête de `reason` (voir plus bas). |
| Préférences d'un membre (`MemberSettingsResponse`) | `memberId` du membre (code lisible, pas l'id numérique) | `user_settings`, une ligne par membre, créée au premier `PATCH` | Sans ligne, le `GET` renvoie des valeurs par défaut sans rien enregistrer |
| Préférence de notification (`NotificationPreferenceResponse`) | couple `eventType` + `channel` | `notification_preferences`, une ligne par couple modifié | 7 événements x 5 canaux = 35 entrées toujours renvoyées ; un couple sans ligne est activé |

### Domaines et droits

`adminEditable = true` (ouverts à `ADMIN`) : `BOOKING`, `RESOURCES`, `SUBSCRIPTIONS`, `PASSES`, `MARKETPLACE`, `PROMOTIONS`, `DOMICILIATION`, `CONTRACTS`, `SUPPORT`, `TASKS`, `CRM`, `EVENTS`, `PORTAL`.

`adminEditable = false` (`SUPER_ADMIN` seul) : `COMPANIES`, `WALLET`, `PAYMENT`, `BILLING`, `COMPLIANCE`, `DOCUMENTS`, `LOCALE`, `SECURITY`, `NOTIFICATIONS`, `WEBHOOKS`, `OUTBOX`, `GRAPH_API`, `WEBSOCKET`, `ENVIRONMENT`.

Règle appliquée par `PlatformSettingsAdminService.mayAccess` (`src/main/java/com/sni/bokaticowork/core/settings/platform/PlatformSettingsAdminService.java:256`) : `ROLE_SUPER_ADMIN` voit tout ; `ROLE_ADMIN` voit les domaines ouverts ; tout autre rôle est refusé avant même le service par `@PreAuthorize("hasRole('SUPER_ADMIN') or hasRole('ADMIN')")` sur le contrôleur, même s'il porte `SYSTEM:SETTINGS`.

### Types de valeur

Toute valeur circule **en texte** (`value`, `defaultValue`, `oldValue`, `newValue`), quel que soit le type.

| `type` | Saisie à proposer | Contrôle serveur (`PlatformSettingValidator.java`) |
| --- | --- | --- |
| `BOOLEAN` | interrupteur | exactement `true` ou `false` |
| `INTEGER` | champ numérique entier | entier, `>= 0` ; une clé finissant par `delay-ms` (hors clés contenant `initial`) vaut au moins `1000` |
| `DECIMAL` | champ numérique décimal (point) | nombre `>= 0` |
| `CRON` | texte, avec aide sur le format cron Spring à 6 champs | `CronExpression.isValidExpression` |
| `EMAIL` | champ courriel | vide accepté, sinon forme `x@y.z` |
| `TIME` | heure | `LocalTime.parse` : `HH:mm` ou `HH:mm:ss` |
| `TIME_RANGE` | deux heures ou un champ `HH:mm-HH:mm` | vide accepté (jour fermé) ; sinon deux heures séparées par `-`, ouverture strictement avant fermeture |
| `TEXT` | texte | aucun contrôle hors `${` |
| `ENUM` | liste déroulante alimentée par `options` | contrôlé par le module fournisseur (seuls les réglages `subscription.policy.*` sont de ce type) |
| `SECRET` | affichage seul : « configuré » ou « non configuré » d'après `configured` | toute modification est refusée |

Répartition actuelle du catalogue : 132 `INTEGER`, 30 `TEXT`, 23 `CRON`, 20 `BOOLEAN`, 11 `SECRET`, 10 `EMAIL`, 6 `DECIMAL`, 4 `TIME_RANGE`, 1 `TIME`. Les 11 secrets sont dans `DOCUMENTS`, `SECURITY` et `WEBSOCKET`.

### Effet d'un changement

`effect` vaut `LIVE` (pris en compte sans redémarrage) ou `RESTART` (au prochain démarrage de l'application). Dans le catalogue, 327 clés sont `LIVE` et 34 `RESTART`. Les 13 clés `subscription.policy.*` sont toutes `LIVE`.

- Une clé `LIVE` est relue au moment où elle sert : une valeur métier (délai, seuil, adresse, URL) au prochain usage, une planification (`cron`, délai entre deux passages) au prochain passage du traitement.
- Les 34 clés `RESTART` : le stockage des documents, les secrets, CORS, le relais websocket, la langue, le fuseau horaire, l'aperçu public des documents, le plafond de requêtes Graph et les 7 délais initiaux (`*-initial-delay-ms`), qui ne servent qu'au démarrage.
- Une valeur estampillée à la création (date d'expiration d'un jeton, d'une offre, d'une attestation) ne change que pour les nouvelles créations.

### Paramètres sensibles

31 clés `LIVE` sont marquées sensibles (`sensitive = true`) : devise, code machine fiscal, numéros de téléphone, mode dev, sécurité des connexions et des jetons, frais et plafonds du portefeuille, plafond de caisse, niveau de conformité de la domiciliation. `risk` dit en une phrase ce qu'un changement risque.

- Avant d'enregistrer une clé sensible, afficher `risk` dans une confirmation.
- Chaque changement ou remise à zéro d'une clé sensible envoie, après enregistrement, un courriel à chaque super-administrateur actif et une alerte dans l'application (`SENSITIVE_SETTING_CHANGED`, module `SETTINGS`, sévérité `WARNING`) avec l'ancienne valeur, la nouvelle, l'auteur et le risque.

(Chemins relatifs à `src/main/java/com/sni/bokaticowork/`.)

Les horaires propres à une ressource (table `resource_opening_hours`) ne sont pas ici : ils relèvent du module `ressource`.

## Statuts et cycle de vie

Un réglage de plateforme n'a pas de champ statut, mais son état se lit dans `modified` et `restartPending`.

| État | Signification | On y arrive par | On en sort vers |
| --- | --- | --- | --- |
| Valeur du fichier (`modified = false`) | aucune ligne en base, `value = defaultValue` | état initial ; `DELETE /sni/api/v1/admin/settings/{key}` | Modifié, par `PUT` |
| Modifié (`modified = true`) | une ligne existe dans `platform_setting` ; `updatedBy` et `updatedAt` renseignés | `PUT /sni/api/v1/admin/settings/{key}` | Valeur du fichier, par `DELETE` ; nouvelle valeur, par `PUT` |
| En attente de redémarrage (`restartPending = true`) | réglage `RESTART` dont la valeur voulue diffère de celle lue au démarrage | `PUT` ou `DELETE` sur une clé `RESTART` | disparaît au redémarrage de l'application ; ne concerne plus que les 34 clés `RESTART` |

Pour les réglages `subscription.policy.*` : `modified` signifie « différent de la valeur par défaut de la politique » (pas « enregistré depuis l'écran »), `restartPending` vaut toujours `false`, `updatedBy` et `updatedAt` sont toujours `null` (`PlatformSettingsAdminService.java:234`).

Pour un secret : `value`, `defaultValue` toujours `null`, `editable = false`, `modified = false`, `configured` indique si une valeur est présente côté serveur (`PlatformSettingsAdminService.java:286`). `configured` est `null` pour tout réglage qui n'est pas un secret.

Les préférences d'un membre n'ont pas de cycle de vie.

## Parcours

### Ouvrir l'écran des paramètres

1. `GET /sni/api/v1/admin/settings/domains` ([détail](routes/PlatformSettingsController.md#r-get-sni-api-v1-admin-settings-domains)). Renvoie seulement les domaines accessibles au compte : 27 pour `SUPER_ADMIN`, 13 pour `ADMIN`. Chaque entrée : `domain`, `label` (à afficher), `adminEditable`, `settings` (nombre de réglages, fournis compris). Afficher la liste comme menu latéral ou onglets.
2. L'utilisateur choisit un domaine : `GET /sni/api/v1/admin/settings?domain=BOOKING` ([détail](routes/PlatformSettingsController.md#r-get-sni-api-v1-admin-settings)). Sans `domain`, tous les réglages accessibles reviennent (environ 250 pour `SUPER_ADMIN`). Un `ADMIN` qui demande un domaine fermé reçoit une liste vide, pas un `403`.
3. Pour chaque réglage, afficher `label`, la valeur `value` dans le contrôle adapté au `type`, un badge selon `effect` (« immédiat » ou « au redémarrage »), un badge « modifié » si `modified`, « redémarrage requis » si `restartPending`, la valeur par défaut `defaultValue` en indication, et `updatedBy` / `updatedAt` quand ils sont présents. Afficher `key` en petit : c'est l'identifiant à utiliser dans les appels. Les secrets s'affichent en lecture seule.

### Modifier un réglage

1. `PUT /sni/api/v1/admin/settings/{key}` avec `{ "value": "45", "reason": "Demande de l'exploitation" }` ([détail](routes/PlatformSettingsController.md#r-put-sni-api-v1-admin-settings-key)). La clé contient des points : la mettre telle quelle dans le chemin (encoder avec `encodeURIComponent` par sécurité). `value` est toujours une chaîne, même pour un nombre ou un booléen. `reason` est facultatif.
2. Le serveur retire les espaces autour de la valeur, la contrôle selon son type, l'enregistre, écrit une ligne au journal et renvoie le `SettingView` à jour. Remplacer la ligne de l'écran par cette réponse.
3. Si `effect = LIVE` : la valeur s'applique dès la validation de la transaction. Si `effect = RESTART` : `restartPending` passe à `true` ; afficher un bandeau indiquant qu'un redémarrage de l'application est nécessaire.
4. En cas de `400`, afficher `message` sous le champ : il est en français et cite le libellé, par exemple `« Délai de paiement d'une commande (minutes) » doit être un nombre entier.`

### Revenir à la valeur du fichier

1. `DELETE /sni/api/v1/admin/settings/{key}?reason=...` ([détail](routes/PlatformSettingsController.md#r-delete-sni-api-v1-admin-settings-key)). Ne proposer le bouton que si `modified = true`.
2. La ligne de `platform_setting` est supprimée, le journal note l'ancienne valeur et la valeur du fichier, et la réponse est le `SettingView` avec `modified = false`. Si le réglage n'avait pas été modifié, rien n'est écrit au journal et la réponse est la même.
3. Pour un réglage `subscription.policy.*`, la remise à zéro enregistre la valeur par défaut de la politique (passe par le même contrôle qu'un `PUT`).

### Consulter l'historique d'un réglage

1. `GET /sni/api/v1/admin/settings/{key}/history` ([détail](routes/PlatformSettingsController.md#r-get-sni-api-v1-admin-settings-key-history)). Renvoie les 100 derniers changements, du plus récent au plus ancien.
2. Chaque entrée : `oldValue`, `newValue`, `changedAt`, et `reason` sous la forme `<auteur> · <motif>` ou `<auteur>` seul. L'auteur est le nom de connexion du compte (`authentication.getName()`). Le frontend peut séparer sur ` · ` pour afficher l'auteur à part.
3. Le journal transversal, avec filtre par clé, est dans le module `socle` : `GET /sni/api/v1/admin/settings-audit-logs` (droit `SYSTEM:AUDIT`).

### Politique d'abonnement

Ses 13 réglages apparaissent dans le domaine `SUBSCRIPTIONS` et suivent exactement les parcours ci-dessus. Deux sont de type `ENUM` avec `options` :

- `subscription.policy.proration-policy` : `DAILY`, `MONTH_STARTED`, `NONE`.
- `subscription.policy.early-termination-formula` : `NONE`, `FIXED_FEE`, `PERCENT_OF_REMAINING`, `REMAINING_PERIODS`.

Les autres routes `GET` / `PUT /sni/api/v1/subscriptions/lifecycle/policy` du module `subscription` existent toujours, mais un `PUT` par elles n'écrit rien au journal des paramètres : l'écran doit passer par `/sni/api/v1/admin/settings`.

### Préférences d'un membre (fiche membre, back-office)

1. `GET /sni/api/v1/members/{memberId}/settings` ([détail](routes/MemberSettingsController.md#r-get-sni-api-v1-members-memberid-settings)), où `memberId` est le code lisible du membre. Renvoie langue (`langage`, défaut `fr`), fuseau (`timeZone`, défaut `Africa/Brazzaville`), `showNameOnDisplay`, `showEmail`, `showPhone` (défaut `false`), `billingPref` (défaut `billingEntityType = PERSONAL`), `walletPref` (dont `receiptPreference`, défaut `EVERY_TRANSACTION`), et `notificationPref` (les 35 couples, activés par défaut).
2. Modifier : `PATCH /sni/api/v1/members/{memberId}/settings?reason=...` ([détail](routes/MemberSettingsController.md#r-patch-sni-api-v1-members-memberid-settings)) avec un `Idempotency-Key`. N'envoyer que les champs à changer : un champ absent ou `null` garde sa valeur. Une chaîne vide retire `billingEmail`, `billingCompanyName` ou `billlingAddress` ; `0` retire un plafond ou un montant de `walletPref`. `notificationPref` accepte une liste de `{ eventType, channel, enabled }` appliquée dans la même requête. Chaque champ réellement changé produit une ligne dans `setting_audit_logs` (clé `langage`, `showEmail`, `billingEmail`, `walletSpendingDailyLimit`, `notification.INVOICE_ISSUED.EMAIL`…).
3. Préférences de notification : `GET /sni/api/v1/members/{memberId}/settings/notifications`, puis une bascule par couple avec `PATCH /sni/api/v1/members/{memberId}/settings/notifications` et `{ "eventType": "INVOICE_ISSUED", "channel": "EMAIL", "enabled": false }` ([détail](routes/MemberSettingsController.md#r-patch-sni-api-v1-members-memberid-settings-notifications)). Les trois champs sont exigés (`400` sinon). La réponse est la liste complète des 35 couples : la substituer à l'affichage. Présenter une grille événements x canaux.

### Préférences d'un membre (portail, le membre lui-même)

Mêmes corps et mêmes réponses que les routes du back-office, pour le membre connecté (rôle membre et accès au portail), sans `memberId` dans le chemin ni `Idempotency-Key` exigé. Le journal note « Portail membre » comme motif.

| Route | Usage |
| --- | --- |
| `GET /sni/api/v1/client/settings` | lire ses préférences |
| `PATCH /sni/api/v1/client/settings` | les modifier (mêmes règles de `PATCH` partiel) |
| `GET /sni/api/v1/client/settings/notifications` | grille des notifications |
| `PATCH /sni/api/v1/client/settings/notifications` | basculer un couple |

### Effet des préférences dans les autres modules

- **Notifications** : un couple coupé arrête l'envoi automatique correspondant : confirmation, annulation, rappel, échec de réservation ; envoi d'une facture ; reçu ou confirmation de paiement ; paiement échoué. Le canal `IN_APP` coupe la notification dans l'application. Ne se coupent jamais : relances d'impayés et rappels d'échéance, sécurité (codes, mots de passe, connexions), contrats, conformité. Un envoi demandé par un administrateur (« renvoyer ») part toujours. La facture et le reçu restent dans le portail.
- **Adresse de facturation** : `billingPref.billingEmail` reçoit les factures, reçus et messages de facturation du membre, à la place du courriel du compte. Une organisation garde son propre courriel de facturation.
- **Fuseau** : les dates et heures des courriels et reçus du membre sont données dans `timeZone`.
- **Portefeuille** : `spendingDailyLimit` et `spendingWeeklyLimit` sont des plafonds personnels de dépense (paiements, transferts sortants, retraits), en plus des limites de la plateforme ; un dépassement répond `400` avec un message clair. `receiptPreference = INVOICE_ONLY` arrête le reçu par courriel à chaque opération du portefeuille.
- **Langue** : sert au formatage, pas à la traduction ; les messages restent en français.
- `autoTopupEnabled`, `autoTopupThreshold`, `autoTopupAmount` et les `show*` sont enregistrés mais ne pilotent encore rien (pas de rechargement automatique, pas d'annuaire des membres).

## Règles et erreurs à refléter dans l'écran

Paramètres de la plateforme :

| Situation | Statut | Message |
| --- | --- | --- |
| Clé absente du catalogue et des fournisseurs | `404` `RESOURCE_NOT_FOUND` | `Paramètre inconnu : <clé>` |
| `ADMIN` sur un domaine fermé (`PUT`, `DELETE`, `history`) | `403` `FORBIDDEN` | `Ce paramètre est réservé au super administrateur.` |
| Rôle autre que `SUPER_ADMIN` / `ADMIN` | `403` | refus de la sécurité de méthode (forme non vérifiée) |
| Valeur contenant `${` | `400` `BAD_REQUEST` | `« <libellé> » ne peut pas contenir de référence à une autre valeur.` |
| `PUT` sur un secret | `400` | `Un secret ne se modifie pas ici : il reste dans la configuration du serveur.` |
| Booléen invalide | `400` | `« <libellé> » doit valoir true ou false.` |
| Entier invalide, négatif, cadence trop courte | `400` | `doit être un nombre entier.`, `ne peut pas être négatif.`, `doit être d'au moins 1000 ms.` |
| Décimal invalide | `400` | `doit être un nombre.` |
| Cron invalide | `400` | `n'est pas une planification valide.` |
| Courriel invalide | `400` | `n'est pas une adresse e-mail valide.` |
| Heure ou plage invalide | `400` | `doit être une heure au format HH:mm.` ; `doit être une plage HH:mm-HH:mm, ouverture avant fermeture.` |
| `reason` du `PUT` au-delà de 500 caractères | `400` `VALIDATION_FAILED` | validation `@Size(max = 500)` |
| Politique d'abonnement : pourcentage hors 0 à 100 | `400` | `Le pourcentage (<quoi>) est entre 0 et 100` |
| Politique : nombre de jours négatif | `400` | `Le nombre de jours (<quoi>) ne peut pas être négatif` |
| Politique : validité de devis inférieure à 1 | `400` | `La validité d'un devis est d'au moins un jour` |
| Politique : formule `FIXED_FEE` sans montant fixe (champ vide) | `400` | `Une pénalité fixe demande un montant` |
| Politique : valeur `ENUM` hors liste | `400` | `« <libellé> » doit valoir <options séparées par des virgules>.` |

Pour `early-termination-fixed-fee`, une valeur vide signifie « pas de montant fixe ».

Préférences d'un membre :

- Membre inconnu ou supprimé : `404`, message `Member not found` (en anglais).
- `billingCompanyName` : 150 caractères au plus, `billlingAddress` : 255 caractères au plus ; `billingEmail` : une ou plusieurs adresses séparées par des virgules, 500 caractères au plus, chaque adresse vérifiée (`400` sinon) et la liste enregistrée en minuscules ; montants de `walletPref` entiers `>= 0` ; plafond journalier au plus égal au plafond hebdomadaire (`400` sinon) ; `timeZone` doit être un fuseau connu (`Africa/Brazzaville`, `Africa/Kinshasa`…, `400` sinon) ; `langage` non vide, 50 caractères au plus.
- `PATCH /settings` : un champ absent ou `null` garde sa valeur ; une chaîne vide retire un texte facultatif ; `0` retire un plafond.
- `PATCH /settings/notifications` : `eventType`, `channel` et `enabled` sont exigés (`400` sinon).

## À prévoir côté frontend

- **Écran « Paramètres de la plateforme »** : visible si le compte a `ROLE_SUPER_ADMIN` ou `ROLE_ADMIN` (et, pour `ADMIN`, le droit `SYSTEM:SETTINGS`, que les migrations accordent à `ADMIN` : `src/main/resources/db/migration/V208__realign_admin_role_permissions.sql`). Ne pas l'afficher pour `MANAGER` ou les autres rôles, même s'ils ont `SYSTEM:SETTINGS`.
- Construire le menu à partir de `/domains` uniquement : la liste est déjà filtrée par le serveur.
- Contrôle de saisie piloté par `type`, liste déroulante par `options` (non vide seulement pour `ENUM`), lecture seule si `editable = false`.
- Envoyer `value` en chaîne : `"true"`, `"30"`, `"08:00-18:00"`, `""` pour un jour fermé.
- Proposer un champ « motif » facultatif sur la modification et la remise à zéro (500 caractères au plus pour le `PUT`, mais voir les écarts : rester sous 200).
- Afficher clairement l'effet `RESTART` et l'état `restartPending` : sans redémarrage, rien ne change pour ces réglages. Le frontend ne peut pas déclencher de redémarrage.
- **Fiche membre, onglet « Préférences »** : lecture avec `CLIENT:READ`, modification avec `CLIENT:UPDATE`. Envoyer `Idempotency-Key` sur les deux `PATCH`. Envoyer seulement les champs changés.
- **Portail, « Mes préférences »** : routes `/sni/api/v1/client/settings`, mêmes écrans sans le motif.
- Le nom du champ d'adresse de facturation est `billlingAddress` (trois `l`), en lecture comme en écriture.
- Aucun temps réel, aucun fichier.

## Effets automatiques

- `PlatformSettings.refresh` (`src/main/java/com/sni/bokaticowork/core/settings/platform/PlatformSettings.java:606`) relit la table `platform_setting` toutes les 60 secondes et après chaque `PUT` / `DELETE` validé : c'est ce qui rend les clés `LIVE` immédiates, y compris sur plusieurs instances (délai d'une minute au plus sur les autres instances).
- Au démarrage, `PlatformSettingsEnvironmentPostProcessor` charge les valeurs enregistrées (hors secrets et hors clés inconnues du catalogue) devant `application.yml` : c'est ce qui applique les clés `RESTART`. Si la base est injoignable, l'application démarre avec le fichier seul.
- Chaque changement écrit une ligne dans `setting_audit_logs`. Les deux `PATCH` des préférences membre passent aussi par l'audit général (`@Audited`, module `SETTINGS`).
- Changement d'une clé sensible : courriel aux super-administrateurs et alerte `SENSITIVE_SETTING_CHANGED` dans l'application, après validation de la transaction. Aucun autre courriel ni événement.

## Écarts relevés

- **Réglé** : remise à zéro d'une clé `LIVE` sans effet avant redémarrage. Sans valeur en base, `PlatformSettings` rend désormais la valeur du fichier de configuration, sans la valeur de base lue au démarrage.
- **Motif trop long refusé par la base** : `UpdateRequest.reason` accepte 500 caractères (`PlatformSettingsController.java:32`), mais l'audit enregistre `<auteur> · <motif>` dans `setting_audit_logs.reason VARCHAR(255)` (`src/main/resources/db/migration/V32__create_and_align_member_settings_module.sql:91`). Au-delà d'environ 200 caractères de motif, l'enregistrement échoue (erreur base, aucun gestionnaire `DataIntegrityViolationException` trouvé dans `GlobalExceptionHandler`, statut non vérifié). Le `DELETE` n'a aucune limite de longueur.
- **Réglé** : `PATCH` des préférences membre destructif. Un champ absent garde sa valeur.
- **Réglé** : préférences membre jamais lues. Notifications, adresse de facturation, fuseau, plafonds et reçus du portefeuille sont appliqués (voir « Effet des préférences dans les autres modules »). Restent sans effet : rechargement automatique et options `show*`.
- **Réglé** : préférences de notification désactivées par défaut. Un couple sans ligne est renvoyé activé.
- **Réglé** : `notificationPref` était typé avec l'entité JPA et ignoré. C'est désormais une liste de `{ eventType, channel, enabled }`, appliquée.
- Le journal des paramètres de plateforme ne renseigne pas `changed_by` (l'auteur est mis dans `reason`, `PlatformSettingsAdminService.java:268`), alors que celui des préférences membre renseigne `changed_by` et laisse `reason` au seul motif : deux formats dans la même table.
- `ChangeView` n'expose pas l'auteur séparément ; il faut le découper dans `reason`.
- Message `Member not found` en anglais (`src/main/java/com/sni/bokaticowork/features/client/member/service/implementation/MemberServiceImpl.java:223`).
- **Réglé** : fuseau par défaut `Africa/Lagos`, remplacé par `Africa/Brazzaville` pour un membre sans préférence enregistrée.
- **Réglé** : `walletReceiptPreference` est renvoyé et modifiable (`walletPref.receiptPreference`).
