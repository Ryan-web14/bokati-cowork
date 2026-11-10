# Réservation multi-ressources (back-office)

Réserver en une fois **plusieurs ressources** pour **un ou plusieurs membres**. Chaque membre reçoit **une seule facture** pour l'ensemble de ses réservations du lot, au lieu d'une facture par réservation.

Back-office seulement : pas de route portail. Droits : `BOOKING:CREATE` pour créer, `BOOKING:READ` pour lire.

## Routes

| Méthode | Route | Rôle |
|---|---|---|
| `POST` | `/sni/api/v1/bookings/batches` | Créer un lot. Réponse `201` : le lot, ses factures et ses réservations. |
| `GET` | `/sni/api/v1/bookings/batches?page=0&size=20` | Liste des lots, du plus récent au plus ancien (résumés, sans réservations). `size` 1 à 100. |
| `GET` | `/sni/api/v1/bookings/batches/{batchNumber}` | Un lot : factures à jour (statut, payé, reste dû) et réservations. |

## Corps de création

```json
{
  "memberIds": ["MBR-COM-202611-00000012", "MBR-COM-202611-00000013"],
  "lines": [
    { "resourceCode": "SALLE-REUNION-A", "startedAt": "2026-11-20T09:00:00", "endedAt": "2026-11-20T12:00:00" },
    { "resourceCode": "BUREAU-B2", "startedAt": "2026-11-20T09:00:00", "endedAt": "2026-11-20T18:00:00", "quantity": 1 },
    { "resourceCode": "PARKING-P1", "startedAt": "2026-11-20T08:00:00", "endedAt": "2026-11-20T19:00:00",
      "memberIds": ["MBR-COM-202611-00000012"] }
  ],
  "paymentMode": "DIRECT",
  "billingCompanyCode": null,
  "confirmImmediately": true,
  "sendEmail": true,
  "notes": "Séminaire équipe commerciale",
  "idempotencyKey": "lot-seminaire-2026-11-20"
}
```

