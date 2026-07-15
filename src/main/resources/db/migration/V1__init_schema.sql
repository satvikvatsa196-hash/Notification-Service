-- ─────────────────────────────────────────────────────────────────────────────
-- V1__init_schema.sql
-- Initial database schema for Notification Service
-- ─────────────────────────────────────────────────────────────────────────────

-- ── TENANTS ──────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenants (
    id            UUID        NOT NULL DEFAULT uuid_generate_v4(),
    name          VARCHAR(255) NOT NULL,
    slug          VARCHAR(100) NOT NULL,
    contact_email VARCHAR(320) NOT NULL,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tenants PRIMARY KEY (id),
    CONSTRAINT uq_tenants_name UNIQUE (name),
    CONSTRAINT uq_tenants_slug UNIQUE (slug),
    CONSTRAINT chk_tenants_email CHECK (contact_email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$')
);

CREATE INDEX IF NOT EXISTS idx_tenants_slug   ON tenants (slug);
CREATE INDEX IF NOT EXISTS idx_tenants_active ON tenants (active);

-- ── CHANNELS ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS channels (
    id           UUID        NOT NULL DEFAULT uuid_generate_v4(),
    tenant_id    UUID        NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    config       JSONB       NOT NULL DEFAULT '{}',
    active       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_channels PRIMARY KEY (id),
    CONSTRAINT fk_channels_tenant FOREIGN KEY (tenant_id)
        REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT chk_channels_type CHECK (
        channel_type IN ('EMAIL', 'SMS', 'PUSH', 'WEBHOOK', 'IN_APP')
    )
);

CREATE INDEX IF NOT EXISTS idx_channels_tenant_id ON channels (tenant_id);
CREATE INDEX IF NOT EXISTS idx_channels_type      ON channels (channel_type);
CREATE INDEX IF NOT EXISTS idx_channels_active    ON channels (active);

-- ── NOTIFICATION TEMPLATES ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notification_templates (
    id           UUID        NOT NULL DEFAULT uuid_generate_v4(),
    tenant_id    UUID        NOT NULL,
    channel_id   UUID        NOT NULL,
    name         VARCHAR(255) NOT NULL,
    subject      VARCHAR(500),
    body_template TEXT       NOT NULL,
    status       VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_notification_templates PRIMARY KEY (id),
    CONSTRAINT fk_templates_tenant  FOREIGN KEY (tenant_id)
        REFERENCES tenants (id) ON DELETE CASCADE,
    CONSTRAINT fk_templates_channel FOREIGN KEY (channel_id)
        REFERENCES channels (id) ON DELETE RESTRICT,
    CONSTRAINT chk_templates_status CHECK (
        status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')
    ),
    CONSTRAINT uq_templates_tenant_name UNIQUE (tenant_id, name)
);

CREATE INDEX IF NOT EXISTS idx_templates_tenant_id  ON notification_templates (tenant_id);
CREATE INDEX IF NOT EXISTS idx_templates_channel_id ON notification_templates (channel_id);
CREATE INDEX IF NOT EXISTS idx_templates_status     ON notification_templates (status);

-- ── AUTOMATIC updated_at trigger ─────────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_channels_updated_at
    BEFORE UPDATE ON channels
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_templates_updated_at
    BEFORE UPDATE ON notification_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
