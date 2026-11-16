# Démarrage rapide et passage à deux dynos

Branche `hotfix/demarrage-taches`. Objectif : démarrer en moins de 60 s sur Heroku (limite d'attente du port) et pouvoir faire tourner deux dynos web sans doubler les tâches planifiées.

## Mesures

Production avant ce lot : **66 s** au démarrage (journal Heroku du 16/11/2026). Mesures dans un conteneur Linux limité à 2 processeurs et 1 Go, comme un dyno, sur une copie de la base de dev :

| Variante | Démarrage | Mémoire après démarrage |
| --- | --- | --- |
| Jar complet (imbriqué), avant | 51 s | 870 à 935 Mo |
| Jar extrait | 40 s | 875 à 935 Mo |
| Jar extrait, sans Spring Batch ni Spring Data JDBC | 37 à 41 s | 875 Mo |
| Jar extrait et cache AOT (ce lot) | **30 à 32 s** | 846 à 854 Mo |

Le profil du démarrage (JFR) montrait 55 % du temps du thread principal dans la lecture des classes à travers le jar imbriqué de Spring Boot (40 000 classes chargées).

## Ce qui change

### Jar extrait et cache AOT, préparés au build Heroku

- `pom.xml`, profil `heroku`, actif seulement quand la variable `STACK` existe (build Heroku) : après le `repackage`, `tools/heroku/preparer-demarrage.sh` :
  1. extrait le jar (`java -Djarmode=tools extract`) dans `target/extracted`, puis supprime le jar complet pour que le slug garde sa taille (environ 377 Mo compressés attendus, limite 500 Mo) ; l'installation dans le dépôt Maven local est sautée (`maven.install.skip`) ;
  2. entraîne le cache AOT de Java 25 (`-XX:AOTCacheOutput=target/app.aot`) : l'application démarre une fois jusqu'à la création des beans (`spring.context.exit=onRefresh`) puis s'arrête. L'entraînement ne touche à rien : environnement vidé (aucune variable de l'application, aucune clé), base et services à une adresse fermée (`127.0.0.1:1`), Flyway et validation du schéma coupés, tâches planifiées coupées, secrets aléatoires jetés. Il dure environ 70 s. Un échec ne fait pas échouer le build : le dyno démarre alors sans cache. Journal : `target/aot-entrainement.log`.
- `Procfile` : `web: sh tools/heroku/demarrer.sh`. Le script prend le jar extrait et le cache s'ils existent, sinon le jar complet comme avant ; sa première ligne dans le journal dit lequel (`Demarrage : jar extrait, cache AOT`).
- `tools/heroku/jvm.options` : options de la JVM, communes au lancement et à l'entraînement (un cache n'est valable qu'avec les mêmes options). Valeurs inchangées depuis le correctif mémoire du 15/11.
- Le cache reste valable quand le dossier change (`/tmp/build_...` au build, `/app` au lancement) : vérifié.

### Dépendances inutilisées retirées

`spring-boot-starter-batch`, `spring-boot-starter-batch-jdbc` et `spring-boot-starter-data-jdbc` (et leurs modules de test) : aucune classe ne s'en servait, mais ils se configuraient au démarrage et interrogeaient la base. `spring-boot-starter-jdbc` (`JdbcTemplate`) reste.

### Tâches planifiées sur un seul dyno

- `@EnableScheduling` n'est plus que dans `core/async/SchedulingConfig`, conditionnel (`SchedulingCondition`). Il était aussi sur la classe principale et sur `AsyncConfig` : un test vérifie qu'il n'en reste qu'un.
- Règle : sur Heroku (variable `DYNO`), seul `web.1` exécute les `@Scheduled` et les `SchedulingConfigurer` (environ 90 tâches) ; `web.2`, les dynos ponctuels (`run.*`) et la phase de release n'en exécutent aucune. Hors Heroku (poste de dev, tests), l'instance planifie.
- Réglages : `BOKATI_SCHEDULING_ENABLED` (`auto` par défaut, `true` ou `false` pour forcer), `BOKATI_SCHEDULING_DYNO` (`web.1` par défaut).
- Exception : la relecture des paramètres de la plateforme (`PlatformSettings.refresh`) entretient une copie en mémoire propre à chaque instance. Elle tourne sur chaque dyno, une fois au démarrage puis chaque minute (`PlatformSettingsRefresher`), hors de la planification. Sans cela, un dyno sans planification n'aurait jamais lu les paramètres.
- Vérifié en conteneur : `web.2` n'exécute aucune tâche, `web.1` les exécute.

### Arrêt du dyno

Heroku tue le processus 30 s après SIGTERM. Tomcat (30 s par défaut), l'exécuteur asynchrone (30 s) et le planificateur (30 s) attendaient l'un après l'autre : l'ancien processus finissait tué (code 137) à chaque redémarrage. Bornes ramenées à 10 s pour Tomcat (`spring.lifecycle.timeout-per-shutdown-phase`, profil prod) et 8 s pour chacun des deux autres.

### Test de démarrage complet

`BokatiCoworkApplicationTests` démarre l'application complète avec le profil `prod`, sans base ni Docker, avec les mêmes réglages que l'entraînement (le seul traitement de démarrage qui lit la base, `SequenceEngineStartupValidator`, est remplacé). Il échoue sur la panne du 15/11 (conflit de nom de bean), vérifié. Il tourne dans la suite normale (environ 1 à 2 min).

## Après le déploiement

1. Journal du build : `-----> Extraction du jar pour le demarrage`, puis `cache cree : ...`.
2. Journal du dyno : `Demarrage : jar extrait, cache AOT`, puis `Started BokatiCoworkApplication in ... seconds` (attendu autour de 40 s).
3. Si le cache est refusé (version de Java différente entre build et dyno), la JVM l'écrit dans le journal et démarre sans : le démarrage reste celui du jar extrait.

## Passer à deux dynos

À faire par l'exploitant, une fois ce lot en production :

1. **Relais STOMP** : les messages en direct (WebSocket) d'un dyno doivent atteindre les clients connectés à l'autre. Le relais passe par CloudAMQP (`CLOUDAMQP_URL`, port STOMP 61614 en TLS). Il est actif par défaut en production ; vérifier que `BOKATI_WEBSOCKET_USE_STOMP_RELAY` n'est pas à `false` et que le journal affiche `WebSocket · relais STOMP disponible`.
2. **Affinité de session** : l'API WebSocket a un repli SockJS dont les requêtes doivent revenir au même dyno : `heroku features:enable http-session-affinity -a bokati-cowork-elleaose`.
3. `heroku ps:scale web=2 -a bokati-cowork-elleaose`.
4. Contrôle : seules les lignes de `web.1` montrent les tâches planifiées (`[scheduler-N]`).

Déjà partagé entre dynos : sessions et limitation de débit (Redis), boîte de sortie (réclamation par verrou), passages quotidiens gardés par `scheduled_job_run`. Propre à chaque dyno et acceptable : paramètres relus chaque minute (un changement atteint l'autre dyno en moins d'une minute), limitation des écritures du journal de sécurité (une par minute et par clé, sur chaque dyno).
