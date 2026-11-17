# Lot KG7 : améliorations du centre KYC

> Partie du [plan KYC et GED](README.md). Fait le 17/11/2026 sur `feature/kyc-ged`.

## Tableau de bord du centre

`GET /kyc/dashboard/center?from=&to=` (`KYC:READ`, `KycCenterService.dashboard`) : dossiers par statut, file d'attente par ancienneté (moins de 2 jours, 2 à 7, plus de 7), délai moyen et médian de la soumission à la décision sur la période (30 jours par défaut), dossiers en retard sur `slaDeadline`, charge par agent (affectés ouverts, décidés sur la période), dossiers bloqués (correction depuis plus de 14 jours, aucune pièce 14 jours après l'ouverture), nombre de groupes de doublons.

## Renouvellements, refus définitif, motifs

- `GET /kyc/renewals?days=90` : pièces vivantes qui expirent, par semaine. `POST /kyc/renewals/remind` (`KYC:MANAGE`) : un rappel d'échéance par pièce, sauf relancée depuis moins de 7 jours (`kyc_document.renewal_reminded_at`).
- Refus définitif : `POST /kyc/cases/{code}/reject-final` (`KYC:REJECT`), motif de la liste obligatoire ; statut `REJECTED` réellement posé, membre non actif `REJECTED`, courriel `KYC_CASE_REJECTED` (« Votre dossier KYC n'a pas été retenu », modèle réécrit), dépôts refusés (`KycPieceLinker`), recalcul et synchronisation du membre qui ne le défont plus. `/reject` reste la demande de correction (nom gardé pour le frontend).
- Réouverture : `POST /kyc/cases/{code}/reopen`, administrateur seulement ; événement `KYC_CASE_REOPENED` (chronologie, sans courriel).
- Motifs : table `kyc_reason` (11 motifs préenregistrés, `systemReason`, non supprimables), `GET/POST/PUT/DELETE /kyc/reasons` ; `reasonCode` sur la correction (facultatif, l'ancien texte seul reste accepté) et sur le refus définitif ; `kyc_case.decision_reason_code`, `decisionReasonCode` et `decisionReasonLabel` dans la réponse.

## WhatsApp et doublons

- `KycWhatsAppNotifier`, appelé par `KycOutboxEventProcessor` après le courriel pour `KYC_CASE_REQUIREMENT_ADDED`, `KYC_CASE_LEVEL_CHANGED`, `KYC_CASE_CORRECTION_REQUESTED`, `KYC_DOCUMENT_CORRECTION_REQUESTED` : objet `KYC_DOCUMENTS_REQUESTED`, variables prénom, pièces, référence ; bouton `bokati.whatsapp.portal-path.kyc` (`client/kyc`). Consentement : préférence du membre `EventType.KYC_DOCUMENTS_REQUESTED` sur WhatsApp, plus l'interrupteur général des envois automatiques.
- Doublons : même type et numéro normalisé chez deux titulaires ; `duplicateOf` sur chaque pièce de la fiche, `GET /kyc/duplicates`. Jamais bloquant (la règle préenregistrée `NUMERO_UNIQUE` du lot KG9 le signale aussi à l'approbation).

## Historique dans le centre d'audit

`KycCase`, `KycDocument`, `KycCaseRequirement` et `Document` ajoutés aux entités principales (`AuditedEntities`) : avant et après de chaque champ, chronologie par `entityCode`.

## Migration

`V370__kyc_ged_kg7_motifs_renouvellements.sql` (production `V364`) : `kyc_reason` et ses motifs, `kyc_case.decision_reason_code`, `kyc_document.renewal_reminded_at`.

## Action de l'exploitant

Modèle WhatsApp `kyc_pieces_demandees` à créer chez Infobip et à faire approuver par Meta (catégorie « Utility »), puis à enregistrer avec l'objet `KYC_DOCUMENTS_REQUESTED`. Texte proposé :

> Bonjour {{1}}, pour compléter votre dossier de vérification ({{3}}), merci de déposer : {{2}}. Vous pouvez le faire depuis votre espace client.

Bouton « Ouvrir mon espace » vers `https://espace.elleaose.com/{{1}}`. Rien n'est soumis à Meta sans votre accord.

## Tests

`KycCenterKg7Test` : refus définitif (motif obligatoire, du bon type, précision pour « Autre »), fermeture et événement, réouverture par un administrateur seulement, correction refusée sur un dossier refusé, doublons (normalisation, même titulaire ignoré), relance groupée avec délai, tableau de bord.
