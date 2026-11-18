# Constat : demande de fonds et approbation

> Partie du [constat des listes de choix](README.md). Relevé du 18/11/2026 sur `feature/booking` (lecture du code, sans modification). Chemins relatifs à `src/main/java/com/sni/bokaticowork/features/`. `FundDtos` = `fund/dto/FundDtos.java`, `ApprovalDtos` = `approval/dto/ApprovalDtos.java`.

## En bref

| Module | Valeurs fixes non exposées | Liste de recherche manquante | Texte libre à mettre en liste | Recherche manquante | Automatisation manquante |
| --- | --- | --- | --- | --- | --- |
| Demande de fonds | 7 | 24 | 6 | 10 | 19 |
| Approbation | 6 | 5 | 3 | 6 | 9 |

**Cause principale** : la plupart des listes dont les formulaires ont besoin existent déjà, mais derrière des droits qu'un demandeur n'a pas. Le rôle `STAFF` n'a que `FUND_REQUEST_CREATE` (migration V300, ligne 253), alors que :

| Liste | Droit exigé |
| --- | --- |
| `/cash-registers/natures`, `/cash-registers` | `CASH:READ` |
| `/accounting/analytic/**` | `ANALYTIC_REPORT:READ` |
| `/accounting/budget/forecasts`, `/unplanned-reasons` | `BUDGET_REPORT:READ` |
| `/inventory/**` | `INVENTORY:READ` |
| `/admin/users`, `/admin/permissions` | `SYSTEM:USERS`, `SYSTEM:PERMISSIONS` |

Aucune route ne rend les valeurs fixes de ces deux modules avec leurs libellés français. Le seul modèle qui marche est `/inventory/reference/enums` et `/data`.

**Mécanisme à réutiliser pour les listes de l'administrateur** : les valeurs de référence de la caisse (`CashReferenceValue`). Elles acceptent n'importe quel nouveau code de liste sans changement de code (`listCodeOf`, `CashRegisterAdminServiceImpl.java:1185`). Plusieurs listes sont déjà semées (`ABSENCE_REASON`, `BANK`, `EXTERNAL_DOCUMENT_TYPE`, `REVERSAL_REASON`…). `CashReferenceLists.describe()` gère déjà « Autre, préciser » et le décompte des usages. Le module des fonds n'en utilise aucune.

## Demande de fonds (`fund/`)

### Valeurs fixes à exposer

| Champ | Constat | Proposition |
| --- | --- | --- |
| `NeedRequest.urgency` (FundDtos:30) | `FundUrgency` NORMAL, URGENT, CRITICAL sans libellés | `GET /fund-requests/reference/enums` (modèle `/inventory/reference/enums`, droit `FUND_REQUEST:CREATE`) |
| `TransformNeed.type`, `RequestInput.type` (FundDtos:49, 63) | `FundRequestType` (7 valeurs), libellés seulement dans la doc | Même route ; suggérer le type d'après les lignes |
| `RequestInput.beneficiaryType` (FundDtos:67) | SELF, EMPLOYEE, THIRD_PARTY | Même route |
| `RequestInput.deliveryMode` (FundDtos:73) | CASH, MOBILE_MONEY, TRANSFER ; ni opérateur, ni numéro, ni banque saisis ; le décaissement l'ignore (toujours sortie de caisse) | Même route ; banque depuis la liste de caisse `BANK`, liste des opérateurs mobile money |
| `Delegation.level` (FundDtos:165) | Entier 1 à 3, libellés en dur (`FundRequestService.java:411`) | `GET /fund-requests/delegations/levels` : niveaux de l'utilisateur, libellés |
| Statuts et filtres (`FundController.java:126`) | `FundRequestStatus`, `FundNeedStatus`, `ExpenseLineStatus`, `FundDisbursementKind`, `FundApprovalDecision` sans libellés | Dans la route des valeurs fixes |

### Listes de recherche à ouvrir au demandeur

