-- LooseNotes Database Schema
-- Compatible with H2 (embedded), MySQL 8+, and PostgreSQL 14+.
-- All VARCHAR lengths are sized conservatively to enforce input bounds at the DB layer.

CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(254) NOT NULL UNIQUE,
    -- BCrypt hash – never store plaintext passwords
    password_hash VARCHAR(72) NOT NULL,
    role         VARCHAR(10)  NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);

-- -----------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS notes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title       VARCHAR(255) NOT NULL,
    content     CLOB         NOT NULL,
    -- 'PRIVATE' or 'PUBLIC'
    visibility  VARCHAR(7)   NOT NULL DEFAULT 'PRIVATE' CHECK (visibility IN ('PRIVATE', 'PUBLIC')),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notes_user_id    ON notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_visibility ON notes(visibility);

-- -----------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS attachments (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id       BIGINT       NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    original_name VARCHAR(255) NOT NULL,
    -- Stored filename is a UUID – never the original name
    stored_name   VARCHAR(255) NOT NULL UNIQUE,
    mime_type     VARCHAR(100) NOT NULL,
    file_size     BIGINT       NOT NULL,
    uploaded_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id);

-- -----------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ratings (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id    BIGINT NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    user_id    BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    -- Constrained to 1-5 at DB level; validated in service layer too
    stars      SMALLINT NOT NULL CHECK (stars >= 1 AND stars <= 5),
    comment    VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (note_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id);

-- -----------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS share_links (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id    BIGINT       NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    -- 32-byte URL-safe random token stored as hex; indexed for fast lookup
    token      VARCHAR(64)  NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_share_links_token ON share_links(token);

-- -----------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP   NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_prt_token_hash ON password_reset_tokens(token_hash);

-- -----------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    -- NULL user_id means unauthenticated action
    user_id     BIGINT,
    -- Category of event (AUTH, NOTE, ADMIN, SHARE, ATTACHMENT, RATING)
    event_type  VARCHAR(20)  NOT NULL,
    event_detail VARCHAR(500) NOT NULL,
    -- Truncated IP address – no full PII storage
    ip_address  VARCHAR(45),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id    ON audit_logs(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_event_type ON audit_logs(event_type);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
