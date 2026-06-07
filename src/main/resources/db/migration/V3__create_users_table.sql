create table if not exists users (
    id uuid primary key,
    username varchar(120) not null unique,
    password varchar(255) not null,
    client_id varchar(120) not null,
    enabled boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table if not exists user_roles (
    user_id uuid not null,
    role_name varchar(120) not null,
    primary key (user_id, role_name),
    constraint fk_user_roles_users
        foreign key (user_id) references users(id)
        on delete cascade
);

create index if not exists idx_users_username on users (username);
create index if not exists idx_users_client_id on users (client_id);

insert into users (id, username, password, client_id, enabled, created_at, updated_at)
values
    ('11111111-aaaa-aaaa-aaaa-111111111111', 'client-a', '$2a$10$5DoMcm0Je/geJCoc4k3RG.Ls89iSAgYFhu/BUsLHSJVcGqNDffEM.', 'client-a', true, now(), now()),
    ('22222222-bbbb-bbbb-bbbb-222222222222', 'client-b', '$2a$10$s4jKvwgSiE7U6wezZEBvReN4uZeVnBdbT8/.WZ/yzMJIfYEIPBhJe', 'client-b', true, now(), now()),
    ('33333333-cccc-cccc-cccc-333333333333', 'client-c', '$2a$10$q8IMCVvpBRKheWFy2G41.u9GM1cEAUyeW1kib28Fh6mmq/DbO7s9C', 'client-c', true, now(), now()),
    ('44444444-dddd-dddd-dddd-444444444444', 'system', '$2a$10$nHTI3n7AwR/QcR29HqWmlO23q9IRF2OWzdNrTlNsrIiZ4AMSZuxRm', 'system', true, now(), now())
on conflict (username) do nothing;

insert into user_roles (user_id, role_name)
values
    ('11111111-aaaa-aaaa-aaaa-111111111111', 'ROLE_CLIENT'),
    ('22222222-bbbb-bbbb-bbbb-222222222222', 'ROLE_CLIENT'),
    ('33333333-cccc-cccc-cccc-333333333333', 'ROLE_CLIENT'),
    ('44444444-dddd-dddd-dddd-444444444444', 'ROLE_SYSTEM')
on conflict do nothing;

