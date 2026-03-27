'use strict';
const Database = require('better-sqlite3');
const path = require('path');
const { mkdirSync } = require('fs');
const config = require('../config');

mkdirSync(path.dirname(config.dbPath), { recursive: true });

const db = new Database(config.dbPath);

// Enable WAL mode and foreign keys for integrity and performance
db.pragma('journal_mode = WAL');
db.pragma('foreign_keys = ON');

function initializeSchema() {
  db.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      username TEXT UNIQUE NOT NULL,
      email TEXT UNIQUE NOT NULL,
      password_hash TEXT NOT NULL,
      role TEXT NOT NULL DEFAULT 'user' CHECK(role IN ('user', 'admin')),
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS notes (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      title TEXT NOT NULL,
      content TEXT NOT NULL,
      visibility TEXT NOT NULL DEFAULT 'private' CHECK(visibility IN ('public', 'private')),
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS attachments (
      id TEXT PRIMARY KEY,
      note_id TEXT NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id),
      original_filename TEXT NOT NULL,
      stored_filename TEXT NOT NULL,
      mime_type TEXT NOT NULL,
      file_size INTEGER NOT NULL,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS ratings (
      id TEXT PRIMARY KEY,
      note_id TEXT NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      rating INTEGER NOT NULL CHECK(rating >= 1 AND rating <= 5),
      comment TEXT,
      created_at INTEGER NOT NULL,
      updated_at INTEGER NOT NULL,
      UNIQUE(note_id, user_id)
    );

    CREATE TABLE IF NOT EXISTS share_links (
      id TEXT PRIMARY KEY,
      note_id TEXT NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
      token_hash TEXT NOT NULL UNIQUE,
      created_at INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS password_reset_tokens (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      token_hash TEXT NOT NULL UNIQUE,
      expires_at INTEGER NOT NULL,
      used INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS login_attempts (
      id TEXT PRIMARY KEY,
      username TEXT NOT NULL,
      attempted_at INTEGER NOT NULL,
      ip_address TEXT NOT NULL,
      success INTEGER NOT NULL DEFAULT 0
    );

    CREATE TABLE IF NOT EXISTS audit_logs (
      id TEXT PRIMARY KEY,
      event_type TEXT NOT NULL,
      user_id TEXT,
      ip_address TEXT,
      resource_type TEXT,
      resource_id TEXT,
      details TEXT,
      created_at INTEGER NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id);
    CREATE INDEX IF NOT EXISTS idx_notes_visibility ON notes(visibility);
    CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id);
    CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id);
    CREATE INDEX IF NOT EXISTS idx_ratings_user_id ON ratings(user_id);
    CREATE INDEX IF NOT EXISTS idx_share_links_token_hash ON share_links(token_hash);
    CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token_hash ON password_reset_tokens(token_hash);
    CREATE INDEX IF NOT EXISTS idx_login_attempts_username ON login_attempts(username);
    CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs(user_id);
    CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs(created_at);
  `);
}

initializeSchema();

module.exports = db;
