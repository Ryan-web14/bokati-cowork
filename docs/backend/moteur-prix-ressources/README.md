# Moteur de prix des ressources : prix manquant et superposition (23/11/2026)

Branche `hotfix/moteur-prix-ressources`, partie de `new-main`. Aucune migration.

## Constat

Signalement : « le moteur de prix ne fonctionne toujours pas, l'utilisation intelligente des règles prix n'est pas prise en compte, et les superpositions puis l'utilisation ne passent pas ».

J'ai relu les règles de la base de développement (lecture seule). Sur la salle `RES-SAL-202606-00000000` :

| Règle | Unité | Rôle | Conditions |
|---|---|---|---|
| 2 | HOUR | prix de base 15000, priorité 10 | lundi, 8 h-18 h |
| 9 | DAY | prix de base 105014 | aucune |
| 12 | HOUR | +20 % (avec un prix 35000 saisi, que le formulaire imposait) | samedi, 11 h-23 h |

Le tarif horaire général (21000) avait été supprimé. Défauts trouvés :

1. **Calcul à l'heure abandonné** dès qu'une tranche n'avait pas de prix horaire de base (samedi, lundi après 18 h). Résultat : la journée entière facturée (105014 pour deux heures), ou refus si l'heure était imposée. La majoration du samedi ne s'appliquait jamais.
2. **Réservation gratuite** quand aucune unité n'avait de prix pour le créneau (salle `RES-SAL-202609-00000001` hors du lundi 8 h-18 h ; prix réservés aux membres pour un client public).
3. **Ajustements exclusifs** : un seul ajustement non cumulable s'appliquait, toutes natures confondues. Une majoration du samedi et une remise membre s'excluaient, la priorité choisissait. Entre deux remises de même priorité, la plus récente l'emportait, pas la meilleure pour le client.
4. **Saisie** :
   - le prix était obligatoire pour un ajustement, alors qu'il n'y sert à rien ;
   - une plage horaire qui passe minuit était refusée, alors que le moteur sait la calculer.
5. **Suggestions de créneaux** calculées sans le client, donc sans ses prix.

## Correctifs

- **`ResourcePriceEngine`** :
  - **prix manquant** : déduit d'une unité voisine au prorata ; à défaut, prix de référence de l'unité (sa règle de base la plus générale qui vise ce client) ; puis les ajustements de l'unité s'appliquent. La ligne du devis et la trace de la réservation disent d'où vient le prix ;
  - **plus de gratuit par erreur** : refus `400` quand la ressource a des prix, mais aucun pour ce client ;
  - **superposition** : prix fixe, puis majorations, puis remises. La remise non cumulable retenue est la plus prioritaire puis la plus avantageuse ;
  - une unité sans aucune règle n'est jamais inventée, même imposée.
- **`ResourcePricingRuleServiceImpl`** :
  - prix facultatif et ignoré pour un ajustement (enregistré nul) ;
  - valeur obligatoire pour un ajustement ;
  - plage qui passe minuit admise, plage vide refusée.
- **`BookingSuggestionServiceImpl`** : prix au contexte du client ; un créneau sans prix est proposé sans montant au lieu de faire tomber la liste.

Doc frontend : [tarifs.md](../../final-frontend-doc/ressource/fonctions/tarifs.md), section « Prix manquant et superposition des règles ».

## Tests

- `ResourcePriceEngineTest` :
  - la salle relevée :
    - samedi 12 h-14 h : 25203,36, à l'heure, déduit de la journée et majoré ;
    - heure imposée le samedi : calculée ;
    - lundi 17 h-19 h : 25501,40 ;
  - majoration et remise membre superposées ;
  - meilleure remise pour le client ;
  - prix de référence ;
  - refus au lieu de gratuit ;
  - ressource sans prix gratuite.
- Ajustés :
  - `ResourceR7Test` : le cas incohérent est une plage vide, plus une plage qui passe minuit ;
  - `SlotSuggestionsAndOccupancyTest` : le calcul reçoit le contexte.
- Non-régression : tests unitaires des ressources, réservations, facturation, abonnements et clients (1 157).

## À noter pour l'exploitant

- La règle 12 garde en base son prix 35000, sans effet. Il disparaît à la prochaine modification de la règle.
- Si le samedi doit avoir son propre prix horaire, il faut créer un prix de base HOUR le samedi. La majoration de +20 % s'appliquera alors sur ce prix au lieu du prix déduit de la journée.
- Les réservations déjà enregistrées gardent leur prix ; une modification de créneau les recalcule avec le nouveau moteur.
