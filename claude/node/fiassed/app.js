'use strict';

require('dotenv').config();

const path = require('path');
const express = require('express');
const session = require('express-session');
const flash = require('connect-flash');
const methodOverride = require('method-override');
const csrf = require('csurf');
const ConnectSqlite3 = require('connect-sqlite3')(session);

const { correlationId, helmetMiddleware } = require('./middleware/security');
const { softAuthenticate } = require('./middleware/authenticate');
const { generalLimiter } = require('./middleware/rateLimiter');
const { errorHandler, notFoundHandler } = require('./middleware/errorHandler');
const { logger } = require('./config/logger');
const constants = require('./config/constants');

// ─── App Factory ──────────────────────────────────────────────────────────────

function createApp() {
  const app = express();

  // Trust proxy if configured (needed for correct IP behind load balancers)
  if (process.env.TRUST_PROXY === 'true') {
    app.set('trust proxy', 1);
  }

  // ─── View Engine ───────────────────────────────────────────────────────
  app.set('view engine', 'ejs');
  app.set('views', path.join(__dirname, 'views'));

  // EJS layouts support via simple wrapper (no extra library)
  app.use((req, res, next) => {
    const originalRender = res.render.bind(res);
    res.render = (view, locals, callback) => {
      const data = typeof locals === 'object' ? locals : {};
      originalRender(view, data, (err, html) => {
        if (err) return next(err);
        // Inject rendered body into layout
        const layoutLocals = Object.assign({}, data, { body: html });
        originalRender('layout', layoutLocals, callback || ((err2, fullHtml) => {
          if (err2) return next(err2);
          res.send(fullHtml);
        }));
      });
    };
    next();
  });

  // ─── Security Headers ──────────────────────────────────────────────────
  app.use(helmetMiddleware);

  // ─── Correlation ID (attach early for all request tracing) ─────────────
  app.use(correlationId);

  // ─── Request Parsing ───────────────────────────────────────────────────
  app.use(express.urlencoded({ extended: false, limit: '1mb' }));
  app.use(express.json({ limit: '1mb' }));

  // ─── Method Override (supports DELETE/PUT via POST + _method) ──────────
  app.use(methodOverride('_method'));

  // ─── Static Files ──────────────────────────────────────────────────────
  app.use(express.static(path.join(__dirname, 'public'), {
    maxAge: '1h',
    etag: true
  }));

  // ─── Session ───────────────────────────────────────────────────────────
  // Confidentiality: session secret from env, never hardcoded
  const sessionSecret = process.env.SESSION_SECRET;
  if (!sessionSecret || sessionSecret.length < 32) {
    logger.warn('SESSION_SECRET is missing or too short - use a long random value in production', {
      event: 'app.config_warning'
    });
  }

  const dbPath = process.env.DATABASE_PATH || path.join(__dirname, 'data', 'loose-notes.db');

  const sessionStore = new ConnectSqlite3({
    db: path.basename(dbPath),
    dir: path.dirname(path.resolve(dbPath)),
    table: 'sessions'
  });

  app.use(session({
    store: sessionStore,
    secret: sessionSecret || 'change-this-insecure-default',
    resave: false,
    saveUninitialized: false,
    name: 'sessionId',  // Don't use default 'connect.sid' name
    cookie: {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      maxAge: constants.AUTH.SESSION_IDLE_MS
    }
  }));

  // Absolute session timeout enforcement
  app.use((req, res, next) => {
    if (!req.session.loginAt) return next();
    const age = Date.now() - req.session.loginAt;
    if (age > constants.AUTH.SESSION_ABSOLUTE_MS) {
      return req.session.destroy(() => {
        res.redirect('/auth/login');
      });
    }
    next();
  });

  // ─── Flash Messages ────────────────────────────────────────────────────
  app.use(flash());

  // ─── CSRF Protection ───────────────────────────────────────────────────
  // Applied globally; excluded paths bypass below
  const csrfProtection = csrf({ cookie: false });

  // Exclude unauthenticated public share view from CSRF (no session/form)
  app.use((req, res, next) => {
    if (req.path.startsWith('/share/view/')) return next();
    csrfProtection(req, res, next);
  });

  // ─── Template Locals ───────────────────────────────────────────────────
  app.use((req, res, next) => {
    res.locals.messages = req.flash();
    res.locals.correlationId = req.correlationId;
    // csrfToken available on all views (will be empty string for excluded paths)
    try {
      res.locals.csrfToken = req.csrfToken ? req.csrfToken() : '';
    } catch {
      res.locals.csrfToken = '';
    }
    next();
  });

  // ─── Rate Limiting (general) ───────────────────────────────────────────
  app.use(generalLimiter);

  // ─── Routes ────────────────────────────────────────────────────────────
  app.use('/', require('./routes/index'));
  app.use('/auth', require('./routes/auth'));
  app.use('/notes', softAuthenticate, require('./routes/notes'));
  app.use('/attachments', require('./routes/attachments'));
  app.use('/share', softAuthenticate, require('./routes/share'));
  app.use('/profile', require('./routes/profile'));
  app.use('/admin', require('./routes/admin'));

  // ─── Error Handling ────────────────────────────────────────────────────
  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}

module.exports = { createApp };
