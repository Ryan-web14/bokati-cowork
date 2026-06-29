# Items d'audit restants — Résumé pour contexte

> Dernière mise à jour : 2026-06-19
> Référence : `docs/to-implement/audit-contrat-resource-member-customer.md` + `docs/to-implement/audit-kyc-document-management.md`
> Contexte : 38 items déjà implémentés sur 42 identifiés. 4 restent (#1, #4, #9, #10).

---

## Items restants

| # | Module | Item | Effort | Description | Fichiers concernés |
|---|--------|------|--------|-------------|-------------------|
| 1 | Ressource | **Granularité créneaux configurable** | Faible | `SLOT_MINUTES = 30` est hardcodé dans `ResourceAvailabilityServiceImpl.java:43`. Le champ `slotDurationMinutes` existe sur `ResourcePolicy` mais n'est jamais lu. Exploiter le champ existant au lieu de la constante. | `ResourceAvailabilityServiceImpl.java` |
| 2 | Contrat | **Intégration e-signature** | Élevé | `markSigned()` est purement manuel. Intégrer DocuSign/Yousign avec callback webhook pour auto-activation. Nécessite choix de fournisseur + contrat commercial. | `ContractServiceImpl.java`, nouveau service d'intégration |
| 3 | Customer | **Address en LAZY fetch** | Faible | `Customer.java:61` a `@ManyToOne(fetch = FetchType.EAGER)` sur Address. Passer en LAZY et ajouter `@EntityGraph` sur les requêtes qui en ont besoin. Risque de régression sur les vues liste. | `Customer.java`, `CustomerRepository.java` |
| 4 | GED | **Antivirus ClamAV** | Moyen | Le scan actuel détecte seulement les headers MZ/ELF. Intégrer ClamAV via socket/REST. Les statuts `PENDING_SCAN`/`QUARANTINED` existent déjà dans `DocumentVersionUploadStatus` mais ne sont jamais atteints. Pipeline : upload → quarantine → scan → CLEAN/QUARANTINED. | `DocumentSecurityService.java`, `DocumentServiceImpl.java` |
| 5 | KYC | **Notifications signataires** | Faible | `DocumentSignatureService` ne notifie jamais les signataires (ni création de demande, ni signature, ni refus). Publier un événement outbox à chaque transition de `DocumentSignature`. | `DocumentSignatureServiceImpl.java` |
| 6 | KYC | **Emails différenciés** | Faible | `KycOutboxEventProcessor` envoie un template générique unique (`kyc-event.html`). Ajouter des sujets et contenus différenciés par type d'événement (approbation/rejet/soumission/expiration). L'ancien `KycEmailNotifier` (supprimé) avait déjà les sujets différenciés — réutiliser la logique. | `KycOutboxEventProcessor.java` |
| 7 | KYC | **Événement KYC_CASE_REOPENED** | Faible | Quand `recomputeCaseState()` fait repasser un dossier `APPROVED` à `IN_PROGRESS`, l'événement générique `KYC_CASE_AUTO_STATUS_SYNC` ne permet pas de distinguer une réouverture. Publier `KYC_CASE_REOPENED` dans ce cas précis. | `KycAutomationServiceImpl.java:recomputeCaseState()` |
| 8 | KYC | **KYC différencié par plan** | Moyen | Le champ `kycLevel` existe sur `KycCase` (1-4). Ajouter un champ `requiredKycLevel` sur `SubscriptionPlan` et vérifier à l'activation d'un abonnement que le KYC du client atteint le niveau requis. | `SubscriptionPlan.java`, activation service, migration |
| 9 | KYC | **Screening PEP/AML** | Élevé | Intégration d'un service tiers (ComplyAdvantage, Dow Jones, etc.) pour vérifier les noms contre les listes de sanctions. Nécessite choix de fournisseur. | Nouveau service, intégration dans `KycServiceImpl.approve()` |
| 10 | KYC | **Webhooks KYC** | Moyen | Publier les événements KYC (approbation/rejet/expiration) vers un CRM/ERP externe via webhook. Le pattern Outbox est déjà en place — ajouter un processeur webhook. | Nouveau `KycWebhookEventProcessor`, `WebhookEndpoint` entity existe déjà |
| 11 | Transverse | **Constante SYSTEM_REVIEWER_ID dupliquée** | Faible | `SYSTEM_REVIEWER_ID = 0L` est dupliqué dans `KycAutomationWorker.java:41` et `DocumentServiceImpl.java:68`. Créer `SystemActors.SYSTEM_USER_ID` dans un utilitaire partagé. | `core/utils/`, les 2 fichiers appelants |
| 12 | Transverse | **Locale codée en dur** | Faible | `Locale.FRANCE` est hardcodé dans `KycServiceImpl.java` (export PDF), `ContractGenerationServiceImpl.java` (rendu HTML), et l'ancien `KycEmailNotifier`. Paramétrer via config ou résoudre depuis le profil utilisateur. | `KycServiceImpl.java`, `ContractGenerationServiceImpl.java` |

---

## Résumé par effort

| Effort | Items | Numéros |
|--------|-------|---------|
| **Faible** (< 30 min) | 6 | #1, #3, #5, #6, #7, #11, #12 |
| **Moyen** (1-3h) | 3 | #4, #8, #10 |
| **Élevé** (décision externe requise) | 2 | #2, #9 |

## Items déjà faits (résumé)

Les 30 items suivants ont été implémentés dans la session du 18/06/2026 :

**Module KYC/GED** : OCR 6 champs + confidence score réel, requiresSignature auto-création, businessLegalForm filtrage, KycEmailNotifier supprimé, myQueue paginé DB, CRUD KycCrossValidationRule, handler 415, endpoint onboarding-status (mapping 9→5 statuts), fallback JSON upload 400.

**Module Customer** : bug validation téléphone corrigé (root cause dans `ValidationUtils`), WhatsApp validation réactivée, unicité email/phone pré-vérifiée (create + update), machine à états `CustomerStatus`, null-safety `toSummary()`, `isMember` synchronisé auto, DTOs morts supprimés.

**Module Member** : validation téléphone corrigée (3 appelants), mot de passe sécurisé (lien reset au lieu de clair), `GET /search/by-name` supprimé, `getAllMembers` capé à 1000, champs profil mappés (photo, gender, emergency contact), `MemberPdfGenerationImpl` supprimé.

**Module Contrat** : `ContractEmailNotifier` branché, moteur renouvellement (AUTO_RENEW + alertes), workflow approbation (approve + reject-review), signOrder enforced, suspension_reason séparé, templates dynamiques en base (`ContractTemplate` + seed), migration V163.

**Module Ressource** : `@NotNull @Min(0)` sur prix, devis croisé `ResourcePolicy`, transaction readOnly corrigée.

**Transverse** : `DataPurgeService` (vide toutes les données sauf admins + tables système/seedées).
