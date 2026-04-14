alter table if exists role_permission
    drop column if exists role,
    drop column if exists permission;

alter table if exists role_user
    drop column if exists role,
    drop column if exists "user";
