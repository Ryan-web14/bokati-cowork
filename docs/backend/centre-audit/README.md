# Centre d'audit et de supervision : plan

Branche `feature/centre-audit`, partie de new-main 0044b67e le 14/11/2026. Plan seulement : rien n'est implémenté.

## Besoin

Un centre unique où l'exploitant et l'auditeur voient :
- **les actions** : qui a fait quoi, quand, d'où, sur quel élément, avec l'avant et l'après ;
- **les événements applicatifs** : tâches planifiées, boîte de sortie, intégrations (courriel, WhatsApp, PawaPay), erreurs ;
- **le trafic** : requêtes, temps de réponse, erreurs, limitations ;
- **les utilisateurs** : connexions, échecs, verrouillages, sessions, refus d'accès.

Le tout avec recherche, chronologie par utilisateur et par élément, tableaux de bord, alertes, rétention et intégrité.

## Existant (inventaire du 13/11/2026, vérifié le 14/11/2026)

| Domaine | Ce qui existe | Manques |
| --- | --- | --- |
| Actions | `audit_log` alimenté par `@Audited` (591 usages, 93 fichiers) : acteur, action, module, ressource, IP, navigateur, session, résultat, erreur, URI. Routes `GET /admin/audit-logs`, `/admin/settings-audit-logs` (droit `SYSTEM:AUDIT`). | `diff_json` jamais rempli (`AuditContext` est lu mais jamais alimenté) ; IP lue par `getRemoteAddr` (l'adresse du proxy Heroku, pas celle du client) ; pas d'identifiant d'élément structuré ; pas de rétention ; attribution signalée fausse dans `docs/a-realiser/audit-securite.md` (tout au nom d'un administrateur). |
| Journaux métier | `fiscal_audit_log`, `contract_audit_event` et `compliance_audit_entry` (chaînés par empreinte), `wallet_admin_action`, `wallet_risk_flag`, `document_access_log`, `approval_event`, `setting_audit_logs`, `email_delivery_log`, `whatsapp_message`, `outbox_event`, `notification_message`, `webhook_delivery`, `acc_posting_event`, `pawapay_callback`, nombreux `*_history` | Dispersés, chacun avec sa route ou sans route ; aucune vue commune. |
| Requêtes | `RateLimitingFilter` (Redis) : 429 et une ligne de journal | Aucun identifiant de requête, aucun MDC, aucun journal des requêtes, journaux texte non structurés. |
| Métriques | Actuator `health` et `info` en production | Pas de registre de métriques exposé, pas d'indicateur de santé propre ; Datadog configuré sans dépendance. |
| Tâches planifiées | Environ 75 tâches ; `scheduled_job_run` utilisé par 2 seulement | Pas de suivi des exécutions, durées, échecs. |
| Utilisateurs | `last_login`, `failed_login_attempts`, `user_sessions` (IP, navigateur, appareil, géolocalisation, dernière activité, révocation) | Verrouillage de compte inactif (sauvegarde commentée, `UserServiceImpl`) ; verrouillages et réutilisation de jeton de rafraîchissement seulement écrits dans les journaux ; sessions sans route ; refus d'accès (403) non tracés. |
| Alertes | `AdminInAppNotifier.broadcastAlert` vers `/topic/admin/alerts` | Non persistées, pas de règles, pas d'accusé de lecture. |
| Droits | `SYSTEM:AUDIT`, rôle `AUDITOR` | À répartir entre consultation et administration du centre. |

## Principes

- **Dans l'application, sans service externe obligatoire.** Les données d'audit sont celles de l'exploitant et restent en base. Un outil externe (Datadog, Sentry) reste possible plus tard pour la technique, sans en dépendre.
- **Sobre en mémoire.** Le dyno a 1 Go et démarre déjà serré (voir la mémoire de démarrage Heroku). Le trafic s'agrège en mémoire et s'écrit par lots chaque minute ; rien n'est gardé en mémoire au-delà de la minute en cours.
- **Écriture asynchrone, jamais bloquante.** Un échec d'écriture d'audit ne fait jamais échouer l'action, mais il est compté et alerté.
- **Données sensibles masquées.** Mots de passe, jetons, codes, numéros de carte, clés : jamais écrits, même dans l'avant / après.
- **L'audit est lui-même audité.** Chaque consultation et chaque export du centre est tracé.

## Lots proposés

