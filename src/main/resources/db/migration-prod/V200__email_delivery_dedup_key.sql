-- Deduplication key for outbound email.
--
-- email_number is a generated sequence: it is unique per row, never per logical email,
-- so nothing in the pipeline could tell "this message already went out". Two layers fix that:
--
--   * dedup_key embeds a time bucket, so a worker that re-selects the same rows on every
--     run cannot re-queue the same message inside its dedup window;
--   * the unique index makes a concurrent double-insert impossible, which is exactly what
--     the unlocked reminder workers were doing once the app ran on two or more dynos.
--
-- NULL means "never deduplicate" (admin-triggered resends go through the retry endpoint and
-- must always ship). The partial index lets those rows coexist.

ALTER TABLE email_delivery_log
    ADD COLUMN IF NOT EXISTS dedup_key VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS ux_email_delivery_dedup_key
    ON email_delivery_log (dedup_key)
    WHERE dedup_key IS NOT NULL;
