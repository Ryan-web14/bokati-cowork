# Pièces comptables émises par le logiciel

Les pièces sont produites en PDF (Thymeleaf vers OpenHtmlToPDF, comme la facture et le reçu) et rangées dans la GED. Aujourd'hui existent : `cash/movement-document.html` (bons d'entrée et de sortie), `cash/request-document.html` (demande), `cash/session-closing-report.html` (clôture), `payment/receipt.html` (reçu). Ce document fixe ce que chaque pièce doit contenir après la refonte.

## Règles communes à toutes les pièces

| Élément | Règle |
| --- | --- |
| En-tête | Logo, raison sociale et identifiants de l'exploitant (RCCM, NIU, adresse), organisation (`business_entity`) si plusieurs |
| Titre | Nom de la pièce en grand (« BON DE SORTIE DE CAISSE ») |
| Numéro | Séquence continue **sans trou** par type, organisation et exercice, attribuée à l'émission dans la même transaction, jamais modifiée ni réutilisée ; une pièce abandonnée reste dans la séquence avec le statut ANNULÉ (voir [README, section 4.8](README.md#48-numérotation-des-pièces--continuité-et-protection)) ; préfixes proposés ci-dessous |
| Dates | Date de la pièce, date et heure d'émission, date comptable si différente |
| Caisse et session | Code et nom de la caisse, numéro de session, caissière |
| Montant | En chiffres et **en lettres** (formateur existant `inWords` du reçu), devise |
| Imputation | Journal, numéro de pièce comptable, comptes au débit et au crédit ; sections analytiques si activées |
| Références | Demande, état de besoin, facture, fournisseur, mission selon le cas |
| Budget (si activé) | Poste budgétaire, disponible avant et après ; « Dépense prévue : réf. de la prévision » ou « Dépense non prévue : motif » ; mention de dérogation le cas échéant |
| Signatures | Cases nommées selon la pièce ; signature électronique horodatée si faite à l'écran, sinon case vide à signer |
| Vérification | QR vers la page publique de vérification (module `verify`) : numéro, montant, date, statut actuel |
| Réimpression | Mention « DUPLICATA » avec date de réimpression ; chaque impression est tracée (`printedAt` existe sur le mouvement) |
| Annulation | Une pièce ne se modifie pas ; une pièce d'annulation liée est émise et l'originale porte « ANNULÉE PAR ... » à la réimpression |
| Pied | Mentions de conservation, page x/y, identifiant de génération |

## 1. Bon d'entrée de caisse (`BEC-`)

Pour toute entrée hors encaissement client : approvisionnement, retour de reliquat, dépôt de garantie, produit divers.

| Champ | Automatique |
| --- | --- |
| Numéro, date, caisse, session, caissière | Oui |
| Nature d'opération et famille | Choisie ; libellé en clair |
| Remettant : nom, qualité (employé, client, tiers), pièce d'identité, téléphone | Prérempli si employé ou client connu |
| Motif | Prérempli depuis la nature ou la demande |
| Montant en chiffres et en lettres | Lettres automatiques |
| Billetage reçu (facultatif) | Saisi |
| Référence d'origine (avance `DDF-`, transfert, contrat de caution) | Oui si l'entrée en découle |
| Journal, comptes, analytique | Oui |
| Signatures | Remettant, caissière |

## 2. Bon de sortie de caisse (`BSC-`)

Pour une dépense directe sur pièce, un paiement fournisseur, une restitution de caution.

| Champ | Automatique |
| --- | --- |
| Numéro, date, caisse, session, caissière | Oui |
| Nature d'opération | Choisie |
| Bénéficiaire : nom, qualité, pièce d'identité, téléphone | Prérempli si employé ou fournisseur connu |
| Fournisseur, numéro de sa facture | Saisi ou depuis la facture fournisseur |
| Détail : désignation, montant HT, TVA, TTC | TVA calculée selon la nature |
| Montant en chiffres et en lettres | Oui |
| Demande d'origine et ses validations (noms, dates) | Oui si issue d'une demande |
| Journal, comptes, analytique | Oui |
| Pièce justificative jointe : oui/non, référence GED | Oui |
| Signatures | Bénéficiaire (« reçu la somme de »), caissière, approbateur si dépense sans demande au-delà d'un seuil |

