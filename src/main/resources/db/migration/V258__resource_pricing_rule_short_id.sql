-- Un numero court pour chaque regle de tarif, renvoye a la place de l identifiant genere.
--
-- L identifiant genere depasse 2^53 · JavaScript l arrondit en le lisant, et l interface
-- renvoyait ensuite un identifiant qui ne designait aucune regle. Le numero court vient d une
-- sequence : les regles existantes le recoivent dans leur ordre de creation, les suivantes a
-- l insertion, par la valeur par defaut de la colonne.
CREATE SEQUENCE IF NOT EXISTS resource_pricing_rule_short_id_seq START WITH 1;

ALTER TABLE resource_pricing_rule ADD COLUMN IF NOT EXISTS short_id BIGINT;

UPDATE resource_pricing_rule rule
SET short_id = numbered.n
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at, id) AS n
      FROM resource_pricing_rule) numbered
WHERE rule.id = numbered.id
  AND rule.short_id IS NULL;

SELECT setval('resource_pricing_rule_short_id_seq',
              GREATEST((SELECT COALESCE(MAX(short_id), 0) FROM resource_pricing_rule), 1),
              (SELECT COUNT(*) > 0 FROM resource_pricing_rule));

ALTER TABLE resource_pricing_rule ALTER COLUMN short_id SET DEFAULT nextval('resource_pricing_rule_short_id_seq');
ALTER TABLE resource_pricing_rule ALTER COLUMN short_id SET NOT NULL;
ALTER SEQUENCE resource_pricing_rule_short_id_seq OWNED BY resource_pricing_rule.short_id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_resource_pricing_rule_short_id ON resource_pricing_rule (short_id);
