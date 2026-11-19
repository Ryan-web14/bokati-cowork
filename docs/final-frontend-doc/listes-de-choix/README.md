# Listes de choix : valeurs fixes, listes de l'administrateur, recherches

> Rédigé d'après le code (lot L0, 19/11/2026). Routes générées : module [socle](../socle/routes.md) (`ReferenceController`, `LookupController`).

Règle pour tout écran : un champ dont les valeurs sont connues du serveur est une **liste déroulante alimentée par le serveur**, jamais une saisie au clavier ni une liste écrite en dur dans le frontend. Trois sources, selon la nature de la valeur :

| Nature | Exemple | Source | Qui la modifie |
| --- | --- | --- | --- |
| Valeur fixe (énumération du code) | Statut d'une réservation, canal, mode de règlement | `GET /reference/enums` | Personne : elle change avec le code |
| Liste de l'administrateur | Motifs, banques, types de pièce | `GET /reference-lists/{listCode}` | L'administrateur, depuis son panneau |
| Recherche de choix (donnée vivante, nombreuse) | Utilisateur à assigner, client, ressource | `GET /lookup/...` ou `GET /{module}/lookup/{type}` | Les écrans de la donnée elle-même |

## Valeurs fixes

`GET /sni/api/v1/reference/enums?module=BOOKING&group=STATUS` (les deux filtres sont facultatifs) :

```json
[ { "module": "BOOKING", "group": "CHANNEL", "label": "Canal",
    "values": [ { "code": "PORTAL", "label": "Espace client", "description": null, "order": 10, "selectable": false },
                { "code": "PHONE", "label": "Téléphone", "description": null, "order": 30, "selectable": true } ] } ]
```

