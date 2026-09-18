-- Numeros de telephone · forme internationale.
--
-- Le systeme n'acceptait que la forme locale congolaise, 0[456] suivi de sept chiffres, et une
-- colonne de douze caracteres. Un abonne qui s'inscrit avec +242 06 123 45 67, ou depuis un autre
-- pays, etait refuse en production. Desormais tout numero lisible est accepte et garde en forme
-- internationale, E.164 : un plus, puis les chiffres. Une seule ecriture par numero, donc une seule
-- cle pour retrouver quelqu'un.
--
-- Les numeros deja enregistres en forme locale sont convertis, sinon une recherche par numero
-- international ne les retrouverait plus.

ALTER TABLE member
    ALTER COLUMN phone TYPE VARCHAR(30),
    ALTER COLUMN whatsapp_phone TYPE VARCHAR(30);

ALTER TABLE customer
    ALTER COLUMN phone TYPE VARCHAR(30),
    ALTER COLUMN whatsapp_phone TYPE VARCHAR(30);

-- 0X XXX XX XX · avec ou sans espaces · devient +2420XXXXXXXX. Au Congo le zero de tete fait
-- partie du numero et se garde en forme internationale, il n'est pas un prefixe d'acces comme en
-- France. Seule la forme locale congolaise est convertie : un numero qu'on ne sait pas lire est
-- laisse tel quel, il vaut mieux une ligne inchangee qu'une ligne devinee.
UPDATE member
SET phone = '+242' || regexp_replace(phone, '[^0-9]', '', 'g')
WHERE regexp_replace(phone, '[^0-9]', '', 'g') ~ '^0[456][0-9]{7}$';

UPDATE member
SET whatsapp_phone = '+242' || regexp_replace(whatsapp_phone, '[^0-9]', '', 'g')
WHERE whatsapp_phone IS NOT NULL
  AND regexp_replace(whatsapp_phone, '[^0-9]', '', 'g') ~ '^0[456][0-9]{7}$';

UPDATE customer
SET phone = '+242' || regexp_replace(phone, '[^0-9]', '', 'g')
WHERE phone IS NOT NULL
  AND regexp_replace(phone, '[^0-9]', '', 'g') ~ '^0[456][0-9]{7}$';

UPDATE customer
SET whatsapp_phone = '+242' || regexp_replace(whatsapp_phone, '[^0-9]', '', 'g')
WHERE whatsapp_phone IS NOT NULL
  AND regexp_replace(whatsapp_phone, '[^0-9]', '', 'g') ~ '^0[456][0-9]{7}$';

-- Un numero international deja saisi avec des espaces ou des points prend aussi sa forme canonique.
UPDATE member
SET phone = '+' || regexp_replace(phone, '[^0-9]', '', 'g')
WHERE phone ~ '^\+' AND phone ~ '[^+0-9]';

UPDATE customer
SET phone = '+' || regexp_replace(phone, '[^0-9]', '', 'g')
WHERE phone ~ '^\+' AND phone ~ '[^+0-9]';
