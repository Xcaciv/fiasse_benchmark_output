'use strict';
const express = require('express');
const helmet = require('helmet');
const morgan = require('morgan');
const session = require('express-session');
const passport = require('passport');
const flash = require('connect-flash');
const csrf = require('csurf');
const expressLayouts = require('express-ejs-layouts');
const path = require('path');
const SequelizeStore = require('connect-session-sequelize')(session.Store);
const { sequelize } = require('./config/database');
const { configurePassport } = require('./config/passport');
const { User } = require('./models');
const { mountRoutes } = require('./routes/index');
const { errorHandler } = require('./middleware/errorHandler');
const { globalLimiter } = require('./middleware/rateLimiter');
const { SESSION_MAX_AGE_MS } = require('./config/constants');
const logger = require('./utils/logger');

function createApp(config = {}) {
  const app = express();

  // Security headers — Availability + Confidentiality
  app.use(helmet({
    contentSecurityPolicy: {
      directives: {
        defaultSrc: ["'self'"],
        scriptSrc: ["'self'", 'cdn.jsdelivr.net'],
        styleSrc: ["'self'", 'cdn.jsdelivr.net', "'unsafe-inline'"],
        imgSrc: ["'self'", 'data:']
      }
    }
  }));

  app.use(globalLimiter);
  app.use(morgan('combined', { stream: { write: (msg) => logger.http(msg.trim()) } }));

  // Body parsing with size limits — Availability: prevent large payload attacks
  app.use(express.urlencoded({ extended: true, limit: '1mb' }));
  app.use(express.json({ limit: '1mb' }));

  // Session — Authenticity: signed, httpOnly, sameSite strict
  const sessionStore = new SequelizeStore({ db: sequelize });
  app.use(session({
    secret: process.env.SESSION_SECRET || 'dev-secret-change-in-production',
    store: sessionStore,
    resave: false,
    saveUninitialized: false,
    cookie: {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'strict',
      maxAge: SESSION_MAX_AGE_MS
    }
  }));
  sessionStore.sync();

  // CSRF — Integrity: protect all state-changing requests
  app.use(csrf());

  // Auth
  configurePassport(passport, User);
  app.use(passport.initialize());
  app.use(passport.session());
  app.use(flash());

  // Views
  app.set('view engine', 'ejs');
  app.set('views', path.join(__dirname, 'views'));
  app.use(expressLayouts);
  app.set('layout', 'layouts/main');

  // Inject locals for all views
  app.use((req, res, next) => {
    res.locals.currentUser = req.user || null;
    res.locals.csrfToken = req.csrfToken();
    res.locals.success = req.flash('success');
    res.locals.error = req.flash('error');
    next();
  });

  app.use(express.static(path.join(__dirname, '..', 'public')));

  // Mount all routes
  mountRoutes(app);

  // 404 handler
  app.use((req, res) => res.status(404).render('error', { message: 'Page not found', layout: 'layouts/main' }));

  // Centralized error handler
  app.use(errorHandler);

  return app;
}

module.exports = { createApp };
