# Lot KG3 : événements, relances, automatismes

> Partie du [plan KYC et GED](README.md). Fait le 17/11/2026 sur `feature/kyc-ged`.

## Événements

- `KycOutboxEventProcessor` prend **tous** les événements `KYC_CASE`. Ceux qui prévenaient le titulaire continuent ; s'y ajoutent `KYC_CASE_REQUIREMENT_ADDED` (« pièce demandée », avec le nom de la pièce et son caractère facultatif), `KYC_DOCUMENT_CORRECTION_REQUESTED` (avec le motif) et `KYC_RENEWAL_REQUIRED`. Les autres (soumission, affectation, note, risque, recalcul, réouverture…) sont consignés sans courriel. Avant, faute de processeur, ils étaient retentés puis passés `DEAD`.
- Un échec de mise en file du courriel remonte : la boîte d'envoi réessaie (avant, l'erreur était avalée). L'envoi lui-même est suivi par la file des courriels (dédoublonnage compris).
- `KYC_CASE_REJECTED` reste reconnu ; il sera publié par le refus définitif (lot KG7).
- Gabarit `email/kyc-event.html` : trois cas ajoutés.

## Échéances et expiration (`KycAutomationWorker`)

- Chaque jour à 8 h (avant : le lundi seulement, à date exacte).
- Pièces vivantes seulement : en attente ou vérifiées, fichier ni remplacé, ni archivé, ni refusé, ni expiré, dossier non refusé.
- Rappel par **seuil franchi** (`app.kyc.expiry.reminder-days`, 60, 30, 7) et mémorisé sur le document (`expiry_reminder_sent_days`) : un jour manqué est rattrapé, un seuil n'est jamais envoyé deux fois, une nouvelle pièce repart de zéro.
- Expiration après le délai de grâce (`app.kyc.expiry.grace-period-days`, 15) : pièce et document `EXPIRED`, dossier approuvé `RENEWAL_REQUIRED` (courriel) sinon `PENDING_CORRECTION`, titulaire à corriger.
- Une seule logique : la tâche horaire de la GED n'expire plus les documents KYC et ne leur envoie plus de rappel (un seul courriel).
- Une transaction par pièce (et par dossier pour les relances de dossier incomplet) : une erreur n'annule plus tout le passage.

## Approbation automatique (une seule règle)

- `autoApprove` sans délai : approuvé au dépôt.
- `autoApproveAfterDays` > 0 : le document attend la revue (même avec `autoApprove`) ; la tâche du cycle de vie l'approuve quand la **version courante** attend depuis ce délai, jamais s'il a déjà été refusé ou renvoyé en correction.
- Par l'approbation normale (`DocumentService.approve`, utilisateur système) : trace de revue, pièce KYC et dossier mis à jour, une transaction par document.
- La tâche KYC n'approuve plus rien de son côté (avant : deux règles contradictoires, approbation possible d'une pièce corrigée, sans trace de revue).

## Écarts réglés

K7, K8, K9, K-n4, K-n5, K-n12, G-n3.

## Tests

`KycAutomationWorkerKg3Test` (seuil rattrapé une fois, pièces mortes ignorées, renouvellement après grace, échec isolé), `KycOutboxEventProcessorKg3Test` (tous pris, courriel seulement pour les notifiants, échec remonté), `DocumentAutoApproveKg3Test` (délai seul suffit, version récente attend, correction exclue, échec isolé).
