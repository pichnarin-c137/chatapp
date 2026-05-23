-- =====================================================================
-- V1: baseline schema for the chat app.
--
-- Cross-cutting columns on most tables (created_at, created_by,
-- updated_at, updated_by, deleted_at, deleted_by) match the entities
-- that extend BaseAuditedEntity. Hibernate runs in validate mode against
-- this schema, so column types / nullability must stay aligned with the
-- @Column annotations.
-- =====================================================================

-- ---------- extensions ------------------------------------------------
create extension if not exists "pgcrypto";  -- gen_random_uuid()

-- ---------- identity --------------------------------------------------
create table users (
    id              uuid        primary key default gen_random_uuid(),
    username        varchar(32) not null unique,
    email           varchar(255) not null unique,
    password_hash   varchar(255) not null,
    status          varchar(16) not null default 'ACTIVE',  -- ACTIVE | DISABLED | BANNED
    created_at      timestamptz not null default now(),
    created_by      uuid,
    updated_at      timestamptz not null default now(),
    updated_by      uuid,
    deleted_at      timestamptz,
    deleted_by      uuid,
    constraint chk_users_status check (status in ('ACTIVE', 'DISABLED', 'BANNED'))
);
create index idx_users_email           on users (lower(email));
create index idx_users_username        on users (lower(username));
create index idx_users_deleted_at      on users (deleted_at);

create table user_profiles (
    user_id         uuid        primary key references users(id) on delete cascade,
    display_name    varchar(64),
    avatar_url      varchar(500),
    bio             varchar(500),
    timezone        varchar(64) not null default 'Asia/Phnom_Penh',
    updated_at      timestamptz not null default now()
);

-- ---------- rbac ------------------------------------------------------
create table roles (
    id              uuid        primary key default gen_random_uuid(),
    code            varchar(64) not null unique,   -- SUPER_ADMIN, ADMIN, MODERATOR, USER
    name            varchar(64) not null,
    description     varchar(255),
    is_system       boolean     not null default false,  -- system roles can't be deleted
    created_at      timestamptz not null default now(),
    created_by      uuid,
    updated_at      timestamptz not null default now(),
    updated_by      uuid
);

create table permissions (
    id              uuid        primary key default gen_random_uuid(),
    code            varchar(96) not null unique,   -- USER_DELETE, MESSAGE_DELETE, ROOM_EDIT, ...
    description     varchar(255)
);

create table role_permissions (
    role_id         uuid not null references roles(id)       on delete cascade,
    permission_id   uuid not null references permissions(id) on delete cascade,
    primary key (role_id, permission_id)
);

create table user_roles (
    user_id         uuid not null references users(id)  on delete cascade,
    role_id         uuid not null references roles(id)  on delete cascade,
    assigned_at     timestamptz not null default now(),
    assigned_by     uuid,
    primary key (user_id, role_id)
);

-- ---------- conversations (DMs + groups unified) ---------------------
create table conversations (
    id              uuid        primary key default gen_random_uuid(),
    type            varchar(16) not null,  -- DIRECT | GROUP | CHANNEL
    name            varchar(64),           -- null for DIRECT
    topic           varchar(255),
    avatar_url      varchar(500),
    last_message_at timestamptz,           -- denormalised, updated on send (for ordering)
    created_at      timestamptz not null default now(),
    created_by      uuid,
    updated_at      timestamptz not null default now(),
    updated_by      uuid,
    deleted_at      timestamptz,
    deleted_by      uuid,
    constraint chk_conversations_type check (type in ('DIRECT', 'GROUP', 'CHANNEL'))
);
create index idx_conversations_type            on conversations (type);
create index idx_conversations_last_message    on conversations (last_message_at desc);

create table conversation_members (
    conversation_id         uuid not null references conversations(id) on delete cascade,
    user_id                 uuid not null references users(id)         on delete cascade,
    role                    varchar(16) not null default 'MEMBER',  -- OWNER | MOD | MEMBER
    joined_at               timestamptz not null default now(),
    left_at                 timestamptz,
    last_read_message_id    uuid,           -- nullable; updated when user reads
    last_read_at            timestamptz,
    muted_until             timestamptz,
    created_at              timestamptz not null default now(),
    created_by              uuid,
    updated_at              timestamptz not null default now(),
    updated_by              uuid,
    primary key (conversation_id, user_id),
    constraint chk_member_role check (role in ('OWNER', 'MOD', 'MEMBER'))
);
create index idx_members_user_id on conversation_members (user_id);

-- ---------- messages --------------------------------------------------
create table messages (
    id                  uuid        primary key default gen_random_uuid(),
    conversation_id     uuid        not null references conversations(id) on delete cascade,
    sender_id           uuid        references users(id) on delete set null,  -- null = system message
    type                varchar(16) not null default 'TEXT',  -- TEXT | IMAGE | FILE | SYSTEM
    body                varchar(4000),                        -- nullable for attachment-only
    reply_to_id         uuid        references messages(id) on delete set null,
    thread_root_id      uuid        references messages(id) on delete set null,
    edited_at           timestamptz,
    sent_at             timestamptz not null default now(),
    created_at          timestamptz not null default now(),
    created_by          uuid,
    updated_at          timestamptz not null default now(),
    updated_by          uuid,
    deleted_at          timestamptz,
    deleted_by          uuid,
    constraint chk_messages_type check (type in ('TEXT', 'IMAGE', 'FILE', 'SYSTEM'))
);
create index idx_messages_conv_sent      on messages (conversation_id, sent_at desc);
create index idx_messages_conv_id_sent   on messages (conversation_id, id);
create index idx_messages_sender         on messages (sender_id);
create index idx_messages_deleted        on messages (deleted_at);

