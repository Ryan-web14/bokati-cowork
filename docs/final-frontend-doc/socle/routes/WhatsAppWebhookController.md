# WhatsAppWebhookController

> Fichier généré par `tools/frontend-doc` depuis le code. Ne pas le modifier à la main : relancer la génération.

Module [socle](../README.md) · 1 route(s).

## Routes

<a id="r-post-sni-api-v1-public-webhooks-infobip-whatsapp"></a>
### POST /sni/api/v1/public/webhooks/infobip/whatsapp

- Accès : public, sans jeton
- Corps JSON (facultatif) : `String`
- Réponse : `vide`
- Code : `src/main/java/com/sni/bokaticowork/core/communication/whatsapp/WhatsAppWebhookController.java`, méthode `receive`

| Paramètre | Où | Type | Requis | Défaut |
| --- | --- | --- | --- | --- |
| `X-Hub-Signature` | en-tête | `String` |  |  |
