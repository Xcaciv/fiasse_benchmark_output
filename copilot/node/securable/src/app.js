'use strict';

const express = require('express');
const helmet = require('helmet');
const session = require('express-session');
const passport = require('passport');
const flash = require('connect-flash');
const csrf = require('csurf');
const path = require('path');
const logger = require('./utils/logger');
const { AppError } = require('./utils/AppError');
const configurePassport = require('./config/passport');

const app = express();

// 1. Security headers
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", 'cdn.jsdelivr.net'],
      styleSrc: ["'self'", 'cdn.jsdelivr.net', "'unsafe-inline'"],
      imgSrc: ["'self'", 'data:'],
      fontSrc: ["'self'", 'cdn.jsdelivr.net'],
    },
  },
}));

// 2. Body parsers with limits
app.use(express.json({ limit: '1mb' }));
app.use(express.urlencoded({ extended: false, limit: '1mb' }));

// 3. Static files
app.use(express.static(path.join(__dirname, '..', 'public')));

// 4. Session store
const SQLiteStore = require('connect-sqlite3')(session);

const sessionSecret = process.env.SESSION_SECRET;
if (process.env.NODE_ENV === 'production' && (!sessionSecret || sessionSecret.length < 32)) {
  logger.error('SESSION_SECRET must be set and at least 32 characters in production.');
  process.exit(1);
}

app.use(session({
  store: new SQLiteStore({ db: 'sessions.sqlite', dir: './data' }),
  secret: sessionSecret || 'dev-secret-change-in-production',
  resave: false,
  saveUninitialized: false,
  cookie: {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'strict',
    maxAge: 24 * 60 * 60 * 1000,
  },
}));

// 5. Passport
configurePassport(passport);
app.use(passport.initialize());
app.use(passport.session());

// 6. Flash messages
app.use(flash());

// 7. CSRF protection (after session/passport)
const csrfProtection = csrf({ cookie: false });
app.use(csrfProtection);

// 8. Template engine
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, '..', 'views'));

// 9. Locals middleware
app.use((req, res, next) => {
  res.locals.user = req.user || null;
  res.locals.csrfToken = req.csrfToken();
  res.locals.success = req.flash('success');
  res.locals.error = req.flash('error');
  res.locals.info = req.flash('info');
  res.locals.currentPath = req.path;
  next();
});

// 10. Routes
app.use('/', require('./routes/index'));

// 11. 404 handler
app.use((_req, res) => {
  res.status(404).render('error', { statusCode: 404, message: 'Page not found.' });
});

// 12. Global error handler — no stack traces to client
app.use((err, req, res, _next) => {
  if (err.code === 'EBADCSRFTOKEN') {
    logger.warn('CSRF token validation failed', { ip: req.ip, path: req.path });
    return res.status(403).render('error', { statusCode: 403, message: 'Invalid form submission. Please try again.' });
  }

  const statusCode = err.statusCode || 500;
  const isOperational = err.isOperational === true;
  const userMessage = isOperational ? err.message : 'An unexpected error occurred. Please try again later.';

  logger.error('Unhandled error', {
    message: err.message,
    stack: err.stack,
    path: req.path,
    method: req.method,
    userId: req.user ? req.user.id : null,
  });

  if (res.headersSent) return;
  res.status(statusCode).render('error', { statusCode, message: userMessage });
});

module.exports = app;
