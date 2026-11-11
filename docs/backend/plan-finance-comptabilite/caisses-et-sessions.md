# Système de caisse complet : caisses, sessions, opérations

Volet caisse du [plan finance](README.md). Il reprend le système de caisse existant (`features/payment`), en relève les défauts, et décrit le système cible : référentiel des caisses, cycle de vie complet des sessions, catalogue de toutes les opérations de caisse, contrôles, états, données, routes, droits, paramétrage et découpage en lots.

Rédigé d'après le code au 11/11/2026 (branche `feature/reservation-places`, commit `2d2db8b`). Les défauts de la section 1.4 sont relevés **à la lecture du code** ; ils n'ont pas été reproduits sur l'application en marche.

Liens : écritures produites par chaque opération dans [catalogue-ecritures.md](catalogue-ecritures.md#2-caisse) ; pièces (bons, procès-verbaux, bordereaux) dans [pieces-comptables.md](pieces-comptables.md) ; demandes de fonds et avances dans [demandes-et-justification.md](demandes-et-justification.md) ; contrôle budgétaire des sorties dans [analytique-et-budget.md](analytique-et-budget.md).

---

## Sommaire

1. [Existant](#1-existant)
2. [Principes du système cible](#2-principes-du-système-cible)
3. [Organisation de la trésorerie physique](#3-organisation-de-la-trésorerie-physique)
4. [Référentiel des caisses](#4-référentiel-des-caisses)
5. [Sessions : cycle de vie complet](#5-sessions--cycle-de-vie-complet)
6. [Comptages et billetage](#6-comptages-et-billetage)
7. [Catalogue des opérations de caisse](#7-catalogue-des-opérations-de-caisse)
8. [Transferts internes à deux temps](#8-transferts-internes-à-deux-temps)
9. [Corrections : annulation, contre-passation, ajustement](#9-corrections--annulation-contre-passation-ajustement)
10. [Moyens de paiement et instruments en caisse](#10-moyens-de-paiement-et-instruments-en-caisse)
11. [Plafonds, contrôles et sécurité](#11-plafonds-contrôles-et-sécurité)
12. [Caisses automatiques](#12-caisses-automatiques)
13. [Tableau de bord, états et procès-verbaux](#13-tableau-de-bord-états-et-procès-verbaux)
14. [Notifications](#14-notifications)
15. [Modèle de données](#15-modèle-de-données)
16. [Routes](#16-routes)
17. [Droits](#17-droits)
18. [Paramétrage](#18-paramétrage)
19. [Reprise de l'existant](#19-reprise-de-lexistant)
20. [Découpage en lots et critères d'acceptation](#20-découpage-en-lots-et-critères-dacceptation)
21. [Saisie minimale : champs déduits, générés et listes](#21-saisie-minimale--champs-déduits-générés-et-listes)

Points clés ajoutés : titulaire et remplaçants d'une caisse ([4.5](#45-titulaire-remplaçants-et-transfert-de-responsabilité)) ; caissier en congé, absent ou parti ([5.6](#56-caissier-absent--congé-absence-imprévue-départ)).

---

## 1. Existant

### 1.1 Caisses (`cash_register`)

| Champ | Rôle |
| --- | --- |
| `register_code`, `name` | Identité ; code généré à la création |
| `location_code` | Site |
| `business_entity_code` | Organisation à laquelle la caisse appartient |
| `device_code` | Poste ; généré si absent |
| `system_managed`, `restricted_to_method` | Caisse automatique (`CSR-AUTO-WALLET`, `CSR-AUTO-MOBILE-MONEY`, `CSR-AUTO-PAYMENT`) réservée à un moyen ; toute saisie humaine y est refusée |
| `active` | Activation et désactivation (refusée si une session est en cours) |
| `cash_control_enabled`, `max_cash_amount` | Contrôle et plafond d'espèces ; le plafond ne sert qu'à la détection d'anomalies (`NEAR_MAX_CASH_RECURRENCE`) |
| `manager_email` | Destinataire des alertes |

Routes : création, liste, activation, désactivation. **Aucune modification** d'une caisse après sa création.

### 1.2 Sessions (`cash_session`)

- Statuts : `OPEN`, `SUSPENDED`, `CLOSING_REVIEW`, `CLOSED`, `CANCELLED`. `SUSPENDED` et `CANCELLED` n'ont aucune route qui y mène.
- Règles d'ouverture : caisse active et non automatique ; une seule session active par caisse ; une seule session active par caissier.
- Le fonds d'ouverture est saisi librement. Il produit un mouvement `OPENING_FLOAT`.
- Clôture : montant compté (un total, pas de billetage).
  - Sans écart, la session passe en `CLOSED`.
  - Avec écart, un motif est obligatoire et la session passe en `CLOSING_REVIEW`. Le superviseur est prévenu par courriel.
- Approbation de l'écart (`approve-variance`) : la session passe en `CLOSED`. La note est concaténée au motif. On ne choisit pas l'imputation, et on ne peut ni refuser l'écart ni demander un recomptage.
- Clôture de fin de mois (`CashSessionMonthEndWorker`, tous les jours à 00:05, heure de Brazzaville, sur les sessions du mois écoulé) : la session est close **sans comptage** et l'attendu est reporté sur une nouvelle session du même caissier. Les caisses automatiques sont closes et rouvertes.
- Rapport de clôture PDF envoyé par courriel ; résumé (`summary`) ; statistiques par session, caisse et caissier.

### 1.3 Mouvements (`cash_movement`)

- Types :
  - `OPENING_FLOAT` et `PAYMENT`, réservés au système ;
  - `REFUND`, `CASH_IN`, `CASH_OUT` ;
  - `SAFE_DEPOSIT`, `TRANSFER_IN`, `TRANSFER_OUT` ;
  - `ADJUSTMENT`, `CLOSING_COUNT`.
- Statuts : `PENDING`, `CONFIRMED`, `REVERSED`, `CANCELLED`. Tout est créé en `CONFIRMED`, et aucune route ne change ce statut.
- Champs :
  - montant, devise, taux ;
  - pièce (type, numéro), `flow_category` (texte libre), sous-catégorie, étiquettes ;
  - référence, contrepartie, motif ;
  - canal, poste, IP, score de risque ;
  - signature et impression ;
  - pièces jointes ;
  - `related_movement_id`, `batch_id`, `running_balance`.
- Bons d'entrée et de sortie, avec leur PDF.
- Encaissement au guichet (commit `c6089dc`) :
  - monnaie rendue ;
  - chèque, virement, mobile money hors plateforme ;
  - mobile money de la plateforme placé dans la caisse automatique.
- Annonces de paiement en espèces depuis l'espace client (`cash_payment_declaration`), confirmées au guichet.

### 1.4 Défauts relevés

| # | Défaut | Où | Effet |
| --- | --- | --- | --- |
| D1 | Les acteurs sont **saisis dans le corps de la requête** (`openedBy`, `closedBy`, `reviewedBy`, `createdBy`, `signedBy`), pas tirés du jeton | DTO `OpenCashSessionRequest`, `CloseCashSessionRequest`, `ApproveCashVarianceRequest`, mouvements, signature | N'importe quel utilisateur habilité peut agir au nom d'un autre ; la séparation des tâches est invérifiable, la piste d'audit n'est pas fiable |
| D2 | Droits trop grossiers : hors `GET` et les chemins contenant `/open` ou `/close`, tout tombe sur `CASH:ADJUST` | `AdminApiAuthorizationManager` | L'ouverture de session (`POST /sessions`, sans `/open`), la création de caisse, les mouvements, la demande de clôture et l'approbation d'écart exigent le même droit |
| D3 | Rien n'empêche le caissier de **valider son propre écart** | `approveVariance` | Contrôle interne inopérant |
| D4 | Le **solde courant** compte deux fois le fonds d'ouverture : il part de `openingAmount` puis ajoute le mouvement `OPENING_FLOAT` | `computeRunningBalance` | `running_balance` affiché trop haut du montant du fonds. L'attendu de clôture n'est pas touché (`sessionTotals` ignore `OPENING_FLOAT`) |
| D5 | `ADJUSTMENT` n'a pas de sens : montant toujours positif, **ajouté** à l'attendu, mais **ignoré** par le solde courant | `sessionTotals`, `applyDirection` | Impossible d'ajuster à la baisse ; les deux soldes divergent |
| D6 | Les totaux ne filtrent pas le statut des mouvements | `sessionTotals` | Latent tant que tout est `CONFIRMED` ; deviendra faux dès qu'on annulera ou contre-passera |
| D7 | Aucune route de contre-passation, alors que `REVERSED` et `related_movement_id` existent | | Une erreur de saisie ne se corrige que par un mouvement libre, sans lien fiable |
| D8 | Transferts **unilatéraux** : un `TRANSFER_OUT` n'attend pas de `TRANSFER_IN` ; `SAFE_DEPOSIT` n'a pas de destination | | L'argent en route n'est suivi nulle part ; une remise jamais arrivée passe inaperçue |
| D9 | Fonds d'ouverture libre, sans lien avec le dernier comptage de la caisse | `openSession` | Un écart entre deux sessions (argent disparu la nuit) est invisible |
| D10 | Le plafond `max_cash_amount` n'est pas appliqué à l'encaissement | | La caisse peut détenir n'importe quel montant |
| D11 | Clôture de fin de mois sans comptage, reportée indéfiniment | `closeForMonthEnd` | Une caisse peut rester des mois sans être comptée, sans que personne ne soit alerté |
| D12 | Pas de modification d'une caisse, pas de type, pas d'affectation des caissiers, pas d'horaires | | Tout le référentiel est figé à la création |
| D13 | Une seule devise réelle (XAF) ; `exchange_rate` est stocké mais inutilisé | | Pas d'espèces en devises |

---

## 2. Principes du système cible

1. **L'argent n'est jamais nulle part.** À tout instant, chaque franc est dans une caisse, en transit (transfert envoyé et non reçu), en banque ou chez un tiers identifié (avance). Toute sortie interne a une entrée correspondante.
2. **Ce qui est compté fait foi.** Le système calcule un attendu, le comptage le confirme. Une caisse a toujours un dernier comptage daté, et son ancienneté est surveillée.
3. **Continuité.** Le fonds d'ouverture d'une session est le montant compté à la clôture précédente de la même caisse. Toute différence est un écart d'ouverture à expliquer.
4. **L'identité vient du jeton.** Aucun acteur n'est saisi dans une requête ; le serveur l'inscrit.
5. **Rien ne s'efface.** Un mouvement confirmé se contre-passe, il ne se modifie pas. Seul un mouvement en attente s'annule.
6. **Un seul calcul de solde.** L'attendu, le solde courant, le rapport et les statistiques utilisent la même règle de sens (section 7.5).
7. **Séparation des tâches** imposée par le serveur : le caissier ne valide ni son écart, ni son ajustement, ni la réception de son propre transfert.
8. **Tout est paramétrable** (section 18), avec des valeurs initiales sûres.
9. **Le moins de saisie possible.** Le caissier saisit ce que seul lui connaît : les quantités comptées, le montant d'une dépense, une pièce externe. Tout le reste est **déduit** (du contexte, de la nature, de la facture, de la demande), **généré** (numéros, dates, libellés, montant en lettres) ou **choisi dans une liste préenregistrée** (natures, motifs, tiers, banques, porteurs). Détail champ par champ en [section 21](#21-saisie-minimale--champs-déduits-générés-et-listes).
10. **La caisse ne dépend pas d'une personne.** Une caisse a un titulaire et des remplaçants. Elle se passe à quelqu'un d'autre, temporairement (congé, absence) ou définitivement (changement de titulaire, départ), toujours par un comptage contradictoire et un procès-verbal (sections 4.5 et 5.6).

---

## 3. Organisation de la trésorerie physique

```
                 Banque(s)  <-------->  Coffre
                     ^                    ^
      remise / retrait|                    | dépôt / retrait
                     v                    v
               +----------------------------+
               |     Caisse principale      |
               +----------------------------+
          alimentation |   ^ remise de fin de journée
                       v   |
     +-------------+  +-------------+  +------------------------+
     | Guichet A   |  | Guichet B   |  | Petite caisse fonds fixe |
     +-------------+  +-------------+  +------------------------+

     Caisses automatiques (portefeuille, mobile money plateforme) : hors circuit des espèces,
     rapprochées avec le grand livre du portefeuille et le relevé de l'opérateur.
```

- Chaque caisse a une **caisse mère** (paramètre) : celle qui l'alimente et reçoit ses remises. Par défaut, c'est la caisse principale du site.
- Plusieurs sites : une caisse principale par site, et un coffre facultatif par site.
- Les circuits autorisés (qui peut transférer vers qui) sont paramétrés ; par défaut, on suit l'arbre du schéma.

---

## 4. Référentiel des caisses

### 4.1 Types

| Type | Session | Espèces | Règles propres |
| --- | --- | --- | --- |
| `COUNTER` (guichet) | Oui, par caissier | Oui | Encaissements clients, petites dépenses ; remise obligatoire au-delà du plafond de détention |
| `MAIN` (principale) | Oui | Oui | Alimente les guichets, reçoit leurs remises, fait les remises en banque ; pas d'encaissement client par défaut (paramètre) |
| `PETTY_CASH` (petite caisse à fonds fixe) | Oui | Oui | Montant fixe ; dépenses sur pièce seulement ; réalimentée exactement du total justifié (voir 7.3) |
| `SAFE` (coffre) | Non : mouvements datés, comptage périodique | Oui | Uniquement des transferts internes ; deux personnes pour ouvrir (paramètre) |
| `AUTOMATIC` | Automatique | Non | Existant ; aucune saisie humaine (section 12) |

### 4.2 Fiche caisse

| Groupe | Champs |
| --- | --- |
| Identité | Code, libellé, type, site, organisation, poste(s) autorisé(s), caisse mère, statut |
| Comptabilité | Compte de trésorerie (`571x`), journal par défaut, journaux autorisés (lot F3 du plan) ; vides tant que le socle comptable n'est pas livré |
| Moyens | Moyens acceptés (espèces, chèque, virement déclaré, mobile money hors plateforme, carte) ; devises acceptées |
| Plafonds | Détention maximale (existant `max_cash_amount`), comportement au plafond (alerte ou blocage), plafond par opération de sortie, plafond journalier de sorties, fonds de caisse standard |
| Fonctionnement | Horaires d'ouverture par jour, durée maximale d'une session, clôture à l'aveugle (oui/non), billetage obligatoire à l'ouverture, à la clôture, seuil de double comptage, comptage inopiné attendu tous les N jours |
| Personnes | Responsable(s), caissiers habilités (section 4.3), destinataires des alertes (remplace `manager_email`, gardé en lecture) |

### 4.3 Habilitation des caissiers

Une table d'affectation dit qui peut ouvrir une session sur quelle caisse, avec un rôle, une **qualité** (titulaire, remplaçant, intérimaire, voir 4.5) et une période.

| Rôle | Peut |
| --- | --- |
| `CASHIER` | Ouvrir une session, encaisser, faire les opérations de sa caisse dans ses plafonds |
| `HEAD_CASHIER` | En plus : sorties au-delà du plafond caissier, transferts, création de journal (décision 10 du plan) |
| `SUPERVISOR` | Valider écarts, ajustements et contre-passations ; comptages inopinés ; clôture d'office |
| `VIEWER` | Lecture |

L'ouverture d'une session par une personne non affectée est refusée. Une affectation d'urgence, limitée dans le temps et tracée, est possible pour un superviseur.

### 4.4 Cycle de vie d'une caisse

`ACTIVE` vers `SUSPENDED` (bloquée temporairement, par exemple pendant une enquête), vers `ACTIVE` ou vers `CLOSED`, qui est définitif.

- On ne peut suspendre ou fermer une caisse que si elle n'a ni session en cours ni transfert en transit.
- On ne peut la fermer que si son dernier comptage vaut zéro.
- Une caisse fermée reste consultable. Son code ne se réutilise pas.

Toute modification de la fiche est historisée : qui, quand, ancienne et nouvelle valeur. Un changement de plafond prend effet immédiatement, un changement de compte comptable à la date d'effet indiquée.

### 4.5 Titulaire, remplaçants et transfert de responsabilité

| Qualité | Sens |
| --- | --- |
| `HOLDER` (titulaire) | Responsable du fonds de la caisse ; un seul titulaire à la fois par caisse de guichet, de petite caisse et de caisse principale. Le coffre a deux titulaires (double clé) |
| `BACKUP` (remplaçant) | Habilité en permanence à tenir la caisse en l'absence du titulaire ; un ou plusieurs, classés par ordre de préférence. Le premier est proposé d'office |
| `INTERIM` (intérimaire) | Habilité pour une période fixe (congé du titulaire, renfort) ; l'affectation s'active et s'éteint seule aux dates prévues |

**Transfert de responsabilité** (la caisse change de titulaire, temporairement ou définitivement) :

1. Le responsable choisit le nouveau titulaire dans la liste des personnes habilitées ou habilitables, et la date d'effet. Le motif vient d'une liste : congé, mutation, départ, réorganisation, autre.
2. Comptage contradictoire par l'ancien et le nouveau titulaire (billetage, chèques, instruments), comme une passation (5.4). En l'absence de l'ancien titulaire, le comptage se fait par un superviseur et un témoin.
3. Le système liste ce que l'ancien titulaire laisse en cours, et chaque élément est soldé ou transféré explicitement :
   - session ouverte ;
   - transferts en transit, à envoyer ou à recevoir ;
   - mouvements en attente de visa ;
   - écarts en revue ;
   - annonces de paiement en espèces rattachées ;
   - chèques non remis ;
   - avances qu'il a décaissées et qui restent à justifier.
4. Un procès-verbal de remise de caisse (`PRC-`) est produit, signé par les deux (ou par le superviseur et le témoin). Les affectations sont mises à jour à la date d'effet, et l'historique de la caisse garde la succession des titulaires.

Un écart constaté à la remise est imputé à la période de l'ancien titulaire, et suit la revue d'écart (5.2).

---

## 5. Sessions : cycle de vie complet

### 5.1 États

| État | Sens |
| --- | --- |
| `OPENING` | Ouverture en cours : comptage d'ouverture saisi, en attente de confirmation (si écart d'ouverture ou réception d'alimentation) |
| `OPEN` | Ouverte, opérations possibles |
| `PAUSED` | En pause (pause déjeuner, absence courte) : aucune opération ; reprise par le même caissier, mot de passe redemandé |
| `SUSPENDED` | Suspendue par un superviseur (incident, contrôle) : aucune opération ; seul un superviseur lève la suspension |
| `COUNTING` | Clôture demandée : plus d'opération, comptage en cours |
| `CLOSING_REVIEW` | Comptée, écart hors tolérance en attente de revue |
| `RECOUNT` | Recomptage demandé par le réviseur |
| `CLOSED` | Close |
| `CANCELLED` | Ouverte par erreur, sans aucune opération : annulée |

### 5.2 Transitions

| De | Vers | Qui | Conditions et effets |
| --- | --- | --- | --- |
| (aucune) | `OPENING` ou `OPEN` | Caissier affecté | Caisse active, dans ses horaires (sinon justification et anomalie `OFF_HOURS_SESSION`), aucune autre session active ni sur la caisse ni pour le caissier. **Fonds attendu = dernier comptage de la caisse.** Billetage d'ouverture si exigé. Sans écart : `OPEN` directement |
| `OPENING` | `OPEN` | Caissier, et superviseur si écart d'ouverture | Écart d'ouverture enregistré, motivé, signalé au responsable ; produit une écriture d'écart comme un écart de clôture |
| `OPEN` | `PAUSED` | Caissier | Verrou de l'écran ; la durée de pause est tracée |
| `PAUSED` | `OPEN` | Le même caissier | Réauthentification |
| `OPEN`, `PAUSED` | `SUSPENDED` | Superviseur | Motif obligatoire ; caissier prévenu |
| `SUSPENDED` | `OPEN` ou `COUNTING` | Superviseur | Reprise, ou clôture d'office (le superviseur compte) |
| `OPEN` | `COUNTING` | Caissier, ou superviseur pour une clôture d'office | Plus d'opération ; transferts sortants en transit autorisés ; annonces de paiement en espèces de la session rattachées ailleurs |
| `COUNTING` | `CLOSED` | Caissier | Écart dans la tolérance (paramètre, 0 par défaut) ; écart toléré comptabilisé automatiquement |
| `COUNTING` | `CLOSING_REVIEW` | Système | Écart hors tolérance ; motif obligatoire ; réviseur prévenu |
| `CLOSING_REVIEW` | `RECOUNT` | Réviseur | Recomptage demandé ; second comptage par une autre personne au-delà du seuil de double comptage |
| `RECOUNT` | `CLOSING_REVIEW` ou `CLOSED` | Système | Selon le nouvel écart |
| `CLOSING_REVIEW` | `CLOSED` | Réviseur, **différent du caissier** | Décision d'imputation : charge ou produit (`658` / `758`), imputation au caissier (`421`, avec retenue possible sur paie via l'import de paie), ou mise en recherche (reste en `471`, échéance de revue) |
| `OPENING`, `OPEN` sans aucune opération | `CANCELLED` | Caissier ou superviseur | Aucune trace financière |

### 5.3 Clôture

1. Le caissier demande la clôture ; la session passe en `COUNTING`.
2. **Clôture à l'aveugle** (paramètre, actif par défaut) : le caissier saisit son billetage par coupure et devise, puis ses instruments non espèces (chèques par nombre et montant, reçus de virements et de mobile money hors plateforme). L'attendu n'est **jamais** affiché avant validation du comptage.
3. Le système calcule l'écart par devise et par moyen.
4. **Fonds conservé et remise** : le caissier indique ce qu'il garde comme fonds pour la session suivante (proposé : fonds standard de la caisse) et ce qu'il remet à la caisse mère. La remise crée un transfert en transit (section 8). La caisse mère le reçoit en comptant.
5. Le rapport de clôture est produit avec le procès-verbal de billetage ; envoi par courriel (existant), signature du caissier, et du réviseur s'il y a écart.

### 5.4 Passation (relève)

Un caissier en remplace un autre sans fermer la caisse :

1. Comptage contradictoire : billetage saisi par le sortant, confirmé ou contesté par l'entrant.
2. La session du sortant est close sur ce comptage ; celle de l'entrant s'ouvre avec ce montant.
3. Procès-verbal de passation (`PVP-`) signé par les deux.
4. Un désaccord passe en revue, comme un écart.

### 5.5 Fin de journée et fin de mois

- **Durée maximale** d'une session (paramètre, 24 h par défaut). Au-delà : alerte au caissier et au responsable, puis anomalie.
- **Fin de mois** : la clôture silencieuse avec report sans comptage disparaît pour les caisses manuelles. Le comportement est paramétré :
  - **`REQUIRE_COUNT`** (défaut) : relances les derniers jours ; le premier du mois, toute session de guichet encore ouverte est **suspendue**, et un superviseur doit la compter et la clore d'office ;
  - **`CARRY_OVER`** : comportement actuel, gardé en option ; chaque report est signalé, et le compteur « jours sans comptage » de la caisse continue de courir.
- Les caisses automatiques gardent la clôture et la réouverture automatiques.

### 5.6 Caissier absent : congé, absence imprévue, départ

Une caisse n'attend jamais le retour d'une personne, et aucune session ne reste ouverte au nom d'un absent.

**Absence planifiée** (congé, mission, formation) :

1. L'absence est déclarée à l'avance, par le caissier ou son responsable : dates et motif choisis dans une liste. Le **remplaçant est proposé d'office** : premier remplaçant (`BACKUP`) disponible sur la période, sinon un caissier affecté à une autre caisse du site. Le responsable valide.
2. L'affectation `INTERIM` du remplaçant est créée automatiquement pour la période. Elle s'active au premier jour et s'éteint au retour.
3. **Avant le départ**, le système relance le caissier (J-2 par défaut). Il faut clore sa session par une passation au remplaçant (5.4), ou par une clôture avec remise du fonds à la caisse mère. Au premier jour de l'absence, une session encore ouverte est suspendue et signalée au superviseur.
4. Pendant l'absence, le caissier absent ne peut ouvrir aucune session. Ce qui lui était adressé est redirigé vers le remplaçant : transferts à réceptionner, visas, annonces de paiement en espèces, notifications de caisse.
5. **Au retour**, le titulaire reprend la caisse par une passation avec l'intérimaire ; l'écart éventuel est imputé à la période de l'intérimaire.
6. **Sans remplaçant**, le comportement est paramétré :
   - **remise à la caisse mère** (défaut) : le fonds est transféré à la caisse mère par un transfert à deux temps, la caisse est suspendue et rouvre au retour avec un nouveau fonds ;
   - **mise sous scellés** : le fonds est compté par deux personnes, mis sous enveloppe scellée numérotée et déposé au coffre, avec un PV de mise sous scellés ; au retour, l'ouverture du scellé est un comptage contradictoire ;
   - **suspension simple** : pour une absence très courte, le fonds reste dans la caisse fermée à clé ; ce n'est permis que sous un montant plafond.

**Absence imprévue** (maladie, urgence) :

1. Le responsable enregistre l'absence, avec une date de fin éventuellement inconnue.
2. Si une session est ouverte, un superviseur fait une **clôture d'office** : comptage par le superviseur et un témoin (deux personnes différentes, pas le caissier), puis PV de clôture d'office (`PCO-`).
3. Le caissier est informé. Il peut **contester** le comptage dans un délai donné (3 jours par défaut) ; d'ici là, l'écart reste en revue et n'est pas imputé.
4. Le remplaçant ouvre ensuite sa propre session avec le montant compté.

**Départ définitif** (fin de contrat, mutation, démission) :

1. Transfert de responsabilité (4.5) à la date de départ.
2. Toutes les affectations de la personne prennent fin à cette date ; ses droits de caisse sont retirés.
3. Le départ est bloqué tant que restent en cours une avance non justifiée, un écart non décidé ou une session non close. Le responsable peut le forcer, en indiquant la suite donnée (imputation, retenue sur le solde de tout compte via l'import de paie) ; c'est tracé.

**Réviseurs et superviseurs absents** : ils déclarent un **délégué** pour la période (liste des personnes ayant le même droit). Les revues d'écart et les visas de leurs caisses vont au délégué, pour qu'aucune clôture ne reste bloquée par une absence.

Le calendrier des absences de caisse est visible sur le tableau de bord (13.1) : qui est absent, qui remplace, quelles caisses sont suspendues ou sous scellés.

---

## 6. Comptages et billetage

### 6.1 Moments de comptage

| Moment | Qui | Pièce |
| --- | --- | --- |
| `OPENING` | Caissier | PV de billetage d'ouverture si écart |
| `CLOSING` | Caissier | PV de billetage (`PVB-`) |
| `RECOUNT` | Second compteur | PV de recomptage |
| `HANDOVER` | Sortant et entrant | PV de passation (`PVP-`) |
| `SPOT_CHECK` (comptage inopiné) | Superviseur, sans prévenir | PV de comptage inopiné ; session non interrompue, écart constaté et revu |
| `TRANSFER_RECEIPT` | Destinataire d'un transfert | Accusé de réception de l'ordre de transfert (`OTI-`) |
| `PERIODIC` (coffre) | Deux personnes | PV de comptage du coffre |
| `RESPONSIBILITY_TRANSFER` | Ancien et nouveau titulaire, ou superviseur et témoin | PV de remise de caisse (`PRC-`) |
| `FORCED_CLOSE` | Superviseur et témoin | PV de clôture d'office (`PCO-`) |
| `SEAL`, `UNSEAL` | Deux personnes | PV de mise sous scellés et d'ouverture du scellé |

### 6.2 Billetage

- Coupures par devise, paramétrées et datées : billets de 10 000, 5 000, 2 000, 1 000 et 500, pièces de 500, 100, 50, 25, 10, 5, 2 et 1 pour le XAF. Une coupure retirée de la circulation se désactive sans perdre l'historique.
- Saisie par quantité ; le total se calcule. Option « rouleaux et liasses » : une liasse de 100 billets compte pour 100.
- Les billets abîmés ou suspects se saisissent à part : ils sont exclus du total disponible et suivis jusqu'à leur échange en banque.
- Un comptage n'est jamais modifié : un nouveau comptage le remplace et l'ancien reste visible.

### 6.3 Tolérance et double comptage

- Tolérance d'écart par caisse (montant et pourcentage, 0 par défaut). En dessous, l'écart est comptabilisé sans revue, mais il reste visible et compte dans les anomalies.
- Au-delà du seuil de double comptage, un second compteur différent du premier est obligatoire.
- Les comptages successifs sont tous conservés.

---

## 7. Catalogue des opérations de caisse

Chaque opération est une **nature d'opération** paramétrable (section 7.2 du plan) rattachée à un **type technique** qui fixe le sens et le traitement. Le caissier choisit la nature ; le type en découle. Les écritures sont dans le [catalogue](catalogue-ecritures.md#2-caisse).

### 7.1 Encaissements

| Opération | Type technique | Origine | Pièce | Particularités |
| --- | --- | --- | --- | --- |
| Paiement d'une facture | `PAYMENT` | Module paiement (existant) | Reçu | Monnaie rendue ; option « monnaie au portefeuille » |
| Acompte, échéance d'un échéancier | `PAYMENT` | Facturation | Reçu | |
| Recharge de portefeuille en espèces | `PAYMENT` | Portefeuille | Reçu | Plafonds LBC du portefeuille appliqués |
| Confirmation d'une annonce de paiement en espèces | `PAYMENT` | `cash_payment_declaration` (existant) | Reçu | |
| Dépôt de garantie reçu | `CASH_IN` | Contrat | Bon d'entrée | Lié au contrat ; restitution suivie |
| Retour de reliquat d'avance | `CASH_IN` | Justification d'avance | Bon d'entrée | Solde l'avance |
| Produit divers | `CASH_IN` | Saisie | Bon d'entrée | Nature obligatoire |
| Apport en espèces (associé, emprunt) | `CASH_IN` | Saisie, financement | Bon d'entrée | Double visa |

### 7.2 Décaissements

| Opération | Type technique | Origine | Pièce | Particularités |
| --- | --- | --- | --- | --- |
| Remboursement d'un client | `REFUND` | Paiement (existant) | Reçu de remboursement | Signature du client |
| Dépense directe sur pièce | `CASH_OUT` | Saisie | Bon de sortie | Sous le plafond de dépense directe (50 000 XAF, décision 7b) ; contrôle budgétaire |
| Décaissement d'une demande de fonds approuvée | `CASH_OUT` | Demande | Bon de décaissement (`BDF-`) | Bénéficiaire, pièce d'identité, signature |
| Avance à justifier | `CASH_OUT` | Demande | Bon de décaissement | Suivi de justification |
| Paiement d'une facture fournisseur | `CASH_OUT` | Achats (lot F13) | Bon de sortie | Lettrage fournisseur |
| Restitution d'un dépôt de garantie | `CASH_OUT` | Contrat | Bon de sortie | Retenues éventuelles |
| Retrait de portefeuille en espèces | `CASH_OUT` | Portefeuille | Reçu | Si activé ; plafonds LBC |

### 7.3 Opérations internes

| Opération | Type technique | Pièce |
| --- | --- | --- |
| Alimentation d'un guichet par la caisse principale | `TRANSFER_OUT` puis `TRANSFER_IN` | Ordre de transfert interne (`OTI-`) |
| Remise d'un guichet à la caisse principale | `TRANSFER_OUT` puis `TRANSFER_IN` | `OTI-` |
| Dépôt au coffre, retrait du coffre | `TRANSFER_OUT` puis `TRANSFER_IN` | `OTI-` |
| Remise en banque (espèces et chèques) | `SAFE_DEPOSIT` | Bordereau de remise (`BRB-`) ; soldé au relevé bancaire |
| Retrait en banque pour alimenter la caisse | `TRANSFER_IN` depuis un compte bancaire | Pièce de retrait ; pointée au relevé |
| **Échange de monnaie** (gros billets contre petites coupures) | `EXCHANGE` (nouveau) | Bon d'échange ; montant net nul, billetage avant et après |
| Réalimentation de la petite caisse | `TRANSFER_IN` | `OTI-` ; montant égal au total des dépenses justifiées depuis la dernière réalimentation, calculé par le système |

### 7.4 Corrections et contrôles

| Opération | Type technique | Voir |
| --- | --- | --- |
| Annulation d'un mouvement en attente | Statut `CANCELLED` | 9.1 |
| Contre-passation d'un mouvement confirmé | Mouvement inverse, lien `related_movement_id` | 9.2 |
| Ajustement | `ADJUSTMENT` avec sens | 9.3 |
| Écart d'ouverture, de clôture, de comptage inopiné | `VARIANCE` (nouveau), produit par le système | 5, 6 |

### 7.5 Règle unique de sens

Une seule fonction, utilisée par l'attendu, le solde courant, le rapport et les statistiques :

| Sens | Types |
| --- | --- |
| Entrée | `OPENING_FLOAT` (une seule fois, le fonds n'est plus compté à part), `PAYMENT`, `CASH_IN`, `TRANSFER_IN`, `ADJUSTMENT` sens `IN`, `VARIANCE` sens `IN` |
| Sortie | `REFUND`, `CASH_OUT`, `SAFE_DEPOSIT`, `TRANSFER_OUT`, `ADJUSTMENT` sens `OUT`, `VARIANCE` sens `OUT` |
| Neutre | `EXCHANGE`, `CLOSING_COUNT` |

Seuls les mouvements `CONFIRMED` et `REVERSED` comptent (un mouvement contre-passé et sa contre-passation s'annulent). Les mouvements `PENDING` et `CANCELLED` ne comptent pas. Le solde se calcule par devise.

### 7.6 Mouvements en attente et double visa

Une opération au-delà d'un seuil (paramètre par nature et par caisse) est créée en `PENDING`. Elle ne touche pas au solde et n'est pas payée tant qu'un second visa ne l'a pas confirmée. Si personne ne la vise dans le délai, elle expire en `CANCELLED`.

---

## 8. Transferts internes à deux temps

1. **Envoi** : la caisse émettrice crée un ordre de transfert : destination, montant par devise, billetage facultatif, porteur. Un `TRANSFER_OUT` confirmé sort l'argent ; l'ordre passe en `IN_TRANSIT`.
2. **Réception** : la caisse destinataire, qui doit avoir une session ouverte (sauf pour le coffre), compte et confirme. Un `TRANSFER_IN` est créé du montant **reçu**, et l'ordre passe en `RECEIVED`.
3. **Écart de transfert** : si le montant reçu diffère du montant envoyé, l'ordre passe en `DISPUTED` et part en revue, comme un écart de caisse. Le réceptionnaire ne peut pas être l'émetteur.
4. **Annulation** : tant que l'ordre n'est pas reçu, l'émetteur peut l'annuler. Le `TRANSFER_OUT` est alors contre-passé.
5. **Transit trop long** : au-delà du délai paramétré (2 h par défaut), une alerte part au responsable. Le tableau de bord montre l'argent en transit et son ancienneté. En comptabilité, ce montant est sur le compte `585`.
6. **Remise en banque** : même principe. Le bordereau est en transit jusqu'au pointage du relevé (lot F10) ; tout écart signalé par la banque crée une revue.

---

## 9. Corrections : annulation, contre-passation, ajustement

### 9.1 Annulation

Seul un mouvement `PENDING` s'annule : motif obligatoire, et il passe en `CANCELLED`.

### 9.2 Contre-passation

- **Route** : `POST /cash-registers/movements/{n}/reverse`, avec un motif.
- **Mouvement produit** : un mouvement de sens inverse, du même montant, lié par `related_movement_id`. L'original passe en `REVERSED`.
- **Session où il s'inscrit** : la session de l'original si elle est ouverte, sinon la session ouverte de la même caisse. Il ne réécrit jamais une session close.
- **Qui l'autorise** : le caissier seul, s'il agit dans les N minutes et sous un seuil donné. Sinon il faut un superviseur.
- **Exclusions** : un `PAYMENT` ou un `REFUND` ne se contre-passe pas ici. Il passe par l'annulation ou le remboursement de la transaction de paiement, qui produit lui-même le mouvement inverse. On évite ainsi une caisse et une facture désaccordées.
- **Pièce** : la pièce d'origine est réimprimée avec la mention « ANNULÉ PAR » et le numéro du mouvement inverse.

### 9.3 Ajustement

- `ADJUSTMENT` porte désormais un **sens** (`IN` ou `OUT`, le champ `CashFlowDirection` existe déjà) et une nature de correction.
- Toujours créé en `PENDING`, confirmé par un superviseur différent du caissier.
- L'ajustement ne remplace pas l'écart de clôture : il corrige une erreur identifiée (mauvais montant saisi sur un bon, par exemple), avec un renvoi au mouvement corrigé.

---

## 10. Moyens de paiement et instruments en caisse

- **Espèces** : seules elles entrent dans le billetage et le solde physique.
- **Chèques** : chaque chèque reçu devient un instrument suivi : numéro, banque, tireur, montant, transaction, puis statuts `RECEIVED`, `DEPOSITED`, `CLEARED`, `REJECTED` (lot F10 du plan).
  - À la clôture, le caissier pointe les chèques de sa session.
  - Ils voyagent avec la remise vers la caisse principale, puis vers la banque, sur le bordereau.
- **Virements et mobile money hors plateforme déclarés au guichet** :
  - ils ne sont pas dans la caisse physique, mais ils figurent dans le rapport de session (existant `nonCashPayments`) ;
  - ils attendent leur pointage sur le relevé ou l'historique marchand.
- **Carte bancaire** (si un terminal est ajouté) : même logique que le mobile money hors plateforme, avec le ticket du terminal comme pièce et la remise du terminal comme pointage.
- **Devises** : une caisse peut accepter d'autres devises (paramètre).
  - Le taux du jour est saisi par un responsable.
  - Le solde et le billetage sont tenus par devise.
  - Un encaissement en devise est converti au taux du jour pour la facture.
  - Les écarts de change vont en `676` / `776`.
  - Rendu de monnaie en XAF uniquement (paramètre).
- **Monnaie rendue** (existant) : le choix « créditer le portefeuille » est ajouté ; il est tracé sur le reçu.

---

## 11. Plafonds, contrôles et sécurité

### 11.1 Plafonds

| Plafond | Effet au dépassement (paramètre) |
| --- | --- |
| Détention maximale d'espèces (existant `max_cash_amount`, enfin appliqué) | `ALERT` : bannière et notification ; `BLOCK` : encaissements en espèces refusés tant qu'une remise n'a pas ramené le solde sous le plafond |
| Montant d'une sortie par le caissier | Au-delà : visa du caissier principal ou du superviseur (7.6) |
| Total des sorties par jour et par caissier | Idem |
| Dépense directe sans demande | Au-delà : demande de fonds obligatoire (décision 7b) |
| Remboursement en espèces | Au-delà : virement ou mobile money imposé |

### 11.2 Identité et traçabilité

- Les acteurs sont tirés du jeton : les champs `openedBy`, `closedBy`, `reviewedBy`, `createdBy` et `signedBy` disparaissent des requêtes. Ils restent acceptés et **ignorés** pendant une version, pour ne pas casser le frontend.
- Chaque mouvement enregistre l'utilisateur, le poste (`device_code`) et l'IP. Une caisse peut être limitée à certains postes.
- Réimpression d'une pièce : tracée, avec la mention « DUPLICATA ».
- Toute action sensible passe par le journal d'audit (`core/audit`) : ouverture, clôture, écart, ajustement, contre-passation, transfert, suspension, modification de caisse.

### 11.3 Séparation des tâches (imposée)

- Le caissier ne valide ni son écart, ni son ajustement, ni la contre-passation de son mouvement au-delà de son seuil.
- L'émetteur d'un transfert ne le réceptionne pas.
- Le second compteur est différent du premier.
- Une dérogation est possible par paramètre (petite structure) ; elle est alors tracée à chaque usage.

### 11.4 Anomalies (complète l'existant)

Existant : écart anormal, montants ronds, remboursements excessifs, ajustements excessifs, session hors horaires, fractionnement sous le seuil, plafond souvent frôlé.

Ajouts :

- caisse non comptée depuis N jours ;
- transfert en transit trop long ;
- écart de transfert ;
- écart d'ouverture ;
- session trop longue ;
- pauses répétées ou longues ;
- contre-passations répétées par un même caissier ;
- encaissement en espèces au-delà du plafond de détention ;
- dépense sans justificatif dans le délai ;
- avance en retard ;
- opération depuis un poste non autorisé ;
- billets suspects.

### 11.5 Comptages inopinés

Un superviseur peut compter une caisse à tout moment, sans interrompre la session. Un calendrier minimal est paramétré (par exemple, une fois par mois et par caisse). Le tableau de bord montre les caisses en retard de comptage.

---

## 12. Caisses automatiques

Elles restent comme aujourd'hui : création à la volée, une session ouverte en permanence, clôture et réouverture au seuil (`bokati.payment.cash-session.auto-limit`) et en fin de mois. Ajouts :

- **Rapprochement plutôt que comptage** :
  - la caisse `CSR-AUTO-MOBILE-MONEY` se rapproche du relevé PawaPay (dépôts, remboursements, frais) ;
  - la caisse `CSR-AUTO-WALLET` se rapproche du grand livre du portefeuille (`WalletTreasuryReconciliation`).
  - Un écart de rapprochement suit la même revue qu'un écart de caisse.
- **Reversement** : le reversement réel de l'opérateur vers le compte bancaire est un transfert à deux temps : en transit jusqu'au relevé bancaire.
- Elles sont exclues du billetage, des plafonds de détention et des comptages inopinés.

---

## 13. Tableau de bord, états et procès-verbaux

### 13.1 Position de trésorerie en temps réel

Pour chaque caisse, par devise :

- état de la caisse et de sa session, caissier ;
- solde attendu, dernier comptage (montant, date, auteur), ancienneté ;
- plafond et taux d'occupation ;
- transferts en transit, entrants et sortants ;
- instruments non espèces en attente (chèques à remettre) ;
- écarts en revue.

Totaux par site et pour l'ensemble.

### 13.2 États

| État | Contenu |
| --- | --- |
| Brouillard de caisse journalier | Tous les mouvements d'une caisse sur une journée, solde de début et de fin, signé par le caissier |
| Livre de caisse (SYSCOHADA) | Par période, par caisse, en lien avec le journal comptable `CA-` (lot F3) |
| Historique des sessions | Ouvertures, clôtures, durées, écarts, réviseurs |
| État des écarts | Par caisse, caissier, période ; décision d'imputation ; cumul par caissier |
| État des transferts | En transit, reçus, contestés |
| État des comptages | Derniers comptages, retards, billetage |
| Chèques en caisse | Reçus non remis, remis non encaissés, rejetés |
| Sorties par nature, par poste et par bénéficiaire | Liaison avec l'analytique et le budget |
| Statistiques (existant) | Complétées des sorties par nature, des avances et des transferts |

Exports PDF et Excel ; chaque état porte sa date d'édition et son auteur.

### 13.3 Procès-verbaux

Billetage, passation, comptage inopiné, recomptage, revue d'écart, réception de transfert. Les gabarits sont dans [pieces-comptables.md](pieces-comptables.md) (sections 7 à 11) ; il y faut en plus le PV de comptage inopiné et le PV de revue d'écart.

---

## 14. Notifications

| Événement | Destinataires |
| --- | --- |
| Écart en revue, écart de transfert, écart d'ouverture | Réviseurs de la caisse |
| Écart décidé (imputation) | Caissier |
| Plafond de détention atteint ou dépassé | Caissier, caissier principal |
| Transfert envoyé, transfert en transit trop long | Caisse destinataire, responsable |
| Opération en attente de visa, visa expiré | Viseurs, demandeur |
| Session trop longue, session ouverte à la fin du mois | Caissier, responsable |
| Caisse non comptée depuis N jours | Superviseur |
| Session suspendue ou reprise | Caissier |
| Absence déclarée, remplaçant désigné | Responsable (validation), remplaçant |
| Absence qui commence avec une session encore ouverte (J-2 puis J) | Caissier, responsable |
| Clôture d'office faite, délai de contestation | Caissier absent |
| Retour d'absence : reprise de caisse à faire | Titulaire, intérimaire |
| Transfert de responsabilité : éléments en cours à solder | Ancien et nouveau titulaire, responsable |

Canaux et destinataires paramétrés (section 20 du plan).

---

## 15. Modèle de données

Migrations dev et prod à chaque lot. Les tables existantes sont enrichies, rien n'est supprimé.

### 15.1 Colonnes ajoutées

| Table | Colonnes |
| --- | --- |
| `cash_register` | `register_type`, `status` (remplace `active`, qui reste calculé), `parent_register_code`, `treasury_account_number`, `default_journal_code`, `accepted_methods`, `accepted_currencies`, `cap_behavior`, `max_out_per_operation`, `max_out_per_day`, `standard_float`, `blind_close`, `count_tolerance`, `double_count_threshold`, `max_session_hours`, `spot_check_every_days`, `month_end_policy`, `last_count_at`, `last_count_amount`, `updated_by` |
| `cash_session` | `expected_opening_amount`, `opening_variance`, `paused_at`, `pause_seconds`, `suspended_by`, `suspension_reason`, `kept_float`, `handover_to_session`, `variance_decision` (`EXPENSE`, `INCOME`, `CASHIER`, `INVESTIGATION`), `variance_decided_by`, `forced_close` |
| `cash_movement` | `direction` (pour `ADJUSTMENT` et `VARIANCE`), `transfer_order_number`, `approved_by`, `approved_at`, `expires_at`, `reversal_reason`, plus les colonnes comptables de la section 7.1 du plan |
| `cash_movement_type` (enum) | `EXCHANGE`, `VARIANCE` |
| `cash_session_status` (enum) | `OPENING`, `PAUSED`, `COUNTING`, `RECOUNT` |

### 15.2 Tables nouvelles

| Table | Champs principaux |
| --- | --- |
| `cash_register_assignment` | register_code, user_code, role, quality (`HOLDER`, `BACKUP`, `INTERIM`), backup_rank, valid_from, valid_to, emergency, absence_number, granted_by |
| `cash_staff_absence` | number, user_code, kind (liste paramétrable : congé, maladie, mission, formation, départ), starts_on, ends_on (facultatif), replacement_user, register_codes, no_replacement_policy, status (`PLANNED`, `APPROVED`, `ACTIVE`, `ENDED`, `CANCELLED`), declared_by, approved_by |
| `cash_responsibility_transfer` | number (`PRC-`), register_code, from_user, to_user, reason, effective_at, count_id, pending_items (jsonb : éléments en cours et leur sort), signatures, permanent |
| `cash_seal` | number, register_code, count_id, envelope_number, sealed_by, witness, deposited_in (coffre), unsealed_at, unseal_count_id |
| `cash_delegation` | delegator, delegate, permissions, starts_on, ends_on |
| `cash_register_schedule` | register_code, day_of_week, opens_at, closes_at |
| `cash_register_history` | register_code, field, old_value, new_value, changed_by, changed_at, effective_at |
| `cash_denomination` | currency, value, kind (BILL, COIN), active_from, active_to, label |
| `cash_count` | number, register_code, session_number, moment, currency, total, counted_by, second_counter, status, superseded_by |
| `cash_count_line` | count_id, denomination_id, quantity, damaged_quantity |
| `cash_transfer_order` | number (`OTI-`), from_register, to_register, currency, sent_amount, received_amount, status (`IN_TRANSIT`, `RECEIVED`, `DISPUTED`, `CANCELLED`), carrier, sent_by, sent_at, received_by, received_at, out_movement, in_movement |
| `cash_handover` | number (`PVP-`), from_session, to_session, count_id, outgoing_signature, incoming_signature, disputed |
| `cash_variance_review` | session_number ou transfer_order, kind (OPENING, CLOSING, SPOT_CHECK, TRANSFER), amount, reason, decision, decided_by, decided_at, note, entry_number |
| `cash_spot_check` | register_code, session_number, count_id, checked_by, variance |
| `cash_exchange_rate` | currency, rate, valid_on, set_by |

Les tables `cheque`, `bank_deposit_slip` et `cash_operation_nature` sont celles de la section 15 du plan.

---

## 16. Routes

Les routes existantes restent (`/cash-registers/...`) ; elles ignorent les acteurs envoyés dans le corps. Ajouts :

| Méthode et chemin | Rôle |
| --- | --- |
| `PATCH /cash-registers/{code}` | Modifier une caisse (historisé) |
| `PATCH /cash-registers/{code}/suspend`, `/close` | Cycle de vie de la caisse |
| `GET`, `PUT /cash-registers/{code}/assignments` | Caissiers habilités |
| `GET /cash-registers/{code}/expected-opening` | Fonds attendu pour ouvrir (dernier comptage) |
| `PATCH /cash-registers/sessions/{n}/pause`, `/resume` | Pause |
| `PATCH /cash-registers/sessions/{n}/suspend`, `/unsuspend` | Suspension par un superviseur |
| `POST /cash-registers/sessions/{n}/counts` | Enregistrer un comptage (ouverture, clôture, recomptage, inopiné) |
| `POST /cash-registers/sessions/{n}/handover` | Passation |
| `PATCH /cash-registers/sessions/{n}/variance-decision` | Décider d'un écart (remplace `approve-variance`, qui reste comme alias avec la décision par défaut) |
| `PATCH /cash-registers/sessions/{n}/recount` | Demander un recomptage |
| `PATCH /cash-registers/sessions/{n}/cancel` | Annuler une session vide |
| `POST /cash-registers/transfers`, `PATCH /cash-registers/transfers/{n}/receive`, `/cancel`, `GET /cash-registers/transfers` | Transferts à deux temps |
| `POST /cash-registers/movements/{n}/reverse` | Contre-passation |
| `PATCH /cash-registers/movements/{n}/approve`, `/cancel` | Visa ou annulation d'un mouvement en attente |
| `POST /cash-registers/sessions/{n}/exchanges` | Échange de monnaie |
| `GET /cash-registers/position` | Position de trésorerie en temps réel |
| `GET /cash-registers/reports/{kind}` | États de la section 13.2 |
| `GET`, `PUT /cash-registers/denominations`, `/exchange-rates` | Référentiels |
| `POST`, `GET /cash-registers/absences`, `PATCH .../{n}/approve`, `/cancel`, `/end` | Absences de caisse ; la réponse de création contient le remplaçant proposé |
| `GET /cash-registers/absences/replacement-suggestions?registerCode=&from=&to=` | Remplaçants disponibles, dans l'ordre de préférence |
| `POST /cash-registers/{code}/responsibility-transfers`, `GET .../pending-items` | Transfert de responsabilité et liste des éléments en cours |
| `PATCH /cash-registers/sessions/{n}/force-close` | Clôture d'office avec témoin |
| `PATCH /cash-registers/sessions/{n}/contest` | Contestation d'une clôture d'office par le caissier |
| `POST /cash-registers/{code}/seal`, `/unseal` | Mise sous scellés et ouverture |
| `POST`, `GET /cash-registers/delegations` | Délégations des réviseurs et superviseurs |
| `GET /cash-registers/form-defaults?context=` | Valeurs proposées pour un formulaire (section 21) |

Les routes de clôture et de comptage ne renvoient jamais l'attendu tant que le comptage n'est pas validé, si la clôture à l'aveugle est active.

---

## 17. Droits

Remplace le découpage actuel (`CASH:READ`, `OPEN_SESSION`, `CLOSE_SESSION`, `ADJUST`) par des permissions fines, vérifiées sur la route **et** sur l'affectation à la caisse :

| Permission | Pour |
| --- | --- |
| `CASH:READ` | Consulter |
| `CASH_REGISTER:MANAGE` | Créer, modifier, suspendre, fermer une caisse ; affectations ; coupures ; taux |
| `CASH_SESSION:OPEN`, `:CLOSE`, `:PAUSE` | Caissier affecté |
| `CASH_SESSION:SUSPEND`, `:FORCE_CLOSE`, `:SPOT_CHECK` | Superviseur |
| `CASH_VARIANCE:DECIDE` | Réviseur |
| `CASH_MOVEMENT:CREATE` | Caissier, dans ses plafonds |
| `CASH_MOVEMENT:APPROVE` | Visa des mouvements en attente et des ajustements |
| `CASH_MOVEMENT:REVERSE` | Contre-passation au-delà du seuil caissier |
| `CASH_TRANSFER:SEND`, `:RECEIVE` | Caissier principal, caissier affecté à la destination |
| `CASH_REPORT:READ` | États |
| `CASH_STAFF:DECLARE_ABSENCE` | Le caissier pour lui-même, son responsable |
| `CASH_STAFF:MANAGE` | Valider les absences, désigner les remplaçants, transférer la responsabilité d'une caisse, forcer un départ |

Les anciennes permissions sont reprises par correspondance à la migration :

- `OPEN_SESSION` donne `CASH_SESSION:OPEN` ;
- `CLOSE_SESSION` donne `CASH_SESSION:CLOSE` ;
- `ADJUST` donne `CASH_MOVEMENT:CREATE`, `CASH_TRANSFER:SEND` et `CASH_TRANSFER:RECEIVE`.

`CASH_VARIANCE:DECIDE` et `CASH_MOVEMENT:APPROVE` ne sont **pas** attribués automatiquement : ils sont donnés explicitement aux superviseurs.

---

## 18. Paramétrage

| Paramètre | Valeur initiale |
| --- | --- |
| Types de caisse disponibles | Guichet, principale, petite caisse, coffre, automatique |
| Caisse mère par défaut | Caisse principale du site |
| Circuits de transfert autorisés | Arbre caisse mère et caisses filles, coffre, banque |
| Coupures par devise | XAF : billets de 10 000 à 500, pièces de 500 à 1 |
| Devises acceptées | XAF |
| Clôture à l'aveugle | Active |
| Billetage obligatoire | Clôture : oui ; ouverture : oui si écart avec le dernier comptage |
| Tolérance d'écart | 0 |
| Seuil de double comptage | 10 000 XAF |
| Comportement au plafond de détention | Alerte |
| Plafond de sortie par opération et par jour | 50 000 XAF par opération (décision 7b) ; journalier sans limite |
| Seuil de double visa d'une sortie | 50 000 XAF |
| Délai de contre-passation par le caissier seul | 15 minutes, sous 50 000 XAF |
| Durée maximale d'une session | 24 h |
| Politique de fin de mois | `REQUIRE_COUNT` |
| Transit maximal d'un transfert | 2 h |
| Comptage inopiné | Au moins une fois par mois et par caisse |
| Caisse non comptée | Alerte à 7 jours |
| Décision d'écart proposée par défaut | Charge ou produit (décision 8 du plan) |
| Dérogation à la séparation des tâches | Désactivée |
| Rendu de monnaie en devise | Non |
| Monnaie rendue au portefeuille | Proposée |
| Rappel avant une absence | 2 jours |
| Sans remplaçant | Remise à la caisse mère (sinon : mise sous scellés, suspension simple sous un plafond) |
| Plafond de la suspension simple | 100 000 XAF |
| Délai de contestation d'une clôture d'office | 3 jours |
| Choix du remplaçant | Premier remplaçant disponible, sinon caissier du site le moins chargé |
| Départ avec éléments en cours | Bloqué, forçable par `CASH_STAFF:MANAGE` |
| Motifs (absence, écart, ajustement, contre-passation, suspension, annulation, transfert de responsabilité) | Listes initiales de la section 21.3 ; option « Autre, préciser » activable par liste |
| Champs déduits modifiables par le caissier | Montant d'une sortie liée, libellé ; non modifiables : compte, journal, analytique (modifiables par le caissier principal) |

Tous ces paramètres se règlent par caisse quand ils ont un sens par caisse ; la valeur générale sert par défaut.

---

## 19. Reprise de l'existant

1. Chaque caisse existante reçoit :
   - le type `COUNTER`, ou `AUTOMATIC` pour les caisses `system_managed` ;
   - le statut tiré de `active` ;
   - pour caisse mère, une caisse principale à créer par site.
2. **Affectations** : chaque caissier qui a ouvert une session sur une caisse durant les 90 derniers jours y est affecté comme `CASHIER`. La liste est à revoir avant mise en service.
3. **Dernier comptage** :
   - pour une caisse, c'est le `counted_closing_amount` de sa dernière session close ;
   - pour une session close par la fin de mois sans comptage, la caisse est marquée « jamais comptée depuis » cette date.
4. **Solde courant** : `running_balance` est recalculé sur toutes les sessions avec la règle unique (D4), par une migration de données.
5. **Ajustements existants** : ils reçoivent le sens `IN`, c'est-à-dire l'effet qu'ils avaient déjà sur l'attendu. Une liste est produite pour vérification.
6. **Transferts existants** : les `TRANSFER_OUT` et `SAFE_DEPOSIT` passés restent tels quels, sans ordre de transfert. Seuls les nouveaux suivent le circuit à deux temps.
7. **Sessions ouvertes au moment du déploiement** : elles continuent avec les anciennes règles jusqu'à leur clôture. Les nouvelles règles s'appliquent dès la session suivante.

---

## 20. Découpage en lots et critères d'acceptation

Ces lots détaillent le lot **F8** du plan. C0 n'a aucune dépendance et peut partir tout de suite. Les autres n'ont pas besoin du socle comptable : les champs comptables restent vides jusqu'au lot F3.

| Lot | Contenu | Dépend de | Critère d'acceptation |
| --- | --- | --- | --- |
| **C0. Corrections** | D1 (acteurs tirés du jeton), D2 (droits fins sur les routes existantes), D3 (pas d'auto-validation d'un écart), D4 (solde courant), D5 (sens de l'ajustement), D6 (filtre de statut), règle unique de sens, recalcul des soldes | | Un caissier ne peut ni ouvrir au nom d'un autre ni valider son propre écart ; le solde courant d'une session ouverte avec 10 000 de fonds et un encaissement de 5 000 vaut 15 000 |
| **C1. Référentiel des caisses** | Types, fiche complète avec valeurs déduites du type et du site, modification historisée, cycle de vie, caisse mère, affectations, titulaire et remplaçants, transfert de responsabilité, horaires, postes autorisés, listes préenregistrées | C0 | Une personne non affectée ne peut pas ouvrir de session ; chaque modification de caisse apparaît dans l'historique |
| **C2. Sessions et comptages** | États complets, continuité du fonds, écart d'ouverture, pause, suspension, billetage, clôture à l'aveugle, tolérance, double comptage, recomptage, décision d'écart, passation, politique de fin de mois, durée maximale ; absences (congé, imprévue, départ), remplaçant proposé, clôture d'office avec témoin et contestation, mise sous scellés, délégation des réviseurs | C1 | L'attendu n'est visible qu'après le comptage ; une session ouverte avec un fonds différent du dernier comptage crée un écart d'ouverture ; une passation produit son PV signé par les deux ; un congé déclaré avec remplaçant crée l'intérim aux bonnes dates et bloque l'ouverture par l'absent |
| **C3. Opérations et transferts** | Natures d'opération en caisse, transferts à deux temps, remise de fin de journée, échange de monnaie, petite caisse à fonds fixe, contre-passation, ajustement visé, mouvements en attente et double visa | C2 | Un transfert de 200 000 envoyé et reçu pour 195 000 passe en contestation et en revue ; une contre-passation remet le solde à sa valeur d'avant |
| **C4. Contrôles** | Plafonds appliqués, comptages inopinés, nouvelles anomalies, réimpressions tracées, audit | C2, C3 | En mode blocage, un encaissement en espèces au-delà du plafond est refusé jusqu'à la remise |
| **C5. Instruments et devises** | Chèques en caisse (avec le lot F10), devises, taux, billetage par devise, monnaie au portefeuille | C2 | Une caisse acceptant l'euro tient un solde et un billetage en EUR et en XAF |
| **C6. Pilotage** | Position de trésorerie en temps réel, états, PV manquants, notifications | C2, C3 | La position montre, pour chaque caisse, le dernier comptage, son ancienneté et l'argent en transit |

La **saisie minimale** (section 21) n'est pas un lot à part : chaque lot livre ses formulaires avec leurs champs déduits, générés et listés, et un formulaire qui demande à la main une valeur que le système connaît est un défaut à corriger.

Ordre conseillé : C0 (correctifs, livrable en quelques jours, à faire même si le reste attend), C1, C2, C3, puis C4 et C6, et C5 avec le lot banque F10.

Chaque lot a son dossier de doc backend et sa doc frontend dans `docs/final-frontend-doc/`, conformément aux règles du dépôt.

---

## 21. Saisie minimale : champs déduits, générés et listes

### 21.1 Règles

1. **Quatre origines possibles pour un champ**, dans cet ordre de préférence :
   - **déduit** : du contexte (utilisateur, poste, caisse, session), d'un objet lié (facture, demande, avance, transfert) ou d'un paramétrage (nature, caisse) ;
   - **généré** : numéros, dates, libellés, montant en lettres, totaux ;
   - **liste** : référentiel préenregistré, avec recherche, plus utilisés d'abord, favoris par caissier ;
   - **saisi** : uniquement ce que personne d'autre ne sait (quantités comptées, montant d'une dépense libre, numéro d'une pièce externe).
2. **Un champ déduit s'affiche**, il ne se cache pas : le caissier voit ce qui sera enregistré. Il ne le modifie que si le paramétrage le permet, et la modification est tracée (valeur proposée, valeur retenue).
3. **Les listes sont administrables** (ajout, désactivation, ordre). L'option « Autre, préciser », qui ouvre un texte libre, est activable liste par liste. Les « Autre » fréquents sont signalés à l'administrateur pour enrichir la liste.
4. **Pré-remplissage par scan** : le QR ou le code-barres d'une facture, d'une demande de fonds, d'un ordre de transfert ou d'un reçu remplit le formulaire correspondant.
5. **Mémoire** : le dernier choix du caissier pour un même contexte est proposé (par exemple, la dernière banque de remise).
6. Le serveur expose les valeurs proposées (`GET /cash-registers/form-defaults?context=`) pour que le frontend n'ait aucune règle à recoder.

### 21.2 Champ par champ

| Objet | Champ | Origine |
| --- | --- | --- |
| Caisse | Code | Généré (existant) |
| Caisse | Organisation, caisse mère | Déduites du site |
| Caisse | Compte de trésorerie | Généré : prochain sous-compte `571x` libre ; journal `CA-<code>` généré |
| Caisse | Plafonds, tolérance, horaires, clôture à l'aveugle | Hérités des valeurs par défaut du type de caisse |
| Caisse | Site, responsables, titulaire, remplaçants | Listes (sites, utilisateurs habilités) |
| Caisse | Poste autorisé | Proposé à la première connexion depuis ce poste |
| Session | Caisse | Déduite : la caisse dont l'utilisateur est titulaire ou intérimaire sur ce poste ; liste s'il en a plusieurs |
| Session | Caissier, date et heure, poste, IP | Déduits (jeton, serveur) |
| Session | Fonds d'ouverture attendu | Déduit du dernier comptage |
| Comptage | Quantités par coupure | **Saisies** ; total, écart et montant en lettres calculés |
| Clôture | Fonds conservé | Proposé : fonds standard de la caisse |
| Clôture | Montant à remettre, destination | Calculé (compté moins fonds conservé) ; destination déduite (caisse mère) |
| Clôture | Chèques et instruments à pointer | Liste des instruments de la session, tous cochés |
| Écart | Montant | Calculé |
| Écart | Motif | Liste (21.3) |
| Écart | Décision d'imputation | Proposée (paramètre) |
| Mouvement | Numéro, date, session, caissier, poste | Générés ou déduits |
| Mouvement | Nature | Liste par famille, recherche par mots, plus utilisées d'abord |
| Mouvement | Type technique, sens, compte, journal, TVA, section analytique, poste budgétaire, justificatif exigé | Déduits de la nature (et de la caisse pour le journal) |
| Mouvement | Montant | Déduit de l'objet lié (facture, demande approuvée, reliquat d'avance, remboursement) ; saisi pour une dépense libre |
| Mouvement | HT et TVA | Calculés à partir du TTC saisi et du taux de la nature |
| Mouvement | Contrepartie | Liste (clients, membres, entreprises, fournisseurs, employés) avec recherche par nom, téléphone, code ; ou déduite de l'objet lié ; fiche fournisseur en création rapide (nom, téléphone, NIU facultatif) |
| Mouvement | Bénéficiaire, pièce d'identité | Déduits de la demande ou de la fiche employé |
| Mouvement | Libellé | Généré : nature + contrepartie + référence ; modifiable |
| Mouvement | Numéro de pièce interne (`BEC-`, `BSC-`, `BDF-`) | Généré |
| Mouvement | Numéro de pièce externe (facture fournisseur, ticket) | Saisi, ou lu sur la photo jointe |
| Mouvement | Montant en lettres | Généré |
| Mouvement | Signature | Capturée sur la tablette ou le pavé de signature |
| Paiement en espèces | Montant dû | Déduit de la facture (scan) ; seul le montant remis est saisi, la monnaie se calcule (existant) |
| Chèque | Tireur, montant | Déduits du client et du paiement ; banque : liste des banques ; numéro saisi |
| Transfert | Destination | Déduite (caisse mère, ou caisse fille unique) ; liste sinon |
| Transfert | Montant | Proposé : excédent au-dessus du fonds standard (remise), ou montant de réalimentation (petite caisse) |
| Transfert | Porteur | Liste des employés |
| Remise en banque | Compte bancaire | Compte par défaut de la caisse ; liste sinon |
| Remise en banque | Lignes du bordereau | Chèques non remis et espèces à remettre, déduits |
| Devise | Taux | Taux du jour, déduit |
| Contre-passation | Montant, sens, session | Déduits du mouvement d'origine ; motif dans une liste |
| Passation | Entrant | Liste des caissiers affectés et présents ; remplaçant proposé d'office |
| Absence | Remplaçant | Proposé d'office (5.6) ; motif et période : liste et calendrier |
| Absence | Caisses concernées | Déduites des affectations de la personne |
| Transfert de responsabilité | Éléments en cours | Listés par le système |

### 21.3 Listes préenregistrées initiales

| Liste | Valeurs initiales |
| --- | --- |
| Natures d'opération | Celles de la section 7.2 du plan, par famille |
| Motifs d'écart | Erreur de rendu de monnaie, billet non compté, encaissement non saisi, sortie non saisie, billet faux ou abîmé, erreur de saisie de montant, inexpliqué |
| Motifs d'ajustement | Erreur de montant sur un bon, doublon, mouvement oublié, régularisation demandée par la comptabilité |
| Motifs de contre-passation | Erreur de saisie, mauvaise nature, mauvaise caisse, opération non réalisée, doublon |
| Motifs de suspension | Contrôle, incident, enquête, absence imprévue, panne |
| Motifs d'annulation de session | Ouverte par erreur, mauvaise caisse |
| Motifs d'absence | Congé annuel, maladie, mission, formation, congé exceptionnel, départ |
| Motifs de transfert de responsabilité | Congé, mutation, départ, réorganisation |
| Motifs de session hors horaires | Événement, inventaire, rattrapage |
| Banques | Banques du pays (à compléter par l'administrateur) |
| Coupures, devises | Section 6.2 |
| Types de pièce externe | Facture, ticket de caisse, reçu, quittance, note de frais |