- Afficher `label`, envoyer `code`, trier par `order`.
- `selectable: false` : valeur posée par le serveur seul. L'afficher (badge, filtre de liste) mais ne pas la proposer dans un formulaire.
- Le serveur contrôle un code reçu avec le même registre : un code inconnu ou non sélectionnable donne `400` `VALEUR_INCONNUE` avec les valeurs permises.
- À charger une fois par session (les valeurs ne changent qu'avec une nouvelle version du serveur).
- Groupes déclarés au lot L0 : module `BOOKING` (`STATUS`, `PAYMENT_MODE`, `PAYMENT_STATUS`, `CHANNEL`, `PARTICIPANT_ROLE`, `PARTICIPANT_STATUS`, `RECURRENCE`, `HOLD_STATUS`, `WAITLIST_STATUS`, `LINE_TYPE`, `OPTION_KIND`). Les autres modules s'ajoutent lot par lot (L1 à L4).
- L'ancienne route de l'inventaire (`GET /inventory/reference/enums`) reste en place.

## Listes de l'administrateur

### Lire

- `GET /sni/api/v1/reference-lists?module=PAYMENT` : les listes, **vides comprises** : `{ code, module, label, description, strict, valueCount, activeValueCount, autoAddedCount, usageCount, otherUsageCount }`.
- `GET /sni/api/v1/reference-lists/{listCode}` : valeurs actives, dans l'ordre : `{ listCode, code, label, sortOrder, active, requiresText, systemValue, autoAdded, usageCount }`. `?includeInactive=true` pour l'administration.

Lecture ouverte à toute l'équipe (rôle du back-office, sans droit particulier) : un formulaire n'a pas à demander un droit d'administration pour remplir sa liste.

### Choisir une valeur dans un formulaire

- Proposer les valeurs actives ; `requiresText: true` (en particulier `OTHER`, « Autre, préciser ») ouvre un champ de précision obligatoire.
- Un **motif** s'envoie en deux champs : `reasonCode` (code de la liste) et `reasonText` (précision). L'ancien champ texte libre (`reason`) reste accepté pendant la transition, mais ne donne aucune statistique : passer au code.
- Erreurs (`400`) : `VALEUR_REQUISE` (rien choisi), `VALEUR_INCONNUE` (code absent d'une liste stricte), `VALEUR_INACTIVE` (valeur retirée), `PRECISION_REQUISE` (précision manquante).
- Liste **souple** (`strict: false`) : une valeur inconnue est acceptée et ajoutée d'elle-même (`autoAdded: true`), pour que l'administrateur la relise. Liste **stricte** : refusée.

### Administrer (droit `SYSTEM:REFERENCE_LISTS`, rôles `SUPER_ADMIN` et `ADMIN` par défaut)

| Geste | Route | Corps |
| --- | --- | --- |
| Ajouter une valeur | `POST /reference-lists/{listCode}/values` | `{ code, label, sortOrder?, active?, requiresText? }` ; `400 VALEUR_EXISTANTE` si le code existe déjà |
| Modifier une valeur | `PATCH /reference-lists/{listCode}/values/{code}` | `{ label?, sortOrder?, active?, requiresText? }` ; une valeur relue perd `autoAdded` |
| Remplacer toute la liste | `PUT /reference-lists/{listCode}` | `[ { code, label, sortOrder?, active?, requiresText? } ]` ; une valeur absente est **désactivée**, jamais supprimée |
| Régler la liste | `PATCH /reference-lists/{listCode}` | `{ label?, description?, strict? }` |

Une valeur n'est jamais supprimée : les enregistrements qui la citent gardent leur libellé. `systemValue: true` : valeur livrée avec l'application, désactivable seulement. `OTHER` exige toujours une précision.

Écran conseillé : liste des listes par module, avec en évidence `autoAddedCount` (valeurs à relire) et `otherUsageCount` (beaucoup de « Autre » : une valeur manque) ; puis la liste ouverte, glisser-déposer pour l'ordre, interrupteur actif.

### Listes livrées

| Module | Listes |
| --- | --- |
| `BOOKING` (réservations, lot B7) | `BOOKING_CANCEL_REASON`, `BOOKING_REJECT_REASON` |
| `PAYMENT` (caisse) | `VARIANCE_REASON`, `ADJUSTMENT_REASON`, `REVERSAL_REASON`, `SUSPENSION_REASON`, `SESSION_CANCEL_REASON`, `ABSENCE_REASON`, `RESPONSIBILITY_TRANSFER_REASON`, `OUT_OF_HOURS_REASON`, `EXTERNAL_DOCUMENT_TYPE`, `BANK` |

Les listes de la caisse vivent désormais dans ce socle ; leurs routes `/cash-registers/reference-lists` restent valables et rendent les mêmes valeurs. Les autres modules ajoutent leurs listes lot par lot.

## Recherches de choix

Convention : `GET /{module}/lookup/{type}?q=&page=0&size=20&includeInactive=false`, réponse paginée (`{ data, pageable }`, voir [README racine](../README.md)) de :

```json
{ "type": "USER", "code": "agent@exemple.cg", "label": "Awa Mabiala", "hint": "agent@exemple.cg", "active": true }
```

- Afficher `label` et `hint`, envoyer `code`. `type` distingue les éléments d'une recherche qui en mélange plusieurs.
- `size` : 50 au plus. Saisie de 2 caractères minimum conseillée avant d'appeler, avec un délai de frappe.
- Ouvertes au droit du formulaire qui s'en sert, pas au droit d'administration de la donnée.

Recherches livrées au lot L0 :

| Route | Éléments | Accès |
| --- | --- | --- |
| `GET /lookup/users` | Utilisateurs de l'équipe (assigner, désigner un responsable) ; `code` = courriel | Toute l'équipe |
| `GET /bookings/lookup/owners` | Titulaires d'une réservation : `MEMBER`, `CUSTOMER`, `BUSINESS_ENTITY` (lot B7) | `BOOKING:READ` |

Un module peut aussi rendre ses listes d'écran en un appel : `GET /bookings/reference` (valeurs fixes, listes de l'administrateur et grille d'annulation des réservations).

## Acteur

« Qui a fait » est lu dans la session : les champs `createdBy`, `changedBy`, `actor`, `approvedBy`... d'une requête sont ignorés là où le socle est branché (caisse, réservations). Ne plus les envoyer ; les afficher depuis la réponse.
