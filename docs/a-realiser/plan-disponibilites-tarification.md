# Disponibilités et tarification — plan

Écrit le 2026-09-01. Trois demandes : rendre la durée de créneau configurable, créer des
disponibilités pour plusieurs ressources à la fois, et tarifer plusieurs ressources à la fois.

---

## 0. État des lieux

### 0.1 Ce qui existe déjà — et n'est donc pas à construire

**Plusieurs plans tarifaires pour une même ressource fonctionnent aujourd'hui.** Rien ne le
limite : aucune contrainte d'unicité sur `resource_pricing_rule`, et `findApplicableRules`
sélectionne toutes les règles applicables puis les arbitre `order by priority desc, id desc`.

Le modèle porte déjà tout ce qu'il faut pour différencier plusieurs plans sur une même
ressource :

| Champ | Rôle |
|---|---|
| `dayOfWeek` | Une règle par jour de semaine |
| `startsAt` / `endsAt` | Tarif horaire — heures creuses, heures pleines |
| `validFrom` / `validUntil` | Saisonnalité |
| `adjustmentType` / `adjustmentValue` | Majoration ou remise sur le tarif de base |
| `lastMinuteMinutes` | Tarif de dernière minute |
| `priority` | Arbitrage quand plusieurs règles s'appliquent |

Il suffit d'appeler `POST /resource-pricing-rules` plusieurs fois. Le manque n'est pas
structurel, il est **ergonomique** : rien ne permet de poser la même règle sur vingt ressources
en une fois, ni de visualiser les règles d'une ressource comme une grille cohérente.

### 0.2 Ce qui bloque réellement

| Point | Constat |
|---|---|
| Durée de créneau | `SLOT_MINUTES = 30`, constante privée, **neuf usages** dans `ResourceAvailabilityServiceImpl` |
| Duplication | La même constante est **redéfinie** dans `BookingResourceGuard` — deux sources de vérité déjà en place |
| Disponibilité groupée | `CreateResourceAvailabilityRequest` porte un seul `resourceCode` |
| Tarification groupée | `CreateResourcePricingRuleRequest` porte un seul `resourceCode` |

---

## 1. Lot A — Durée de créneau configurable

**3 j.** C'est le seul des trois qui touche au moteur.

### 1.1 La décision qui structure tout : la durée appartient à la ressource

Le champ `slot_duration_minutes` existe sur `resource_availability`, ce qui suggère une durée
**par disponibilité**. C'est un piège.

Le moteur compose une fenêtre réservable à partir de **plusieurs créneaux contigus** :

```java
int requiredSlots = normalizedDuration / SLOT_MINUTES;
List<ResourceAvailability> candidate = slots.subList(index, index + requiredSlots);
if (!isContiguous(candidate)) { continue; }
```

Cette arithmétique suppose que **tous les créneaux d'une ressource ont la même durée**. Si une
disponibilité de septembre était en créneaux de 30 minutes et celle d'octobre en créneaux de 60,
`requiredSlots` deviendrait faux dès qu'une recherche chevauche les deux périodes — et le défaut
serait silencieux : on renverrait des fenêtres de la mauvaise longueur, pas une erreur.

**La durée doit donc être portée par la ressource**, pas par la requête de création. Le champ sur
`resource_availability` devient une copie figée de la valeur au moment de la création, utile pour
lire l'historique sans jointure.

Corollaire : changer la durée d'une ressource ne doit pas être possible tant qu'elle a des
créneaux futurs. Il faut d'abord les supprimer, puis les recréer. C'est contraignant, et c'est
la seule façon d'éviter un parc de créneaux hétérogènes.

### 1.2 Schéma — `V214` dev / `V210` prod

```sql
ALTER TABLE resource
    ADD COLUMN IF NOT EXISTS slot_duration_minutes INTEGER NOT NULL DEFAULT 30;

ALTER TABLE resource
    ADD CONSTRAINT ck_resource_slot_duration
        CHECK (slot_duration_minutes IN (15, 30, 60)) NOT VALID;
```

Le défaut à 30 reprend la valeur actuelle : aucune reprise de données, le parc existant reste
identique à lui-même.

**Pourquoi une énumération et non un entier libre.** Une durée arbitraire — 7 minutes, 45
minutes — casse l'alignement des créneaux sur l'heure, qui est ce que `BookingResourceGuard`
vérifie (`startedAt.getMinute() % SLOT_MINUTES == 0`). Seuls les diviseurs de 60 se composent
proprement. À élargir plus tard si le besoin apparaît, mais commencer permissif se paie.

### 1.3 Ce qui change dans le code

Un `ResourceSlotPolicy` centralise la lecture, et **les deux constantes disparaissent** :

```java
int slotMinutes(Resource resource);          // valeur de la ressource
void assertAligned(Resource r, LocalDateTime start, LocalDateTime end);
```

Les neuf usages de `ResourceAvailabilityServiceImpl` et celui de `BookingResourceGuard` passent
par là. C'est l'essentiel du lot : supprimer la seconde source de vérité est un gain en soi,
indépendamment de la configurabilité.

| Ligne | Aujourd'hui | Devient |
|---|---|---|
| `requiredSlots = duration / SLOT_MINUTES` | constante | `duration / slotMinutes(resource)` |
| `current.plusMinutes(SLOT_MINUTES)` | constante | `plusMinutes(slotMinutes(resource))` |
| `expectedSlotCount = minutes / SLOT_MINUTES` | constante | idem |
| `minutes % SLOT_MINUTES != 0` | constante | idem |
| `slotDurationMinutes(SLOT_MINUTES)` à la création | constante | valeur de la ressource, figée sur le créneau |

