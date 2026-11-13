# Envoi WhatsApp par Infobip

Service commun d'envoi de messages WhatsApp sortants, en texte seul, par l'API d'Infobip. Premier usage : les confirmations de solde de la clôture (lot F12). Branche `feature/finance-comptabilite`.

## Principes

- **Aucune ouverture vers l'extérieur** : pas de lien public, pas de route `/public/**`, pas de rappel (webhook) d'Infobip vers l'application. Le destinataire répond sur WhatsApp ; l'équipe saisit sa réponse.
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

Désactivé ou incomplet, tout envoi est refusé avec « L'envoi WhatsApp n'est pas configuré (Infobip) ».

La clé d'API doit porter les droits d'envoi WhatsApp et de gestion des modèles. Un expéditeur partagé ne peut pas créer de modèle : il faut un numéro propre.

## Migrations

- Dev `V320__whatsapp_infobip.sql`, prod `V314__whatsapp_infobip.sql`, même contenu.
- `whatsapp_template` : objet, nom, langue, catégorie, texte avec ses variables `{{1}}`..., exemples, nombre de variables, statut (`DRAFT`, `PENDING`, `APPROVED`, `REJECTED`, `PAUSED`, `DISABLED`, `UNKNOWN`).
- `whatsapp_message` : journal de chaque envoi (destinataire, modèle ou texte, valeurs, référence métier, statut rendu par Infobip, erreur).
- Droit `WHATSAPP:MANAGE`, donné à `SUPER_ADMIN` et `ADMIN`.

## Cycle d'un modèle

1. Création (`POST /admin/whatsapp/templates`, ou `/templates/defaults` pour les modèles fournis) : le modèle part chez Infobip, qui le soumet à Meta. Statut `PENDING`.
2. Approbation par Meta : de quelques minutes à 24 heures.
3. Synchronisation (`POST /admin/whatsapp/templates/sync`) : relit les statuts chez Infobip. Seul un modèle `APPROVED` sert à l'envoi.

Règles de création :
- nom en minuscules, chiffres et soulignés ;
- catégorie `UTILITY` (défaut), `MARKETING` ou `AUTHENTICATION` ;
- un exemple par variable `{{n}}`.

Modèle fourni :

| Objet | Nom | Texte |
| --- | --- | --- |
| `BALANCE_CONFIRMATION` | `confirmation_solde` | « Bonjour, dans le cadre de la révision des comptes de {{1}}, le solde de votre compte au {{2}} est de {{3}} XAF {{4}}. Merci de nous confirmer ce solde, ou de nous indiquer le vôtre avec le détail de l'écart, en rappelant la référence {{5}}. » |

Variables : nom de l'exploitant, date, montant, « en notre faveur » ou « en votre faveur », numéro de la confirmation.

## Envoi

`WhatsAppMessageSender.send(numéro, objet, variables, texte libre, référence)` :
- le numéro se normalise au format international (pays par défaut `CG`) ;
- modèle approuvé de l'objet : envoi par modèle (le nombre de variables doit correspondre) ;
- sinon texte libre si admis, sinon refus « Aucun modèle WhatsApp approuvé » ;
- chaque envoi est journalisé, avec l'erreur s'il échoue.

Le statut rendu à l'envoi est celui de la prise en charge (`PENDING`). Le statut de remise n'est pas suivi : il faudrait un rappel d'Infobip vers l'application, exclu.

## Routes (`/sni/api/v1/admin/whatsapp`, droit `WHATSAPP:MANAGE`)

| Route | Usage |
| --- | --- |
| `GET /templates` | Modèles et statuts |
| `POST /templates` `{purpose, name, language, category, text, examples}` | Créer un modèle chez Infobip |
| `POST /templates/defaults` | Créer les modèles fournis manquants |
| `POST /templates/sync` | Relire les statuts chez Infobip |
| `GET /messages?reference=` | Journal des envois |
| `POST /test` `{phone, purpose, placeholders, text}` | Message d'essai |

## Pour le déploiement

1. Créer le compte Infobip, l'expéditeur WhatsApp (numéro propre) et une clé d'API.
2. Renseigner les variables d'environnement, puis `INFOBIP_WHATSAPP_ENABLED=true`.
3. `POST /admin/whatsapp/templates/defaults`, attendre l'approbation, puis `POST /admin/whatsapp/templates/sync`.
4. Essayer avec `POST /admin/whatsapp/test`.

## Tests

`WhatsAppServiceTest` : envoi par modèle approuvé, contrôle du nombre de variables, texte libre seulement s'il est admis, refus sans configuration, validation des modèles.

## Pour le frontend

Écrans à prévoir :
- modèles WhatsApp : liste et statuts, création, création des modèles fournis, synchronisation ;
- journal des envois ;
- message d'essai.
