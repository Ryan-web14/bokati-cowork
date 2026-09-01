# Ressources — durée de créneau, ouverture groupée, tarification groupée

Écrit le 2026-09-01. Couvre les trois lots livrés sur le module ressources : durée de créneau
configurable, ouverture de disponibilités sur plusieurs ressources, pose d'une grille tarifaire sur
plusieurs ressources avec prévisualisation.

Base des chemins : `/sni/api/v1`.

**Rien de ce qui existe ne change de comportement.** Les points d'entrée unitaires acceptent les
mêmes charges utiles qu'avant et répondent pareil. Tout ce qui suit est additif.

---

## 1. La durée de créneau devient configurable

Le pas de 30 minutes était figé dans le code. Une salle de réunion s'en accommode ; une cabine
téléphonique louée au quart d'heure, non.

### Ce qui change pour l'interface

Un champ facultatif `slotDurationMinutes` sur la création de disponibilité :

```
POST /resource-availabilities
```

```json
{
  "resourceCode": "RES-SAL-202609-00000002",
  "startedAt": "2026-09-25T08:00",
  "endedAt": "2026-09-25T18:00",
  "totalCapacity": 10,
  "active": true,
  "slotDurationMinutes": 30
}
```

**Absent, la durée de la ressource s'applique ; une ressource qui n'en porte aucune reste à 30.**
Une interface qui envoie `30` sur un parc à 30 ne change donc rien. Le champ porte exactement le
nom que le frontend employait déjà.

### La durée appartient à la ressource, pas à la disponibilité

C'est le point à comprendre avant de dessiner l'écran. Le champ ne fixe pas la durée de *cette*
disponibilité : il porte la valeur jusqu'à la ressource, où elle vit. Le moteur de réservation
compose une fenêtre à partir de créneaux contigus et calcule `nombre de créneaux = durée / pas`.
Des durées mélangées sur une même ressource fausseraient ce décompte **en silence** — une
réservation de deux heures réserverait quatre créneaux là où il en faut huit.

Conséquence directe : **une valeur différente de celle de la ressource n'est acceptée que si
celle-ci n'a aucun créneau futur.** Sinon la création est refusée, en disant combien de créneaux
supprimer d'abord :

```
409 Conflict
La ressource RES-SAL-202609-00000001 a 8 creneau(x) futur(s) de 30 minutes ·
supprimez-les avant de passer a 15 minutes, sinon ses creneaux auraient des durees
melangees et les fenetres calculees seraient fausses.
```

Le message est destiné à être affiché tel quel : il contient le compte et les deux durées.

### Valeurs admises

Seuls les **diviseurs de 60** : `5`, `10`, `15`, `20`, `30`, `60`. Toute autre valeur est refusée :

```
400 Bad Request
La duree de creneau doit diviser 60 · valeurs admises : 5, 10, 15, 20, 30, 60. Recue : 45
```

La contrainte n'est pas cosmétique. Avec un pas de 45 minutes, les créneaux d'une journée cessent
de tomber sur les heures rondes et une partie des heures devient inaccessible à la réservation,
sans que rien ne l'explique à l'utilisateur. Un sélecteur fermé sur ces six valeurs évite au
formulaire d'avoir à gérer le refus.

### Lecture de la durée en vigueur

`slotDurationMinutes` figure désormais dans `GET /resources` et `GET /resources/{code}`, en lecture
seule. Sans lui, l'interface ne pouvait ni afficher la durée en vigueur ni pré-remplir le
formulaire, et proposait 30 par défaut à une ressource travaillant au quart d'heure.

Il était déjà présent dans `ResourceAvailabilityResponse`, les calendriers admin et public, et les
créneaux occupés.

---

## 2. Ouvrir la même plage sur plusieurs ressources

```
POST /resource-availabilities/bulk
```

```json
{
  "resourceCodes": ["RES-SAL-202609-00000001", "RES-SAL-202609-00000002"],
  "startedAt": "2026-09-22T08:00",
  "endedAt": "2026-09-22T12:00",
  "totalCapacity": 6,
  "active": true,
  "slotDurationMinutes": 30
}
```

Hormis `resourceCodes` qui remplace `resourceCode`, le corps est celui de la création unitaire —
mêmes noms, mêmes alias (`totalCapacity`), mêmes règles.

### La réponse est un compte rendu, pas un tout-ou-rien

**`200 OK`**, même quand des ressources échouent :

```json
{
  "processed": 2,
  "skipped": 1,
  "results": [
    { "resourceCode": "RES-SAL-202606-00000000", "status": "SKIPPED",
      "reason": "Could not process resource availability: the requested range overlaps existing availability slots",
      "detail": null },
    { "resourceCode": "RES-SAL-202609-00000001", "status": "PROCESSED", "reason": null, "detail": 8 },
    { "resourceCode": "RES-SAL-202609-00000002", "status": "PROCESSED", "reason": null, "detail": 8 }
  ]
}
```

