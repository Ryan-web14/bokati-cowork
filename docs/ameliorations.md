# Plan d'amélioration — Bokati Cowork

> Généré le 2026-05-05 · Basé sur l'audit complet du code source

---

## 1. Modules à construire (stubs / quasi-vides)

| Module | État actuel | Ce qu'on peut faire |
|---|---|---|
| `verify/` | Un seul controller, rien d'autre | Workflow de vérification d'identité post-KYC |
| `crm/` | 2 services, modèles vides | Pipeline leads, historique interactions client |
| `support/` | Tickets basiques | Catégories, SLA, escalade, réponse par email |
| `task/` | CRUD basique | Assignation, statuts, notifications à l'assigné |
| `visitor/` | Logs de visites | Badge visiteur, pré-enregistrement, alertes |
| `reporting/` | 2 controllers vides | Export PDF/CSV des rapports financiers et d'occupation |

---

## 2. Gaps précis dans les modules existants

| Module | Gap | Complexité |
|---|---|---|
| `payment/` | Dunning — relance automatique si paiement mobile échoue (J+1, J+3, J+7 → suspension) | Moyenne |
| `payment/` | USSD flow — stub dans le code, rien d'implémenté | Haute |
| `payment/` | Stripe / PayPal / Adyen — aucun provider carte câblé | Haute |
| `subscription/` | Pro-rata billing lors d'un changement de plan mid-cycle | Moyenne |
| `subscription/` | Alertes de quota — notifier à 80 % d'un entitlement consommé | Faible |
| `subscription/` | Dunning — retry sur paiement échoué avant suspension | Moyenne |
| `contract/` | Substitution de variables dans les templates Freemarker | Faible |
| `contract/` | Intégration e-signature (DocuSign, Hellosign) | Haute |
| `analytics/` | Drill-down, export CSV/PDF, filtres avancés | Moyenne |
| `notification/` | SMS — interface déclarée (`NotificationChannel.SMS`), aucun provider branché | Moyenne |
| `notification/` | Push — canal déclaré, aucun provider (Firebase / APNs) | Moyenne |
| `kyc/` | Providers OCR avancés (Google Vision, AWS Textract) | Haute |
| `kyc/` | Vérification biométrique / liveness check | Haute |

---

## 3. TODOs marqués dans le code

| Fichier | TODO |
|---|---|
| `core/baseClasses/mapper/decorator/AddressMapperDecorator.java` | Améliorer le decorator d'adresse |
| `core/configuration/ApplicationConfig.java` | Définir la valeur `app.error.verbose` |
| `core/exception/customs/BadCredentialException.java` | Créer un `httpStatus` custom pour cette exception |
| `core/settings/userSettings/mapper/interfaces/NotificationPreferenceMapper.java` | Finir le mapper |
| `features/company/model/BusinessEntity.java` | Ajouter la colonne `deleted` dans le script SQL |
| `security/admin/role/service/implementation/PermissionServiceImpl.java` | Implémenter la vérification d'action et de module |
| `security/admin/user/mapper/interfaces/UserMapper.java` | Ajouter la méthode `updateUser` |
| `security/admin/user/service/interfaces/UserService.java` | Implémenter la méthode marquée `// implement later` |

---

## 4. Features concrètes à ajouter (par priorité business)

| Priorité | Feature | Module | Impact |
|---|---|---|---|
| 🔴 Haute | Dunning management — relance automatique sur paiement échoué | `payment/` + `subscription/` | Évite la perte d'abonnements |
| 🔴 Haute | SMS via Twilio ou Africa's Talking | `notification/` | Atteindre les clients sans email |
| 🟡 Moyenne | Alerte de quota d'entitlement (80 %, 100 %) | `subscription/` | Améliore l'expérience membre |
| 🟡 Moyenne | Export rapport PDF (financier + occupation) | `reporting/` | Nécessaire pour la comptabilité |
| 🟡 Moyenne | Substitution de variables dans les contrats | `contract/` | Contrats personnalisés automatiques |
| 🟡 Moyenne | Pré-enregistrement visiteur avec QR code | `visitor/` | Accueil professionnel |
| 🟢 Faible | UserMapper.updateUser | `security/` | Compléter le CRUD utilisateur |
| 🟢 Faible | Permission verification dans PermissionServiceImpl | `security/` | Sécurité renforcée |
| 🟢 Faible | Push notifications Firebase | `notification/` | Canal mobile |

---

## 5. Workflows email à améliorer