-- Now that messages exists, FK the conversation_members.last_read_message_id to it.
alter table conversation_members
    add constraint fk_members_last_read_message
    foreign key (last_read_message_id) references messages(id) on delete set null;

create table message_attachments (
    id              uuid        primary key default gen_random_uuid(),
    message_id      uuid        not null references messages(id) on delete cascade,
    kind            varchar(16) not null,           -- IMAGE | FILE
    url             varchar(500) not null,
    mime_type       varchar(127),
    size_bytes      bigint,
    original_name   varchar(255),
    width           int,                            -- nullable; only meaningful for IMAGE
    height          int,
    created_at      timestamptz not null default now(),
    constraint chk_attachment_kind check (kind in ('IMAGE', 'FILE'))
);
create index idx_attachments_message on message_attachments (message_id);

-- Per-message read receipts. Optional: we also keep last_read_message_id on
-- conversation_members for cheap unread counts. This table answers "who has
-- seen this message" for individual messages.
create table message_reads (
    message_id      uuid not null references messages(id) on delete cascade,
    user_id         uuid not null references users(id)    on delete cascade,
    read_at         timestamptz not null default now(),
    primary key (message_id, user_id)
);
create index idx_message_reads_user on message_reads (user_id);

-- ---------- moderation -----------------------------------------------
create table message_reports (
    id              uuid        primary key default gen_random_uuid(),
    message_id      uuid        not null references messages(id) on delete cascade,
    reporter_id     uuid        not null references users(id)    on delete cascade,
    reason          varchar(500),
    status          varchar(16) not null default 'OPEN',  -- OPEN | RESOLVED | DISMISSED
    resolved_at     timestamptz,
    resolved_by     uuid        references users(id)     on delete set null,
    created_at      timestamptz not null default now(),
    constraint chk_report_status check (status in ('OPEN', 'RESOLVED', 'DISMISSED'))
);
create index idx_reports_status  on message_reports (status);
create index idx_reports_message on message_reports (message_id);

-- ---------- audit log ------------------------------------------------
-- One row per mutation. Captures who/what/when plus before/after JSON for
-- diffable history. Keyed only by entity_type + entity_id (no FK) so the
-- log survives entity deletion.
create table audit_log (
    id              bigserial   primary key,
    at              timestamptz not null default now(),
    actor_user_id   uuid,
    action          varchar(16) not null,           -- CREATE | UPDATE | DELETE
    entity_type     varchar(64) not null,
    entity_id       varchar(64) not null,
    before_json     jsonb,
    after_json      jsonb,
    constraint chk_audit_action check (action in ('CREATE', 'UPDATE', 'DELETE'))
);
create index idx_audit_at         on audit_log (at desc);
create index idx_audit_actor      on audit_log (actor_user_id, at desc);
create index idx_audit_entity     on audit_log (entity_type, entity_id, at desc);

-- =====================================================================
-- Seed data: default permissions, roles, role-permission mapping, and
-- the system Lobby conversation. The admin user is bootstrapped in Java
-- (so the password hash is generated with the same encoder the app uses).
-- =====================================================================

insert into permissions (code, description) values
    ('USER_READ',         'View user list and profiles'),
    ('USER_EDIT',         'Edit user profiles and metadata'),
    ('USER_DELETE',       'Delete users'),
    ('USER_BAN',          'Ban/disable users'),
    ('ROLE_READ',         'View roles and permissions'),
    ('ROLE_EDIT',         'Create/edit roles and assign permissions'),
    ('ROLE_ASSIGN',       'Assign roles to users'),
    ('CONVERSATION_READ', 'View any conversation'),
    ('CONVERSATION_EDIT', 'Create/edit/delete any conversation'),
    ('MESSAGE_DELETE',    'Delete any message (moderation)'),
    ('REPORT_HANDLE',     'View and resolve message reports'),
    ('AUDIT_READ',        'View the audit log');

insert into roles (code, name, description, is_system) values
    ('SUPER_ADMIN', 'Super admin', 'Full access to everything',           true),
    ('ADMIN',       'Admin',       'Site administration without role mgmt', true),
    ('MODERATOR',   'Moderator',   'Moderation only',                     true),
    ('USER',        'User',        'Regular chat user',                   true);

-- SUPER_ADMIN gets every permission.
insert into role_permissions (role_id, permission_id)
select r.id, p.id
  from roles r
 cross join permissions p
 where r.code = 'SUPER_ADMIN';

-- ADMIN: everything except role-management.
insert into role_permissions (role_id, permission_id)
select r.id, p.id
  from roles r, permissions p
 where r.code = 'ADMIN'
   and p.code in (
       'USER_READ', 'USER_EDIT', 'USER_DELETE', 'USER_BAN',
       'ROLE_READ', 'ROLE_ASSIGN',
       'CONVERSATION_READ', 'CONVERSATION_EDIT',
       'MESSAGE_DELETE', 'REPORT_HANDLE', 'AUDIT_READ'
   );

-- MODERATOR: moderation only.
insert into role_permissions (role_id, permission_id)
select r.id, p.id
  from roles r, permissions p
 where r.code = 'MODERATOR'
   and p.code in (
       'USER_READ', 'USER_BAN',
       'CONVERSATION_READ',
       'MESSAGE_DELETE', 'REPORT_HANDLE'
   );

-- USER: no special permissions (regular user; chat is gated by membership).

-- The default Lobby conversation. Deterministic UUID so existing client
-- code that hard-codes the lobby keeps working.
insert into conversations (id, type, name, topic, created_at, updated_at)
values (
    '00000000-0000-0000-0000-000000000001',
    'CHANNEL',
    'Lobby',
    'General chat — everyone welcome',
    now(),
    now()
);
