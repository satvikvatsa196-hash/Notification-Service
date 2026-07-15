-- ─────────────────────────────────────────────────────────────────────────────
-- V2__seed_channels.sql
-- Seed default channel types for the system tenant
-- ─────────────────────────────────────────────────────────────────────────────

-- Insert a system-level tenant for internal use
INSERT INTO tenants (id, name, slug, contact_email, active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'System Tenant',
    'system',
    'system@notificationservice.internal',
    TRUE
) ON CONFLICT (slug) DO NOTHING;
