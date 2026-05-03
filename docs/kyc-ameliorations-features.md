# KYC — Améliorations et nouvelles fonctionnalités

## État actuel — Ce qui fonctionne déjà

Avant de parler des améliorations, voici ce que le module fait déjà correctement :

- Cycle de vie complet d'un dossier KYC (IN_PROGRESS → SUBMITTED → APPROVED / REJECTED / PENDING_CORRECTION)
- Support de 3 types de propriétaires : MEMBER, CUSTOMER, BUSINESS
- Synchronisation bidirectionnelle automatique avec les statuts Member/Customer
- Requirements configurables par type de propriétaire et type de client
- Vérification manuelle des documents (approve/reject) avec audit trail
- Notifications email via le pattern Outbox sur chaque changement d'état
- Idempotence sur toutes les actions sensibles
- Vue des requirements manquants par dossier (`GET /kyc/cases/{code}/missing-requirements`)
- Async sur les syncs post-upload de document

---

## Légende

| Symbole | Signification |
|---------|---------------|
| 🔴 Critique | Trou fonctionnel qui bloque la conformité ou l'expérience |
| 🟠 Important | Valeur métier forte, à planifier bientôt |
| 🟡 Moyen | Améliore le confort ou la complétude |
| 🟢 Long terme | Puissant mais complexe à implémenter |

---

## 1. 🔴 Expiration et renouvellement KYC

### Problème actuel
Les documents ont une `expiryDate`, le statut `KycDocumentVerificationStatus.EXPIRED` existe, mais **aucun worker ne surveille les expirations**. Un membre dont la carte d'identité a expiré reste en statut APPROVED indéfiniment.

### Ce qu'il faut construire

**Worker de surveillance des documents KYC expirés :**
```java
// Nouveau worker @Scheduled
@Scheduled(cron = "0 0 8 * * MON")  // Tous les lundis à 8h
public void checkExpiringKycDocuments() {
    // Trouve les KycDocuments dont expiryDate dans 60, 30, 7 jours
    // Envoie une notification de rappel au propriétaire
    // Trouve les KycDocuments dont expiryDate < aujourd'hui
    // → Document.status = EXPIRED
    // → KycDocument.status = EXPIRED
    // → Recalcule le KycCase → PENDING_CORRECTION
    // → Member/Customer.status → PENDING_CORRECTION
    // → Email d'alerte : "Votre pièce d'identité a expiré, veuillez la renouveler"
}
```

**Niveaux de rappel configurables :**
```yaml
kyc:
  expiry:
    reminder-days: [60, 30, 7]   # Jours avant expiration pour envoyer un rappel
    grace-period-days: 15        # Jours de grâce après expiration avant de bloquer le compte
```

**Nouveau statut de dossier :** `RENEWAL_REQUIRED` → le dossier était APPROVED mais un document a expiré.

**Endpoints :**
```
GET /v1/kyc/cases/expiring-soon           → dossiers avec documents qui expirent dans 30j
GET /v1/kyc/cases/{code}/expiry-status    → détail des dates d'expiration par document
```

---

## 2. 🔴 Tableau de bord compliance (vue opérateur)

### Problème actuel
Il n'existe aucune vue d'ensemble des dossiers KYC. Un responsable de conformité ne sait pas combien de dossiers sont en attente de revue, depuis combien de temps, ni lesquels sont urgents.

### Ce qu'il faut construire

**Endpoint de métriques KYC :**
```
GET /v1/kyc/dashboard
```

**Réponse :**
```json
{
  "generatedAt": "2026-04-29T08:00:00Z",
  "summary": {
    "totalCases": 142,
    "inProgress": 23,
    "submitted": 18,
    "underReview": 12,
    "pendingCorrection": 7,
    "approved": 75,
    "rejected": 7
  },
  "sla": {
    "casesReviewedWithin24h": 15,
    "casesExceeding48h": 3,
    "avgReviewTimeHours": 18.5
  },
  "expiringSoon": {
    "within7Days": 4,
    "within30Days": 11,
    "within60Days": 22
  },
  "byOwnerType": [
    { "ownerType": "MEMBER", "count": 89, "pendingReview": 8 },
    { "ownerType": "CUSTOMER", "count": 41, "pendingReview": 6 },
    { "ownerType": "BUSINESS", "count": 12, "pendingReview": 4 }
  ],
  "recentActivity": [
    { "action": "APPROVED", "count": 12, "period": "LAST_7_DAYS" },
    { "action": "REJECTED", "count": 3, "period": "LAST_7_DAYS" }
  ]
}
```

