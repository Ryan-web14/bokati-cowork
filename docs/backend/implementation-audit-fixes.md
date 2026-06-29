# Implémentation Audit — Contrat, Ressource, Member, Customer

> Date d'implémentation : 2026-06-18
> Référence audit : `docs/to-implement/audit-contrat-resource-member-customer.md` (08/06/2026)
> Statut : **23 items implémentés sur 26 identifiés** (1 déjà corrigé, 2 reportés)

---

## Table des matières

1. [Résumé des changements](#1-résumé-des-changements)
2. [Module Customer](#2-module-customer)
3. [Module Member](#3-module-member)
4. [Module Contrat](#4-module-contrat)
5. [Module Ressource](#5-module-ressource)
6. [Migration de données](#6-migration-de-données)
7. [Guide d'intégration Frontend](#7-guide-dintégration-frontend)
8. [Items reportés](#8-items-reportés)

---

## 1. Résumé des changements

### Par criticité

| Phase | Items | Description |
|-------|-------|-------------|
| **Phase 1 — Bugs critiques** | 3 | Validation téléphone corrigée, mot de passe sécurisé, endpoint REST invalide supprimé |
| **Phase 2 — Intégrité** | 6 | Unicité email/phone, machine à états Customer, null-safety, transactions, pagination |
| **Phase 3 — Nettoyage** | 5 | Code mort supprimé, notifications contrat branchées, champs DB mappés, isMember synchronisé |
| **Phase 4 — Compléments** | 4 | Contraintes prix, devis croisé policy, signOrder enforced, suspension_reason séparé |
| **Phase 5 — Features** | 5 | Renouvellement contrats, workflow approbation, templates dynamiques, alertes expiration |

### Fichiers modifiés/créés

- **Modifiés** : ~25 fichiers Java
- **Supprimés** : 4 fichiers (dead code)
- **Créés** : 3 fichiers Java + 1 migration SQL
- **Migration** : `V163__add_contract_suspension_reason.sql`

---

## 2. Module Customer

### 2.1 Validation téléphone corrigée

**Fichier** : `core/utils/validation/ValidationUtils.java`

**Avant** : `validatePhoneNumber()` retournait `!matches()` — un numéro valide retournait `false`.

**Après** : Retourne `matches()` directement. Null-safe. Tous les appelants corrigés (Customer, Member, Business).

**Pattern accepté** : `^0[456][0-9]{7}$` — numéros congolais 9 chiffres commençant par 04, 05 ou 06.

**Impact frontend** : Aucun changement d'API. Les numéros valides sont désormais acceptés au lieu d'être rejetés.

### 2.2 Validation WhatsApp réactivée

**Fichier** : `CustomerServiceImpl.java`

Le bloc de validation WhatsApp était commenté. Il est réactivé : si `whatsappPhone` est fourni, il doit passer la même validation que `phone`.

**Impact frontend** : Les requêtes avec un `whatsappPhone` invalide retourneront désormais une erreur 400. Valider côté frontend avant soumission.

### 2.3 Pré-vérification unicité email/téléphone

**Fichiers** : `CustomerServiceImpl.java`, `CustomerRepository.java`

**Création** : Avant d'insérer, le service vérifie :
- `existsByEmailAndDeletedFalse(email)` — rejette si l'email existe déjà
- `findByPhoneAndDeletedFalse(phone)` — rejette si le téléphone existe déjà

**Mise à jour** : Avant de modifier, vérifie :
- `existsByEmailAndIdNot(email, id)` — l'email n'est pas utilisé par un autre customer
- `existsByPhoneAndIdNot(phone, id)` — idem pour le téléphone

**Impact frontend** :

| Erreur | Code HTTP | Message |
|--------|-----------|---------|
| Email en doublon | 409 Conflict | `A customer with this email already exists` |
| Téléphone en doublon | 409 Conflict | `A customer with this phone number already exists` |

### 2.4 Machine à états CustomerStatus

**Fichier** : `CustomerServiceImpl.java`

Transitions autorisées :

```
PENDING    → ACTIVE, INACTIVE, ARCHIVED
ACTIVE     → SUSPENDED, INACTIVE, ARCHIVED
SUSPENDED  → ACTIVE, INACTIVE, ARCHIVED
INACTIVE   → ACTIVE, ARCHIVED
ARCHIVED   → (aucune — terminal)
```

**Impact frontend** :
- `PATCH /customers/{id}/status` retourne désormais **400** si la transition n'est pas autorisée
- Message : `Status transition from {current} to {target} is not allowed`
- Adapter les boutons d'action en fonction du statut courant

### 2.5 Null-safety sur le nom affiché

**Fichier** : `CustomerMapper.java`

`toSummary()` gère désormais les cas où `firstname` ou `lastname` est `null` — plus de `"null null"` dans les réponses.

### 2.6 Code mort supprimé

- `CompanyCustomerResponse.java` — supprimé
- `CustomerCreationResponse.java` — supprimé

### 2.7 Synchronisation `isMember`

Le champ `Customer.isMember` est désormais synchronisé automatiquement :
- `true` quand un Member est créé avec ce Customer
- `false` quand le dernier Member lié est supprimé (soft-delete)

**Impact frontend** : Le champ `isMember` dans les réponses Customer reflète désormais la réalité.

---

## 3. Module Member

### 3.1 Validation téléphone corrigée

Même correctif que Customer — tous les appelants dans `MemberServiceImpl` corrigés (création + mise à jour + WhatsApp).

### 3.2 Mot de passe sécurisé

**Fichier** : `MemberServiceImpl.java`

**Avant** : Le mot de passe temporaire généré par l'admin était envoyé en clair par email.

**Après** : Le mot de passe n'est plus inclus dans aucun email. À la place, un lien de définition de mot de passe à usage unique est envoyé via `PasswordResetService.generatePasswordResetToken()`.

**Impact frontend** :
- Le flow de création admin ne change pas côté API
- L'email de bienvenue ne contient plus de mot de passe
- Le membre reçoit un second email avec un lien pour définir son mot de passe
- Le formulaire de reset password existant est réutilisé

### 3.3 Endpoint doublon supprimé

`GET /sni/api/v1/members/search/by-name` — supprimé (doublon de `POST /sni/api/v1/members/search` avec violation HTTP spec `@RequestBody` sur GET).

**Impact frontend** : Utiliser `POST /members/search` à la place. Le body `MemberSearchCriteria` reste identique.

### 3.4 Pagination sécurisée

`getAllMembers()` et `getAllMembersSummary()` sont désormais capés à 1000 résultats maximum (au lieu de `Integer.MAX_VALUE`). Utiliser l'endpoint paginé `GET /members` pour les listes complètes.

### 3.5 Champs profil débloqués

**Fichier** : `MemberProfile.java`

Les champs suivants, présents en base de données depuis V3, sont désormais mappés et accessibles :

| Champ | Type | Description |
|-------|------|-------------|
| `profilePictureUrl` | String | URL de la photo de profil |
| `gender` | String | Genre |
| `emergencyContactName` | String | Nom du contact d'urgence |
| `emergencyContactPhone` | String | Téléphone du contact d'urgence |

**Impact frontend** : Ces champs sont désormais disponibles dans les réponses profil et modifiables via les endpoints de mise à jour du profil.

### 3.6 Code mort supprimé

- `MemberPdfGenerationImpl.java` — supprimé
- `MemberPdfGeneration.java` (interface) — supprimé

---

## 4. Module Contrat

### 4.1 Workflow d'approbation (NOUVEAU)

**Fichiers** : `ContractController.java`, `ContractServiceImpl.java`

Deux nouveaux endpoints pour le cycle de revue :

#### `POST /sni/api/v1/contracts/{contractCode}/approve`

Approuve un contrat en statut `UNDER_REVIEW` → le passe en `AWAITING_SIGNATURE`.

| Paramètre | Type | Requis | Description |
|-----------|------|--------|-------------|
| `reviewedBy` | Long | Oui | ID du reviewer |
| `comment` | String | Non | Commentaire d'approbation |

**Réponse** : `ContractResponse` avec le nouveau statut.

#### `POST /sni/api/v1/contracts/{contractCode}/reject-review`

Rejette un contrat en revue → le repasse en `GENERATED` pour correction.

| Paramètre | Type | Requis | Description |
|-----------|------|--------|-------------|
| `reviewedBy` | Long | Oui | ID du reviewer |
| `comment` | String | Non | Motif du rejet |

**Réponse** : `ContractResponse` avec le statut `GENERATED`.

**Impact frontend** : Ajouter deux boutons dans l'écran de revue contrat : "Approuver" et "Rejeter". Le commentaire peut être affiché dans le champ `reviewComment` de la réponse.

### 4.2 Moteur de renouvellement (NOUVEAU)

**Fichier** : `ContractLifecycleAutomationServiceImpl.java`

Le worker horaire exécute désormais deux nouvelles tâches :

**Auto-renouvellement (`AUTO_RENEW`)** :
- Les contrats `ACTIVE` avec `renewalType=AUTO_RENEW` dont `endDate` est dépassée sont automatiquement renouvelés
- Un nouveau contrat `DRAFT` est créé avec les mêmes propriétés, nouvelles dates (même durée), et `renewedFromCode` pointant vers l'original
- Événement outbox : `CONTRACT_AUTO_RENEWED`

**Alertes d'expiration (`FIXED_TERM` et `NONE`)** :
- Envoi d'alertes à J-30 et J-7 pour les contrats `ACTIVE` qui ne sont pas en `AUTO_RENEW`
- Événement outbox : `CONTRACT_EXPIRY_ALERT` avec `daysUntilExpiry`

**Impact frontend** :
- Afficher le champ `renewedFromCode` dans le détail du contrat pour montrer la chaîne de renouvellement
- Créer des notifications pour les alertes d'expiration reçues via outbox

### 4.3 Séparation suspension/résiliation

**Fichier** : `Contract.java`, `ContractServiceImpl.java`

**Avant** : `terminationReason` était réutilisé pour SUSPENDED et TERMINATED/CANCELLED.

**Après** : Nouveau champ `suspensionReason` dédié. `terminationReason` est réservé aux statuts terminaux.

| Statut | Champ utilisé |
|--------|---------------|
| `SUSPENDED` | `suspensionReason` |
| `TERMINATED` | `terminationReason` |
| `CANCELLED` | `terminationReason` |
| `ACTIVE` (après dé-suspension) | `suspensionReason` vidé |

**Impact frontend** : Afficher `suspensionReason` pour les contrats suspendus et `terminationReason` pour les contrats terminés/annulés.

### 4.4 Enforcement du signOrder

**Fichier** : `ClientContractService.java`

Quand un membre tente de signer un contrat, le système vérifie que toutes les parties avec un `signOrder` inférieur (et `mustSign=true`) ont déjà signé. Sinon → 400 :

```
You cannot sign yet — previous signatories have not completed their signatures
```

**Impact frontend** : Afficher un message d'information si le bouton "Signer" est désactivé, indiquant que d'autres signataires doivent d'abord signer.

### 4.5 Notifications email branchées

**Fichier** : `ContractGenerationServiceImpl.java`

`ContractEmailNotifier.notifyGenerated()` est désormais appelé après chaque génération de PDF contrat. Le propriétaire reçoit un email avec le PDF en pièce jointe.

### 4.6 Templates dynamiques en base (NOUVEAU)

**Fichiers** : `ContractTemplate.java`, `ContractTemplateRepository.java`, `ContractGenerationServiceImpl.java`

**Entité `ContractTemplate`** :

| Champ | Type | Description |
|-------|------|-------------|
| `code` | String (120) | Identifiant unique |
| `name` | String (250) | Nom du template |
| `description` | Text | Description |
| `language` | String (10) | Langue (défaut: `fr`) |
| `version` | Integer | Numéro de version |
| `active` | Boolean | Actif/inactif |

**Comportement** :
- `listTemplates()` charge d'abord depuis la base. Si vide, retombe sur les 4 templates hardcodés
- `validate()` accepte les templates de la base ET les hardcodés
- Les templates Thymeleaf dans `resources/templates/contracts/` restent le moteur de rendu — le code du template détermine quel fichier Thymeleaf utiliser

**Migration V163** : La table est créée et pré-seedée avec les 4 templates existants.

**Impact frontend** :
- `GET /contracts/templates` retourne désormais les templates de la base
- Un admin CRUD pour les templates peut être construit sur cette entité (endpoints à ajouter si besoin)
- Les templates existants continuent de fonctionner sans changement

### 4.7 Nouveaux champs sur `ContractResponse`

| Champ | Type | Description |
|-------|------|-------------|
| `suspensionReason` | String | Motif de suspension (séparé de terminationReason) |
| `renewedFromCode` | String | Code du contrat d'origine (chaîne de renouvellement) |
| `reviewedBy` | Long | ID du reviewer qui a approuvé/rejeté la revue |
| `reviewComment` | String | Commentaire de revue |

---

## 5. Module Ressource

### 5.1 Contraintes de validation prix

**Fichier** : `ResourcePricingRule.java`

| Champ | Contrainte ajoutée |
|-------|-------------------|
| `price` | `@NotNull @Min(0)` — obligatoire, ≥ 0 |
| `adjustmentValue` | `@Min(0)` — ≥ 0 si fourni |

**Impact frontend** : Les formulaires de création/édition de règles tarifaires doivent valider ces contraintes côté client. Le backend retournera 400 si elles ne sont pas respectées.

### 5.2 Devis croisé avec ResourcePolicy

**Fichier** : `ResourcePricingRuleServiceImpl.java`

`GET /resource-pricing-rules/quote` vérifie désormais les contraintes de la politique de réservation **avant** de retourner le prix :

| Contrainte | Erreur si violée |
|------------|-----------------|
| Durée minimum | `Booking duration (X min) is below the minimum allowed (Y min)` |
| Durée maximum | `Booking duration (X min) exceeds the maximum allowed (Y min)` |
| Préavis minimum | `Booking requires at least X minutes advance notice` |

**Impact frontend** : Un devis qui échoue retourne désormais **400** au lieu d'un prix pour une réservation qui aurait été refusée ensuite. Afficher l'erreur à l'utilisateur avant qu'il ne tente de réserver.

### 5.3 Transaction readOnly corrigée

**Fichier** : `ResourceAvailabilityServiceImpl.java`

Les méthodes `list()`, `listByResource()`, `listGroupedByResource()` n'ont plus `@Transactional(readOnly=true)` car elles appellent `expirePastAvailabilitySlots()` qui écrit en base.

**Impact frontend** : Aucun — correctif interne de cohérence transactionnelle.

---

## 6. Migration de données

### V163 — Contrat : suspension, renouvellement, revue, templates

```sql
-- Nouveaux champs sur contract_record
ALTER TABLE contract_record ADD COLUMN IF NOT EXISTS suspension_reason TEXT;
ALTER TABLE contract_record ADD COLUMN IF NOT EXISTS renewed_from_code VARCHAR(120);
ALTER TABLE contract_record ADD COLUMN IF NOT EXISTS reviewed_by BIGINT;
ALTER TABLE contract_record ADD COLUMN IF NOT EXISTS review_comment TEXT;

-- Migration des raisons de suspension existantes
UPDATE contract_record SET suspension_reason = termination_reason, termination_reason = NULL
WHERE status = 'SUSPENDED' AND termination_reason IS NOT NULL;

-- Table des templates de contrat
CREATE TABLE IF NOT EXISTS contract_template (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(250) NOT NULL,
    description TEXT,
    language VARCHAR(10) DEFAULT 'fr',
    version INTEGER NOT NULL DEFAULT 1,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Seed des 4 templates existants
INSERT INTO contract_template (code, name, description) VALUES
    ('membership-agreement', 'Membership Agreement', 'Standard membership contract template'),
    ('business-service-agreement', 'Business Service Agreement', 'Business service contract template'),
    ('contrat-domiciliation', 'Contrat de domiciliation', 'Domiciliation contract template'),
    ('subscription-pass-non-refundable', 'Subscription / Pass Non Refundable Agreement',
     'Non refundable subscription, pass and addon contract template')
ON CONFLICT (code) DO NOTHING;
```

---

## 7. Guide d'intégration Frontend

### 7.1 Changements breaking

| Changement | Endpoint affecté | Action requise |
|-----------|-----------------|----------------|
| `GET /members/search/by-name` supprimé | Recherche membre | Migrer vers `POST /members/search` |
| Transitions Customer restreintes | `PATCH /customers/{id}/status` | Adapter les actions selon le statut courant |
| Devis peut retourner 400 | `GET /resource-pricing-rules/quote` | Gérer l'erreur 400 (violation policy) |
| WhatsApp validé | Création/update Customer | Valider le format avant soumission |

### 7.2 Nouveaux endpoints

| Endpoint | Description | Écran concerné |
|----------|-------------|----------------|
| `POST /contracts/{code}/approve` | Approuver la revue d'un contrat | Détail contrat (back-office) |
| `POST /contracts/{code}/reject-review` | Rejeter la revue | Détail contrat (back-office) |

### 7.3 Nouveaux champs dans les réponses

| Réponse | Nouveaux champs | Écran concerné |
|---------|----------------|----------------|
| `ContractResponse` | `suspensionReason`, `renewedFromCode`, `reviewedBy`, `reviewComment` | Détail contrat |
| `MemberProfileResponse` | `profilePictureUrl`, `gender`, `emergencyContactName`, `emergencyContactPhone` | Profil membre |
| `CustomerResponse` | `isMember` (désormais fiable) | Fiche client |

### 7.4 Gestion des erreurs à implémenter

| Erreur | Code | Quand | Action frontend |
|--------|------|-------|----------------|
| Email/phone doublon Customer | 409 | Création/mise à jour | Afficher le message d'erreur sous le champ concerné |
| Transition statut interdite | 400 | Changement statut Customer | Désactiver les actions non autorisées selon le statut |
| Signataires en attente | 400 | Signature contrat | Afficher "En attente de signature des parties précédentes" |
| Violation policy réservation | 400 | Demande de devis | Afficher la contrainte violée (durée min/max, préavis) |
| Phone/WhatsApp invalide | 400 | Formulaires avec téléphone | Valider format `0[456]XXXXXXX` avant soumission |

### 7.5 Workflow contrat — Diagramme mis à jour

```
DRAFT
  │
  ├─→ GENERATED (draft PDF généré + email envoyé au propriétaire)
  │     │
  │     ├─→ UNDER_REVIEW
  │     │     │
  │     │     ├─→ AWAITING_SIGNATURE  ← POST /approve (reviewedBy, comment)
  │     │     │     │
  │     │     │     └─→ SIGNED → ACTIVE (auto ou manuel)
  │     │     │
  │     │     └─→ GENERATED  ← POST /reject-review (reviewedBy, comment)
  │     │
  │     └─→ AWAITING_SIGNATURE (si signataire dans les parties)
  │
  ├─→ CANCELLED
  │
  ACTIVE
  │
  ├─→ SUSPENDED (suspensionReason)
  │     └─→ ACTIVE (suspensionReason vidé)
  │
  ├─→ TERMINATED (terminationReason)
  ├─→ EXPIRED (auto par worker)
  │     └─→ AUTO_RENEW: nouveau contrat DRAFT créé (renewedFromCode)
  │     └─→ FIXED_TERM: alertes J-30/J-7
  │     └─→ NONE: expiration simple
```

### 7.6 Machine à états Customer — Transitions autorisées

```
        ┌──────────┐
        │ PENDING  │
        └────┬─────┘
             │
    ┌────────┼──────────┐
    ▼        ▼          ▼
 ACTIVE   INACTIVE   ARCHIVED
    │        │          (terminal)
    │        │
    ▼        ▼
SUSPENDED  ARCHIVED
    │
    ▼
  ACTIVE
```

Le frontend devrait désactiver les boutons d'action non autorisés en fonction du statut courant.

---

## 8. Items reportés

Les items suivants n'ont pas été traités dans ce cycle :

| Item | Raison | Module |
|------|--------|--------|
| Intégration e-signature (DocuSign/Yousign) | Nécessite un choix de fournisseur et un contrat commercial | Contrat |
| Granularité créneaux configurable (`slotDurationMinutes`) | Changement architectural à planifier | Ressource |
| Address en LAZY fetch (Customer) | Risque de régression sur les vues existantes — à tester exhaustivement | Customer |
