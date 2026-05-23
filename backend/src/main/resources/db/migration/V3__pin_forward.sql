-- =====================================================================
-- V3: Phase 2 chat features — pins + forwards.
-- =====================================================================

-- Pinned messages, one row per (conversation, message). pinned_at orders
-- the panel newest-first.
create table pinned_messages (
    conversation_id uuid not null references conversations(id) on delete cascade,
    message_id      uuid not null references messages(id)      on delete cascade,
    pinned_by       uuid not null references users(id),
    pinned_at       timestamptz not null default now(),
    primary key (conversation_id, message_id)
);
create index idx_pin_conv on pinned_messages (conversation_id, pinned_at desc);

-- Forwarded message provenance. A forwarded message is a NEW row in
-- `messages` (its own id, sender = the forwarder, sent_at = now); this
-- table points back to the original so the UI can render
-- "Forwarded from @kira" or "from #lobby".
create table message_forwards (
    forwarded_message_id     uuid primary key references messages(id) on delete cascade,
    original_message_id      uuid not null    references messages(id) on delete cascade,
    original_sender_id       uuid not null    references users(id),
    original_conversation_id uuid not null    references conversations(id),
    forwarded_at             timestamptz not null default now()
);
create index idx_forward_orig on message_forwards (original_message_id);
