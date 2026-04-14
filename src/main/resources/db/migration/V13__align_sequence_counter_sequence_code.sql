alter table if exists sequence_counter
    add column if not exists sequence_code varchar(100);

update sequence_counter sc
set sequence_code = lower(trim(sd.code))
from sequence_definition sd
where sc.sequence_definition_id = sd.id
  and (sc.sequence_code is null or btrim(sc.sequence_code) = '');

alter table if exists sequence_counter
    alter column sequence_code set not null;

create index if not exists idx_sequence_counter_sequence_code
    on sequence_counter(sequence_code);
