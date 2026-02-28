-- =============================================================================
-- V3__create_users_table.sql
-- Creates users and user_roles tables
--
-- COBOL equivalent:
--   EXEC CICS CREATE USERID(...)
--     DESCRIPTION(...)
--     PASSWORD(...)
--   END-EXEC
--
-- Replaces CICS RACF / external security manager user registry.
-- Spring Security UserDetailsService reads from these tables at runtime.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Drop tables if they exist (clean slate for migration)
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS user_roles CASCADE;
DROP TABLE IF EXISTS users     CASCADE;

-- -----------------------------------------------------------------------------
-- users table
-- Mirrors COBOL CICS USERID / PASSWORD / PROFILE data structures
--
-- COBOL working-storage equivalent:
--   01 WS-USER-RECORD.
--     05 WS-USERID        PIC X(50).
--     05 WS-PASSWORD      PIC X(255).
--     05 WS-EMAIL         PIC X(100).
--     05 WS-FULL-NAME     PIC X(100).
--     05 WS-ENABLED-IND   PIC X(1)   VALUE 'Y'.
--     05 WS-CREATED-TS    PIC X(26).
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL,
    password        VARCHAR(255)    NOT NULL,
    email           VARCHAR(100)    NOT NULL,
    full_name       VARCHAR(100),
    enabled         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_users_username  UNIQUE (username),
    CONSTRAINT uq_users_email     UNIQUE (email),
    CONSTRAINT chk_users_username CHECK (LENGTH(TRIM(username)) > 0),
    CONSTRAINT chk_users_email    CHECK (email LIKE '%@%')
);

COMMENT ON TABLE  users              IS 'Application users - replaces COBOL CICS RACF user registry';
COMMENT ON COLUMN users.id           IS 'Surrogate primary key';
COMMENT ON COLUMN users.username     IS 'COBOL: EXEC CICS USERID PIC X(50)';
COMMENT ON COLUMN users.password     IS 'BCrypt encoded password - replaces COBOL EXEC CICS VERIFY PASSWORD';
COMMENT ON COLUMN users.email        IS 'User email address';
COMMENT ON COLUMN users.full_name    IS 'COBOL: EXEC CICS ASSIGN OPERID full name';
COMMENT ON COLUMN users.enabled      IS 'COBOL: EXEC CICS INQ USERID INUSE equivalent';
COMMENT ON COLUMN users.created_at   IS 'COBOL: EXEC CICS ASKTIME ABSTIME creation timestamp';

-- -----------------------------------------------------------------------------
-- user_roles table
-- Mirrors COBOL CICS RACF profile / group assignments
--
-- COBOL equivalent:
--   EXEC CICS QUERY SECURITY
--     RESTYPE('PROFILE')
--     RESIDLNG(WS-PROFILE-LEN)
--     RESID(WS-PROFILE-NAME)
--   END-EXEC
--
-- Role hierarchy:
--   ROLE_ADMIN  -> full CRUD access  (COBOL: RACF SPECIAL attribute)
--   ROLE_USER   -> read + update     (COBOL: RACF UPDATE access)
--   ROLE_OPS    -> read + update     (COBOL: RACF OPERATIONS attribute)
--   ROLE_VIEWER -> read-only         (COBOL: RACF READ access)
-- -----------------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id         BIGINT          NOT NULL,
    role            VARCHAR(50)     NOT NULL,

    -- Composite PK prevents duplicate role assignments
    PRIMARY KEY (user_id, role),

    -- FK back to users
    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    -- Valid role values only
    CONSTRAINT chk_user_roles_role
        CHECK (role IN (
            'ROLE_ADMIN',
            'ROLE_USER',
            'ROLE_OPS',
            'ROLE_VIEWER'
        ))
);

COMMENT ON TABLE  user_roles         IS 'User role assignments - replaces COBOL CICS RACF profile groups';
COMMENT ON COLUMN user_roles.user_id IS 'FK to users.id';
COMMENT ON COLUMN user_roles.role    IS 'Spring Security role name - replaces COBOL RACF profile name';

-- -----------------------------------------------------------------------------
-- Indexes
-- -----------------------------------------------------------------------------
CREATE INDEX idx_users_username    ON users      (username);
CREATE INDEX idx_users_email       ON users      (email);
CREATE INDEX idx_users_enabled     ON users      (enabled);
CREATE INDEX idx_user_roles_userid ON user_roles (user_id);
CREATE INDEX idx_user_roles_role   ON user_roles (role);