| Champ | Sens |
|---|---|
| `processed` / `skipped` | compteurs, pour l'en-tête du récapitulatif |
| `results[].status` | `PROCESSED` ou `SKIPPED` |
| `results[].reason` | motif de l'écart, à afficher tel quel · `null` en cas de succès |
| `results[].detail` | nombre de créneaux écrits pour cette ressource · `null` si écartée |

Chaque ressource est traitée dans sa propre transaction : **celles qui aboutissent sont écrites même
si d'autres échouent**, et une ressource déjà pourvue conserve ce qu'elle avait — la capacité
existante n'est pas écrasée par celle du lot.

C'est délibérément un compte rendu. Sur vingt ressources il est normal que deux échouent
— chevauchement, fermeture active, plafond d'un mois d'avance — et refuser l'ensemble obligerait à
retirer les fautives une par une puis à relancer, c'est-à-dire exactement le travail que l'appel
groupé doit épargner.

### Côté interface

`status: "SKIPPED"` n'est pas une erreur à traiter dans un `catch` : le HTTP est 200. Le
récapitulatif attendu ressemble à **« 18 ressources ouvertes, 2 écartées »**, avec les deux lignes
en cause et leur motif, et un bouton pour relancer sur la sélection écartée une fois corrigée.

### Ce qui est refusé en bloc, avant toute écriture

Ce qui relève de la requête et non des ressources sort en **400**, sans rien écrire :

| Cas | Réponse |
|---|---|
| `slotDurationMinutes` non diviseur de 60 | `400` — le message du §1 |
| liste vide, ou seulement des valeurs vides | `400 Au moins un code ressource est requis` |
| plus de 20 ressources | `400 Lot de 25 ressources · maximum autorise : 20. Decoupez l'appel.` |

Une durée invalide sur vingt ressources donnerait vingt lignes écartées identiques sous un HTTP 200 :
rien de ce qui est demandé ne peut aboutir, le refus est donc global.

**Bornes.** 20 ressources par appel (réglable côté serveur par
`bokati.resource.bulk-max-resources`). Une plage d'un mois produit déjà environ 384 créneaux par
ressource ; au-delà de vingt, l'interface doit découper.

**Doublons.** Un même code présent deux fois est ramené à une occurrence, dans l'ordre de saisie.
Sans cela, le second passage entrerait en chevauchement avec ce que le premier vient d'écrire et
ressortirait en échec incompréhensible. Le compte rendu ne porte donc qu'une ligne par ressource
distincte — ne comptez pas sur `results.length` pour égaler `resourceCodes.length`.

### Motifs d'écart les plus fréquents

- `the requested range overlaps existing availability slots`
- `the requested range overlaps an active closure`
- `availability can only be created up to one month in advance`
- `availability must be created within working hours from 08:00 to 20:00`
- `availability cannot be created in the past`
- `the resource is inactive` · `the resource is not bookable`
- `Resource with code X not found`
- le message de durée du §1, quand la ressource a déjà des créneaux futurs d'un autre pas

---

## 3. Poser la même grille tarifaire sur plusieurs ressources

```
POST /resource-pricing-rules/bulk
```

```json
{
  "resourceCodes": ["RES-SAL-202609-00000001", "RES-SAL-202609-00000002"],
  "bookingUnit": "HOUR",
  "price": 15000,
  "label": "Tarif heures ouvrables",
  "dayOfWeek": 1,
  "startsAt": "08:00:00",
  "endsAt": "18:00:00",
  "priority": 10,
  "active": true
}
```

Corps identique à `POST /resource-pricing-rules`, `resourceCode` devenant `resourceCodes`. Même
réponse que le §2 ; `detail` y vaut `null`, il n'y a rien à compter. Mêmes bornes, même
dédoublonnage, même isolation par ressource.

### « Plusieurs plans pour une ressource » fonctionnait déjà

À ne pas confondre avec ce point d'entrée. Rien n'a jamais limité le nombre de règles portées par
une ressource : `findApplicableRules` les arbitre par priorité décroissante, puis par identifiant
décroissant à priorité égale. Il suffit d'appeler plusieurs fois `POST /resource-pricing-rules` sur
le même `resourceCode`. Ce qui manquait était l'inverse : poser une même grille sur tout un parc.

---

## 4. Prévisualiser les conflits tarifaires

```
POST /resource-pricing-rules/bulk/preview
```

