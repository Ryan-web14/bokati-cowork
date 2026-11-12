# Paramètres de la plateforme pris en compte sans redémarrage

Branche `feature/parametres-a-chaud`. Décision de l'exploitant : tous les paramètres qui peuvent l'être prennent effet sans redémarrage, y compris les paramètres sensibles, mais ceux-ci sont journalisés et signalés aux super-administrateurs.

## Résultat

- Catalogue `src/main/resources/settings/platform-settings.tsv` : 327 clés `LIVE`, 34 `RESTART` (avant : 85 `LIVE`, 276 `RESTART`).
- Restent `RESTART` :
  - 27 clés d'infrastructure ou de démarrage : `app.document.storage.*`, les secrets (JWT, hachage des jetons, amorçage, aperçu signé, MinIO, STOMP), `app.security.cors.allowed-origins`, `bokati.websocket.*`, `app.locale`, `app.time-zone`, `app.error.verbose`, `app.documents.public-preview-enabled` (chaîne de filtres de sécurité construite au démarrage), `bokati.graph-api.rate-limit.limit-for-period` (sémaphore dimensionné au démarrage) ;
  - 7 délais initiaux `*-initial-delay-ms`, qui ne servent qu'au premier passage après le démarrage.

## Socle (`core/settings/platform/`)

| Classe | Rôle |
| --- | --- |
| `PlatformSettings` | Lecture au moment de servir : `text`, `number`, `integer`, `bool`, `decimal`. Valeur en base si elle existe, sinon valeur du fichier de configuration et de l'environnement sans la source `platformSettings` : une remise à zéro prend donc effet sans redémarrage. Le repli de l'appelant ne sert que si la configuration ne dit rien de la clé. |
| `SettingTriggers` | Déclencheurs relus à chaque passage : `fixedDelay(clé, défaut, délaiInitial)` et `cron(clé, défaut, zone)`. Délai minimal 1 s ; une expression cron invalide garde le défaut et le journalise. |
| `SensitiveSettings` | Les 31 clés sensibles et le risque de chacune. |
| `SensitiveSettingAlerter` | Après validation de la transaction, courriel à chaque `SUPER_ADMIN` actif et alerte `SENSITIVE_SETTING_CHANGED` dans l'application ; un échec d'envoi est journalisé et n'annule pas le changement. |
| `PlatformSettingsAdminService` | Appelle l'alerte sur `update`, `reset` et les réglages fournis ; `SettingView` gagne `sensitive` et `risk`. |

## Conversion des consommateurs

### Valeurs (environ 110 classes)

- Le champ `@Value` reste, comme repli.
- `PlatformSettings` arrive par un setter `@Autowired(required = false)`, donc aucun constructeur ne change et les tests qui font `new X(...)` gardent la valeur du champ.
- Chaque usage passe par un accesseur qui relit la clé.
- Valeurs données au constructeur (`CashClosingReportMailer`, `CompanyWalletMailer`, `BookingVirtualMeetingSupport`, `EmailBranding`, `EmailDomainPolicy`, `GedTextService`) : la valeur du constructeur sert de repli, relue à l'usage. `GedTextService` reconstruit son extracteur à chaque lecture, `EmailDomainPolicy` relit les domaines bloqués à chaque contrôle.
- `RateLimitRules` compose les clés `app.security.rate-limit.{règle}.max-requests` et `window-seconds` à partir d'un préfixe.

### Planifications (66 traitements)

- `@Scheduled(... "${clé}")` est remplacé par `SchedulingConfigurer` : `registrar.addTriggerTask(() -> self.getObject().méthode(), triggers.fixedDelay|cron(...))`.
- L'appel passe par le proxy Spring (`ObjectProvider` de la classe elle-même), pour que `@Transactional` reste actif.
- Les zones explicites (`Africa/Brazzaville`) sont gardées.
- `BookingNoShowWorker` a son propre déclencheur, qui relit `bokati.booking.lifecycle.worker-delay-ms` puis `bokati.booking.no-show.worker-delay-ms`.
- `PostingWorker` démarre après le délai courant (`fixedDelay(clé, défaut, -1)`).
- Les crons en dur (`KycAutomationWorker`, `VisitorPassExpiryWorker`, `InventoryDailyWorker`) ne sont pas des paramètres et restent tels quels.

## Limites connues

- Une valeur estampillée à la création (expiration d'un jeton, d'une offre, d'une attestation, d'une réservation de coupon) ne change que pour les créations suivantes.
- Plusieurs instances : chacune relit la table toutes les 60 s ; l'instance qui enregistre relit aussitôt.
- Frais de transfert du portefeuille : calculés dans `simulate()` puis de nouveau dans `initiate()` ; un changement entre les deux fait payer d'autres frais que ceux affichés. C'est le risque annoncé par l'alerte.
- `SecurityConfigurationGuard` ne contrôle les durées des jetons qu'au démarrage.

## Catalogue corrigé

- `bokati.graph-api.rate-limit.limit-for-period` : défaut 800 dans le catalogue, 25 dans `application.yml` ; aligné sur 25.
- `app.documents.public-preview-enabled` : défaut `false` dans le catalogue, `true` dans `application.yml` ; aligné sur `true`.

## Tests

- `PlatformSettingsAdminServiceTest` : `sensitiveSettingsAlert`, `sensitiveResetAlerts`.
- `SensitiveSettingAlerterTest` : envoi aux super-administrateurs, silence pour une clé ordinaire ou une valeur inchangée, échappement HTML.
