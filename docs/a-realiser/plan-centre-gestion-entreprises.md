
# Plan · Centre de gestion des clients entreprises

> État : **plan, rien n'est implémenté.** Une décision de modèle est bloquante (lot 0).
> Rédigé le 2026-10-05, après le correctif de l'entité exploitante (`378496a`).

## 1. Ce qui existe aujourd'hui

Relevé, pas d'impression. Le module `features/company/` tient une seule table,
`business_entity`, avec douze champs modifiables :

| Champ | Obligatoire | Remarque |
|---|---|---|
| `name` | oui | raison sociale |
| `legalForm` | oui | texte libre · aucune liste de valeurs |
| `niuNumber` | oui | 12 à 18 caractères |
| `rccmNumber` | oui | aucun format vérifié |
| `taxId` | non | |
| `activity` | non | texte libre |
| `address` | non | une seule adresse |
| `phone` | oui | un seul numéro |
| `email` | non | un seul courriel |
| `baseCurrencyCode` | oui | |
| `status` | — | `ACTIVE`, `INACTIVE`, `DELETED`, `BLOCKED` |
| `is_operator` | — | ajouté par `378496a` |

Douze points d'entrée : création, lecture par code, lecture par nom, liste, liste paginée,
recherche simple, recherche par critères, mise à jour complète, `PATCH` adresse, `PATCH` statut,
`PATCH` exploitant, suppression logique.

Huit modules référencent une entreprise : `billing`, `booking`, `company`, `contract`,
`document`, `domiciliation`, `payment`, `subscription`.

## 2. Les trois problèmes de fond

Le constat « les infos demandées sont brèves » est juste, mais ce n'est pas le plus gênant.
Ajouter des champs sur un modèle qui ne tient pas reviendrait à les ajouter trois fois.

### 2.1 Une même entreprise existe à trois endroits, sans lien

```
Customer { type = COMPANY, companyName = "SARL Mboté" }     ← texte libre, aucune donnée légale
BusinessEntity { name, legalForm, niuNumber, rccmNumber }   ← la fiche légale
DomiciliationDtos.OpenContractRequest { legalName, legalForm,
    registrationNumber, taxNumber, legalRepresentativeName } ← resaisi à l'ouverture du contrat
```

`Customer` n'a **aucune clé étrangère** vers `business_entity`. Un abonnement se souscrit
indifféremment avec `subscriberType = CUSTOMER` ou `BUSINESS_ENTITY`. La même entreprise peut
donc être enregistrée deux fois, facturée d'un côté et contractée de l'autre, sans que rien ne
rapproche les deux. Et la domiciliation recollecte les mêmes informations légales une troisième
fois, dans un formulaire à part.

C'est ce qui explique que les informations paraissent brèves : elles sont dispersées, pas
absentes.

### 2.2 Aucune personne n'est rattachée à une entreprise

`Member` n'a pas d'employeur. `BusinessEntity` n'a ni contact, ni représentant légal, ni
signataire autorisé, ni liste de collaborateurs. Pour un espace qui vend des formules
d'entreprise, c'est le concept central qui manque : un compte entreprise dont des personnes
nommées consomment la formule, avec des droits distincts.

Conséquences concrètes aujourd'hui : on ne sait pas qui peut réserver au nom de l'entreprise, qui
reçoit les factures, qui est habilité à signer un contrat, ni combien de collaborateurs sont
couverts.

### 2.3 L'entité exploitante partage la table des clients

Corrigé par le drapeau `is_operator` (`378496a`), mais le mélange des deux rôles reste. Il
oblige chaque requête de liste à se souvenir d'exclure l'exploitant, et il a déjà coûté
l'absence totale de génération de contrat en production. À surveiller tant que les deux rôles
cohabitent.

## 3. Lot 0 · Mesurer, puis décider le modèle — **bloquant**

Rien ne doit être écrit avant. Deux heures de travail, pas plus.

### 3.1 Mesurer l'existant en production

