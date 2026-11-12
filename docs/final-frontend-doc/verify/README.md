# verify : fonctionnement

> Rédigé d'après le code. Routes exactes, droits et champs JSON : [routes.md](routes.md) et `routes/`. Règles communes : [README racine](../README.md).

## En bref

Pages publiques, sans jeton, qui confirment qu'un document de facturation ou un reçu de paiement a bien été émis par la plateforme. Ce sont des **pages HTML rendues par le serveur** (Thymeleaf, `src/main/resources/templates/verify/`), pas des routes JSON : elles sont ouvertes par le QR code imprimé sur les PDF. Le module lit les données des modules `billing` (factures, avoirs, devis…) et `payment` (reçus) ; il n'a ni table ni statut propres. Le frontend n'a en général rien à appeler ici : au plus un lien ou un formulaire de comparaison.

## Notions

| Objet | Identifiant dans l'URL | Source |
| --- | --- | --- |
| Document de facturation | `documentNumber` (numéro lisible du document, séquentiel donc énumérable) | `BillingDocumentService.get` |
| Reçu de paiement | `receiptNumber` | `PaymentReceiptService.getByReceiptNumber` |
| Clé de détention `k` | 16 premiers caractères hexadécimaux de la signature fiscale du document | `VerifyController.SIGNATURE_PREFIX_LENGTH` |

L'URL encodée dans le QR code :

- Document : `<app.verify-base-url>/verify/doc/<documentNumber>`, suivi de `?k=<16 caractères>` **seulement** si le document est verrouillé et porte un numéro et une signature fiscaux (`src/main/java/com/sni/bokaticowork/features/billing/service/fiscal/FiscalQrCodeService.java:35`).
- Reçu : `<app.verify-base-url>/verify/receipt/<receiptNumber>`, sans `k` (`src/main/java/com/sni/bokaticowork/features/payment/service/implementation/PaymentReceiptServiceImpl.java:315`).

`app.verify-base-url` vaut `APP_BASE_URL` en production (`application-prod.yml:212`) : c'est l'adresse du **serveur**, pas celle du frontend.

## Statuts et cycle de vie

Pas d'objet à statut propre. La page affiche un libellé de statut volontairement grossier, calculé depuis le statut du document de facturation (`VerifyController.java:169`) :

| Statut du document (`billing`) | Libellé affiché | Bandeau |
| --- | --- | --- |
| `CANCELLED`, `VOIDED` | Annulé | « Document annulé, il ne vaut plus » |
| `DRAFT` | Brouillon | « Document authentique » |
| tout autre, ou absent | Émis | « Document authentique » |

Un reçu est toujours affiché « Encaissé ».

## Parcours

### Vérifier un document depuis son QR code

