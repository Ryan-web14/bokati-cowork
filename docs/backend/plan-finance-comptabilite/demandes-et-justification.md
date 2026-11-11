# Demandes de fonds, état de besoin et justification des dépenses

Refonte des demandes de caisse (`cash_request`) en un circuit complet : **état de besoin**, puis **demande de fonds**, **validation**, **décaissement** et, pour une avance, **justification ligne par ligne**. Le tout doit être plus intuitif pour celui qui demande et plus sûr pour celui qui paie.

## 1. Ce qui ne va pas aujourd'hui

| Constat (code actuel) | Effet |
| --- | --- |
| `CashRequestServiceImpl.submit(sessionNumber, ...)` exige une session de caisse ouverte | Seule une caissière peut créer une demande. L'employé qui a le besoin passe par elle, et `requestedBy` est saisi à la main. |
| `approve` crée le mouvement de caisse aussitôt | « Accordé » et « payé » se confondent. On ne peut pas approuver le matin et décaisser l'après-midi, ni approuver depuis le bureau sans caisse. |
| Un montant, un motif, des pièces jointes en JSON | Pas de lignes, de nature, de fournisseur, d'analytique, de budget ; le motif libre ne se classe pas. |
| Un seul niveau de validation | Aucun seuil : 5 000 XAF et 5 000 000 XAF suivent le même chemin. |
| `CASH_ADVANCE` sans suite | On ne sait pas ce qui reste à justifier, ni par qui, ni depuis quand. |

## 2. Les objets

| Objet | Numéro | Rôle |
| --- | --- | --- |
| **État de besoin** | `EDB-AAAAMM-NNNNNN` | « J'ai besoin de... » : désignations, quantités, prix estimés, motif, urgence. Pas encore une demande d'argent. |
| **Demande de fonds** | `DDF-AAAAMM-NNNNNN` | « Je demande X XAF pour... » : lignes chiffrées, type, bénéficiaire, mode de remise. |
| **Décaissement** | numéro du mouvement de caisse | Remise effective de l'argent par la caissière. Une demande peut être décaissée en plusieurs fois. |
| **Justification** | `JUS-AAAAMM-NNNNNN` | Le détail des dépenses réelles d'une avance, ligne par ligne, avec pièces. |

## 3. État de besoin

### Champs

| Champ | Saisi ou automatique |
| --- | --- |
| Numéro, date | Automatique |
| Demandeur, service, fonction | Automatique (compte connecté, fiche employé) |
| Site, centre de coût | Automatique (fiche employé), modifiable |
| Objet | Saisi |
| Urgence (normale, urgente, critique) et date souhaitée | Saisi |
| Lignes : désignation, quantité, unité, prix unitaire estimé, total, nature d'opération, fournisseur suggéré | Saisi ; total calculé ; nature suggérée par les mots de la désignation |
| Total estimé et montant en lettres | Automatique |
| Budget de la section, déjà engagé, restant | Automatique (si budget activé) |
| Pièces (devis, photos) | Saisi |

### Cycle

`BROUILLON`, `SOUMIS`, `VALIDÉ` ou `REJETÉ`, puis `TRANSFORMÉ`. La transformation se fait en un clic : en **demande de fonds** (toutes les lignes reprises), en **commande fournisseur** (lot achats) ou en **achat sur petite caisse**. Un état de besoin peut se transformer en partie, ligne par ligne.

## 4. Demande de fonds

### 4.1 Types

| Type | Usage | Après le décaissement |
| --- | --- | --- |
| **Dépense directe** | Le montant est connu, la pièce existe ou existera au décaissement | Pièce jointe au décaissement, sinon justification simple attendue |
| **Avance à justifier** | Achat ponctuel au montant incertain, déplacement, mission | Justification ligne par ligne dans le délai paramétré |
| **Remboursement de frais** | Dépense déjà faite par l'employé | Pièces exigées avant validation |
| **Paiement fournisseur** | Facture fournisseur à régler en espèces | Lettrage de la facture |
| **Acompte au personnel** | Avance sur salaire | Transmis à la paie |
| **Approvisionnement de caisse** | Une caisse demande des fonds à la caisse principale ou à la banque | Transfert interne |
| **Remise de fonds** | Remettre l'excédent d'une caisse (existant `REMISE_DE_FONDS`) | Transfert ou remise en banque |