| Champ | Constat | Proposition |
| --- | --- | --- |
| `NeedLineRequest.designation` (FundDtos:23) | Texte libre | Recherche d'article facultative (inventaire, catalogue) |
| `NeedLineRequest.unit` (FundDtos:23) | Texte libre non contrôlé (`FundNeedService.java:85`) | Unités dans le lot de données des fonds ; contrôle |
| `NeedLineRequest.natureCode`, `RequestLine.natureCode`, `ExpenseInput.natureCode` | Existence seule contrôlée (ou rien pour les besoins, `FundNeedService.java:82`) ; natures inactives ou d'entrée acceptées (`FundRequestService.java:223`) ; recherche des natures en `CASH:READ` sans pagination | `GET /fund-requests/lookups/natures?q=&direction=OUT&page=` (actives : code, libellé, compte, section par défaut) ; refuser inactives et entrées |
| `supplier`, `supplierCode` (besoins, demandes, dépenses) | Texte libre et code facultatif ; fournisseurs en `INVENTORY:READ` ; suggestions seulement sur un numéro enregistré | `GET /fund-requests/lookups/suppliers?q=` ; suggestions sur des lignes non enregistrées |
| `NeedRequest.department`, `RequestInput.requesterDepartment` | Texte libre, aucun référentiel de service | Liste `FUND_DEPARTMENT` (valeurs de caisse) ou axe analytique CENTRE ; défaut depuis le profil |
| `NeedRequest.locationCode` (FundDtos:30) | Non contrôlé ; `/inventory/locations` sans recherche ni pagination | Emplacements actifs dans le lot de données ; contrôle |
| `NeedRequest.costCenter`, `RequestLine.section`, `ExpenseInput.section` | Texte libre, aucun axe défini | `GET /fund-requests/lookups/sections?axis=CENTRE&q=` ; défaut depuis la nature ; contrôle |
| `attachments` (besoins, demandes) | Chaîne opaque | Dépôt ou codes de documents de la GED, en liste |
| `TransformNeed.registerCode`, `RequestInput.registerCode` | `trim()` seulement | `GET /fund-requests/lookups/registers?q=` (caisses actives et solde) ; défaut par site |
| `RequestInput.beneficiaryCode` (FundDtos:68) | Tout texte accepté ; repli sur le nom (`FundRequestService.java:242`) | `GET /fund-requests/lookups/beneficiaries?type=EMPLOYEE|THIRD_PARTY&q=` ; contrôle |
| `RequestInput.forecastNumber` (FundDtos:85) | Non contrôlé (`BudgetEngine.java:276`) ; prévisions en `BUDGET_REPORT:READ`, sans recherche | `GET /fund-requests/lookups/forecasts?q=&natureCode=&section=` (ouvertes) ; contrôle |
| `RequestInput.unplannedCode` (FundDtos:86) | Facultatif, non contrôlé ; aucune administration des motifs | Obligatoire si hors budget ; dans le lot de données ; administration (ou liste `BUDGET_UNPLANNED_REASON`) |
| `LinkTransfer.transferOrderNumber` (FundDtos:107) | N'importe quel texte solde la demande (`FundRequestService.java:663`) | Filtre `?reference=` sur les virements ; vérifier l'existence et le montant |
| `ExpenseInput.documentReference` (FundDtos:154) | Texte libre | Dépôt dans la GED et type depuis `EXTERNAL_DOCUMENT_TYPE` (existe) |
| `Delegation.delegate` (FundDtos:165) | Courriel saisi, non contrôlé | Recherche des utilisateurs éligibles |

### Textes libres à mettre en liste

| Champ | Proposition |
| --- | --- |
| `beneficiaryIdNumber` (FundDtos:70) | Ajouter `beneficiaryIdType` (liste `ID_DOCUMENT_TYPE`) ; reprendre la dernière demande du bénéficiaire |
| `missionDestination` (FundDtos:75) | Liste souple `FUND_MISSION_DESTINATION` avec « Autre » |
| `unplannedReason` (FundDtos:79) | Le code devient obligatoire, le texte devient la précision |
| `Decision.comment` (rejet, renvoi, annulation, réduction, rejet de besoin) | `reasonCode` et précision ; listes `FUND_REJECT_REASON`, `FUND_RETURN_REASON`, `FUND_CANCEL_REASON`, `FUND_REDUCTION_REASON` |
| `Reject.reason` (FundDtos:162) | Liste `FUND_EXPENSE_REJECT_REASON` (pièce illisible, hors objet, doublon…) |
| Délégation (sans motif, `FundRequestService.java:865`) | `reasonCode` depuis `ABSENCE_REASON` (existe) |

