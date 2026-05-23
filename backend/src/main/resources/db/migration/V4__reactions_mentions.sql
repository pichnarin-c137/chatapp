-- =====================================================================
-- V4: Phase 3 chat features — reactions + mentions.
-- Presence and typing are Redis-only (ephemeral) and have no schema.
-- =====================================================================

-- Reactions. Composite PK ensures one (user, emoji) per message is
-- idempotent (re-adding the same reaction is a no-op). reacted_at
-- supports newest-first ordering inside an emoji bucket.
create table message_reactions (
    message_id  uuid        not null references messages(id) on delete cascade,
    user_id     uuid        not null references users(id),
    emoji       varchar(32) not null,
    reacted_at  timestamptz not null default now(),
    primary key (message_id, user_id, emoji)
);
create index idx_reactions_msg on message_reactions (message_id);

-- Mentions parsed at send time. start_index / end_index are character
-- offsets into messages.body so the frontend can render highlight spans
-- without re-parsing. Composite PK collapses duplicate @mentions of the
-- same user inside one message; the parser picks the first range.
create table message_mentions (
    message_id         uuid not null references messages(id) on delete cascade,
    mentioned_user_id  uuid not null references users(id),
    start_index        int  not null,
    end_index          int  not null,
    primary key (message_id, mentioned_user_id)
);
create index idx_mentions_user on message_mentions (mentioned_user_id);