## 3. Bon de décaissement de fonds (`BDF-`)

Pièce du décaissement d'une **demande de fonds** approuvée, notamment une avance à justifier. Distincte du bon de sortie parce qu'elle engage le bénéficiaire à justifier.

| Champ | Automatique |
| --- | --- |
| Numéro, date, caisse, session, caissière | Oui |
| Demande `DDF-`, type, objet | Oui |
| Demandeur et bénéficiaire, service | Oui |
| Lignes de la demande (nature, désignation, montant) | Oui |
| Montant approuvé, montant décaissé, déjà décaissé, reste à décaisser | Oui |
| Montant décaissé en lettres | Oui |
| Circuit de validation : niveau, nom, décision, date | Oui |
| Pour une avance : date limite de justification, mention d'engagement (« Je m'engage à justifier cette somme avant le ... ou à la restituer ») | Oui |
| Mission : destination, dates | Oui si mission |
| Journal, comptes (`421` du bénéficiaire, caisse), analytique | Oui |
| Signatures | Bénéficiaire, caissière |

## 4. État de besoin (`EDB-`)

| Champ | Automatique |
| --- | --- |
| Numéro, date | Oui |
| Demandeur, service, fonction, site, centre de coût | Oui |
| Objet, urgence, date souhaitée | Saisi |
| Tableau : n°, désignation, quantité, unité, prix unitaire estimé, total, nature, fournisseur suggéré | Totaux automatiques |
| Total général en chiffres et en lettres | Oui |
| Budget de la section : alloué, engagé, restant après ce besoin | Oui si budget activé |
| Justification du besoin | Saisi |
| Validations : niveau, nom, décision, date | Oui |
| Suite donnée : demande `DDF-`, commande, petite caisse | Oui une fois transformé |
| Signatures | Demandeur, chef de service, responsable financier |

## 5. Demande de fonds (`DDF-`)

Remplace l'actuel `cash/request-document.html`.

| Champ | Automatique |
| --- | --- |
| Numéro, date, type de demande | Oui |
| Demandeur, bénéficiaire, service | Oui |
| Objet, lignes, total en lettres | Totaux automatiques |
| Caisse de remise, mode de remise, date souhaitée | Oui |
| Budget, avances non soldées du bénéficiaire au jour de la demande | Oui |
| Circuit de validation (niveaux requis et décisions) | Oui |
| État de besoin d'origine | Oui |
| Signatures | Demandeur, valideurs |

## 6. Fiche de justification d'avance (`JUS-`)

| Champ | Automatique |
| --- | --- |
| Numéro, avance `DDF-` et bon `BDF-` d'origine | Oui |
| Bénéficiaire, mission ou objet, dates | Oui |
| Tableau : date, fournisseur, nature, désignation, HT, TVA, TTC, pièce n°, statut de validation | Totaux automatiques |
| Avance reçue, total justifié, total rejeté, reliquat à rendre ou complément dû | Oui |
| Bon d'entrée du reliquat ou bon de décaissement du complément | Oui une fois émis |
| Écritures produites (numéros de pièce) | Oui |
| Signatures | Bénéficiaire, comptable valideur |
| Annexe | Copies des justificatifs, une par page |

## 7. Procès-verbal de billetage (`PVB-`)

À l'ouverture, à la clôture, au recomptage.

| Champ | Automatique |
| --- | --- |
| Caisse, session, moment (ouverture, clôture, recomptage), date et heure | Oui |
| Tableau par coupure : valeur, quantité, montant | Montants automatiques |
| Total compté, total attendu, écart | Oui ; l'attendu masqué à la saisie si clôture à l'aveugle |
| Paiements hors espèces à remettre (chèques, mobile money marchand) | Oui (`nonCashPayments`) |
| Compteurs (1 ou 2 en double comptage) | Oui |
| Signatures | Caissière, second compteur ou responsable |

