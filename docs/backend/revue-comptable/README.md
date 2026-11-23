# Revue du module comptable

Branche `feature/revue-comptable`, partie de new-main `7c6b8491`, revue du 23/11/2026.

## Demande

1. Revoir les écritures automatiques passées par le module comptable.
2. Revoir les propositions que fait le module et ses recommandations.
3. Revoir les indications données pendant la saisie des écritures.
4. Dire et outiller ce qui doit être fait quand une écriture est mal placée.
5. Revoir tout ce qui est analytique.

## Constat en bref

| Volet | Constat |
|---|---|
| Écritures automatiques | Tous les événements ont un schéma et tous les comptes existent, mais plusieurs schémas produisent des soldes faux : avoir sur acompte en produit au lieu du 4191, avoir imputé sur une autre facture sans écriture, TVA bloquée au 4438 en régime des encaissements, échéance jamais portée sur le 411, factures avec caution toujours en attente (165 sans tiers). |
| Moteur | Deux blocages de file possibles (50 événements coincés suffisent), une correction manuelle d'une pièce en attente n'est jamais reportée sur l'événement, un fait corrigé à la source n'est jamais relu, un fait en retard change de mois sans erreur. |
| Saisie | Seuls des refus bloquants : pas d'avertissement (sens anormal, TVA incohérente, doublon, compte collectif), pas d'aide (contrepartie, TVA calculée, aperçu analytique), messages sans accents ni action proposée. |
| Pièce mal placée | Une seule voie : contre-passer toute la pièce puis ressaisir. Pas de reclassement d'une ligne, pas de lien entre erreur et correction ; contre-passer une pièce automatique empêche de la régénérer. |
| Propositions | Créances douteuses : rejet oublié chaque nuit, classement d'office après 365 jours ; banque : pointage d'office de combinaisons ambiguës ; lettrage 1 pour 1 seulement ; estimations ni modifiables ni rejetables. Aucune proposition n'explique pourquoi. |
| Recommandations | La clôture perd tous les avertissements ; pas de contrôles permanents (471 non soldé, caisse créditrice, 4191 à imputer, TVA à déclarer) ; notifications par courriel seulement, liste vide par défaut. |
| Analytique | Contre-passation sans analytique (montants qui restent), budget par section faux, reflet classe 9 impossible à couper, section obligatoire jamais contrôlée, presque tout sur STRUCTURE. |

Détail : [ecarts.md](ecarts.md), 85 écarts avec fichier, ligne, effet et lot.

## Ce qui doit être fait quand une écriture est mal placée (règle cible)

| Situation | Aujourd'hui | Cible |
|---|---|---|
| Brouillard ou pièce en attente | Modifier ou rejeter | Idem, avec trace de l'écart avec ce que le schéma avait produit (S4) ; un brouillard produit par un événement ne se supprime pas, il se rejette (S3) |
| Pièce validée, mauvais compte ou mauvais tiers sur une ligne | Contre-passer toute la pièce, ressaisir | **Reclassement** : une pièce OD qui sort le montant du mauvais compte et le porte sur le bon, liée à la pièce d'origine, lettrage et analytique suivis |
| Pièce validée entièrement fausse | Contre-passer, ressaisir | **Corriger** : contre-passation et pièce juste en une seule action, les trois pièces liées ; pour une pièce automatique, la pièce juste vient de l'événement régénéré avec les données à jour |
| Pièce automatique fausse parce que la donnée source était fausse | Impossible de régénérer | Corriger la source, puis « Recomptabiliser » : contre-passation, relecture des données, nouvelle pièce |
| Pièce automatique fausse parce que le schéma était faux | Impossible de régénérer | Corriger le schéma, puis recomptabiliser en lot les événements concernés (aperçu avant) |
| Période close | Refus | Correction datée de la première période ouverte, avec mention de la période d'origine |
| Analytique seule fausse | Reclassement analytique | Idem, refusé sur période close et pièce contre-passée |

## Lots proposés

| Lot | Contenu | Écarts |
|---|---|---|
| K0 | Écritures automatiques justes et file qui ne se bloque pas | A1 à A14, M1 à M4, M7 |
| K1 | Corriger une pièce mal placée : reclasser, corriger, recomptabiliser ; contre-passation sûre | M5, M6, S1 à S6, S11 |
| K2 | Saisie guidée : avertissements non bloquants, propositions de contrepartie et de TVA, messages en français avec l'action à mener | S7 à S10 |
| K3 | Propositions traçables : motif lisible, score, rejet durable, péremption ; créances, banque, lettrage, estimations, immobilisations, budget, fiscalité | P1 à P17 |
| K4 | Recommandations : tableau des anomalies permanent, clôture complète, notifications dans l'application | P18, R1 à R9 |
| K5 | Analytique | N1 à N18 |
| K6 | Points de norme arbitrés par l'expert-comptable, puis appliqués | X1 à X8 |

## Décisions

À prendre avec l'exploitant avant K0 (voir la conversation du 23/11/2026).
