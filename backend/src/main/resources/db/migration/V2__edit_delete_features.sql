-- =====================================================================
-- V2: Phase 1 chat features
--   * message edit history (audit trail of prior bodies)
--   * delete-for-me bookkeeping (per-user soft hide)
--   * partial composite index for cursor pagination on history
--   * forward-looking permissions (PIN / FORWARD / CHANNEL_POST)
-- =====================================================================

-- Edit history. One row per edit, captures the body the message had
-- BEFORE the edit. Current body remains on messages.body.
create table message_edits (
    id              uuid        primary key default gen_random_uuid(),
    message_id      uuid        not null references messages(id) on delete cascade,
    editor_id       uuid        not null references users(id),
    previous_body   varchar(4000) not null,
    edited_at       timestamptz not null default now()
);
create index idx_message_edits_msg on message_edits (message_id, edited_at desc);

-- Per-user delete-for-me. Row presence means "this user has hidden
-- this message for themselves"; the row in messages is untouched and
-- other users still see it. Distinct from messages.deleted_at, which
-- is delete-for-everyone (and broadcast as a tombstone).
create table message_deletions (
    message_id      uuid        not null references messages(id) on delete cascade,
    user_id         uuid        not null references users(id),
    deleted_at      timestamptz not null default now(),
    primary key (message_id, user_id)
);
create index idx_msg_deletions_user on message_deletions (user_id);

-- Cursor pagination index. Adds id as a tiebreaker on top of the
-- existing (conversation_id, sent_at desc) index, partial on
-- deleted_at to skip tombstones cheaply.
create index idx_messages_conv_sent_id_active
    on messages (conversation_id, sent_at desc, id desc)
    where deleted_at is null;

-- Permissions used by later phases. Inserting them now keeps role
-- mappings additive across phases.
insert into permissions (code, description) values
    ('MESSAGE_PIN',     'Pin/unpin messages in a conversation'),
    ('MESSAGE_FORWARD', 'Forward messages to other conversations'),
    ('CHANNEL_POST',    'Post in announcement-only channels')
on conflict (code) do nothing;

-- Grant the new permissions to existing high-tier roles.
insert into role_permissions (role_id, permission_id)
select r.id, p.id
  from roles r, permissions p
 where r.code in ('SUPER_ADMIN', 'ADMIN')
   and p.code in ('MESSAGE_PIN', 'MESSAGE_FORWARD', 'CHANNEL_POST')
on conflict do nothing;

insert into role_permissions (role_id, permission_id)
select r.id, p.id
  from roles r, permissions p
 where r.code = 'MODERATOR'
   and p.code in ('MESSAGE_PIN')
on conflict do nothing;
