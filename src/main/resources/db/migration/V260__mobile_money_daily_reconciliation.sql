-- Le rapprochement quotidien des paiements mobile money.
--
-- scheduled_job_run · une ligne par passage d'une tache planifiee. Les deux dynos executent les
-- memes taches ; le premier qui inscrit le passage du jour le fait, l'autre s'abstient. Sans cela le
-- rapport partait deux fois, et les relectures chez l'operateur etaient faites deux fois.

CREATE TABLE IF NOT EXISTS scheduled_job_run (
    job_name    VARCHAR(100) NOT NULL,
    run_key     VARCHAR(100) NOT NULL,
    started_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ,
    summary     VARCHAR(2000),
    PRIMARY KEY (job_name, run_key)
);

INSERT INTO notification_template
(id, template_code, channel, subject, template_name, body_template, active, admin_only, created_at, updated_at)
SELECT 2600001, 'MOBILE_MONEY_DAILY_RECONCILIATION', 'EMAIL', 'Rapprochement mobile money du jour',
       'mobile-money-daily-reconciliation', NULL, TRUE, TRUE, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM notification_template WHERE template_code = 'MOBILE_MONEY_DAILY_RECONCILIATION');