Même corps que la création groupée. **Ne modifie rien** — aucune règle n'est créée, aucun compteur
consommé. À appeler avant de valider : poser une grille sur vingt ressources rend l'erreur aussi
triviale que la saisie.

```json
{
  "resourcesInspected": 3,
  "resourcesWithOverlap": 2,
  "strictDuplicates": 0,
  "results": [
    {
      "resourceCode": "RES-SAL-202606-00000000",
      "duplicate": false,
      "overlapping": [
        { "id": 7291495199787880448, "label": "Tarif heures ouvrables", "price": 15000,
          "priority": 10, "dayOfWeek": 1, "startsAt": "08:00", "endsAt": "18:00", "wins": true },
        { "id": 7267238835280580608, "label": null, "price": 21000,
          "priority": 0, "dayOfWeek": null, "startsAt": null, "endsAt": null, "wins": false }
      ]
    },
    { "resourceCode": "RES-SAL-202609-00000003", "duplicate": false, "overlapping": [] }
  ]
}
```

### Comment lire le résultat

**Un chevauchement n'est pas une faute.** C'est le mécanisme même des grilles tarifaires : un tarif
général, un tarif week-end plus prioritaire, une promotion encore au-dessus. `priority` arbitre.
Afficher les chevauchements comme des erreurs pousserait à corriger ce qui fonctionne.

- **`wins: true`** — la règle existante l'emporterait sur celle que vous proposez ; sa priorité est
  supérieure. La règle proposée serait donc créée mais **jamais appliquée** sur cette plage. C'est
  le seul signal qui mérite un avertissement franc.
- **`wins: false`** — la règle proposée prendrait le dessus. À priorité égale, elle gagne : elle
  sera plus récente, et l'arbitrage départage par identifiant décroissant.
- **`duplicate: true`** — une règle active est déjà **strictement identique** (même unité, prix,
  jour, plage horaire, priorité). Elle n'apporte rien et brouille l'arbitrage. C'est ce qui mérite
  un regard, pas le chevauchement.
- **`overlapping: []`** — voie libre.

Les compteurs de tête (`resourcesInspected`, `resourcesWithOverlap`, `strictDuplicates`) donnent le
bandeau de synthèse sans parcourir la liste.

`dayOfWeek`, `startsAt` et `endsAt` à `null` sur une règle existante signalent une règle
**attrape-tout** : elle s'applique en permanence, donc chevauche par construction tout ce qu'on
propose.

### Un code inconnu échoue la prévisualisation entière

`404`, contrairement à la création qui l'écarterait en ligne. La prévisualisation est une question
posée sur un ensemble précis : répondre sur un sous-ensemble sans le dire serait trompeur.
Validez la sélection avant d'appeler, ou traitez le 404 en invitant à corriger la liste.

La prévisualisation applique les **mêmes bornes et le même dédoublonnage** que la création, pour
qu'un lot accepté en prévisualisation le soit aussi à la création.

---

## 5. Récapitulatif des points d'entrée

| Méthode | Chemin | Réponse |
|---|---|---|
| `POST` | `/resource-availabilities` | `201` — inchangé, plus `slotDurationMinutes` facultatif |
| `POST` | `/resource-availabilities/bulk` | `200` + compte rendu |
| `POST` | `/resource-pricing-rules` | `201` — inchangé |
| `POST` | `/resource-pricing-rules/bulk` | `200` + compte rendu |
| `POST` | `/resource-pricing-rules/bulk/preview` | `200` + conflits, n'écrit rien |
| `GET` | `/resources`, `/resources/{code}` | inchangé, plus `slotDurationMinutes` en lecture |

Forme d'erreur inchangée sur tout le module :

```json
{
  "errorCode": "BAD_REQUEST",
  "status": 400,
  "message": "La duree de creneau doit diviser 60 · valeurs admises : 5, 10, 15, 20, 30, 60. Recue : 45",
  "traceId": "4df23a7f-3b6c-495e-99d7-1bd5756c1ef1"
}
```

---

## 6. Parcours conseillé pour l'écran d'administration

1. **Sélection** des ressources (max 20 par envoi ; découper au-delà).
2. **Durée de créneau** : sélecteur fermé sur `5 / 10 / 15 / 20 / 30 / 60`, pré-rempli avec le
   `slotDurationMinutes` de la ressource lorsqu'une seule est sélectionnée. Sur une sélection
   multiple aux durées hétérogènes, prévenir que celles qui ont des créneaux futurs seront écartées.
3. **Tarification** : appeler `/bulk/preview`, montrer les doublons stricts et les `wins: true`,
   puis laisser confirmer.
4. **Envoi** vers `/bulk`.
5. **Récapitulatif** : `processed` / `skipped`, les lignes écartées avec leur `reason`, et une
   relance possible sur la seule sélection écartée.
