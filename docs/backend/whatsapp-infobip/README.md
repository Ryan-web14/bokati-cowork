# Envoi WhatsApp par Infobip

Service commun d'envoi de messages WhatsApp sortants, en texte seul, par l'API d'Infobip. Premier usage : les confirmations de solde de la clôture (lot F12). Branche `feature/finance-comptabilite`.

## Principes

- **Aucune donnée exposée** : pas de lien public pour les destinataires. Le destinataire répond sur WhatsApp ; l'équipe saisit sa réponse.
- **Une seule entrée ouverte** : le webhook des statuts appelé par Infobip (remise, lecture). Il refuse tout appel sans signature HMAC valide et ne renvoie aucune donnée.
- **Texte seul** : pas de PDF. Infobip n'accepte un document que par une URL qu'il télécharge, ce qui exposerait le fichier. La lettre complète part par courriel ou s'imprime.
- **Modèles** : hors des 24 heures qui suivent un message du destinataire, Meta n'accepte qu'un modèle de message approuvé. Chaque modèle sert un **objet** (`purpose`, par exemple `BALANCE_CONFIRMATION`) dans une langue.
- **Texte libre** : seulement si le paramètre `free-text-allowed` est vrai et qu'aucun modèle approuvé n'existe. Meta ne le délivre que dans la fenêtre de 24 heures.

## Configuration (`bokati.whatsapp.infobip`, variables d'environnement)

| Clé | Variable | Défaut |
| --- | --- | --- |
| `enabled` | `INFOBIP_WHATSAPP_ENABLED` | `false` |
| `base-url` | `INFOBIP_BASE_URL` | adresse propre au compte, `xxxxx.api.infobip.com` |
| `api-key` | `INFOBIP_API_KEY` | à mettre dans `.env` ou la configuration de l'hébergeur, jamais dans le dépôt |
| `sender` | `INFOBIP_WHATSAPP_SENDER` | numéro WhatsApp expéditeur, international sans `+` |
| `language` | `INFOBIP_WHATSAPP_LANGUAGE` | `fr` |
| `free-text-allowed` | `INFOBIP_WHATSAPP_FREE_TEXT` | `false` |
| `webhook-secret` | `INFOBIP_WEBHOOK_SECRET` | secret HMAC (24 caractères au moins), transmis à Infobip à la création de l'abonnement |
| `webhook-base-url` | `INFOBIP_WEBHOOK_BASE_URL`, à défaut `APP_BASE_URL` | adresse publique https de l'API |
| `webhook-algorithm` | | `HMAC_SHA_256` |
| `portal-url` | `INFOBIP_WHATSAPP_PORTAL_URL` | `https://espace.elleaose.com`, base des boutons de lien |
| `subscription-id`, `profile-id`, `auth-id` | | `BOKATI-WHATSAPP`, `BOKATI-WHATSAPP-PROFILE`, `BOKATI-WHATSAPP-HMAC` |

Désactivé ou incomplet, tout envoi est refusé avec « L'envoi WhatsApp n'est pas configuré (Infobip) ».

La clé d'API doit porter les droits d'envoi WhatsApp et de gestion des modèles. Un expéditeur partagé ne peut pas créer de modèle : il faut un numéro propre.

## Migrations

- Dev `V320__whatsapp_infobip.sql`, prod `V314__whatsapp_infobip.sql`, même contenu.
- `whatsapp_template` : objet, nom, langue, catégorie, texte avec ses variables `{{1}}`..., exemples, nombre de variables, statut (`DRAFT`, `PENDING`, `APPROVED`, `REJECTED`, `PAUSED`, `DISABLED`, `UNKNOWN`).
- `whatsapp_message` : journal de chaque envoi (destinataire, modèle ou texte, valeurs, référence métier, statut rendu par Infobip, erreur).
- Droit `WHATSAPP:MANAGE`, donné à `SUPER_ADMIN` et `ADMIN`.
- Suivi : dev `V321__suivi_whatsapp.sql`, prod `V315__suivi_whatsapp.sql`.
  - `whatsapp_message` : numéro de notification, description du statut, dates d'envoi, de remise, de lecture et de dernière relecture ;
  - `notification_message.recipient_phone`.