| Template | Problème | Amélioration |
|---|---|---|
| `billing-reminder.html` | Générique, pas de montant ni lien | Ajouter montant exact, date d'échéance, lien de paiement direct |
| `booking-reminder.html` | Rappel simple | Ajouter détails ressource, adresse, QR code d'accès |
| `subscription-expiry-reminder.html` | Date d'expiration seule | Ajouter comparatif des plans pour encourager le renouvellement |
| `kyc-expiry-reminder.html` | Notification sèche | Ajouter les documents spécifiques qui expirent + lien direct de re-upload |
| `contract-event.html` | Générique pour tous les statuts | Personnaliser par statut (AWAITING_SIGNATURE vs SIGNED vs EXPIRED) |
| `document-event.html` | Générique | Différencier APPROVED vs REJECTED avec le motif de rejet inclus |
| `refund-confirmation.html` | Montant + méthode seulement | Ajouter délai estimé selon méthode (mobile money = immédiat, virement = 3-5j) |
| `customer-created.html` | Email de bienvenue basique | Ajouter les prochaines étapes (profil, KYC, choisir un abonnement) |
| `member-created.html` | *(Remplacé par `welcome-member-email`)* | — |
| `subscription-activated.html` | Confirmation simple | Injecter le plan, les dates, les heures incluses |
| `subscription-cancelled.html` | Notification sèche | Ajouter raison, offre de rétention, contact support |

---

## 6. Templates HTML ajoutés — statut du câblage

| Template | Type | Câblé | Service |
|---|---|---|---|
| `billing/avoir-note-credit.html` | PDF | ✅ | `BillingDocumentPdfServiceImpl` — auto sur `CREDIT_NOTE` |
| `form/welcome-member-email.html` | Email | ✅ | `MemberServiceImpl.sendMemberCreatedEmailAfterCommit` |
| `form/badge-membre.html` | PDF | ❌ | À câbler dans un service membre / PDF |
| `member/fiche-inscription.html` | PDF | ✅ | `MemberPdfGenerationImpl.generateMemberInformationPdf()` |
| `member/fiche-client.html` | PDF | ✅ | `MemberPdfGenerationImpl.generateFicheClientPdf()` |
| `contracts/membership-agreement.html` | PDF | ✅ | `ContractGenerationServiceImpl` — code `membership-agreement` |
| `contracts/business-service-agreement.html` | PDF | ✅ | `ContractGenerationServiceImpl` — code `business-service-agreement` |
| `contracts/subscription-pass-non-refundable.html` | PDF | ✅ | `ContractGenerationServiceImpl` — code `subscription-pass-non-refundable` |
| `contracts/contrat-domiciliation.html` | PDF statique | ❌ | Pas encore intégré dans le générateur de contrats |

---

## 7. Templates verify/ — à câbler

| Template | Rôle attendu | Action |
|---|---|---|
| `verify/receipt.html` | Page web de vérification d'un reçu de paiement | Câbler dans `VerifyController` |
| `verify/document.html` | Page web de vérification d'un document / facture (via QR code) | Câbler dans `VerifyController` — le QR code des factures pointe vers `/verify/doc/{number}` |
| `verify/not-found.html` | Page 404 pour un document non trouvé | Câbler dans `VerifyController` |

> Le QR code des factures pointe déjà vers `${verifyBaseUrl}/verify/doc/{documentNumber}` — le controller `VerifyController` est vide.

---

## 8. Providers de paiement — état

| Provider | Méthode | État |
|---|---|---|
| PawaPay | Mobile money (Congo COG + COD) | ✅ Complet — deposit, refund, callback, signature |
| NoopMobileMoneyProvider | Fallback | ✅ Stub fonctionnel |
| Stripe | Carte bancaire | ❌ Non implémenté |
| PayPal | Portefeuille / carte | ❌ Non implémenté |
| Adyen | Multi-méthode | ❌ Non implémenté |
| USSD | Mobile (sans app) | ❌ Stub uniquement |

---

## 9. Modules minimaux — effort estimé pour compléter

| Module | Fichiers actuels | Effort estimé | Valeur métier |
|---|---|---|---|
| `reporting/` | 14 | 3-5 jours | Haute — comptabilité et pilotage |
| `support/` | 16 | 5-7 jours | Haute — relation client |
| `crm/` | 11 | 7-10 jours | Moyenne — prospection |
| `visitor/` | 12 | 3-4 jours | Moyenne — accueil |
| `task/` | 14 | 2-3 jours | Faible — interne |
| `verify/` | 1 | 1-2 jours | Haute — confiance client (QR codes actifs) |

---

## 10. Ordre de réalisation recommandé

```
Sprint 1 — Impact immédiat (1-2 semaines)
├── VerifyController (QR codes factures déjà générés mais page vide)
├── badge-membre.html → câbler dans MemberPdfGenerationImpl
├── contrat-domiciliation.html → ajouter dans ContractGenerationServiceImpl
├── TODOs marqués (UserMapper.update, PermissionServiceImpl)
└── Amélioration emails billing-reminder + document-event

Sprint 2 — Fonctionnel manquant (2-3 semaines)
├── Dunning payment (relance J+1, J+3, J+7)
├── Alertes quota entitlement (80%)
├── Module reporting — export PDF/CSV
└── SMS provider (Africa's Talking ou Twilio)

Sprint 3 — Enrichissement (3-4 semaines)
├── Module support — SLA, catégories, escalade
├── Module visitor — pré-enregistrement + QR badge
├── Substitution variables dans les contrats
└── Push notifications Firebase

Sprint 4 — Scale (selon besoin)
├── Stripe / autre provider carte
├── Module CRM complet
├── OCR avancé pour KYC
└── E-signature (DocuSign / HelloSign)
```