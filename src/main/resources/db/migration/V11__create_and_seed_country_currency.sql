create table if not exists currency (
    id bigint primary key,
    currency_code varchar(3) not null unique,
    currency_name varchar(255) not null,
    created_at timestamp not null default now(),
    updated_at timestamp
);

create table if not exists country (
    id bigint primary key,
    name varchar(255) not null,
    country_code varchar(3) not null unique,
    default_currency_code bigint,
    phone_code varchar(20) not null,
    is_ohada_member boolean not null default false,
    deleted boolean not null default false,
    created_at timestamp not null default now(),
    updated_at timestamp
);

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'country_default_currency_code_fk'
    ) then
        alter table country
            add constraint country_default_currency_code_fk
            foreign key (default_currency_code) references currency(id);
    end if;
end $$;

create index if not exists idx_currency_code on currency(currency_code);
create index if not exists idx_country_code on country(country_code);
create index if not exists idx_country_deleted on country(deleted);

do $$
declare
    v_currency_id bigint;
begin
    select id
    into v_currency_id
    from currency
    where currency_code = 'XAF'
    limit 1;

    if v_currency_id is null then
        insert into currency (
            id,
            currency_code,
            currency_name,
            created_at,
            updated_at
        )
        values (
            1001,
            'XAF',
            'Central African CFA Franc',
            now(),
            now()
        )
        returning id into v_currency_id;
    else
        update currency
        set currency_name = 'Central African CFA Franc',
            updated_at = now()
        where id = v_currency_id;
    end if;

    if exists (
        select 1
        from country
        where country_code = 'CG'
    ) then
        update country
        set name = 'Congo',
            phone_code = '+242',
            is_ohada_member = true,
            default_currency_code = v_currency_id,
            deleted = false,
            updated_at = now()
        where country_code = 'CG';
    else
        insert into country (
            id,
            name,
            country_code,
            default_currency_code,
            phone_code,
            is_ohada_member,
            deleted,
            created_at,
            updated_at
        )
        values (
            2001,
            'Congo',
            'CG',
            v_currency_id,
            '+242',
            true,
            false,
            now(),
            now()
        );
    end if;
end $$;