**Endpoints de listing enrichis :**
```
GET /v1/kyc/cases?status=SUBMITTED&ownerType=MEMBER&reviewedBy=null&sort=submittedAt,asc
→ Liste paginée filtrée, triée par date de soumission croissante (les plus anciens d'abord)
```

**Filtres à ajouter sur le listing existant :**
- `status` : filtrer par statut
- `ownerType` : MEMBER, CUSTOMER, BUSINESS
- `submittedBefore` / `submittedAfter` : plage de dates
- `reviewedBy` : par agent
- `pendingReviewOnly` : booléen
- `expiringWithinDays` : documents expirant dans N jours

---

## 3. 🔴 Workflow de revue avec assignation

### Problème actuel
La revue est anonyme. N'importe quel admin peut approuver n'importe quel dossier sans contrôle. Il n'y a pas de suivi de qui est en train de revoir quoi.

### Ce qu'il faut construire

**Assignation d'un dossier à un reviewer :**
```
PATCH /v1/kyc/cases/{code}/assign
Body: { "assignedTo": 42 }  // userId de l'agent compliance
```

**Nouveau champ sur `KycCase` :**
```java
@Column(name = "assigned_to")
private Long assignedTo;          // userId assigné

@Column(name = "assigned_at")
private Instant assignedAt;

@Column(name = "sla_deadline")
private Instant slaDeadline;      // calculé à partir du submittedAt + config SLA
```

**Endpoint "mes dossiers à revoir" :**
```
GET /v1/kyc/cases/my-queue        // Les dossiers assignés à l'utilisateur connecté
```

**SLA configurable :**
```yaml
kyc:
  sla:
    review-hours-by-type:
      MEMBER: 24
      CUSTOMER_PERSON: 24
      CUSTOMER_COMPANY: 48
      BUSINESS: 72
```

---

## 4. 🟠 Notes internes sur les dossiers

### Problème actuel
Les agents de conformité n'ont pas de moyen de laisser des notes internes sur un dossier (ex : "À valider avec le service commercial", "Client à risque moyen").

### Ce qu'il faut construire

**Nouveau modèle :**
```java
@Entity @Table(name = "kyc_case_note")
public class KycCaseNote {
    Long id;
    KycCase kycCase;            // Dossier KYC
    String content;             // Texte de la note
    Long authorId;              // UserId de l'agent
    Instant createdAt;
    Boolean internal;           // true = visible seulement par les agents
}
```

**Endpoints :**
```
POST /v1/kyc/cases/{code}/notes          → ajouter une note interne
GET  /v1/kyc/cases/{code}/notes          → lister les notes du dossier
```

**Règle :** les notes internes (`internal = true`) ne sont jamais exposées sur le portail client.

---

## 5. 🟠 Historique / Timeline complète du dossier

### Problème actuel
`KycVerification` enregistre les vérifications de documents mais il n'y a pas de timeline unifiée de toutes les actions sur un dossier.

### Ce qu'il faut construire

**Endpoint de timeline :**
```
GET /v1/kyc/cases/{code}/timeline
```

**Réponse :**
```json
[
  {
    "timestamp": "2026-04-01T09:00:00Z",
    "action": "CASE_CREATED",
    "actor": "SYSTEM",
    "description": "Dossier KYC créé automatiquement lors de l'inscription du membre"
  },
  {
    "timestamp": "2026-04-02T14:30:00Z",
    "action": "DOCUMENT_UPLOADED",
    "actor": "MBR-2026-000001",
    "description": "Document MEMBER_ID_CARD (DOC-202604-000012) soumis"
  },
  {
    "timestamp": "2026-04-03T10:15:00Z",
    "action": "DOCUMENT_APPROVED",
    "actor": "USR-004 (Agent compliance)",
    "description": "Carte d'identité vérifiée et approuvée"
  },
  {
    "timestamp": "2026-04-03T10:16:00Z",
    "action": "CASE_APPROVED",
    "actor": "SYSTEM (Auto-approuvé)",
    "description": "Tous les documents requis vérifiés. Dossier approuvé automatiquement."
  }
]
```

