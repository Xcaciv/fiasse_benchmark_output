-- Loose Notes Application Schema
-- ASVS L2 compliant: typed enums, unique constraints, FK integrity

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',  -- USER | ADMIN
    failed_login_attempts INT NOT NULL DEFAULT 0,
    lockout_until TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS notes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(500) NOT NULL,
    content CLOB NOT NULL,
    visibility VARCHAR(10) NOT NULL DEFAULT 'PRIVATE',  -- PUBLIC | PRIVATE
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_notes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_visibility CHECK (visibility IN ('PUBLIC', 'PRIVATE'))
);

CREATE TABLE IF NOT EXISTS attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    stored_filename VARCHAR(255) NOT NULL UNIQUE,  -- UUID-based, no path traversal
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachments_note FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ratings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating_value INT NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ratings_note FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
    CONSTRAINT fk_ratings_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_rating_per_user_note UNIQUE (note_id, user_id),
    CONSTRAINT chk_rating_value CHECK (rating_value BETWEEN 1 AND 5)
);

CREATE TABLE IF NOT EXISTS share_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,  -- SHA-256 of the token, token itself not stored
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP NULL,
    CONSTRAINT fk_share_links_note FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    actor_id BIGINT NULL,
    actor_username VARCHAR(50) NULL,
    ip_address VARCHAR(45) NULL,
    resource_type VARCHAR(50) NULL,
    resource_id VARCHAR(100) NULL,
    outcome VARCHAR(20) NOT NULL,  -- SUCCESS | FAILURE | DENIED
    detail VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    -- Append-only: no UPDATE or DELETE grants on this table in production
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_visibility ON notes(visibility);
CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id);
CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id);
CREATE INDEX IF NOT EXISTS idx_share_links_token_hash ON share_links(token_hash);
CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_id ON audit_logs(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_prt_user_id ON password_reset_tokens(user_id);
