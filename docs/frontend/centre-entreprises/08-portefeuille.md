# Portefeuille de l'organisation

Préfixe des routes : `/sni/api/v1`.

L'organisation a son portefeuille (`ownerType = CUSTOMER`, un par devise, `XAF` par défaut), avec les
règles de plafonds et de sécurité d'un portefeuille de membre. Une recharge produit un reçu, pas une
facture.

- **Recharge.** Le gérant recharge par mobile money depuis son portail. L'administration saisit les
  espèces reçues à l'accueil ou un virement constaté (`method` `CASH` ou `BANK_TRANSFER`) ; au-delà du
  seuil des actions d'administration, une seconde personne valide.
- **Code secret.** Le gérant fixe le code du portefeuille de l'organisation ; il le donne quand il
  règle lui-même une facture avec ce portefeuille.
- **Ce que chacun voit.** Le gérant voit le solde, les mouvements (avec le collaborateur de chaque
  dépense) et les validations en attente. Un collaborateur ne voit que son autorisation et ses
  propres dépenses, jamais le solde.

## Dépenses autorisées

Le gérant (ou l'administration) autorise un collaborateur rattaché à dépenser sur le portefeuille.

| Champ | Rôle |
| --- | --- |
| `operations` | `BOOKING`, `SUBSCRIPTION`, `PASS`, `INVOICE_PAYMENT`, `MARKETPLACE` |
| `maxPerTransaction`, `maxPerDay`, `maxPerMonth` | plafonds, chacun facultatif |
| `approvalAbove` | au-delà, la dépense attend la validation du gérant (vide : jamais) |
| `validFrom`, `validUntil`, `status` | période ; `ACTIVE`, `SUSPENDED` ou `REVOKED` |

- Le collaborateur règle une facture de l'organisation, ou reliée à elle. L'opération se lit sur la
  facture : réservation, abonnement (dépassement compris), pass, sinon facture.
- Sans autorisation en vigueur couvrant l'opération, ou au-delà d'un plafond, refus (400) ; le
  message dit lequel bloque. Les plafonds du jour et du mois comptent les dépenses réglées et
  celles qui attendent.
- Au-delà de `approvalAbove`, la dépense attend (`PENDING_APPROVAL`) : les fonds sont retenus sur le
  portefeuille et le gérant est prévenu. Il accepte ou refuse ; le collaborateur peut retirer sa
  demande. Sans réponse sous 48 heures, elle est annulée (`EXPIRED`) et les fonds rendus. Une seule
  demande en attente par facture.
- À l'acceptation, la facture est réglée pour ce qu'elle doit encore ; déjà réglée entre-temps, rien
  n'est prélevé.
- Le paiement crée une intention au nom de l'organisation, la facture en source ; le collaborateur
  est noté comme celui qui a dépensé (`spentByMember`, et auteur de l'écriture du portefeuille).
- La fin du rattachement suspend ses autorisations et annule ses demandes en attente.
- Une autorisation suspendue ou révoquée annule les demandes en attente du collaborateur.
- Le gérant reçoit un e-mail (et une notification dans le portail) à chaque demande de validation ;
  le collaborateur, à chaque décision ou annulation.

## Codes

- **Code de dépense du collaborateur.** Le code du portefeuille reste celui du gérant. Chaque
  collaborateur autorisé pose son propre code (`POST /client/me/company-wallets/{customerId}/pin`,
  `{ currentPin?, newPin }`). Il est exigé quand la règle de sécurité du portefeuille demande un
  code pour payer une facture ; il se passe dans `{ pin }` du règlement. Mêmes règles qu'un code de
  portefeuille : longueur de la règle, ni répétition ni suite, blocage après trop d'essais
  (`maxFailedAttempts`, `lockoutMinutes` de la règle). Le gérant ou l'administration l'efface
  (`.../spending-authorizations/{id}/pin/reset`) ; le collaborateur en pose alors un nouveau sans
  l'ancien. `pinSet` dans l'autorisation dit s'il en a un.
- **Code à usage unique.** Au-delà du seuil OTP de la règle de sécurité du portefeuille, le
  règlement (collaborateur ou gérant) ne prélève rien : il répond `AWAITING_OTP` et un code de six
  chiffres part par e-mail à celui qui paie. Il se saisit sous 10 minutes, trois essais au plus
  (`.../spends/{id}/confirm`, `{ code }`) ; ensuite la dépense suit son cours (validation du gérant
  au-delà du seuil, sinon règlement). Échu ou après trois erreurs, la dépense expire et il faut
  recommencer.

## Réservation prépayée pour l'organisation

Une réservation en mode `WALLET` faite pour une organisation (`billingCompanyCode`) puise dans le
portefeuille de l'organisation quand le membre y a une autorisation en vigueur couvrant `BOOKING`.
Les plafonds s'appliquent et la retenue compte dedans (`HELD`) jusqu'au règlement de la facture,
au nom de l'organisation. Au-delà du seuil de validation, le prépaiement est refusé (une
réservation ne peut pas attendre le gérant pour être confirmée) : réserver en paiement direct puis
régler la facture avec le portefeuille de l'organisation. Sans autorisation, rien ne change : le
portefeuille personnel du membre paie. Une réservation annulée rend la retenue et annule la
dépense.

| Qui | Méthode et chemin | Effet |
| --- | --- | --- |
| Administration | `GET /companies/{customerId}/wallet?currency=` | solde, mouvements avec le collaborateur, validations en attente |
| Administration | `GET /companies/{customerId}/wallet/spends?status=` | les dépenses |
| Administration | `POST /companies/{customerId}/wallet/top-ups` | `{ amount, currency?, method, reference?, reason }` |
| Administration et gérant | `GET`, `POST /companies/{customerId}/spending-authorizations` | autorisations ; crée `{ memberId, operations, maxPerTransaction?, maxPerDay?, maxPerMonth?, approvalAbove?, validFrom?, validUntil?, currency? }` |
| Administration et gérant | `PATCH /companies/{customerId}/spending-authorizations/{id}` | remplace opérations, plafonds et période ; `status` et `reason` facultatifs |
| Gérant | `GET /client/companies/{customerId}/wallet` (et `/wallet/spends`) | comme l'administration |
| Gérant | `POST /client/companies/{customerId}/wallet/top-ups` | mobile money `{ amount, phoneNumber, correspondent }` |
| Gérant | `POST /client/companies/{customerId}/wallet/security/pin` | `{ currentPin?, newPin }` |
| Gérant | `POST /client/companies/{customerId}/invoices/{n}/pay/wallet` | règle avec le portefeuille `{ pin? }` |
| Gérant | `POST /client/companies/{customerId}/wallet/spends/{id}/confirm` | saisit le code à usage unique `{ code }` |
| Administration et gérant | `POST .../spending-authorizations/{id}/pin/reset` | efface le code de dépense du collaborateur |
| Gérant | `GET /client/companies/{customerId}/approvals` | dépenses en attente |
| Gérant | `POST /client/companies/{customerId}/approvals/{id}/approve`, `/reject` | accepte, ou refuse `{ reason? }` |
| Collaborateur | `GET /client/me/company-wallets` | ses autorisations et ses dépenses |
| Collaborateur | `POST /client/me/company-wallets/{customerId}/invoices/{n}/pay` | règle avec le portefeuille de l'organisation `{ pin? }` |
| Collaborateur | `POST /client/me/company-wallets/{customerId}/spends/{id}/confirm` | saisit le code à usage unique `{ code }` |
| Collaborateur | `POST /client/me/company-wallets/{customerId}/pin` | pose ou change son code de dépense |
| Collaborateur | `POST /client/me/company-wallets/{customerId}/spends/{id}/cancel` | retire sa demande en attente |

Côté gérant, les autorisations sont sous `/client/companies/{customerId}/spending-authorizations`.
`id` est le numéro court de l'autorisation ou de la dépense. Le résultat d'un règlement :

```json
{
  "spend": { "id": 12, "memberId": "MBR-...", "operation": "BOOKING", "amount": 80000, "currency": "XAF",
             "documentNumber": "INV-...", "status": "PENDING_APPROVAL", "expiresAt": "2026-11-02T10:00:00Z" },
  "message": "Au-delà de 50000 XAF, la dépense attend la validation du gérant. ..."
}
```
