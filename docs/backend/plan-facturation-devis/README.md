# Plan facturation et devis

Remise à niveau complète du module de facturation (`features/billing`) et des services qui l'entourent : devis, factures, proformas, avoirs, rectificatives, acomptes, échéanciers, catalogue, relevés clients, envois, avec la comptabilité déjà branchée (lot F4 du plan finance).

Demandé par l'exploitant le 13/11/2026 : d'abord corriger les écarts relevés, puis ajouter les fonctionnalités qui manquent, et compléter le module jusqu'au bout. **Livré le 13/11/2026** sur `feature/facturation-devis` : voir [livraison.md](livraison.md).

## 1. Sommaire

| Fichier | Contenu |
| --- | --- |
| [livraison.md](livraison.md) | **État livré** (13/11/2026) : les 16 lots, routes, migrations, paramètres, droits, schémas comptables, reprise des données, points ouverts |
| [ecarts.md](ecarts.md) | Les 19 écarts relevés : tous corrigés ou réglés par les lots du plan |
| [realisation.md](realisation.md) | Plan de réalisation : décisions retenues (D1 à D7), ordre des lots, ce que chaque lot change, ses routes et son test |
| [ameliorations.md](ameliorations.md) | Les améliorations demandées : négociation et révision des devis, acomptes du devis à la facture, destinataires et canaux d'envoi, proforma, changement de client, devis avec l'inventaire, saisie d'un montant ventilé par le système, relevé client, catalogue |

## 2. Périmètre

| Domaine | Aujourd'hui | Cible |
| --- | --- | --- |
| Devis | Création, options, signature en ligne, acompte unique, conversion | Négociation avec versions, révision après acceptation, plusieurs acomptes, vérification de l'inventaire, expiration |
| Factures | Création manuelle ou automatique, émission, certification par notre système, paiements, avoirs, rectificatives | Cycle de vie strict, acomptes déduits, changement de client, ventilation d'un total, envois tracés |
| Proforma | Type existant, traité comme une facture | Document sans valeur fiscale, converti une fois en facture |
| Envoi | Courriel à une adresse | Destinataires choisis, copies, préférence par client, courriel et WhatsApp, journal des envois |
| Catalogue | Prix, plancher, marge, remise maximale | Familles, prix par segment et datés, lien à l'inventaire, compte de produit par article, import et export |
| Relevé client et balance âgée | Paginés sans métadonnées, CSV partiel, devises mêlées | Complets, exportables, par devise, tirés de la comptabilité quand elle est active |
| Comptabilité | Écritures automatiques des factures, avoirs, paiements (F4) | Acomptes en `4191`, changement de client par avoir, proforma hors comptabilité |

## 3. Principes

- **Une pièce scellée ne se modifie jamais** : avoir, rectificative ou duplicata.
- **Avant scellement, tout se modifie**, avec une version tracée (auteur, motif) à chaque changement.
- **Un seul calcul** pour la création, la mise à jour et la simulation : mêmes taxes, mêmes exonérations, mêmes contrôles de remise.
- **Une table des transitions par type de document**, appliquée par toutes les routes.
- **Aucune ouverture vers l'extérieur** pour les données de facturation, hors signature de devis à jeton (réponse réduite, écart E4) et webhook WhatsApp signé.
- Messages en français accentué, routes uniques, chaque lot avec ses migrations (dev et prod), ses tests et sa doc backend dans son propre dossier.

## 4. Lots

