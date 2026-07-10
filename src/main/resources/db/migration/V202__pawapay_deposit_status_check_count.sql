-- Track how many times the polling worker has checked a stuck mobile money deposit
-- so it can give up after a bounded number of attempts instead of polling forever.
ALTER TABLE pawapay_deposit
    ADD COLUMN IF NOT EXISTS status_check_count INTEGER NOT NULL DEFAULT 0;
