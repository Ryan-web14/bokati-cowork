# Ressources : plan d'amélioration et règlement des écarts

Branche `feature/ressources`, partie de new-main f33d6089 le 17/11/2026. Plan à valider : les décisions de l'exploitant sont plus bas, à trancher avant le lot concerné.

État : plan validé le 18/11/2026 (décisions ci-dessous) ; **R0 fait** ([lot-r0-securite-acces.md](lot-r0-securite-acces.md)), **R1 fait** ([lot-r1-moteur-prix.md](lot-r1-moteur-prix.md)), **R2 fait** ([lot-r2-politiques-cycle-de-vie.md](lot-r2-politiques-cycle-de-vie.md)), **R3 fait** ([lot-r3-creneaux-horizon.md](lot-r3-creneaux-horizon.md)), **R4 fait** ([lot-r4-listes-recherche.md](lot-r4-listes-recherche.md)), **R6 fait** ([lot-r6-messages.md](lot-r6-messages.md)), **R5a fait** ([lot-r5a-calendrier.md](lot-r5a-calendrier.md)).

## Constat (vérifié dans le code le 17/11/2026)

Les 16 écarts relevés par la doc frontend (`docs/final-frontend-doc/ressource/README.md`) sont tous confirmés. La vérification en ajoute 16, dont 4 graves :

- **Trois sources de prix, pas deux.**
  - Réservation : `BookingPricingCalculator` prend la dernière règle active de chaque unité, avec demi-journée = 5 h et journée = 10 h.
  - Devis et aperçu groupé : `ResourcePricingRuleServiceImpl` appliquent jour, heures, validité, ajustement et priorité, avec demi-journée = 12 h et journée = 24 h, et ignorent la quantité.
  - Catalogue de facturation : `ServiceCatalogServiceImpl:464` prend la dernière règle active, toutes unités confondues.
  
  Le portail affiche le devis, mais la facture vient du calcul de la réservation. Ce prix est fixé une seule fois, à la création.
- **Pas de nouveau calcul de prix** quand on change l'horaire ou la ressource d'une réservation (`BookingServiceImpl.java:648-690`).
- **Horizon de réservation instable** : les créneaux ne sont générés que jusqu'à la fin du mois courant, et la création manuelle est limitée à un mois. Le 28, on réserve au plus trois jours à l'avance.
- **Calendrier public** sans authentification, sans contrôle de visibilité ni d'état, et sans borne de plage : on peut énumérer les ressources et saturer le serveur.

## Lots proposés

