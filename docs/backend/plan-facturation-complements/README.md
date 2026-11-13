# Plan facturation : compléments

Suite de la refonte devis et facturation (lots B0 à B15, fusionnée dans new-main le 13/11/2026, voir [../plan-facturation-devis/livraison.md](../plan-facturation-devis/livraison.md)).

Demandé par l'exploitant le 13/11/2026 : terminer ce que la refonte a laissé à moitié, rendre le catalogue pilotable par des listes de référence tenues dans le backend (familles, sous-familles, unités, taxes), puis ajouter les fonctions qui manquent pour un module de facturation complet. Tout est à implémenter, lot par lot, sur `feature/facturation-complements`.

## 1. Sommaire

| Fichier | Contenu |
| --- | --- |
| [livraison.md](livraison.md) | **État livré**, lot par lot (C0 à C16, tous livrés) |
| [lots.md](lots.md) | Détail de chaque lot : ce qui change, tables, routes, paramètres, comptabilité, critère d'acceptation, tests |
| [decisions.md](decisions.md) | Les décisions à trancher (CD1 à CD9), chacune avec la recommandation retenue par défaut |

## 2. Point de départ (constaté dans le code de new-main)

| Sujet | Aujourd'hui | Conséquence |
| --- | --- | --- |
| Famille, sous-famille, unité d'un article du catalogue | Texte libre (`service_catalog_item.category`, `subcategory`, `unit`) | Le frontend code ses listes en dur, les fautes de frappe créent des familles fantômes, aucun ajout possible sans développeur |
| Taxe d'un article | `tax_rule_code` enregistré mais jamais lu ; le calcul prend toujours le profil par défaut (`BillingTaxRuleResolver.defaultTaxProfile`) | Un article exonéré ou à taux réduit se facture quand même au taux normal |
| Règles de taxe | Table `billing_tax_rule` sans aucune route | Un nouveau taux exige une migration |
| WhatsApp | Deux modèles seulement : facture payée, rappel de paiement | Un devis ou une proforma envoyé par WhatsApp part avec le texte d'une facture à régler |
| Dérogation de remise | Circuit `BILLING_DISCOUNT_OVERRIDE` créé, jamais appelé | Une remise hors limites passe par un droit et un motif, sans approbation |
| Sortie de stock | `billing.stock.issue-on` vaut `INVOICE` ou `NONE` | Pas de livraison partielle, stock sorti à la facture et non au départ de la marchandise |
| Journal des envois | Courriel noté « en file » | Le statut réel (remis, rejeté) reste dans le journal des courriels du socle |
| Caution | Facturée comme une ligne d'abonnement, rubrique de sortie `DEPOSIT_SETTLEMENT` | Pas de suivi en `165`, ni de restitution ou de retenue tracée |
| Litige | Lot F19 : une facture contestée suspend les relances (`ReceivableRiskService.disputed`) | Pas de réclamation par ligne, ni de règlement par avoir ou refus motivé |
| Retenue à la source | Gérée sur les factures fournisseurs (`AccSupplierInvoice.withholdingAmount`) | Rien côté clients : une facture payée sous retenue reste ouverte |

## 3. Principes

- **Rien de cassant.** Les colonnes texte du catalogue restent, les routes existantes répondent pareil, avec des champs en plus. Les valeurs déjà saisies sont reprises dans les listes par la migration ; un ancien écran qui envoie une valeur inconnue continue de fonctionner (décision CD2).
- **Le frontend lit les listes dans le backend** : une route unique renvoie toutes les listes du catalogue pour remplir un formulaire, plus une route de gestion par liste. Plus aucune liste codée en dur dans les écrans.
- **Un code stable, un libellé modifiable.** Renommer une famille ne touche pas les articles. Une entrée utilisée ne se supprime pas, elle se désactive : elle disparaît des choix, reste lisible sur ce qui l'emploie.
- **Un taux de taxe ne se modifie pas, il se remplace** : l'ancien prend une date de fin, le nouveau une date de début. Les pièces émises gardent leur taux.
- Les règles de la refonte restent : pièce scellée jamais modifiée, un seul calcul pour création, mise à jour et simulation, aucune ouverture vers l'extérieur (l'espace client est authentifié, pas de lien à jeton), messages en français accentué avec un code stable, « facture certifiée par notre système ».
- Chaque lot : migrations dev et prod, paramètres dans `platform-settings.tsv`, tests, doc backend dans ce dossier. La doc frontend finale (`docs/final-frontend-doc/billing`) est mise à jour à la fin, avec les routes régénérées.

## 4. Lots

