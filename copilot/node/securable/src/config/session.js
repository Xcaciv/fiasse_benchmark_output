'use strict';

const session = require('express-session');
const SQLiteStore = require('connect-sqlite3')(session);
const path = require('path');

const isProduction = process.env.NODE_ENV === 'production';

// Session store backed by SQLite — separate file from application data
const store = new SQLiteStore({
  db: 'sessions.sqlite',
  dir: path.dirname(path.resolve(process.env.DB_PATH || './database.sqlite')),
  table: 'sessions',
});

const sessionConfig = {
  store,
  // SESSION_SECRET must be overridden in production via env
  secret: process.env.SESSION_SECRET || 'dev-secret-change-in-production',
  resave: false,
  saveUninitialized: false,
  // Use a non-default cookie name to reduce fingerprinting
  name: 'loose_notes_sid',
  cookie: {
    httpOnly: true,           // Prevent JS access to cookie
    secure: isProduction,     // HTTPS only in production
    sameSite: 'strict',       // CSRF mitigation layer
    maxAge: 24 * 60 * 60 * 1000, // 24-hour session lifetime
  },
};

module.exports = { sessionConfig };
