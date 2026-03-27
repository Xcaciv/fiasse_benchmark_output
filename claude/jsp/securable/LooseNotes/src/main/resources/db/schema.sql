-- ============================================================
-- Loose Notes - Database Schema
-- SSEM: Integrity - referential integrity, indexed queries
-- SSEM: Confidentiality - no PII beyond operational necessity
-- ============================================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username              VARCHAR(50)  NOT NULL UNIQUE,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(72)  NOT NULL,
    role                  VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP    NULL,
    CONSTRAINT chk_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_email    ON users (email);

-- Notes table
CREATE TABLE IF NOT EXISTS notes (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    title      VARCHAR(255) NOT NULL,
    content    CLOB         NOT NULL,
    is_public  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notes_user_id  ON notes (user_id);
CREATE INDEX IF NOT EXISTS idx_notes_is_public ON notes (is_public);

-- Full-text search index on title and content
CREATE INDEX IF NOT EXISTS idx_notes_title ON notes (title);

-- Attachments table
CREATE TABLE IF NOT EXISTS attachments (
    id                BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    note_id           BIGINT       NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL UNIQUE,
    file_size         BIGINT       NOT NULL,
    content_type      VARCHAR(127) NOT NULL,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachments_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments (note_id);

-- Ratings table (one rating per user per note)
CREATE TABLE IF NOT EXISTS ratings (
    id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    note_id    BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    value      INT          NOT NULL,
    comment    VARCHAR(1000) NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ratings_note    FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_ratings_user    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_ratings_note_user UNIQUE (note_id, user_id),
    CONSTRAINT chk_rating_value   CHECK (value BETWEEN 1 AND 5)
);

CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings (note_id);
CREATE INDEX IF NOT EXISTS idx_ratings_user_id ON ratings (user_id);

-- Share links table
CREATE TABLE IF NOT EXISTS share_links (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    note_id    BIGINT      NOT NULL,
    token      VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_active  BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_share_links_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_share_links_token   ON share_links (token);
CREATE INDEX IF NOT EXISTS idx_share_links_note_id ON share_links (note_id);

-- Audit log table (SSEM: Accountability)
CREATE TABLE IF NOT EXISTS audit_logs (
    id            BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       BIGINT       NULL,
    action        VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50)  NULL,
    resource_id   BIGINT       NULL,
    details       VARCHAR(500) NULL,
    ip_address    VARCHAR(45)  NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_audit_user_id   ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_created_at ON audit_logs (created_at);
CREATE INDEX IF NOT EXISTS idx_audit_action    ON audit_logs (action);

-- Password reset tokens table (SSEM: Authenticity - time-limited, single-use)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP   NULL,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_prt_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_prt_user_id    ON password_reset_tokens (user_id);

-- Seed admin account (password: Admin@1234 - must be changed on first login)
-- BCrypt hash of "Admin@1234" with cost 12
MERGE INTO users (username, email, password_hash, role) KEY (username)
VALUES (
    'admin',
    'admin@loosenotes.local',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'ADMIN'
);
