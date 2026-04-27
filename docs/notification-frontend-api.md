# Notification Frontend API

Base path: `/sni/api/v1`

Le module notification centralise les emails template, notifications applicatives, publications outbox et webhooks. Les identifiants techniques (`notificationNumber`, `endpointCode`, `deliveryNumber`) sont generes par le backend.

## Envoyer Une Notification

`POST /notifications`

```json
{
  "eventType": "EMAIL_CONFIRMATION",
  "aggregateType": "MEMBER",
  "aggregateId": "MBR-00001",
  "channel": "EMAIL",
  "recipientType": "MEMBER",
  "recipientCode": "MBR-00001",
  "recipientEmail": "client@example.com",
  "recipientName": "Client",
  "templateCode": "EMAIL_CONFIRMATION",
  "payload": {
    "token": "123456",
    "expiresInMinutes": 15
  }
}
```

Response:

```json
{
  "notificationNumber": "NTF-202604-000001",
  "webhookDeliveries": 1
}
```

## Publier Un Evenement Outbox

`POST /notifications/events`

```json
{
  "eventType": "PAYMENT_SUCCEEDED",
  "aggregateType": "PAYMENT",
  "aggregateId": "PAY-202604-000001",
  "payload": {
    "recipientEmail": "client@example.com",
    "recipientName": "Client",
    "recipientType": "CUSTOMER",
    "recipientCode": "CUS-00001",
    "message": "Votre paiement a ete confirme.",
    "reference": "PAY-202604-000001"
  }
}
```

L'outbox traite ensuite l'evenement et cree les notifications/webhook deliveries associees.

## Lister Les Notifications

`GET /notifications?status=PENDING&channel=EMAIL&eventType=PAYMENT_SUCCEEDED&recipientEmail=client@example.com&search=PAY`

Filtres disponibles: `status`, `channel`, `eventType`, `recipientEmail`, `search`, pagination Spring.

## Retry Notification

`PATCH /notifications/{notificationNumber}/retry`

Replace une notification en `PENDING`.

## Templates

`GET /notifications/templates?channel=EMAIL&active=true`

`POST /notifications/templates`

```json
{
  "templateCode": "BOOKING_CONFIRMED",
  "channel": "EMAIL",
  "subject": "Reservation confirmee",
  "templateName": "generic-notification",
  "active": true,
  "adminOnly": false
}
```

Templates seeds: `EMAIL_CONFIRMATION`, `ONE_TIME_TOKEN`, `GENERATED_PASSWORD`, `ADMIN_ALERT`, `BOOKING_CONFIRMED`, `PAYMENT_SUCCEEDED`, `BILLING_DOCUMENT_ISSUED`.

## Webhooks

`POST /webhook-endpoints`

```json
{
  "name": "CRM",
  "url": "https://crm.example.com/webhooks/bokati",
  "secret": "shared-secret",
  "eventTypes": ["PAYMENT_SUCCEEDED", "BOOKING_CONFIRMED"],
  "active": true
}
```

`GET /webhook-endpoints?active=true&search=crm`

`PATCH /webhook-endpoints/{endpointCode}/activate`

`PATCH /webhook-endpoints/{endpointCode}/deactivate`

`GET /webhook-deliveries?status=FAILED&endpointCode=WHK-202604-000001&eventType=PAYMENT_SUCCEEDED`

`PATCH /webhook-deliveries/{deliveryNumber}/retry`

Headers envoyes aux endpoints:

- `X-Bokati-Event`
- `X-Bokati-Delivery`
- `X-Bokati-Signature` si un secret est configure, format `sha256=<hmac>`