## Cycle d'un modèle

1. Création (`POST /admin/whatsapp/templates`, ou `/templates/defaults` pour les modèles fournis) : le modèle part chez Infobip, qui le soumet à Meta. Statut `PENDING`.
2. Approbation par Meta : de quelques minutes à 24 heures.
3. Synchronisation (`POST /admin/whatsapp/templates/sync`) : relit les statuts chez Infobip. Seul un modèle `APPROVED` sert à l'envoi.

Règles de création :
- nom en minuscules, chiffres et soulignés ;
- catégorie `UTILITY` (défaut), `MARKETING` ou `AUTHENTICATION` ;
- un exemple par variable `{{n}}`.

Modèles fournis (`WhatsAppService.DEFAULTS`), créés par `POST /admin/whatsapp/templates/defaults` :

| Objet | Nom | Catégorie | Contenu | Bouton |
| --- | --- | --- | --- | --- |
| `OTP` | `code_verification` | AUTHENTICATION | Texte imposé par Meta (« {{1}} est votre code de vérification »), recommandation de sécurité, validité de 10 minutes | Copier le code |
| `BOOKING_CONFIRMED` | `reservation_confirmee` | UTILITY | Réservation confirmée : prénom, espace, date, horaire, référence ; se présenter 10 minutes avant | aucun |
| `BOOKING_UPDATED` | `reservation_modifiee` | UTILITY | Réservation modifiée : prénom, référence, espace, date, horaire | Voir ma réservation |
| `PAYMENT_REMINDER` | `rappel_paiement` | UTILITY | Facture à régler : prénom, numéro, montant, échéance ; paiement dans l'espace client ou à l'accueil de l'espace ELLE A OSE | Payer ma facture |
| `SUBSCRIPTION_ACTIVATED` | `abonnement_actif` | UTILITY | Abonnement actif : prénom, formule, dates | Mon espace client |
| `INVOICE_PAID` | `facture_payee` | UTILITY | Paiement reçu, facture soldée, à télécharger dans l'espace client | Télécharger ma facture |
| `BALANCE_CONFIRMATION` | `confirmation_solde` | UTILITY | Confirmation de solde (lot F12) : exploitant, date, montant, sens, référence | aucun |

Règles :
- les textes nomment l'entreprise et ne commencent ni ne finissent par une variable (règles de Meta) ;
- les boutons de lien pointent sur `https://espace.elleaose.com/{{1}}` : le suffixe se donne à l'envoi (par exemple `factures/FAC-2026-00012`) ;
- pour le code OTP, le code est la seule variable ; il sert aussi au bouton de copie.

## Envoi

`WhatsAppMessageSender.send(numéro, objet, variables, texte libre, référence[, suffixe du bouton])` :
- le numéro se normalise au format international (pays par défaut `CG`) ;
- modèle approuvé de l'objet : envoi par modèle (le nombre de variables doit correspondre) ;
- sinon texte libre si admis, sinon refus « Aucun modèle WhatsApp approuvé » ;
- chaque envoi est journalisé, avec l'erreur s'il échoue.

Le statut rendu à l'envoi est celui de la prise en charge (`PENDING`).

## Suivi de remise et de lecture

Deux sources, qui mettent à jour le même journal :

1. **Webhook** (`POST /sni/api/v1/public/webhooks/infobip/whatsapp`), alimenté par l'abonnement Infobip aux événements `DELIVERY` et `SEEN` de l'expéditeur :
   - la signature `X-Hub-Signature` doit être le HMAC du corps brut avec le secret configuré (hexadécimal ou Base64, préfixe `sha256=` accepté) ;
   - sans secret configuré, ou avec une signature fausse : refus `401` ;
   - un identifiant de message inconnu est ignoré.
2. **Relecture des journaux** (`GET /messages-api/1/logs?messageId=`), toutes les 10 minutes et à la demande (`POST /admin/whatsapp/messages/refresh`), pour les messages encore en attente depuis moins de 47 heures (Infobip garde 48 heures). C'est le secours si le webhook ne passe pas, par exemple en local.

