'use strict';

require('dotenv').config();

const path    = require('path');
const express = require('express');
const helmet  = require('helmet');
const morgan  = require('morgan');
const session = require('express-session');
const SqliteStore = require('connect-sqlite3')(session);

const logger   = require('./config/logger');
const { loadUser } = require('./middleware/auth');
const csrf     = require('./middleware/csrf');

// Route handlers
const authRoutes    = require('./routes/auth');
const notesRoutes   = require('./routes/notes');
const adminRoutes   = require('./routes/admin');
const profileRoutes = require('./routes/profile');
const searchRoutes  = require('./routes/search');
const shareRoutes   = require('./routes/share');

const app  = express();
const PORT = parseInt(process.env.PORT || '3000', 10);

// ── Security headers ────────────────────────────────────────────────────────
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc:  ["'self'", 'cdn.jsdelivr.net'],
      styleSrc:   ["'self'", 'cdn.jsdelivr.net'],
      fontSrc:    ["'self'", 'cdn.jsdelivr.net'],
      imgSrc:     ["'self'", 'data:'],
      frameSrc:   ["'none'"],
      objectSrc:  ["'none'"]
    }
  },
  crossOriginEmbedderPolicy: false
}));

// ── HTTP request logging ────────────────────────────────────────────────────
app.use(morgan('combined', {
  stream: { write: (msg) => logger.info(msg.trim()) }
}));

// ── Body parsing ────────────────────────────────────────────────────────────
app.use(express.urlencoded({ extended: false, limit: '1mb' }));
app.use(express.json({ limit: '1mb' }));

// ── Static files ─────────────────────────────────────────────────────────────
app.use(express.static(path.join(__dirname, '..', 'public')));

// ── Session ──────────────────────────────────────────────────────────────────
const sessionStore = new SqliteStore({
  db: 'sessions.db',
  dir: path.resolve('./data')
});

app.use(session({
  store: sessionStore,
  secret: process.env.SESSION_SECRET || 'CHANGE_ME_IN_PRODUCTION',
  resave: false,
  saveUninitialized: false,
  name: 'lnSid',   // avoid default 'connect.sid' fingerprinting
  cookie: {
    httpOnly: true,
    sameSite: 'lax',
    secure: process.env.NODE_ENV === 'production',
    maxAge: 24 * 60 * 60 * 1000  // 24 hours
  }
}));

// ── View engine ───────────────────────────────────────────────────────────────
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));

// ── Global middleware ─────────────────────────────────────────────────────────
app.use(loadUser);   // attach currentUser to res.locals
app.use(csrf);       // CSRF token for all mutating routes

// Flash-message helper (simple session-based)
app.use((req, res, next) => {
  res.locals.flash = req.session.flash || {};
  delete req.session.flash;

  res.flash = (type, msg) => {
    req.session.flash = req.session.flash || {};
    req.session.flash[type] = msg;
  };
  next();
});

// ── Routes ────────────────────────────────────────────────────────────────────
app.get('/', (req, res) => {
  if (req.session.userId) return res.redirect('/notes');
  res.redirect('/auth/login');
});

app.use('/auth',    authRoutes);
app.use('/notes',   notesRoutes);
app.use('/admin',   adminRoutes);
app.use('/profile', profileRoutes);
app.use('/search',  searchRoutes);
app.use('/share',   shareRoutes);

// ── 404 ───────────────────────────────────────────────────────────────────────
app.use((_req, res) => {
  res.status(404).render('error', { title: 'Not Found', message: 'Page not found.' });
});

// ── Global error handler ──────────────────────────────────────────────────────
// eslint-disable-next-line no-unused-vars
app.use((err, req, res, _next) => {
  logger.error('Unhandled error', { err: err.message, stack: err.stack, url: req.originalUrl });
  const status = err.status || 500;
  res.status(status).render('error', {
    title: 'Error',
    message: process.env.NODE_ENV === 'production' ? 'An unexpected error occurred.' : err.message
  });
});

// ── Start ──────────────────────────────────────────────────────────────────────
app.listen(PORT, () => {
  logger.info(`Loose Notes started on http://localhost:${PORT} [${process.env.NODE_ENV || 'development'}]`);
});

module.exports = app;
