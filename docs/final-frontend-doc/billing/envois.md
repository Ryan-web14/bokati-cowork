# billing : envois (lot B4)

> Retour au [README du module](README.md). Toutes les routes sont sous `/sni/api/v1/billing`.

## Envoyer une pièce

`POST /documents/{documentNumber}/send` (corps facultatif) :

```json
{
  "to": ["dg@organisation.cg"],
  "cc": ["compta@organisation.cg", "achats@organisation.cg"],
  "bcc": [],
  "channels": ["EMAIL", "WHATSAPP"],
  "whatsappNumbers": ["+242061135836"],
  "message": "Bonne réception"
}
```

- `channels` : `EMAIL` (courriel, PDF joint) et/ou `WHATSAPP` (modèle Infobip avec un bouton vers l'espace client : `facture_payee` si la pièce est soldée, `rappel_paiement` sinon).
- Corps vide : les destinataires et canaux habituels du client (sa préférence), à défaut son adresse de facturation par courriel.
- `message` : mot de l'expéditeur, placé avant le message type du courriel.
- Un brouillon ne s'envoie pas (`400`). **Le statut ne change pas.**
- Une pièce certifiée dont le destinataire a été corrigé après certification part en **duplicata daté** (« DUPLICATA DU JJ/MM/AAAA (DESTINATAIRE CORRIGÉ) ») ; l'original certifié reste la référence (écart E15).

Réponse : une ligne de journal par canal (et par numéro WhatsApp), `BillingSendingResponse`.

## Journal des envois

`GET /documents/{documentNumber}/sendings` :

| Champ | Contenu |
| --- | --- |
| `channel` | `EMAIL` ou `WHATSAPP` |
| `to`, `cc`, `bcc` | Destinataires (numéro pour WhatsApp) |
| `subject` | Objet du courriel, ou modèle WhatsApp utilisé |
| `duplicate` | PDF envoyé en duplicata |
| `status` | Courriel : `QUEUED` (en file d'envoi), `FAILED` ; WhatsApp : statut de l'opérateur (`PENDING`, `DELIVERED`, `UNDELIVERABLE`...) |
| `error` | Raison d'un échec |
| `sentBy`, `sentByName`, `sentAt` | Auteur et date |
| `deliveredAt`, `seenAt` | WhatsApp : remise au téléphone, lecture |

Le renvoi d'une pièce est un nouvel envoi tracé.

## Destinataires habituels d'un client

- `GET /customers/{customerType}/{customerCode}/billing-recipients` : préférence enregistrée (`to`, `cc`, `bcc`, `whatsappNumbers`, `channels`) et adresses connues à proposer (`suggestedEmails`, `suggestedPhones`).
- `PUT /customers/{customerType}/{customerCode}/billing-recipients` avec `{ "to": [...], "cc": [...], "bcc": [...], "whatsappNumbers": [...], "channels": ["EMAIL"] }`. Exemple : « les factures de cette organisation partent à sa comptabilité, copie à la direction ».

Écran conseillé : à l'envoi, un formulaire prérempli par la préférence, avec des puces d'adresses (principal, copie, copie cachée), des cases de canal, un champ de message, et la case « Enregistrer comme destinataires habituels ».

## Routes anciennes

`PATCH /documents/{n}/send` trace seulement l'envoi (date, version transmise) sans rien expédier. `POST /documents/{n}/send-email` envoie au destinataire du document : préférer `POST /documents/{n}/send`.