```sql
SELECT 'customers COMPANY'      AS objet, count(*) FROM customer
  WHERE type = 'COMPANY' AND NOT deleted
UNION ALL SELECT 'business_entity (hors exploitant)', count(*) FROM business_entity
  WHERE NOT deleted AND NOT is_operator
UNION ALL SELECT 'abonnements subscriberType=CUSTOMER', count(*) FROM subscription
  WHERE subscriber_type = 'CUSTOMER'
UNION ALL SELECT 'abonnements subscriberType=BUSINESS_ENTITY', count(*) FROM subscription
  WHERE subscriber_type = 'BUSINESS_ENTITY'
UNION ALL SELECT 'doublons probables (meme nom des deux cotes)', count(*)
  FROM customer c JOIN business_entity b
    ON lower(trim(c.company_name)) = lower(trim(b.name))
  WHERE c.type = 'COMPANY' AND NOT c.deleted AND NOT b.deleted;
```

Le dernier chiffre décide : s'il est nul, la consolidation est indolore ; s'il ne l'est pas, il
faut une étape de rapprochement avant toute migration.

### 3.2 La décision · **arrêtée le 2026-10-05**

**`Customer` reste le client commercial, `BusinessEntity` devient son profil légal attaché.**

```
customer                        business_entity
  id                              niu_number, rccm_number, legal_form,
  type = COMPANY                  share_capital, legal_representative,
  company_name                    vat_regime, ...
  business_entity_id  ──────────>  (profil legal, 1-1, optionnel)
```

Ce qui en découle :

- facturation, abonnements, réservations et portail continuent de pointer sur `Customer` ·
  aucun de ces modules n'est à reprendre ;
- `subscriberType = BUSINESS_ENTITY` est **déprécié** · les abonnements concernés migrent vers
  `CUSTOMER`, et le profil légal suit par la clé étrangère ;
- la domiciliation **lit** le profil légal au lieu de resaisir `legalName`, `legalForm`,
  `registrationNumber`, `taxNumber`, `legalRepresentativeName` ;
- `business_entity` garde l'entité exploitante, reconnue par `is_operator` · le drapeau reste
  nécessaire et le restera.

Le point de vigilance : la migration de `subscriberType = BUSINESS_ENTITY` doit créer un
`Customer` pour chaque société cliente qui n'en a pas, puis réécrire `subscriber_code`. Elle est
irréversible, donc elle se fait en deux temps · une migration qui ajoute et remplit, une seconde
qui retire l'ancienne valeur une fois la première vérifiée en production.

## 4. Lot 1 · La fiche entreprise complète

Une fois le modèle arrêté. Ce que la fiche doit porter et ne porte pas :

