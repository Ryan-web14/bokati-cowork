-- Une version de pass plan se designe par un code lisible, comme son plan. Son identifiant technique
-- depasse ce que JavaScript sait representer : renvoye au navigateur, il revenait arrondi et ne
-- designait plus rien.

ALTER TABLE pass_plan_version ADD COLUMN IF NOT EXISTS version_code VARCHAR(120);

UPDATE pass_plan_version v
SET version_code = p.code || '-V' || v.version_number
FROM pass_plan p
WHERE p.id = v.plan_id
  AND v.version_code IS NULL;

ALTER TABLE pass_plan_version ALTER COLUMN version_code SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_pass_plan_version_code ON pass_plan_version (version_code);
