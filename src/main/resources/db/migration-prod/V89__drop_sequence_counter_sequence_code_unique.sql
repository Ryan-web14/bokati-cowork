do $$
declare
    constraint_record record;
begin
    for constraint_record in
        select con.conname
        from pg_constraint con
        join pg_class rel on rel.oid = con.conrelid
        join pg_namespace nsp on nsp.oid = rel.relnamespace
        where nsp.nspname = 'public'
          and rel.relname = 'sequence_counter'
          and con.contype = 'u'
          and (
              select string_agg(att.attname::text, ',' order by att.attnum)
              from unnest(con.conkey) as key(attnum)
              join pg_attribute att on att.attrelid = rel.oid and att.attnum = key.attnum
          ) = 'sequence_code'
    loop
        execute format('alter table public.sequence_counter drop constraint %I', constraint_record.conname);
    end loop;
end $$;

drop index if exists public.uk5f5daq6pnpxd4wxwhgjw307hd;
drop index if exists public.uk_sequence_counter_sequence_code;

create index if not exists idx_sequence_counter_sequence_code
    on sequence_counter(sequence_code);
