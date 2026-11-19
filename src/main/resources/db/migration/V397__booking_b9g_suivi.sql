-- Booking, lot B9g : avis (commentaire, date), agenda personnel synchronise (jeton par membre).

ALTER TABLE booking ADD COLUMN IF NOT EXISTS csat_comment VARCHAR(1000);
ALTER TABLE booking ADD COLUMN IF NOT EXISTS csat_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS booking_calendar_feed
(
    member_id  VARCHAR(300) NOT NULL,
    token      VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_booking_calendar_feed PRIMARY KEY (member_id),
    CONSTRAINT uq_booking_calendar_feed_token UNIQUE (token)
);
