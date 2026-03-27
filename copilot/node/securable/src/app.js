'use strict';

require('dotenv').config();

const express = require('express');
const helmet = require('helmet');
const session = require('express-session');
const methodOverride = require('method-override');
const path = require('path');

const { sessionConfig } = require('./config/session');
const { logger } = require('./config/logger');
const { loadUser } = require('./middleware/authenticate');
const { csrfMiddleware, csrfTokenHelper, csrfErrorHandler } = require('./middleware/csrfProtection');
const { notFoundHandler, globalErrorHandler } = require('./middleware/errorHandler');
const expressLayouts = require('express-ejs-layouts');
const mainRouter = require('./routes/index');

const app = express();

// --- Security headers (Availability + Confidentiality) ---
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", 'https://cdn.jsdelivr.net'],
      styleSrc: ["'self'", 'https://cdn.jsdelivr.net', "'unsafe-inline'"],
      fontSrc: ["'self'", 'https://cdn.jsdelivr.net'],
      imgSrc: ["'self'", 'data:'],
      connectSrc: ["'self'"],
      frameSrc: ["'none'"],
      objectSrc: ["'none'"],
    },
  },
}));

// --- Body parsing (limit enforces Availability) ---
app.use(express.urlencoded({ extended: true, limit: '1mb' }));
app.use(express.json({ limit: '1mb' }));

// --- Method override for PUT/DELETE from HTML forms ---
// Reads _method from body first, then query string
app.use(methodOverride((req) => {
  if (req.body && typeof req.body === 'object' && '_method' in req.body) {
    const method = req.body._method;
    delete req.body._method;
    return method;
  }
  return req.query._method;
}));

// --- Session ---
app.use(session(sessionConfig));

// --- Load authenticated user into req.user + res.locals.user ---
app.use(loadUser);

// --- Flash message relay (consume and expose for this request) ---
app.use((req, res, next) => {
  res.locals.flash = req.session.flash || null;
  if (req.session.flash) delete req.session.flash;
  next();
});

// --- CSRF protection (excluded for public share routes) ---
app.use((req, res, next) => {
  if (req.path.startsWith('/share')) return next();
  csrfMiddleware(req, res, next);
});
app.use(csrfTokenHelper);

// --- HTTP request logging (Transparency / Accountability) ---
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    logger.http('HTTP', {
      method: req.method,
      path: req.path,
      status: res.statusCode,
      ms: Date.now() - start,
      ip: req.ip,
    });
  });
  next();
});

// --- View engine ---
app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, 'views'));
app.use(expressLayouts);
app.set('layout', 'layout');
app.set('layout extractScripts', true);

// --- Static assets ---
app.use(express.static(path.join(__dirname, '..', 'public')));

// --- Routes ---
app.use(mainRouter);

// --- Error handlers (must be last) ---
app.use(csrfErrorHandler);
app.use(notFoundHandler);
app.use(globalErrorHandler);

module.exports = app;