**Identité légale** — représentant légal (nom, qualité, pièce d'identité), capital social,
date d'immatriculation, secteur d'activité codifié (liste fermée, non plus du texte libre),
forme juridique en liste fermée, régime de TVA (assujetti, exonéré, et le texte de l'exonération
qui doit figurer sur la facture).

**Coordonnées** — plusieurs adresses typées (siège, facturation, livraison), plusieurs téléphones
typés, courriel de facturation distinct du courriel général, site web, logo.

**Commercial** — conditions de paiement (échéance en jours), plafond d'encours, mode de règlement
préféré, devise de facturation si elle diffère de la devise de base, société mère et groupe.

**Validation par forme juridique** — le NIU et le RCCM n'ont pas le même format pour une SARL et
une entreprise individuelle. Aujourd'hui `rccmNumber` n'est pas vérifié du tout et `niuNumber`
accepte n'importe quoi entre 12 et 18 caractères.

Livrables : une migration dev + prod, le modèle, les DTO, la validation, les points d'entrée de
mise à jour par bloc (comme `PATCH /address` existant), les tests.

## 5. Lot 2 · Le compte entreprise · abonnement d'ensemble réparti entre les membres

**C'est le but principal, et ce n'est pas un carnet d'adresses.** Une entreprise souscrit **un**
abonnement pour ses collaborateurs, en répartit l'usage entre eux, et c'est elle seule qui est
facturée, alertée et qui contrôle la dépense.

Rien de tout cela n'existe : `Member` n'a pas d'employeur, un abonnement a un souscripteur
unique, et un pass appartient à une personne.

### 5.1 Le rattachement, et ce qu'il porte

Une table entre l'entreprise et des personnes, avec un **rôle** qui décide de droits et non
seulement d'un libellé :

| Rôle | Ce qu'il peut |
|---|---|
| représentant légal | signe les contrats de l'entreprise |
| signataire autorisé | signe par délégation |
| contact facturation | reçoit les factures et les relances |
| administrateur du compte | répartit l'abonnement, ouvre et ferme des accès, voit tout |
| collaborateur | consomme sa part, voit la sienne seulement |

### 5.2 La répartition de l'abonnement

Le point le plus délicat du lot. L'entreprise détient le droit, les collaborateurs le consomment.
Questions auxquelles le modèle doit répondre, et qui n'ont pas de réponse aujourd'hui :

- **quelle unité se répartit** · des jours, des heures, des places, un montant, ou plusieurs à la
  fois selon la formule ;
- **quota nominatif ou pot commun** · 5 jours chacun, ou 50 jours que n'importe qui tire jusqu'à
  épuisement, ou les deux par niveau ;
- **que fait un dépassement** · refus, facturation en supplément à l'entreprise, ou alerte sans
  blocage · le module `BookingOverageGuard` existe déjà et traite ce cas pour un abonné unique ;
- **qui ajuste** · l'administrateur du compte en libre-service, ou le back-office seulement ;
- **que devient le solde d'un collaborateur qui part** en cours de période.

Cela touche `Subscription`, `Pass`, les droits d'usage (`entitlement`) et le décompte.

### 5.3 La facturation et les alertes au niveau de l'entreprise

- une seule facture, à l'entreprise, quel que soit le nombre de collaborateurs ;
- le détail de consommation par collaborateur **en annexe** de la facture, pas en lignes
  facturables · sinon le total change de nature ;
- les alertes (échéance, fin d'abonnement, solde bas, dépassement) vont au contact facturation et
  à l'administrateur du compte, jamais au collaborateur concerné seul ;
- les relances existantes (`dunning`) visent déjà un client · elles suivent sans changement.

### 5.4 Le contrôle du portefeuille et des réservations

- **qui peut réserver** au nom de l'entreprise, et dans la limite de quoi ;
- **le portefeuille** est-il unique à l'entreprise, ou un portefeuille par collaborateur alimenté
  par elle · décision à prendre, elle change le modèle de `wallet` ;
- **le plafond par collaborateur** et le plafond global, avec refus au-delà ;
- **la validation préalable** d'une réservation par l'administrateur du compte · à confirmer si
  c'est voulu, cela ajoute un état au cycle de vie d'une réservation.

### 5.5 Le choix de formule par collaborateur

Une entreprise peut vouloir des niveaux différents selon les personnes · un dirigeant en bureau
privé, des collaborateurs en espace partagé. Cela signifie **plusieurs abonnements sous un même
compte entreprise**, facturés ensemble, et non un abonnement unique réparti. Les deux cas doivent
coexister.

### 5.6 Le portail client

- un collaborateur voit sa part, sa consommation, ses réservations ;
- un administrateur du compte voit l'entreprise entière, répartit, et pilote les plafonds ;
- un contact facturation voit les factures et l'encours.

### 5.7 Conséquence sur les contrats

Le signataire du client cesse d'être déduit : il est désigné, par le rôle de représentant légal ou
de signataire autorisé — exactement le correctif appliqué côté espace dans `378496a`.

## 6. Lot 3 · Administration et contrôle

- **Statut motivé** — bloquer une entreprise sans consigner pourquoi, par qui et depuis quand
  n'est pas exploitable. Aujourd'hui `PATCH /status` ne prend qu'une valeur.
- **Plafond d'encours** — avec le refus de nouvelle commande au-delà, en s'appuyant sur
  `BillingReceivables` qui calcule déjà l'encours réel.
- **Gestionnaire de compte** — l'utilisateur interne responsable, pour le filtrage et les
  relances.
- **Journal d'activité consolidé** — l'audit AOP enregistre déjà les écritures ; il manque la vue
  par entreprise.
- **Droits** — qui peut modifier une fiche légale, qui peut lever un blocage, qui peut relever un
  plafond. À brancher sur le RBAC existant, pas à réinventer.

## 7. Lot 4 · La vue consolidée et la recherche

- **Un point d'entrée « 360 »** par entreprise : fiche, personnes rattachées, abonnements et
  passes, encours et factures, réservations, documents et état KYC, contrats, domiciliation.
  Il n'existe rien de tel aujourd'hui, pour aucun type de client.
- **La recherche** — les onze critères actuels sont des égalités. Il manque la recherche plein
  texte, les tranches (encours, date d'entrée), le tri, et le filtre par état KYC ou par
  gestionnaire.
- **L'export** — CSV de la liste filtrée.

## 8. Lot 5 · Cycle de vie

- **Parcours d'entrée** — une fiche créée est rarement complète. Un état d'avancement
  (`INCOMPLETE`, `EN_VERIFICATION`, `ACTIVE`) et la liste de ce qui manque, plutôt qu'un refus de
  création sur un champ obligatoire.
- **Fusion de doublons** — conséquence directe du lot 0 ; irréversible, donc avec prévisualisation
  et trace d'audit, sur le modèle de `member-purge` qui existe déjà.
- **Archivage et restauration** — la suppression est déjà logique ; la restauration n'existe pas.

## 9. Lot 6 · Documentation

- `docs/frontend/business.md` — tous les points d'entrée, les charges utiles, les codes d'erreur,
  les champs obligatoires par forme juridique. Au format des documents existants
  (`docs/frontend/customer.md` comme modèle).
- `docs/frontend/contract.md` — **à compléter, pas à recréer** : 2364 lignes existent et ne
  mentionnent nulle part `businessCode` ni l'entité exploitante. Il faut y documenter que la
  génération exige une entreprise désignée exploitante, ce que `PATCH /businesses/{code}/operator`
  fait, et pourquoi `businessCode` ne doit jamais porter le code du souscripteur.
- `docs/backend/centre-gestion-entreprises.md` — le modèle retenu et le pourquoi, pour que la
  décision du lot 0 ne se reperde pas.

## 10. Ordre, et ce que chaque lot débloque

| Lot | Débloque | Migrations | Modules touchés |
|---|---|---|---|
| 0 · mesurer et décider | tout le reste | aucune | **fait** · modèle arrêté, mesure à prendre |
| 1 · fiche complète | facturation conforme, contrats justes | dev + prod | company, billing, contract |
| 2 · personnes | formules entreprise, signature, portail | dev + prod | company, client, contract, booking |
| 3 · contrôle | pilotage du risque client | dev + prod | company, billing, security |
| 4 · vue 360 | le travail quotidien de l'équipe | aucune | company, lecture seule ailleurs |
| 5 · cycle de vie | nettoyage des données | dev + prod | company |
| 6 · documentation | le frontend | aucune | — |

**Ordre retenu : lots 1 et 2 ensemble.** Une fiche complète sans personne rattachée reste un
formulaire, pas un centre de gestion.

Mais le lot 2 tel qu'il est maintenant décrit pèse plus que le lot 1 à lui seul : la répartition
d'un abonnement entre collaborateurs touche `Subscription`, `Pass`, les droits d'usage et le
décompte. Il se livrera en tranches, dans cet ordre, chacune utilisable seule :

| Tranche | Contenu | Utilisable pour |
|---|---|---|
| 2a | rattachement + rôles + droits | savoir qui est qui, qui signe, qui reçoit les factures |
| 2b | facturation et alertes au niveau entreprise | une seule facture, les bons destinataires |
| 2c | répartition de l'abonnement et dépassement | le but principal |
| 2d | portefeuille, plafonds, réservations | le contrôle de la dépense |
| 2e | portail client par rôle | le libre-service |

La tranche 2c est celle qui demande vos réponses sur l'unité de répartition et le traitement du
dépassement · voir §5.2. Je ne l'écris pas avant.

## 11. Ce qu'il me faut avant de commencer

1. ~~La décision du lot 0~~ · **arrêtée** · voir §3.2.
2. Les chiffres du SQL de mesure, pris en production (§3.1).
3. Les réponses de §5.2 sur la répartition : unité, quota nominatif ou pot commun, traitement du
   dépassement, qui ajuste, et le sort du solde d'un partant. Sans elles, la tranche 2c n'a pas
   de modèle.
4. Portefeuille unique à l'entreprise, ou un par collaborateur alimenté par elle (§5.4).
5. Une réservation de collaborateur doit-elle être validée par l'administrateur du compte (§5.4).
6. Les listes fermées que vous utilisez réellement : formes juridiques congolaises retenues,
   secteurs d'activité, régimes de TVA.
7. Le format exact du NIU et du RCCM au Congo, pour la validation du lot 1.