**Sources des événements :**
- Events Outbox existants
- `KycVerification` existant
- Nouveaux events à publier lors des transitions

---

## 6. 🟠 Rappels automatiques aux membres incomplets

### Problème actuel
Un membre peut s'inscrire, démarrer son dossier KYC et ne jamais le compléter. Il reste en `IN_PROGRESS` sans aucune relance automatique.

### Ce qu'il faut construire

**Worker de relance :**
```java
@Scheduled(cron = "0 0 9 * * *")  // Tous les jours à 9h
public void remindIncompleteKycCases() {
    // Trouve tous les KycCase en IN_PROGRESS ou PENDING_CORRECTION
    // dont le startedAt ou dernière modification est > 3 jours
    // Envoie un email de rappel personnalisé avec la liste des documents manquants
    // Garde trace de la dernière relance pour ne pas spammer
}
```

**Configuration :**
```yaml
kyc:
  reminders:
    enabled: true
    reminder-after-days: [3, 7, 14]   # Jours sans action avant relance
    max-reminders: 3                   # Arrêter après 3 relances
```

**Nouveau champ sur `KycCase` :**
```java
@Column(name = "last_reminder_sent_at")
private Instant lastReminderSentAt;

@Column(name = "reminder_count")
private Integer reminderCount;
```

---

## 7. 🟠 Approbation automatique configurable

### Problème actuel
Toutes les vérifications sont manuelles. Pour des clients de faible risque ou des documents simples, l'auto-approbation pourrait accélérer l'onboarding.

### Ce qu'il faut construire

**Configuration sur `DocumentType` :**
```java
@Column(name = "auto_approve")
private Boolean autoApprove;     // Si true, le document est approuvé dès l'upload

@Column(name = "auto_approve_after_days")
private Integer autoApproveAfterDays;  // Si > 0, approuvé automatiquement après N jours sans rejet
```

**Logique dans `syncFromDocumentUpload()` :**
```java
if (documentType.getAutoApprove()) {
    // Approuver le document immédiatement
    document.setStatus(DocumentStatus.APPROVED);
    kycDocument.setStatus(KycDocumentVerificationStatus.VERIFIED);
    recomputeCaseState(kycCase);
}
```

**Worker d'auto-approbation différée :**
```java
// Approve documents "auto_approve_after_days" jours après upload si non rejetés
@Scheduled(cron = "0 0 8 * * *")
public void autoApproveEligibleDocuments() { }
```

---

## 8. 🟠 Niveau de risque KYC (Risk Score)

### Pourquoi
Un coworking est soumis aux obligations de lutte contre le blanchiment d'argent (AML). Certains clients présentent un risque plus élevé selon leur profil.

### Ce qu'il faut construire

**Nouveau champ sur `KycCase` :**
```java
@Enumerated(EnumType.STRING)
@Column(name = "risk_level")
private KycRiskLevel riskLevel;   // LOW, MEDIUM, HIGH, VERY_HIGH
```

**Enum `KycRiskLevel` :**
```java
public enum KycRiskLevel {
    LOW,        // Particulier, documents simples
    MEDIUM,     // Entreprise, documents multiples
    HIGH,       // Client politiquement exposé (PEP), opérations importantes
    VERY_HIGH   // Pays à risque, transactions suspectes
}
```

**Calcul automatique du risque :**
```java
// Facteurs augmentant le risque :
// - ownerType = BUSINESS → MEDIUM
// - montant des transactions > seuil → HIGH
// - pays d'origine à risque → HIGH
// - correspondance liste PEP → HIGH ou VERY_HIGH
```

**Règles d'approbation par niveau :**
- LOW → auto-approbation possible
- MEDIUM → 1 approuveur requis
- HIGH → 2 approuveurs requis (double validation)
- VERY_HIGH → alerte compliance, blocage jusqu'à revue spéciale

**Endpoint :**
```
PATCH /v1/kyc/cases/{code}/risk-level      → modifier manuellement le niveau
GET   /v1/kyc/cases?riskLevel=HIGH         → filtrer par risque
```