| Lot | Contenu |
| --- | --- |
| **A0. Socle** | Identifiant de requête (`X-Request-Id` reçu ou créé, renvoyé dans la réponse, porté dans le MDC, la boîte de sortie, les tâches asynchrones et chaque ligne d'audit). Adresse IP réelle du client derrière le proxy Heroku (`X-Forwarded-For`, liste de proxies de confiance). Journaux JSON en production (`logback-spring.xml`). Attribution corrigée : l'acteur est celui du jeton. |
| **A1. Journal des actions complet** | Avant / après sur les entités principales (facture, paiement, abonnement, réservation, membre, client, utilisateur, rôle, paramètre, modèle), en ne gardant que les champs modifiés, données sensibles masquées. Élément structuré (`entity_type`, `entity_code`) en plus de `ressource`. Recherche par acteur, module, action, élément, résultat, période, identifiant de requête ; export CSV. |
| **A2. Sécurité et utilisateurs** | Table `security_event` : connexion réussie ou échouée, verrouillage et déverrouillage, réutilisation d'un jeton de rafraîchissement, réinitialisation et changement de mot de passe, refus d'accès (403), limitation (429), attribution ou retrait de rôle et de droit. Verrouillage de compte réactivé (seuil et durée en paramètres). Sessions : liste par utilisateur, révocation, déconnexion de toutes les sessions. |
| **A3. Trafic** | Filtre de mesure : par minute et par route (gabarit `/billing/documents/{n}`, pas l'adresse brute), nombre, erreurs 4xx et 5xx, durées p50, p95 et max, écrits par lots dans `traffic_minute`. Requêtes lentes ou en erreur 5xx gardées une par une (`request_log`, avec utilisateur, IP, identifiant de requête). Métriques Actuator exposées au back-office seulement. |
| **A4. Événements applicatifs** | Chaque exécution de tâche planifiée tracée (`scheduled_job_run` étendu à toutes : début, fin, durée, résultat, nombre traité, erreur). Santé de la boîte de sortie (en attente, en échec, mortes, âge du plus ancien). Intégrations : taux d'échec des courriels, de WhatsApp, des rappels PawaPay. Erreurs 500 enregistrées une par une (`app_error`, avec l'identifiant de requête et la trace tronquée). Indicateurs de santé Actuator (base, Redis, RabbitMQ, Infobip, PawaPay). |
| **A5. Centre unifié** | Chronologie commune, une forme pour tous les journaux : `audit_log`, `security_event` et les journaux métier existants, lus là où ils sont, sans copie. Trois vues : tout ce qu'a fait un utilisateur ; tout ce qui est arrivé à un élément (une facture, un membre, une réservation) ; tout ce qui s'est passé dans une requête. Filtres communs, pagination par curseur, export. |
| **A6. Tableaux de bord et alertes** | Indicateurs : requêtes par minute, taux d'erreur, p95, connexions échouées, comptes verrouillés, boîte de sortie morte, tâches en échec, intégrations en échec. Règles d'alerte à seuil (paramétrables à chaud), alertes enregistrées avec accusé de lecture, envoyées dans l'application, par courriel et par WhatsApp aux administrateurs choisis. |
| **A7. Rétention, intégrité, confidentialité** | Durée de conservation par journal (paramètres), purge planifiée par lots. Chaînage par empreinte de `audit_log` et `security_event`, comme les journaux du contrat et de la conformité, avec une route de vérification. Masquage centralisé. Consultations et exports du centre audités. |
| **A8. Doc frontend** | `docs/final-frontend-doc/audit/` : écrans, filtres, routes, erreurs ; routes régénérées. |

Ordre conseillé : A0, puis A2 et A1 (le plus utile pour l'exploitant), A4, A3, A5, A6, A7, A8.

## Questions à trancher

1. **Durées de conservation.** Proposition :
   - actions : 5 ans (pièces comptables liées) ;
   - sécurité : 1 an ;
   - requêtes une par une : 30 jours ;
   - agrégats de trafic : 13 mois ;
   - erreurs : 90 jours.
2. **Journal des requêtes.** Toutes les requêtes une par une, ou seulement les lentes et les erreurs (proposition, plus sobre pour la base et le dyno) ?
3. **Qui voit quoi.** Proposition :
   - `AUDITOR` lit tout et exporte ;
   - `ADMIN` lit les actions et la sécurité ;
   - `SUPER_ADMIN` règle la rétention et les alertes.
4. **Alertes.** Canaux par défaut (application, courriel, WhatsApp) et destinataires.
5. **Avant / après.** Liste des entités à couvrir dès A1 (proposition ci-dessus) ou toutes d'un coup.
6. **Géolocalisation des adresses IP** des connexions : à garder ou à retirer (données personnelles) ?
7. **Outil externe.** Rester entièrement dans l'application (proposition), ou ajouter un service externe pour les erreurs techniques ?

## Points liés relevés en chemin

- Les gabarits de notification stockés en base (`notification_template.body_template`) sont interprétés comme du Thymeleaf. Vérifié le 14/11/2026 : Thymeleaf 3.1.5 refuse l'accès direct à `Runtime`, à `System` et à `Class.forName`. Un modèle saisi garde pourtant les autres possibilités d'une expression (appeler des méthodes des valeurs passées au courriel, créer certains objets). Le droit qui les modifie est l'accès général au back-office (`ADMIN:ACCESS`). À durcir dans ce chantier ou à part : réserver ce droit, ou remplacer ces gabarits par les modèles de courriel en blocs, dont le texte n'est jamais interprété.
- `static/index.html` : galerie d'aperçu dont tous les liens sont morts (voir le lot D6 des modèles).
