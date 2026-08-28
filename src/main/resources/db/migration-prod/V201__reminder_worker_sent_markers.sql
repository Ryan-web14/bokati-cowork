-- "Already notified" markers for the reminder workers.
--
-- Each of these workers re-selected the same rows on every run because nothing recorded that the
-- mail had gone out, so the reminder was re-sent for as long as the row kept matching. Same shape,
-- and same fix, as contract_record.expiry_alert_sent_days in V203.

-- Payment intent expiry alert: a 2 h look-ahead window scanned every 30 min sent 4 identical
-- "Rappel : paiement en attente" mails per intent.
ALTER TABLE payment_intent
    ADD COLUMN IF NOT EXISTS expiry_alert_sent_at TIMESTAMP WITH TIME ZONE;

-- Document pre-expiry reminder: the hourly worker matched "expiry_date = today + N" on all 24 runs
-- of the day. Stores the threshold already notified (30 then 7), so each one fires once.
ALTER TABLE document
    ADD COLUMN IF NOT EXISTS expiry_reminder_sent_days INTEGER;

-- Overdue invoice reminder: an invoice stuck in OVERDUE was re-sent, PDF regenerated and attached,
-- every single day with no cap. Becomes a paced dunning sequence instead.
ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS payment_reminder_sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE billing_document
    ADD COLUMN IF NOT EXISTS payment_reminder_count INTEGER NOT NULL DEFAULT 0;

-- SLA breach alerts sat outside the escalation cooldown that was meant to pace them, so an open
-- ticket past its SLA mailed every hour indefinitely.
ALTER TABLE support_ticket
    ADD COLUMN IF NOT EXISTS first_response_alert_sent_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE support_ticket
    ADD COLUMN IF NOT EXISTS resolution_alert_sent_at TIMESTAMP WITH TIME ZONE;