| Lot | Contenu | Écarts |
| --- | --- | --- |
| **R0. Sécurité et accès** | Calendrier public : seulement une ressource visible au portail, active et réservable ; plage bornée (92 jours, l'horizon de 90 jours tient dans une demande) ; dates inversées refusées ; jour sans créneau marqué `CLOSED` et non plus `FULL`. Même borne pour le calendrier de l'équipe. `POST /resources/search` en lecture (`RESOURCE:READ`). Photos : la photo doit appartenir à la ressource du chemin, le document doit exister dans la GED, une seule couverture, lecture de la galerie en `RESOURCE:READ`. Horaires d'ouverture ouverts à `RESOURCE:UPDATE` (plus seulement le rôle ADMIN). Journal (`@Audited`) sur les traitements groupés, les photos et les horaires. | 2 (droit), 8, 13, Rn-7, Rn-8, Rn-9 |
| **R1. Un seul moteur de prix** | `ResourcePriceEngine` dans le module ressource, appelé par la réservation, le devis, l'aperçu groupé, le portail et le catalogue. Il applique jour, heures, validité, ajustement (pourcentage en décimal), priorité, dernier moment et quantité. Durées de demi-journée et de journée en paramètres (décision D2). Le prix d'une réservation est recalculé quand son horaire, sa durée ou sa ressource change (décision D1). Équipements optionnels facturés (`extraPrice`). Modification d'une règle revalidée (jour, heures) avec possibilité de vider un champ ; suppression d'une règle douce et cohérente (champ `deleted` lu partout). Le devis expose le détail : règle retenue, base, ajustements, quantité. | 1, Rn-1, Rn-5, Rn-6, Rn-10, Rn-11 |
| **R2. Politiques et cycle de vie** | Une politique désactivée ne s'applique plus (contrôles des réservations, des créneaux, de l'annulation, du devis) et ne peut plus être rattachée. `allowCancellation` vrai par défaut (décision D3). Suppression d'un groupe, d'un type ou d'une politique refusée en `409` s'il reste des ressources, celles supprimées en douce comprises (fini le 500). Suppression, mise en maintenance ou hors service d'une ressource qui a des réservations à venir : traitement selon la décision D4. Fermeture : liste des réservations touchées dans la réponse, traitement selon D4, `reason` rendu. Étage (`floorLabel`) enregistré. | 4, 5, 6, 7, 12, 15, Rn-4, Rn-12 |
| **R3. Créneaux et horizon** | Horizon glissant paramétrable (`app.resources.booking-horizon-days`, 90 jours par défaut, décision D5) : génération quotidienne jusqu'à l'horizon, et non plus au mois. Création manuelle d'après les horaires d'ouverture de la ressource (fini le 08:00-22:00 en dur). Routes pour désactiver, réactiver et supprimer un créneau sans réservation, ce qui débloque le changement de durée de créneau. `capacity` retiré de la requête, ou appliqué. Expiration des créneaux passés faite par une tâche, plus jamais dans une lecture. `/reserve` et `/release` manuels réservés au rattrapage, liés à une réservation et journalisés (décision D6). | 9, 10, 11, 14, Rn-2, Rn-3 |
| **R4. Listes, recherche, performances** | `GET /resources` filtre par type, groupe et politique, en résumé comme en détail. `POST /resources/search` par codes (`typeCode`, `groupCode`, `policyCode`). `search/basic` paginé. Graphe d'entités sur les listes (fin du N+1). Nouvel essai hors de la transaction compromise. Créations en `201` avec la ressource créée (code compris). Fermetures et créneaux désignés par code et non plus par identifiant numérique. | 2 (filtres), 3, Rn-13, Rn-14, Rn-15, Rn-16 |
| **R5a. Calendrier et règles de réservation** | Battements entre réservations (ménage, installation) ; fenêtre d'avance maximale par politique ; fermetures et maintenances récurrentes (chaque semaine, chaque année) ; blocages typés (maintenance, ménage, événement privé) ; jours fériés du Congo et horaires exceptionnels par date, pris en compte par les créneaux, le prix et le calendrier. | Fait |
| **R5b. Trouver, situer, exporter** | Sites (adresse, horaires, fuseau) et filtrage par site ; étages et plan d'étage par site, position de chaque ressource, disponibilité en direct sur le plan ; « Trouver une salle » par besoin (participants, équipements, créneau, site, budget) avec alternatives ; lien iCal par ressource et invitation `.ics` à la confirmation. | |
| **R5c. Vente** | Codes promo et remises (moteur des promotions des abonnements) ; heures incluses dans un abonnement, consommées avant facturation, solde au portail ; services additionnels (café, traiteur, impression, matériel) facturés avec la réservation ; équipements optionnels facturés ; participants supplémentaires facturables ou non, au choix de l'équipe, par ressource ou par réservation ; caution bloquée puis rendue ou retenue ; frais de dépassement (mesuré au départ) et de non-présentation, selon la politique ; lots de ressources réservées ensemble. | |
| **R5d. Exploitation** | Tâches créées à la réservation dans la liste de tâches de l'équipe (préparer la salle, servir le café, remettre en état), modèles de tâches par ressource ou par service ; signalement d'incident (membre ou équipe) avec blocage éventuel, ticket au support et historique de maintenance ; règlement d'usage accepté à la réservation, liste de vérification et photos de fin d'utilisation. | |
| **R5e. Pilotage** | Coûts par ressource (loyer, ménage, énergie, amortissement du matériel, coût des services) et marge par réservation, par ressource et par période ; historique des prix : versions des règles et prix appliqués à chaque réservation, avec leur trace ; taux d'occupation et chiffre d'affaires par ressource, type, site et période, heures creuses et heures pleines. | |
| **R6. Erreurs et messages** | Environ 165 messages en anglais traduits ; réponses dédiées (409 conflit, 400 nommant le champ). | 16 |
| **R7. Doc et tests** | Doc frontend `ressource/` mise à jour (écarts marqués Réglé, parcours corrigés), routes régénérées ; tests unitaires par lot, tests d'intégration du moteur de prix et des créneaux sur base réelle. | |

