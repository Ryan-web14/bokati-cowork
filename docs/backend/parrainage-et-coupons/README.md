# Parrainage et lots de coupons

Chantier du 2026-11-09. Commits `d7d7b6f` (coupons), `18e3e63` (parrainage), `623a70b` (doc frontend).
Doc d'écran détaillée : [docs/final-frontend-doc/subscription/parcours-promotions.md](../../final-frontend-doc/subscription/parcours-promotions.md).

## Le constat

- **Coupons** : un lot se générait (`POST /coupons/batches`), mais rien ne permettait de le relire : ni liste
  des lots, ni codes générés, ni compteurs, ni export pour le partenaire.
- **Parrainage** : le module existait (modèle, qualification, récompenses) mais ne servait à rien :
  - aucune route ne créait de programme, et aucun n'existait en base ;
  - l'inscription n'acceptait pas de code de parrainage ;
  - la règle par défaut, « récompense au premier paiement du filleul », n'était appelée par aucun code ;
  - la page d'un lien partagé exigeait une connexion d'administrateur ;
  - le membre n'avait aucun moyen d'obtenir son lien.

## Ce qui a été fait

### Coupons (`features/subscription/promotion/pricing/coupon`)

| Route | Rôle |
| --- | --- |
| `GET /coupons/batches?promotionCode=&page&size` | Lots, du plus récent au plus ancien. |
| `GET /coupons/batches/{batchCode}` | Un lot et ses compteurs par statut, calculés sur les codes (`coupon_batch.redeemed_quantity` n'est tenu à jour par personne). |
| `GET /coupons/batches/{batchCode}/coupons?status=&page&size` | Les codes du lot, triés par code. |
| `GET /coupons/batches/{batchCode}/export` | CSV des codes (BOM, `;`, même échappement que les autres exports). |
| `POST /coupons/batches/{batchCode}/revoke?reason=` | Révoque les codes encore utilisables ; épargne les déjà révoqués, épuisés, expirés. |
| `GET /coupons/{code}` | Un code. |

`POST /coupons/batches` garde ses trois clés et ajoute `batch`, la vue du lot.

### Parrainage (`features/subscription/promotion/pricing/referral`)

- `ReferralProgramService` (nouveau) : création, modification, ouverture, pause, clôture des programmes ;
  liens (back-office et espace client) ; listes ; page publique ; vérification d'un code à l'inscription.
  Toutes les vues sont construites dans la transaction du service.
- `ReferralService` (existant) garde l'enregistrement, la qualification et les récompenses.
- Contrôleurs : `ReferralController` (back-office, routes ajoutées), `ClientReferralController`
  (`/client/referrals`, le membre connecté est toujours le parrain), `PublicReferralController`
  (`/public/referrals/{code}`, sans connexion, limité en débit comme tout `/public/**`).
- Inscription : `RegisterMemberRequest.referralCode`. `AuthenticationServiceImpl.register` vérifie le code
  **avant** de créer le compte (un code faux renvoie `400`, rien n'est créé), puis enregistre le filleul.
- Premier paiement : `PaymentTransactionWorkflowProcessor` publie `RefereePaidEvent` à chaque paiement
  réussi. `ReferralPaymentListener` l'écoute **après la validation** du paiement, dans sa propre
  transaction, et attrape toute erreur.

### Règles retenues

- Un programme doit prévoir au moins une récompense, dès sa création : la base l'impose aussi
  (`ck_referral_program_rewards`).
- Une fois le programme ouvert, sa règle de qualification et ses récompenses ne changent plus : les
  parrainages en cours ont été promis avec elles. Nom, description, dates, plafond et devise restent modifiables.
- Un seul lien par parrain et par programme (règle déjà en place).
- Le parrain ne voit de son filleul que le prénom et l'initiale du nom (« Awa N. »), et seulement sa propre récompense.
- La qualification au premier paiement ne s'exécute jamais dans la transaction du paiement. La
  récompense crédite des portefeuilles, et le paiement peut tenir le verrou de celui du filleul (un
  rechargement par exemple) : dans la même transaction, l'attente serait sans fin. Si elle échoue, le
  paiement n'en souffre pas ; elle se rejoue au paiement suivant, car seuls les parrainages en attente sont repris.

### Droits

`/coupons/**` et `/referrals/**` relèvent maintenant des droits de la facturation, comme `/promotions` :
`BILLING:READ` (GET), `BILLING:CREATE` (POST), `BILLING:UPDATE` (PUT). Auparavant, aucune règle de chemin
ne les couvrait et seul `ADMIN:ACCESS` était exigé. Un compte qui n'a que `ADMIN:ACCESS` n'y accède plus.

### Réglage

| Clé | Défaut | Rôle |
| --- | --- | --- |
| `app.referral.share-url-template` | `https://elleaose.com/inscription?parrain={code}` | Adresse d'un lien partagé ; `{code}` est remplacé. À faire pointer vers la vraie page d'inscription du frontend. |

## Tests

- `CouponBatchServiceTest` (9) et `CouponBatchRepositoryIT` (4).
- `ReferralProgramServiceTest` (7) : brouillon à la création, refus d'un code pris ou d'une ancienneté sans délai, ouverture qui exige une récompense, règles figées une fois ouvert, lien du membre, page publique, écouteur de paiement qui n'échoue jamais.
- `RegisterWithReferralTest` (3) : code vérifié avant la création du compte, refus sans écriture, inscription sans code.
- `PaymentTransactionWorkflowProcessorTest` : l'événement du payeur est publié.
- `ReferralQueriesIT` : liste filtrable (filtres facultatifs), liens et parrainages d'un parrain, comptes par statut.

## Pour essayer

1. Créer une promotion de récompense (par exemple un crédit de portefeuille) dans `/promotions`.
2. Console de test, écran « Parrainage · administration » : créer un programme avec cette promotion, puis l'ouvrir.
3. Se connecter avec un membre, écran « Parrainage · portail » : obtenir son lien.
4. S'inscrire avec `POST /auth/register` et `referralCode`, puis faire un premier paiement avec ce nouveau
   membre. Le parrainage passe en `REWARDED`, et le crédit apparaît dans le portefeuille du parrain.

## Limites

- L'inscription d'un membre par le back-office (`POST /members`) ne prend pas de code de parrainage :
  pour un filleul déjà client, utiliser `POST /referrals`.
- Le lien d'une organisation (`referrerType=CUSTOMER`) se crée depuis le back-office ; il n'y a pas
  d'espace « parrainage » côté organisation.