- `memberIds` du lot : les membres de **toutes** les lignes. Une ligne qui a ses propres `memberIds` les utilise **à la place** (la ligne parking ci-dessus ne concerne qu'un membre).
- Chaque couple ligne et membre devient une réservation : l'exemple en crée 2 + 2 + 1 = 5.
- Champs d'une ligne : `resourceCode`, `startedAt`, `endedAt` (obligatoires), `quantity` (défaut 1), `bookingUnit`, `memberIds`, `notes` (sinon celles du lot), `participants` (même forme que la réservation seule).
- Champs du lot : `paymentMode` obligatoire (`DIRECT`, `SUBSCRIPTION` ou `PASS`), puis `billingCompanyCode`, `confirmImmediately`, `sendEmail`, `notes` et `idempotencyKey` (même sens que pour une réservation seule, voir le [README](README.md#créer-une-réservation-back-office)).
- Au plus 50 lignes et 200 réservations par lot.
- `paymentMode: "WALLET"` est **refusé** (`400`) : le portefeuille règle chaque réservation au moment du blocage des fonds, ce qui ne marche pas avec une facture commune. Réserver une par une dans ce cas.
- Dates sans fuseau, comme pour une réservation seule.

## Tout ou rien

Chaque réservation passe exactement les mêmes contrôles qu'une réservation seule : disponibilité, politique, droits d'abonnement ou de pass, tarif. Si **une seule** échoue, **rien** n'est créé ni facturé. La réponse est alors un `400` `VALIDATION_FAILED` dont `errors` liste chaque réservation impossible :

```json
{
  "errorCode": "VALIDATION_FAILED",
  "message": "Réservation multi-ressources refusée : 1 réservation(s) sur 5 impossible(s). Rien n'a été créé.",
  "errors": [
    "Ligne 1 (SALLE-REUNION-A, 20/11/2026 09:00 - 20/11/2026 12:00), membre MBR-COM-202611-00000013 : ..."
  ]
}
```

Afficher `errors` sous le formulaire et laisser corriger la ligne fautive (autre créneau, autre ressource, retrait du membre). Une ligne sans aucun membre est aussi refusée de cette façon, avant toute tentative.

Le contrôle de capacité tient compte des réservations précédentes du même lot : deux membres sur une salle à capacité 1, au même créneau, font échouer la seconde.

## Réponse

```json
{
  "batchNumber": "BKL-202611-000001",
  "paymentMode": "DIRECT",
  "billingCompanyCode": null,
  "bookingCount": 5,
  "memberCount": 2,
  "notes": "Séminaire équipe commerciale",
  "createdBy": "admin@sni-cg.com",
  "createdAt": "2026-11-10T16:40:12.345Z",
  "invoices": [
    { "documentNumber": "FAC-2026-00123", "status": "ISSUED", "customerType": "MEMBER",
      "customerCode": "MBR-COM-202611-00000012", "customerName": "Grace Mavoungou",
      "totalAmount": 85000, "paidAmount": 0, "balanceDue": 85000, "currency": "XAF",
      "bookingNumbers": ["BKG-...", "BKG-...", "BKG-..."] }
  ],
  "uninvoicedBookingNumbers": [],
  "bookings": [ { "bookingNumber": "BKG-...", "batchNumber": "BKL-202611-000001", "...": "même forme que GET /bookings/{bookingNumber}" } ]
}
```

- `invoices` : une facture par membre (plus exactement, par titulaire, organisation payeuse et devise). Elle liste une ligne par réservation et part par courriel au membre comme une facture de réservation seule.
- `uninvoicedBookingNumbers` : réservations sans facture pour l'instant. C'est le cas d'une ressource **soumise à approbation** : sa réservation reste `PENDING_APPROVAL` et reçoit sa propre facture à l'approbation, hors de la facture commune.
- Une réservation en `SUBSCRIPTION` ou `PASS` figure dans la facture commune pour un montant nul, comme aujourd'hui pour une réservation seule.
- Chaque réservation porte désormais `batchNumber` (aussi dans `GET /bookings/{bookingNumber}`) : `null` pour une réservation seule. S'en servir sur la fiche pour renvoyer vers le lot.

## Après la création

- **Paiement** : en `DIRECT`, les réservations sont `PENDING_PAYMENT`. L'encaissement se fait sur la facture du membre (modules `billing` et `payment`). Quand elle est **entièrement** payée, **toutes** les réservations qu'elle couvre passent `CONFIRMED`. Un paiement partiel ne confirme rien.
- **Annuler une réservation du lot** (route habituelle `PATCH /bookings/{bookingNumber}/cancel`) : seule sa part est retirée de la facture commune.
  - Rien d'encaissé : un avoir de son montant réduit le reste dû ; si c'était la dernière réservation encore facturée, la facture est annulée.
  - Déjà encaissé : la politique d'annulation s'applique à la part de cette réservation (avoir et remboursement), les autres réservations restent.
- **Annuler la facture commune** (module `billing`) : annule **toutes** ses réservations.
- **Remboursement total** de la facture : annule toutes ses réservations.
- Recharger `GET /bookings/batches/{batchNumber}` après une action pour voir les statuts et soldes à jour.

## Écran conseillé

1. Choix des membres (multi-sélection), puis des lignes : ressource, créneau, quantité, et en option « seulement pour » (membres de la ligne).
2. Aperçu avant envoi : nombre de réservations = somme sur les lignes du nombre de membres. Les prix ne sont pas calculés côté écran ; `POST /bookings/check-availability` peut chiffrer chaque ligne si besoin.
3. Envoi avec une `idempotencyKey` générée à l'ouverture du formulaire, pour qu'un double clic ne crée pas deux lots.
4. Résultat : une carte par facture (membre, total, reste dû, bouton d'encaissement), puis la liste des réservations.