Ordre conseillé : R0 (sécurité), R1 (prix, ce qu'on facture), R2, R3, R4, R6, puis R5a, R5b, R5c, R5d, R5e ; R7 au fil de l'eau.

## Décisions de l'exploitant (17/11/2026)

| N° | Sujet | Décision |
| --- | --- | --- |
| D1 | Prix d'une réservation dont l'horaire, la durée ou la ressource change | **Recalcul** ; si la réservation est déjà facturée, facture complémentaire pour une hausse, avoir pour une baisse |
| D2 | Durées de la demi-journée et de la journée | **5 h et 10 h** (valeurs facturées aujourd'hui), en paramètres |
| D3 | Annulation par défaut d'une nouvelle politique | Permise (`allowCancellation = true`), défaut proposé, non contesté |
| D4 | Ressource supprimée, fermée ou mise en maintenance avec des réservations à venir | **Refus par défaut** avec la liste des réservations ; option explicite « annuler et prévenir » (courriel, remboursement selon la politique) |
| D5 | Horizon de réservation | 90 jours glissants, paramétrable (défaut proposé, non contesté) |
| D6 | Routes manuelles `/reserve` et `/release` | Gardées pour le rattrapage, liées à une réservation et journalisées (défaut proposé, non contesté) |
| D7 | Fonctions nouvelles | **Les quatre groupes** (battements et fenêtre, fermetures récurrentes, sites et équipements, statistiques) ; parmi les fonctions proposées, toutes sauf l'écran de porte ; ajouts de l'exploitant : historique des prix appliqués, coûts et marges, tâches créées à la réservation, participants supplémentaires facturables ou non au choix de l'équipe ; moteur de tarification revu en profondeur |

## Moteur de tarification revu (lot R1)

Aujourd'hui une règle porte une unité, un jour, une plage horaire, une validité, un ajustement et une priorité, et trois codes différents l'interprètent. Le nouveau moteur (`ResourcePriceEngine`, seul point de calcul) :

1. **Conditions d'une règle**, toutes facultatives et combinables :
   - ressource, type, groupe ou site ;
   - jours de la semaine (plusieurs) ;
   - plage horaire ;
   - période de validité ;
   - jours fériés ou calendrier spécial ;
   - segment de client : public, membre, abonné d'un plan donné, entreprise, partenaire ;
   - nombre de participants ;
   - durée (paliers) ;
   - délai avant le début (réservation anticipée, dernière minute) ;
   - canal : portail, guichet, équipe.
2. **Effets** : prix fixe, écart en montant ou en pourcentage (en décimal), prix par participant, minimum facturable, pas de facturation (arrondi au quart d'heure ou à la demi-heure).
3. **Combinaison** :
   - une règle de base par unité, choisie par priorité puis précision (la plus spécifique gagne) ;
   - des règles d'ajustement cumulables ou exclusives, selon un drapeau de la règle ;
   - un ordre d'application stable.
4. **Découpage** : une réservation à cheval sur deux plages (heures creuses et heures pleines, ou deux jours) est tarifée tranche par tranche, au prorata.
5. **Choix de l'unité** : le moins cher pour le client parmi les unités possibles. Exemple : 7 heures facturées au prix d'une journée si c'est moins cher, plafond journalier et hebdomadaire compris. Demi-journée = 5 h et journée = 10 h (D2), en paramètres.
6. **Suppléments** : équipements optionnels (`extraPrice`), services additionnels, participants supplémentaires.
7. **Remises** : codes promo et remises de fidélité (voir R5), appliqués après le prix de base, avec plafond.
8. **Heures incluses** dans un abonnement : consommées avant la facturation, si la fonction est retenue (voir R5).
9. **Explication** : chaque devis et chaque réservation gardent le détail (règles retenues, tranches, base, ajustements, suppléments, remises, quantité). La trace est enregistrée avec la réservation, pour que la facture reste explicable même après un changement de règle.
10. **Versions** : une règle modifiée garde son historique (date d'effet). Le recalcul (D1) se fait sur les règles en vigueur au moment du changement.
11. **Simulateur** : `POST /resource-pricing/simulate` renvoie le prix pour une ressource, un créneau, un client et des options, avec la trace. Il sert au devis, à l'aperçu de l'équipe et aux tests.
12. **Contrôles** : règles en conflit (même précision et même priorité) signalées à l'enregistrement ; aucune ressource réservable sans prix de base.

## Fonctions proposées en plus (lot R5)

Absentes aujourd'hui (approbation, quotas, liste d'attente, pointage et non-présentation existent déjà dans les réservations) :

1. **Codes promo et remises** sur les réservations, en réutilisant le moteur de promotions des abonnements.
2. **Heures incluses dans un abonnement**, par exemple 10 heures de salle de réunion par mois, consommées avant facturation, avec le solde affiché au portail.
3. **Services additionnels** à la réservation : café, traiteur, impression, matériel. Lignes facturées avec la réservation et préparées par l'équipe (liste du jour).
4. **« Trouver une salle »** : recherche par besoin (participants, équipements, créneau, site, budget). Elle propose des créneaux ou des ressources de remplacement quand le premier choix est pris.
5. **Plan d'étage** : étages, plan en image par site, position de chaque ressource sur le plan, disponibilité en direct sur le plan (portail et équipe).
6. **Écran de porte** (non retenu le 18/11/2026) : un lien signé par ressource, pour une tablette devant la salle. Il affiche libre ou occupé, la réservation en cours et la suivante, et permet la réservation immédiate par un membre.
7. **Jours fériés et horaires exceptionnels** : calendrier des jours fériés du Congo, plus des horaires spéciaux par date. Il est pris en compte par les créneaux, le prix et le calendrier.
8. **Caution** pour certaines ressources (salle événementielle) : montant bloqué puis rendu ou retenu après l'état des lieux.
9. **Signalement d'incident** sur une ressource, par le membre ou l'équipe. Il bloque la ressource si nécessaire et ouvre un ticket au support, avec historique de maintenance par ressource.
10. **Export de calendrier** : lien iCal par ressource pour l'équipe, et invitation `.ics` jointe au courriel de confirmation du membre.
11. **Règlement et état des lieux** : règles d'usage par ressource, affichées et acceptées à la réservation ; liste de vérification et photos de fin d'utilisation.
12. **Frais de dépassement et de non-présentation** : dépassement mesuré au départ, non-présentation facturée selon la politique, avec des montants sur la politique.

## Tableau des écarts

| N° | Écart | État |
| --- | --- | --- |
| 1 | Deux moteurs de prix divergents | Réglé (R1) |
| 2 | `POST /resources/search` en `RESOURCE:CREATE`, filtres par identifiants numériques | Réglé (R0 et R4) |
| 3 | `GET /resources` sans filtres hors résumé | Réglé (R4) |
| 4 | Politique désactivée toujours appliquée | Réglé (R2) |
| 5 | `allowCancellation` faux par défaut | Réglé (R2) |
| 6 | Suppression d'un groupe sans contrôle (500 sur clé étrangère) | Réglé (R2) |
| 7 | Suppression d'une ressource sans contrôle des réservations | Réglé (R2) |
| 8 | Photos : appartenance, document, couverture | Réglé (R0) |
| 9 | `/reserve` et `/release` manuels sans réservation | Réglé (R3) |
| 10 | Pas de suppression ni réactivation de créneau | Réglé (R3) |
| 11 | `capacity` de création de créneaux ignoré | Réglé (R3) |
| 12 | Fermeture sans `reason` dans la réponse | Réglé (R2) |
| 13 | Calendrier public sans contrôle ni borne, jour vide `FULL` | Réglé (R0) |
| 14 | Lectures de créneaux qui écrivent | Réglé (R3) |
| 15 | `floorLabel` perdu | Réglé (R2) |
| 16 | Messages en anglais | Réglé (R6) |
| Rn-1 | Prix non recalculé au changement d'horaire ou de ressource | Réglé (R1) |
| Rn-2 | Horizon limité à la fin du mois courant | Réglé (R3) |
| Rn-3 | Création manuelle de créneaux en 08:00-22:00 fixe | Réglé (R3) |
| Rn-4 | Fermeture sans contrôle des réservations ni avis | Réglé (R2) |
| Rn-5 | Devis : base prise sur une règle de week-end ; pourcentage en division entière | Réglé (R1) |
| Rn-6 | Modification de règle non revalidée, champ impossible à vider | en partie réglé (R1 : champs texte vidables, doublons refusés) |
| Rn-7 | Traitements groupés, photos, horaires non journalisés | Réglé (R0) |
| Rn-8 | Horaires d'ouverture réservés au rôle ADMIN | Réglé (R0) |
| Rn-9 | Lecture de la galerie en `RESOURCE:GALLERY` | Réglé (R0) |
| Rn-10 | `ResourcePricingRule.deleted` mort | Réglé (R1) |
| Rn-11 | `extraPrice` et `optional` des équipements jamais lus | nouveau |
| Rn-12 | Maintenance ou hors service sans effet sur l'existant | Réglé (R2) |
| Rn-13 | N+1 sur les listes de ressources | Réglé (R4) |
| Rn-14 | Nouvel essai dans une transaction compromise | Réglé (R4) |
| Rn-15 | Créations en `201` vide (code jamais rendu) | Réglé (R4) |
| Rn-16 | Identifiants numériques des fermetures et créneaux | Réglé (codes réversibles du 17/11, constaté au R4) |
