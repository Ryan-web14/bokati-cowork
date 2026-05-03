create table if not exists public.email_delivery_log (
    id bigint not null primary key,
    email_number varchar(100) not null unique,
    provider varchar(60) not null,
    from_email varchar(255),
    recipient_email varchar(255) not null,
    subject varchar(255),
    body_type varchar(20) not null,
    body_content text,
    status varchar(30) not null,
    attempts integer not null default 0,
    last_error text,
    related_type varchar(80),
    related_code varchar(120),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    sent_at timestamp with time zone,
    failed_at timestamp with time zone
);

create index if not exists idx_email_delivery_number on public.email_delivery_log (email_number);
create index if not exists idx_email_delivery_status on public.email_delivery_log (status, created_at);
create index if not exists idx_email_delivery_recipient on public.email_delivery_log (recipient_email);
create index if not exists idx_email_delivery_related on public.email_delivery_log (related_type, related_code);

insert into public.sequence_definition (
    id,
    code,
    name,
    description,
    prefix,
    suffix,
    pattern,
    padding,
    initial_value,
    increment_step,
    reset_policy,
    enabled,
    system_managed,
    created_at,
    updated_at
)
select
    900090,
    'email_delivery',
    'Email delivery log',
    'Sequence des journaux d envoi email',
    'EML',
    null,
    '{PREFIX}-{YYYY}{MM}{DD}-{SEQ}',
    8,
    1,
    1,
    'DAILY',
    true,
    true,
    now(),
    now()
where not exists (
    select 1 from public.sequence_definition where code = 'email_delivery'
);
