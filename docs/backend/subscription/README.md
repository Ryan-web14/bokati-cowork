# Plan du module subscription (abonnements, pass, droits, promotions)

> Chantier ouvert le 19/11/2026 sur `feature/subscription` (depuis `new-main` 5a054625). Audit en trois parties : cœur et cycle de vie (écarts de la doc frontend, à compléter par l'audit en cours), sièges, droits et consommation, moteur de promotions et coupons. Une page par lot, à mesure. Décisions à valider avant l'implémentation (section [Décisions](#décisions)).

## Demande

1. Revue entière du module : plus efficace, complet, répondant à tous les besoins ; corriger tous les écarts.
2. **Moteur de promotions et coupons** revu : les voir, les envoyer, les publier, les attribuer, les encaisser, les lier à des produits, leurs effets.
3. **Correction du prix à l'activation** d'un abonnement qui ne passe pas par un devis.
4. **Sièges** d'un abonnement d'entreprise : assignation par l'entreprise.
5. **Avantages et droits** définis pour chaque offre (et chaque siège).
6. Une vingtaine de **fonctions nouvelles**.
7. Garder et fiabiliser toutes les fonctions existantes.

## Constat en bref

| Domaine | État |
| --- | --- |
| Cycle de vie | 23 écarts documentés (README frontend), dont 21 encore ouverts : reprise après pause inversée, droits non fermés à la suspension, activation et annulation sans garde de statut, clôtures automatiques incomplètes, statuts jamais atteints (essai, impayé), demande de changement qui ne fait rien pour les options |
| Prix | Aucune correction possible à l'activation sans devis ; devis négocié : première facture au prix catalogue |
| Promotions | **Le moteur ne fonctionne pas par l'API** : aucune route n'écrit récompenses, conditions, audiences ni listes de prix ; toute promotion créée est rejetée (« Cette promotion n'accorde rien ») |
| Coupons | Lots de codes aléatoires seulement ; pas de code nommé, pas d'attribution à un client, pas d'envoi, pas de « mes coupons » ; un coupon réservé au panier est ensuite refusé au paiement ; un coupon invalide est ignoré en silence (plein tarif) |
| Effets | Seuls pourcentage, montant fixe et frais d'installation offerts s'appliquent ; mois offerts, essai, heures offertes, option offerte, montée en gamme : calculés puis jamais appliqués |
| Sièges | Deux API sur la même table : l'ancienne contourne plafond, lien à l'entreprise et statut ; pas d'invitation, pas de réaffectation ; statuts `INVITED`, `SUSPENDED` jamais posés |
| Droits | `resetPolicy`, `consumptionMode`, `stackable`, `restrictionsJson` déclarés jamais appliqués ; crédits (`CREDIT`) inutilisables en réservation ; couverture partielle fausse ; domiciliation et impression ne consomment pas les droits ; une option n'accorde aucun droit |
| Dépassement | Franchise (`freeQuantity`) déduite à chaque usage au lieu d'une fois par période ; annuler une réservation en dépassement rend des heures gratuites |
| Sécurité | `/entitlements` et `/internal/entitlements/{consume,refund}` ouverts à tout agent ; contexte client (segment, ancienneté, prix unitaire) fourni par le client au portail |
| Forme | Messages en anglais, listes non paginées, requêtes répétées dans le partage d'entreprise, lectures qui écrivent |

Détail des preuves : [ecarts.md](ecarts.md).

## Lots proposés

| Lot | Contenu |
| --- | --- |
| **S0** Sécurité, socle, forme | Droits des routes (`/entitlements`, `/internal/entitlements`, `/pricing`, sièges) ; ancienne API des sièges branchée sur les contrôles du partage d'entreprise ; messages en français ; pagination ; `@Audited` ; identifiants texte ; listes de choix (motifs) sur le socle L0 |
| **S1** Cycle de vie | Gardes de statut (activer, annuler, retirer un préavis) ; pause et reprise justes, reprise automatique ; suspension et pause ferment les droits ; clôtures automatiques complètes (droits, contrat, dépôt) ; essai `TRIALING` et impayé `PAST_DUE` réels ; renouvellement payé d'avance ; dernier jour ; courriels |
| **S2** Prix à l'activation | Correction du prix sans devis (prix ou remise, motif, durée de la correction, plafond, approbation) ; devis négocié facturé au bon prix dès la première facture ; prix garanti au renouvellement |
| **S3** Avantages et droits | Catalogue d'avantages et de droits par offre ; surcharge par abonnement sans dérivation ; règles de remise à zéro, report, cumul appliquées ; crédits utilisables ; couverture partielle juste ; domiciliation consomme les droits ; options qui accordent des droits ; dépassement corrigé |
| **S4** Sièges d'entreprise | Une seule API ; invitations par le gestionnaire ; réaffectation avec le reste du quota ; rôles du siège ; ressources et horaires par siège ; siège suspendu ; siège ajouté en cours de période facturé au prorata |
| **S5** Moteur de promotions | Configuration complète par l'API (récompenses, conditions, audiences, listes de prix) ; durée d'une remise (N premières périodes) ; cumul corrigé ; budget et limites par client ; approbation ; contexte client lu par le serveur ; annulation qui rend budget et usage ; trace sur la facture |
| **S6** Coupons | Codes nommés, attribués à un client ou un segment ; envoi par courriel (et WhatsApp après approbation d'un modèle) ; publication au portail ; « Mes coupons » ; encaissement partout (abonnement, pass, réservation au portail, facture) ; liens aux produits ; effets non monétaires appliqués ; refus clairs ; statistiques et comptabilité |
| **S7** Fonctions nouvelles | Selon la sélection (voir ci-dessous) |
| **S8** Pass | Pass utilisé qui expire et se renouvelle ; renouvellement après paiement avec ses droits ; transfert avec droits et frais ; plans de pass versionnés ; verrou d'usage |
| **S9** Clôture | Doc frontend, routes, réglages, actions de l'exploitant, tests d'intégration |

## Fonctions nouvelles proposées (24)

| N° | Fonction | Pour qui |
| --- | --- | --- |
| N1 | Essai gratuit avec conversion automatique en payant (ou fin sans frais) | Vente |
| N2 | Engagement 3, 6, 12 mois avec prix dégressif et pénalité de sortie | Vente |
| N3 | Montée ou descente de gamme au portail avec aperçu du prorata | Client |
| N4 | Pause « vacances » au portail, dans les limites fixées par l'offre | Client |
| N5 | Relances d'impayés et suspension progressive (rappels J+1, J+7, suspension J+15, réglables) | Recouvrement |
| N6 | Renouvellement payé automatiquement par le portefeuille ou le mobile money | Client, trésorerie |
| N7 | Recharges : packs d'heures ou de crédits achetés en un clic quand le quota s'épuise | Client |
| N8 | Alertes de consommation (80 %, 100 %) au client et au gestionnaire de l'entreprise | Client, entreprise |
| N9 | Abonnement offert (carte cadeau) : achat par un tiers, activation par code | Vente |
| N10 | Offre privée : plan sur mesure visible d'un seul client ou d'une seule entreprise | Vente |
| N11 | Hausse de prix planifiée d'une offre avec préavis aux abonnés et prix garanti aux anciens | Direction |
| N12 | Reconquête : à la résiliation, enquête de départ (motifs en liste) et offre de rétention automatique | Rétention |
| N13 | Fidélité : remise automatique selon l'ancienneté (paliers réglables) | Rétention |
| N14 | Coupons de parrainage générés et envoyés automatiquement au parrain et au filleul | Croissance |
| N15 | Campagnes : envoi groupé de coupons à un segment, suivi ouverture et usage | Marketing |
| N16 | Tableau de bord : revenu mensuel récurrent, attrition, valeur client, cohortes, coupons | Direction |
| N17 | Prévision : renouvellements et fins d'engagement à venir, revenu attendu | Direction |
| N18 | Factures groupées d'une entreprise (tous ses abonnements et sièges sur une facture) | Entreprise |
| N19 | Accès physique lié au statut : badge ou QR coupé à la suspension, rouvert au paiement | Accueil |
| N20 | Contrat ou conditions signés à l'activation, sans quoi l'abonnement reste en attente | Juridique |
| N21 | Changement d'offre planifié à une date (fin d'engagement, début de mois) | Client, équipe |
| N22 | Suggestions d'options selon l'usage (salle souvent réservée, impression fréquente) | Vente |
| N23 | Simulateur de devis au portail : composer offre, sièges, options, coupon, voir le prix | Client |
| N24 | Relevé de consommation mensuel par abonnement et par siège (PDF) | Entreprise |

## Décisions (19/11/2026)

| N° | Question | Décision |
| --- | --- | --- |
| D1 | Correction du prix à l'activation | **Au choix à chaque fois** : première période, N périodes ou toute la durée ; motif obligatoire (liste de choix) ; plafond sans approbation réglable (20 % par défaut, comme booking) ; au-delà, approbation N+1 par le moteur d'approbation |
| D2 | Envoi des coupons | **Courriel, WhatsApp, portail « Mes coupons »** ; les canaux se choisissent dans l'application à la création du lot, puis à tout moment ensuite (WhatsApp : modèle à faire approuver par Meta, soumis seulement avec l'accord de l'exploitant) |
| D3 | Publication | **Portail seulement**, selon l'audience ; rien d'ouvert à l'extérieur |
| D4 | Remise sur la facture | **Ligne de remise visible** avec son origine (coupon, promotion, prix corrigé), compte de contrepartie en comptabilité |
| D5 | Siège ajouté en cours de période | **Prorata immédiat** ; au retrait, avoir du prorata ou rien, au choix de l'équipe |
| D6 | Fonctions nouvelles | L'exploitant choisit dans la liste N1 à N24 (en attente) |
