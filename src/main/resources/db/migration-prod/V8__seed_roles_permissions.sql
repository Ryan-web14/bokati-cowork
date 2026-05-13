insert into role (id, name, display_name, description, is_system_role, version, created_at, updated_at, is_active)
select 1, 'ADMIN', 'Administrator', 'System administrator role', true, 0, now(), now(), true
where not exists (select 1 from role where name = 'ADMIN');

insert into role (id, name, display_name, description, is_system_role, version, created_at, updated_at, is_active)
select 2, 'MANAGER', 'Manager', 'Operational management role', true, 0, now(), now(), true
where not exists (select 1 from role where name = 'MANAGER');

insert into role (id, name, display_name, description, is_system_role, version, created_at, updated_at, is_active)
select 3, 'STAFF', 'Staff', 'Standard staff role', true, 0, now(), now(), true
where not exists (select 1 from role where name = 'STAFF');

insert into role (id, name, display_name, description, is_system_role, version, created_at, updated_at, is_active)
select 4, 'CUSTOMER', 'Customer', 'Customer portal role', true, 0, now(), now(), true
where not exists (select 1 from role where name = 'CUSTOMER');

insert into role (id, name, display_name, description, is_system_role, version, created_at, updated_at, is_active)
select 5, 'MEMBER', 'Member', 'Member portal role', true, 0, now(), now(), true
where not exists (select 1 from role where name = 'MEMBER');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 1, 'ROLE_READ', 'Read roles', 'ROLE', 'READ', true, true
where not exists (select 1 from permission where name = 'ROLE_READ');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 2, 'ROLE_WRITE', 'Manage roles', 'ROLE', 'WRITE', true, true
where not exists (select 1 from permission where name = 'ROLE_WRITE');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 3, 'PERMISSION_READ', 'Read permissions', 'PERMISSION', 'READ', true, true
where not exists (select 1 from permission where name = 'PERMISSION_READ');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 4, 'PERMISSION_WRITE', 'Manage permissions', 'PERMISSION', 'WRITE', true, true
where not exists (select 1 from permission where name = 'PERMISSION_WRITE');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 5, 'USER_READ', 'Read users', 'USER', 'READ', true, true
where not exists (select 1 from permission where name = 'USER_READ');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 6, 'USER_WRITE', 'Manage users', 'USER', 'WRITE', true, true
where not exists (select 1 from permission where name = 'USER_WRITE');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 7, 'CUSTOMER_READ', 'Read customers', 'CUSTOMER', 'READ', true, true
where not exists (select 1 from permission where name = 'CUSTOMER_READ');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 8, 'CUSTOMER_WRITE', 'Manage customers', 'CUSTOMER', 'WRITE', true, true
where not exists (select 1 from permission where name = 'CUSTOMER_WRITE');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 9, 'MEMBER_READ', 'Read members', 'MEMBER', 'READ', true, true
where not exists (select 1 from permission where name = 'MEMBER_READ');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 10, 'MEMBER_WRITE', 'Manage members', 'MEMBER', 'WRITE', true, true
where not exists (select 1 from permission where name = 'MEMBER_WRITE');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 11, 'RESOURCE_READ', 'Read resources', 'RESOURCE', 'READ', true, true
where not exists (select 1 from permission where name = 'RESOURCE_READ');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 12, 'RESOURCE_WRITE', 'Manage resources', 'RESOURCE', 'WRITE', true, true
where not exists (select 1 from permission where name = 'RESOURCE_WRITE');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 13, 'DOCUMENT_READ', 'Read documents', 'DOCUMENT', 'READ', true, true
where not exists (select 1 from permission where name = 'DOCUMENT_READ');

insert into permission (id, name, display_name, module, action, is_system_permission, is_active)
select 14, 'DOCUMENT_WRITE', 'Manage documents', 'DOCUMENT', 'WRITE', true, true
where not exists (select 1 from permission where name = 'DOCUMENT_WRITE');

insert into role_permission (role_id, permission_id, created_by, is_active, role, permission)
values (1, 1, 'SYSTEM', true, 1, 1),
       (1, 2, 'SYSTEM', true, 1, 2),
       (1, 3, 'SYSTEM', true, 1, 3),
       (1, 4, 'SYSTEM', true, 1, 4),
       (1, 5, 'SYSTEM', true, 1, 5),
       (1, 6, 'SYSTEM', true, 1, 6),
       (1, 7, 'SYSTEM', true, 1, 7),
       (1, 8, 'SYSTEM', true, 1, 8),
       (1, 9, 'SYSTEM', true, 1, 9),
       (1, 10, 'SYSTEM', true, 1, 10),
       (1, 11, 'SYSTEM', true, 1, 11),
       (1, 12, 'SYSTEM', true, 1, 12),
       (1, 13, 'SYSTEM', true, 1, 13),
       (1, 14, 'SYSTEM', true, 1, 14)
on conflict do nothing;


insert into role_permission (role_id, permission_id, created_by, is_active, role, permission)
values
    (2, 5, 'SYSTEM', true, 2, 5),
    (2, 7, 'SYSTEM', true, 2, 7),
    (2, 8, 'SYSTEM', true, 2, 8),
    (2, 9, 'SYSTEM', true, 2, 9),
    (2, 10, 'SYSTEM', true, 2, 10),
    (2, 11, 'SYSTEM', true, 2, 11),
    (2, 12, 'SYSTEM', true, 2, 12),
    (2, 13, 'SYSTEM', true, 2, 13),
    (2, 14, 'SYSTEM', true, 2, 14)
on conflict do nothing;

insert into role_permission (role_id, permission_id, created_by, is_active, role, permission)
values
    (3, 7, 'SYSTEM', true, 3, 7),
    (3, 9, 'SYSTEM', true, 3, 9),
    (3, 11, 'SYSTEM', true, 3, 11),
    (3, 13, 'SYSTEM', true, 3, 13)
on conflict do nothing;

insert into role_permission (role_id, permission_id, created_by, is_active, role, permission)
values
    (4, 7, 'SYSTEM', true, 4, 7),
    (4, 8, 'SYSTEM', true, 4, 8),
    (4, 13, 'SYSTEM', true, 4, 13)
on conflict do nothing;

insert into role_permission (role_id, permission_id, created_by, is_active, role, permission)
values
    (5, 9, 'SYSTEM', true, 5, 9),
    (5, 10, 'SYSTEM', true, 5, 10),
    (5, 11, 'SYSTEM', true, 5, 11),
    (5, 13, 'SYSTEM', true, 5, 13)
on conflict do nothing;