### 4.2 Formulaire intuitif

Le formulaire s'adapte au type choisi en premier (grandes tuiles avec pictogramme et une phrase d'explication).

| Champ | Saisi ou automatique |
| --- | --- |
| Demandeur, service, fonction, téléphone | **Automatique** |
| Bénéficiaire | Le demandeur par défaut ; un autre employé ou un tiers (nom, pièce d'identité, téléphone) au choix |
| Caisse de remise | **Automatique** : la caisse habituelle du site du demandeur ; modifiable |
| Solde actuel de cette caisse | **Automatique**, affiché (« la caisse ne peut pas couvrir ce montant aujourd'hui ») |
| Objet | Saisi, avec suggestions de libellés fréquents |
| Lignes : nature d'opération, désignation, montant, fournisseur, section analytique | Saisi ; nature proposée ; section **automatique** depuis la nature et le service |
| Mission (pour un déplacement) : destination, dates, nombre de jours, barème | Saisi ; montant **proposé** selon le barème (per diem, transport) |
| Total, montant en lettres | **Automatique** |
| Mode de remise | Espèces par défaut ; mobile money marchand ou virement en option |
| Date souhaitée | Saisi |
| Budget restant de la section | **Automatique** (si activé) |
| Avances non soldées du bénéficiaire | **Automatique**, avec alerte ; blocage si l'une dépasse le délai |
| Plafond applicable et niveau de validation qui sera requis | **Automatique**, affiché avant l'envoi |
| État de besoin d'origine | **Automatique** si la demande en vient |
| Pièces jointes (photo depuis le téléphone) | Saisi |

Améliorations d'usage :

- brouillon enregistré automatiquement ;
- duplication d'une demande précédente ;
- modèles (« mission Pointe-Noire 2 jours ») ;
- version mobile complète ;
- aperçu de la pièce avant envoi ;
- suivi visuel de l'avancement (frise des étapes avec noms et dates).

### 4.3 Validation

- **Niveaux** déterminés par le montant total, la nature et le type, d'après une grille paramétrable. Exemple :

  | Montant | Niveaux |
  | --- | --- |
  | Jusqu'à 50 000 XAF | Chef de service |
  | Jusqu'à 500 000 XAF | Chef de service, puis responsable financier |
  | Au-delà | Chef de service, responsable financier, puis direction |

- Chaque niveau **approuve**, **rejette** (motif obligatoire), **renvoie pour correction** (la demande revient en brouillon avec commentaire), ou **réduit le montant** (avec motif).
- **Délégation** en cas d'absence, avec date de fin ; **rappel** au valideur après N heures ; **escalade** après N jours.
- Le demandeur ne valide jamais sa propre demande, même s'il a le niveau.
- Validation depuis le courriel (lien signé vers l'écran) et depuis le téléphone.
- Toute décision est datée et signée électroniquement, et figure sur la pièce imprimée.

### 4.4 Décaissement

- La demande approuvée apparaît dans la file « **À décaisser** » de la caisse choisie, triée par date souhaitée.
- La caissière ouvre la demande, vérifie l'identité du bénéficiaire, saisit le montant remis (total ou partiel), et fait signer le bénéficiaire (signature sur tablette, ou pièce imprimée signée puis scannée).
- Le système crée le mouvement `CASH_OUT` avec la nature, le journal, l'analytique et la référence de la demande. L'écriture comptable suit (`421` pour une avance, charge pour une dépense directe).
- La pièce de décaissement s'imprime aussitôt (voir [pieces-comptables.md](pieces-comptables.md)).
- Décaissement **refusé** si la caisse n'a pas les fonds : la demande reste à décaisser et un approvisionnement est proposé.
- Une demande approuvée non décaissée **expire** après N jours (paramètre), ce qui libère l'engagement budgétaire.

### 4.5 Cycle de vie complet

```
BROUILLON → SOUMISE → EN_VALIDATION (niveau 1..n) → APPROUVÉE → DÉCAISSÉE_PARTIELLEMENT → DÉCAISSÉE
                 │              │                                                         │
                 │              ├→ RENVOYÉE (retour en BROUILLON)                        ├ dépense directe : SOLDÉE
                 │              └→ REJETÉE                                               └ avance : À_JUSTIFIER → PARTIELLEMENT_JUSTIFIÉE → JUSTIFIÉE → SOLDÉE
                 └→ ANNULÉE (par le demandeur, avant décaissement)
APPROUVÉE sans décaissement après N jours → EXPIRÉE
```

## 5. Justification des dépenses, une par une

Cas visés : achat ponctuel dont le montant exact n'était pas connu, déplacement, mission. L'argent est sorti sous forme d'**avance** (compte `421` du bénéficiaire) et la justification vient **après**, dépense par dépense.

### 5.1 Saisie d'une ligne

| Champ | Saisi ou automatique |
| --- | --- |
| Date de la dépense | Saisi (contrôle : dans la période de la mission ou de l'avance) |
| Nature d'opération | Saisi, proposée d'après les lignes de la demande |
| Fournisseur (ou « particulier ») | Saisi, recherche dans les fournisseurs connus |
| Désignation | Saisi |
| Montant TTC | Saisi |
| TVA récupérable | **Automatique** selon la nature ; ajustable si la facture porte la TVA |
| Section analytique | **Automatique** depuis la demande |
| Pièce (photo du ticket, de la facture) | Saisi, **obligatoire** sauf nature dispensée (taxi sans reçu, plafond par ligne) avec attestation sur l'honneur |
| Ligne de la demande couverte | **Automatique**, modifiable |

Ce qui reste à justifier se recalcule en direct : **avance**, moins **justifié**, égale **reste**.

### 5.2 Qui saisit, qui valide

- Le **bénéficiaire** saisit depuis son espace ou son téléphone, au fil de l'eau, même pendant la mission.
- La **caissière** peut saisir pour lui au guichet.
- Le **comptable** valide chaque ligne (ou le lot) : il accepte, rejette (motif) ou corrige la nature, la TVA ou l'analytique. **Chaque ligne validée produit son écriture** : D charge, D TVA, C `421`.
- Une ligne rejetée reste à la charge du bénéficiaire : il la remplace ou rembourse.

### 5.3 Solde de l'avance

| Situation | Action proposée | Écriture |
| --- | --- | --- |
| Justifié = avance | Clôture | Aucune de plus |
| Justifié < avance | **Retour du reliquat** au guichet : bon d'entrée prérempli | D `571x` ; C `421` |
| Justifié > avance | **Complément** à décaisser, après validation | D `421` ; C `571x` |
| Reliquat non rendu au-delà du délai | Alerte responsable ; option retenue sur salaire | Le `421` reste ouvert, transmis à la paie |

### 5.4 Délais et relances

- Délai de justification par type (ex. 7 jours après la fin de la mission, 3 jours pour un achat ponctuel).
- Relances automatiques au bénéficiaire à J-1, J+0, J+3, puis à son responsable.
- **Blocage** : aucune nouvelle avance pour une personne qui a une avance en retard (paramétrable : alerte ou blocage).
- Anomalie de caisse ajoutée au détecteur existant : « avance non justifiée depuis plus de N jours ».

### 5.5 États de suivi

- **Avances ouvertes** par bénéficiaire, ancienneté, montant restant.
- **Justifications en attente de validation** par comptable.
- **Dépenses par nature et par section** issues des justifications.
- Fiche de justification imprimable (voir [pieces-comptables.md](pieces-comptables.md#6-fiche-de-justification-davance)).

## 6. Notifications

| Événement | Destinataires |
| --- | --- |
| Demande soumise | Valideur du niveau 1 |
| Niveau validé | Niveau suivant ; le demandeur suit l'avancement |
| Demande renvoyée ou rejetée | Demandeur, avec motif |
| Demande approuvée | Demandeur (« présentez-vous à la caisse X ») et caissière (file à décaisser) |
| Décaissement effectué | Demandeur, bénéficiaire |
| Justification attendue, relances | Bénéficiaire, puis responsable |
| Justification validée ou rejetée | Bénéficiaire |
| Avance soldée | Bénéficiaire, comptable |

Canaux existants : courriel, notification de l'application, temps réel (WebSocket).

## 7. Reprise des demandes existantes

- `cash_request` reste en lecture (historique, PDF existant).
- Les demandes `PENDING` à la bascule sont recréées en demandes `SOUMISE` avec leur numéro d'origine en référence.
- Les avances `CASH_ADVANCE` exécutées et non soldées sont reprises en avances `À_JUSTIFIER` avec le montant décaissé.