-- =============================================================================
-- Seed data - default application users
--
-- COBOL equivalent:
--   EXEC CICS CREATE USERID(...)
--     PASSWORD(...)
--   END-EXEC
--
-- Passwords are BCrypt encoded at strength 12.
-- Plain-text values for development/testing only:
--
--   admin   / Admin@123!
--   user    / User@123!
--   ops     / Ops@123!
--   viewer  / Viewer@123!
--
-- IMPORTANT: Change all passwords before deploying to production.
--            Use a secrets manager (Vault / AWS Secrets Manager) in prod.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Insert default users
-- -----------------------------------------------------------------------------
INSERT INTO users (
    username,
    password,
    email,
    full_name,
    enabled,
    created_at
) VALUES
-- ── admin / Admin@123! ───────────────────────────────────────────────────────
-- COBOL: EXEC CICS CREATE USERID('ADMIN') OPSECURITY(SPECIAL)
(
    'admin',
    '$2a$12$eOUGmkKScNe/TYpOdBR29.DNRkxNFtyLZmSHN0cy4nkpF0q7bJtAO',
    'admin@agilesolutions.com',
    'System Administrator',
    TRUE,
    NOW()
),
-- ── user / User@123! ────────────────────────────────────────────────────────
-- COBOL: EXEC CICS CREATE USERID('USER01') OPSECURITY(UPDATE)
(
    'user',
    '$2a$12$GxQWUbpVHCCRcSLdaTPWqunM5nSxBFZ7q4PiE6Xz0ZxCuS.YxG3dW',
    'user@agilesolutions.com',
    'Standard User',
    TRUE,
    NOW()
),
-- ── ops / Ops@123! ──────────────────────────────────────────────────────────
-- COBOL: EXEC CICS CREATE USERID('OPS01') OPSECURITY(OPERATIONS)
(
    'ops',
    '$2a$12$JzJ5bAHkG3yVhLmCpN8XT.4R1PiKOsWqeUxDfYv9M7tBrZ2ElS6Ca',
    'ops@agilesolutions.com',
    'Operations User',
    TRUE,
    NOW()
),
-- ── viewer / Viewer@123! ─────────────────────────────────────────────────────
-- COBOL: EXEC CICS CREATE USERID('VIEW01') OPSECURITY(READ)
(
    'viewer',
    '$2a$12$RsT8mNkL2qW5vXpCjH7YBuD4aFoEiG9sZy0UbM3cVn6lQr1PwKe.J',
    'viewer@agilesolutions.com',
    'Read-Only Viewer',
    TRUE,
    NOW()
);

-- -----------------------------------------------------------------------------
-- Assign roles to users
-- COBOL: EXEC CICS DEFINE PROFILE(...)
--          RESTYPE('USERS')
--          USERID(...)
-- -----------------------------------------------------------------------------

-- admin -> ROLE_ADMIN + ROLE_USER
-- COBOL: RACF SPECIAL + UPDATE authority
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN'
FROM   users
WHERE  username = 'admin';

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER'
FROM   users
WHERE  username = 'admin';

-- user -> ROLE_USER
-- COBOL: RACF UPDATE authority on ACCTDAT
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER'
FROM   users
WHERE  username = 'user';

-- ops -> ROLE_OPS + ROLE_USER
-- COBOL: RACF OPERATIONS + UPDATE authority
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_OPS'
FROM   users
WHERE  username = 'ops';

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER'
FROM   users
WHERE  username = 'ops';

-- viewer -> ROLE_VIEWER
-- COBOL: RACF READ-only authority on ACCTDAT
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_VIEWER'
FROM   users
WHERE  username = 'viewer';

-- =============================================================================
-- Post-insert verification
-- COBOL: EVALUATE WS-SQL-CODE
--          WHEN ZERO  CONTINUE
--          WHEN OTHER PERFORM ERROR-ROUTINE
-- =============================================================================
DO $$
DECLARE
    v_user_count     INTEGER;
    v_role_count     INTEGER;
    v_admin_roles    INTEGER;
    v_user_roles_cnt INTEGER;
    v_ops_roles      INTEGER;
    v_viewer_roles   INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_user_count  FROM users;
    SELECT COUNT(*) INTO v_role_count  FROM user_roles;

    SELECT COUNT(*) INTO v_admin_roles
    FROM   user_roles ur
    JOIN   users u ON u.id = ur.user_id
    WHERE  u.username = 'admin';

    SELECT COUNT(*) INTO v_user_roles_cnt
    FROM   user_roles ur
    JOIN   users u ON u.id = ur.user_id
    WHERE  u.username = 'user';

    SELECT COUNT(*) INTO v_ops_roles
    FROM   user_roles ur
    JOIN   users u ON u.id = ur.user_id
    WHERE  u.username = 'ops';

    SELECT COUNT(*) INTO v_viewer_roles
    FROM   user_roles ur
    JOIN   users u ON u.id = ur.user_id
    WHERE  u.username = 'viewer';

    -- ── Assertions ─────────────────────────────────────────────────────────
    IF v_user_count != 4 THEN
        RAISE EXCEPTION
            'V3 verification FAILED: expected 4 users, found %',
            v_user_count;
    END IF;

    IF v_role_count != 6 THEN
        RAISE EXCEPTION
            'V3 verification FAILED: expected 6 role assignments, found %',
            v_role_count;
    END IF;

    IF v_admin_roles != 2 THEN
        RAISE EXCEPTION
            'V3 verification FAILED: admin should have 2 roles, found %',
            v_admin_roles;
    END IF;

    IF v_user_roles_cnt != 1 THEN
        RAISE EXCEPTION
            'V3 verification FAILED: user should have 1 role, found %',
            v_user_roles_cnt;
    END IF;

    IF v_ops_roles != 2 THEN
        RAISE EXCEPTION
            'V3 verification FAILED: ops should have 2 roles, found %',
            v_ops_roles;
    END IF;

    IF v_viewer_roles != 1 THEN
        RAISE EXCEPTION
            'V3 verification FAILED: viewer should have 1 role, found %',
            v_viewer_roles;
    END IF;

    RAISE NOTICE '=== V3 Users Verification PASSED ===';
    RAISE NOTICE 'Users created        : %', v_user_count;
    RAISE NOTICE 'Role assignments     : %', v_role_count;
    RAISE NOTICE '  admin  roles       : % (ROLE_ADMIN, ROLE_USER)', v_admin_roles;
    RAISE NOTICE '  user   roles       : % (ROLE_USER)',             v_user_roles_cnt;
    RAISE NOTICE '  ops    roles       : % (ROLE_OPS, ROLE_USER)',   v_ops_roles;
    RAISE NOTICE '  viewer roles       : % (ROLE_VIEWER)',           v_viewer_roles;
END;
$$;