## 8. Procès-verbal de passation de caisse (`PVP-`)

| Champ | Automatique |
| --- | --- |
| Caisse, session cédante, session reprenante, date et heure | Oui |
| Billetage contradictoire | Oui |
| Chèques et pièces en instance remis | Oui |
| Demandes approuvées restant à décaisser | Oui |
| Avances ouvertes décaissées par la session cédante | Oui |
| Écart constaté et commentaire | Oui |
| Signatures | Caissière sortante, caissière entrante, témoin facultatif |

## 9. Ordre de transfert interne (`OTI-`)

Entre caisses, vers le coffre, vers ou depuis la banque.

| Champ | Automatique |
| --- | --- |
| Caisse ou compte émetteur, récepteur | Oui |
| Montant, billetage remis | Oui |
| Demande d'approvisionnement d'origine | Oui |
| Statut : émis, reçu (avec date de réception) | Oui |
| Signatures | Émetteur, transporteur si différent, récepteur |

## 10. Bordereau de remise en banque (`BRB-`)

| Champ | Automatique |
| --- | --- |
| Banque, compte, caisse d'origine | Oui |
| Espèces : billetage, total | Oui |
| Chèques : numéro, banque tirée, tireur, montant, facture réglée | Depuis les chèques `RECEIVED` sélectionnés |
| Total général en lettres | Oui |
| Statut : préparé, déposé, crédité (date du relevé) | Oui |
| Signatures | Caissière, déposant ; cachet de la banque sur la copie scannée |

## 11. Rapport de clôture de session

Existe déjà (`cash/session-closing-report.html`). Ajouter :

- le billetage ;
- les paiements hors espèces par moyen ;
- les demandes décaissées et les avances ouvertes ;
- les écritures produites, avec le solde comptable de la caisse comparé au solde compté ;
- l'écart et sa décision (charge, produit, imputation au caissier).

## 12. Reçu de paiement

Existe (`payment/receipt.html`). Ajouter le numéro de pièce comptable de l'encaissement, en petit, pour l'audit.

## Préfixes proposés

| Pièce | Préfixe |
| --- | --- |
| Bon d'entrée de caisse | `BEC` |
| Bon de sortie de caisse | `BSC` |
| Bon de décaissement de fonds | `BDF` |
| État de besoin | `EDB` |
| Demande de fonds | `DDF` |
| Fiche de justification | `JUS` |
| PV de billetage | `PVB` |
| PV de passation | `PVP` |
| Ordre de transfert interne | `OTI` |
| Bordereau de remise en banque | `BRB` |
| PV de remise de caisse (changement de titulaire) | `PRC` |
| PV de clôture d'office | `PCO` |
| PV de mise sous scellés et d'ouverture du scellé | `PVS` |
| PV de comptage inopiné | `PCI` |
| PV de revue d'écart | `PRE` |

Les PV de remise de caisse, de clôture d'office, de mise sous scellés, de comptage inopiné et de revue d'écart suivent le modèle du PV de passation (section 8) : caisse, date, comptage par coupure, attendu, écart, motif choisi dans une liste, signatures des deux parties (ou du superviseur et du témoin) ; voir [caisses-et-sessions.md](caisses-et-sessions.md) sections 4.5, 5.6 et 6.1.

Format : `{PREFIX}-{AAAA}{MM}-{SEQ}`, séquence sur 6 chiffres. La continuité est contrôlée chaque nuit et figure dans l'état de continuité des numéros.

Le moteur `core/generator` attribue ses numéros dans une transaction séparée (`REQUIRES_NEW`) : une émission annulée y laisse un trou. Pour ces pièces, la numérotation passe donc par un compteur transactionnel, comme les pièces comptables.
