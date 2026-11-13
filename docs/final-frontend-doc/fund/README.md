# Module demandes de fonds (`fund`)

États de besoin, demandes de fonds, validation à plusieurs niveaux, décaissement par la caisse, avances au personnel et leur justification, et pièces imprimables (bons, reçus, pièces de caisse) avec leur QR de vérification.

Lire d'abord le [README général](../README.md), puis ce fichier. Le contrat exact de chaque route est dans [routes.md](routes.md) et `routes/<Contrôleur>.md`.

## 1. Domaines

| Domaine | Fichier | Contenu |
| --- | --- | --- |
| Demandes et avances | [demandes-avances.md](demandes-avances.md) | États de besoin, demandes multi-lignes, circuit de validation, délégations, file « À décaisser », avances, justification, reliquats et compléments, relances |
| Pièces imprimables | [pieces-comptables.md](pieces-comptables.md) | Bons de décaissement, reçus, pièces de caisse : numérotation, PDF, signatures, QR, vérification publique |

## 2. Ce qu'il faut savoir

- **Tout employé** peut créer une demande, sans session de caisse. La caissière décaisse depuis sa session, une fois la demande approuvée.
- **Approuver** et **payer** sont deux actions distinctes, avec des droits distincts.
- Une demande non prévue au budget porte un motif. Le contrôle budgétaire du module comptable peut avertir ou bloquer ([budget](../accounting/budget.md)).
- Une avance reste ouverte jusqu'à sa justification complète. Le reliquat est rendu à la caisse, ou un complément est versé.
- Chaque décaissement produit son écriture comptable quand la comptabilité est active ([caisse comptable](../accounting/caisse-comptable.md)).

## 3. Écrans

| Écran | Usage |
| --- | --- |
| Mes demandes | Création, suivi, frise de validation, pièces jointes |
| À valider | Demandes en attente de mon niveau, délégations |
| À décaisser | File de la caissière, décaissement depuis la session |
| Avances ouvertes | Justification ligne par ligne, retour du reliquat, relances |
| Pièces | Impression et réimpression des bons et reçus |