Statuts d'Infobip : `PENDING` (pris en charge), `DELIVERED` (remis), `UNDELIVERABLE`, `REJECTED`, `EXPIRED` (échec, avec le code d'erreur), et la lecture (`seenAt`).

**Reflet dans les notifications** : chaque message WhatsApp a sa ligne dans `notification_message`, canal `WHATSAPP`, numéro `WA-{identifiant}`, numéro du destinataire dans `recipientPhone`. Elle passe :
- à `SENT` à l'envoi ;
- à `DELIVERED` à la remise ;
- à `FAILED` en cas d'échec, avec le motif dans `lastError` ;
- la lecture renseigne `readAt`.

Le module de notifications ne renvoie jamais lui-même un message WhatsApp : sa relance le marque en échec avec un motif.

`core` reste indépendant des modules métier : le service WhatsApp publie un `WhatsAppStatusEvent`, que `WhatsAppNotificationTracker` reprend.

## Abonnement aux webhooks, créé par le code

`POST /admin/whatsapp/subscription` crée chez Infobip, ou met à jour s'il existe déjà :
- la sécurité HMAC avec le secret configuré ;
- le profil de notification avec l'URL du webhook ;
- l'abonnement de l'expéditeur aux événements `DELIVERY` et `SEEN`.

`GET /admin/whatsapp/subscription` le relit chez Infobip.

Conditions :
- clé d'API avec le droit `subscriptions:manage` (la clé d'essai actuelle ne l'a pas : refus `403`) ;
- secret configuré ;
- adresse publique en `https`.

## Routes (`/sni/api/v1/admin/whatsapp`, droit `WHATSAPP:MANAGE`)

| Route | Usage |
| --- | --- |
| `GET /templates` | Modèles et statuts |
| `POST /templates` `{purpose, name, language, category, text, examples}` | Créer un modèle chez Infobip |
| `POST /templates/defaults` | Créer les modèles fournis manquants |
| `POST /templates/sync` | Relire les statuts chez Infobip |
| `GET /messages?reference=` | Journal des envois et leur statut |
| `POST /messages/refresh` | Relire chez Infobip le statut des messages en attente |
| `GET`, `POST /subscription` | Lire, créer ou mettre à jour l'abonnement aux webhooks |
| `POST /test` `{phone, purpose, placeholders, text}` | Message d'essai |

## Pour le déploiement

1. Créer le compte Infobip, l'expéditeur WhatsApp (numéro propre) et une clé d'API.
2. Renseigner les variables d'environnement, puis `INFOBIP_WHATSAPP_ENABLED=true`.
3. `INFOBIP_WEBHOOK_SECRET` (secret aléatoire) et l'adresse publique `APP_BASE_URL` en https.
4. Clé d'API avec le droit `subscriptions:manage`, puis `POST /admin/whatsapp/subscription`.
5. `POST /admin/whatsapp/templates/defaults`, attendre l'approbation, puis `POST /admin/whatsapp/templates/sync`.
6. Essayer avec `POST /admin/whatsapp/test`, puis suivre le statut dans le journal des envois ou les notifications.

## Tests

- `WhatsAppServiceTest` : envoi par modèle approuvé, contrôle du nombre de variables, texte libre seulement s'il est admis, refus sans configuration, validation des modèles.
- `WhatsAppTrackingTest` : signature du webhook (hexadécimal, préfixe, Base64, corps altéré, secret absent), lecture des rapports d'Infobip, refus d'un appel non signé, reflet dans `notification_message` (envoyé, remis, lu, en échec).
- `InfobipWhatsAppLiveTest` : essai réel, ignoré sans clé. Le 13/11/2026, un message d'essai vers 242055645569 a été remis au téléphone en 4 secondes (`DELIVERED_TO_HANDSET`, lu dans les journaux d'Infobip), et le modèle `reservation_confirmee` a été créé par le code.

## Pour le frontend

Écrans à prévoir :
- modèles WhatsApp : liste et statuts, création, création des modèles fournis, synchronisation ;
- journal des envois, avec le statut de remise et de lecture, et le bouton de relecture ;
- abonnement aux webhooks : état et création ;
- message d'essai.
