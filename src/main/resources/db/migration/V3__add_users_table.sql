-- ─────────────────────────────────────────────────────────────────────────────
-- V3__add_users_table.sql
-- Creates the application users table for authentication and authorization
-- ─────────────────────────────────────────────────────────────────────────────

-- ── USERS ─────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            UUID        NOT NULL DEFAULT uuid_generate_v4(),
    email         VARCHAR(320) NOT NULL,
    password_hash VARCHAR(60)  NOT NULL,   -- BCrypt output is always exactly 60 chars
    role          VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- ── Automatic updated_at trigger ──────────────────────────────────────────────
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
