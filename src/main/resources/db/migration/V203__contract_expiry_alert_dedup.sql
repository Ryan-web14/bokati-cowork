-- Track the closest expiry-alert threshold (in days) already sent for a contract so the
-- hourly lifecycle worker stops re-publishing CONTRACT_EXPIRY_ALERT every hour on the
-- day a contract is 30 or 7 days from expiry.
ALTER TABLE contract_record
    ADD COLUMN IF NOT EXISTS expiry_alert_sent_days INTEGER;