1. Le scanner ouvre `GET /verify/doc/{documentNumber}?k=...` ([détail](routes/VerifyController.md#r-get-verify-doc-documentnumber)).
2. Le serveur charge le document. S'il est introuvable (ou sur toute autre exception), il rend la page `verify/not-found` (« Cette référence n'existe pas »).
3. Sinon, il compare `k` au préfixe de la signature fiscale, sans tenir compte de la casse et à temps constant (`VerifyController.java:132`) :
   - `k` correct : vue **détaillée** (référence, type de document, date d'émission, statut, nom du client, montant total formaté, devise) ;
   - `k` absent, faux, ou document sans signature : vue **minimale** (référence, type, date, statut) avec la mention « Seules la référence, la date et le statut sont publics ».

Types affichés : Facture, Facture proforma, Facture rectificative, Avoir, Note de débit, Devis, sinon « Document » (`VerifyController.java:150`).

### Vérifier un reçu

1. `GET /verify/receipt/{receiptNumber}` ([détail](routes/VerifyController.md#r-get-verify-receipt-receiptnumber)). Le paramètre `k` est accepté mais ignoré : un reçu n'a pas de signature fiscale.
2. Vue toujours minimale : numéro, « Reçu de paiement », date d'émission du reçu, « Encaissé ». Reçu introuvable : page `not-found` avec le type « reçu ».

### Comparer un PDF détenu à l'original

Sert à trancher un litige : « ce fichier est-il celui qui a été émis ? ».

1. `POST /verify/doc/{documentNumber}/compare` en `multipart/form-data`, partie `file` = le PDF ([détail](routes/VerifyController.md#r-post-verify-doc-documentnumber-compare)). Aucune page du module ne contient de formulaire pour cet envoi : si le produit en veut un, c'est au frontend de le fournir (formulaire HTML classique `<form method="post" enctype="multipart/form-data" action="<url serveur>/verify/doc/<n>/compare">`, la réponse étant une page HTML complète).
2. Le serveur calcule le SHA-256 du fichier reçu et le compare à l'empreinte enregistrée au scellement (`pdfSha256`) (`src/main/java/com/sni/bokaticowork/features/billing/service/implementation/BillingDocumentPdfServiceImpl.java:136`).
3. Page `verify/comparison`, quatre issues :
   - **Conforme** : empreintes identiques ;
   - **Copie filigranée** : empreintes différentes, mais le document est `CANCELLED` ou `VOIDED` (la copie téléchargée porte un filigrane, elle ne peut pas correspondre) ;
   - **Non conforme** : empreintes différentes, document non annulé ;
   - **Non scellé** : le document n'a pas d'empreinte (émis avant le scellement), aucune comparaison possible.
   La page affiche aussi les deux empreintes et la date de comparaison. Aucune donnée du document n'est révélée.
4. Document introuvable ou fichier illisible : page `not-found`.

## Règles et erreurs à refléter dans l'écran

- Les trois routes renvoient du **HTML**, avec le statut `200` y compris pour « introuvable » (la vue `not-found` est rendue normalement, `VerifyController.java:119`). Ne pas les appeler en `fetch` pour lire du JSON.
- Plafonds par adresse IP (`src/main/java/com/sni/bokaticowork/security/ratelimit/RateLimitRules.java:128`) : `/verify/**` 60 requêtes par 10 minutes (`app.security.rate-limit.verify.*`) ; `/verify/.../compare` 5 envois par 10 minutes (`app.security.rate-limit.upload.*`). Au-delà : `429` au format JSON décrit dans le [README racine](../README.md#erreurs).
- Fichier de comparaison : 10 Mo au plus (règle commune). Partie `file` absente : erreur de requête générée par Spring avant le contrôleur (non vérifié : forme exacte de la réponse, probablement le JSON `MISSING_PARAMETER` ou `BAD_REQUEST`).
- Le préfixe `k` fait exactement 16 caractères ; une autre longueur donne la vue minimale, sans message d'erreur.

## À prévoir côté frontend

- Rien d'obligatoire : ces pages sont autonomes et servies par le backend sous `/verify/**`, sans le préfixe `/sni/api/v1` et hors CORS d'API.
- Un écran « Vérifier un document » éventuel peut proposer un champ de saisie du numéro qui redirige vers `<serveur>/verify/doc/<numéro>` (sans `k`, la vue sera minimale), et un formulaire d'envoi de PDF vers la route de comparaison.
- Ne pas fabriquer de lien `?k=` côté frontend pour un tiers : `k` prouve la détention du document. Le QR code imprimé sur le PDF porte déjà le bon lien.
- Les liens pointent vers l'adresse du serveur (`app.verify-base-url`), pas vers le domaine du frontend.

## Effets automatiques

Aucun. Les pages ne journalisent rien et n'envoient ni courriel ni notification.

## Écarts relevés

- `src/main/resources/templates/verify/document.html:17` : la vue affiche « L'empreinte correspond à celle enregistrée à l'émission » même en vue minimale, alors qu'aucune empreinte n'a été vérifiée (seule l'existence du numéro l'a été). Même bandeau « Document authentique » pour un document au statut `DRAFT` ou pour un devis.
- `src/main/resources/templates/verify/receipt.html` n'est rendu par aucun contrôleur (le reçu utilise `verify/document`). Ce gabarit affiche le téléphone du payeur, le numéro de transaction et le dépôt : s'il était rebranché, il exposerait ces données sans jeton sur un numéro énumérable.
- `VerifyController.java:58`, `:88`, `:103` : toute exception (y compris une panne de base ou de stockage) est rendue comme « Cette référence n'existe pas », avec un statut `200`. Un document réel peut donc être déclaré inexistant.
- `VerifyController.java:185` : la date d'un reçu est convertie avec `ZoneId.systemDefault()` et non le fuseau de l'application (`app.time-zone`) ; autour de minuit, la date affichée peut différer d'un jour de celle du reçu.
