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

### 3.2 La décision

Trois modèles possibles, détaillés dans la question qui suit ce plan. Elle porte sur une seule
chose : **qu'est-ce que le client, commercialement ?** Tout le reste en découle, y compris le
nombre de migrations et les modules à toucher.

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

## 5. Lot 2 · Les personnes rattachées

Le cœur de ce qui manque.

- Une table de rattachement entre une entreprise et des personnes, avec un **rôle** :
  représentant légal, signataire autorisé, contact facturation, contact administratif,
  collaborateur.
- Les points d'entrée pour rattacher, détacher, changer le rôle, lister.
- La conséquence sur les contrats : le signataire du client cesse d'être déduit, il est désigné —
  exactement le correctif qu'on vient d'appliquer côté espace.
- La conséquence sur les réservations : qui peut réserver au nom de l'entreprise et sur quel
  budget.
- La conséquence sur le portail client : un collaborateur voit sa consommation, un contact
  administratif voit celle de l'entreprise.

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
| 0 · mesurer et décider | tout le reste | aucune | aucun |
| 1 · fiche complète | facturation conforme, contrats justes | dev + prod | company, billing, contract |
| 2 · personnes | formules entreprise, signature, portail | dev + prod | company, client, contract, booking |
| 3 · contrôle | pilotage du risque client | dev + prod | company, billing, security |
| 4 · vue 360 | le travail quotidien de l'équipe | aucune | company, lecture seule ailleurs |
| 5 · cycle de vie | nettoyage des données | dev + prod | company |
| 6 · documentation | le frontend | aucune | — |

Les lots 1 et 2 se livrent ensemble si vous voulez un résultat visible vite : une fiche complète
sans personne rattachée reste un formulaire, pas un centre de gestion.

## 11. Ce qu'il me faut avant de commencer

1. **La décision du lot 0** (question posée à la suite de ce plan).
2. Les chiffres du SQL de mesure, pris en production.
3. Les listes fermées que vous utilisez réellement : formes juridiques congolaises retenues,
   secteurs d'activité, régimes de TVA.
4. Le format exact du NIU et du RCCM au Congo, pour la validation du lot 1.
