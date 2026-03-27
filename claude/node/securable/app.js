'use strict';

require('dotenv').config();

const express = require('express');
const helmet = require('helmet');
const session = require('express-session');
const SequelizeStore = require('connect-session-sequelize')(session.Store);
const passport = require('passport');
const flash = require('connect-flash');
const csurf = require('csurf');
const path = require('path');
const { engine } = require('express-handlebars');
const Handlebars = require('handlebars');

const { sequelize, connectDatabase } = require('./config/database');
const { configurePassport } = require('./config/passport');
const security = require('./config/security');
const logger = require('./config/logger');
const { generalLimiter } = require('./middleware/rateLimiter');
const { errorHandler, notFoundHandler } = require('./middleware/errorHandler');

const authRoutes = require('./routes/auth');
const notesRoutes = require('./routes/notes');
const attachmentsRouter = require('./routes/attachments');
const shareRoutes = require('./routes/share');
const adminRoutes = require('./routes/admin');
const profileRoutes = require('./routes/profile');

const app = express();

// ── Security Headers (Helmet) ─────────────────────────────────────────────
// Confidentiality + Integrity: restrict browser feature surface
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      styleSrc: ["'self'", 'https://cdn.jsdelivr.net'],
      scriptSrc: ["'self'", 'https://cdn.jsdelivr.net'],
      imgSrc: ["'self'", 'data:'],
      fontSrc: ["'self'", 'https://cdn.jsdelivr.net'],
    },
  },
  referrerPolicy: { policy: 'same-origin' },
}));

// ── View Engine ───────────────────────────────────────────────────────────
app.engine('handlebars', engine({
  defaultLayout: 'main',
  layoutsDir: path.join(__dirname, 'views/layouts'),
  partialsDir: path.join(__dirname, 'views/partials'),
  helpers: {
    eq: (a, b) => a === b,
    formatDate: (d) => (d ? new Date(d).toLocaleDateString() : ''),
    excerpt: (text, len = 200) => (text && text.length > len ? text.substring(0, len) + '…' : text || ''),
    stars: (n) => '★'.repeat(Math.max(0, Math.min(5, parseInt(n, 10) || 0))),
    encodeURIComponent: (s) => encodeURIComponent(s),
  },
}));
app.set('view engine', 'handlebars');
app.set('views', path.join(__dirname, 'views'));

// ── Body Parsers ─────────────────────────────────────────────────────────
// Limit payload size — Availability: prevent large payload DoS
app.use(express.urlencoded({ extended: false, limit: '64kb' }));
app.use(express.json({ limit: '64kb' }));

// ── Static Files ───────────────────────────────────────────────────────────
app.use(express.static(path.join(__dirname, 'public'), { maxAge: '1d' }));

// ── Session ────────────────────────────────────────────────────────────────
const sessionStore = new SequelizeStore({ db: sequelize, checkExpirationInterval: 15 * 60 * 1000, expiration: 8 * 60 * 60 * 1000 });

app.use(session({
  secret: process.env.SESSION_SECRET || (() => {
    if (process.env.NODE_ENV === 'production') throw new Error('SESSION_SECRET must be set in production');
    return 'dev-only-insecure-secret-change-me';
  })(),
  resave: false,
  saveUninitialized: false,
  store: sessionStore,
  cookie: security.SESSION_COOKIE_OPTIONS,
  name: 'sid', // obscure default name — Confidentiality
}));

// ── Passport ───────────────────────────────────────────────────────────────
configurePassport();
app.use(passport.initialize());
app.use(passport.session());

// ── Flash Messages ─────────────────────────────────────────────────────────
app.use(flash());

// ── CSRF Protection ────────────────────────────────────────────────────────
// Applied after session; Integrity: state-changing forms require valid token
app.use(csurf());

// ── Rate Limiting ──────────────────────────────────────────────────────────
app.use(generalLimiter);

// ── Template Locals ────────────────────────────────────────────────────────
// Make user, flash messages, and CSRF available to all views
app.use((req, res, next) => {
  res.locals.currentUser = req.user || null;
  res.locals.isAdmin = req.user?.role === 'admin';
  res.locals.flashSuccess = req.flash('success');
  res.locals.flashError = req.flash('error');
  res.locals.csrfToken = req.csrfToken();
  res.locals.currentYear = new Date().getFullYear();
  next();
});

// ── Routes ──────────────────────────────────────────────────────────────────
app.get('/', (req, res) => {
  if (req.isAuthenticated()) return res.redirect('/notes');
  return res.render('home', { title: 'Loose Notes', layout: 'main' });
});

app.use('/auth', authRoutes);
app.use('/notes', notesRoutes);
// Nested attachment routes — mergeParams on the router passes :noteId through
app.use('/notes/:noteId/attachments', attachmentsRouter);
app.use('/share', shareRoutes);
app.use('/admin', adminRoutes);
app.use('/profile', profileRoutes);

// ── Error Handling ─────────────────────────────────────────────────────────
app.use(notFoundHandler);
app.use(errorHandler);

// ── Bootstrap ──────────────────────────────────────────────────────────────
const PORT = parseInt(process.env.PORT, 10) || 3000;

async function start() {
  await connectDatabase();
  const { sequelize: db } = require('./models');
  await db.sync({ alter: false, force: false });
  sessionStore.sync();

  app.listen(PORT, () => {
    logger.info(`Loose Notes started on port ${PORT} [${process.env.NODE_ENV || 'development'}]`);
  });
}

start().catch((err) => {
  logger.error('Failed to start application', { error: err.message, stack: err.stack });
  process.exit(1);
});

module.exports = app; // for testing
