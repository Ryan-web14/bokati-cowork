create table if not exists sequence_definition (
    id bigint primary key,
    code varchar(100) not null,
    name varchar(150) not null,
    description varchar(500),
    prefix varchar(50),
    suffix varchar(50),
    pattern varchar(255) not null,
    padding integer not null,
    initial_value bigint not null,
    increment_step integer not null,
    reset_policy varchar(20) not null,
    enabled boolean not null default true,
    system_managed boolean not null default false,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint uk_sequence_definition_code unique (code),
    constraint uk_sequence_definition_name unique (name),

    constraint ck_sequence_definition_padding check (padding > 0),
    constraint ck_sequence_definition_initial_value check (initial_value > 0),
    constraint ck_sequence_definition_increment_step check (increment_step > 0),
    constraint ck_sequence_definition_reset_policy check (
        reset_policy in ('NEVER', 'YEARLY', 'MONTHLY', 'DAILY')
    )
);

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_name = 'sequence_definition'
          and column_name = 'sequence_code'
    ) and not exists (
        select 1
        from information_schema.columns
        where table_name = 'sequence_definition'
          and column_name = 'code'
    ) then
        alter table sequence_definition rename column sequence_code to code;
    end if;
end $$;

create table if not exists sequence_counter (
    id bigint primary key,
    sequence_definition_id BIGINT not null,
    period_key varchar(20) not null,
    current_value bigint not null,
    version bigint,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),

    constraint fk_sequence_counter_definition foreign key (sequence_definition_id) references sequence_definition(id) on delete restrict,

    constraint uk_sequence_counter_definition_period unique (sequence_definition_id, period_key)
);

do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_name = 'sequence_counter'
          and column_name = 'defintion_id'
    ) and not exists (
        select 1
        from information_schema.columns
        where table_name = 'sequence_counter'
          and column_name = 'sequence_definition_id'
    ) then
        alter table sequence_counter rename column defintion_id to sequence_definition_id;
    end if;

    if exists (
        select 1
        from information_schema.columns
        where table_name = 'sequence_counter'
          and column_name = 'next_value'
    ) and not exists (
        select 1
        from information_schema.columns
        where table_name = 'sequence_counter'
          and column_name = 'current_value'
    ) then
        alter table sequence_counter rename column next_value to current_value;
    end if;
end $$;

alter table if exists sequence_counter add column if not exists sequence_definition_id bigint;
alter table if exists sequence_counter add column if not exists period_key varchar(20);
alter table if exists sequence_counter add column if not exists current_value bigint;
alter table if exists sequence_counter add column if not exists version bigint;
alter table if exists sequence_counter add column if not exists created_at timestamptz not null default now();
alter table if exists sequence_counter add column if not exists updated_at timestamptz not null default now();

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_sequence_counter_definition'
    ) then
        alter table sequence_counter
            add constraint fk_sequence_counter_definition
            foreign key (sequence_definition_id) references sequence_definition(id) on delete restrict;
    end if;

    if not exists (
        select 1
        from pg_constraint
        where conname = 'uk_sequence_counter_definition_period'
    ) then
        alter table sequence_counter
            add constraint uk_sequence_counter_definition_period unique (sequence_definition_id, period_key);
    end if;
end $$;

create index if not exists idx_sequence_counter_definition_period on sequence_counter(sequence_definition_id, period_key);

create index if not exists idx_sequence_definition_enabled
    on sequence_definition(enabled);