---

## 9. 🟠 Export du dossier KYC en PDF

### Pourquoi
Pour les audits de conformité, les régulateurs demandent souvent un rapport complet du dossier KYC avec tous les documents joints.

### Ce qu'il faut construire

**Endpoint :**
```
GET /v1/kyc/cases/{code}/export/pdf
```

**Contenu du PDF :**
- Page de couverture : identité du propriétaire, date d'approbation, agent ayant approuvé
- Résumé du dossier : statut, dates clés, score de risque
- Liste des documents vérifiés avec dates de validité
- Timeline complète des actions
- Notes internes (version compliance) ou non (version client)
- Signature électronique de l'exporteur

**Template Thymeleaf :**
```
templates/kyc/
  kyc-case-report.html    → rapport complet
  kyc-summary-card.html   → carte récap pour intégration dans un autre document
```

---

## 10. 🟠 Vérification de documents croisée

### Problème actuel
Les documents sont vérifiés indépendamment. Rien ne valide que le nom sur le passeport correspond au nom sur la carte d'identité.

### Ce qu'il faut construire

**Règles de cohérence configurables :**
```java
@Entity @Table(name = "kyc_cross_validation_rule")
public class KycCrossValidationRule {
    Long id;
    String documentTypeCode1;    // Ex: MEMBER_ID_CARD
    String documentTypeCode2;    // Ex: MEMBER_PASSPORT
    String fieldToCompare;       // "name", "date_of_birth", "nationality"
    Boolean blocking;            // Si true, bloque l'approbation si incohérence
}
```

**Logique lors de l'approbation :**
```java
// Avant d'approuver un dossier, vérifie toutes les règles de cross-validation
// Si une règle est violée → warning sur le dossier ou blocage selon blocking=true
```

**Note :** nécessite que les données extraites des documents (nom, date de naissance) soient stockées quelque part. Voir amélioration OCR ci-dessous.

---

## 11. 🟡 OCR — Extraction automatique des données

### Pourquoi
Actuellement, l'agent doit entrer manuellement le numéro de document, la date d'émission, la date d'expiration. L'OCR peut automatiser ça.

### Ce qu'il faut construire

