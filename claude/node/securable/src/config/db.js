'use strict';

const path = require('path');
const fs = require('fs');
const Database = require('better-sqlite3');
const logger = require('./logger');

const dbPath = path.resolve(process.env.DB_PATH || './data/loose-notes.db');

// Ensure the data directory exists
fs.mkdirSync(path.dirname(dbPath), { recursive: true });

const db = new Database(dbPath, {
  verbose: process.env.NODE_ENV === 'development' ? (msg) => logger.debug(msg) : null
});

// Enforce foreign keys and WAL mode for performance
db.pragma('foreign_keys = ON');
db.pragma('journal_mode = WAL');

// Schema initialisation
db.exec(`
  CREATE TABLE IF NOT EXISTS users (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    username    TEXT    UNIQUE NOT NULL COLLATE NOCASE,
    email       TEXT    UNIQUE NOT NULL COLLATE NOCASE,
    password_hash TEXT  NOT NULL,
    role        TEXT    NOT NULL DEFAULT 'user' CHECK(role IN ('user','admin')),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    reset_token          TEXT,
    reset_token_expires  DATETIME
  );

  CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
  CREATE INDEX IF NOT EXISTS idx_users_email    ON users(email);

  CREATE TABLE IF NOT EXISTS notes (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    title      TEXT    NOT NULL,
    content    TEXT    NOT NULL,
    is_public  INTEGER NOT NULL DEFAULT 0 CHECK(is_public IN (0,1)),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  );

  CREATE INDEX IF NOT EXISTS idx_notes_user_id  ON notes(user_id);
  CREATE INDEX IF NOT EXISTS idx_notes_is_public ON notes(is_public);

  CREATE TABLE IF NOT EXISTS attachments (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    note_id       INTEGER NOT NULL,
    original_name TEXT    NOT NULL,
    stored_name   TEXT    NOT NULL,
    mime_type     TEXT,
    size          INTEGER,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
  );

  CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id);

  CREATE TABLE IF NOT EXISTS ratings (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    note_id    INTEGER NOT NULL,
    user_id    INTEGER NOT NULL,
    stars      INTEGER NOT NULL CHECK(stars >= 1 AND stars <= 5),
    comment    TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(note_id, user_id),
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
  );

  CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id);

  CREATE TABLE IF NOT EXISTS share_links (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    note_id    INTEGER NOT NULL UNIQUE,
    token      TEXT    NOT NULL UNIQUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
  );

  CREATE TABLE IF NOT EXISTS activity_log (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER,
    action     TEXT    NOT NULL,
    details    TEXT,
    ip_address TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  );

  CREATE INDEX IF NOT EXISTS idx_activity_log_created ON activity_log(created_at DESC);
`);

logger.info('Database initialised', { path: dbPath });

module.exports = db;
