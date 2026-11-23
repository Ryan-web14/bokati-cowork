# Lot K1 : corriger une pièce mal placée

Branche `feature/revue-comptable`. Migrations : dev `V401__comptabilite_k1_corriger_une_piece.sql`, prod `V395__comptabilite_k1_corriger_une_piece.sql` (contenu identique).

> Numérotation : la branche `feature/subscription` (lot S0, pas encore fusionné) utilise V399 / V393. Flyway refuse les migrations hors ordre : fusionner S0 avant K0 et K1, ou renuméroter S0.

## Ce qui change

| Écart | Avant | Après |
|---|---|---|
| S1 | Contre-passer à la main une pièce automatique laissait l'événement « comptabilisé » : régénération refusée, module source pas prévenu | L'événement passe `REVERSED` (nouvel état) avec le motif, le module source est prévenu. **Recomptabiliser** (`PATCH /posting/events/{id}/repost`) le refait ; **Écarter** s'il ne doit plus rien produire |
| S2 | Pas de correction : contre-passer toute la pièce et ressaisir, sans lien | **Reclasser** (`POST /entries/{ref}/reclassify`) : pièce OD liée, une sortie et une entrée par ligne, montant partiel possible, analytique d'origine ou donnée, lettrage de la sortie si la ligne est reclassée en entier. **Corriger** (`POST /entries/{ref}/correct`) : contre-passation et pièce juste en une action (pièce manuelle), ou recomptabilisation (pièce automatique). Liens `correctionOf`, `correctionKind` (`RECLASS`, `CORRECTION`, `REPOST`), `correctedBy`. Recomptabilisation en lot après correction d'un schéma, avec aperçu (`POST /posting/events/repost`) |
| S3 | Un brouillard d'événement se supprimait ; l'événement pointait vers une pièce disparue | Suppression refusée : il se rejette. Rejeter une pièce d'événement fait repasser l'événement `FAILED` avec « Pièce … rejetée : motif » |
| S4 | Pièce automatique modifiée librement, sans trace | Le journal ne change plus (il vient du schéma) ; à la première modification, les lignes du schéma sont gardées (`generatedLines`), avec l'auteur, la date et un motif facultatif (`changeReason`) |
| S5 | Contre-passation et régularisation datées de la date du serveur (UTC) | Date du jour à Brazzaville ; une contre-passation n'est jamais datée avant sa pièce d'origine |
| S6 | Régularisation toujours datée du jour | `date` dans le corps (défaut : aujourd'hui), dans une période ouverte, chronologie du mois respectée |
| S11 | Seconde validation au-delà du seuil seulement à la validation d'une saisie | Couvre aussi contre-passations, régularisations, reclassements et corrections : au-delà du seuil, brouillard à valider par une autre personne (`awaitingSecondValidation`). Le moteur (annulations de faits métier) n'y est pas soumis |
| M5 | Un fait en retard prenait la dernière date du journal, même un autre mois | Décision D3 : la chronologie stricte ne s'impose que dans le mois. Un fait garde sa date si sa période est ouverte ; dans son mois, il prend au plus tôt la dernière date du journal. Même règle pour les saisies manuelles (une pièce de mars après des pièces d'avril est acceptée) |
| A13 | Aucune contre-passation pour la déduction d'un acompte quand la facture finale est annulée, ni pour le transfert de règlement quand la nouvelle facture est annulée | Événements `DEPOSIT_APPLICATION_CANCELLED` et `RECEIVABLE_TRANSFER_CANCELLED` (contre-passations). Une facture annulée par un avoir n'en produit pas (l'avoir porte la correction). Le mouvement de caution n'a pas d'annulation dans la facturation : rien à contre-passer |
| A14 | Un remboursement rouvrait d'abord les factures, jamais le trop-perçu ; deux remboursements partiels rouvraient deux fois la même | Règle commune facturation et comptabilité (`RefundSplit`) : la part non affectée (4719) est remboursée d'abord, puis les affectations dans l'ordre ; chaque remboursement reprend là où le précédent s'est arrêté |

## Règles de date

| Action | Date par défaut | Contrôles |
|---|---|---|
| Contre-passer | aujourd'hui (Brazzaville) | période ouverte, pas avant la pièce d'origine, chronologie du mois |
| Reclasser, Corriger | date de la pièce d'origine si son mois est ouvert (au plus tôt la dernière pièce du journal ce mois-là), sinon aujourd'hui | idem |
| Régulariser | aujourd'hui | période ouverte, chronologie du mois |

Avec une numérotation annuelle (défaut des journaux), une pièce de mars passée après des pièces d'avril prend le numéro suivant : les numéros suivent l'ordre de validation, les dates restent dans leur mois. Pour des numéros dans l'ordre des dates de chaque mois, passer le journal en remise à zéro par période.

## Pour l'exploitant

- Réglage `accounting.correction.journal` (défaut `OD`) : journal des reclassements.
- Droits : Reclasser et Corriger demandent `ACCOUNTING_ENTRY:REVERSE` ; Recomptabiliser demande `ACCOUNTING_POSTING:REPLAY`.
- Seuil de seconde validation : `accounting.manual-entry.second-validation-threshold` (0 = jamais) s'applique désormais aussi aux contre-passations et corrections.

## Tests

- `K1CorrectionsTest` : règle de remboursement (trop-perçu d'abord, remboursements successifs, transaction entièrement affectée), migration identique en dev et en prod.
- `PaymentAllocationServiceTest` : un trop-perçu remboursé ne rouvre aucune facture, le remboursement suivant rouvre la suite.
- `K1CorrectionsIT` (PostgreSQL, migration appliquée) : contre-passation à la main puis recomptabilisation ; recomptabilisation en lot après correction d'un schéma (aperçu puis exécution) ; reclassement de compte et de tiers avec lettrage ; correction d'une pièce manuelle, refus de lignes sur une pièce automatique ; seconde validation d'une contre-passation ; fait en retard qui garde son mois ; trace d'une pièce automatique modifiée et rejet qui repasse l'événement en échec.
- Non-régression : tests unitaires de la comptabilité, de la facturation, des paiements, des fonds, des réglages et de la sécurité (1 104) ; intégration `AccountingFoundationF1IT`, `PostingEngineF2IT`, `CashAccountingF3IT`, `SalesAccountingF4IT`, `VatF11IT`.