### Recherches manquantes

`GET /fund-needs/mine`, `/fund-needs/submitted` (toutes les demandes pour tout niveau 1), une liste générale des besoins (absente), `GET /fund-requests`, `/mine`, `/to-approve`, `/to-disburse`, `/advances`, `/justifications-to-validate`, `/delegations` : listes complètes, sans recherche texte, sans filtres (ou un seul) et sans pagination. Proposition : `q` (numéro, objet, bénéficiaire), type, statut, demandeur, service, montants, dates, pagination ; compteur pour les badges.

### Automatisations manquantes

- **Valeurs par défaut** : téléphone du demandeur, nom et téléphone du bénéficiaire, signataire, prix unitaire (dernier prix), TVA (règle de la nature), session de caisse ouverte du caissier. Une route `GET /fund-requests/form-defaults?context=` (modèle `/cash-registers/form-defaults`).
- **Circuit** : niveaux, libellés et droits en dur, envoyés au moteur (`FundRequestService.java:336, 410-426, 969`) ; les circuits de l'administrateur pour CASH sont ignorés. Semer le circuit CASH/FUND_REQUEST avec ses seuils.
- **Approbation automatique** : sous un seuil, budget couvert et prévision citée (le moteur sait déjà approuver sans niveau, `ApprovalEngine.java:117`).
- **Besoins hors moteur** (`FundNeedService.java:126`) : tout titulaire du niveau 1 décide, sans routage, rappel ni relance. Les passer au moteur.
- **Renvoi** : la demande repart toujours au niveau 1 (`FundRequestService.java:330`) ; reprendre au niveau qui l'a renvoyée.
- **Décaissement partiel** : jamais clos ni expiré (`FundLifecycleService.java:63`) ; clore le reste et libérer le budget.
- **Virement** : liaison manuelle ; lier quand un virement cite la demande.
- **Suggestion de nature** : seulement sur les besoins ; l'étendre aux demandes et aux dépenses.
- **Avances** : complément dû et retenue sur salaire manuels ; prévenir le caissier, proposer la retenue après N jours.
- **Bon de décaissement** : émis seulement à la lecture du PDF (`fund/voucher/FundVoucherService.java:200`) ; l'émettre au décaissement et l'archiver dans la GED.
- **Duplication** (`FundRequestService.java:208`) : ancien constructeur, `forecastNumber` et `unplannedCode` perdus. Défaut à corriger.
- **Transformation du besoin** : manuelle même entièrement validé ; option de transformation automatique.

## Approbation (`approval/`)