| Lot | Contenu | Critère d'acceptation |
| --- | --- | --- |
| **C0. Listes de référence du catalogue** | Familles, sous-familles, unités, règles de taxe : tables, reprise des valeurs existantes, routes de lecture et de gestion, contrôle à l'enregistrement d'un article | Le formulaire d'article se remplit avec une seule route ; une famille ajoutée par l'écran apparaît aussitôt ; un ancien écran qui envoie « Salles » fonctionne encore |
| **C1. Taxe de l'article appliquée** | La règle de taxe de l'article pilote le calcul de la ligne (normal, réduit, exonéré, hors champ), mention d'exonération sur le PDF | Un article exonéré produit une ligne sans TVA, en création, mise à jour et simulation |
| **C2. WhatsApp pour devis et proforma** | Modèles par type de pièce, refus clair si le modèle n'est pas configuré | Un devis envoyé par WhatsApp part avec le modèle « devis disponible », jamais « facture à régler » |
| **C3. Dérogation de remise approuvée** | Remise ou prix hors limites : demande au N+1 par le centre de facturation, émission bloquée jusqu'à l'accord | Une remise de 30 % sur un article plafonné à 10 % attend l'accord ; refusée, la pièce reste en brouillon avec le motif |
| **C4. Bon de livraison** | Pièce non fiscale tirée d'un devis accepté ou d'une facture, livraisons partielles, reste à livrer, sortie de stock à la livraison | Deux livraisons de 3 puis 2 chaises sur une commande de 5 ; stock sorti à chaque bon |
| **C5. Statut réel des courriels** | Le journal des envois reprend remis, rejeté, rebond depuis le journal des courriels | Un envoi à une adresse fausse apparaît « rejeté » avec la raison |
| **C6. Retenues à la source** | Liste des retenues, saisie à l'encaissement, attestation jointe, écriture comptable, facture soldée | Une facture de 1 180 000 payée 1 000 000 avec une retenue de TVA de 180 000 est soldée |
| **C7. Dépôts de garantie** | Encaissement en `165`, restitution, retenue motivée, imputation sur la dernière facture, lien avec la sortie d'abonnement | Une caution de 200 000 retenue de 50 000 pour dégâts : 150 000 restitués, 50 000 en produit, écritures justes |
| **C8. Tarifs négociés par client** | Prix ou remise propre à une organisation ou un membre, daté, prioritaire sur le segment | La simulation d'un devis pour le client sous contrat affiche son prix et la source « contrat » |
| **C9. Paiements non affectés** | Propositions d'affectation des sommes en attente aux factures ouvertes, affectation en un geste | Un virement de 590 000 sans référence est proposé sur la seule facture ouverte de 590 000 du même client |
| **C10. Débours** | Type de ligne hors TVA et hors chiffre d'affaires, compte de tiers dédié, rubrique à part sur le PDF | Une facture avec 25 000 de débours : TVA calculée sans eux, produit sans eux |
| **C11. Réclamations** | Réclamation par ligne (back-office et espace client), litige, règlement par avoir ou refus motivé | Une réclamation acceptée produit l'avoir de la ligne et clôt le litige ; les relances reprennent |
| **C12. Recouvrement** | Promesse de paiement, plan de règlement négocié, palier de relance par WhatsApp | Une promesse au 30/11 suspend les relances jusqu'au 30/11 puis les relance si rien n'est arrivé |
| **C13. Facturation groupée périodique** | Facture mensuelle par client reprenant ses consommations, lancement en lot avec aperçu | Un lancement de novembre produit une facture par client, un second lancement n'en crée aucune de plus |
| **C14. Devis types et lots d'articles** | Modèles de devis, articles composés vendus à prix global | « Séminaire journée » crée un devis de 4 lignes ; un lot vendu sort le stock de ses composants |
| **C15. Tableau de bord commercial** | Transformation des devis, délai moyen de paiement, prévision d'encaissement, affaires par étape | Les indicateurs d'un mois se recoupent avec la liste des pièces du mois |
| **C16. Espace client** | Accepter et signer un devis, payer une proforma ou une facture en mobile money, relevé, réclamation | Un membre accepte son devis depuis son espace, l'acompte payé par PawaPay est imputé |

## 5. Ordre de réalisation

1. **C0 puis C1** : le catalogue d'abord, puisque le frontend doit lire ses listes et que la taxe par article s'appuie sur la liste des taxes.
2. **C2, C3, C5** : les finitions de la refonte.
3. **C4, C6, C7, C8, C9, C10** : justesse des pièces, du stock et des soldes clients.
4. **C11, C12** : litiges et recouvrement.
5. **C13, C14, C15, C16** : productivité, pilotage, espace client (C16 s'appuie sur C11 et sur les envois).

## 6. Migrations

Prochains numéros au départ : dev `V338`, prod `V332`. Un fichier par lot dans chaque dossier, numéros réservés au moment du lot.