| Lot | Contenu | Écarts | Critère d'acceptation |
| --- | --- | --- | --- |
| **B0. Sécurité et intégrité** | Signature publique réduite et contrôlée, annulation d'une rectificative payée, exonération au recalcul | E4, E5, E2 | Une signature sur un devis annulé est refusée et ne renvoie aucune note interne ; une rectificative payée s'annule par avoir |
| **B1. Cycle de vie** | Table des transitions, `send` sans changement de statut, `validate` après émission, avoir imputé ou remboursé, statuts EXPIRED, REFUNDED, acompte CANCELLED, auteurs tirés du jeton | E6, E7, E8, E10, E14 | Une facture payée envoyée reste PAID ; un brouillon ne se valide pas sans émission |
| **B2. Négociation des devis** | Modification en négociation, révision après acceptation, frise des versions, contrôles de prix, validité | E9 | Voir [B2](ameliorations.md#b2-négociation-et-révision-des-devis) |
| **B3. Acomptes** | Plusieurs acomptes, factures d'acompte, acompte demandé sur la facture, déduction, `4191` | | Voir [B3](ameliorations.md#b3-acomptes-du-devis-à-la-facture) |
| **B4. Envois** | Destinataires, copies, préférences, WhatsApp, journal des envois, duplicata | E15 | Voir [B4](ameliorations.md#b4-envoi--destinataires-canaux-traces) |
| **B5. Proforma** | Document sans valeur fiscale, conversion unique en facture | | Voir [B5](ameliorations.md#b5-facture-proforma) |
| **B6. Changement de client** | Libre avant scellement ; avoir et nouvelle facture après | | Voir [B6](ameliorations.md#b6-changement-de-client-sur-une-facture) |
| **B7. Devis et inventaire** | Lignes d'article, disponibilités, réservation et sortie de stock, ressources | | Voir [B7](ameliorations.md#b7-devis-et-inventaire) |
| **B8. Montant ventilé** | Saisie d'un total, ventilation des taxes et des lignes, remise implicite, simulation fidèle | E17 | Voir [B8](ameliorations.md#b8-saisie-dun-montant-et-ventilation-par-le-système) |
| **B9. États clients** | Relevé complet et exportable, balance âgée par devise, échéanciers | E11, E12, E13 | Voir [B9](ameliorations.md#b9-relevé-client-balance-âgée-et-échéanciers) |
| **B10. Catalogue, routes, messages** | Catalogue complet, routes uniques, messages | E18, E19 | Voir [B10](ameliorations.md#b10-catalogue-routes-et-messages) |
| **B11. Affaires** | Regroupement des devis, proformas, acomptes, factures et paiements par affaire, tableau de l'affaire | | Voir [B11](ameliorations.md#b11-gestion-des-affaires) |
| **B12. Auteur et visa** | Utilisateur connecté dans « Établi par » et dans le visa | E14 | Voir [B12](ameliorations.md#b12-auteur-de-la-pièce-et-visa) |
| **B13. Simulation interactive** | Calcul ligne par ligne à chaque saisie, descriptions longues | E17 | Voir [B13](ameliorations.md#b13-simulation-interactive-et-descriptions) |
| **B14. Facture modifiable avant paiement** | Versions sans suppression, approbation, rectificative si scellée | | Voir [B14](ameliorations.md#b14-modification-dune-facture-non-payée-avec-versions-et-approbation) |
| **B15. Centres d'approbation** | Centres distincts (facturation, caisse) sur un même modèle : hiérarchie N+1, N+2, circuits, validation en avance par un N+, suivi et historique | | Voir [B15](ameliorations.md#b15-centres-dapprobation-et-hiérarchie) |

Ordre conseillé : B0 et B1 d'abord, puisqu'ils corrigent des défauts existants, dont un de sécurité. Puis B12 et B15, sur lesquels s'appuient les modifications approuvées ; ensuite le cœur du devis et de la facture (B2, B3, B8, B13, B14) ; puis B11, B5, B4, B6 et B7 ; enfin B9 et B10. Les écrans suivent chaque lot, et la doc frontend finale (`docs/final-frontend-doc/billing`) est mise à jour à la fin de chaque lot.

## 5. Décisions

Tranchées le 13/11/2026 : voir [realisation.md](realisation.md#1-décisions-retenues). Le tableau ci-dessous garde les propositions d'origine.

| N° | Sujet | Question | Proposition |
| --- | --- | --- | --- |
| D1 | Révision d'un devis accepté | Une révision après acceptation est-elle admise, ou faut-il un nouveau devis ? | Admise : retour en NEGOTIATION, signature annulée, nouvelle acceptation |
| D2 | Paiement sur proforma | Un paiement total convertit-il la proforma en facture, ou crée-t-il une facture d'acompte ? | Paiement total : conversion ; paiement partiel : facture d'acompte |
| D3 | TVA sur les acomptes | Exigible à l'encaissement de l'acompte, ou à la facture finale ? | **Validé par l'exploitant le 13/11/2026** : TVA portée par la facture finale ; en régime des encaissements, exigible au fur et à mesure que les acomptes sont déduits de la facture finale, pas à leur encaissement |
| D4 | Acompte d'un devis refusé | Remboursé, ou gardé en avoir ? | Au choix de l'utilisateur, avec motif |
| D5 | Stock insuffisant sur un devis | Avertissement ou blocage ? | Avertissement sur le devis, blocage à la facturation (paramètre) |
| D6 | Changement de client d'une facture payée | Refusé, ou paiements reportés en avoir ? | Paiements du client d'origine reportés en avoir à rembourser ou imputer |
| D7 | Statut VOIDED | Le garder (distinct de CANCELLED), ou le retirer ? | Le retirer : CANCELLED suffit, l'avoir porte la trace |
| D8 | Facture scellée non payée modifiée | Nouvelle version de la même pièce, ou facture rectificative ? | Rectificative, montrée comme nouvelle version : une pièce certifiée par notre système ne change pas |
