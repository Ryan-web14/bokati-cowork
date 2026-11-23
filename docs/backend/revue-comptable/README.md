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
| K7 | Achats et dépenses guidés pour les non-comptables, lecture OCR, déduction automatique de l'écriture, mode expert | voir plus bas |

## K7 : achats guidés, OCR, déduction automatique, mode expert (demande du 23/11/2026)

### Mode simple (par défaut, pour les non-comptables)

On saisit une **opération courante**, jamais une écriture : achat chez un fournisseur, reçu de restaurant, carburant, course ou transport, fournitures, facture d'eau, d'électricité ou de téléphone, loyer, réparation, abonnement, frais bancaires, note de frais. Chaque type porte sa règle de déduction :

- compte de charge depuis la nature d'opération (déjà reliée aux catégories de fournisseurs) ;
- TVA récupérable ou non selon le type (restauration, réception, cadeaux, carburant de tourisme : règle à confirmer par l'expert-comptable) ;
- fournisseur reconnu par son NIU, son nom ou son téléphone ; fournisseur occasionnel sinon ;
- contrepartie selon le moyen de paiement : caisse, banque, mobile money, à payer (401) ;
- immobilisation proposée au-dessus du seuil ;
- section analytique proposée (règles, fournisseur, ressource).

L'utilisateur voit la pièce qui sera passée, expliquée en phrases (« la dépense va en 6053 Fournitures d'entretien, la TVA n'est pas récupérable sur un reçu de restaurant »), puis confirme.

### Recommandations à l'enregistrement

Doublon probable (même fournisseur, montant, date ou référence) ; pièce justificative absente ; reçu non normalisé ou sans NIU (TVA non déductible, retenue à la source éventuelle) ; paiement en espèces au-delà du plafond ; montant au-dessus du seuil d'immobilisation passé en charge ; dépassement de budget de la section ; facture d'un fournisseur sans commande alors qu'une commande validée existe ; avance fournisseur (4091) à imputer.

### Lecture OCR et déduction

Photo ou PDF d'un reçu ou d'une facture, lu par Tesseract (déjà présent pour le KYC et la GED) : fournisseur (nom, NIU, téléphone), date, numéro, montants HT, TVA, TTC, moyen de paiement. Chaque champ arrive avec un indice de confiance ; les champs douteux sont signalés à vérifier. Le document lu est classé dans la GED et lié à la pièce.

### Mode expert

Droit dédié. L'utilisateur choisit lui-même comment passer l'écriture : comptes, TVA, journal, lignes modifiables à partir de la proposition ; il peut enregistrer sa façon de faire comme **modèle** réutilisable pour ce type d'opération ou ce fournisseur. La proposition automatique reste affichée à côté, et l'écart est tracé.

### Tout ce qui peut devenir automatique

Inventaire à faire en début de K7 : paiements fournisseurs, frais et agios bancaires depuis les relevés, échéances d'emprunt, liquidation de TVA, paie, refacturations de débours ; pour chacun, écriture automatique en mode simple, modifiable en mode expert.

## Décisions (23/11/2026)

| # | Question | Décision |
|---|---|---|
| D1 | Comptabilité déjà active en prod ? | Pas encore : aucun rattrapage de pièces, on corrige le code |
| D2 | Pièce validée mal placée | Reclasser (pièce OD qui déplace une ligne) et Corriger (contre-passation et pièce juste en une action) ; recomptabiliser une pièce automatique avec les données à jour ; pièces liées |
| D3 | Fait arrivé en retard | Garde sa date si sa période est ouverte : la chronologie stricte ne s'impose que dans le mois ; période verrouillée, premier jour ouvert avec mention |
| D4 | Créances de plus de 365 jours | Proposer seulement ; un rejet est durable et ne revient que si la situation change (nouveau palier, contentieux) |
| D5 | Points de norme X1 à X8 | À faire trancher par l'expert-comptable avant K6 |
