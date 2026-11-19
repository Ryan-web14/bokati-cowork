# Lot B5 : rendez-vous pris au téléphone

> Partie du [plan booking](README.md). Fait le 19/11/2026 sur `feature/booking`. Décision D5. Migration V391 (prod V385). Demande 4.

## À la réservation

- `CreateBookingRequest.caller` (équipe seulement) : `{ fullName, phone, email, company, callNote, leadNumber, createLead }`.
- Canal : `PHONE` par défaut quand un appelant est donné sans `channel` (`DESK`, `EMAIL`, `WHATSAPP` restent possibles, lot B2b).
- **Note d'appel** (`callNote`) gardée sur la réservation (`callNote` dans la réponse) et sur la fiche du prospect.
- **Appelant inconnu** (ni `memberId`, ni `customerId`, ni `businessCode`) : un prospect du CRM est créé, source `PHONE` (nouvelle valeur de `LeadSource`), intérêt déduit de la ressource (salle de réunion, bureau, espace de travail), montant estimé = prix de la réservation. `createLead: false` l'évite. `leadNumber` relie un prospect existant à la place.
- Une activité « Appel » (`CALL`) est ajoutée au prospect, avec la note ; la réservation porte `leadNumber` et l'événement `BOOKING_PHONE_CALL`.
- Le titulaire de la réservation reste un invité de passage (lot B0) tant qu'il n'a pas de fiche.

## Conversion

À la venue (arrivée pointée, ou arrivée anticipée validée) ou au paiement (paiement reçu, portefeuille, confirmation par la chaîne de paiement), le prospect relié encore ouvert est converti (`WON`), rattaché au titulaire de la réservation, avec une activité « Visite ». Événement `BOOKING_LEAD_CONVERTED`. Un prospect déjà gagné ou perdu n'est pas touché ; un échec du CRM n'arrête pas l'arrivée ni le paiement.

## Confirmation

Le courriel de confirmation part au contact (`contactEmail`) comme pour toute réservation ; le WhatsApp de confirmation existant (`reservation_confirmee`) part selon les préférences du membre. Un WhatsApp aux invités de passage demande un modèle à soumettre à Meta (avec les rappels du lot B6).

## Tests

`BookingB5Test` : prospect créé pour un appelant inconnu (source, téléphone, activité, note), aucun prospect pour un client connu, conversion à la venue une seule fois.