| Champ ou route | Nature | Constat | Proposition |
| --- | --- | --- | --- |
| `LevelSpec.rule`, `depth` (ApprovalDtos:25) | Valeurs fixes | `ApproverRule` MANAGER, PERMISSION, USER ; profondeur brute | `GET /approvals/reference` (valeurs et libellés, N+1 à N+5) |
| `CircuitInput.center`, `requestType` (ApprovalDtos:73-74) | Valeurs fixes | Texte libre ; centres et types cachés dans les gestionnaires ; non contrôlés | `GET /approvals/centers` (centres, types, niveaux, depuis les gestionnaires) ; contrôle |
| `DelegationInput.center`, `level` (ApprovalDtos:87) | Valeurs fixes | Texte et entier sans signification à l'écran | `GET /approvals/centers/{c}/levels` |
| Filtres de l'historique (`ApprovalController.java:92-95`) | Valeurs fixes | Type libre, `ApprovalStatus` sans libellés | Route de référence |
| `LevelSpec.permission` (ApprovalDtos:26) | Recherche | Tout texte accepté (`ApprovalEngine.java:498`) ; permissions en `SYSTEM:PERMISSIONS` | `GET /approvals/reference/permissions?q=` ; contrôle |
| `LevelSpec.userLogin`, `DelegationInput.delegate`, `HierarchyUpdate.userCode` | Recherche | Courriel saisi, non contrôlé ; organigramme complet non paginé ; codes ici, courriels ailleurs | `GET /approvals/users?q=&page=` (utilisateurs actifs : courriel, nom, poste ; les deux identifiants) ; contrôle |
| Demandeur et approbateur de l'historique | Recherche | Égalité exacte du courriel | Recherche partielle |
| `Decision.comment` (ApprovalDtos:40) | Texte en liste | Obligatoire, libre (`ApprovalEngine.java:289`) | `reasonCode` et précision ; `APPROVAL_REJECT_REASON`, `APPROVAL_RETURN_REASON` |
| `DelegationInput.reason` (ApprovalDtos:88) | Texte en liste | Libre | `ABSENCE_REASON` et précision |
| `LevelSpec.label` | Texte en liste | Libre (faible priorité) | Liste souple `APPROVAL_LEVEL_LABEL` |
| `/approvals/{center}/mine`, `/to-approve`, `/delegations`, `/hierarchy` | Recherche | Non paginées, sans filtres ; une lecture de l'organigramme par demande | `q`, statut, type, montant, ancienneté, tri, pagination ; `GET /approvals/{center}/to-approve/count` |
| `/approvals/{center}/history` | Recherche | Paginée et filtrée, sans recherche texte | `q`, montants, tri |

Automatisations manquantes :

- **Relance** : une seule notification (`ApprovalEngine.java:595`), jamais de réaffectation. Il faut des étapes, puis le supérieur ou un suppléant.
- **Rappels** : en heures brutes, week-ends compris (`ApprovalEngine.java:573`). Il faut un calendrier ouvré et un récapitulatif quotidien.
- **Absences** : les absences de caisse n'alimentent pas les délégations. Il faut une délégation automatique quand une absence est approuvée.
- **Approbateur absent** : la soumission échoue (`ApprovalEngine.java:136`). Il faut un repli défini dans le circuit.
- **Même approbateur sur deux niveaux** : le niveau n'est pas sauté. Il faut le sauter (réglage).
- **Approbation automatique** : seulement implicite, quand le seuil écarte tous les niveaux. Il faut une règle explicite, tracée « SYSTEM ».
- **Demandes en attente** : ouvertes sans fin. Il faut une annulation après N jours, avec avis.
- **Décisions groupées** : absentes. Il faut une route d'approbation par lot.
- **Délégations** : ni chevauchement, ni chaîne, ni droit du délégant contrôlés sur la route générale (`ApprovalEngine.java:533`).

Même constat côté facturation : `RequestInvoiceChangeRequest.reason` et `discountOverrideReason` sont libres ; même solution par listes de motifs.

## Priorités

1. **Lot de référence des fonds** : `GET /fund-requests/reference/enums` et `/data` (droit `FUND_REQUEST:CREATE`). Il rend natures, caisses, unités, emplacements, sections, prévisions ouvertes, motifs hors budget et listes des fonds.
2. **Recherches des fonds pour le demandeur** : bénéficiaires, fournisseurs, prévisions, sections, puis contrôle des codes.
3. **Références de l'approbation** : centres, valeurs fixes, utilisateurs et permissions ; contrôle à l'enregistrement.
4. **Circuits de l'administrateur pour les fonds**, et seuil d'approbation automatique.
5. **Codes de motif** pour les rejets, renvois, annulations et réductions (fonds et approbation).
6. **Recherche paginée** sur toutes les listes des fonds et de l'approbation, et compteur pour les badges.
7. **Valeurs par défaut des formulaires** (session de caisse, signataire, service, téléphone, section, prévision).
8. **Besoins dans le moteur d'approbation** ; reprise au niveau qui a renvoyé.
9. **Délégations depuis les absences**, relance en étapes, repli sans approbateur.
10. **Reliquats** :
    - virements contrôlés et liés automatiquement ;
    - décaissements partiels clos ;
    - suggestion de nature ;
    - bon de décaissement émis au décaissement ;
    - duplication corrigée ;
    - type de pièce d'identité ;
    - liste des destinations de mission.
