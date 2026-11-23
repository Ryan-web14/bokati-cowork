# Caisse par défaut et ouverture par un administrateur (23/11/2026)

Branche `feature/revue-comptable` (passe entre K1 et K2). Migrations : dev `V402__caisse_par_defaut_et_ouverture_administrateur.sql`, prod `V396__caisse_par_defaut_et_ouverture_administrateur.sql` (contenu identique).

## Demandes

1. Une session ouverte par un administrateur ou un super-administrateur ne demande pas de validation.
2. Un administrateur confirme une session de caisse, qu'il en soit le caissier ou non.
3. Le système crée une caisse par défaut qui accepte les paiements par défaut. Elle est close chaque mois, et ses sessions aussi.
4. La comptabilité suit la caisse automatiquement. Le système livre les écritures, montre les comptes où il écrit et permet de les modifier ; l'expert peut intervenir au passage des écritures.

## Ce qui change

| Sujet | Avant | Après |
|---|---|---|
| Ouverture par un administrateur | Affectation à la caisse, poste autorisé, motif hors horaires exigés ; un écart d'ouverture mettait la session en `OPENING`, à confirmer avec un motif | `ADMIN` ou `SUPER_ADMIN` : rien de tout cela. La session est `OPEN` tout de suite. Hors horaires, elle est notée « Ouverture hors horaires par un administrateur ». Un écart d'ouverture part quand même en revue et le responsable est prévenu. Trace : `openingChecksWaivedBy`. La caisse doit rester active, et un coffre n'a toujours pas de session |
| Confirmer une ouverture | Le caissier de la session seulement | Le caissier, ou n'importe quel administrateur. Le motif d'écart reste demandé |
| Paiement sans session | Espèces refusées (« La session de caisse est obligatoire ») ; chèque, virement et mobile money hors plateforme rattachés à aucune session | Tout va à la session ouverte de la caisse par défaut `CSR-DEFAULT`. Les espèces entrent dans son solde ; les autres moyens sont rattachés à la session sans entrer dans le compte des espèces |
| Caisse par défaut | n'existait pas | Créée au démarrage (`DefaultCashRegisterInitializer`), ou au premier paiement sans session. Type `MAIN`, tous moyens, `systemManaged = false` (un administrateur peut y compter, sortir des espèces, remettre en banque), politique de fin de mois `CARRY_OVER`. Sa session est tenue par `SYSTEM` |
| Fin de mois de la caisse par défaut | sans objet | Le worker de fin de mois clôt sa session au montant attendu, sans comptage, et en rouvre une avec ce solde comme fonds ; le rapport de clôture part à l'administration, à la gérance et au responsable. En fin de passage, le worker s'assure qu'elle a une session ouverte |

## Comptabilité de la caisse

C'était déjà en place, et la caisse par défaut en profite sans réglage :
- **Suivi automatique** : chaque mouvement de caisse et chaque paiement devient une pièce comptable, par le moteur d'imputation.
- **Compte et journal de la caisse** : le serveur crée pour chaque caisse son compte `571x` et son journal `CA-571x`, au premier mouvement. La caisse par défaut reçoit les siens de la même façon.
- **Comptes visibles** : chaque pièce livrée montre ses comptes, et le schéma qui l'a produite se consulte et se corrige (mode expert du moteur d'imputation).
- **Modification** : depuis K1, Reclasser, Corriger ou Recomptabiliser, avec la trace des lignes produites par le système.
- **Intervention de l'expert** :
  - un journal passé en `autoValidate = false` livre ses pièces en brouillard, que l'expert relit avant validation ;
  - au-delà du seuil, la seconde validation s'applique.

Le choix de l'imputation au moment de la saisie (mode expert à la saisie) vient en K7.

## Pour l'exploitant

- Rien à configurer : la caisse est créée au premier démarrage après déploiement.
- Pour remettre en banque les espèces de la caisse par défaut : sortie ou transfert depuis sa session, par un administrateur.
- La caisse par défaut ne peut pas recevoir de session humaine : sa session `SYSTEM` est toujours ouverte. Les caissiers gardent leurs propres caisses.
- La renommer ou changer son responsable se fait comme pour une autre caisse. La désactiver fait refuser les paiements sans session.

## Tests

- `CashDefaultRegisterTest` :
  - création de la caisse et de sa session `SYSTEM` ;
  - reprise de la session ouverte ;
  - refus quand la session est suspendue ;
  - report de fin de mois ;
  - ouverture sans validation par un super-administrateur ;
  - contrôles gardés pour un caissier.
- `CashRegisterAccessGuardTest` : un administrateur ouvre sans affectation, depuis un poste non autorisé et hors horaires.
- `CashSessionLifecycleServiceImplTest` :
  - un administrateur confirme l'ouverture d'une session qui n'est pas la sienne, un autre utilisateur non ;
  - l'écart d'une ouverture par un administrateur part en revue.
- `PaymentGapsFixTest` : un paiement en espèces sans session va à la caisse par défaut.
- `CashCorrectionsC0Test` : son utilisateur de test est désormais un caissier. Il était administrateur, ce qui aurait levé les contrôles que ce test vérifie.
