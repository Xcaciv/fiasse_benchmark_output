-- Loose Notes Application Schema
-- Parameterized DDL; executed once on startup by DatabaseManager

CREATE TABLE IF NOT EXISTS users (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username        TEXT    UNIQUE NOT NULL,
    email           TEXT    UNIQUE NOT NULL,
    password_hash   TEXT    NOT NULL,
    role            TEXT    NOT NULL DEFAULT 'USER',
    created_at      TEXT    NOT NULL DEFAULT (datetime('now')),
    reset_token     TEXT,
    reset_token_exp TEXT
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);

CREATE TABLE IF NOT EXISTS notes (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id     INTEGER NOT NULL,
    title       TEXT    NOT NULL,
    content     TEXT    NOT NULL,
    visibility  TEXT    NOT NULL DEFAULT 'PRIVATE',
    created_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at  TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_notes_user_id    ON notes(user_id);
CREATE INDEX IF NOT EXISTS idx_notes_visibility ON notes(visibility);

CREATE TABLE IF NOT EXISTS attachments (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    note_id           INTEGER NOT NULL,
    original_filename TEXT    NOT NULL,
    stored_filename   TEXT    NOT NULL,
    file_size         INTEGER NOT NULL,
    mime_type         TEXT    NOT NULL,
    uploaded_at       TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id);

CREATE TABLE IF NOT EXISTS ratings (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    note_id    INTEGER NOT NULL,
    user_id    INTEGER NOT NULL,
    stars      INTEGER NOT NULL CHECK (stars >= 1 AND stars <= 5),
    comment    TEXT,
    created_at TEXT    NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT    NOT NULL DEFAULT (datetime('now')),
    UNIQUE(note_id, user_id),
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id);
CREATE INDEX IF NOT EXISTS idx_ratings_user_id ON ratings(user_id);

CREATE TABLE IF NOT EXISTS share_links (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    note_id    INTEGER NOT NULL UNIQUE,
    token      TEXT    UNIQUE NOT NULL,
    created_at TEXT    NOT NULL DEFAULT (datetime('now')),
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_share_links_token ON share_links(token);

CREATE TABLE IF NOT EXISTS audit_log (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_id       INTEGER,
    actor_username TEXT,
    action         TEXT    NOT NULL,
    resource_type  TEXT,
    resource_id    TEXT,
    ip_address     TEXT,
    outcome        TEXT    NOT NULL,
    detail         TEXT,
    created_at     TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS idx_audit_log_actor_id  ON audit_log(actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);