### 1.4 Garde-fous

- **Changement de durée refusé** si la ressource a des créneaux futurs, avec le compte dans le
  message : « 384 créneaux futurs · supprimez-les avant de changer la durée ».
- **Contrôle de cohérence** au démarrage de la recherche : si les créneaux candidats d'une
  ressource n'ont pas tous la même durée, on refuse plutôt que de calculer faux. Ce cas ne
  devrait pas exister ; s'il se produit, c'est qu'une reprise a échoué, et le silence serait pire.

### 1.5 Ce que l'API expose

`slotDurationMinutes` s'ajoute à la ressource — création, modification, réponse. Il reste
**refusé** sur la création de disponibilité : le champ n'y a pas sa place, et l'accepter pour le
recopier depuis la ressource entretiendrait la confusion.

---

## 2. Lot B — Disponibilité pour plusieurs ressources

**1,5 j.** Aucune migration.

```
POST /resource-availabilities/bulk
{
  "resourceCodes": ["RES-SAL-...-0000", "RES-SAL-...-0001"],
  "startedAt": "2026-09-02T08:00",
  "endedAt": "2026-09-30T18:00",
  "capacity": 10,
  "active": true
}
```

### 2.1 La décision qui compte : un compte rendu, pas un tout-ou-rien

Chaque ressource a ses propres contraintes — plafond d'un mois d'avance, chevauchement avec des
créneaux existants, capacité, politique de réservation. Sur vingt ressources, il est **normal**
que deux échouent.

Refuser l'ensemble pour deux échecs obligerait à retirer les ressources fautives une par une et à
relancer. La réponse rend donc un compte rendu par ressource :

```json
{
  "created": 18, "skipped": 2, "slotsCreated": 6912,
  "results": [
    { "resourceCode": "RES-...-0000", "status": "CREATED", "slots": 384 },
    { "resourceCode": "RES-...-0007", "status": "SKIPPED",
      "reason": "Des creneaux existent deja sur cette periode" }
  ]
}
```

**Transaction par ressource**, pas globale : une ressource en échec ne doit pas annuler les
dix-sept créneaux déjà écrits. Cela impose un composant séparé pour le `REQUIRES_NEW` — un
auto-appel ne passe pas par le proxy Spring, piège déjà rencontré sur `WalletReconciliationWorker`.

### 2.2 Borne

Vingt ressources par appel, configurable. Une plage d'un mois sur une ressource produit déjà
~384 créneaux ; vingt ressources en font 7 680. Au-delà, l'appel doit être découpé.

---

## 3. Lot C — Tarification pour plusieurs ressources

**1 j.** Aucune migration.

```
POST /resource-pricing-rules/bulk
{
  "resourceCodes": ["RES-...-0000", "RES-...-0001"],
  "bookingUnit": "HOUR", "price": 5000,
  "label": "Heures creuses", "startsAt": "08:00", "endsAt": "12:00",
  "priority": 10
}
```

Même compte rendu par ressource, même borne, mêmes règles de validation que la création unitaire.

### 3.1 Le vrai risque n'est pas technique

Poser une règle sur vingt ressources en un appel rend trivial ce qui était fastidieux — y compris
l'erreur. Deux garde-fous :

- **Prévisualisation** — `POST /resource-pricing-rules/bulk/preview`, en lecture seule, qui
  renvoie pour chaque ressource les règles déjà en place que la nouvelle **concurrencerait**,
  c'est-à-dire celles dont la fenêtre chevauche et que `priority` devra départager.
- **Signalement des doublons** — aucune contrainte d'unicité n'existe sur
  `resource_pricing_rule`, et c'est délibéré puisque le chevauchement est arbitré par priorité.
  Mais rien ne distingue un chevauchement voulu d'un doublon accidentel. La création groupée
  signale, sans refuser, toute règle strictement identique à une règle active.

### 3.2 Ce qui n'est pas dans ce lot

« Plusieurs plans tarifaires pour une ressource » fonctionne déjà (§ 0.1). Si le besoin réel est
de **voir** et **gérer** ces plans comme un ensemble — une grille horaire lisible, dupliquer la
grille d'une ressource vers une autre — c'est un sujet d'interface plus que d'API, et il mérite
d'être posé séparément.

---

## 4. Récapitulatif

| Lot | Contenu | Migration | Coût |
|---|---|---|---|
| **A** | Durée de créneau portée par la ressource | `V214` / `V210` | 3 j |
| **B** | Disponibilité groupée | — | 1,5 j |
| **C** | Tarification groupée + prévisualisation | — | 1 j |

**5,5 jours.** B et C sont indépendants de A et l'un de l'autre ; ils peuvent partir en premier
si le besoin presse, la durée de créneau étant le seul chantier qui touche au moteur.

### Décisions attendues

- **D-1.** Durées admises : `15, 30, 60` ? Une ressource à la demi-journée demanderait 240, ce qui
  reste un diviseur de 1440 mais casse l'alignement horaire. Ma recommandation : commencer à
  `15, 30, 60` et élargir sur besoin constaté.
- **D-2.** Changer la durée d'une ressource ayant des créneaux futurs — refuser (ma
  recommandation), ou supprimer et recréer automatiquement ? La suppression automatique détruirait
  des créneaux **déjà réservés** ; le refus laisse la décision à l'humain.
- **D-3.** Borne de 20 ressources par appel groupé — suffisante, ou le parc est-il plus large ?
