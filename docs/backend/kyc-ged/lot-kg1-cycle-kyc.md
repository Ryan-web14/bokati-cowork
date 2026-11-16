# Lot KG1 : cycle KYC cohérent, exigences du dossier, niveau KYC

> Partie du [plan KYC et GED](README.md). Fait le 16/11/2026 sur `feature/kyc-ged`.

## Un seul calcul des exigences

`KycRequirementResolver` (`features/document/kyc/service/support`) sert au recalcul, à la fiche du dossier, à la soumission, à l'approbation, au portail et aux dépôts.

Exigences d'un dossier =

1. les exigences globales actives du type de titulaire, filtrées par type de client (`customerType`), forme juridique (`businessLegalForm`) et **niveau KYC demandé** du dossier (une exigence de niveau 2 n'est demandée qu'aux dossiers de niveau 2 et plus) ;
2. **plus** les exigences propres au dossier : une ligne active ajoute la pièce ou remplace le caractère obligatoire d'une exigence globale ; une ligne inactive dispense le dossier de la pièce.

Avant, une seule exigence propre active faisait disparaître toutes les exigences globales, et le recalcul automatique ignorait les exigences propres.

## Exigences propres au dossier

| Action | Effet |
| --- | --- |
| Ajout (`POST /kyc/cases/{code}/requirements`) | Type vérifié (`400` « Type de document inconnu »). Une exigence retirée se remet. La pièce apparaît aussitôt dans la fiche, la liste et l'avancement du portail ; le dossier est recalculé (un dossier approuvé se rouvre si la pièce est obligatoire et manque). |
| Retrait (`DELETE /kyc/cases/{code}/requirements/{type}`) | Pour une exigence globale, une ligne de dispense est créée. Recalcul. `404` si la pièce n'est pas demandée. |
| Dépôt | L'équipe (`/kyc/documents/admin/upload`) et le titulaire (portail) ne peuvent déposer qu'une pièce demandée pour le dossier : `400` « Cette pièce n'est pas demandée … ». |

## Niveau KYC demandé (demande de l'exploitant du 16/11/2026)

- Colonne `document_requirement.kyc_level` (1 à 4, 1 par défaut) : niveau à partir duquel la pièce est demandée.
- Colonne `kyc_case.requested_kyc_level` (1 par défaut) : niveau demandé pour le dossier. `kycLevel` reste le niveau atteint, au moins égal au niveau demandé à l'approbation.
- `PATCH /sni/api/v1/kyc/cases/{code}/level`, corps `{ "level": 2, "reason": "…" }`, droit `KYC:MANAGE` :
  - `400` si le niveau sort de 1 à 4 ou si le dossier est `REJECTED` ou `EXPIRED` ;
  - même niveau : aucun effet ;
  - sinon : niveau enregistré, événement `KYC_CASE_LEVEL_CHANGED` (niveaux avant et après, motif, pièces manquantes) et courriel au titulaire « Quelques pièces complémentaires pour votre dossier KYC » listant les pièces à fournir ; recalcul : un dossier approuvé auquel il manque une pièce obligatoire du nouveau niveau repasse `IN_PROGRESS`. Le membre ou le client reste actif pendant ce temps.
  - Baisser le niveau ne rouvre rien ; les pièces du niveau supérieur ne sont plus exigées.
  - Portail : un membre `ACTIVE` dont le dossier est rouvert (`IN_PROGRESS`, `SUBMITTED`, `UNDER_REVIEW`) garde l'accès à son espace (`ClientOnboardingGuard`) ; une correction, un refus ou une échéance le bloquent toujours.
- Migrations : dev `V368__kyc_ged_kg1_niveaux.sql`, prod `V362__kyc_ged_kg1_niveaux.sql`.

## Décisions et recalcul

- Une seule approbation : que l'équipe approuve ou que le recalcul approuve, le titulaire devient actif, son portefeuille est créé et le courriel `KYC_CASE_APPROVED` part.
- Approbation refusée sur un dossier déjà `APPROVED` (pas de second courriel), `REJECTED` ou `EXPIRED`.
- Le recalcul ne touche plus un dossier `REJECTED` ; il garde `SUBMITTED` ; un dossier renvoyé en correction n'est plus écrasé.
- Activer un client n'approuve plus son dossier.
- Le verso doit être approuvé quand le type en demande un (évaluation et recalcul).
- Une pièce renvoyée en correction par les routes documentaires passe `REJECTED` (à refaire) et non plus `PENDING`.
- Liste des pièces du portail : exigences du dossier, avec l'état, le code du recto et du verso de chaque pièce.

## Écarts réglés

K2, K3, K-n2, K-n6, K-n7, K-n8, K-n9, K-n11.

## Tests

`KycRequirementResolverKg1Test` (ajout aux globales, remplacement et dispense, niveaux, type de client), `KycLevelChangeKg1Test` (changement de niveau, dossier clos, niveau hors bornes), `ClientOnboardingGuardReopenKg1Test` (accès gardé pendant la réouverture) ; suites KYC, documents et boîte d'envoi vertes.