**Intégration OCR :**
- Option A : Google Cloud Vision (API externe)
- Option B : AWS Textract
- Option C : Tesseract OCR (open source, auto-hébergé)
- Option D : Smile ID (spécialisé Afrique, reconnaît CNI Congo, Cameroun, Côte d'Ivoire)

**Nouveau modèle :**
```java
@Entity @Table(name = "kyc_document_ocr_result")
public class KycDocumentOcrResult {
    Long id;
    KycDocument kycDocument;
    String extractedFirstName;
    String extractedLastName;
    LocalDate extractedDateOfBirth;
    LocalDate extractedExpiryDate;
    String extractedDocumentNumber;
    String extractedNationality;
    BigDecimal confidenceScore;       // 0.0 à 1.0
    String rawOcrJson;                // JSON brut retourné par l'API OCR
    Instant processedAt;
}
```

**Flow :**
```
Upload document KYC
  → @Async → appel API OCR
  → Remplir KycDocumentOcrResult
  → Comparer avec les données du profil (nom, date de naissance)
  → Si correspondance > 0.85 → flag "données cohérentes"
  → Si incohérence → alerte sur l'interface de l'agent de revue
```

**Endpoint de revue avec données OCR :**
```
GET /v1/kyc/documents/{documentCode}/ocr-result
→ Retourne les données extraites + score de confiance
```

---

## 12. 🟡 Vérification liveness (selfie)

### Pourquoi
Pour les clients à risque moyen ou élevé, vérifier que la personne qui soumet les documents est bien la personne sur le document.

### Ce qu'il faut construire

**Type de document spécial :**
- `MEMBER_SELFIE_WITH_ID` : photo du membre tenant sa pièce d'identité
- `MEMBER_LIVENESS_VIDEO` : courte vidéo (cligner des yeux, tourner la tête)

**Intégration externe :**
- Smile ID Liveness Check (Afrique)
- Onfido
- Simple comparaison faciale via un service tiers

**Logique :**
- Pour les KycCase avec riskLevel = HIGH → selfie obligatoire
- Score de correspondance faciale stocké en DB
- Seuil configurable (ex : > 0.80 pour passer)

---

## 13. 🟡 Screening PEP / Sanctions (AML)

### Pourquoi
Les personnes politiquement exposées (PEP) ou les personnes sous sanctions (OFAC, ONU, UE) doivent être identifiées pour la conformité AML.

### Ce qu'il faut construire

**Nouveau modèle :**
```java
@Entity @Table(name = "kyc_aml_screening_result")
public class KycAmlScreeningResult {
    Long id;
    KycCase kycCase;
    String screeningProvider;      // "INTERNAL", "REFINITIV", "COMPLYADVANTAGE"
    Boolean pepMatch;
    Boolean sanctionMatch;
    BigDecimal matchScore;
    String matchDetails;           // JSON des correspondances trouvées
    Instant screenedAt;
    String screenedBy;             // SYSTEM ou userId
}
```

**Flow :**
```
KycCase soumis (SUBMITTED)
  → Screening automatique contre listes locales
  → Si match → riskLevel = HIGH, alerte compliance
  → Si pas de match → processing normal
```

**Listes minimales à intégrer en local :**
- Liste des personnes sous sanctions OFAC (téléchargeable gratuitement)
- Liste des PEP Congo (si disponible)

---

## 14. 🟡 KYC différencié par type de service

### Problème actuel
Tous les membres passent par le même KYC. Un membre qui veut juste réserver une salle ponctuellement n'a pas besoin du même niveau de vérification qu'un membre avec abonnement annuel.

### Ce qu'il faut construire

**Niveaux de KYC :**
```
KYC Niveau 1 (Basic)     → Nom + email + téléphone vérifié (pour réservations ponctuelles)
KYC Niveau 2 (Standard)  → Pièce d'identité (pour abonnements mensuels)
KYC Niveau 3 (Enhanced)  → ID + justificatif de domicile + RCCM (pour entreprises)
KYC Niveau 4 (Full)      → Tout + selfie + screening AML (pour contrats annuels > seuil)
```

**Configuration sur le `Plan` d'abonnement :**
```java
@Column(name = "required_kyc_level")
private Integer requiredKycLevel;   // 1, 2, 3, ou 4
```

**Logique lors de l'activation d'abonnement :**
```java
// Vérifier que le subscriber a le niveau KYC requis par le plan
if (subscription.getPlan().getRequiredKycLevel() > member.getKycLevel()) {
    throw new BadRequestException("KYC niveau " + requiredLevel + " requis pour ce plan");
}
```

---

## 15. 🟡 Portail client KYC (interface pour le membre)

### Problème actuel
Le membre n'a aucune visibilité sur son dossier KYC. Il ne sait pas quoi soumettre ni pourquoi il est bloqué.

### Ce qu'il faut construire (côté ClientPlatform)

**Endpoints client :**
```
GET /v1/client/kyc                              → mon dossier KYC (statut, complétude)
GET /v1/client/kyc/requirements                 → ce que je dois soumettre
GET /v1/client/kyc/missing                      → documents manquants
POST /v1/client/documents/upload                → soumettre un document KYC
GET /v1/client/kyc/documents                    → mes documents soumis + leur statut
```

**Réponse enrichie pour le client :**
```json
{
  "status": "PENDING_CORRECTION",
  "completionPercentage": 67,
  "canAccessPlatform": false,
  "blockedReason": "Votre dossier KYC nécessite des corrections",
  "requirements": [
    {
      "documentTypeCode": "MEMBER_ID_CARD",
      "documentTypeName": "Carte nationale d'identité",
      "required": true,
      "status": "REJECTED",
      "rejectionReason": "Document illisible, veuillez resoumettre une photo plus nette",
      "documentCode": "DOC-202604-000012"
    },
    {
      "documentTypeCode": "MEMBER_PROOF_OF_ADDRESS",
      "documentTypeName": "Justificatif de domicile",
      "required": true,
      "status": null,
      "helpText": "Facture d'eau, d'électricité ou relevé bancaire de moins de 3 mois"
    }
  ],
  "nextStep": "Veuillez resoumettre votre carte d'identité et ajouter un justificatif de domicile"
}
```

**Points clés de l'interface client :**
- Afficher clairement le % de complétion du dossier
- Expliquer le motif de rejet en language clair (pas de jargon)
- Afficher la date de validité de chaque document
- Envoyer un email avec les instructions de correction détaillées

---

## 16. 🟡 Types de documents spécifiques Congo/Afrique centrale

### Problème actuel
Les types de documents sont génériques. Certains documents spécifiques au Congo ne sont pas référencés.

### Documents à ajouter

```sql
-- Documents membres Congo
INSERT INTO document_type (code, name, category) VALUES
  ('MEMBER_BIRTH_CERTIFICATE',     'Acte de naissance',                      'KYC'),
  ('MEMBER_CONSULAR_CARD',         'Carte consulaire',                        'KYC'),
  ('MEMBER_REFUGEE_CARD',          'Carte de réfugié UNHCR',                 'KYC'),
  ('MEMBER_WORK_PERMIT',           'Permis de travail',                       'KYC'),
  ('MEMBER_TAX_DECLARATION',       'Attestation fiscale (NIU)',                'KYC');

-- Documents entreprise Congo
INSERT INTO document_type (code, name, category) VALUES
  ('BUSINESS_DECRET_CREATION',     'Décret de création (associations, ONG)',  'KYC'),
  ('BUSINESS_AGRÉMENT',            'Agrément ministériel',                    'KYC'),
  ('BUSINESS_ATTESTATION_FISCALE', 'Attestation de mise à jour fiscale',      'KYC'),
  ('BUSINESS_CNSS_ATTESTATION',    'Attestation CNSS',                        'KYC'),
  ('BUSINESS_PLAN',                'Plan de localisation du siège',           'KYC');
```

---

## 17. 🟡 API webhook KYC pour systèmes tiers

### Pourquoi
Si Bokati Cowork s'intègre avec un CRM, un ERP ou un partenaire externe, ces systèmes ont besoin d'être notifiés quand un KYC est approuvé.

### Ce qu'il faut construire

**Configuration webhook :**
```
POST /v1/admin/webhooks
Body: {
  "url": "https://erp.example.com/webhooks/kyc",
  "events": ["KYC_APPROVED", "KYC_REJECTED", "KYC_EXPIRING"],
  "secret": "hmac-secret"
}
```

**Payload webhook KYC :**
```json
{
  "event": "KYC_APPROVED",
  "timestamp": "2026-04-29T10:00:00Z",
  "data": {
    "kycCaseCode": "KCS-202604-000123",
    "ownerType": "MEMBER",
    "ownerCode": "MBR-2026-000001",
    "ownerName": "Jean Dupont",
    "riskLevel": "LOW",
    "approvedAt": "2026-04-29T09:55:00Z",
    "approvedBy": "USR-004",
    "documentsVerified": ["MEMBER_ID_CARD", "MEMBER_PROOF_OF_ADDRESS"]
  }
}
```

---

## 18. 🟡 Revue en lot (Bulk actions)

### Pourquoi
Quand des dizaines de membres s'inscrivent en même temps (ex : après un événement), le reviewer doit traiter chaque dossier un par un.

### Ce qu'il faut construire

**Endpoints bulk :**
```
POST /v1/kyc/documents/bulk-approve
Body: { "documentCodes": ["DOC-001", "DOC-002", "DOC-003"], "reviewedBy": 42 }

POST /v1/kyc/documents/bulk-reject
Body: { 
  "items": [
    { "documentCode": "DOC-004", "reason": "Photo floue" },
    { "documentCode": "DOC-005", "reason": "Document expiré" }
  ],
  "reviewedBy": 42
}
```

**Logique :**
- Traitement transactionnel par document (un échec n'annule pas les autres)
- Résumé retourné : X approuvés, Y échoués, avec détail des erreurs

---

## 19. 🟢 Intégration vérification RCCM / NIU (Congo)

### Pourquoi
Le RCCM (Registre du Commerce) et le NIU (Numéro d'Identification Unique) des entreprises congolaises peuvent être vérifiés en ligne si une API est disponible.

### Ce qu'il faut construire

**Service externe :**
```java
@Service
public class CongoRegistryVerificationService {
    
    public RegistryVerificationResult verifyRccm(String rccmNumber) {
        // Appel API Tribunal de Commerce / GUICHET UNIQUE
        // Retourne : entreprise trouvée, nom légal, statut, date enregistrement
    }
    
    public RegistryVerificationResult verifyNiu(String niuNumber) {
        // Appel API DGI (Direction Générale des Impôts)
        // Retourne : contribuable trouvé, statut fiscal, mise à jour fiscale
    }
}
```

**Intégration KYC :**
- Lors de l'upload d'un document de type BUSINESS_RCCM → lancer la vérification en arrière-plan
- Si vérification réussie → flag sur le document "Vérifié contre le registre officiel"
- Si vérification échoue → alerte au reviewer

---

## 20. 🟢 Machine Learning — Détection de documents frauduleux

### Long terme
Utiliser un modèle de classification pour détecter les documents falsifiés.

**Signaux à analyser :**
- Qualité d'impression (résolution, uniformité)
- Cohérence des polices de caractères
- Présence des éléments de sécurité (hologrammes, microimpressions)
- Métadonnées EXIF de l'image (modèle d'appareil, date de prise)

**Approche :**
- Intégrer un service tiers (Veriff, Jumio, Smile ID Document Verification)
- OU entraîner un modèle léger sur des exemples de documents congolais

---

## Récapitulatif par ordre de priorité

### 🔴 À faire en premier (impact immédiat sur la conformité)

| # | Quoi | Effort |
|---|------|--------|
| 1 | Worker surveillance documents expirés + rappels | Moyen |
| 2 | Tableau de bord compliance (dashboard + filtres) | Moyen |
| 3 | Workflow de revue avec assignation + SLA | Moyen |

### 🟠 Sprint suivant (améliore l'efficacité opérationnelle)

| # | Quoi | Effort |
|---|------|--------|
| 4 | Notes internes sur les dossiers | Faible |
| 5 | Timeline complète du dossier | Faible |
| 6 | Rappels automatiques aux membres incomplets | Faible |
| 7 | Approbation automatique configurable | Faible |
| 8 | Niveau de risque KYC (LOW/MEDIUM/HIGH) | Moyen |
| 9 | Export dossier KYC en PDF | Moyen |
| 10 | Types documents spécifiques Congo | Faible (migration SQL) |

### 🟡 Planifier ensuite (valeur ajoutée significative)

| # | Quoi | Effort |
|---|------|--------|
| 11 | OCR extraction automatique | Élevé |
| 12 | Interface client KYC (portail) | Moyen |
| 13 | KYC différencié par niveau/plan | Moyen |
| 14 | Revue en lot (bulk actions) | Faible |
| 15 | Vérification croisée de documents | Moyen |
| 16 | Webhook KYC pour systèmes tiers | Faible |

### 🟢 Long terme (avancé)

| # | Quoi | Effort |
|---|------|--------|
| 17 | Screening PEP / Sanctions (AML) | Élevé |
| 18 | Liveness check / selfie | Élevé |
| 19 | Intégration RCCM / NIU Congo | Élevé (dépend API externe) |
| 20 | ML détection fraude documents | Très élevé |

---

## Notes d'implémentation

**Concernant les workers d'expiration et de rappel :**
Les workers doivent garder trace des actions déjà effectuées (champs `lastReminderSentAt`, `reminderCount`) pour ne pas envoyer plusieurs fois le même email. Utiliser le pattern outbox existant pour les emails de rappel.

**Concernant l'OCR :**
Smile ID est le service recommandé pour l'Afrique centrale car il reconnaît nativement les documents d'identité des pays de la CEMAC (Congo, Cameroun, Gabon, etc.). L'intégration se fait via une API REST simple.

**Concernant l'AML :**
Commencer par une vérification interne manuelle (liste de noms surveillés en DB) avant d'intégrer un service externe coûteux. Une simple table `aml_watchlist (name, type, reason, addedAt)` avec une recherche floue trigram suffit pour un premier niveau.

**Concernant les niveaux KYC :**
Ne pas casser la logique existante. Ajouter le niveau comme un champ calculé sur `KycCase` et une configuration sur les plans d'abonnement. L'activation d'abonnement vérifie déjà des conditions — c'est là qu'on ajoute la vérification du niveau KYC requis.