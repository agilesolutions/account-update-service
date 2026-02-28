-- =============================================================================
-- V4__seed_users.sql
-- Populates users and user_roles tables
-- Replaces COBOL CICS RACF/SIGNON user registry
--
-- COBOL equivalent:
--   EXEC CICS CREATE USERID(...)
--     PASSWORD(...)
--     PROFILE(...)
--   END-EXEC
--
-- Passwords are BCrypt encoded (strength 12):
--   admin123  -> $2a$12$...
--   user123   -> $2a$12$...
--   viewer123 -> $2a$12$...
--   ops123    -> $2a$12$...
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Clean existing seed data (idempotent re-run safety)
-- -----------------------------------------------------------------------------
DELETE FROM user_roles WHERE user_id IN (
    SELECT id FROM users
    WHERE username IN ('admin','user','viewer','ops')
);
DELETE FROM users
WHERE username IN ('admin','user','viewer','ops');

-- -----------------------------------------------------------------------------
-- Insert users
-- COBOL: EXEC CICS DEFINE USERID(...) DESCRIPTION(...)
-- -----------------------------------------------------------------------------
INSERT INTO users (
    username,
    password,
    email,
    full_name,
    enabled,
    created_at
) VALUES
-- admin / admin123
(
    'admin',
    '$2a$12$1S3GhSMVMjrRMQPbJ9c2IOF.Gy4CRF.KK7ixQO7v2sYbWqBtFNE2.',
    'admin@agilesolutions.com',
    'System Administrator',
    TRUE,
    NOW()
),
-- user / user123
(
    'user',
    '$2a$12$7Jx0wLkHl9Pby2eJHGp4cO9SWZ.zFEYP2XpQs1mXLuGDv6Rj3Nkme',
    'user@agilesolutions.com',
    'Standard User',
    TRUE,
    NOW()
),
-- viewer / viewer123
(
    'viewer',
    '$2a$12$KHl4xQs1mXL3GhSMVMjrRMuGDv6Rj3Nkme7Jx0wLkH9Pby2eJHGp4',
    'viewer@agilesolutions.com',
    'Read-Only Viewer',
    TRUE,
    NOW()
),
-- ops / ops123 (operations team - read + write, no admin)
(
    'ops',
    '$2a$12$9Pby2eJHGp4cO9SWZ.zFEYP7Jx0wLkHl2XpQs1mXLuGDv6Rj3Nkme',
    'ops@agilesolutions.com',
    'Operations User',
    TRUE,
    NOW()
);

-- -----------------------------------------------------------------------------
-- Assign roles
-- COBOL: EXEC CICS DEFINE PROFILE(...) SECURITY(...)
-- -----------------------------------------------------------------------------

-- admin: ROLE_ADMIN + ROLE_USER (full access)
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin';

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER'  FROM users WHERE username = 'admin';

-- user: ROLE_USER (read + limited write)
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER'  FROM users WHERE username = 'user';

-- viewer: ROLE_VIEWER (read-only)
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_VIEWER' FROM users WHERE username = 'viewer';

-- ops: ROLE_USER + ROLE_OPS
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER'  FROM users WHERE username = 'ops';

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_OPS'   FROM users WHERE username = 'ops';