# Audit & plan d'amélioration — Modules Contrat, Ressource, Member, Customer

> Dernière mise à jour : 2026-06-08
> Statut : audit + propositions — à prioriser et planifier par phases

---

## Sommaire

1. [Module Contrat](#1-module-contrat-featurescontract)
2. [Module Ressource](#2-module-ressource-featuresressource)
3. [Module Member](#3-module-member-featuresclientmember)
4. [Module Customer](#4-module-customer-featuresclientcustomer)
5. [Constats transverses](#5-constats-transverses)

---

## 1. Module Contrat (`features/contract`)

### 1.1 État actuel

Module solide pour le CRUD, la machine à états et la génération documentaire :

- **Entités** : `Contract` (code unique, owner polymorphe, statut, type de renouvellement, dates, codes de documents brouillon/signé) et `ContractParty` (rôle, ordre de signature, `mustSign`).
- **Statuts** (`ContractStatus`, 10 états) : `DRAFT → GENERATED → UNDER_REVIEW/AWAITING_SIGNATURE → SIGNED → ACTIVE → SUSPENDED/EXPIRED/TERMINATED/CANCELLED`.
- **`ContractRenewalType`** : `NONE`, `FIXED_TERM`, `AUTO_RENEW`.
- **`ContractPartyRole`** : `BENEFICIARY`, `OPERATOR`, `SIGNATORY`, `BILLING_CONTACT`.
- **API** (13 endpoints) : templates, CRUD, transitions de statut (activate/suspend/terminate/cancel), preview HTML, génération PDF, régénération de brouillon, attachement du document signé avec auto-activation.
- **Génération** : 4 templates Thymeleaf hardcodés, rendu HTML → PDF (openhtmltopdf), substitution de tokens `{{key}}`, événement outbox `CONTRACT_DRAFT_GENERATED`.
- **Automatisation** (`ContractLifecycleWorker`, horaire) : auto-activation des contrats `SIGNED` à échéance, auto-expiration des contrats `ACTIVE`/`SUSPENDED` dépassant `endDate`.
- **Email** : `ContractEmailNotifier` prêt à l'emploi (HTML + pièce jointe PDF) mais jamais branché.

### 1.2 Faiblesses identifiées

| Problème | Référence | Gravité |
|---|---|---|
| `renewalType` (`NONE`/`FIXED_TERM`/`AUTO_RENEW`) stocké mais **aucune logique de renouvellement** | `Contract.java:66` | Haute |
| Aucun workflow d'approbation pour `UNDER_REVIEW` — le statut peut être forcé directement | `ContractServiceImpl.java` | Haute |
| Pas de gestion d'amendements/versions — toute modification post-signature est destructive | `Contract.java` | Haute |
| Pas d'intégration e-signature (DocuSign/HelloSign…) — `markSigned()` purement manuel | `ContractServiceImpl.java:183` | Haute |
| `signOrder` (ordre de signature) défini mais jamais appliqué | `ContractServiceImpl.java:351-353` | Moyenne |
| `ContractEmailNotifier.notifyGenerated()` jamais appelé — code mort | `ContractEmailNotifier.java` | Moyenne |
| Liaison contrat ↔ abonnement à moitié faite : FK ajoutée en V66 mais pas de mapping ORM ni service | `ContractServiceImpl` / `ContractRepository.java` | Moyenne |
| Locale codée en dur (`Locale.FRANCE`), pas d'i18n des templates | `ContractGenerationServiceImpl.java:100` | Moyenne |
| `termination_reason` réutilisé pour `SUSPENDED` et `TERMINATED`/`CANCELLED` — ambiguïté sémantique | `ContractServiceImpl.java:259, 263, 268` | Basse |
| Templates limités à 4 entrées hardcodées — pas de gestion dynamique en base | `ContractGenerationServiceImpl.java:212-214` | Basse |

### 1.3 Plan d'amélioration (priorisé)

1. **Brancher les notifications email** sur les événements de génération/signature/expiration — le code existe (`ContractEmailNotifier`), il manque le câblage outbox → notifier.
2. **Implémenter le moteur de renouvellement** : calcul de `renewalDate`, génération automatique d'un contrat de renouvellement pour `AUTO_RENEW`, alerte avant échéance pour `FIXED_TERM`.
3. **Ajouter un vrai workflow d'approbation** sur `UNDER_REVIEW` : rôles réviseurs, commentaires, validation/rejet avant passage à `AWAITING_SIGNATURE`.
4. **Faire respecter `signOrder`** : bloquer la signature d'une partie tant que les parties précédentes n'ont pas signé.
5. **Finaliser le pont contrat ↔ abonnement** : mapping JPA + endpoint pour lier/consulter les contrats par abonnement.
6. **Ajouter le versioning/amendements** : historique des modifications avec diff, en particulier post-signature.
7. **Supporter plusieurs locales** dans la génération de templates.
8. **Distinguer la raison de suspension de la raison de résiliation** (deux champs ou un type de raison).

### 1.4 Nouvelles fonctionnalités proposées

- **Intégration e-signature** (DocuSign/Yousign/HelloSign) avec callback webhook pour auto-activation.
- **Alertes d'expiration** (J-30/J-7) envoyées aux parties et à l'équipe commerciale.
- **Bibliothèque de templates dynamiques** stockée en base (au lieu du hardcoding), avec éditeur admin.
- **Export en masse** des contrats (CSV/PDF) et opérations de statut par lot.
- **Tableau de bord contrats** : taux de renouvellement, contrats à risque d'expiration, pipeline de signature.
- **Bibliothèque de clauses paramétrables** réutilisables entre templates.

---

## 2. Module Ressource (`features/ressource`)

### 2.1 État actuel

Module très complet :

- **Entités** : `Resource` (code, capacité, zone, statut, `bookingEnabled`/`portalVisible`, ordre d'affichage), `ResourceType`, `ResourceGroup`, `ResourcePolicy` (durées min/max, préavis, annulation), `ResourceAvailability` (créneaux de 30 min, capacité totale/restante, verrou optimiste `@Version`), `ResourceAmenities` + `ResourceAmenityLink` (quantité, optionnel, prix supplémentaire), `ResourceClosure`, `ResourcePricingRule` (multi-critères : unité, jour, plage horaire, plage de validité, dernière minute, priorité, 3 modes d'ajustement), `ResourcePhoto`.
- **Enums** : `ResourceStatus` (5), `ResourceBookingUnit` (5), `ResourcePriceAdjustmentType` (3).
- **API** : CRUD ressources + recherche basique/avancée, gestion des amenities, disponibilités (création, regroupement, capacité restante, réservation/libération avec verrou optimiste), règles de tarification (CRUD + devis), fermetures, galerie photo, calendrier public (statuts `AVAILABLE`/`PARTIAL`/`FULL`).
- **Logique métier** : spécifications de recherche multi-champs, vérification de chevauchement disponibilité/fermeture, expiration automatique des créneaux passés, calcul de devis multi-critères avec priorité.

### 2.2 Faiblesses identifiées

| Problème | Référence | Gravité |
|---|---|---|
| Granularité de créneau (`SLOT_MINUTES = 30`) et horaires de travail (8h–20h) **codés en dur**, alors que `slotDurationMinutes` existe sur l'entité mais n'est pas utilisé | `ResourceAvailabilityServiceImpl.java:42-45` | Moyenne |
| `expirePastAvailabilitySlots()` exécuté en transaction `readOnly = true` alors qu'il a un effet de bord (mise à jour de slots) | `ResourceAvailabilityServiceImpl.java:78, 88` | Moyenne |
| Le devis (`quote`) ne croise pas les contraintes de `ResourcePolicy` (durée min/max, préavis) — un devis peut réussir pour une réservation qui échouera ensuite | `ResourcePricingRuleService` | Moyenne |
| `price`/`adjustmentValue` en `Integer` sans contraintes (`@NotNull`/`@Min`) — risque de NPE dans le calcul | `ResourcePricingRule.java:39, 57` | Moyenne |
| Pas de cascade de suppression Resource → Photos (orphelins possibles) | `Resource.java` | Basse |
| `displayOrder` des photos nullable sans valeur par défaut — ordre d'affichage imprévisible | `ResourcePhoto.java:38` | Basse |
| Recherche basée sur une fonction SQL native `search_resource(:query)` non vérifiable côté Java | `ResourceRepository.java:28-37` | Basse |

### 2.3 Plan d'amélioration (priorisé)

1. **Rendre la granularité des créneaux configurable** (par ressource ou politique) au lieu de la constante globale, et exploiter réellement `slotDurationMinutes`.
2. **Corriger la transaction `readOnly`** sur `expirePastAvailabilitySlots()` pour un comportement cohérent.
3. **Croiser le devis de tarification avec la politique de réservation** (durée min/max, préavis) avant de retourner un prix.
4. **Ajouter des contraintes de validation** sur les champs monétaires des règles de tarification.
5. **Mettre en cascade la suppression des photos** lors de la suppression logique d'une ressource, et garantir un ordre déterministe (`displayOrder` non-null + index).
6. **Documenter ou remplacer la dépendance à `search_resource`** par une spécification Java maintenable, ou vérifier sa présence en migration.

### 2.4 Nouvelles fonctionnalités proposées

- **Fermetures récurrentes** (motif hebdomadaire/mensuel/jours fériés) au lieu d'entrées ponctuelles.
- **Support multi-site/multi-localisation** (champ `siteId`/`locationId` sur `Resource`).
- **Tarification dynamique basée sur l'occupation** (yield management) en complément des règles statiques.
- **Packs/bundles de ressources** (ex. « Salle de réunion + Projecteur + Pause-café » comme offre unique).
- **Analytique d'occupation** : taux d'utilisation historique, heures de pointe, prévision de la demande.
- **Informations d'accessibilité** (accès PMR, équipements spécifiques) sur la fiche ressource.
- **Gestion des dépendances entre ressources** (ressources mutuellement exclusives).

---

## 3. Module Member (`features/client/member`)

### 3.1 État actuel

Module riche couvrant tout le cycle de vie membre :

- **Entités** : `Member` (lien 1-1 avec `Users`, lien N-1 avec `Customer`, `memberId` au format `MBR-YYYYMM-NNNNN`, email/téléphone uniques, `portalAccess`, suppression logique) et `MemberProfile` (poste, rôle, date de naissance, adresse).
- **`MemberStatus`** (8 états) : `ACTIVE`, `PENDING`, `UNDER_REVIEW`, `PENDING_CORRECTION`, `REJECTED`, `INACTIVE`, `SUSPENDED`, `ARCHIVED`.
- **API** (20 endpoints) : inscription portail, création admin, recherche (basique, avancée, fuzzy via `similarity()` PostgreSQL), gestion de profil (admin + self-service), changement de statut, gestion de l'accès portail, transfert entre clients, suppression logique.
- **Création** : normalisation des entrées, validations multiples, résolution/création du `Customer`, création du compte `User`, génération du `memberId`, statut initial `PENDING`, profil vide auto-créé, initialisation KYC, emails de bienvenue asynchrones.
- **Intégrations** : KYC (`KycAutomationService`), Customer (création/synchronisation), Sécurité (`UserService`/`UserProvisioningService`), Email (Thymeleaf), recherche full-text PostgreSQL.

### 3.2 Faiblesses identifiées — 2 bugs fonctionnels prioritaires

| Problème | Référence | Gravité |
|---|---|---|
| **Validation téléphone potentiellement inversée** — `validatePhoneNumber()` semble retourner `true` sur un numéro invalide | `MemberServiceImpl.java:540, 549` | **Haute (bug)** |
| **`/members/search/by-name` en GET avec `@RequestBody`** — design REST invalide, doublon de `/search` | `MemberController.java:183-188` | Moyenne |
| Champs `emergency_contact_*` et `profile_picture_url` définis en base (V3) mais **non mappés** dans `MemberProfile` | `MemberProfile.java:29, 42` | Moyenne |
| `MemberPdfGenerationImpl` — service entièrement défini mais **jamais appelé** | `MemberPdfGenerationImpl.java` | Basse (code mort) |
| `transferCustomer()` sans aucune validation (client cible actif ? abonnements en cours ?) | `MemberServiceImpl.java:313-325` | Moyenne |
| Mots de passe générés envoyés **en clair par email** (TODO explicite non résolu) | `MemberServiceImpl.java:84` | Haute |
| `getAllMembers()` charge tout via `PageRequest.of(0, Integer.MAX_VALUE)` — risque mémoire | `MemberServiceImpl.java:237-247` | Moyenne |
| TODOs explicites non résolus : activation portail/profil, stratégie de mot de passe | `MemberController.java:41`, `MemberServiceImpl.java:84` | — |

### 3.3 Plan d'amélioration (priorisé)

1. **Corriger la validation téléphone** (vérifier `ValidationUtils.validatePhoneNumber()` et la négation manquante/en trop, écrire un test de régression).
2. **Sécuriser la distribution des identifiants** : lien d'activation à usage unique au lieu d'un mot de passe en clair par email.
3. **Refaire `/members/search/by-name`** en GET avec query params plutôt que `@RequestBody`.
4. **Mapper les champs déjà présents en base** (`emergency_contact_*`, `profile_picture_url`) sur `MemberProfile` et exposer leur CRUD.
5. **Ajouter des garde-fous au transfert de client** (statut du client cible, abonnements/contrats actifs à migrer ou bloquer).
6. **Brancher ou supprimer `MemberPdfGenerationImpl`** (clarifier l'intention produit).
7. **Paginer réellement `getAllMembers()`** ou documenter sa limite d'usage.
8. **Lever les TODOs** de `MemberController.java:41` et `MemberServiceImpl.java:84` (décisions produit en attente).

### 3.4 Nouvelles fonctionnalités proposées

- **Contacts d'urgence** (champ déjà présent en base — à exposer côté API/profil).
- **Upload de photo de profil** (champ déjà prévu dans le schéma).
- **Préférences de communication** (canaux email/SMS/WhatsApp, opt-in/opt-out marketing).
- **Gestion de badges/cartes d'accès** physiques avec niveaux d'habilitation.
- **Programme de parrainage** (codes de référence, suivi des conversions).
- **Tableau de bord d'engagement membre** (fréquentation, utilisation des espaces, score de fidélité).
- **File d'approbation explicite** `PENDING → ACTIVE` avec checklist KYC avant validation.

---

## 4. Module Customer (`features/client/customer`)

### 4.1 État actuel

Module compact :

- **Entité `Customer`** : `customerId` unique (format `CUS-[TYPE]-MMYY-SEQ`), `type` (`PERSON`/`COMPANY`), coordonnées, `isMember`, adresse (fetch eager), `status`, suppression logique.
- **`CustomerType`** : `PERSON`, `COMPANY`. **`CustomerStatus`** : `ACTIVE` (par défaut), `PENDING`, `INACTIVE`, `SUSPENDED`, `ARCHIVED` — sans machine à états formelle.
- **API** (10 endpoints) : CRUD, recherche par email/multi-champs, listes paginées et résumés, changement de statut, suppression logique.
- **Logique** : validations à la création/mise à jour, génération de `customerId`, résolution/synchronisation avec Member, intégration KYC, email de bienvenue post-commit.
- **Documentation** : `docs/customer-frontend-api.md` à jour et fidèle à l'implémentation.

### 4.2 Faiblesses identifiées — 1 bug critique

| Problème | Référence | Gravité |
|---|---|---|
| **Validation téléphone à logique inversée — rejette les numéros valides** (`\|\|` au lieu de `!`) | `CustomerServiceImpl.java:269` | **Critique (bug bloquant)** |
| Validation WhatsApp désactivée (commentée) | `CustomerServiceImpl.java:273-277` | Moyenne |
| Aucune vérification d'unicité email/téléphone **avant** insertion — repose sur la contrainte DB | `CustomerServiceImpl.createCustomer()` | Moyenne |
| Aucune vérification d'unicité email lors d'une **mise à jour** | `CustomerMapperDecorator.java:100` | Moyenne |
| `isMember` mappé mais jamais renseigné/lu — incohérence de design avec le module Member | `Customer.java:66` | Basse |
| Pas de garde-fou sur les transitions de statut (ex. `ARCHIVED → ACTIVE` permis sans règle) | `CustomerServiceImpl.changeStatus:121` | Moyenne |
| Concaténation `firstname + lastname` sans null-check → `"null null"` possible pour les `PERSON` | `CustomerMapperDecorator.toSummary:53` | Basse |
| `CompanyCustomerResponse` et `CustomerCreationResponse` définis mais jamais utilisés | `dto/response` | Basse (code mort) |
| Adresse en fetch `EAGER` — risque N+1 sur les listes | `Customer.java:61` | Basse |

### 4.3 Plan d'amélioration (priorisé)

1. **Corriger immédiatement le bug de validation téléphone** (`CustomerServiceImpl.java:269`) — il bloque potentiellement toute création/mise à jour avec un numéro valide. *(À traiter conjointement avec le bug similaire du module Member — possible régression partagée dans `ValidationUtils`.)*
2. **Réactiver la validation WhatsApp** ou documenter pourquoi elle est désactivée.
3. **Ajouter une pré-vérification d'unicité** (email/téléphone) avant insertion/mise à jour, avec une exception métier claire plutôt qu'une erreur de contrainte DB.
4. **Définir une vraie machine à états** pour `CustomerStatus` (transitions autorisées, garde-fous).
5. **Corriger le null-safety** sur la concaténation du nom affiché.
6. **Clarifier/utiliser `isMember`** : soit le synchroniser depuis le module Member, soit le retirer.
7. **Nettoyer les DTO morts** (`CompanyCustomerResponse`, `CustomerCreationResponse`) ou les brancher s'ils répondent à un besoin réel.
8. **Passer `Address` en fetch lazy** par défaut, avec `EntityGraph` ciblé si besoin.

### 4.4 Nouvelles fonctionnalités proposées

- **Détection/fusion de doublons** (par similarité email/téléphone/nom).
- **Segmentation et tags clients** (VIP, prospect, à risque…).
- **Historique de communication / timeline d'activité** (emails envoyés, interactions).
- **Notes internes CRM** consultables par l'équipe.
- **Score de santé client / risque de churn**, alimenté par les données de facturation et de réservation.
- **Export de données conforme RGPD** (au-delà de la suppression logique).
- **Limite de crédit / solde prépayé** pour les clients entreprise.

---

## 5. Constats transverses

Trois constats traversent les quatre modules et méritent une action groupée :

### 5.1 Bug de validation téléphone potentiellement partagé

La même logique inversée apparaît dans `CustomerServiceImpl.java:269` (confirmé critique) et probablement `MemberServiceImpl.java:540, 549`. Il est recommandé de vérifier directement `ValidationUtils.validatePhoneNumber()` — un seul correctif central pourrait résoudre les deux occurrences, accompagné de tests de régression sur les deux services.

### 5.2 Code mort récurrent

Plusieurs services/DTOs sont entièrement écrits mais jamais branchés :
- `ContractEmailNotifier` (module Contrat),
- `MemberPdfGenerationImpl` (module Member),
- `CompanyCustomerResponse` / `CustomerCreationResponse` (module Customer).

Cela suggère des fonctionnalités commencées puis interrompues. Chaque cas doit être tranché : terminer le câblage (généralement peu coûteux puisque le code existe déjà) ou supprimer proprement.

### 5.3 Relation `Customer.isMember` ↔ `Member`

Le champ `isMember` sur `Customer` semble avoir été pensé pour refléter la relation avec le module Member, mais n'est jamais renseigné ni lu. C'est une bonne occasion de clarifier et documenter le modèle de données client/membre (synchronisation automatique à la création/suppression d'un `Member`, ou suppression du champ s'il est redondant avec la relation `Member.customer`).

---

## Priorités recommandées (vue d'ensemble)

| Priorité | Action | Modules concernés |
|---|---|---|
| 1 | Corriger le(s) bug(s) de validation téléphone | Customer, Member |
| 2 | Sécuriser la distribution des mots de passe membres | Member |
| 3 | Brancher les notifications email contrat | Contrat |
| 4 | Trancher le sort du code mort (brancher ou supprimer) | Contrat, Member, Customer |
| 5 | Implémenter le moteur de renouvellement de contrats | Contrat |
| 6 | Ajouter le workflow d'approbation des contrats | Contrat |
| 7 | Rendre la granularité des créneaux de ressources configurable | Ressource |
| 8 | Définir une machine à états formelle pour `CustomerStatus` | Customer |
| 9 | Clarifier la relation `isMember` ↔ `Member` | Customer, Member |
