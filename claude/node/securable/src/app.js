'use strict';

/**
 * Express application factory.
 * Security middleware applied before any route handlers.
 * SSEM Integrity / Availability / Authenticity.
 */

const path = require('path');
const express = require('express');
const helmet = require('helmet');
const session = require('express-session');
const flash = require('connect-flash');
const expressLayouts = require('express-ejs-layouts');
const methodOverride = require('method-override');
const { csrfSync } = require('csrf-sync');

const passport = require('./config/passport');
const security = require('./config/security');
const logger = require('./config/logger');
const { generalLimiter } = require('./middleware/rateLimiter');
const { errorHandler, notFoundHandler } = require('./middleware/errorHandler');

const indexRouter = require('./routes/index');
const authRouter = require('./routes/auth');
const notesRouter = require('./routes/notes');
const attachmentsRouter = require('./routes/attachments');
const profileRouter = require('./routes/profile');
const shareRouter = require('./routes/share');
const adminRouter = require('./routes/admin');

function createApp() {
  const app = express();

  // ── Security headers ────────────────────────────────────────────────────────
  app.use(
    helmet({
      contentSecurityPolicy: {
        directives: {
          defaultSrc: ["'self'"],
          scriptSrc: ["'self'"],
          styleSrc: ["'self'", "'unsafe-inline'"],
          imgSrc: ["'self'", 'data:'],
          fontSrc: ["'self'"],
          objectSrc: ["'none'"],
          frameAncestors: ["'none'"],
        },
      },
      crossOriginEmbedderPolicy: false,
    })
  );

  // ── View engine ─────────────────────────────────────────────────────────────
  app.set('view engine', 'ejs');
  app.set('views', path.join(__dirname, 'views'));
  app.use(expressLayouts);
  app.set('layout', 'layout');

  // ── Static files ────────────────────────────────────────────────────────────
  app.use(express.static(path.join(__dirname, '..', 'public')));

  // ── Body parsers ─────────────────────────────────────────────────────────────
  app.use(express.urlencoded({ extended: false, limit: '256kb' }));
  app.use(express.json({ limit: '256kb' }));
  app.use(methodOverride('_method'));

  // ── Session ──────────────────────────────────────────────────────────────────
  app.use(
    session({
      secret: security.sessionSecret,
      resave: false,
      saveUninitialized: false,
      cookie: {
        httpOnly: true,
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'lax',
        maxAge: security.sessionCookieMaxAge,
      },
    })
  );

  // ── Passport ─────────────────────────────────────────────────────────────────
  app.use(passport.initialize());
  app.use(passport.session());

  // ── Flash messages ───────────────────────────────────────────────────────────
  app.use(flash());

  // ── CSRF protection ──────────────────────────────────────────────────────────
  // csrf-sync uses the synchronizer token pattern.
  // The token is injected into res.locals so views can include it in forms.
  const { csrfSynchronisedProtection, generateToken } = csrfSync({
    getTokenFromRequest: (req) => {
      return req.body && req.body._csrf;
    },
  });

  // Attach CSRF token generator to res.locals for all views
  app.use((req, res, next) => {
    res.locals.csrfToken = generateToken(req);
    next();
  });

  // Apply CSRF protection to all state-changing routes
  app.use((req, res, next) => {
    const safeMethods = new Set(['GET', 'HEAD', 'OPTIONS']);
    if (safeMethods.has(req.method)) return next();
    csrfSynchronisedProtection(req, res, next);
  });

  // ── Rate limiting ────────────────────────────────────────────────────────────
  app.use(generalLimiter);

  // ── Template locals ───────────────────────────────────────────────────────────
  app.use((req, res, next) => {
    res.locals.user = req.user || null;
    res.locals.flash = {
      success: req.flash('success'),
      error: req.flash('error'),
    };
    next();
  });

  // ── Request logging ───────────────────────────────────────────────────────────
  app.use((req, res, next) => {
    const start = Date.now();
    res.on('finish', () => {
      logger.info({
        event: 'HTTP_REQUEST',
        method: req.method,
        path: req.path,
        status: res.statusCode,
        ms: Date.now() - start,
        userId: req.user?.id,
        ip: req.ip,
      });
    });
    next();
  });

  // ── Routes ────────────────────────────────────────────────────────────────────
  app.use('/', indexRouter);
  app.use('/auth', authRouter);
  app.use('/notes', notesRouter);
  app.use('/attachments', attachmentsRouter);
  app.use('/profile', profileRouter);
  app.use('/share', shareRouter);
  app.use('/admin', adminRouter);

  // ── 404 / Error ───────────────────────────────────────────────────────────────
  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}

module.exports = createApp;
