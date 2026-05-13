alter table if exists document add column if not exists owner_id bigint;
alter table if exists document add column if not exists owner_type varchar(50);
alter table if exists document add column if not exists category varchar(50);
alter table if exists document add column if not exists document_type_id bigint;
alter table if exists document add column if not exists title varchar(300);
alter table if exists document add column if not exists description text;
alter table if exists document add column if not exists file_name varchar(350);
alter table if exists document add column if not exists file_url text;
alter table if exists document add column if not exists file_size bigint;
alter table if exists document add column if not exists status varchar(50);
alter table if exists document add column if not exists issue_date date;
alter table if exists document add column if not exists expiry_date date;
alter table if exists document add column if not exists uploaded_by bigint;
alter table if exists document add column if not exists uploaded_at timestamp;
alter table if exists document add column if not exists updated_at timestamp;

alter table if exists document_type add column if not exists document_requirement_id bigint;

alter table if exists document_review add column if not exists document_id bigint;
alter table if exists document_review add column if not exists review_status varchar(50);
alter table if exists document_review add column if not exists reviewed_by bigint;
alter table if exists document_review add column if not exists reviewed_at timestamp;
alter table if exists document_review add column if not exists comment text;

alter table if exists document_signature add column if not exists document_id bigint;
alter table if exists document_signature add column if not exists signature_path text;
alter table if exists document_signature add column if not exists signer_type varchar(30);
alter table if exists document_signature add column if not exists signer_id bigint;
alter table if exists document_signature add column if not exists signer_name varchar(250);
alter table if exists document_signature add column if not exists signer_email varchar(250);
alter table if exists document_signature add column if not exists ip_address varchar(60);
alter table if exists document_signature add column if not exists signed_at timestamp;
alter table if exists document_signature add column if not exists signature_data text;

alter table if exists document_requirement add column if not exists document_type_name varchar(255);
