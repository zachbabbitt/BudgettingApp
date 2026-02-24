-- BudgettingTogether Supabase Schema
-- Run this in the Supabase SQL Editor after creating your project.
-- Auth is handled by Supabase Auth (GoTrue). User profiles and pairings are in Supabase.
-- Supabase Dashboard: Auth → Settings → Email → disable "Confirm email" for immediate login.

CREATE TABLE expenses (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    category TEXT NOT NULL,
    date BIGINT NOT NULL,               -- epoch millis (matches Room TypeConverter output)
    recurring_type TEXT NOT NULL DEFAULT 'NONE',  -- 'NONE', 'WEEKLY', or 'MONTHLY'
    original_amount DOUBLE PRECISION,
    original_currency TEXT,
    user_guid TEXT NOT NULL
);
CREATE INDEX idx_expenses_user_guid ON expenses(user_guid);

CREATE TABLE income (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    source TEXT NOT NULL,
    date BIGINT NOT NULL,
    recurring_type TEXT NOT NULL DEFAULT 'NONE',
    notes TEXT NOT NULL DEFAULT '',
    original_amount DOUBLE PRECISION,
    original_currency TEXT,
    user_guid TEXT NOT NULL
);
CREATE INDEX idx_income_user_guid ON income(user_guid);

CREATE TABLE budget_limits (
    category TEXT NOT NULL,
    limit_amount DOUBLE PRECISION NOT NULL,
    currency_code TEXT NOT NULL DEFAULT 'USD',
    user_guid TEXT NOT NULL,
    PRIMARY KEY (category, user_guid)   -- matches Room primaryKeys = ["category", "userGuid"]
);

CREATE TABLE categories (
    name TEXT NOT NULL,
    is_default BOOLEAN NOT NULL DEFAULT false,
    user_guid TEXT NOT NULL,
    PRIMARY KEY (name, user_guid)
);

CREATE TABLE user_preferences (
    user_guid TEXT PRIMARY KEY,         -- Room uses id=1 integer PK; Supabase uses user_guid
    default_currency_code_expenses TEXT NOT NULL DEFAULT 'USD',
    default_currency_code_tracking TEXT NOT NULL DEFAULT 'USD',
    default_currency_code_income TEXT NOT NULL DEFAULT 'USD',
    last_rates_update BIGINT NOT NULL DEFAULT 0,
    last_recurring_generation_month INT NOT NULL DEFAULT -1,
    last_recurring_generation_year INT NOT NULL DEFAULT -1
);

-- ── Auth: User profiles and cross-device pairing ─────────────────────────────

-- Profiles: app-specific fields for each Supabase Auth user
-- id     = auth.users.id (Supabase auth UUID, userId)
-- user_guid = separate data-sync UUID used on all data tables above
CREATE TABLE profiles (
    id          TEXT PRIMARY KEY,
    user_guid   TEXT UNIQUE NOT NULL,
    username    TEXT UNIQUE NOT NULL,
    first_name  TEXT NOT NULL,
    last_name   TEXT NOT NULL,
    email       TEXT NOT NULL,
    created_at  BIGINT NOT NULL
);

-- Cross-device pairing (requester/receiver are Supabase auth UUIDs, i.e. userId)
CREATE TABLE user_pairings (
    id           BIGSERIAL PRIMARY KEY,
    requester_id TEXT NOT NULL,
    receiver_id  TEXT NOT NULL,
    status       TEXT NOT NULL DEFAULT 'PENDING',
    created_at   BIGINT NOT NULL,
    accepted_at  BIGINT
);
CREATE INDEX idx_pairings_requester ON user_pairings(requester_id);
CREATE INDEX idx_pairings_receiver  ON user_pairings(receiver_id);
